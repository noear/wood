package org.noear.wood;

import java.sql.SQLException;

/**
 * @author noear 2024/7/12 created
 */
public class DataReaderForEntity<T> implements IDataReader<T> {
    private DataReader reader;
    private Class<T> clazz;

    public DataReaderForEntity(DataReader reader, Class<T> clazz) {
        this.reader = reader;
        this.clazz = clazz;
    }

    @Override
    public T next() throws SQLException {
        DataItem dataItem = reader.next();

        if (dataItem != null) {
            return dataItem.toEntity(clazz);
        } else {
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
