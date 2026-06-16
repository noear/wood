package org.noear.wood.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Mock 工具：构造一个返回指定 url 的 Connection。 */
final class TestDialectSupport {

    private TestDialectSupport() {}

    static Connection mockConn(String url) throws SQLException {
        Connection conn = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(conn.getMetaData()).thenReturn(md);
        when(md.getURL()).thenReturn(url);
        return conn;
    }
}
