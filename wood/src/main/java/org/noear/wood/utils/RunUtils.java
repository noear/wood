package org.noear.wood.utils;

import org.noear.wood.ext.Fun0Ex;

public class RunUtils {
    public static <T> T call(Fun0Ex<T, Exception> fun) {
        try {
            return fun.run();
        } catch (Throwable ex) {
            ex = ThrowableUtils.throwableUnwrap(ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }
}
