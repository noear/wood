package org.noear.wood;

import org.noear.wood.ext.Act2;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Created by noear on 15/9/2.
 *
 * IDataItem 是为跨平台设计的接口，不能去掉
 */
public interface IDataItem extends GetHandler, Serializable {
    int count();
    void clear();
    boolean exists(String name);
    Set<String> keys();

    void remove(String name);

    IDataItem set(String name, Object value);
    default IDataItem setIf(boolean condition, String name, Object value){
        if(condition){
            set(name,value);
        }
        return this;
    }

    default IDataItem setDf(String name, Object value, Object def) {
        if (value == null) {
            set(name, def);
        } else {
            set(name, value);
        }
        return this;
    }

    Object get(int index);
    @Override
    Object get(String name);

    <T> T getOrDef(String name, T def);

    Variate getVariate(String name);

    Short getShort(String name);
    Integer getInt(String name);
    Long getLong(String name);
    Float getFloat(String name);
    Double getDouble(String name);
    BigDecimal getBigDecimal(String name);
    BigInteger getBigInteger(String name);
    String getString(String name);
    Boolean getBoolean(String name);
    Date getDateTime(String name);
    LocalDateTime getLocalDateTime(String name);
    LocalDate getLocalDate(String name);
    LocalTime getLocalTime(String name);


    void forEach(Act2<String, Object> action);

    Map<String, Object> getMap();
}
