# Wood 外部方言可插拔机制 — 设计

| 项目   | Wood                |
|------|---------------------|
| 日期   | 2026-06-12          |
| 状态   | 已批准（2026-06-16，决策：走法 A + defaultSchema 迁移） |
| 适用版本 | 1.4.5+              |
| 目标读者 | Wood 维护者、二方库开发者     |

---

## 1. 背景与目标

### 1.1 现状

Wood 的方言机制由三个相互耦合的部分组成：

- `DbType` 是一个 enum（`wood/src/main/java/org/noear/wood/wrap/DbType.java`），共 17 个常量，定义在核心包中。
- `DbContextMetaData.setDatabaseType()` 通过一长串 `if/else jdbcUrl.startsWith(...)` 把 JDBC URL 映射到内置 `DbDialect`
  实现（`DbContextMetaData.java:292-367`）。
- Kingbase mode、OceanBase mode 这类"需要查连接"的判断散落在 `DbContextMetaData.isOceanBaseUseMysqlMode()` /
  `getKingbaseMode()` 私有方法里。

每次新增数据库都要：

1. 在 `DbType` 加 enum 常量；
2. 新建 `DbXxxDialect` 类；
3. 修改 `DbContextMetaData.setDatabaseType()` 的 if/else 链；
4. （可选）扩展 `DbContextMetaData` 的私有工具方法。

改动分散在 3 个核心文件里，对二方库开发者和 Wood 核心维护者都是负担。

### 1.2 目标

允许**不修改 Wood 核心源码**即可注册新的数据库方言，使得：

- 新数据库适配以独立 Maven 模块发布成为可能；
- 二方库 / 业务方在启动期通过一行代码完成注册；
- 内置方言行为零变化；
- 现有 `DbContext.setDialect(DbType, DbDialect)`、`DbContext.getDialect()`、`DbContext.getType()` 等 API 保持兼容。

### 1.3 非目标

- **不做 Java SPI 自动发现**。本次设计只做"编程式注册 API"。SPI 可以在后续版本按需追加。
- **不改 `DbDialect` 现有 15 个方法的语义**。本次只新增 1 个 `default` 方法。
- **不引入新的函数式接口**。复用 `org.noear.wood.ext.Fun1`。
- **不改造 `DbType` 为 class**。enum 保留，仅加 1 个 `External` 常量。

---

## 2. 架构总览

### 2.1 核心思路

把"识别数据库 → 选方言"的逻辑从 `DbContextMetaData.setDatabaseType()` 的硬编码 if/else 中抽出来，落到一个可扩展的
Registry 上。原 if/else 退化为"内置兜底"，作为全局 Registry 的预填项；行为完全等价。

### 2.2 组件清单

| 组件                      | 位置                                        | 角色                                                                                                                                                           |
|-------------------------|-------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DbDialect`（接口扩展）       | `wood/.../dialect/DbDialect.java`         | 新增 2 个 `default` 方法 `typeName()` / `defaultSchema()`                                                                                                              |
| `DbDialectRegistry`（新）  | `wood/.../dialect/DbDialectRegistry.java` | 持有 matcher 列表 + fixed 槽位 + fallback；提供 `find(conn)`                                                                                                          |
| `WoodConfig`（扩展）        | `wood/.../WoodConfig.java`                | 新增 `static DbDialectRegistry globalDialectRegistry` 与 `registerDialect(...)` 便捷方法                                                                            |
| `DbContext`（扩展）         | `wood/.../DbContext.java`                 | 持有实例级 `DbDialectRegistry`；新增 `getDialectRegistry()` / `setDialectRegistry(...)`；构造 metadata 时设 `owner` 反向引用；`setDialect(DbType, DbDialect)` 内部重写为 `setFixed` |
| `DbContextMetaData`（重构） | `wood/.../DbContextMetaData.java`         | 新增 `owner` 反向引用；`setDatabaseType()` 改为"实例 → 全局"两级查找                                                                                                          |
| `DbType`（兼容保留）          | `wood/.../wrap/DbType.java`               | 新增 1 个 `External` 常量；其余 17 个保持不变                                                                                                                             |

### 2.3 调用形态

```java
// 方式一：全局一次性注册（最常见场景）
WoodConfig.registerDialect(
    new DbGbase8sDialect(),

conn ->{
String url = conn.getMetaData().getURL();
        return url.

startsWith("jdbc:gbasedbt-sqli:")
            ||url.

startsWith("jdbc:informix-sqli:");
    }
            );

// 方式二：实例级覆盖（多数据源 / 测试用）
DbContext ctx = new DbContext(ds);
ctx.

getDialectRegistry().

register(new DbMyDialect(),

conn ->conn.

getMetaData().

getURL().

startsWith("jdbc:mydb:"));

// 方式三：手动 setDialect（保持现有 API，内部转写为 setFixed）
        ctx.

setDialect(DbType.GBase8s, new DbGbase8sDialect());
```

---

## 3. 接口与数据结构

### 3.1 `DbDialect` 接口扩展

新增 2 个 `default` 方法：

```java
public interface DbDialect {
    // ... 现有 15 个方法保持不变 ...

    /** 类型名（用于日志/调试/外部标识） */
    default String typeName() {
        return getClass().getSimpleName();
    }

    /** 默认 schema 名（用于 schema 解析失败时的兜底） */
    default String defaultSchema() {
        return null;   // 多数方言用 conn.getSchema() 就够了
    }
}
```

**加法**：

- `typeName()`：用于外部方言自报家门；
- `defaultSchema()`：把 `setSchema` 里的 `switch (type)` 迁移过来，让 DbDialect 自报默认 schema。

各内置方言按需 override `defaultSchema()`：

```java
public class DbPostgreSQLDialect extends DbDialectBase {
    @Override public String defaultSchema() { return "public"; }
}
public class DbH2Dialect         extends DbDialectBase {
    @Override public String defaultSchema() { return "PUBLIC"; }
}
public class DbSQLServerDialect  extends DbDialectBase {
    @Override public String defaultSchema() { return "dbo"; }
}
public class DbOracleDialect     extends DbDialectBase {
    @Override public String defaultSchema() { return null; }  // 用 metaData.getUserName()，保留旧行为
    // 见 §3.5 setSchema 的特殊处理
}
```

`urlPrefixes()` / `matcher(Connection)` 经讨论后**不放在接口上**，匹配条件完全由注册方提供，dialect 类不需要自描述 URL 知识。

### 3.2 `DbDialectRegistry`（新类）

```java
public class DbDialectRegistry {
    private DbDialect fixedDialect;            // 强制覆盖槽
    private DbType fixedType;

    private final List<MatcherEntry> matchers = new ArrayList<>();
    private DbDialect fallback = new DbMySQLDialect();
    private DbType fallbackType = DbType.MySQL;

    /** 构造一个预填了 20 个内置方言的注册表 */
    public static DbDialectRegistry builtin();

    /** 外部注册：dialect + matcher，type 固定为 DbType.External */
    public void register(DbDialect dialect, Fun1<Boolean, Connection> matcher);

    /** 内部用：注册时携带具体 DbType（用于 builtin 预填） */
    void register(DbDialect dialect, DbType type, Fun1<Boolean, Connection> matcher);

    /** 强制覆盖：来自 DbContext.setDialect */
    public void setFixed(DbDialect dialect, DbType type);

    /** 兜底：未命中时使用 */
    public void setFallback(DbDialect dialect, DbType type);

    /** 查找 */
    public Match find(Connection conn);

    public static class Match {
        public final DbDialect dialect;
        public final DbType type;
        public final boolean isFallback;
    }

    private static class MatcherEntry {
        final DbDialect dialect;
        final DbType type;
        final Fun1<Boolean, Connection> matcher;
    }
}
```

**查找算法**：

1. 若 `fixedDialect != null`，返回 `Match(fixedDialect, fixedType, isFallback=true)`；
2. 否则顺序遍历 `matchers`，调用 `matcher.run(conn)`，首个 `true` 返回对应 `Match`；
3. 全未命中 → 返回 `Match(fallback, fallbackType, isFallback=true)`。

**`builtin()` 静态工厂**：把现有 if/else 等价转写为 matcher 条目。OceanBase / Kingbase 这类需要查连接的 ad-hoc
判断转写为闭包，逻辑平移（行为不变）。

### 3.3 `WoodConfig` 扩展

```java
public final class WoodConfig {
    // ... 现有静态字段 ...

    /** 全局方言注册表。启动时已预填内置方言；外部可追加；实例级条目优先于这里。 */
    public static DbDialectRegistry globalDialectRegistry = DbDialectRegistry.builtin();

    /** 便捷方法：向全局注册表注册一个方言 + 匹配器 */
    public static void registerDialect(DbDialect dialect, Fun1<Boolean, Connection> matcher) {
        globalDialectRegistry.register(dialect, matcher);
    }
}
```

### 3.4 `DbContext` 扩展

```java
public class DbContext {
    // ... 现有字段 ...
    private DbDialectRegistry registry = new DbDialectRegistry();

    public DbDialectRegistry getDialectRegistry() {
        return registry;
    }

    public void setDialectRegistry(DbDialectRegistry registry) {
        this.registry = registry;
    }

    /** 兼容旧 API：等价于"强制指定"，无视 URL/连接 */
    public void setDialect(DbType dbType, DbDialect dbDialect) {
        registry.setFixed(dbDialect, dbType);
    }
}
```

### 3.5 `DbContextMetaData` 重构

`DbContextMetaData` 新增一个反向引用字段：

```java
public class DbContextMetaData implements Closeable {
    // ... 现有字段 ...
    private DbContext owner;        // 新增：用于回查实例级 registry

    /** 由 DbContext 在创建 metadata 时设置（protected 包内可见） */
    protected void setOwner(DbContext owner) {
        this.owner = owner;
    }
}
```

`DbContext` 构造 metadata 时设上：

```java
// DbContext 现有初始化 metadata 的地方追加一行
this.metaData.setOwner(this);
```

`setDatabaseType()` 改为：

```java
private boolean initDo() {
    initPrintln("The db metadata dialect", false);
    return openMetaConnection(conn -> {
        DatabaseMetaData metaData = conn.getMetaData();
        url = metaData.getURL();
        productName = metaData.getDatabaseProductName();
        productVersion = metaData.getDatabaseProductVersion();

        if (dialect == null) {
            DbDialectRegistry reg = (owner != null)
                    ? owner.getDialectRegistry()
                    : new DbDialectRegistry();

            DbDialectRegistry.Match m = reg.find(conn);
            if (m.isFallback()) {
                m = WoodConfig.globalDialectRegistry.find(conn);
            }

            type = m.type;
            dialect = m.dialect;

            setSchema(conn, metaData);
        }
    });
}
```

`setSchema` 同步改造（消除 `switch (type)`）：

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
            schema = dialect.defaultSchema();
            if (schema == null && type == DbType.Oracle) {
                // 保留 Oracle 特殊处理：getUserName()
                schema = metaData.getUserName();
            }
            if (schema == null) {
                schema = catalog;
            }
        }
    } catch (Throwable e) {
        schema = dialect.defaultSchema();
        if (schema == null) {
            schema = catalog;
        }
    }
}
```

> 备注：Oracle 那个 `metaData.getUserName()` 是一种**有副作用**的查询（部分驱动会建临时连接），不算纯函数。
> 严格做法是给 `DbOracleDialect` 加一个 `defaultUserSchema(Connection, DatabaseMetaData)` 之类的方法，但这会引入
> 新的 default 方法，**本次不做**；先把 switch 拆掉，行为完全保留。后续可单独立项把 Oracle 这个特例迁移出去。

`isOceanBaseUseMysqlMode(Connection)` 与 `getKingbaseMode(Connection)` 从 `DbContextMetaData` 平移到 `DbDialectRegistry`
（或保留在 metadata 中并在 builtin 闭包内引用，效果相同——见 §6 决策点）。

### 3.6 `DbType` 扩展

```java
public enum DbType {
    Unknown,
    MySQL, MariaDB, SQLServer, PostgreSQL, Oracle, DB2,
    SQLite, H2, Phoenix, ClickHouse, Presto, DuckDb,
    OceanBase, KingbaseES, DM, GBase8s,
    External        // 新增：表示"通过外部 registry 注册的方言"
}
```

外部注册的方言在 `find` 命中时 `type` 字段统一为 `DbType.External`；调用方若需具体名字，通过 `ctx.getDialect().typeName()`
拿。

---

## 4. 错误处理

| 场景                                                | 行为                                      |
|---------------------------------------------------|-----------------------------------------|
| `matcher.run(conn)` 抛 `SQLException`              | 捕获后**继续遍历下一个 matcher**，记 warn 日志        |
| `matcher.run(conn)` 抛 `RuntimeException`          | 同上，捕获并 warn                             |
| `register(dialect, matcher)` 时 `dialect == null`  | 抛 `IllegalArgumentException`（fail-fast） |
| `register(dialect, matcher)` 时 `matcher == null`  | 抛 `IllegalArgumentException`            |
| `setFixed(null, _)`                               | 等价于"取消强制"，fixed 槽位清空                    |
| 多次 `setFixed`                                     | 后注册覆盖前一次                                |
| 多次 `register` 同一 dialect                          | 允许（不报错），按注册顺序匹配                         |
| `find()` 全未命中                                     | 返回 fallback（默认 MySQL）                   |
| `WoodConfig.globalDialectRegistry == null`（被用户清空） | 退化为空匹配，命中 fallback                      |

### 4.1 日志规范

- 命中内置 → 保留现有 `[Wood] Init: The db metadata is loaded successfully`，不变；
- 命中外部注册 → debug 日志 `[Wood] Init: Use external dialect - <typeName>`；
- matcher 抛异常 → warn 日志 `[Wood] Dialect matcher failed: <ex.getMessage()>`；
- `setFixed` 覆盖 → info 日志 `[Wood] Init: Dialect fixed by setDialect - <typeName>`。

---

## 5. 迁移路径

按破坏性从低到高：

1. **零破坏**：现有 20 个内置方言类、`DbDialect` 现有 15 个方法、`DbType` 已有 17 个常量 —— **完全不动**。
2. **加法**：
    - `DbType.External` 增 1 个 enum 常量；
    - `DbDialect` 增 2 个 `default` 方法（`typeName` + `defaultSchema`），4 个内置方言 override `defaultSchema`（PostgreSQL / H2 / SQLServer / Oracle）；
    - `WoodConfig` 增 1 个静态字段 + 1 个静态方法；
    - `DbContext` 增 1 个字段 + 2 个 getter/setter + `setDialect` 内部重写 + 构造 metadata 时设 owner；
    - `DbContextMetaData` 增 1 个 `owner` 字段 + 1 个 `setOwner` 方法 + `setSchema` 内部 switch 拆除；
    - 新增 1 个类 `DbDialectRegistry`。
3. **重构（行为等价）**：
    - `DbContextMetaData.setDatabaseType()` 改为两级 registry 查找；
    - `isOceanBaseUseMysqlMode` / `getKingbaseMode` 从 `DbContextMetaData` 平移到 `DbDialectRegistry`（行为不变）。
4. **API 兼容**：
    - `DbContext.setDialect(DbType, DbDialect)` 签名与可观察语义不变；
    - `DbContextMetaData.setDialect(DbDialect)` 仍可用（走 `setFixed`）；
    - `DbContext.getDialect()` / `getType()` 返回值不变。

---

## 6. 决策点

### 6.1 已决定

- **编程式注册 API，不做 SPI**（用户明确选择）。
- **`DbDialect` 只增 `typeName()`，不增 `urlPrefixes()` / `matcher(Connection)`**（用户明确选择：让下游实现 URL 匹配）。
- **URL 前缀与 matcher 都在注册时传入**（用户明确选择：同时支持两种匹配形态）。
- **复用 `org.noear.wood.ext.Fun1<Boolean, Connection>`**，不新增函数式接口。

### 6.2 待决

- `isOceanBaseUseMysqlMode` / `getKingbaseMode` 是平移到 `DbDialectRegistry` 静态私有方法，还是保留在 `DbContextMetaData`
  作为 package-private 方法供 builtin 闭包引用？两者行为等价。前者内聚性更好，后者改动面更小。*
  *倾向：保留在 `DbContextMetaData`，builtin 闭包通过调用方注入**（即 `DbDialectRegistry.builtin()` 接受一个
  `DbContextMetaData` 引用或一组工具函数）。最终实现期定。

---

## 7. 测试策略

```
src/test/java/org/noear/wood/dialect/
├─ DbDialectRegistryTest.java
│   ├─ testBuiltin_PrePopulated              // 验证 builtin() 含 20 个内置项
│   ├─ testFind_FixedWinsOverMatcher
│   ├─ testFind_MatcherOrderRespected        // 多个 matcher 命中时返回先注册
│   ├─ testFind_FallbackWhenNoMatch
│   ├─ testFind_MatcherException_Continues   // matcher 抛异常不影响后续
│   ├─ testRegister_NullDialect_Throws
│   └─ testRegister_NullMatcher_Throws
│
├─ DbDialectRegistryBuiltinTest.java
│   ├─ testMysql_ByUrlPrefix
│   ├─ testGbase8s_ByUrlPrefix
│   ├─ testOceanBase_MysqlMode_ByConnection  // mock connection
│   ├─ testOceanBase_OracleMode_ByConnection
│   ├─ testKingbase_MysqlMode
│   ├─ testKingbase_OracleMode
│   ├─ testKingbase_PostgreFallback
│   └─ testFallback_IsMysql                  // 未知 URL 走 MySQL
│
└─ DbDialectExternalTest.java
    ├─ testExternal_RegisteredAndMatched
    ├─ testExternal_TypeIsExternal            // 验证 DbType.External
    ├─ testExternal_TypeNameIsCustom
    └─ testGlobalVsInstance_Priority          // 实例 registry 优先于全局
```

**回归测试**：

- 现有 `wood/src/test/` 全跑一遍，用 mock DataSource 验证内置方言选择不变；
- 现有 `DbGbase8sDialectTest.java` 不动，作为"内置 dialect 样板"。

---

## 8. 文档

- `WOOD_JAVA_接口字典.md` 增"外部方言注册"章节；
- `DbDialectRegistry` 类 Javadoc 给完整使用示例；
- `LOG_java_update.md` 加版本条目（标题"支持外围方言注册"）。

---

## 9. 范围核对

- 本 spec 只覆盖"方言注册机制"一项；
- 不涉及 SQLBuilder 改造、不涉及 `TypeConverter`、不涉及缓存层；
- 单次实现可在一个 PR 完成（5 个文件改动 + 1 个新文件 + 测试）。

## 10. 歧义核对

- "内置 vs 外部"以**注册源**区分：`builtin()` 预填的算"内置"，运行时 `register(...)` 算"外部"；
- "fallback"在 `Match.isFallback` 上明确标记为 `true`，调用方可区分"显式命中 fallback"与"主动用 fallback"
  ——本设计中两者等价，但保留字段便于未来扩展；
- `DbType.External` 不会被 builtin 使用，仅外部注册时填入。

---

## 11. `DbType` 演化评估

### 11.1 现状与痛点

`DbType` 是一个**封闭 enum**（`wood/src/main/java/org/noear/wood/wrap/DbType.java`，17 个常量）。其角色有三：

1. **类型标签**：`ctx.getType()` 返回它，调用方用以识别"当前是哪种数据库"；
2. **switch 分支键**：`DbContextMetaData.setSchema()` 里 `switch (type) { case PostgreSQL: ... }` 是它在核心代码里唯一的内部消费者；
3. **API 形参**：`setDialect(DbType, DbDialect)` 接收它。

新增 `DbType.External` 只是**承认"我不够用了"**的妥协 —— 外部方言统一打上 `External` 标签后，`getType()` 不再能区分 GBase 8s 和 Kingbase，外围判断还得回到 `getDialect().typeName()`。这等于让 enum 和 typeName **同时存在并承担同一职责**，长期是双源真相。

### 11.2 三种走法

#### 走法 A：保留 enum + `DbType.External`（当前 spec 默认）

- 不破坏现有 API，`getType()` 仍是 `DbType` 枚举；
- 双源真相：enum 用于"内置 17 种"的快速分支，typeName 用于"任意字符串"的扩展标识；
- **代价**：外部方言在 `getType()` 上被压扁成 `External`，调用方必须组合使用两个 API。

#### 走法 B：把 enum 降级为 class，保留内置常量做静态字段

```java
public final class DbType {
    public static final DbType MySQL     = new DbType("MySQL");
    public static final DbType MariaDB   = new DbType("MariaDB");
    // ... 其他 15 个 ...
    public static final DbType GBase8s   = new DbType("GBase8s");
    public static final DbType External  = new DbType("External"); // 兜底占位

    private final String name;
    public DbType(String name) { this.name = name; }
    public String name() { return name; }

    @Override public boolean equals(Object o) { ... }   // 按 name 比
    @Override public int hashCode() { ... }
    @Override public String toString() { return name; }
}
```

- **保留 `DbType.MySQL` 这种调用语法**（同一标识符），但语义从"枚举常量"变成"单例对象"；
- 外部可以 `new DbType("MyDb")` 或 `DbType.named("MyDb")`，无注册门槛；
- `getType()` 签名不变，行为更准确：外部方言拿到自己声明的 `DbType("MyDb")` 而不是 `External`；
- **唯一破坏点**：现有 `switch (type)`（`setSchema`）需要改成 `if-else` 或迁移到 `DbDialect` 接口（见 §11.3）。

#### 走法 C：彻底去 enum，`getType()` 直接返回 `String`（或 `typeName()`）

- `DbType` 类删除，`ctx.getType()` 返回 `String`；
- 所有 `== DbType.X` 调用点全断（user-facing breaking change）；
- 不推荐 —— 改动面铺到所有调用方，enum 提供的可读性和编译期检查也丢了。

### 11.3 内部 `switch (type)` 的归宿（无论 B 还是 A+）

`DbContextMetaData.setSchema` 当前是唯一内部消费者：

```java
switch (type) {
    case PostgreSQL: schema = "public"; break;
    case H2:         schema = "PUBLIC"; break;
    case SQLServer:  schema = "dbo";
    case Oracle:     schema = metaData.getUserName(); break;
}
```

不管选 A 还是 B，这段都应该从 enum 上解耦。最干净的做法：在 `DbDialect` 接口加一个 `default` 方法：

```java
public interface DbDialect {
    // ... 现有 15 个方法 + typeName() ...

    /** 默认 schema 名（用于 schema 解析失败时的兜底） */
    default String defaultSchema() {
        return null;  // 多数方言用 conn.getSchema() 就够了
    }
}
```

`setSchema` 改为：

```java
if (schema == null) {
    schema = dialect.defaultSchema();   // 各方言自报家门
}
if (schema == null) {
    schema = catalog;                   // 再兜底到 catalog
}
```

各内置方言按需 override `defaultSchema()`。这样 enum 的 `switch` 分支被消解，type 字段在 `setSchema` 里彻底无依赖。

### 11.4 决策（2026-06-12）

经与维护者确认，本 spec 采用 **走法 A + §11.3 的 `defaultSchema()` 迁移**：

- **`DbType` 保留为 enum**（17 个内置常量 + 新增 `External`）；
- **新增 `DbDialect.defaultSchema()`**，把 `setSchema` 里的 `switch (type)` 拆掉；
- **新增 `DbType.External`**，外部方言统一打标，调用方通过 `getDialect().typeName()` 拿真实名字。

**接受代价**：外部方言的 `getType()` 统一是 `DbType.External`（双源真相）。后续若需要更精细的"外部 DbType"标识，可另起 spec 走 B 路径。

### 11.5 对其他章节的影响

- **§1.3** 保持原样（"enum 保留，仅加 1 个 External 常量"已是准确描述）；
- **§2.2 组件表** `DbDialect` 行：从"新增 1 个 `default` 方法"改为"新增 2 个 `default` 方法（`typeName` + `defaultSchema`）"；
- **§3.1** 已更新，包含两个 default 方法 + 各内置方言的 `defaultSchema` override 示例；
- **§3.5** 已更新，`setSchema` 改造为调用 `dialect.defaultSchema()`，Oracle 特例保留为受控的 `if (type == DbType.Oracle)`（**不再用 switch**）；
- **§5 迁移路径** 加法项：原有"DbDialect 增 1 个 `default` 方法"应改为"DbDialect 增 2 个 `default` 方法"；
- **§7 测试** 增加 `testDefaultSchema_OverrideByDialect` 类的用例；
- **§9 范围核对** 不变。

> 走法 B（enum 降级为 class）作为**未来选项**留在本文档，触发条件：用户量级出现"必须区分外部方言类型"的需求时。
> 届时只需在当前 spec 基础上重写 `DbType` 类、`MatcherEntry` 字段类型、`find` 返回类型；其他结构（Registry、查找顺序、fallback 语义）均不动。

