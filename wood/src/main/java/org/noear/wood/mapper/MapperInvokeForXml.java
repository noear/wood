package org.noear.wood.mapper;

import org.noear.wood.DbContext;
import org.noear.wood.IMapperInvoke;
import org.noear.wood.wrap.MethodWrap;
import org.noear.wood.xml.XmlSqlBlock;
import org.noear.wood.xml.XmlSqlFactory;

import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapperInvokeForXml implements IMapperInvoke {
    public Object call(Object proxy, DbContext db, String sqlid, Class<?> caller, MethodWrap mWrap, Object[] vals) throws Throwable {
        //1.获取代码块，并检测有效性
        XmlSqlBlock block = XmlSqlFactory.get(sqlid);
        if (block == null) {
            return MapperHandler.UOE;
        }

        //2.构建参数
        Map<String, Object> _map = new LinkedHashMap<>();
        Parameter[] names = mWrap.parameters;
        for (int i = 0, len = names.length; i < len; i++) {
            if (vals[i] != null) {
                String key = names[i].getName();
                Object val = vals[i];

                //如果是_map参数，则做特殊处理
                if ("_map".equals(key) && val instanceof Map) {
                    _map.putAll((Map<String, Object>) val);
                } else {
                    _map.put(key, val);
                }
            }
        }

        //3.确定输出类型
        return MapperUtil.exec(db, block, "@" + sqlid, _map, mWrap.returnType, mWrap.returnGenericType);
    }
}
