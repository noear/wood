package org.noear.wood;

import org.noear.wood.dialect.DbDialect;
import org.noear.wood.ext.*;
import org.noear.wood.utils.StringUtils;
import org.noear.wood.wrap.DbFormater;
import org.noear.wood.wrap.DbType;
import org.noear.wood.xml.XmlSqlLoader;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Created by noear on 14-6-12.
 * 数据库上下文
 */
public class DbContext implements Closeable {

    /**
     * 最后次执行命令 (线程不安全，仅供调试用)
     */
    @Deprecated
    public Command lastCommand;
    /**
     * 充许多片段执行
     */
    private boolean allowMultiQueries;


    public boolean isAllowMultiQueries() {
        return allowMultiQueries;
    }

    public void setAllowMultiQueries(boolean allowMultiQueries) {
        this.allowMultiQueries = allowMultiQueries;
    }

    /**
     * 编译模式（用于产生代码）
     */
    private boolean compilationMode = false;

    /**
     *
     */
    public boolean isCompilationMode() {
        return compilationMode;
    }

    public void setCompilationMode(boolean compilationMode) {
        this.compilationMode = compilationMode;
    }

    private DbContextMetaData metaData = new DbContextMetaData();

    /**
     * 获取元信息
     */
    public DbContextMetaData getMetaData() {
        return metaData;
    }

    /**
     * 初始化元信息
     */
    public void initMetaData() {
        getMetaData().init();
    }

    /**
     * 获取类型
     */
    public DbType getType() {
        return getMetaData().getType();
    }

    /**
     * 获取方言
     */
    public DbDialect getDialect() {
        return getMetaData().getDialect();
    }

    /**
     * 获取链接
     */
    public Connection getConnection() throws SQLException {
        return getMetaData().getConnection();
    }


    //数据集名称
    protected DbFormater _formater = new DbFormater(this);

    //特性支持
    protected Map<String, String> _attrMap = new HashMap<>();

    //代码注解
    protected String _codeHint = null;
    protected String _name;


    /**
     * 名字获取
     */
    public String name() {
        return _name;
    }

    public DbContext nameSet(String name) {
        _name = name;
        WoodConfig.libOfDb.put(name, this);
        return this;
    }

    //
    // 构建函数 end
    //

    /**
     * 特性设置
     */
    public DbContext attrSet(String name, String value) {
        _attrMap.put(name, value);
        return this;
    }

    /**
     * 特性获取
     */
    public String attr(String name) {
        return _attrMap.get(name);
    }


    /**
     * 设置JDBC驱动
     */
    public DbContext driverSet(String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this;
    }


    /**
     * 数据集合名称设置
     */
    public DbContext schemaSet(String schema) {
        if (StringUtils.isNotEmpty(schema)) {
            getMetaData().setSchema(schema);

            if (_name == null) {
                _name = schema;
            }
        }

        return this;
    }

    /**
     * 代码注解设置
     */
    public DbContext codeHintSet(String hint) {
        _codeHint = hint;
        return this;
    }

    /**
     * 代码注解获取
     */
    public String codeHint() {
        return _codeHint;
    }


    /**
     * 是否配置了schema
     */
    @Deprecated
    public boolean schemaHas() {
        return getMetaData().getSchema() != null;
    }

    /**
     * 获取schema
     */
    public String schema() {
        return getMetaData().getSchema();
    }

    //
    // 格式化处理
    //
    public DbContext formaterSet(DbFormater formater) {
        _formater = formater;
        return this;
    }

    public DbFormater formater() {
        return _formater;
    }

    //
    // 构建函数 start
    //
    public DbContext(DataSource dataSource) {
        this(dataSource, null);
    }

    public DbContext(DataSource dataSource, String schema) {
        schemaSet(schema);
        getMetaData().setDataSource(dataSource);
    }

    public DbContext(Properties prop) {
        String schema = prop.getProperty("schema");
        String url = prop.getProperty("url");
        String username = prop.getProperty("username");
        String password = prop.getProperty("password");
        String driverClassName = prop.getProperty("driverClassName");

        if (StringUtils.isEmpty(url) || url.startsWith("jdbc:") == false) {
            throw new IllegalArgumentException("DataSource url configuration error");
        }

        if (StringUtils.isNotEmpty(driverClassName)) {
            driverSet(driverClassName);
        }

        if (StringUtils.isNotEmpty(schema)) {
            getMetaData().setSchema(schema);
        }

        if (StringUtils.isEmpty(getMetaData().getSchema()) && url.indexOf("://") > 0) {
            getMetaData().setSchema(URI.create(url.substring(5)).getPath().substring(1));
        }

        if (StringUtils.isEmpty(username)) {
            getMetaData().setDataSource(new DbDataSource(url));
        } else {
            getMetaData().setDataSource(new DbDataSource(url, username, password));
        }
    }


    //基于线程池配置（如："proxool."）
    public DbContext(String schema, String url) {
        schemaSet(schema);
        getMetaData().setDataSource(new DbDataSource(url));
    }

    //基于手动配置（无线程池）
    public DbContext(String schema, String url, String username, String password) {
        schemaSet(schema);
        getMetaData().setDataSource(new DbDataSource(url, username, password));
    }

    public DbContext(String schema, DataSource dataSource) {
        schemaSet(schema);
        getMetaData().setDataSource(dataSource);
    }

    //
    // 执行能力支持
    //

    public <T> BaseMapper<T> mapperBase(Class<T> clz) {
        return new BaseMapperWrap<T>(this, clz, null);
    }

    public <T> BaseMapper<T> mapperBase(Class<T> clz, String tableName) {
        return new BaseMapperWrap<T>(this, clz, tableName);
    }

    /**
     * 印映一个接口代理
     */
    public <T> T mapper(Class<T> clz) {
        return MapperUtil.proxy(clz, this);
    }

    /**
     * 印映一份数据
     *
     * @param xsqlid @{namespace}.{id}
     */
    public <T> T mapper(String xsqlid, Map<String, Object> args) throws Exception {
        return (T) MapperUtil.exec(this, xsqlid, args, null, null);
    }


    /**
     * 获取一个表对象［用于操作插入也更新］
     */
    public DbTableQuery table(String table) {
        return new DbTableQuery(this).table(table);
    }

    /**
     * 输入process name，获取process执行对象
     *
     * @param process process name,process code,xsqlid
     */
    public DbProcedure call(String process) {
        if (process.startsWith("@")) {
            XmlSqlLoader.tryLoad();
            return new DbXmlsqlProcedure(this).sql(process.substring(1));
        }

        if (process.lastIndexOf(" ") > 0) {
            return new DbQueryProcedure(this).sql(process);
        }

        return new DbStoredProcedure(this).call(process);
    }

    /**
     * 输入process name，获取process执行对象
     *
     * @param process process name,process code,xsqlid
     */
    public DbProcedure call(String process, Map<String, Object> args) {
        if (process.startsWith("@")) {
            XmlSqlLoader.tryLoad();
            return new DbXmlsqlProcedure(this).sql(process.substring(1)).setMap(args);
        }

        if (process.lastIndexOf(" ") > 0) {
            return new DbQueryProcedure(this).sql(process).setMap(args);
        }

        return new DbStoredProcedure(this).call(process).setMap(args);
    }


    /**
     * 输入SQL，获取查询器
     */
    public DbQuery sql(String code, Object... args) {
        return sql(new SQLBuilder().append(code, args));
    }

    /**
     * 输入SQL builder，获取查询器
     */
    public DbQuery sql(Act1<SQLBuilder> buildRuner) {
        SQLBuilder sql = new SQLBuilder();
        buildRuner.run(sql);
        return sql(sql);
    }

    /**
     * 输入SQL builder，获取查询器
     */
    public DbQuery sql(SQLBuilder sqlBuilder) {
        return new DbQuery(this).sql(sqlBuilder);
    }


    /**
     * 执行代码，按需返回
     */
    public Object exe(String code, Object... args) throws Exception {
        String cmd = "val";
        String[] ss = code.split("::");
        if (ss.length > 1) {
            cmd = ss[0];
            code = ss[1];
        }

        String codeUp = code.trim().substring(0, 10).toUpperCase();
        if (codeUp.startsWith("SELECT ")) {
            switch (cmd) {
                case "obj":
                case "map":
                    return sql(code, args).getMap();

                case "ary":
                case "list":
                    return sql(code, args).getMapList();

                default:
                    return sql(code, args).getValue();
            }
        } else {
            return sql(code, args).execute();
        }
    }

    /**
     * 批量执行
     */
    public int[] exeBatch(String code, List<Object[]> args) throws Exception {
        SQLBuilder sql = new SQLBuilder();
        sql.append(code, args.toArray());
        return sql(sql).executeBatch();
    }

    @Override
    public void close() throws IOException {
        if (metaData != null) {
            metaData.close();
        }
    }
}
