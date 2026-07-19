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
 *
 * 按实体 Class 缓存表元信息，避免以 BaseMapper 实例为 key 导致内存泄漏。
 * 场景：频繁调用 DbContext.mapperBase(clz) 会创建大量 BaseMapperWrap，
 * 若以实例为 key 缓存，实例无法被 GC，会持续堆积直至 OOM。
 */
public class BaseEntityWrap {
    public Class<?> entityClz;
    public String tableName;
    public String pkName;

    private static final ReentrantLock SYNC_LOCK = new ReentrantLock();

    // 以实体 Class 为 key（而非 BaseMapper 实例），缓存大小与实体类型数相关，不会随调用次数膨胀
    private static Map<Class<?>, BaseEntityWrap> _lib = new HashMap<>();

    public static BaseEntityWrap get(BaseMapper bm) {
        return get(resolveEntityClz(bm));
    }

    public static BaseEntityWrap get(Class<?> entityClz) {
        if (entityClz == null || entityClz == Object.class) {
            throw new RuntimeException("There is no entity type declaration, entityClz：" + entityClz);
        }

        BaseEntityWrap tmp = _lib.get(entityClz);
        if (tmp == null) {
            SYNC_LOCK.lock();
            try {
                tmp = _lib.get(entityClz);
                if (tmp == null) {
                    tmp = new BaseEntityWrap(entityClz);
                    _lib.put(entityClz, tmp);
                }
            } finally {
                SYNC_LOCK.unlock();
            }
        }

        return tmp;
    }

    private static Class<?> resolveEntityClz(BaseMapper baseMapper) {
        Class<?> entityClz;
        if (baseMapper instanceof BaseMapperWrap) {
            entityClz = ((BaseMapperWrap) baseMapper).entityType();
        } else {
            Type type = baseMapper.getClass().getInterfaces()[0].getGenericInterfaces()[0];
            entityClz = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        if (entityClz == Object.class) {
            throw new RuntimeException("There is no entity type declaration, baseMapper：" + baseMapper.getClass().getName());
        }

        return entityClz;
    }

    private BaseEntityWrap(Class<?> entityClz) {
        this.entityClz = entityClz;

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

        if (pkName == null) {
            throw new RuntimeException("There is no primary key declaration，entity: " + entityClz.getName());
        }
    }
}
