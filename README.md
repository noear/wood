<h1 align="center" style="text-align:center;">
  Wood
</h1>
<p align="center">
微型ORM框架（支持：java sql，xml sql，annotation sql；事务；缓存；监控；等...），无依赖！
</p>
<p align="center">
    <a target="_blank" href="https://search.maven.org/search?q=org.noear%20wood">
        <img src="https://img.shields.io/maven-central/v/org.noear/wood.svg?label=Maven%20Central" alt="Maven" />
    </a>
    <a target="_blank" href="https://www.apache.org/licenses/LICENSE-2.0.txt">
		<img src="https://img.shields.io/:license-Apache2-blue.svg" alt="Apache 2" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html">
		<img src="https://img.shields.io/badge/JDK-8-green.svg" alt="jdk-8" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html">
		<img src="https://img.shields.io/badge/JDK-11-green.svg" alt="jdk-11" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html">
		<img src="https://img.shields.io/badge/JDK-17-green.svg" alt="jdk-17" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html">
		<img src="https://img.shields.io/badge/JDK-21-green.svg" alt="jdk-21" />
	</a>
    <br />
    <a target="_blank" href='https://gitee.com/noear/wood/stargazers'>
		<img src='https://gitee.com/noear/wood/badge/star.svg' alt='gitee star'/>
	</a>
    <a target="_blank" href='https://github.com/noear/wood/stargazers'>
		<img src="https://img.shields.io/github/stars/noear/wood.svg?style=flat&logo=github" alt="github star"/>
	</a>
</p>
<br/>
<p align="center">
	<a href="https://jq.qq.com/?_wv=1027&k=kjB5JNiC">
	<img src="https://img.shields.io/badge/QQ交流群-22200020-orange"/></a>
</p>


#### 特点和理念：
* 跨平台：可以嵌入到JVM脚本引擎（js, groovy, lua, python, ruby）及GraalVM支持的部分语言。
* 很小巧：0.2Mb（且是功能完整，方案丰富；可极大简化数据库开发）。
* 有个性：不喜欢反射（主打弱类型）、不喜欢配置（除了连接，不需要其它配置）。
* 其它的：支持缓存控制和跨数据库事务。

#### 支持数据库：

H2, Db2, MySql, Oracle, PostrgeSQL, SqlLite, SqlServer, Phoenix, Presto

#### 核心对象和功能：

* 上下文：DbContext db
* 四个接口：db.mapper(), db.table(), db.call(), db.sql()

```java
//BaseMapper 接口
db.table(User.class).selectById(1);

//BaseMapper 接口，lambda 条件查询
db.table(User.class).selectList(mq->mq
        .whereLt(User::getGroup,1)
        .andEq(User::getLabel,"T"));


//Table 接口
db.table("user u")
  .innerJoin("user_ext e").onEq("u.id","e.user_id")
  .whereEq("u.type",11)
  .limit(100,20)
  .selectList("u.*,e.sex,e.label", User.class);

db.table("user u")
  .innerJoin("user_ext e").onEq("u.id","e.user_id")
  .whereEq("u.type",11)
  .limit(100,20)
  .selectAsCmd("u.*,e.sex,e.label"); //构建查询命令（即查询语句）

//Table 接口，拼装条件查询（特别适合管理后台）
db.table(logger)
  .where("1 = 1")
  .andIf(TextUtils.isNotEmpty(trace_id), "trace_id = ?", trace_id)
  .andIf(TextUtils.isNotEmpty(tag), "tag = ?", tag)
  .andIf(TextUtils.isNotEmpty(tag1), "tag1 = ?", tag1)
  .andIf(TextUtils.isNotEmpty(tag2), "tag2 = ?", tag2)
  .andIf(TextUtils.isNotEmpty(tag3), "tag3 = ?", tag3)
  .andIf(log_date > 0, "log_date = ?", log_date)
  .andIf(log_id > 0, "log_id <= ?", log_id)
  .andIf(level > 0, "level=?", level)
  .orderBy("log_fulltime desc")
  .limit(size)
  .selectList("*", LogModel.class);
```



#### 组件列表： 

| 组件                  | 说明                          |
|---------------------|-----------------------------|
| org.noear:wood      | 主框架（没有任何依赖）                 |
| org.noear:wood.plus | 增强框架（支持 Mapper 和 XmlMapper） |


| 可选组件 | 说明                     |
| --- |------------------------|
| org.noear:wood-maven-plugin| Maven插件，用于生成 XmlMapper |
| |                        |
| org.noear:wood.cache.memcached| 基于 Memcached 适配的扩展缓存服务 |
| org.noear:wood.cache.redis| 基于 Redis 适配的扩展缓存服务     |
| org.noear:wood.cache.ehcache| 基于 ehcache 适配的扩展缓存服务   |
| org.noear:wood.cache.j2cache| 基于 j2cache 适配的扩展缓存服务   |





#### Meven配置：

```xml
<!-- 框架包 -->
<dependency>
    <groupId>org.noear</groupId>
    <artifactId>wood</artifactId>
    <version>1.3.15</version>
</dependency>

<!-- 可选：maven 插件，用于生成Xml sql mapper接口 -->
<plugin>
    <groupId>org.noear</groupId>
    <artifactId>wood-maven-plugin</artifactId>
    <version>1.3.15</version>
</plugin>
```



#### 入门示例：
```java
/** 1.1.实例化上下文 */
//DbContext db  = new DbContext(properties); //使用Properties配置的示例
//DbContext db  = new DbContext(map); //使用Map配置的示例
//DbContext db  = new DbContext("user","proxool.xxx_db"); //使用proxool线程池配置的示例
//DbContext db  = new DbContext("user","jdbc:mysql://x.x.x:3306/user","root","1234");
DbContext db  = new DbContext("user",new HikariDataSource(...)); //使用DataSource配置的示例

//如果是动态创建，临时用的。用完要关掉
db.close();


/** 1.2.配置事件，执行后打印sql */
public class DemoApp {
    public static void main(String[] args) {
        //或者使用 WoodConfig.onExecuteBef 事件
        WoodConfig.onExecuteAft(cmd -> { 
            System.out.println("[Wood]" + cmd.text + "\r\n" + cmd.paramMap());
        });
    }
}

/** 1.3.多实例切换 */
new DbContext(...).nameSet("a");
new DbContext(...).nameSet("b");
DbContext.use("a").table("user").limit(1).selectItem("*", User.class);


/** 2.1.Mapper用法 */
@Namespace("demo.dso.db")
public interface UserDao extends BaseMapper<UserModel>{
    @Sql("select * from user where id=@{id} limit 1")
    UserModel getUser(int id);
  
    @Sql("select * from user where id=? limit 1")
    UserModel getUser2(int id);

    void addUser(UserModel user); //没注解，需要配xml
}

UserDao userDao = db.mapper(UserDao.class);
//调用 BaseMapper 方法
userDao.selectById(12); 

//调用 @Sql 方法
UserModel user = userDao.getUser(2); 

//调用 Xml sql
userDao.addUser(user); 

//调用Template sql
StatModel stat = userDao.userStat(20201010);



/** 2.2.Table用法 */
//增::
db.table("user").setEntity(user).insert();
db.table("user").setMap(map).insert();
db.table("user").setMap(map).insertAsCmd(); //构建查询命令（即查询语句）
//删::
db.table("user").whereEq("id",2).delete();
//改::
db.table("user").set("sex",1).whereEq("id",2).update();
db.table("user").setInc("level",1).whereEq("id",2).update(); //字段自+1
//查::
db.table("user u")
  .innerJoin("user_ext e").onEq("u.id","e.user_id")
  .whereEq("u.id",1001)
  .selectItem("u.*,e.sex,e.label", User.class);

//查++（折开来拼接条件）::
var tb = db.table("user u");
if(test.a){
  tb.innerJoin("user_ext e").onEq("u.id","e.user_id");
}

if(test.b){
  tb.whereEq("u.id",1001);
}

tb.selectItem("u.*,e.sex,e.label", User.class);

//查++2（通过构建函数拼接条件）::
db.table("user u")
  .build(tb->{
    if(test.a){
      tb.innerJoin("user_ext e").onEq("u.id","e.user_id"); 
    }
       
    if(test.b){
      tb.whereEq("u.id",1001);
    }
  }).selectItem("u.*,e.sex,e.label", User.class);

/** 2.3.Call用法 */
//调用存储过程
db.call("user_get_list_by").set("_type",12).getList(User.class);

//调用xml sql
db.call("@demo.dso.db.user_get").set("id",1001).getItem(User.class);


/** 2.4.Sql用法 */
//快速执行SQL语句
db.sql("select * from user id=?",12).getDataItem();
db.sql("select name from user id=?",12).getValue();


/** 3.1.事件用法（全局配置事件可用 WoodConfig） */
//出异常时
db.onException((cmd,err)->{});
//命令构建时
db.onCommandBuilt((cmd)->{});
//命令执行前
db.onExecuteBef((cmd)->{});
//命令执行中
db.onExecuteStm((cmd,stm)->{});
//命令执行后
db.onExecuteAft((cmd)->{});
```



#### 附：语法参考：

##### （一）Xml sql 语法
* 示例

```xml
<?xml version="1.0" encoding="utf-8" ?>
<!DOCTYPE mapper PUBLIC "-//noear.org//DTD Mapper 3.0//EN" "http://noear.org/dtd/wood-mapper.dtd">
<mapper namespace="wood_demo.xmlsql2"
        import="demo.model.*"
        baseMapper="UserModel">
    <sql id="getUser" return="UserModel" remarks="获取用户信息">
        SELECT * FROM user WHERE id = @{id:int}
    </sql>
</mapper>
```

* 具体参考：[《WOOD XML 语法》](WOOD_mapper_XML_语法.md)

##### （二）Table 语法

* 条件操作（与Mapper共享）

| 方法 | 效果说明 |
| --- | --- |
| where, whereIf |  |
| whereEq, whereNeq | ==, != |
| whereLt, whereLte | \<, \<= |
| whereGt, whereGte | \>, \>= |
| whereLk, whereNlk | LIKE, NOT LIKE |
| whereIn, whereNin | IN(..), NOT IN(..) |
| whereBtw, whereNbtw | BETWEEN, NOT BETWEEN |
| and系统方法 | 同where |
| or系统方法 | 同where |
| begin | \( |
| end | \) |

* 表操作（Table独占）

| 方法 | 效果说明 |
| --- | --- |
| set, setIf | 设置值 |
| setMap, setMapIf | 设置值 |
| setEntity, setEntityIf | 设置值 |
| table | 主表 |
| innerJoin, leftJoin, rightJoin | 关联表 |
| on, onEq | 关联条件 |
| orderBy, orderByAsc, orderByDesc | 排序 |
| groupBy | 组 |
| having | 组条件 |
| limit | 限制范围 |
| select | 查询（返回IQuery） |
| count | 查询快捷版，统计数量 |
| exists | 查询快捷版，是否存在 |
| update | 更新 |
| insert | 插入 |
| delete | 删除 |

* 更多参考：[《WOOD JAVA 接口字典》](WOOD_JAVA_接口字典.md)


