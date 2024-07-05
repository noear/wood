package org.noear.wood.cache;

import java.lang.reflect.Type;

/**
 * 对象序列化接口
 *
 * @author noear
 * @since 3.0
 * */
public interface ISerializer<T> {
    /**
     * 名称
     */
    String name();

    /**
     * 序列化
     */
    T serialize(Object fromObj) throws Exception;

    /**
     * 反序列化
     */
    Object deserialize(T fromDta, Type toType) throws Exception;

    /**
     * 反序列化
     */
    default Object deserialize(T fromDta, Class<?> toClz) throws Exception {
        return deserialize(fromDta, (Type) toClz);
    }
}
