package org.noear.wood.impl;

import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.DbProcedure;
import org.noear.wood.mapper.IMapperAdaptor;

import java.util.Map;

/**
 * @author noear
 * @since 1.1
 */
public class IMapperAdaptorImpl implements IMapperAdaptor {
    static final String hint = "To use the mapper feature, use the 'wood.plus' dependency package";

    private IMapperAdaptor real;

    public IMapperAdaptorImpl() {
        try {
            Class<?> clz = IMapperAdaptor.class.getClassLoader().loadClass("org.noear.wood.impl.IMapperAdaptorPlusImpl");
            real = (IMapperAdaptor) clz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {

        }
    }

    @Override
    public DbProcedure createXmlProcedure(DbContext db, String process, Map<String, Object> args) {
        if (real == null) {
            throw new UnsupportedOperationException(hint);
        } else {
            return real.createXmlProcedure(db, process, args);
        }
    }

    @Override
    public <T> BaseMapper<T> createMapperBase(DbContext db, Class<T> clz, String tableName) {
        if (real == null) {
            throw new UnsupportedOperationException(hint);
        } else {
            return real.createMapperBase(db, clz, tableName);
        }
    }

    @Override
    public <T> T createMapper(DbContext db, Class<T> clz) {
        if (real == null) {
            throw new UnsupportedOperationException(hint);
        } else {
            return real.createMapper(db, clz);
        }
    }

    @Override
    public <T> T createMapper(DbContext db, String xsqlid, Map<String, Object> args) throws Exception {
        if (real == null) {
            throw new UnsupportedOperationException(hint);
        } else {
            return real.createMapper(db, xsqlid, args);
        }
    }
}
