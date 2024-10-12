package org.noear.wood.dialect;

/**
 * DuckDb数据库方言处理
 *
 * @author noear
 * @since 3.2
 * */
public class DbDuckDbDialect extends DbDialectBase{
    @Override
    public boolean supportsVariablePaging() {
        return true;
    }
}
