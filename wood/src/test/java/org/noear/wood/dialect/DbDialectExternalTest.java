package org.noear.wood.dialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.noear.wood.WoodConfig;
import org.noear.wood.wrap.DbType;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DbDialectExternalTest {

    @AfterEach
    void cleanup() {
        // 还原 global registry 到 builtin，避免污染其他测试
        WoodConfig.globalDialectRegistry = DbDialectRegistry.builtin();
    }

    @Test
    void externalRegistry_globalSelectsExternal() throws SQLException {
        // 注册一个 "MyDb" 方言
        DbDialect myDb = new DbMySQLDialect();  // 复用即可，验证机制
        WoodConfig.globalDialectRegistry.register(myDb, c -> {
            try {
                String url = c.getMetaData().getURL();
                return url.startsWith("jdbc:mydb:");
            } catch (SQLException e) { return false; }
        });

        Connection conn = TestDialectSupport.mockConn("jdbc:mydb://localhost:9999/test");
        DbDialectRegistry.Match m = WoodConfig.globalDialectRegistry.find(conn);

        assertFalse(m.isFallback);
        assertEquals(DbType.External, m.type, "外部方言应打上 DbType.External");
        assertSame(myDb, m.dialect);
    }

    @Test
    void externalRegistry_doesNotInterfereWithBuiltin() throws SQLException {
        WoodConfig.globalDialectRegistry.register(new DbH2Dialect(), c -> false);

        Connection conn = TestDialectSupport.mockConn("jdbc:mysql://localhost/test");
        DbDialectRegistry.Match m = WoodConfig.globalDialectRegistry.find(conn);

        assertEquals(DbType.MySQL, m.type, "内置 MySQL 匹配不应被外部干扰");
        assertEquals("DbMySQLDialect", m.dialect.typeName());
    }

    @Test
    void externalRegistry_appendedAfterBuiltin() throws SQLException {
        // 全局注册表里 builtin 的 matcher 先于外部注册的 matcher
        // 所以外部追加的 matcher 不会截胡内置的 MySQL URL
        DbDialect overrideDialect = new DbH2Dialect();
        WoodConfig.globalDialectRegistry.register(overrideDialect, c -> {
            try {
                return c.getMetaData().getURL().startsWith("jdbc:mysql:");
            } catch (SQLException e) { return false; }
        });

        Connection conn = TestDialectSupport.mockConn("jdbc:mysql://localhost/test");
        DbDialectRegistry.Match m = WoodConfig.globalDialectRegistry.find(conn);

        // 内置 MySQL matcher 先命中，外部追加的排在后面
        assertEquals(DbType.MySQL, m.type, "builtin matcher 应先于外部追加的");
        assertEquals("DbMySQLDialect", m.dialect.typeName());
    }

    @Test
    void instanceRegistry_prioritizesOverGlobal() throws SQLException {
        // 实例级 registry 优先于 global
        DbDialect overrideDialect = new DbH2Dialect();
        DbDialectRegistry instanceReg = new DbDialectRegistry();
        instanceReg.register(overrideDialect, c -> {
            try {
                return c.getMetaData().getURL().startsWith("jdbc:mysql:");
            } catch (SQLException e) { return false; }
        });

        Connection conn = TestDialectSupport.mockConn("jdbc:mysql://localhost/test");
        DbDialectRegistry.Match m = instanceReg.find(conn);

        assertEquals(DbType.External, m.type, "实例级 matcher 应胜出");
        assertSame(overrideDialect, m.dialect);
    }
}
