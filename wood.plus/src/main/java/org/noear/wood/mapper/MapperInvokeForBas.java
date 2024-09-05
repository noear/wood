package org.noear.wood.mapper;

import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.IMapperInvoke;
import org.noear.wood.WoodConfig;
import org.noear.wood.wrap.MethodWrap;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MapperInvokeForBas implements IMapperInvoke {
    private static final ReentrantLock SYNC_LOCK = new ReentrantLock();

    static Map<Object, BaseMapper> _lib = new HashMap<>();

    static BaseMapper getWrap(Object proxy, DbContext db) {
        BaseMapper tmp = _lib.get(proxy.getClass());

        if (tmp == null) {
            SYNC_LOCK.tryLock();

            try {
                tmp = _lib.get(proxy.getClass());

                if (tmp == null) {
                    BaseEntityWrap _table = BaseEntityWrap.get((BaseMapper) proxy);
                    tmp = WoodConfig.mapperAdaptor.createMapperBase(db, _table.entityClz, _table.tableName);
                    _lib.put(proxy.getClass(), tmp);
                }
            }finally {
                SYNC_LOCK.unlock();
            }
        }


        return tmp;
    }

    protected Object callDo(Object proxy, DbContext db, Method method, Object[] args) throws Throwable {
        Object tmp = getWrap(proxy, db);
        return method.invoke(tmp, args);
    }

    public Object call(Object proxy, DbContext db, String sqlid, Class<?> caller, MethodWrap mWrap, Object[] args) throws Throwable {
        if (caller == BaseMapper.class) {
            return callDo(proxy, db, mWrap.method, args);
        }

        return MapperHandler.UOE;
    }
}
