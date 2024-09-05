package org.noear.wood;

import org.noear.wood.dialect.*;
import org.noear.wood.ext.Act1Ex;
import org.noear.wood.wrap.*;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class DbContextMetaData implements Closeable {
    private String schema;
    private String catalog;
    private String productName;
    private String productVersion;
    private String url;

    private transient DataSource dataSource;

    private transient Map<String, TableWrap> tableAll;
    private transient DbType type = DbType.Unknown;
    private transient DbDialect dialect;

    protected transient ReentrantLock SYNC_LOCK = new ReentrantLock();

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

    public Collection<TableWrap> getTableAll() {
        init();
        initTables();

        return tableAll.values();
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
     * 刷新
     */
    public void refresh() {
        SYNC_LOCK.tryLock();
        try {
            initDo();
        } finally {
            SYNC_LOCK.unlock();
        }
    }

    /**
     * 刷新表（即清空）
     */
    public void refreshTables() {
        SYNC_LOCK.tryLock();
        try {
            if (tableAll != null) {
                Map<String, TableWrap> tmp = tableAll;
                tableAll = null;
                tmp.clear();
            }

            initTablesDo();
        } finally {
            SYNC_LOCK.unlock();
        }
    }

    /**
     * 初始化
     */
    public boolean init() {
        if (dialect != null) {
            return true;
        }

        SYNC_LOCK.tryLock();
        try {
            if (dialect != null) {
                return true;
            }

            return initDo();
        } finally {
            SYNC_LOCK.unlock();
        }
    }

    private void initPrintln(String x) {
        if (schema == null) {
            System.out.println("[Wood] Init: " + x);
        } else {
            System.out.println("[Wood] Init: " + x + " - " + schema);
        }
    }

    private boolean initDo() {
        //这段不能去掉
        initPrintln("Init metadata dialect");

        return openMetaConnection(conn -> {
            DatabaseMetaData md = conn.getMetaData();

            url = md.getURL();
            productName = md.getDatabaseProductName();
            productVersion = md.getDatabaseProductVersion();

            if (dialect == null) {
                //1.
                setDatabaseType(url);

                //2.
                setSchema(conn);
            }
        });
    }

    private void setDatabaseType(String jdbcUrl) {
        if (jdbcUrl != null) {
            String pn = jdbcUrl.toLowerCase().replace(" ", "");

            if (pn.startsWith("jdbc:mysql:")) {
                type = DbType.MySQL;
                dialect = new DbMySQLDialect();
            } else if (pn.startsWith("jdbc:mariadb:")) {
                type = DbType.MariaDB;
                dialect = new DbMySQLDialect();
            } else if (pn.startsWith("jdbc:sqlserver:")) {
                type = DbType.SQLServer;
                dialect = new DbSQLServerDialect();
            } else if (pn.startsWith("jdbc:oracle:")) {
                type = DbType.Oracle;
                dialect = new DbOracleDialect();
            } else if (pn.startsWith("jdbc:postgresql:")) {
                type = DbType.PostgreSQL;
                dialect = new DbPostgreSQLDialect();
            } else if (pn.startsWith("jdbc:db2:")) {
                type = DbType.DB2;
                dialect = new DbDb2Dialect();
            } else if (pn.startsWith("jdbc:sqlite:")) {
                type = DbType.SQLite;
                dialect = new DbSQLiteDialect();
            } else if (pn.startsWith("jdbc:h2:")) {
                type = DbType.H2;
                dialect = new DbH2Dialect();
            } else if (pn.startsWith("jdbc:phoenix:")) {
                type = DbType.Phoenix;
                dialect = new DbPhoenixDialect();
            } else if (pn.startsWith("jdbc:clickhouse:")) {
                type = DbType.ClickHouse;
                dialect = new DbClickHouseDialect();
            } else if (pn.startsWith("jdbc:presto:")) {
                type = DbType.Presto;
                dialect = new DbPrestoDialect();
            } else {
                //做为默认
                dialect = new DbMySQLDialect();
            }
        } else {
            //默认为mysql
            //
            type = DbType.MySQL;
            dialect = new DbMySQLDialect();
        }
    }

    private void setSchema(Connection conn) throws SQLException {
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
                schema = catalog;
            }

        } catch (Throwable e) {
            switch (type) {
                case PostgreSQL:
                    schema = "public";
                    break;
                case H2:
                    schema = "PUBLIC";
                    break;
                case SQLServer:
                    schema = "dbo";
                case Oracle:
                    schema = conn.getMetaData().getUserName();
                    break;
            }
        }
    }

    private void initTables() {
        if (tableAll != null) {
            return;
        }

        SYNC_LOCK.tryLock();
        try {
            if (tableAll != null) {
                return;
            }

            initTablesDo();
        } finally {
            SYNC_LOCK.unlock();
        }
    }

    private void initTablesDo(){
        tableAll = new HashMap<>();

        //这段不能去掉
        initPrintln("Init metadata tables");

        openMetaConnection(conn -> {
            initTablesLoadDo(conn.getMetaData());
        });
    }

    private void initTablesLoadDo(DatabaseMetaData md) throws SQLException {
        ResultSet rs = null;

        rs = getDialect().getTables(md, catalog, schema);
        while (rs.next()) {
            String name = rs.getString("TABLE_NAME");
            String remarks = rs.getString("REMARKS");
            TableWrap tWrap = new TableWrap(name, remarks);
            tableAll.put(name.toLowerCase(), tWrap);
        }
        rs.close();

        List<ColumnWrap> columnAll = new ArrayList<>();
        rs = md.getColumns(catalog, schema, "%", "%");
        while (rs.next()) {
            int digit = 0;
            Object o = rs.getObject("DECIMAL_DIGITS");
            if (o != null) {
                digit = ((Number) o).intValue();
            }

            ColumnWrap cw = new ColumnWrap(
                    rs.getString("TABLE_NAME"),
                    rs.getString("COLUMN_NAME"),
                    rs.getInt("DATA_TYPE"),
                    rs.getInt("COLUMN_SIZE"),
                    digit,
                    rs.getString("IS_NULLABLE"),
                    rs.getString("REMARKS")
            );

            columnAll.add(cw);
        }
        rs.close();


        for (String key : tableAll.keySet()) { //key 为小写
            TableWrap tWrap = tableAll.get(key);
            columnAll.stream().filter(c1 -> key.equals(c1.getTable())).forEach(c1 -> {
                tWrap.addColumn(c1);
            });

            rs = md.getPrimaryKeys(catalog, schema, tWrap.getName());
            while (rs.next()) {
                String idName = rs.getString("COLUMN_NAME");
                tWrap.addPk(idName);
            }
            rs.close();
        }
    }

    private boolean openMetaConnection(Act1Ex<Connection, Exception> callback) {
        Connection conn = null;
        try {
            initPrintln("The db metadata connectivity...");
            conn = getMetaConnection();
            callback.run(conn);
            initPrintln("The db metadata is loaded successfully");
            return true;
        } catch (Throwable ex) {
            initPrintln("The db metadata is loaded failed");
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