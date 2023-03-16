package org.noear.wood;

import java.util.*;

/**
 * Created by noear on 14-9-10.
 *
 * 不能转为继承自Map
 * 否则，嵌入别的引擎时，会变转为不可知的MapAdapter
 */
public class DataItem extends DataItemBase<DataItem> implements Map<String,Object>{
    public DataItem(){
        super(false);
    }

    public DataItem(boolean isUsingDbNull) {
        super(isUsingDbNull);
    }

    @Override
    public int size() {
        return _data.size();
    }

    @Override
    public boolean isEmpty() {
        return _data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return _data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return _data.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return _data.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return _data.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return _data.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        _data.putAll(m);
    }

    @Override
    public Set<String> keySet() {
        return _data.keySet();
    }

    @Override
    public Collection<Object> values() {
        return _data.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return _data.entrySet();
    }
}

