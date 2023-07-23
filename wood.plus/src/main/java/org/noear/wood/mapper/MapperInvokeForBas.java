package org.noear.wood.mapper;


import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.IMapperInvoke;
import org.noear.wood.WoodConfig;
import org.noear.wood.wrap.MethodWrap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapperInvokeForBas implements IMapperInvoke {

    static Map<Object, BaseMapper> _lib = new ConcurrentHashMap<>();

    static BaseMapper getWrap(Object proxy, DbContext db) {
        BaseMapper tmp = _lib.get(proxy.getClass());

        if (tmp == null) {
            BaseEntityWrap _table = BaseEntityWrap.get((BaseMapper) proxy);
            tmp = WoodConfig.mapperAdaptor.createMapperBase(db, _table.entityClz, _table.tableName);
            BaseMapper l = _lib.putIfAbsent(proxy.getClass(), tmp);
            if (l != null) {
                tmp = l;
            }
        }

        return tmp;
    }

    public Object call(Object proxy, DbContext db, String sqlid, Class<?> caller, MethodWrap mWrap, Object[] args) throws Throwable {
        if (WoodConfig.mapperAdaptor.isMapperBase(caller)) {
            Object tmp = getWrap(proxy, db);

            return mWrap.method.invoke(tmp, args);
        }

        return MapperHandler.UOE;
    }
}
