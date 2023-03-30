package org.noear.wood.impl;

import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.DbProcedure;
import org.noear.wood.mapper.BaseMapperWrap;
import org.noear.wood.mapper.IMapperAdaptor;
import org.noear.wood.mapper.MapperUtil;
import org.noear.wood.xml.DbXmlsqlProcedure;
import org.noear.wood.xml.XmlSqlLoader;

import java.util.Map;

/**
 * @author noear
 * @since 1.1
 */
public class IMapperAdaptorPlusImpl implements IMapperAdaptor {
    @Override
    public DbProcedure createXmlProcedure(DbContext db, String process, Map<String, Object> args) {
        XmlSqlLoader.tryLoad();
        return new DbXmlsqlProcedure(db).sql(process.substring(1)).setMap(args);
    }

    @Override
    public <T> BaseMapper<T> createMapperBase(DbContext db, Class<T> clz, String tableName) {
        return new BaseMapperWrap<T>(db, clz, tableName);
    }


    /**
     * 印映一个接口代理
     */
    public <T> T createMapper(DbContext db, Class<T> clz) {
        return MapperUtil.createProxy(clz, db);
    }

    /**
     * 印映一份数据
     *
     * @param xsqlid @{namespace}.{id}
     */
    public <T> T createMapper(DbContext db, String xsqlid, Map<String, Object> args) throws Exception {
        return (T) MapperUtil.exec(db, xsqlid, args, null, null);
    }
}
