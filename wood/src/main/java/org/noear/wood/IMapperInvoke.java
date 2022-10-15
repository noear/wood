package org.noear.wood;

import org.noear.wood.wrap.MethodWrap;

public interface IMapperInvoke {
    Object call(Object proxy, DbContext db, String sqlid, Class<?> caller, MethodWrap mWrap, Object[] args) throws Throwable;
}
