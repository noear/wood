package org.noear.wood.dialect;

import org.junit.jupiter.api.Test;
import org.noear.wood.DbContext;
import org.noear.wood.wrap.DbType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbContextSetDialectTest {

    @Test
    void setDialectBeforeInit_overridesDetectedDialect() throws SQLException {
        DataSource dataSource = mockDataSource("jdbc:mysql://localhost/test");
        DbDialect dialect = new DbH2Dialect();
        DbContext context = new DbContext(dataSource);

        context.setDialect(DbType.H2, dialect);

        assertSame(dialect, context.getDialect());
        assertEquals(DbType.H2, context.getType());
    }

    @Test
    void setDialectAfterInit_replacesCachedDialect() throws SQLException {
        DataSource dataSource = mockDataSource("jdbc:mysql://localhost/test");
        DbContext context = new DbContext(dataSource);
        DbDialect dialect = new DbH2Dialect();

        context.getDialect();
        context.setDialect(DbType.H2, dialect);

        assertSame(dialect, context.getDialect());
        assertEquals(DbType.H2, context.getType());
    }

    private DataSource mockDataSource(String url) throws SQLException {
        Connection connection = TestDialectSupport.mockConn(url);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
}
