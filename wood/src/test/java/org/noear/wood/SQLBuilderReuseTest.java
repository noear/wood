package org.noear.wood;

import org.junit.jupiter.api.Test;
import org.noear.wood.dialect.DbH2Dialect;
import org.noear.wood.wrap.DbType;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 同一个查询对象多次构建时，SQL 与参数应保持稳定（不累积）
 *
 * @see <a href="https://gitee.com/noear/wood/issues/IKTJIT">IKTJIT</a>
 */
class SQLBuilderReuseTest {

    private DbContext newContext() {
        DbContext context = new DbContext(mock(DataSource.class));
        context.setDialect(DbType.H2, new DbH2Dialect());
        return context;
    }

    @Test
    void backupRestore_repeated_notAccumulate() {
        SQLBuilder sb = new SQLBuilder();
        sb.append(" WHERE id = ?", 1);

        for (int i = 0; i < 3; i++) {
            sb.backup();
            sb.insert("SELECT * FROM user");
            sb.clear();
            sb.restore();

            assertEquals(" WHERE id = ?", sb.toString());
            assertEquals(Arrays.asList(1), sb.paramS);
        }
    }

    @Test
    void selectAsCmd_repeated_sameSql() {
        DbTableQuery q = newContext().table("user").whereEq("id", 1);

        Command cmd1 = q.selectAsCmd("id");
        Command cmd2 = q.selectAsCmd("id");
        Command cmd3 = q.selectAsCmd("id");

        assertEquals(cmd1.text, cmd2.text);
        assertEquals(cmd1.text, cmd3.text);
        assertEquals(Arrays.asList(1), cmd3.paramS);
    }

    @Test
    void selectQ_repeated_sameSql() {
        DbTableQuery q = newContext().table("user").whereEq("id", 1);

        SelectQ s1 = q.selectQ("id");
        SelectQ s2 = q.selectQ("id");
        Command cmd = q.selectAsCmd("id");

        assertEquals(s1.toString(), s2.toString());
        assertEquals(s1.paramS, s2.paramS);
        assertEquals(s1.toString(), cmd.text);
        assertEquals(Arrays.asList(1), cmd.paramS);
    }

    @Test
    void deleteAsCmd_repeated_sameSql() {
        DbTableQuery q = newContext().table("user").whereEq("id", 1);

        Command cmd1 = q.deleteAsCmd();
        Command cmd2 = q.deleteAsCmd();

        assertTrue(cmd1.text.startsWith("DELETE "));
        assertEquals(cmd1.text, cmd2.text);
        assertEquals(Arrays.asList(1), cmd2.paramS);
    }

    @Test
    void deleteAsCmd_thenSelect_keepCondition() {
        DbTableQuery q = newContext().table("user").whereEq("id", 1);
        Command expected = newContext().table("user").whereEq("id", 1).selectAsCmd("id");

        q.deleteAsCmd();
        Command cmd = q.selectAsCmd("id");

        assertEquals(expected.text, cmd.text);
        assertEquals(Arrays.asList(1), cmd.paramS);
    }

    @Test
    void deleteAsCmd_lackCondition_notPollute() {
        DbTableQuery q = newContext().table("user");

        assertThrows(RuntimeException.class, q::deleteAsCmd);

        //异常后查询对象不应残留 DELETE 语句
        assertEquals("", q._builder.toString());
        assertThrows(RuntimeException.class, q::deleteAsCmd);
    }

    @Test
    void insertList_failed_keepCondition() {
        DbTableQuery q = newContext().table("user").whereEq("id", 1);
        Command expected = newContext().table("user").whereEq("id", 1).selectAsCmd("id");

        //mock 数据源拿不到连接，executeBatch 必然失败
        assertThrows(Exception.class, () -> q.insertList(Arrays.asList(new DataItem().set("name", "a"))));

        Command cmd = q.selectAsCmd("id");
        assertEquals(expected.text, cmd.text);
        assertEquals(Arrays.asList(1), cmd.paramS);
    }

    @Test
    void updateList_failed_keepCondition() {
        DbTableQuery q = newContext().table("user").whereEq("id", 1);
        Command expected = newContext().table("user").whereEq("id", 1).selectAsCmd("id");

        //mock 数据源拿不到连接，executeBatch 必然失败
        assertThrows(Exception.class, () -> q.updateList(Arrays.asList(new DataItem().set("id", 2).set("name", "a")), "id"));

        Command cmd = q.selectAsCmd("id");
        assertEquals(expected.text, cmd.text);
        assertEquals(Arrays.asList(1), cmd.paramS);
    }
}
