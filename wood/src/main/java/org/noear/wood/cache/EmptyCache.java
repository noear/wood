package org.noear.wood.cache;

/**
 * 空缓存服务
 *
 * @author noear
 * @since 3.0
 */
public class EmptyCache implements ICacheServiceEx {
    @Override
    public void store(String s, Object o, int i) {

    }

    @Override
    public <T> T get(String s, Class<T> clz) {
        return null;
    }

    @Override
    public void remove(String s) {

    }

    @Override
    public int getDefalutSeconds() {
        return 0;
    }

    @Override
    public String getCacheKeyHead() {
        return "";
    }
}
