package com.xxl.job.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Gson工具类.
 *
 * @author xuxueli 2020-04-11 20:56:31
 */
public class GsonTool {
    private GsonTool() {
    }

    private static Gson gson;

    static {
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    }

    /**
     * Object 转成 json
     *
     * @param src 源对象
     * @return String json字符串
     */
    public static String toJson(Object src) {
        return gson.toJson(src);
    }

    /**
     * json 转成 特定的cls的Object
     *
     * @param json     json字符串
     * @param classOfT 特定的类class对象
     * @return Class生成的类对象
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    /**
     * json 转成 特定的 rawClass<classOfT> 的Object
     *
     * @param json        json字符串
     * @param classOfT    类
     * @param argClassOfT 特定泛化类型
     * @return 泛型
     */
    public static <T> T fromJson(String json, Class<T> classOfT, Class argClassOfT) {
        Type type = new ParameterizedType4ReturnT(classOfT, new Class[]{argClassOfT});
        return gson.fromJson(json, type);
    }

    /**
     * 参数类型返回泛型实体
     */
    @AllArgsConstructor
    public static class ParameterizedType4ReturnT implements ParameterizedType {
        /**
         * 类型
         */
        private final Class raw;
        /**
         * 实际类型参数
         */
        private final Type[] args;

        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}
