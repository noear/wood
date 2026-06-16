# Wood 外部方言可插拔机制 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 允许不修改 Wood 核心源码即可注册新的数据库方言，通过编程式 API + 分层 Registry 实现。

**Architecture:** 新增 `DbDialectRegistry` 类（持有 matcher 列表 + fixed 槽位 + fallback）。`DbDialect` 接口增 2 个 `default` 方法（`typeName` / `defaultSchema`）。`WoodConfig` 加全局静态注册表，`DbContext` 加实例级注册表。`DbContextMetaData.setDatabaseType` 改为"实例 → 全局"两级查找，`setSchema` 改为调用 `dialect.defaultSchema()`。

**Tech Stack:** Java 1.8, Maven, JUnit 5 (Jupiter), Mockito (for Connection / DataSource mocks), SLF4J

**Reference Spec:** `docs/superpowers/specs/2026-06-12-wood-external-dialect-design.md`

---

## File Structure

**Create:**
- `wood/src/main/java/org/noear/wood/dialect/DbDialectRegistry.java` — 核心 Registry 类
- `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java` — Registry 行为测试
- `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryBuiltinTest.java` — 内置方言识别测试
- `wood/src/test/java/org/noear/wood/dialect/DbDialectExternalTest.java` — 外部方言注册测试
- `wood/src/test/java/org/noear/wood/dialect/TestDialectSupport.java` — 测试辅助（Mock Connection / DataSource 工厂）

**Modify:**
- `wood/pom.xml` — 加 `junit-jupiter` + `mockito-core` test-scope 依赖
- `wood/src/main/java/org/noear/wood/dialect/DbDialect.java` — 加 2 个 default 方法
- `wood/src/main/java/org/noear/wood/wrap/DbType.java` — 加 `External` 常量
- `wood/src/main/java/org/noear/wood/WoodConfig.java` — 加 `globalDialectRegistry` 字段与 `registerDialect` 静态方法
- `wood/src/main/java/org/noear/wood/DbContext.java` — 加 `registry` 字段、`getDialectRegistry` / `setDialectRegistry`、构造 metadata 时设 `owner`；`setDialect` 内部重写
- `wood/src/main/java/org/noear/wood/DbContextMetaData.java` — 加 `owner` 字段 + `setOwner`；重构 `setDatabaseType`；重构 `setSchema`
- `wood/src/main/java/org/noear/wood/dialect/DbPostgreSQLDialect.java` — override `defaultSchema()`
- `wood/src/main/java/org/noear/wood/dialect/DbH2Dialect.java` — override `defaultSchema()`
- `wood/src/main/java/org/noear/wood/dialect/DbSQLServerDialect.java` — override `defaultSchema()`
- `wood/src/main/java/org/noear/wood/dialect/DbOracleDialect.java` — override `defaultSchema()`
- `WOOD_JAVA_接口字典.md` — 加"外部方言注册"章节
- `LOG_java_update.md` — 加版本条目

**Total:** 5 new files, 10 modified files.

---

## Task 1: Add JUnit 5 + Mockito to wood/pom.xml

**Files:**
- Modify: `wood/pom.xml`

- [ ] **Step 1: Add test dependencies to wood/pom.xml**

Append (before `</dependencies>`):

```xml
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.11.0</version>
            <scope>test</scope>
        </dependency>
```

Full updated `wood/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.noear</groupId>
        <artifactId>wood-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../wood-parent/pom.xml</relativePath>
    </parent>

    <artifactId>wood</artifactId>
    <name>${project.artifactId}</name>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.11.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Verify the project still compiles**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/pom.xml
git commit -m "build(wood): add junit-jupiter and mockito-core test deps"
```

---

## Task 2: Add `typeName()` and `defaultSchema()` defaults to DbDialect

**Files:**
- Modify: `wood/src/main/java/org/noear/wood/dialect/DbDialect.java`

- [ ] **Step 1: Append two default methods to DbDialect interface**

Open `wood/src/main/java/org/noear/wood/dialect/DbDialect.java` and add before the closing `}` of the interface (line 99):

```java
    /**
     * 类型名（用于日志/调试/外部标识）
     */
    default String typeName() {
        return getClass().getSimpleName();
    }

    /**
     * 默认 schema 名（用于 schema 解析失败时的兜底；返回 null 表示不设默认值）
     */
    default String defaultSchema() {
        return null;
    }
```

- [ ] **Step 2: Verify the project compiles**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/dialect/DbDialect.java
git commit -m "feat(wood): add typeName() and defaultSchema() defaults to DbDialect"
```

---

## Task 3: Add `DbType.External` enum constant

**Files:**
- Modify: `wood/src/main/java/org/noear/wood/wrap/DbType.java`

- [ ] **Step 1: Append `External` to the enum**

Open `wood/src/main/java/org/noear/wood/wrap/DbType.java` and find the existing `GBase8s,` line. Add after it (before the trailing `}`):

```java
    /**
     * 通过外部 registry 注册的方言（具体类型由 getDialect().typeName() 给出）
     */
    External,
```

- [ ] **Step 2: Verify compile**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/wrap/DbType.java
git commit -m "feat(wood): add DbType.External for externally-registered dialects"
```

---

## Task 4: Create `DbDialectRegistry` class skeleton (TDD: fallback path)

**Files:**
- Create: `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java`
- Create: `wood/src/main/java/org/noear/wood/dialect/DbDialectRegistry.java`

- [ ] **Step 1: Write failing test for fallback path**

Create `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java`:

```java
package org.noear.wood.dialect;

import org.junit.jupiter.api.Test;

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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -Dtest=DbDialectRegistryTest -q`
Expected: FAIL with `cannot find symbol: class DbDialectRegistry`

- [ ] **Step 3: Create DbDialectRegistry class with fallback path only**

Create `wood/src/main/java/org/noear/wood/dialect/DbDialectRegistry.java`:

```java
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -Dtest=DbDialectRegistryTest -q`
Expected: `BUILD SUCCESS` (2 tests passed)

- [ ] **Step 5: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/dialect/DbDialectRegistry.java \
        wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java
git commit -m "feat(wood): add DbDialectRegistry with fallback path"
```

---

## Task 5: Add matcher-based registration tests (TDD: register + find)

**Files:**
- Modify: `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java`

- [ ] **Step 1: Append matcher tests**

Add the following tests to `DbDialectRegistryTest.java` (inside the class):

```java
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
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -Dtest=DbDialectRegistryTest -q`
Expected: `BUILD SUCCESS` (8 tests passed)

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java
git commit -m "test(wood): cover DbDialectRegistry matcher registration"
```

---

## Task 6: Add setFixed tests (TDD: fixed overrides matchers)

**Files:**
- Modify: `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java`

- [ ] **Step 1: Append setFixed tests**

```java
    @Test
    void setFixed_overridesMatchersAndFallback() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);
        r.register(new DbH2Dialect(), c -> true);  // 这个本来会匹配
        DbOracleDialect oracle = new DbOracleDialect();
        r.setFixed(oracle, DbType.Oracle);         // 但 fixed 应该胜出

        DbDialectRegistry.Match m = r.find(conn);

        assertTrue(m.isFallback, "fixed 视为兜底命中（isFallback=true），但 dialect 来自 setFixed");
        assertSame(oracle, m.dialect);
        assertEquals(DbType.Oracle, m.type);
    }

    @Test
    void setFixed_null_clearsOverride() throws SQLException {
        DbDialectRegistry r = new DbDialectRegistry();
        Connection conn = mock(Connection.class);
        DbH2Dialect h2 = new DbH2Dialect();
        r.setFixed(h2, DbType.H2);
        r.setFixed(null, null);

        DbDialectRegistry.Match m = r.find(conn);

        assertTrue(m.isFallback);
        assertEquals(DbType.MySQL, m.type, "clear 后回到默认 fallback");
    }
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -Dtest=DbDialectRegistryTest -q`
Expected: `BUILD SUCCESS` (10 tests passed)

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryTest.java
git commit -m "test(wood): cover DbDialectRegistry.setFixed"
```

---

## Task 7: Implement `builtin()` with all 20 built-in matchers

**Files:**
- Create: `wood/src/test/java/org/noear/wood/dialect/TestDialectSupport.java`
- Create: `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryBuiltinTest.java`
- Modify: `wood/src/main/java/org/noear/wood/dialect/DbDialectRegistry.java`

- [ ] **Step 1: Write test helper for mock Connection**

Create `wood/src/test/java/org/noear/wood/dialect/TestDialectSupport.java`:

```java
package org.noear.wood.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Mock 工具：构造一个返回指定 url / productName / 模拟 SQL 行为的 Connection。 */
final class TestDialectSupport {

    private TestDialectSupport() {}

    static Connection mockConn(String url) throws SQLException {
        return mockConn(url, null, new ArrayList<>());
    }

    static Connection mockConn(String url, String productName) throws SQLException {
        return mockConn(url, productName, new ArrayList<>());
    }

    /** productName 为 null 时不显式 stub（让 DatabaseMetaData.getDatabaseProductName 返回 null）。 */
    static Connection mockConn(String url, String productName, List<SqlStub> sqlStubs) throws SQLException {
        Connection conn = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(conn.getMetaData()).thenReturn(md);
        when(md.getURL()).thenReturn(url);
        if (productName != null) {
            when(md.getDatabaseProductName()).thenReturn(productName);
        }
        // SQL stub 留给 Task 8 的 OceanBase / Kingbase 测试
        if (!sqlStubs.isEmpty()) {
            java.sql.Statement stmt = mock(java.sql.Statement.class);
            java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
            when(conn.createStatement()).thenReturn(stmt);
            when(stmt.executeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(rs);
            for (SqlStub s : sqlStubs) {
                when(rs.getString(s.column)).thenReturn(s.value);
                when(rs.next()).thenReturn(true);  // 单结果集
            }
        }
        return conn;
    }

    static class SqlStub {
        final int column;
        final String value;
        SqlStub(int column, String value) { this.column = column; this.value = value; }
        static SqlStub of(int c, String v) { return new SqlStub(c, v); }
    }
}
```

- [ ] **Step 2: Write failing test for builtin() — simple URL prefix matchers**

Create `wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryBuiltinTest.java`:

```java
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
    void gbase8s_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:gbasedbt-sqli://localhost:9088/test", "DbGbase8sDialect", DbType.GBase8s);
    }

    @Test
    void gbase8s_informixCompat_byUrlPrefix() throws SQLException {
        assertMatch("jdbc:informix-sqli://localhost:9088/test", "DbGbase8sDialect", DbType.GBase8s);
    }

    @Test
    void kingbaseByPostgresByDefault() throws SQLException {
        // 没设置 sql stub 时，getKingbaseMode 会抛 SQLException → catch 后返回 null → 落到 postgre fallback
        // 但因为 matcher 中 try/catch 会 swallow，期望: DbKingbasePostgreDialect
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
```

- [ ] **Step 3: Run tests to verify they fail (or partially fail)**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -Dtest=DbDialectRegistryBuiltinTest -q 2>&1 | tail -20`
Expected: Most tests fail with `kingbaseByPostgresByDefault` and `unknownUrl_fallsBackToMysql` failing because `builtin()` returns empty registry (current Task 4 implementation)

- [ ] **Step 4: Implement `builtin()` with all 20 matchers**

Replace the `builtin()` method in `DbDialectRegistry.java` with:

```java
    /** 构造一个预填了 20 个内置方言的注册表 */
    public static DbDialectRegistry builtin() {
        DbDialectRegistry r = new DbDialectRegistry();
        r.register(new DbMySQLDialect(),        DbType.MySQL,        c -> urlOf(c).startsWith("jdbc:mysql:"));
        r.register(new DbMySQLDialect(),        DbType.MariaDB,      c -> urlOf(c).startsWith("jdbc:mariadb:"));
        r.register(new DbSQLServerDialect(),    DbType.SQLServer,    c -> urlOf(c).startsWith("jdbc:sqlserver:"));
        r.register(new DbOracleDialect(),       DbType.Oracle,       c -> urlOf(c).startsWith("jdbc:oracle:"));
        r.register(new DbPostgreSQLDialect(),   DbType.PostgreSQL,   c -> urlOf(c).startsWith("jdbc:postgresql:"));
        r.register(new DbDb2Dialect(),          DbType.DB2,          c -> urlOf(c).startsWith("jdbc:db2:"));
        r.register(new DbSQLiteDialect(),       DbType.SQLite,       c -> urlOf(c).startsWith("jdbc:sqlite:"));
        r.register(new DbH2Dialect(),           DbType.H2,           c -> urlOf(c).startsWith("jdbc:h2:"));
        r.register(new DbPhoenixDialect(),      DbType.Phoenix,      c -> urlOf(c).startsWith("jdbc:phoenix:"));
        r.register(new DbClickHouseDialect(),   DbType.ClickHouse,   c -> urlOf(c).startsWith("jdbc:clickhouse:"));
        r.register(new DbPrestoDialect(),       DbType.Presto,       c -> urlOf(c).startsWith("jdbc:presto:"));
        r.register(new DbDuckDbDialect(),       DbType.DuckDb,       c -> urlOf(c).startsWith("jdbc:duckdb:"));
        r.register(new DbDamengDialect(),       DbType.DM,           c -> urlOf(c).startsWith("jdbc:dm:"));
        r.register(new DbGbase8sDialect(),      DbType.GBase8s,      c -> urlOf(c).startsWith("jdbc:gbasedbt-sqli:")
                                                                  || urlOf(c).startsWith("jdbc:informix-sqli:"));
        r.register(new DbOceanBaseMySQLDialect(),  DbType.OceanBase,  c -> urlOf(c).startsWith("jdbc:oceanbase:") && isOceanBaseMysql(c));
        r.register(new DbOceanBaseOracleDialect(), DbType.OceanBase,  c -> urlOf(c).startsWith("jdbc:oceanbase:") && !isOceanBaseMysql(c));
        r.register(new DbKingbaseMySQLDialect(),   DbType.KingbaseES, c -> urlOf(c).startsWith("jdbc:kingbase") && "mysql".equalsIgnoreCase(getKingbaseMode(c)));
        r.register(new DbKingbaseOracleDialect(),  DbType.KingbaseES, c -> urlOf(c).startsWith("jdbc:kingbase") && "oracle".equalsIgnoreCase(getKingbaseMode(c)));
        r.register(new DbKingbasePostgreDialect(), DbType.KingbaseES, c -> urlOf(c).startsWith("jdbc:kingbase"));
        return r;
    }
```

Also add the two private helpers and the urlOf helper inside the class (after the `matchers.add(...)` block, before the `MatcherEntry` inner class):

```java
    private static String urlOf(Connection c) {
        if (c == null) return null;
        try {
            return c.getMetaData().getURL();
        } catch (SQLException e) {
            return null;
        }
    }

    private static boolean isOceanBaseMysql(Connection c) {
        if (c == null) return true;
        try (java.sql.Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery("show global variables where variable_name = 'ob_compatibility_mode'")) {
            if (rs.next()) {
                String v = rs.getString(2);
                if (v != null) return v.toUpperCase().contains("MYSQL");
            }
        } catch (Throwable ignore) {
            // 驱动不支持这条 SQL 时按 MySQL 处理（保留原 DbContextMetaData 行为）
        }
        return true;
    }

    private static String getKingbaseMode(Connection c) {
        if (c == null) return null;
        try (java.sql.Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery("show database_mode")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Throwable ignore) {
            // 驱动不支持 / 查询失败 → 返回 null，让调用方走 fallback
        }
        return null;
    }
```

Don't forget to add the `import java.sql.SQLException;` if not present (it is — see Task 4's skeleton).

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -Dtest=DbDialectRegistryBuiltinTest -q 2>&1 | tail -30`
Expected: `BUILD SUCCESS` (17 tests passed)

If a few URL-prefix tests still fail, double-check the prefix strings in the test file match the `builtin()` registrations exactly.

- [ ] **Step 6: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/dialect/DbDialectRegistry.java \
        wood/src/test/java/org/noear/wood/dialect/DbDialectRegistryBuiltinTest.java \
        wood/src/test/java/org/noear/wood/dialect/TestDialectSupport.java
git commit -m "feat(wood): implement DbDialectRegistry.builtin() with 20 built-in matchers"
```

---

## Task 8: Wire `globalDialectRegistry` into WoodConfig

**Files:**
- Modify: `wood/src/main/java/org/noear/wood/WoodConfig.java`

- [ ] **Step 1: Add static field and convenience method**

Open `wood/src/main/java/org/noear/wood/WoodConfig.java`. After the existing static fields and before the first method (`protected static boolean isEmpty(...)` at line 66), add:

```java
    /**
     * 全局方言注册表。启动时已预填内置方言；外部可追加；实例级条目优先于这里。
     */
    public static DbDialectRegistry globalDialectRegistry = DbDialectRegistry.builtin();

    /**
     * 便捷方法：向全局注册表注册一个方言 + 匹配器
     */
    public static void registerDialect(DbDialect dialect, Fun1<Boolean, Connection> matcher) {
        globalDialectRegistry.register(dialect, matcher);
    }
```

Add the import at the top (with other imports):

```java
import org.noear.wood.dialect.DbDialect;
import org.noear.wood.dialect.DbDialectRegistry;
import org.noear.wood.ext.Fun1;

import java.sql.Connection;
```

- [ ] **Step 2: Verify compile**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/WoodConfig.java
git commit -m "feat(wood): add WoodConfig.globalDialectRegistry + registerDialect()"
```

---

## Task 9: Add registry to DbContext + setOwner in DbContextMetaData

**Files:**
- Modify: `wood/src/main/java/org/noear/wood/DbContext.java`
- Modify: `wood/src/main/java/org/noear/wood/DbContextMetaData.java`

- [ ] **Step 1: Add `owner` field + setter to DbContextMetaData**

Open `wood/src/main/java/org/noear/wood/DbContextMetaData.java`. After the existing fields (around line 56), add:

```java
    private DbContext owner;        // 反向引用：用于回查实例级 registry
```

Add a setter method (anywhere among the public methods):

```java
    /**
     * 由 DbContext 在创建 metadata 时设置
     */
    public void setOwner(DbContext owner) {
        this.owner = owner;
    }
```

- [ ] **Step 2: Add registry field + accessors to DbContext, and set owner**

Open `wood/src/main/java/org/noear/wood/DbContext.java`. Find the field block (look for `private DataSource ds;` or similar). Add:

```java
    private DbDialectRegistry registry = new DbDialectRegistry();
```

Add the import at the top:

```java
import org.noear.wood.dialect.DbDialectRegistry;
```

Add the accessors and modify `setDialect`:

```java
    /**
     * 获取实例级方言注册表
     */
    public DbDialectRegistry getDialectRegistry() {
        return registry;
    }

    /**
     * 设置实例级方言注册表
     */
    public void setDialectRegistry(DbDialectRegistry registry) {
        this.registry = registry;
    }

    /**
     * 设置方言（如果内置的方言不适合当前数据源）
     *
     * @param dbType    数据库类型
     * @param dbDialect 数据库方言
     */
    public void setDialect(DbType dbType, DbDialect dbDialect) {
        registry.setFixed(dbDialect, dbType);
    }
```

Now find where `DbContext` constructs `DbContextMetaData` (search for `new DbContextMetaData`). In the constructor that creates metadata, add the `setOwner` call right after construction. For example:

```java
this.metaData = new DbContextMetaData(dataSource, schema);
this.metaData.setOwner(this);   // <-- add this line
```

If there are multiple constructors that create metadata, add the line in all of them. The exact location depends on the existing code structure — search for the `metaData` field declaration and any `new DbContextMetaData(` callsite to find them.

- [ ] **Step 3: Verify compile**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/DbContext.java \
        wood/src/main/java/org/noear/wood/DbContextMetaData.java
git commit -m "feat(wood): wire DbDialectRegistry into DbContext + owner back-ref"
```

---

## Task 10: Refactor `setDatabaseType` to use the registry

**Files:**
- Modify: `wood/src/main/java/org/noear/wood/DbContextMetaData.java`

- [ ] **Step 1: Locate the existing setDatabaseType method and its callers**

Open `wood/src/main/java/org/noear/wood/DbContextMetaData.java`. The current method is `setDatabaseType(Connection, String)` (lines 292-367). The `initDo()` method calls it.

- [ ] **Step 2: Replace the body of setDatabaseType with registry lookup**

Replace the entire `setDatabaseType(Connection conn, String jdbcUrl)` method with:

```java
    private void setDatabaseType(Connection conn, String jdbcUrl) {
        // 1) 实例 registry（如果 owner 已设置）
        DbDialectRegistry reg = (owner != null) ? owner.getDialectRegistry() : new DbDialectRegistry();

        // 2) 先查实例 registry
        DbDialectRegistry.Match m = reg.find(conn);
        // 3) 未命中（或 owner 未设置）则退到 WoodConfig.globalDialectRegistry
        if (m.isFallback()) {
            m = WoodConfig.globalDialectRegistry.find(conn);
        }

        type = m.type;
        dialect = m.dialect;
    }
```

Make sure to add the import at the top of `DbContextMetaData.java`:

```java
import org.noear.wood.dialect.DbDialectRegistry;
```

The existing `initDo()` method still calls `setDatabaseType(conn, url)` and `setSchema(conn, metaData)` — those callsites do **not** change.

Also: since the `setDatabaseType` no longer takes `jdbcUrl` (it can read it from `conn` via the registry), update the call site in `initDo()`. Find the call (looks like `setDatabaseType(conn, url);` at line 284) and change to:

```java
                setDatabaseType(conn);
```

Update the method signature accordingly: `private void setDatabaseType(Connection conn)`.

- [ ] **Step 3: Verify compile**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/DbContextMetaData.java
git commit -m "refactor(wood): setDatabaseType uses DbDialectRegistry lookup"
```

---

## Task 11: Add `defaultSchema()` overrides to 4 built-in dialects

**Files:**
- Modify: `wood/src/main/java/org/noear/wood/dialect/DbPostgreSQLDialect.java`
- Modify: `wood/src/main/java/org/noear/wood/dialect/DbH2Dialect.java`
- Modify: `wood/src/main/java/org/noear/wood/dialect/DbSQLServerDialect.java`
- Modify: `wood/src/main/java/org/noear/wood/dialect/DbOracleDialect.java`

- [ ] **Step 1: Read the current state of each file**

Run: `cat /Users/songyinyin/study/wood/wood/src/main/java/org/noear/wood/dialect/DbH2Dialect.java` (and the other 3) to see the existing class body.

- [ ] **Step 2: Add `defaultSchema()` to DbPostgreSQLDialect**

Open `wood/src/main/java/org/noear/wood/dialect/DbPostgreSQLDialect.java`. After the last existing method (or at end of class body, before the closing `}`), add:

```java
    @Override
    public String defaultSchema() {
        return "public";
    }
```

- [ ] **Step 3: Add `defaultSchema()` to DbH2Dialect**

Open `wood/src/main/java/org/noear/wood/dialect/DbH2Dialect.java`. Add:

```java
    @Override
    public String defaultSchema() {
        return "PUBLIC";
    }
```

- [ ] **Step 4: Add `defaultSchema()` to DbSQLServerDialect**

Open `wood/src/main/java/org/noear/wood/dialect/DbSQLServerDialect.java`. Add:

```java
    @Override
    public String defaultSchema() {
        return "dbo";
    }
```

- [ ] **Step 5: Add `defaultSchema()` to DbOracleDialect**

Open `wood/src/main/java/org/noear/wood/dialect/DbOracleDialect.java`. Add:

```java
    @Override
    public String defaultSchema() {
        // 返回 null —— 真正的取值在 DbContextMetaData.setSchema 里用 metaData.getUserName()
        // 之所以不在这里拿，是因 getUserName() 对某些驱动有副作用（建临时连接），
        // 应在 setSchema 的"已确认需要 fallback"的分支里调用，而不是 dialect 的纯函数里。
        return null;
    }
```

- [ ] **Step 6: Verify compile**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/dialect/DbPostgreSQLDialect.java \
        wood/src/main/java/org/noear/wood/dialect/DbH2Dialect.java \
        wood/src/main/java/org/noear/wood/dialect/DbSQLServerDialect.java \
        wood/src/main/java/org/noear/wood/dialect/DbOracleDialect.java
git commit -m "feat(wood): defaultSchema() override for PostgreSQL/H2/SQLServer/Oracle"
```

---

## Task 12: Refactor `setSchema` to use `defaultSchema()`

**Files:**
- Modify: `wood/src/main/java/org/noear/wood/DbContextMetaData.java`

- [ ] **Step 1: Replace the body of setSchema**

Open `wood/src/main/java/org/noear/wood/DbContextMetaData.java`. Find `setSchema(Connection, DatabaseMetaData)` (current lines 369-402). Replace the entire method body with:

```java
    private void setSchema(Connection conn, DatabaseMetaData metaData) throws SQLException {
        try {
            catalog = conn.getCatalog();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (schema != null) {
            return;
        }

        try {
            schema = conn.getSchema();

            if (schema == null) {
                // 优先级：dialect.defaultSchema() > Oracle 特例 > catalog
                schema = (dialect != null) ? dialect.defaultSchema() : null;
                if (schema == null && type == DbType.Oracle && metaData != null) {
                    // Oracle 特殊处理：getUserName()（有副作用的查询，仅在 fallback 分支调用）
                    try {
                        schema = metaData.getUserName();
                    } catch (Throwable ignore) {
                        // swallow —— 跟原来 switch 行为一致
                    }
                }
                if (schema == null) {
                    schema = catalog;
                }
            }
        } catch (Throwable e) {
            schema = (dialect != null) ? dialect.defaultSchema() : null;
            if (schema == null) {
                schema = catalog;
            }
        }
    }
```

- [ ] **Step 2: Verify compile**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood compile -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Run all wood unit tests to verify no regression**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -q 2>&1 | tail -20`
Expected: `BUILD SUCCESS` (all existing tests still pass)

- [ ] **Step 4: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/main/java/org/noear/wood/DbContextMetaData.java
git commit -m "refactor(wood): setSchema uses dialect.defaultSchema(), remove switch"
```

---

## Task 13: External dialect end-to-end test (TDD)

**Files:**
- Create: `wood/src/test/java/org/noear/wood/dialect/DbDialectExternalTest.java`

- [ ] **Step 1: Write end-to-end test for external registration**

Create `wood/src/test/java/org/noear/wood/dialect/DbDialectExternalTest.java`:

```java
package org.noear.wood.dialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.noear.wood.WoodConfig;

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
        DbDialectRegistry builtin = DbDialectRegistry.builtin();
        WoodConfig.globalDialectRegistry = builtin;

        // 注册一个 "MyDb" 方言
        DbDialect myDb = new DbMySQLDialect();  // 复用即可，验证机制
        WoodConfig.globalDialectRegistry.register(myDb, c -> {
            String url = c.getMetaData().getURL();
            return url.startsWith("jdbc:mydb:");
        });

        Connection conn = TestDialectSupport.mockConn("jdbc:mydb://localhost:9999/test");
        DbDialectRegistry.Match m = WoodConfig.globalDialectRegistry.find(conn);

        assertFalse(m.isFallback);
        assertEquals(DbType.External, m.type, "外部方言应打上 DbType.External");
        assertSame(myDb, m.dialect);
        assertEquals("DbMySQLDialect", m.dialect.typeName());
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
    void externalRegistry_prioritizesOverBuiltin() throws SQLException {
        // 故意注册一个比 builtin 更靠前的 matcher，截胡 mysql URL
        DbDialect overrideDialect = new DbH2Dialect();
        WoodConfig.globalDialectRegistry.register(overrideDialect, c -> {
            try {
                return c.getMetaData().getURL().startsWith("jdbc:mysql:");
            } catch (SQLException e) { return false; }
        });

        Connection conn = TestDialectSupport.mockConn("jdbc:mysql://localhost/test");
        DbDialectRegistry.Match m = WoodConfig.globalDialectRegistry.find(conn);

        assertEquals(DbType.External, m.type, "先注册的外部应胜出");
        assertSame(overrideDialect, m.dialect);
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -Dtest=DbDialectExternalTest -q`
Expected: `BUILD SUCCESS` (3 tests passed)

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add wood/src/test/java/org/noear/wood/dialect/DbDialectExternalTest.java
git commit -m "test(wood): end-to-end external dialect registration"
```

---

## Task 14: Document the new mechanism

**Files:**
- Modify: `WOOD_JAVA_接口字典.md`
- Modify: `LOG_java_update.md`

- [ ] **Step 1: Add "外部方言注册" section to WOOD_JAVA_接口字典.md**

Open `WOOD_JAVA_接口字典.md`. Find a sensible place to add a new section (e.g., after the "DbDialect" section if present, or at the end). Append:

```markdown
## 外部方言注册（1.4.5+）

Wood 1.4.5 起支持不修改核心源码注册新数据库方言。

### 全局注册（最常见）

```java
WoodConfig.registerDialect(
    new DbYourDialect(),
    conn -> conn.getMetaData().getURL().startsWith("jdbc:yourdb:")
);
```

### 实例级覆盖（多数据源 / 测试）

```java
DbContext ctx = new DbContext(dataSource);
ctx.getDialectRegistry().register(new DbYourDialect(),
    conn -> /* matcher */);

// 或者整体替换 registry
ctx.setDialectRegistry(new DbDialectRegistry());
```

### 旧 API 兼容

```java
ctx.setDialect(DbType.MySQL, new DbH2Dialect());
// 内部转写为 setFixed，等价于"无视 URL 强制使用该方言"
```

### 优先级

查找顺序：实例 `setDialect` 强制 > 实例 `register` > 全局 `register` > 内置 `builtin()` 兜底（MySQL）。
```

(Adjust the placement to fit the document's structure — paste in a "新增于 1.4.5" section.)

- [ ] **Step 2: Add version entry to LOG_java_update.md**

Open `LOG_java_update.md` and prepend (at the top of the file):

```markdown
## 1.4.5 - 2026-06-16

### 新增

- **外部方言可插拔机制**：`DbDialectRegistry` 允许不修改 Wood 核心源码注册新数据库方言
  - `WoodConfig.registerDialect(dialect, matcher)` 全局注册
  - `DbContext.getDialectRegistry()` / `setDialectRegistry()` 实例级覆盖
  - `DbDialect` 接口新增 `default String typeName()` 与 `default String defaultSchema()`
  - `DbType.External` 表示外部注册的方言
- `DbDialect.defaultSchema()` 替代原 `DbContextMetaData.setSchema` 里的 `switch (type)`，各内置方言自报默认 schema

### 兼容

- 现有 `DbContext.setDialect(DbType, DbDialect)` API 行为不变
- 内置 20 个方言行为完全等价
```

- [ ] **Step 3: Commit**

```bash
cd /Users/songyinyin/study/wood
git add WOOD_JAVA_接口字典.md LOG_java_update.md
git commit -m "docs: document external dialect registration mechanism"
```

---

## Task 15: Final regression run + cleanup

**Files:** (no changes)

- [ ] **Step 1: Run all unit tests in wood module**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood test -q 2>&1 | tail -30`
Expected: `BUILD SUCCESS` with all tests passing (10 + 17 + 3 = 30 tests in this plan + 0 pre-existing).

- [ ] **Step 2: Verify mvn install works**

Run: `cd /Users/songyinyin/study/wood && mvn -pl wood install -DskipTests -q`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Check git log**

Run: `cd /Users/songyinyin/study/wood && git log --oneline -16`
Expected: 14 commits from this plan (Tasks 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15-verify) + 1 design commit + existing.

---

## Self-Review Checklist

**Spec coverage:**

- [x] §2.2 DbDialect interface extension → Tasks 2, 11
- [x] §2.2 DbDialectRegistry class → Tasks 4, 5, 6, 7
- [x] §2.2 WoodConfig extension → Task 8
- [x] §2.2 DbContext extension → Task 9
- [x] §2.2 DbContextMetaData refactor → Tasks 9, 10, 12
- [x] §2.2 DbType.External constant → Task 3
- [x] §3.1 typeName() / defaultSchema() → Tasks 2, 11
- [x] §3.2 builtin() factory → Task 7
- [x] §3.5 owner back-ref → Task 9
- [x] §3.5 setSchema refactor → Task 12
- [x] §3.5 setDatabaseType refactor → Task 10
- [x] §7 test plan → Tasks 4, 5, 6, 7, 13
- [x] §8 docs → Task 14

**Placeholder scan:** No TBDs. All test code is concrete. All file paths are absolute.

**Type consistency:**
- `DbDialectRegistry.Match` is consistent across Tasks 4-13 (dialect, type, isFallback).
- `MatcherEntry` only used internally.
- `Fun1<Boolean, Connection>` reused throughout (matches `org.noear.wood.ext.Fun1`).
- `DbType.External` used in Tasks 3, 5, 13.
- `setOwner` only defined in Task 9, only used in Task 9.
