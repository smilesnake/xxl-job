package com.xxl.job.core.glue;

import com.xxl.job.core.glue.impl.SpringGlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.lang.StringUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * glue 工厂,生产类/通过名称生成对象 class/object by name
 *
 * @author xuxueli 2016-1-2 20:02:27
 */
public class GlueFactory {


    private static GlueFactory glueFactory = new GlueFactory();

    public static GlueFactory getInstance() {
        return glueFactory;
    }

    /**
     * 刷新GlueFactory，获取SpringGlueFactory
     *
     * @param type 0.GlueFactory 1.SpringGlueFactory
     */
    public static void refreshInstance(int type) {
        // 创建glueFactory实例
        if (type == 0) {
            glueFactory = new GlueFactory();
        } else if (type == 1) {
            glueFactory = new SpringGlueFactory();
        }
    }


    /**
     * Groovy类加载器
     * <pre>bootstrap class loader -->extensions class loader-->system class loader-->.GroovyClassLoader-->GroovyClassLoader.InnerLoader</pre>
     */
    private GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
    /**
     * 类缓存.<groovy源码通过MD5加密后的字符串，源码通过类加载器生成的类对象>
     */
    private ConcurrentMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    /**
     * 加载新的实例 load new instance, prototype
     *
     * @param codeSource Groovy源码
     * @return 转化后的处理器对象
     * @throws IllegalAccessException 非法访问，抛出
     * @throws InstantiationException 不能实例化，抛出
     */
    public IJobHandler loadNewInstance(String codeSource) throws IllegalAccessException, InstantiationException {
        if (StringUtils.isNotBlank(codeSource)) {
            //得到class对象
            Class<?> clazz = getCodeSourceClass(codeSource);
            if (clazz != null) {
                //加载的实体必须为IJobHandler的子类型
                Object instance = clazz.newInstance();
                if (instance != null) {
                    if (instance instanceof IJobHandler) {
                        this.injectService(instance);
                        return (IJobHandler) instance;
                    } else {
                        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, "
                                + "cannot convert from instance[" + instance.getClass() + "] to IJobHandler");
                    }
                }
            }
        }
        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, instance is null");
    }

    /**
     * Groovy源码通过类加载器生成类对象.
     *
     * @param codeSource Groovy源码
     * @return Groovy源码生成的类对象
     */
    private Class<?> getCodeSourceClass(String codeSource) {

        try {
            // md5
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
            String md5Str = new BigInteger(1, md5).toString(16);

            //加载类型对象，不存在就通过类加载器生成
            Class<?> clazz = classCache.get(md5Str);
            if (clazz == null) {
                clazz = groovyClassLoader.parseClass(codeSource);
                classCache.putIfAbsent(md5Str, clazz);
            }
            return clazz;
        } catch (NoSuchAlgorithmException e) {
            //加密抛出异常后直接生成类，不加入缓存
            return groovyClassLoader.parseClass(codeSource);
        }
    }

    /**
     * spring通过bean字段注入服务
     *
     * @param instance groovy源码通过类加载器生成class对象的newInstance()方法生成一个object对象
     */
    public void injectService(Object instance) {
        // do something
    }
}
