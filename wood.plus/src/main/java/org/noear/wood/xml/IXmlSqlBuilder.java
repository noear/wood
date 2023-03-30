package org.noear.wood.xml;

import org.noear.wood.SQLBuilder;

import java.util.Map;

public interface IXmlSqlBuilder {
    SQLBuilder build(Map map) throws Exception;
}
