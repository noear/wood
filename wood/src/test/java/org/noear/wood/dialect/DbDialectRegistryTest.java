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
}
