package org.noear.wood;

import org.noear.wood.ext.Act2;
import org.noear.wood.ext.Fun2;
import org.noear.wood.ext.LinkedCaseInsensitiveMap;
import org.noear.wood.utils.EntityUtils;
import org.noear.wood.wrap.ClassWrap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by noear on 14-9-10.
 *
 * 不能转为继承自Map
 * 否则，嵌入别的引擎时，会变转为不可知的MapAdapter
 */
public class DataItem implements IDataItem, Iterable<Map.Entry<String,Object>> {
    Map<String, Object> _data = new LinkedCaseInsensitiveMap<>();

    public DataItem() {
    }

    public DataItem(Boolean isUsingDbNull) {
        _isUsingDbNull = isUsingDbNull;
    }

    @Override
    public int count() {
        return _data.size();
    }

    @Override
    public void clear() {
        _data.clear();
    }

    @Override
    public boolean exists(String name) {
        if (name == null) {
            return false;
        } else {
            return _data.containsKey(name);
        }
    }

    @Override
    public Set<String> keys() {
        return _data.keySet();
    }

    @Override
    public void remove(String name) {
        _data.remove(name);
    }

    @Override
    public DataItem set(String name, Object value) {
        _data.put(name, value);
        return this;
    }

    @Override
    public DataItem setIf(boolean condition, String name, Object value) {
        if (condition) {
            set(name, value);
        }
        return this;
    }

    @Override
    public DataItem setDf(String name, Object value, Object def) {
        if (value == null) {
            set(name, def);
        } else {
            set(name, value);
        }
        return this;
    }

    @Override
    public Object get(int index) {
        for (String key : _data.keySet()) {
            if (index == 0) {
                return get(key);
            } else {
                index--;
            }
        }
        return null;
    }

    @Override
    public Object get(String name) {
        return _data.get(name);
    }

    @Override
    public <T> T getOrDef(String name, T def) {
        return (T) _data.getOrDefault(name, def);
    }

    @Override
    public Variate getVariate(String name) {
        if (_data.containsKey(name)) {
            return new Variate(name, get(name));
        } else {
            return new Variate(name, null);
        }
    }


    @Override
    public Short getShort(String name) {
        Number tmp = (Number) get(name);
        if (tmp == null) {
            return null;
        } else {
            return tmp.shortValue();
        }
    }

    @Override
    public Integer getInt(String name) {
        Number tmp = (Number) get(name);
        if (tmp == null) {
            return null;
        } else {
            return tmp.intValue();
        }
    }

    @Override
    public Long getLong(String name) {
        Number tmp = (Number) get(name);
        if (tmp == null) {
            return null;
        } else {
            return tmp.longValue();
        }
    }

    @Override
    public Double getDouble(String name) {
        Number tmp = (Number) get(name);
        if (tmp == null) {
            return null;
        } else {
            return tmp.doubleValue();
        }
    }

    @Override
    public BigDecimal getBigDecimal(String name) {
        return  (BigDecimal) get(name);
    }

    @Override
    public BigInteger getBigInteger(String name) {
        return  (BigInteger) get(name);
    }

    @Override
    public Float getFloat(String name) {
        Number tmp = (Number) get(name);
        if (tmp == null) {
            return null;
        } else {
            return tmp.floatValue();
        }
    }

    @Override
    public String getString(String name) {
        return (String) get(name);
    }

    @Override
    public Boolean getBoolean(String name) {
        return (Boolean) get(name);
    }

    @Override
    public Date getDateTime(String name) {
        return (Date) get(name);
    }

    @Override
    public void forEach(Act2<String, Object> callback) {
        for (Map.Entry<String, Object> kv : _data.entrySet()) {
            Object val = kv.getValue();

            if (val == null && _isUsingDbNull) {
                callback.run(kv.getKey(), "$NULL");
            } else {
                callback.run(kv.getKey(), val);
            }
        }
    }

    private boolean _isUsingDbNull = false;

    //============================
    public static IDataItem create(IDataItem schema, GetHandler source) {
        DataItem item = new DataItem();
        for (String key : schema.keys()) {
            Object val = source.get(key);
            if (val != null) {
                item.set(key, val);
            }
        }
        return item;
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return _data.entrySet().iterator();
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<String, Object>> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<String, Object> entry : _data.entrySet()) {
            action.accept(entry);
        }
    }

    @Override
    public Spliterator<Map.Entry<String, Object>> spliterator() {
        return _data.entrySet().spliterator();
    }


    /**
     * 从map加载数据
     */
    public DataItem setMap(Map<String, Object> data) {
        //
        //保持也where的相同逻辑
        //
        return setMapIf(data, (k, v) -> v != null);
    }

    public DataItem setMapIf(Map<String, Object> data, Fun2<Boolean, String, Object> condition) {
        data.forEach((k, v) -> {
            if (condition.run(k, v)) {
                set(k, v);
            }
        });

        return this;
    }

    /**
     * 从Entity 加载数据
     */
    public DataItem setEntity(Object obj) {
        //
        //保持也where的相同逻辑
        //
        return setEntityIf(obj, (k, v) -> v != null);
    }

    public DataItem setEntityIf(Object obj, Fun2<Boolean, String, Object> condition) {
        EntityUtils.fromEntity(obj, (k, v) -> {
            if (condition.run(k, v)) {
                set(k, v);
            }
        });
        return this;
    }

    /**
     * 获取map
     */
    public Map<String, Object> getMap() {
        return _data;
    }



    /**
     * 转为Entity
     */
    public <T> T toEntity(Class<T> cls) {
        ClassWrap classWrap = ClassWrap.get(cls);

        if (IBinder.class.isAssignableFrom(cls)) {
            IBinder mod = classWrap.newInstance();
            mod.bind(key -> getVariate(key));
            return (T) mod;
        } else {
            return classWrap.toEntity(this);
        }
    }
}

