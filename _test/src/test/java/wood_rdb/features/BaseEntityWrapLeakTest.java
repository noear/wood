package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.BaseMapper;
import org.noear.wood.DbContext;
import org.noear.wood.mapper.BaseEntityWrap;
import org.noear.wood.mapper.BaseMapperWrap;
import webapp.model.AppxModel;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 验证 BaseEntityWrap 不再以 BaseMapper 实例为缓存 key，
 * 避免频繁 mapperBase() 导致的内存泄漏（Issue IK215Y）。
 */
public class BaseEntityWrapLeakTest {

    @Test
    public void test_cache_by_entity_class_not_mapper_instance() throws Exception {
        // 不依赖真实数据库连接；直接构造 BaseMapperWrap
        DbContext db = null;

        BaseMapper<AppxModel> m1 = new BaseMapperWrap<>(db, AppxModel.class, null);
        BaseMapper<AppxModel> m2 = new BaseMapperWrap<>(db, AppxModel.class, null);

        // 每次都是新实例
        assert m1 != m2;

        BaseEntityWrap w1 = BaseEntityWrap.get(m1);
        BaseEntityWrap w2 = BaseEntityWrap.get(m2);
        BaseEntityWrap w3 = BaseEntityWrap.get(AppxModel.class);

        // 同一实体类型应复用同一元数据包装
        assert w1 == w2;
        assert w1 == w3;
        assert w1.entityClz == AppxModel.class;
        assert "appx".equals(w1.tableName);
        assert "app_id".equals(w1.pkName);

        // 大量新建 mapper 后，缓存条目数不应随 mapper 实例数膨胀
        int before = cacheSize();
        for (int i = 0; i < 10000; i++) {
            BaseMapper<AppxModel> m = new BaseMapperWrap<>(db, AppxModel.class, null);
            BaseEntityWrap.get(m);
        }
        int after = cacheSize();

        assert after == before : "cache size grew from " + before + " to " + after;
    }

    private static int cacheSize() throws Exception {
        Field f = BaseEntityWrap.class.getDeclaredField("_lib");
        f.setAccessible(true);
        Map<?, ?> lib = (Map<?, ?>) f.get(null);
        return lib.size();
    }
}
