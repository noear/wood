
### 1.1.4

* 调整 qr.setEntity(data)，qr.setMap(data) 根据 usingNull() 做为过滤条件

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