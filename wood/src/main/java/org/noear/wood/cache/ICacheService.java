package org.noear.wood.cache;

/**
 * 缓存服务接口
 *
 * @author noear
 * @since 3.0
 */
public interface ICacheService {
    /**
     * 保存
     */
    void store(String key, Object obj, int seconds);

    /**
     * 获取
     */
    <T> T get(String key, Class<T> clz);

    /**
     * 获取
     * @deprecated 2.5
     */
    @Deprecated
    default Object get(String key) {
        return get(key, Object.class);
    }

    /**
     * 移除
     */
    void remove(String key);

    /**
     * 默认缓存时间
     */
    int getDefalutSeconds();

    /**
     * 缓存键的开头字符
     */
    String getCacheKeyHead();
}
