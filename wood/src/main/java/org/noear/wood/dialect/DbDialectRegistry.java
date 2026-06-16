package org.noear.wood.dialect;

import org.noear.wood.ext.Fun1;
import org.noear.wood.wrap.DbType;

import java.sql.Connection;
import java.sql.SQLException;
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
        r.register(new DbMySQLDialect(),        DbType.MySQL,        c -> urlOf(c).startsWith("jdbc:mysql:"));
        r.register(new DbMySQLDialect(),        DbType.MariaDB,      c -> urlOf(c).startsWith("jdbc:mariadb:"));
        r.register(new DbSQLServerDialect(),    DbType.SQLServer,    c -> urlOf(c).startsWith("jdbc:sqlserver:"));
        r.register(new DbOracleDialect(),       DbType.Oracle,       c -> urlOf(c).startsWith("jdbc:oracle:"));
        r.register(new DbPostgreSQLDialect(),   DbType.PostgreSQL,   c -> urlOf(c).startsWith("jdbc:postgresql:"));
        r.register(new DbDb2Dialect(),          DbType.DB2,          c -> urlOf(c).startsWith("jdbc:db2:"));
        r.register(new DbSQLiteDialect(),       DbType.SQLite,       c -> urlOf(c).startsWith("jdbc:sqlite:"));
        r.register(new DbH2Dialect(),           DbType.H2,           c -> urlOf(c).startsWith("jdbc:h2:"));
        r.register(new DbPhoenixDialect(),      DbType.Phoenix,      c -> urlOf(c).startsWith("jdbc:phoenix:"));
        r.register(new DbClickHouseDialect(),   DbType.ClickHouse,   c -> urlOf(c).startsWith("jdbc:clickhouse:"));
        r.register(new DbPrestoDialect(),       DbType.Presto,       c -> urlOf(c).startsWith("jdbc:presto:"));
        r.register(new DbDuckDbDialect(),       DbType.DuckDb,       c -> urlOf(c).startsWith("jdbc:duckdb:"));
        r.register(new DbDamengDialect(),       DbType.DM,           c -> urlOf(c).startsWith("jdbc:dm:"));
        r.register(new DbOceanBaseMySQLDialect(),  DbType.OceanBase,  c -> urlOf(c).startsWith("jdbc:oceanbase:") && isOceanBaseMysql(c));
        r.register(new DbOceanBaseOracleDialect(), DbType.OceanBase,  c -> urlOf(c).startsWith("jdbc:oceanbase:") && !isOceanBaseMysql(c));
        r.register(new DbKingbaseMySQLDialect(),   DbType.KingbaseES, c -> urlOf(c).startsWith("jdbc:kingbase") && "mysql".equalsIgnoreCase(getKingbaseMode(c)));
        r.register(new DbKingbaseOracleDialect(),  DbType.KingbaseES, c -> urlOf(c).startsWith("jdbc:kingbase") && "oracle".equalsIgnoreCase(getKingbaseMode(c)));
        r.register(new DbKingbasePostgreDialect(), DbType.KingbaseES, c -> urlOf(c).startsWith("jdbc:kingbase"));
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
        this.matchers.add(new MatcherEntry(dialect, DbType.External, matcher));
    }

    /** 内部用：注册时携带具体 DbType（用于 builtin 预填） */
    void register(DbDialect dialect, DbType type, Fun1<Boolean, Connection> matcher) {
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
            return new Match(fixedDialect, fixedType, true);
        }
        for (MatcherEntry e : matchers) {
            try {
                if (e.matcher.run(conn)) {
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
        final Fun1<Boolean, Connection> matcher;

        MatcherEntry(DbDialect dialect, DbType type, Fun1<Boolean, Connection> matcher) {
            this.dialect = dialect;
            this.type = type;
            this.matcher = matcher;
        }
    }

    private static String urlOf(Connection c) {
        if (c == null) return null;
        try {
            return c.getMetaData().getURL();
        } catch (SQLException e) {
            return null;
        }
    }

    private static boolean isOceanBaseMysql(Connection c) {
        if (c == null) return true;
        try (java.sql.Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery("show global variables where variable_name = 'ob_compatibility_mode'")) {
            if (rs.next()) {
                String v = rs.getString(2);
                if (v != null) return v.toUpperCase().contains("MYSQL");
            }
        } catch (Throwable ignore) {
            // 驱动不支持这条 SQL 时按 MySQL 处理（保留原 DbContextMetaData 行为）
        }
        return true;
    }

    private static String getKingbaseMode(Connection c) {
        if (c == null) return null;
        try (java.sql.Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery("show database_mode")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Throwable ignore) {
            // 驱动不支持 / 查询失败 → 返回 null，让调用方走 fallback
        }
        return null;
    }
}
