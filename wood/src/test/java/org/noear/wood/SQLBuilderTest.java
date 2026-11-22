package org.noear.wood;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * SQLBuilder 单元测试（覆盖全部方法与分支）
 */
class SQLBuilderTest {

    private static SQLBuilder of(String code, Object... args) {
        return new SQLBuilder().append(code, args);
    }

    @Test
    void indexOf_substring_length_toString() {
        SQLBuilder sb = of("SELECT * FROM user");

        assertEquals(8, sb.indexOf(" FROM "));
        assertEquals(-1, sb.indexOf(" WHERE "));
        assertEquals("FROM user", sb.substring(9));
        assertEquals("SELECT", sb.substring(0, 6));
        assertEquals(18, sb.length());
        assertEquals("SELECT * FROM user", sb.toString());
    }

    @Test
    void clear() {
        SQLBuilder sb = of("id=?", 1);
        sb.clear();

        assertEquals("", sb.toString());
        assertEquals(0, sb.paramS.size());
    }

    @Test
    void backup_restore() {
        SQLBuilder sb = of(" WHERE id=?", 1);
        sb.backup();

        sb.clear();
        sb.append("SELECT x WHERE a=? AND b=?", 2, 3);
        sb.restore();

        assertEquals(" WHERE id=?", sb.toString());
        assertEquals(Arrays.asList(1), sb.paramS);

        //还原后备份已清空，再次还原得到空
        sb.restore();
        assertEquals("", sb.toString());
        assertEquals(0, sb.paramS.size());
    }

    @Test
    void insertBySymbol_symbolNotFound_append() {
        SQLBuilder sb = of("UPDATE user", (Object[]) null);

        assertSame(sb, sb.insertBySymbol(" WHERE ", " SET a=?", 1));
        assertEquals("UPDATE user SET a=?", sb.toString());
        assertEquals(Arrays.asList(1), sb.paramS);
    }

    @Test
    void insertBySymbol_symbolFound_insertParamsAtRightIndex() {
        SQLBuilder sb = of("UPDATE t(?) WHERE id=?", "x", 9);

        sb.insertBySymbol(" WHERE ", " SET a=?,b=?", 1, 2);

        assertEquals("UPDATE t(?) SET a=?,b=? WHERE id=?", sb.toString());
        assertEquals(Arrays.asList("x", 1, 2, 9), sb.paramS);
    }

    @Test
    void insert_codeAndPartAndOffset() {
        SQLBuilder sb = of(" WHERE id=?", 1);

        assertSame(sb, sb.insert("SELECT ? FROM t", 0));
        assertEquals("SELECT ? FROM t WHERE id=?", sb.toString());
        assertEquals(Arrays.asList(0, 1), sb.paramS);

        assertSame(sb, sb.insert(of("/*h?*/ ", "h")));
        assertEquals("/*h?*/ SELECT ? FROM t WHERE id=?", sb.toString());
        assertEquals(Arrays.asList("h", 0, 1), sb.paramS);

        assertSame(sb, sb.insert(0, "X"));
        assertEquals("X/*h?*/ SELECT ? FROM t WHERE id=?", sb.toString());
        assertEquals(3, sb.paramS.size());
    }

    @Test
    void append_objectAndPart() {
        SQLBuilder sb = new SQLBuilder();

        assertSame(sb, sb.append((Object) 12));
        assertSame(sb, sb.append(of(" a=?", "v")));

        assertEquals("12 a=?", sb.toString());
        assertEquals(Arrays.asList("v"), sb.paramS);
    }

    @Test
    void remove_removeLast() {
        SQLBuilder sb = of("abcdef,");

        assertSame(sb, sb.removeLast());
        assertEquals("abcdef", sb.toString());

        assertSame(sb, sb.remove(1, 2));
        assertEquals("adef", sb.toString());
    }

    @Test
    void trimEnd() {
        //空字符串：不处理（也不 trim）
        SQLBuilder sb = of(" a, ");
        assertSame(sb, sb.trimEnd(""));
        assertEquals(" a, ", sb.toString());

        //循环去除尾部
        sb = of(" a,b,,, ");
        sb.trimEnd(",");
        assertEquals("a,b", sb.toString());

        //没有可去除的
        sb = of("a,b");
        sb.trimEnd(",");
        assertEquals("a,b", sb.toString());

        //全部可去除
        sb = of(",,,");
        sb.trimEnd(",");
        assertEquals("", sb.toString());

        //空内容
        sb = of("");
        sb.trimEnd(",");
        assertEquals("", sb.toString());

        //内容比 str 短
        sb = of("a");
        sb.trimEnd("ab");
        assertEquals("a", sb.toString());
    }

    @Test
    void trimStart() {
        //空字符串：不处理
        SQLBuilder sb = of(" ,a ");
        assertSame(sb, sb.trimStart(""));
        assertEquals(" ,a ", sb.toString());

        //循环去除头部
        sb = of(" ,,a,b ");
        sb.trimStart(",");
        assertEquals("a,b", sb.toString());

        //全部去除后为空
        sb = of(",,");
        sb.trimStart(",");
        assertEquals("", sb.toString());
    }

    @Test
    void addPrefix() {
        SQLBuilder sb = new SQLBuilder();
        assertSame(sb, sb.addPrefix("(", false)); //空时不加
        assertEquals("", sb.toString());

        assertSame(sb, sb.addPrefix("(")); //默认允许空
        assertEquals("(", sb.toString());

        sb.addPrefix("[", false); //非空时加
        assertEquals("[(", sb.toString());
    }

    @Test
    void addSuffix() {
        SQLBuilder sb = new SQLBuilder();
        assertSame(sb, sb.addSuffix(")", false)); //空时不加
        assertEquals("", sb.toString());

        assertSame(sb, sb.addSuffix(")")); //默认允许空
        assertEquals(")", sb.toString());

        sb.addSuffix("]", false); //非空时加
        assertEquals(")]", sb.toString());
    }

    @Test
    void partBuilder_noArgs() {
        assertEquals("a=1", of("a=1", (Object[]) null).toString());
        assertEquals("a=1", of("a=1").toString());
    }

    @Test
    void partBuilder_plainArgs() {
        SQLBuilder sb = of("a=? AND b=?", 1, "x");

        assertEquals("a=? AND b=?", sb.toString());
        assertEquals(Arrays.asList(1, "x"), sb.paramS);
    }

    @Test
    void partBuilder_iterableArgs() {
        SQLBuilder sb = of("id IN (?...) AND type=?", Arrays.asList(1, 2, 3), 5);

        assertEquals("id IN (?,?,?) AND type=?", sb.toString());
        assertEquals(Arrays.asList(1, 2, 3, 5), sb.paramS);
    }

    @Test
    void partBuilder_emptyIterable_null() {
        SQLBuilder sb = of("id IN (?...)", new ArrayList<>());

        assertEquals("id IN (null)", sb.toString());
        assertEquals(0, sb.paramS.size());
    }

    @Test
    void partBuilder_dbQueryArgs() {
        DbContext ctx = new DbContext(mock(DataSource.class));

        DbQuery withParams = new DbQuery(ctx).sql(of("SELECT id FROM t WHERE a=?", 7));
        SQLBuilder sb = of("id IN (?...)", withParams);
        assertEquals("id IN (SELECT id FROM t WHERE a=?)", sb.toString());
        assertEquals(Collections.singletonList(7), sb.paramS);

        DbQuery noParams = new DbQuery(ctx).sql(of("SELECT id FROM t"));
        sb = of("id IN (?...)", noParams);
        assertEquals("id IN (SELECT id FROM t)", sb.toString());
        assertEquals(0, sb.paramS.size());
    }
}
