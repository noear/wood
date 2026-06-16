package org.noear.wood;

import org.noear.wood.dialect.DbDialect;
import org.noear.wood.dialect.DbDialectRegistry;
import org.noear.wood.ext.Act1Ex;
import org.noear.wood.wrap.DbType;
import org.noear.wood.wrap.TableWrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class DbContextMetaData implements Closeable {
    static final Logger log = LoggerFactory.getLogger(DbContextMetaData.class);

    private String schema;
    private String catalog;
    private String productName;
    private String productVersion;
    private String url;

    private transient DataSource dataSource;

    private transient Map<String, TableWrap> tableAll;
    private transient DbType type = DbType.Unknown;
    private transient DbDialect dialect;

    private DbContext owner;        // 反向引用：用于回查实例级 registry

    public final transient ReentrantLock SYNC_LOCK = new ReentrantLock();

    public DbContextMetaData() {

    }

    public DbContextMetaData(DataSource dataSource) {
        this(dataSource, null);
    }

    public DbContextMetaData(DataSource dataSource, String schema) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Parameter dataSource cannot be null");
        }

        this.dataSource = dataSource;
        this.schema = schema;
    }

    /**
     * 获取数据源
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 设置数据源
     */
    protected void setDataSource(DataSource ds) {
        dataSource = ds;
    }

    /**
     * 获取链接字符串
     */
    public String getUrl() {
        return url;
    }

    /**
     * 获取产品名称
     */
    public String getProductName() {
        return productName;
    }

    /**
     * 获取产品版本号
     */
    public String getProductVersion() {
        return productVersion;
    }


    /**
     * 获取连接
     */
    public Connection getConnection() throws SQLException {
        return WoodConfig.connectionFactory.getConnection(getDataSource());
    }

    /**
     * 获取元信息链接
     */
    public Connection getMetaConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * 获取 schema
     */
    public String getSchema() {
        return schema;
    }

    protected void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * 获取 catalog
     */
    public String getCatalog() {
        return catalog;
    }


    //数据集名称

    /**
     * 获取类型
     */
    public DbType getType() {
        init();
        return type;
    }

    public void setType(DbType type) {
        init();
        this.type = type;
    }

    /**
     * 获取方言
     */
    public DbDialect getDialect() {
        init();
        return dialect;
    }

    public void setDialect(DbDialect dialect) {
        init();
        this.dialect = dialect;
    }

    /**
     * 由 DbContext 在创建 metadata 时设置
     */
    public void setOwner(DbContext owner) {
        this.owner = owner;
    }

    public Collection<TableWrap> getTableAll() {
        init();
        initTables();

        return tableAll.values();
    }

    /**
     * 是否有表列
     *
     * @param tableName  表名
     * @param columnName 列表
     * @param refresh    是否刷新
     */
    public boolean hasTableColumn(String tableName, String columnName, boolean refresh) {
        TableWrap tmp = getTable(tableName);
        if (tmp != null) {
            if (refresh) {
                tmp.refresh();
            }
            return tmp.hasColumn(columnName);
        } else {
            return false;
        }
    }

    public TableWrap getTable(String tableName) {
        init();
        initTables();

        return tableAll.get(tableName.toLowerCase());
    }

    public String getTablePk1(String tableName) {
        TableWrap tw = getTable(tableName);
        return tw == null ? null : tw.getPk1();
    }

    /**
     * 刷新元信息
     */
    public DbContextMetaData refresh() {
        SYNC_LOCK.lock();
        try {
            initTablesDo(); //initDo(); 这个没必要刷新
        } finally {
            SYNC_LOCK.unlock();
        }

        return this;
    }

    /**
     * 刷新表元信息（即清空）
     *
     * @deprecated 1.3
     */
    @Deprecated
    public void refreshTables() {
        refresh();
    }

    /**
     * 初始化
     */
    public boolean init() {
        if (dialect != null) {
            return true;
        }

        SYNC_LOCK.lock();
        try {
            if (dialect != null) {
                return true;
            }

            return initDo();
        } finally {
            SYNC_LOCK.unlock();
        }
    }

    private void initPrintln(String x, boolean warn) {
        if (warn) {
            if (schema == null) {
                log.warn("[Wood] Init: " + x);
            } else {
                log.warn("[Wood] Init: " + x + " - " + schema);
            }
        } else {
            if (schema == null) {
                log.debug("[Wood] Init: " + x);
            } else {
                log.debug("[Wood] Init: " + x + " - " + schema);
            }
        }
    }

    private boolean initDo() {
        //这段不能去掉
        initPrintln("The db metadata dialect", false);

        return openMetaConnection(conn -> {
            DatabaseMetaData metaData = conn.getMetaData();

            url = metaData.getURL();
            productName = metaData.getDatabaseProductName();
            productVersion = metaData.getDatabaseProductVersion();

            if (dialect == null) {
                //1. 通过 registry 查找方言
                setDatabaseType(conn);

                //2.
                setSchema(conn, metaData);
            }
        });
    }

    private void setDatabaseType(Connection conn) {
        // 1) 实例 registry（如果 owner 已设置）
        DbDialectRegistry reg = (owner != null) ? owner.getDialectRegistry() : new DbDialectRegistry();

        // 2) 先查实例 registry
        DbDialectRegistry.Match m = reg.find(conn);
        // 3) 未命中（或 owner 未设置）则退到 WoodConfig.globalDialectRegistry
        if (m.isFallback) {
            m = WoodConfig.globalDialectRegistry.find(conn);
        }

        type = m.type;
        dialect = m.dialect;
    }

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

    private void initTables() {
        if (tableAll != null) {
            return;
        }

        SYNC_LOCK.lock();
        try {
            if (tableAll != null) {
                return;
            }

            initTablesDo();
        } finally {
            SYNC_LOCK.unlock();
        }
    }

    private void initTablesDo() {
        //这段不能去掉
        initPrintln("The db metadata tables", false);

        try {
            initTablesLoadDo();
        } catch (Throwable e) {
            initPrintln("The db metadata-tables is loaded failed", true);
            e.printStackTrace();
        }
    }

    private void initTablesLoadDo() throws SQLException {
        tableAll = new HashMap<>();

        DbDialect dbDialect = getDialect();

        try (Connection conn = getMetaConnection();
             ResultSet rs = dbDialect.getTables(conn.getMetaData(), catalog, schema)) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                String remarks = rs.getString("REMARKS");
                TableWrap tWrap = new TableWrap(this, name, remarks);
                tableAll.put(name.toLowerCase(), tWrap);
            }
        }
    }

    private boolean openMetaConnection(Act1Ex<Connection, Exception> callback) {
        Connection conn = null;
        try {
            initPrintln("The db metadata connectivity...", false);
            conn = getMetaConnection();
            callback.run(conn);
            initPrintln("The db metadata is loaded successfully", false);
            return true;
        } catch (Throwable ex) {
            initPrintln("The db metadata is loaded failed", true);
            ex.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ex) {
                    //不用再打印了
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (dataSource != null && dataSource instanceof Closeable) {
            ((Closeable) dataSource).close();
        }
    }
}