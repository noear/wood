package org.noear.wood.dialect;

/**
 * DuckDb数据库方言处理
 *
 * @author noear
 * @since 3.2
 * */
public class DbDuckDbDialect extends DbDialectBase{

    @Override
    public String tableFormat(String tb) {
        return tb.replace("`","");
    }

    @Override
    public String columnFormat(String col) {
        return col.replace("`","");
    }
    @Override
    public boolean supportsVariablePaging() {
        return true;
    }
}
