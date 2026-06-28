package org.noear.wood.dialect;

import org.noear.wood.ext.Fun1;
import org.noear.wood.wrap.DbType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库方言注册表：把"识别数据库 → 选方言"的逻辑从硬编码 if/else 中抽出。
 *
 * <p>查找顺序：fixed（来自 setDialect） → matchers（按注册顺序） → fallback。</p>
 *
 * @author noear
 * @since 1.4.5
 */
public class DbDialectRegistry {

    private DbDialect fixedDialect;
    private DbType fixedType;

    private final List<MatcherEntry> matchers = new ArrayList<>();

    private DbDialect fallback = new DbMySQLDialect();
    private DbType fallbackType = DbType.MySQL;

    /** 构造一个预填了内置方言的注册表 */
    public static DbDialectRegistry builtin() {
        DbDialectRegistry r = new DbDialectRegistry();
        r.register(new DbMySQLDialect(),        DbType.MySQL,        ctx -> urlStartsWith(ctx, "jdbc:mysql:"));
        r.register(new DbMySQLDialect(),        DbType.MariaDB,      ctx -> urlStartsWith(ctx, "jdbc:mariadb:"));
        r.register(new DbSQLServerDialect(),    DbType.SQLServer,    ctx -> urlStartsWith(ctx, "jdbc:sqlserver:"));
        r.register(new DbOracleDialect(),       DbType.Oracle,       ctx -> urlStartsWith(ctx, "jdbc:oracle:"));
        r.register(new DbPostgreSQLDialect(),   DbType.PostgreSQL,   ctx -> urlStartsWith(ctx, "jdbc:postgresql:"));
        r.register(new DbDb2Dialect(),          DbType.DB2,          ctx -> urlStartsWith(ctx, "jdbc:db2:"));
        r.register(new DbSQLiteDialect(),       DbType.SQLite,       ctx -> urlStartsWith(ctx, "jdbc:sqlite:"));
        r.register(new DbH2Dialect(),           DbType.H2,           ctx -> urlStartsWith(ctx, "jdbc:h2:"));
        r.register(new DbPhoenixDialect(),      DbType.Phoenix,      ctx -> urlStartsWith(ctx, "jdbc:phoenix:"));
        r.register(new DbClickHouseDialect(),   DbType.ClickHouse,   ctx -> urlStartsWith(ctx, "jdbc:clickhouse:"));
        r.register(new DbPrestoDialect(),       DbType.Presto,       ctx -> urlStartsWith(ctx, "jdbc:presto:"));
        r.register(new DbDuckDbDialect(),       DbType.DuckDb,       ctx -> urlStartsWith(ctx, "jdbc:duckdb:"));
        r.register(new DbDamengDialect(),       DbType.DM,           ctx -> urlStartsWith(ctx, "jdbc:dm:"));
        r.register(new DbOceanBaseMySQLDialect(),  DbType.OceanBase,  ctx -> urlStartsWith(ctx, "jdbc:oceanbase:") && isOceanBaseMysql(ctx));
        r.register(new DbOceanBaseOracleDialect(), DbType.OceanBase,  ctx -> urlStartsWith(ctx, "jdbc:oceanbase:") && !isOceanBaseMysql(ctx));
        r.register(new DbKingbaseMySQLDialect(),   DbType.KingbaseES, ctx -> urlStartsWith(ctx, "jdbc:kingbase") && "mysql".equalsIgnoreCase(kingbaseMode(ctx)));
        r.register(new DbKingbaseOracleDialect(),  DbType.KingbaseES, ctx -> urlStartsWith(ctx, "jdbc:kingbase") && "oracle".equalsIgnoreCase(kingbaseMode(ctx)));
        r.register(new DbKingbasePostgreDialect(), DbType.KingbaseES, ctx -> urlStartsWith(ctx, "jdbc:kingbase"));
        return r;
    }

    /** 外部注册：dialect + matcher，type 固定为 DbType.External */
    public void register(DbDialect dialect, Fun1<Boolean, Connection> matcher) {
        if (dialect == null) {
            throw new IllegalArgumentException("dialect cannot be null");
        }
        if (matcher == null) {
            throw new IllegalArgumentException("matcher cannot be null");
        }
        // 外部 matcher 仍按 Connection 匹配，包一层适配到 FindContext
        this.matchers.add(new MatcherEntry(dialect, DbType.External, ctx -> matcher.run(ctx.conn)));
    }

    /** 内部用：注册时携带具体 DbType（用于 builtin 预填） */
    void register(DbDialect dialect, DbType type, Fun1<Boolean, FindContext> matcher) {
        if (dialect == null) {
            throw new IllegalArgumentException("dialect cannot be null");
        }
        if (matcher == null) {
            throw new IllegalArgumentException("matcher cannot be null");
        }
        this.matchers.add(new MatcherEntry(dialect, type, matcher));
    }

    /** 强制覆盖：来自 DbContext.setDialect；传 null 等于取消 */
    public void setFixed(DbDialect dialect, DbType type) {
        this.fixedDialect = dialect;
        this.fixedType = (dialect == null) ? null : type;
    }

    /** 兜底：未命中时使用 */
    public void setFallback(DbDialect dialect, DbType type) {
        if (dialect == null) {
            throw new IllegalArgumentException("fallback dialect cannot be null");
        }
        this.fallback = dialect;
        this.fallbackType = (type == null) ? DbType.Unknown : type;
    }

    /** 查找 */
    public Match find(Connection conn) {
        if (fixedDialect != null) {
            return new Match(fixedDialect, fixedType, false);
        }
        // 一次 find 内只取一次 url、只探测一次 OceanBase/Kingbase，避免 matcher 重复开销
        FindContext ctx = new FindContext(conn);
        for (MatcherEntry e : matchers) {
            try {
                if (e.matcher.run(ctx)) {
                    return new Match(e.dialect, e.type, false);
                }
            } catch (Throwable ex) {
                // matcher 异常：跳过继续遍历
                org.slf4j.LoggerFactory.getLogger(DbDialectRegistry.class)
                        .warn("[Wood] Dialect matcher failed: {}", ex.getMessage());
            }
        }
        return new Match(fallback, fallbackType, true);
    }

    public static class Match {
        public final DbDialect dialect;
        public final DbType type;
        public final boolean isFallback;

        public Match(DbDialect dialect, DbType type, boolean isFallback) {
            this.dialect = dialect;
            this.type = type;
            this.isFallback = isFallback;
        }
    }

    private static class MatcherEntry {
        final DbDialect dialect;
        final DbType type;
        final Fun1<Boolean, FindContext> matcher;

        MatcherEntry(DbDialect dialect, DbType type, Fun1<Boolean, FindContext> matcher) {
            this.dialect = dialect;
            this.type = type;
            this.matcher = matcher;
        }
    }

    private static boolean urlStartsWith(FindContext ctx, String prefix) {
        String u = ctx.url();
        return u != null && u.startsWith(prefix);
    }

    private static boolean isOceanBaseMysql(FindContext ctx) {
        return ctx.cached("ob_mysql", k -> computeOceanBaseMysql(ctx.conn));
    }

    private static String kingbaseMode(FindContext ctx) {
        return ctx.cached("kingbase_mode", k -> computeKingbaseMode(ctx.conn));
    }

    private static boolean computeOceanBaseMysql(Connection c) {
        if (c == null) return true;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("show global variables where variable_name = 'ob_compatibility_mode'")) {
            if (rs.next()) {
                String v = rs.getString(2);
                if (v != null) return v.toUpperCase().contains("MYSQL");
            }
        } catch (Throwable ignore) {
            // 驱动不支持这条 SQL 时按 MySQL 处理（保留原 DbContextMetaData 行为）
        }
        return true;
    }

    private static String computeKingbaseMode(Connection c) {
        if (c == null) return null;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("show database_mode")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Throwable ignore) {
            // 驱动不支持 / 查询失败 → 返回 null，让调用方走 fallback
        }
        return null;
    }
}
