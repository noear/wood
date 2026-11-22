package org.noear.wood;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.wood.cache.LocalCache;
import org.noear.wood.dialect.DbDialect;
import org.noear.wood.dialect.DbH2Dialect;
import org.noear.wood.dialect.DbMySQLDialect;
import org.noear.wood.wrap.DbType;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DbTableQueryBase 单元测试（覆盖全部方法与分支）
 * <p>
 * SQL 构建类用例使用 mock 数据源（只编译不执行）；执行类用例使用 H2 内存库。
 */
class DbTableQueryBaseTest {
    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String TB = "tb_user";

    private DbContext db; //H2（已有 3 行：1/a/1, 2/b/1, 3/c/2）

    public static class UserModel {
        public Integer id;
        public String name;
        public Integer type;
    }

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:dtqb" + SEQ.incrementAndGet() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        db = new DbContext(fetchSizeTolerant(ds));
        db.setDialect(DbType.H2, new DbH2Dialect());

        db.sql("CREATE TABLE " + TB + " (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), type INT)").execute();
        db.sql("INSERT INTO " + TB + " (name, type) VALUES ('a',1),('b',1),('c',2)").execute();
    }

    @AfterEach
    void tearDown() {
        WoodConfig.isUsingSchemaPrefix = false;
        WoodConfig.isUpdateMustConditional = true;
        WoodConfig.isDeleteMustConditional = true;
    }

    //
    // 工具
    //

    /**
     * 只用于编译 SQL 的上下文（数据源为 mock）
     */
    private static DbContext mockCtx(DbType type, DbDialect dialect, String schema) {
        DbContext ctx = new DbContext(mock(DataSource.class), schema);
        ctx.setDialect(type, dialect);
        return ctx;
    }

    private static DbContext mockCtx() {
        return mockCtx(DbType.H2, new DbH2Dialect(), null);
    }

    /**
     * H2 不支持负的 fetchSize（框架用 Integer.MIN_VALUE 表示 MySQL 流式读取），这里忽略之
     */
    @SuppressWarnings("unchecked")
    private static <T> T fetchSizeTolerant(Class<T> type, T target) {
        return (T) Proxy.newProxyInstance(DbTableQueryBaseTest.class.getClassLoader(), new Class<?>[]{type}, (p, m, args) -> {
            if ("setFetchSize".equals(m.getName()) && (Integer) args[0] < 0) {
                return null;
            }

            Object rst;
            try {
                rst = m.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }

            if (rst instanceof PreparedStatement) {
                return fetchSizeTolerant(PreparedStatement.class, (PreparedStatement) rst);
            } else if (rst instanceof Connection) {
                return fetchSizeTolerant(Connection.class, (Connection) rst);
            } else {
                return rst;
            }
        });
    }

    private static DataSource fetchSizeTolerant(DataSource ds) {
        return fetchSizeTolerant(DataSource.class, ds);
    }

    private DbTableQuery tb() {
        return db.table(TB);
    }

    private long count() throws SQLException {
        return tb().selectCount();
    }

    private static List<DataItem> items(DataItem... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    /**
     * size() 与迭代内容不一致的集合（用于覆盖防御分支）
     */
    private static <T> Collection<T> sizedButEmpty() {
        return new AbstractCollection<T>() {
            @Override
            public Iterator<T> iterator() {
                return Collections.<T>emptyList().iterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };
    }

    //
    // 构建：log / build / table / with / from / join / append / on
    //

    @Test
    void log_and_compileTag() {
        DbTableQuery q = mockCtx().table("user").whereEq("id", 1);

        Command cmd = q.log(true).selectAsCmd("id");
        assertEquals(1, cmd.isLog);
        assertEquals("`user`", cmd.tag);

        cmd = q.log(false).selectAsCmd("id");
        assertEquals(-1, cmd.isLog);
    }

    @Test
    void build() {
        DbTableQuery q = mockCtx().table("user");

        assertTrue(q == q.build(t -> t.whereEq("id", 1)));
        assertEquals(" WHERE `id` = ? ", q._builder.toString());
    }

    @Test
    void table_rawAndDotAndNormal() {
        DbContext ctx = mockCtx();

        DbTableQuery q = ctx.table("#raw_name");
        assertEquals("raw_name", q._table);
        assertEquals("raw_name", q._table_raw);

        q = ctx.table("s2.user");
        assertEquals("s2.user", q._table);
        assertEquals("s2.user", q._table_raw);

        q = ctx.table("user");
        assertEquals("`user`", q._table);
        assertEquals("user", q._table_raw);
    }

    @Test
    void table_schemaPrefix() {
        WoodConfig.isUsingSchemaPrefix = true;

        assertEquals("s1.user", mockCtx(DbType.H2, new DbH2Dialect(), "s1").table("user")._table);
        assertEquals("`user`", mockCtx().table("user")._table); //schema 为 null
    }

    @Test
    void with_code() {
        DbTableQuery q = mockCtx().table("user")
                .with("a", "SELECT type FROM t WHERE id=?", 1)
                .with("b", "SELECT 2");

        assertEquals(" WITH `a` AS (SELECT type FROM t WHERE id=?) ,`b` AS (SELECT 2) ", q._builder_bef.toString());

        Command cmd = q.whereEq("id", 2).selectAsCmd("id");
        assertEquals(" WITH `a` AS (SELECT type FROM t WHERE id=?) ,`b` AS (SELECT 2) SELECT  `id` FROM `user` WHERE `id` = ? ", cmd.text);
        assertEquals(Arrays.asList(1, 2), cmd.paramS);
    }

    @Test
    void with_selectQ() {
        DbContext ctx = mockCtx();
        SelectQ sq = ctx.table("t").whereEq("id", 1).selectQ("type");

        DbTableQuery q = ctx.table("user").with("a", sq).with("b", sq);

        assertEquals(" WITH `a` AS (SELECT  `type` FROM `t` WHERE `id` = ? ) ,`b` AS (SELECT  `type` FROM `t` WHERE `id` = ? ) ", q._builder_bef.toString());
        assertEquals(Arrays.asList(1, 1), q._builder_bef.paramS);
    }

    @Test
    void from_append_on() {
        DbTableQuery q = mockCtx().table("user u")
                .from("t2")
                .append(" /*x?*/", 1)
                .on("u.id=t2.id")
                .onEq("u.id", "t2.id");

        assertEquals(" FROM `t2` /*x?*/ ON u.id=t2.id ON u.id=t2.id", q._builder.toString());
        assertEquals(Arrays.asList(1), q._builder.paramS);
    }

    @Test
    void join() {
        DbTableQuery q = mockCtx().table("user u")
                .innerJoin("#raw r")
                .leftJoin("b")
                .rightJoin("c x");

        assertEquals(" INNER JOIN raw r LEFT JOIN `b` RIGHT JOIN `c` x", q._builder.toString());
    }

    @Test
    void join_schemaPrefix() {
        WoodConfig.isUsingSchemaPrefix = true;

        DbTableQuery q = mockCtx(DbType.H2, new DbH2Dialect(), "s1").table("user").leftJoin("b");
        assertEquals(" LEFT JOIN s1.b", q._builder.toString());

        q = mockCtx().table("user").leftJoin("b"); //schema 为 null
        assertEquals(" LEFT JOIN `b`", q._builder.toString());
    }

    //
    // insert
    //

    @Test
    void insert() throws Exception {
        assertEquals(0, tb().insert((IDataItem) null));
        assertEquals(0, tb().insert(new DataItem()));

        long id = tb().insert(d -> d.set("name", "d").set("type", 3));
        assertEquals(4, id);
        assertEquals("d", tb().whereEq("id", 4).selectValue("name"));
    }

    @Test
    void insertAndResult() throws Exception {
        assertNull(tb().insertAndResult(null));
        assertNull(tb().insertAndResult(new DataItem()));

        tb().insertAndResult(new DataItem().set("name", "d"));
        assertEquals(4, count());
    }

    @Test
    void insertAsCmd() {
        DbTableQuery q = mockCtx().table("user");

        assertNull(q.insertAsCmd(null));
        assertNull(q.insertAsCmd(new DataItem()));

        Command cmd = q.insertAsCmd(new DataItem().set("name", "d").set("type", 3));
        assertEquals("INSERT INTO `user`  (`name`,`type`) VALUES(?,?)", cmd.text);
        assertEquals(Arrays.asList("d", 3), cmd.paramS);
    }

    @Test
    void insertAsCmd_sqlExpr() {
        StringBuilder longExpr = new StringBuilder("$");
        for (int i = 0; i < 100; i++) {
            longExpr.append("x");
        }

        DataItem data = new DataItem()
                .set("a", "$NOW()")           //表达式
                .set("b", "plain")            //不以 $ 开头
                .set("c", "$a b")             //含空格
                .set("d", longExpr.toString()) //超长
                .set("e", 1);                 //非字符串

        Command cmd = mockCtx().table("user").insertAsCmd(data);
        assertEquals("INSERT INTO `user`  (`a`,`b`,`c`,`d`,`e`) VALUES(NOW(),?,?,?,?)", cmd.text);
        assertEquals(Arrays.asList("plain", "$a b", longExpr.toString(), 1), cmd.paramS);

        //关闭表达式
        cmd = mockCtx().table("user").usingExpr(false).insertAsCmd(new DataItem().set("a", "$NOW()"));
        assertEquals("INSERT INTO `user`  (`a`) VALUES(?)", cmd.text);
        assertEquals(Arrays.asList("$NOW()"), cmd.paramS);
    }

    @Test
    void insertBy() throws Exception {
        assertEquals(0, tb().insertBy(null, "name"));
        assertEquals(0, tb().insertBy(new DataItem(), "name"));
        assertThrows(RuntimeException.class, () -> tb().insertBy(new DataItem().set("name", "a"), ","));

        //已存在
        assertEquals(0, tb().insertBy(new DataItem().set("name", "a").set("type", 9), "name"));
        assertEquals(3, count());

        //不存在
        assertEquals(4, tb().insertBy(new DataItem().set("name", "d").set("type", 9), "name,type"));
        assertEquals(4, count());
    }

    @Test
    void insertList() throws Exception {
        assertFalse(tb().insertList((List<DataItem>) null));
        assertFalse(tb().insertList(new ArrayList<DataItem>()));

        assertTrue(tb().insertList(items(
                new DataItem().set("name", "d").set("type", 3),
                new DataItem().set("name", "e").set("type", 3))));

        assertEquals(2, tb().whereEq("type", 3).selectCount());
    }

    @Test
    void insertList_builder() throws Exception {
        assertFalse(tb().insertList(new ArrayList<String>(), (v, d) -> d.set("name", v)));

        assertTrue(tb().insertList(Arrays.asList("d", "e", "f"), (v, d) -> d.set("name", v).set("type", 3)));
        assertEquals(3, tb().whereEq("type", 3).selectCount());
    }

    @Test
    void insertList_cols_invalid() throws Exception {
        DataItem cols = new DataItem().set("name", "d");

        assertFalse(tb().insertList(cols, (List<DataItem>) null));
        assertFalse(tb().insertList(cols, new ArrayList<DataItem>()));
        assertFalse(tb().insertList((IDataItem) null, items(cols)));
        assertFalse(tb().insertList(new DataItem(), items(cols)));
        assertEquals(3, count());
    }

    @Test
    void insertList_batchEmptyResult() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[0]);

        DbContext ctx = new DbContext(ds);
        ctx.setDialect(DbType.H2, new DbH2Dialect());

        assertFalse(ctx.table("user").insertList(items(new DataItem().set("name", "d"))));
    }

    @Test
    void insertList_compilationMode() throws Exception {
        db.setCompilationMode(true);
        assertFalse(tb().insertList(items(new DataItem().set("name", "d"))));

        db.setCompilationMode(false);
        assertEquals(3, count());
    }

    //
    // upsert / updateBy / update
    //

    @Test
    @SuppressWarnings("deprecation")
    void upsert() throws Exception {
        assertEquals(1, tb().upsert(new DataItem().set("name", "a").set("type", 9), "name"));
        assertEquals(9, tb().whereEq("name", "a").selectValue("type", 0));
    }

    @Test
    void upsertBy() throws Exception {
        assertEquals(0, tb().upsertBy(null, "name"));
        assertEquals(0, tb().upsertBy(new DataItem(), "name"));
        assertThrows(RuntimeException.class, () -> tb().upsertBy(new DataItem().set("name", "a"), ","));

        //已存在 -> 更新
        assertEquals(1, tb().upsertBy(new DataItem().set("name", "b").set("type", 8), "name"));
        assertEquals(8, tb().whereEq("name", "b").selectValue("type", 0));

        //不存在 -> 插入
        assertEquals(4, tb().upsertBy(new DataItem().set("name", "z").set("type", 7), "name"));
        assertEquals(4, count());
    }

    @Test
    void updateBy() throws Exception {
        assertThrows(RuntimeException.class, () -> tb().updateBy(new DataItem().set("id", 1), ","));

        assertEquals(1, tb().updateBy(new DataItem().set("id", 1).set("name", "aa"), "id"));
        assertEquals("aa", tb().whereEq("id", 1).selectValue("name"));
    }

    @Test
    void update() throws Exception {
        assertEquals(0, tb().whereEq("id", 1).update((IDataItem) null));
        assertEquals(0, tb().whereEq("id", 1).update(new DataItem()));

        assertEquals(2, tb().whereEq("type", 1).update(d -> d.set("name", "x")));
        assertEquals(2, tb().whereEq("name", "x").selectCount());
    }

    @Test
    void updateAsCmd() {
        DbTableQuery q = mockCtx().table("user").whereEq("id", 1);

        assertNull(q.updateAsCmd(null));
        assertNull(q.updateAsCmd(new DataItem()));

        Command cmd = q.updateAsCmd(new DataItem().set("name", "x"));
        assertEquals("UPDATE `user` SET `name`=? WHERE `id` = ? ", cmd.text);
        assertEquals(Arrays.asList("x", 1), cmd.paramS);

        //可复用
        assertEquals(cmd.text, q.updateAsCmd(new DataItem().set("name", "x")).text);
    }

    @Test
    void updateAsCmd_lackCondition() {
        DataItem data = new DataItem().set("name", "x");

        assertThrows(RuntimeException.class, () -> mockCtx().table("user").updateAsCmd(data));

        WoodConfig.isUpdateMustConditional = false;
        assertEquals("UPDATE `user` SET `name`=?", mockCtx().table("user").updateAsCmd(data).text);
    }

    @Test
    void updateAsCmd_limit() {
        DataItem data = new DataItem().set("name", "x");

        Command cmd = mockCtx(DbType.MySQL, new DbMySQLDialect(), null).table("user").whereEq("id", 1).top(5).updateAsCmd(data);
        assertEquals("UPDATE `user` SET `name`=? WHERE `id` = ?  LIMIT ?", cmd.text);
        assertEquals(Arrays.asList("x", 1, 5), cmd.paramS);

        cmd = mockCtx(DbType.MariaDB, new DbMySQLDialect(), null).table("user").whereEq("id", 1).top(5).updateAsCmd(data);
        assertEquals("UPDATE `user` SET `name`=? WHERE `id` = ?  LIMIT ?", cmd.text);

        //其它数据库不加 LIMIT
        cmd = mockCtx().table("user").whereEq("id", 1).top(5).updateAsCmd(data);
        assertEquals("UPDATE `user` SET `name`=? WHERE `id` = ? ", cmd.text);
        assertEquals(Arrays.asList("x", 1), cmd.paramS);
    }

    @Test
    void updateAsCmd_items() {
        DataItem data = new DataItem()
                .set("a", null)
                .set("b", "$NOW()")
                .set("c", "x")
                .set("d", 1);

        //默认不使用 null
        Command cmd = mockCtx().table("user").whereEq("id", 1).updateAsCmd(data);
        assertEquals("UPDATE `user` SET `b`=NOW(),`c`=?,`d`=? WHERE `id` = ? ", cmd.text);
        assertEquals(Arrays.asList("x", 1, 1), cmd.paramS);

        //使用 null
        cmd = mockCtx().table("user").usingNull(true).whereEq("id", 1).updateAsCmd(data);
        assertEquals("UPDATE `user` SET `a`=null,`b`=NOW(),`c`=?,`d`=? WHERE `id` = ? ", cmd.text);
    }

    //
    // updateList
    //

    @Test
    void updateList() throws Exception {
        assertNull(tb().updateList((List<DataItem>) null, "id"));
        assertNull(tb().updateList(new ArrayList<DataItem>(), "id"));

        int[] rst = tb().updateList(items(
                new DataItem().set("name", "a1").set("id", 1),
                new DataItem().set("name", "b1").set("id", 2)), "id");

        assertArrayEquals(new int[]{1, 1}, rst);
        assertEquals("a1", tb().whereEq("id", 1).selectValue("name"));
        assertEquals("b1", tb().whereEq("id", 2).selectValue("name"));
    }

    @Test
    void updateList_builder() throws Exception {
        assertNull(tb().updateList((Collection<String>) null, (v, d) -> d.set("name", v), "name"));
        assertNull(tb().updateList(new ArrayList<String>(), (v, d) -> d.set("name", v), "name"));
        assertNull(tb().updateList(DbTableQueryBaseTest.<String>sizedButEmpty(), (v, d) -> d.set("name", v), "name"));

        int[] rst = tb().updateList(Arrays.asList("a", "c"), (v, d) -> d.set("type", 5).set("name", v), "name");
        assertArrayEquals(new int[]{1, 1}, rst);
        assertEquals(2, tb().whereEq("type", 5).selectCount());
    }

    @Test
    void updateList_cols_invalid() throws Exception {
        DataItem cols = new DataItem().set("name", "x").set("id", 1);

        assertNull(tb().updateList(cols, (List<DataItem>) null, "id"));
        assertNull(tb().updateList(cols, new ArrayList<DataItem>(), "id"));
        assertNull(tb().updateList((IDataItem) null, items(cols), "id"));
        assertNull(tb().updateList(new DataItem(), items(cols), "id"));
        assertThrows(RuntimeException.class, () -> tb().updateList(cols, items(cols), ","));
    }

    //
    // delete
    //

    @Test
    void delete() throws Exception {
        DbTableQuery q = tb().whereEq("type", 1);

        assertEquals(2, q.delete());
        assertEquals(0, q.delete());
        assertEquals(1, count());
    }

    @Test
    void deleteAsCmd() {
        Command cmd = mockCtx().table("user").whereEq("id", 1).deleteAsCmd();
        assertEquals("DELETE  FROM `user`  WHERE `id` = ? ", cmd.text);
        assertEquals(Arrays.asList(1), cmd.paramS);

        //已有 FROM
        cmd = mockCtx().table("u").from("user u").whereEq("u.id", 1).deleteAsCmd();
        assertEquals("DELETE `u`  FROM `user` u WHERE u.id = ? ", cmd.text);
    }

    @Test
    void deleteAsCmd_lackCondition() {
        DbTableQuery q = mockCtx().table("user");
        assertThrows(RuntimeException.class, q::deleteAsCmd);
        assertEquals("", q._builder.toString());

        WoodConfig.isDeleteMustConditional = false;
        assertEquals("DELETE  FROM `user` ", mockCtx().table("user").deleteAsCmd().text);
    }

    @Test
    void deleteAsCmd_limit() {
        Command cmd = mockCtx(DbType.MySQL, new DbMySQLDialect(), null).table("user").whereEq("id", 1).limit(5).deleteAsCmd();
        assertEquals("DELETE  FROM `user`  WHERE `id` = ?  LIMIT ?", cmd.text);
        assertEquals(Arrays.asList(1, 5), cmd.paramS);

        cmd = mockCtx(DbType.MariaDB, new DbMySQLDialect(), null).table("user").whereEq("id", 1).limit(5).deleteAsCmd();
        assertEquals("DELETE  FROM `user`  WHERE `id` = ?  LIMIT ?", cmd.text);

        //其它数据库不加 LIMIT
        cmd = mockCtx().table("user").whereEq("id", 1).limit(5).deleteAsCmd();
        assertEquals("DELETE  FROM `user`  WHERE `id` = ? ", cmd.text);
    }

    //
    // select 构建：limit / paging / top / orderBy / hint / selectQ
    //

    @Test
    void selectAsCmd_paging() {
        DbContext ctx = mockCtx();

        Command cmd = ctx.table("user").whereEq("id", 1).selectAsCmd("id,name n");
        assertEquals("SELECT  `id`,`name` n FROM `user` WHERE `id` = ? ", cmd.text);
        assertEquals(Arrays.asList(1), cmd.paramS);

        cmd = ctx.table("user").whereEq("id", 1).limit(10).selectAsCmd("id");
        assertEquals("SELECT  `id` FROM `user` WHERE `id` = ?  LIMIT ?", cmd.text);
        assertEquals(Arrays.asList(1, 10), cmd.paramS);

        cmd = ctx.table("user").whereEq("id", 1).top(10).selectAsCmd("id");
        assertEquals("SELECT  `id` FROM `user` WHERE `id` = ?  LIMIT ?", cmd.text);

        cmd = ctx.table("user").whereEq("id", 1).limit(20, 10).selectAsCmd("id");
        assertEquals("SELECT  `id` FROM `user` WHERE `id` = ?  LIMIT ?,?", cmd.text);
        assertEquals(Arrays.asList(1, 20, 10), cmd.paramS);

        cmd = ctx.table("user").whereEq("id", 1).paging(20, 10).selectAsCmd("id");
        assertEquals("SELECT  `id` FROM `user` WHERE `id` = ?  LIMIT ?,?", cmd.text);
    }

    @Test
    void selectAsCmd_orderBy_hint() {
        Command cmd = mockCtx().table("user").whereEq("id", 1).orderByDesc("id").selectAsCmd("id");
        assertEquals("SELECT  `id` FROM `user` WHERE `id` = ?  ORDER BY `id` DESC ", cmd.text);

        cmd = mockCtx().table("user").hint("/*+ x */ ").selectAsCmd("id");
        assertEquals("/*+ x */ SELECT  `id` FROM `user`", cmd.text);
    }

    @Test
    void selectQ() {
        DbTableQuery q = mockCtx().table("user").whereEq("id", 1);

        SelectQ sq = q.selectQ("id");
        assertEquals("SELECT  `id` FROM `user` WHERE `id` = ? ", sq.toString());
        assertEquals(Arrays.asList(1), sq.paramS);

        //查询对象被还原
        assertEquals(" WHERE `id` = ? ", q._builder.toString());
        assertEquals(Arrays.asList(1), q._builder.paramS);
    }

    //
    // select 执行
    //

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedMethods() throws Exception {
        assertTrue(tb().whereEq("id", 1).exists());
        assertEquals(3, tb().count());
        assertEquals(3, tb().count("id"));
        assertEquals("a", tb().whereEq("id", 1).select("name").getValue());
    }

    @Test
    void selectExists() throws Exception {
        assertTrue(tb().whereEq("id", 1).selectExists());
        assertFalse(tb().whereEq("id", 999).selectExists());

        LocalCache cache = new LocalCache();
        assertTrue(tb().whereEq("id", 1).caching(cache).selectExists());
        assertFalse(tb().whereEq("id", 999).caching(cache).selectExists());
    }

    @Test
    void selectExists_keepLimit() throws Exception {
        DbTableQuery q = tb().top(2);
        assertTrue(q.selectExists());
        assertEquals(2, q.selectList("*", UserModel.class).size());
    }

    @Test
    void selectCount() throws Exception {
        assertEquals(3, tb().selectCount());
        assertEquals(3, tb().selectCount("id"));
        assertEquals(2, tb().selectCount("COUNT(DISTINCT type)"));

        //count 时忽略分页和排序，结束后恢复
        DbTableQuery q = tb().limit(0, 2).orderByDesc("id");
        assertEquals(3, q.selectCount());
        List<UserModel> list = q.selectList("*", UserModel.class);
        assertEquals(2, list.size());
        assertEquals(3, list.get(0).id);
    }

    @Test
    void selectCount_group() throws Exception {
        DbTableQuery q = tb().whereGt("id", 0).groupBy("type");

        assertEquals(2, q.selectCount());
        assertEquals(2, q.selectCount());
        assertEquals(2, q.selectMapList("type").size());
    }

    @Test
    void selectValue_variate() throws Exception {
        assertEquals(3L, tb().selectVariate("COUNT(*)").longValue(0L));
        assertEquals("a", tb().whereEq("id", 1).selectValue("name"));
        assertEquals("def", tb().whereEq("id", 999).selectValue("name", "def"));
    }

    @Test
    void selectItem_list_array() throws Exception {
        UserModel m = tb().whereEq("id", 2).selectItem("*", UserModel.class);
        assertEquals("b", m.name);
        assertEquals(1, m.type);

        assertEquals(3, tb().selectList("*", UserModel.class).size());

        List<Integer> ids = tb().orderByAsc("id").selectArray("id");
        assertEquals(Arrays.asList(1, 2, 3), ids);
    }

    @Test
    void selectData() throws Exception {
        DataItem item = tb().whereEq("id", 3).selectDataItem("*");
        assertEquals("c", item.getString("name"));

        DataList list = tb().selectDataList("*");
        assertEquals(3, list.getRowCount());

        Map<String, Object> map = tb().whereEq("id", 1).selectMap("name");
        assertEquals("a", map.get("name"));

        assertEquals(3, tb().selectMapList("*").size());
    }

    @Test
    void selectPage() throws Exception {
        IPage<UserModel> p1 = tb().limit(0, 2).selectPage("*", UserModel.class);
        assertEquals(3, p1.getTotal());
        assertEquals(2, p1.getList().size());
        assertEquals(2, p1.getSize());

        IPage<DataItem> p2 = tb().limit(2, 2).selectDataPage("*");
        assertEquals(3, p2.getTotal());
        assertEquals(1, p2.getList().size());

        IPage<Map<String, Object>> p3 = tb().limit(0, 2).selectMapPage("*");
        assertEquals(3, p3.getTotal());
        assertEquals(2, p3.getList().size());
    }

    @Test
    void selectReader() throws Exception {
        try (IDataReader<UserModel> reader = tb().orderByAsc("id").selectReader("*", 10, UserModel.class)) {
            assertEquals("a", reader.next().name);
        }

        try (IDataReader<UserModel> reader = tb().orderByAsc("id").selectReader("*", UserModel.class)) {
            assertEquals("a", reader.next().name);
        }

        try (DataReader reader = tb().orderByAsc("id").selectDataReader("*", 10)) {
            assertEquals("a", reader.next().getString("name"));
        }

        try (DataReader reader = tb().orderByAsc("id").selectDataReader("*")) {
            assertEquals("a", reader.next().getString("name"));
        }
    }

    //
    // usingNull / usingExpr / 缓存
    //

    @Test
    void usingNull_usingExpr() {
        DbTableQuery q = mockCtx().table("user");

        assertFalse(q.usingNull());
        assertTrue(q.usingNull(true).usingNull());

        assertTrue(q.usingExpr());
        assertFalse(q.usingExpr(false).usingExpr());
    }

    @Test
    void caching() throws Exception {
        LocalCache cache = new LocalCache();

        DbTableQuery q = tb().whereEq("id", 1).caching(cache).usingCache(60).cacheTag("user");
        assertNotNull(q._cache);
        assertEquals("a", q.selectValue("name"));

        //修改数据后，缓存仍命中旧值
        tb().whereEq("id", 1).update(new DataItem().set("name", "x"));
        assertEquals("a", tb().whereEq("id", 1).caching(cache).usingCache(60).selectValue("name"));

        //不使用缓存
        assertEquals("x", tb().whereEq("id", 1).caching(cache).usingCache(false).selectValue("name"));
    }
}
