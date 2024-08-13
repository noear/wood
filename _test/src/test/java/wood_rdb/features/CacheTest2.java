package wood_rdb.features;

import org.junit.jupiter.api.Test;
import org.noear.wood.cache.ICacheServiceEx;
import org.noear.wood.cache.LocalCache;

/**
 * @author noear 2021/4/2 created
 */
public class CacheTest2 {
    ICacheServiceEx cache = new LocalCache();

    @Test
    public void test() throws Exception{
        long time1 = cache.getBy(6,"cache_test", Long.class,(cu)->{
            return System.currentTimeMillis();
        });


        long time2 = cache.getBy(6,"cache_test", Long.class,(cu)->{
            return System.currentTimeMillis();
        });

        assert time1 == time2;

        Thread.sleep(1000 * 5);


        long time3 = cache.getBy(6,"cache_test", Long.class,(cu)->{
            return System.currentTimeMillis();
        });


        assert time1 == time3;


        Thread.sleep(1000 * 2);


        long time4 = cache.getBy(6,"cache_test", Long.class,(cu)->{
            return System.currentTimeMillis();
        });


        assert time1 != time4;


    }
}
