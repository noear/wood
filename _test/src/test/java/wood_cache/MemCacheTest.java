package wood_cache;

import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;
import org.noear.wood.cache.memcached.MemCache;

/**
 * @author noear 2021/4/1 created
 */
@SolonTest
public class MemCacheTest {

    @Inject("${cache1}")
    MemCache cache;

    @Test
    public void test() throws Exception{
        cache.remove("key");

        cache.getBy(6, "key", Long.class, (uc) -> {
            return null;
        });

        long tmp = cache.getBy(6, "key",  Long.class,(uc) -> {
            return System.currentTimeMillis();
        });

        long tmp2 = cache.getBy(6, "key", Long.class, (uc) -> {
            return System.currentTimeMillis();
        });

        System.out.println(String.format("tmp:%s, tmp2:%s", tmp,tmp2));
        assert tmp == tmp2;


        Thread.sleep(1000 * 5);

        tmp2 = cache.getBy(6, "key", Long.class, (uc) -> {
            return System.currentTimeMillis();
        });

        System.out.println(String.format("tmp:%s, tmp2:%s", tmp,tmp2));
        assert tmp == tmp2;

        Thread.sleep(1000 * 3);

        tmp2 = cache.getBy(6, "key",  Long.class,(uc) -> {
            return System.currentTimeMillis();
        });

        System.out.println(String.format("tmp:%s, tmp2:%s", tmp,tmp2));
        assert tmp != tmp2;
    }

    @Test
    public void test2() throws Exception{
        cache.remove("key2");

        long tmp = cache.getBy(30, "key2",  Long.class,(uc) -> {
            return System.currentTimeMillis();
        });

        long tmp2 = cache.getBy(30, "key2",  Long.class,(uc) -> {
            return System.currentTimeMillis();
        });

        System.out.println(String.format("tmp:%s, tmp2:%s", tmp,tmp2));
        assert tmp == tmp2;


        Thread.sleep(1000 * 20);

        tmp2 = cache.getBy(30, "key2", Long.class, (uc) -> {
            return System.currentTimeMillis();
        });

        System.out.println(String.format("tmp:%s, tmp2:%s", tmp,tmp2));
        assert tmp == tmp2;
    }
}
