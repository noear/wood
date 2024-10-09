package org.noear.wood.xml;

import org.noear.wood.WoodException;

/**
 * @author noear 2024/10/9 created
 */
public class XmlSqlException extends WoodException {
    private CharSequence sqlid;
    private CharSequence javacode;

    public XmlSqlException(Throwable cause, CharSequence sqlid, CharSequence javacode) {
        super(cause.getMessage() + ", sqlid=" + sqlid + ", javacode=\n" + javacode, cause);
        this.sqlid = sqlid;
        this.javacode = javacode;
    }

    public CharSequence getSqlid() {
        return sqlid;
    }

    public CharSequence getJavacode() {
        return javacode;
    }
}
