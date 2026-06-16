package org.noear.wood.dialect;

import org.junit.jupiter.api.Test;
import org.noear.wood.wrap.DbType;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbDialectRegistryBuiltinTest {

    private final DbDialectRegistry builtin = DbDialectRegistry.builtin();

    @Test
    void mysql_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:mysql://localhost/test", "DbMySQLDialect", DbType.MySQL);
    }

    @Test
    void mariadb_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:mariadb://localhost/test", "DbMySQLDialect", DbType.MariaDB);
    }

    @Test
    void sqlserver_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:sqlserver://localhost;DatabaseName=test", "DbSQLServerDialect", DbType.SQLServer);
    }

    @Test
    void oracle_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:oracle:thin:@localhost:1521:ORCL", "DbOracleDialect", DbType.Oracle);
    }

    @Test
    void postgresql_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:postgresql://localhost/test", "DbPostgreSQLDialect", DbType.PostgreSQL);
    }

    @Test
    void db2_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:db2://localhost:50000/test", "DbDb2Dialect", DbType.DB2);
    }

    @Test
    void sqlite_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:sqlite::memory:", "DbSQLiteDialect", DbType.SQLite);
    }

    @Test
    void h2_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:h2:mem:test", "DbH2Dialect", DbType.H2);
    }

    @Test
    void phoenix_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:phoenix:localhost", "DbPhoenixDialect", DbType.Phoenix);
    }

    @Test
    void clickhouse_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:clickhouse://localhost:8123/system", "DbClickHouseDialect", DbType.ClickHouse);
    }

    @Test
    void presto_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:presto://localhost:8080", "DbPrestoDialect", DbType.Presto);
    }

    @Test
    void duckdb_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:duckdb:", "DbDuckDbDialect", DbType.DuckDb);
    }

    @Test
    void dm_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:dm://localhost:5236", "DbDamengDialect", DbType.DM);
    }

    @Test
    void kingbaseByPostgresByDefault() throws SQLException {
        // getKingbaseMode 会抛 SQLException → catch 后返回 null → 落到 postgre fallback
        assertMatch("jdbc:kingbase8://localhost:54321/test", "DbKingbasePostgreDialect", DbType.KingbaseES);
    }

    @Test
    void unknownUrl_fallsBackToMysql() throws SQLException {
        Connection conn = TestDialectSupport.mockConn("jdbc:somethingweird://localhost");
        DbDialectRegistry.Match m = builtin.find(conn);
        assertTrue(m.isFallback);
        assertEquals(DbType.MySQL, m.type);
    }

    private void assertMatch(String url, String expectedDialectTypeName, DbType expectedType) throws SQLException {
        Connection conn = TestDialectSupport.mockConn(url);
        DbDialectRegistry.Match m = builtin.find(conn);
        assertFalse(m.isFallback, "expected non-fallback match for " + url);
        assertEquals(expectedType, m.type, "type mismatch for " + url);
        assertEquals(expectedDialectTypeName, m.dialect.typeName(), "dialect mismatch for " + url);
    }
}
