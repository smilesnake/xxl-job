package com.xxl.job.core.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * jdk序列化工具类
 *
 * @author xuxueli 2020-04-12 0:14:00
 */
@Slf4j
public class JdkSerializeTool {
    private JdkSerializeTool() {
    }
    // ------------------------ serialize and unserialize ------------------------

    /**
     * 序列化对象,将对象-->byte[] (由于jedis中不支持直接存储object所以转换成byte[]存入).
     *
     * @param object 待序列化对象
     * @return 序列化的字节码
     */
    public static byte[] serialize(Object object) {
        try (
                ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(byteArrayInputStream)) {
            // 序列化
            oos.writeObject(object);
            return byteArrayInputStream.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return new byte[]{};
    }


    /**
     * 返序列化，将byte[] -->Object
     *
     * @param bytes 待反序列化对象
     * @return 反序列化生成的对象
     */
    public static Object deserialize(byte[] bytes) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream)) {
            // 反序列化
            return ois.readObject();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
