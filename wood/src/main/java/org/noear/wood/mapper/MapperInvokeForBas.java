package org.noear.wood.mapper;


import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.IMapperInvoke;
import org.noear.wood.wrap.MethodWrap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapperInvokeForBas implements IMapperInvoke {

    static Map<Object, BaseMapperWrap> _lib = new ConcurrentHashMap<>();

    static BaseMapperWrap getWrap(Object proxy, DbContext db) {
        BaseMapperWrap tmp = _lib.get(proxy.getClass());

        if (tmp == null) {
            tmp = new BaseMapperWrap(db, (BaseMapper) proxy);
            BaseMapperWrap l = _lib.putIfAbsent(proxy.getClass(), tmp);
            if (l != null) {
                tmp = l;
            }
        }

        return tmp;
    }

    public Object call(Object proxy, DbContext db, String sqlid, Class<?> caller, MethodWrap mWrap, Object[] args) throws Throwable {
        if (BaseMapper.class == caller) {
            Object tmp = getWrap(proxy, db);

            return mWrap.method.invoke(tmp, args);
        }

        return MapperHandler.UOE;
    }
}
