package org.noear.wood.wrap;

import org.noear.wood.DbContextMetaData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TableWrap {
    private final DbContextMetaData meta;
    private final String name;
    private final String remarks;

    private String pk1;
    private List<String> pks = null;
    private Map<String, ColumnWrap> columns = null;
    private ColumnWrap columnFirst = null;

    public TableWrap(DbContextMetaData meta, String name, String remarks) {
        this.meta = meta;
        this.name = name;
        this.remarks = remarks;
    }

    /**
     * 刷新
     * */
    public TableWrap refresh() {
        tryInit(true);
        return this;
    }

    private void tryInit(boolean refresh) {
        if (columns != null && refresh == false) {
            return;
        }

        meta.SYNC_LOCK.lock();
        try {
            if (columns != null && refresh == false) {
                return;
            }

            columns = new LinkedHashMap<>();
            pks = new ArrayList<>();

            try (ResultSet rs = meta.getReal().getColumns(meta.getCatalog(), meta.getSchema(), getName(), "%")) {
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

                    addColumn(cw);
                }
            }

            try (ResultSet rs = meta.getReal().getPrimaryKeys(meta.getCatalog(), meta.getSchema(), getName())) {
                while (rs.next()) {
                    String idName = rs.getString("COLUMN_NAME");
                    addPk(idName);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        } finally {
            meta.SYNC_LOCK.unlock();
        }
    }

    private void addPk(String name) {
        if (pk1 == null) {
            pk1 = name;
        }
        pks.add(name);
    }

    private void addColumn(ColumnWrap col) {
        if (columnFirst == null) {
            columnFirst = col;
        }

        columns.put(col.getName(), col);
    }

    ////////

    public String getName() {
        return name;
    }

    public String getRemarks() {
        return remarks;
    }

    public List<String> getPks() {
        tryInit(false);

        return pks;
    }

    public String getPk1() {
        tryInit(false);

        if (pk1 != null) {
            return pk1;
        } else {
            if (columns.size() > 0) {
                return columnFirst.getName();
            } else {
                return null;
            }
        }
    }

    public Collection<ColumnWrap> getColumns() {
        tryInit(false);

        return columns.values();
    }

    public ColumnWrap getColumn(String name) {
        tryInit(false);

        return columns.get(name);
    }

    public boolean hasColumn(String name) {
        tryInit(false);

        return columns.containsKey(name);
    }
}