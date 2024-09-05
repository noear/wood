package org.noear.wood.mapper;

import org.noear.wood.BaseMapper;
import org.noear.wood.wrap.ClassWrap;
import org.noear.wood.wrap.FieldWrap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by noear on 19-12-11.
 */
public class BaseEntityWrap {
    public Class<?> entityClz;
    public String tableName;
    public String pkName;

    private static final ReentrantLock SYNC_LOCK = new ReentrantLock();

    private static Map<BaseMapper, BaseEntityWrap> _lib = new HashMap<>();
    public static BaseEntityWrap get(BaseMapper bm) {
        BaseEntityWrap tmp = _lib.get(bm);
        if (tmp == null) {
            SYNC_LOCK.tryLock();
            try {
                tmp = _lib.get(bm);
                if (tmp == null) {
                    tmp = new BaseEntityWrap(bm);
                    _lib.put(bm, tmp);
                }
            } finally {
                SYNC_LOCK.unlock();
            }
        }

        return tmp;
    }

    private BaseEntityWrap(BaseMapper baseMapper) {
        if(baseMapper instanceof BaseMapperWrap){
            entityClz = (Class<?>) ((BaseMapperWrap)baseMapper).entityType();
        }else{
            Type type = baseMapper.getClass().getInterfaces()[0].getGenericInterfaces()[0];
            entityClz = (Class<?>)((ParameterizedType) type).getActualTypeArguments()[0];
        }

        if(entityClz == Object.class){
            throw new RuntimeException("请为BaseMapper申明实体类型");
        }

        ClassWrap classWrap = ClassWrap.get(entityClz);

        tableName = classWrap.tableName;

        if (tableName == null) {
            tableName = entityClz.getSimpleName();
        }


        for (FieldWrap f1 : classWrap.fieldWraps) {
            if (f1.pk) {
                pkName = f1.name;
                break;
            }
        }

        if(pkName == null){
            throw new RuntimeException("没申明主键");
        }
    }
}
