
### 1、配置（要与数据源属性对应上）

```yaml
test.db1:
  jdbcUrl: "jdbc:h2:mem:dbtest;DB_CLOSE_ON_EXIT=FALSE"
  driverClassName: "org.h2.Driver"
  username: "sa"
  password: ""
```

### 2、构建 DbContext

```java
@Configuration
public class Config {
    @Bean
    @ConfigurationProperties(prefix = "test.db1")
    public HikariDataSource ds1(){
        return new HikariDataSource();
    }

    @Bean("db1")
    @Primary
    public DbContext db2(HikariDataSource dataSource){
        DataSourceHelper.initData(dataSource);

        return new DbContext(dataSource);
    }
}
```

### 3、应用示例

```java
@RequestMapping("/java")
@RestController
public class JavaController {
    @Autowired
    DbContext db2;

    @RequestMapping("demo/json")
    public List<AppxModel> demo() throws Exception {
        return db2.table("appx")
                .where("app_id>?", 48)
                .orderBy("app_id ASC")
                .limit(4)
                .selectList("*", AppxModel.class);
    }
}
```