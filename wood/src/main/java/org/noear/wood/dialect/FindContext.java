package org.noear.wood.dialect;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 一次 find 内的上下文：缓存通用元数据（url）与厂商探测结果，
 * 只负责"同一次 find 内不重复计算"，不含识别逻辑本身。
 * loader 自行处理异常并返回默认值（null 也算有效缓存，不重试）。
 *
 * @author noear
 * @since 1.4.5
 */
public class FindContext {
    final Connection conn;

    private String url;
    private boolean urlResolved = false;

    /**
     * 厂商探测结果按 key 懒加载缓存
     */
    private final Map<String, Object> cache = new HashMap<>();

    FindContext(Connection conn) {
        this.conn = conn;
    }

    /**
     * 连接的 jdbc url，一次 find 内只取一次
     */
    public String url() {
        if (!urlResolved) {
            urlResolved = true;
            url = computeUrl(conn);
        }
        return url;
    }

    @SuppressWarnings("unchecked")
    public <T> T cached(String key, Function<String, T> loader) {
        Object v = cache.get(key);
        if (v != null || cache.containsKey(key)) {
            return (T) v;
        }
        T value = loader.apply(key);
        cache.put(key, value);
        return value;
    }

    private String computeUrl(Connection c) {
        if (c == null) return null;
        try {
            return c.getMetaData().getURL();
        } catch (Throwable e) {
            return null;
        }
    }
}
