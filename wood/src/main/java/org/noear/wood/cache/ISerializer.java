package org.noear.wood.cache;

/**
 * 对象序列化接口
 *
 * @author noear
 * @since 3.0
 * */
public interface ISerializer<T> {
    /**
     * 名称
     * */
    String name();
    /**
     * 序列化
     * */
    T serialize(Object fromObj) throws Exception ;
    /**
     * 反序列化
     * */
    Object deserialize(T fromDta, Class<?> toClz) throws Exception ;
}
