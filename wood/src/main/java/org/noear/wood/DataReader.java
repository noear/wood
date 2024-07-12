package org.noear.wood;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * 数据读取器
 *
 * @author noear 2024/7/12 created
 */
public class DataReader implements IDataReader<DataItem> {
    private final Command cmd;
    private final SQLer sqLer;
    private final ResultSet rset;
    private final ResultSetMetaData rsetMeta;

    public DataReader(SQLer sqLer, Command cmd, ResultSet rset) throws SQLException {
        this.cmd = cmd;
        this.sqLer = sqLer;
        this.rset = rset;
        this.rsetMeta = rset.getMetaData();
    }

    @Override
    public void close() throws Exception {
        sqLer.tryClose();
    }

    @Override
    public DataItem next() throws SQLException {
        if (rset != null && rset.next()) {
            try {
                DataItem row = new DataItem();
                int len = rsetMeta.getColumnCount();

                for (int i = 1; i <= len; i++) {
                    row.set(rsetMeta.getColumnLabel(i), getObject(i));
                }

                return row;

            } catch (SQLException ex) {
                cmd.context.runExceptionEvent(cmd, ex);
                throw ex;
            }
        } else {
            return null;
        }
    }

    private Object getObject(int idx) throws SQLException {
        return cmd.context.getDialect().preChange(rset.getObject(idx));
    }

    /**
     * 转为数据实体读取器
     * */
    public <T> DataReaderForEntity<T> toEntityReader(Class<T> cls) {
        return new DataReaderForEntity<>(this, cls);
    }
}