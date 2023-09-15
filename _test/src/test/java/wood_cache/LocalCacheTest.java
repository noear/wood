package wood_cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.wood.cache.LocalCache;

/**
 * @author noear 2021/4/1 created
 */
@RunWith(SolonJUnit4ClassRunner.class)
public class LocalCacheTest {

    LocalCache cache = new LocalCache();


    @Test
    public void test() throws Exception{
        cache.remove("key");

        cache.getBy(6, "key", Long.class, (uc) -> {
            return null;
        });


        Long tmp = cache.getBy(6, "key", Long.class, (uc) -> {
            return System.currentTimeMillis();
        });

        Long tmp2 = cache.getBy(6, "key",  Long.class,(uc) -> {
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

        tmp2 = cache.getBy(6, "key", Long.class, (uc) -> {
            return System.currentTimeMillis();
        });

        System.out.println(String.format("tmp:%s, tmp2:%s", tmp,tmp2));
        assert tmp != tmp2;
    }

    @Test
    public void test2() throws Exception{
        cache.remove("key2");

        Long tmp = cache.getBy(30, "key2", Long.class, (uc) -> {
            return System.currentTimeMillis();
        });

        Long tmp2 = cache.getBy(30, "key2", Long.class, (uc) -> {
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
