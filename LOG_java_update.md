
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