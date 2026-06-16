package org.noear.wood.dialect;

import org.junit.jupiter.api.Test;
import org.noear.wood.wrap.DbType;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DbDialectRegistryTest {

    @Test
    void find_emptyRegistry_returnsFallback() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);

        DbDialectRegistry.Match m = r.find(conn);

        assertNotNull(m);
        assertTrue(m.isFallback);
        assertEquals(DbType.MySQL, m.type);
        assertNotNull(m.dialect);
        assertEquals("DbMySQLDialect", m.dialect.typeName());
    }

    @Test
    void setFallback_changesDefault() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);
        r.setFallback(new DbH2Dialect(), DbType.H2);

        DbDialectRegistry.Match m = r.find(conn);

        assertTrue(m.isFallback);
        assertEquals(DbType.H2, m.type);
        assertEquals("DbH2Dialect", m.dialect.typeName());
    }

    @Test
    void register_singleMatcher_matches() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);
        DbH2Dialect h2 = new DbH2Dialect();
        r.register(h2, c -> true);

        DbDialectRegistry.Match m = r.find(conn);

        assertFalse(m.isFallback);
        assertEquals(DbType.External, m.type);
        assertSame(h2, m.dialect);
    }

    @Test
    void register_noMatch_returnsFallback() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);
        r.register(new DbH2Dialect(), c -> false);

        DbDialectRegistry.Match m = r.find(conn);

        assertTrue(m.isFallback);
        assertEquals(DbType.MySQL, m.type);
    }

    @Test
    void register_multipleMatchers_firstWins() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);
        DbH2Dialect h2 = new DbH2Dialect();
        DbSQLiteDialect sqlite = new DbSQLiteDialect();
        r.register(h2, c -> true);
        r.register(sqlite, c -> true);

        DbDialectRegistry.Match m = r.find(conn);

        assertSame(h2, m.dialect, "先注册的应胜出");
    }

    @Test
    void register_matcherThrows_continuesToNext() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);
        DbH2Dialect h2 = new DbH2Dialect();
        r.register(new DbSQLiteDialect(), c -> { throw new RuntimeException("boom"); });
        r.register(h2, c -> true);

        DbDialectRegistry.Match m = r.find(conn);

        assertSame(h2, m.dialect, "前一个 matcher 抛异常应跳过");
    }

    @Test
    void register_nullDialect_throws() {
        DbDialectRegistry r = new DbDialectRegistry();
        assertThrows(IllegalArgumentException.class, () -> r.register(null, c -> true));
    }

    @Test
    void register_nullMatcher_throws() {
        DbDialectRegistry r = new DbDialectRegistry();
        assertThrows(IllegalArgumentException.class, () -> r.register(new DbH2Dialect(), null));
    }
}
