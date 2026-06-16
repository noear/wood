package org.noear.wood.dialect;

import org.noear.wood.ext.Fun1;
import org.noear.wood.wrap.DbType;

import java.sql.Connection;
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

    /** 构造一个预填了 20 个内置方言的注册表（Task 7 实现） */
    public static DbDialectRegistry builtin() {
        return new DbDialectRegistry();
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
}
