package org.noear.wood.dialect;

/**
 * 达梦数据库
 *
 * @author songyinyin
 * @since 2025/7/17 15:35
 */
public class DbDamengDialect extends DbOracleDialect {

    @Override
    public String tableFormat(String tb) {
        String[] ss = tb.split("\\.");

        if (ss.length > 1) {
            return "\"" + ss[0] + "\".\"" + ss[1] + "\"";
        } else {
            return "\"" + ss[0] + "\"";
        }
    }

    @Override
    public String columnFormat(String col) {
        String[] ss = col.split("\\.");

        if (ss.length > 1) {
            if ("*".equals(ss[1])) {
                return "\"" + ss[0] + "\".*";
            } else {
                return "\"" + ss[0] + "\".\"" + ss[1] + "\"";
            }
        } else {
            return "\"" + ss[0] + "\"";
        }
    }
}
