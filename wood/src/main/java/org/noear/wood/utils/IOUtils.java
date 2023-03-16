package org.noear.wood.utils;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;

public class IOUtils {

    public static void fileWrite(File file, String content) throws Exception{
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        try {
            writer.write(content);
        }finally {
            writer.close();
        }
    }

    /** 根据字符串加载为一个类*/
    public static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        }catch (Throwable ex) {
            return null;
        }
    }

    public static <T> T loadEntity(String className) {
        try {
            Class<?> clz = Class.forName(className);
            if (clz != null) {
                return (T) clz.getDeclaredConstructor().newInstance();
            }
        } catch (Throwable ex) {}
        return null;
    }

    //res::获取资源的RUL
    public static URL getResource(String name) {
        URL url = IOUtils.class.getResource(name);
        if (url == null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader != null) {
                url = loader.getResource(name);
            } else {
                url = ClassLoader.getSystemResource(name);
            }
        }

        return url;
    }

    /** 获取资源URL集*/
    public static Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = IOUtils.class.getClassLoader().getResources(name);
        if (urls == null || urls.hasMoreElements()==false) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader != null) {
                urls = loader.getResources(name);
            } else {
                urls = ClassLoader.getSystemResources(name);
            }
        }

        return urls;
    }


    public static String transferToString(InputStream ins) throws IOException {
        return transferToString(ins, "utf-8");
    }

    /**
     * 将输入流转换为字符串
     *
     * @param ins     输入流
     * @param charset 字符集
     */
    public static String transferToString(InputStream ins, String charset) throws IOException {
        if (ins == null) {
            return null;
        }

        ByteArrayOutputStream outs = transferTo(ins, new ByteArrayOutputStream());

        if (StringUtils.isEmpty(charset)) {
            return outs.toString();
        } else {
            return outs.toString(charset);
        }
    }

    /**
     * 将输入流转换为byte数组
     *
     * @param ins 输入流
     */
    public static byte[] transferToBytes(InputStream ins) throws IOException {
        if (ins == null) {
            return null;
        }

        return transferTo(ins, new ByteArrayOutputStream()).toByteArray();
    }

    /**
     * 将输入流转换为输出流
     *
     * @param ins 输入流
     * @param out 输出流
     */
    public static <T extends OutputStream> T transferTo(InputStream ins, T out) throws IOException {
        if (ins == null || out == null) {
            return null;
        }

        int len = 0;
        byte[] buf = new byte[512];
        while ((len = ins.read(buf)) != -1) {
            out.write(buf, 0, len);
        }

        return out;
    }
}
