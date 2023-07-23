package org.noear.wood.mapper;

import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.IMapperInvoke;
import org.noear.wood.utils.InvocationHandlerUtils;
import org.noear.wood.utils.ThrowableUtils;
import org.noear.wood.wrap.MethodWrap;
import org.noear.wood.xml.Namespace;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.SQLException;

public class MapperHandler implements InvocationHandler {
    private static IMapperInvoke annInvoke = new MapperInvokeForAnn();
    private static IMapperInvoke xmlInvoke = new MapperInvokeForXml();
    private static MapperInvokeForBas basInvoke = new MapperInvokeForBas();


    protected DbContext db;
    protected Class<?> mapperClz;

    protected MapperHandler(DbContext db, Class<?> mapperClz) {
        this.db = db;
        this.mapperClz = mapperClz;
    }



    protected static UnsupportedOperationException UOE = new UnsupportedOperationException();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return invoke0(proxy, method, args);
        } catch (Throwable ex) {
            ex = ThrowableUtils.throwableUnwrap(ex);

            if (ex instanceof RuntimeException) {
                throw ex;
            } else if (ex instanceof SQLException) {
                throw ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    public Object invoke0(Object proxy, Method method, Object[] args) throws Throwable {
        Class caller = method.getDeclaringClass();

        //调用 Default 函数
        if (method.isDefault()) {
            return InvocationHandlerUtils.invokeDefault(proxy, method, args);
        }

        //调用 Object 函数
        if (caller == Object.class) {
            return InvocationHandlerUtils.invokeObject(mapperClz, proxy, method, args);
        }

        //调用 BaseMapper 函数
        if (caller == BaseMapper.class) {
            return basInvoke.callDo(proxy, db, method, args);
        }


        String sqlid = getSqlid(caller, method);
        MethodWrap mWrap = MethodWrap.get(method);

        //1.尝试有@Sql注解的
        Object tmp = annInvoke.call(proxy, db, sqlid, caller, mWrap, args);

        if (UOE.equals(tmp)) {
            //2.尝试有xml的
            tmp = xmlInvoke.call(proxy, db, sqlid, caller, mWrap, args);

            if (UOE.equals(tmp)) {
                //3.尝试BaseMapper
                tmp = basInvoke.call(proxy, db, sqlid, caller, mWrap, args);

                if (UOE.equals(tmp)) {
                    throw new RuntimeException("Mapper is not implemented(or no xmlsql):@" + sqlid);
                }
            }
        }

        return tmp;
    }

    public static String getSqlid(Class<?> mapperClz, Method method) {
        Namespace c_meta = mapperClz.getAnnotation(Namespace.class);
        String fun_name = method.getName();

        if (c_meta == null) {
            return mapperClz.getPackage().getName() + "." + fun_name;
        } else {
            return c_meta.value() + "." + fun_name;
        }
    }
}
