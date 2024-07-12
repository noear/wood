package org.noear.wood.wrap;

import org.noear.wood.annotation.PrimaryKey;

import java.lang.reflect.Field;

public class PrimaryKeyStrategy {

    public  boolean fieldIsPrimaryKey(Class<?> clz, Field f) {
        PrimaryKey annotation = f.getAnnotation(PrimaryKey.class);
        return annotation != null;
    }
}
