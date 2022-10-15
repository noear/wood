package org.noear.wood.utils;

import org.noear.wood.DataItem;
import org.noear.wood.ext.Act2;
import org.noear.wood.wrap.ClassWrap;

public class EntityUtils {
    public static void fromEntity(Object obj, Act2<String, Object> setter) {
        ClassWrap.get(obj.getClass()).fromEntity(obj, setter);
    }

    public static <T> T toEntity(Class<T> clz, DataItem data) {
        return ClassWrap.get(clz).toEntity(data);
    }
}
