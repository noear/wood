package org.noear.wood.annotation;

import java.lang.annotation.*;

/**
 * 为Wood对象，指定数据源（如 Mapper）
 *
 * @author noear
 * @since 3.2
 * */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.FIELD})
public @interface Db {
    String value() default "";
}