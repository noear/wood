package org.noear.wood.utils;


/**
 * @author noear 2022/10/15 created
 */
public class JavaUtils {
    public static final int JAVA_MAJOR_VERSION;

    /*
     * 获取 Java 版本号
     * http://openjdk.java.net/jeps/223
     * 1.8.x  = 8
     * 11.x   = 11
     * 17.x   = 17
     */
    static {
        int majorVersion;
        try {
            String version = System.getProperty("java.specification.version");
            if (version.startsWith("1.")) {
                version = version.substring(2);
            }
            majorVersion = Integer.parseInt(version);
        } catch (Throwable ignored) {
            majorVersion = 8;
        }
        JAVA_MAJOR_VERSION = majorVersion;
    }
}
