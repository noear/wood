### 1.3.10

* 优化 xmlsql 构建命令出错时的异常提示

### 1.3.8

* 调整 表元信息的加载，改为单表加载；且支持单表刷新

### 1.3.7

* liquor 升为 1.3.6

### 1.3.6

* liquor 升为 1.3.0

### 1.3.5

* liquor 升为 1.1.1

### 1.3.4

* 添加 DbContextMetaData:refreshTables
* 优化 内部锁处理，synchronized 改为 ReentrantLock

### 1.3.2

* 添加 db.update 关联更新支持：`db.table("a").leftJoin("b").on("a.a","b.b").set(...).where(...).update()`

### 1.3.1

* lambda 表达式获取 PropertyWrap，增加字段名

### 1.3.0

* 添加 Command.attachment 字段，用于存放 mapper 执行原始参数
* 调整 DbAccess 去掉 Variate 使用，从而简化内核层

### 1.2.15
* xml_mapper 内部变量 map 改为 __map

### 1.2.14
* 添加流读取器接口 IDataReader
* 为流式查询添加 fetchSize 参数选择

### 1.2.13
* Comand 添加影响函数记录
* 增加主键检查策略定制支持

### 1.2.12
* 优化 序列化对泛型的支持
* redisx 升为 1.6.5

### 1.2.11
* 修复 selectCount支持带group by的汇总查询

### 1.2.10
* selectCompile 增加一个重载，支持不对column参数做格式化处理

### 1.2.9
* 添加 WoodConfig.isUsingSchemaExpression

### 1.2.8
* 添加 DbEventBus
* 添加 DbContext 实例事件支持

### 1.2.7
* 添加 TypeConverter.filling 接管 PreparedStatement 的数据填充

### 1.2.6
* 调整 ::initMetaData 恢复返回为空（不然有兼容问题）
* 添加 ::initMetaData2 接口


### 1.2.4

* 添加 BaseMapper::updateById, update

### 1.2.3

* 初始化元信息返回为 bool （之前为空）

### 1.2.2

* DbContextMetaData::getTableAll 改为懒加载

### 1.2.1

* 增加旧版兼容性

### 1.2.0

* 调整 ICacheService 接口（增加类型化 get）
* redisx 升为 1.2.0

### 1.1.9

* 增加代码生成接口（selectAsCmd, insertAsCmd, updateAsCmd, deleteAsCmd）

### 1.1.8

* 增加 DbContext::setDialect 接口，初始化后可以换掉

### 1.1.7

* 增加 BaseMapper::db,tableName,tablePk,eneityClz 接口，提供扩展支持
* 优化 BaseMapper 函数的调用路径，可提高性能

### 1.1.5

* 增加 db.table("table").selectVariate("SUM(num)").longValue(0L);

### 1.1.4

* 调整 qr.setEntity(data)，qr.setMap(data) 根据 usingNull() 做为过滤条件
* 修复 BaseMapper 对 excludeNull 条件无效的问题

### 1.1.0
* 将 xml 和 mapper 的功能分离到 wood.plus
  * 需要 xml 和 mapper 的，则使用 'wood.plus'
  * 不需要的则使用 'wood'

### 1.0.9
* 调整生成器的代码

### 1.0.8
* 增加 Blob,Clob 转 String 的支持
* 增加 Blob,Clob 转 InputStream 的支持
* 增加 DataItem 支持 Map 接口，方便 json 化

### 1.0.7
* 修复 selectPage 接口，在有 orderBy 时会出错的问题（pgsql）

### 1.0.6
* 增强 IDataItem 能力

### 1.0.5
* Db 注解增加作用范围（增加参数注入）

### 1.0.4
* 调整 DbContext::mapper 接口的缓存特性

### 1.0.3
* 增加 DbContext::use 接口

### 1.0.2
init

### 1.0.0
init