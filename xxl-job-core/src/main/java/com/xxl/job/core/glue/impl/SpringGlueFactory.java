package com.xxl.job.core.glue.impl;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.glue.GlueFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Spring的Glue工厂(用于动态脚本任务)
 *
 * @author xuxueli 2018-11-01
 */
@Slf4j
public class SpringGlueFactory extends GlueFactory {

    @Override
    public void injectService(Object instance) {
        if (instance == null) {
            return;
        }

        if (XxlJobSpringExecutor.getApplicationContext() == null) {
            return;
        }
        //所有字段的字段对象数组。这包括公共、受保护、默认（包）访问和专用字段，但不包括继承的字段
        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            //判断字段是否为静态属性
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Object fieldBean = null;
            // with bean-id, bean could be found by both @Resource and @Autowired, or bean could only be found by @Autowired
            // 字段为@Resouce注入时
            if (AnnotationUtils.getAnnotation(field, Resource.class) != null) {
                Resource resource = AnnotationUtils.getAnnotation(field, Resource.class);
                //通过@Resouce的name或者字段名获取注入对象
                if (StringUtils.isNotBlank(resource.name())) {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(resource.name());
                } else {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getName());
                }
                //还没找到直接通过字段的类型注入对象
                if (fieldBean == null) {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            } else if (AnnotationUtils.getAnnotation(field, Autowired.class) != null) {
                // 字段为@Autowired注入时
                Qualifier qualifier = AnnotationUtils.getAnnotation(field, Qualifier.class);
                //通过@Qualifier的value或者字段类型获取注入对象
                if (qualifier != null && StringUtils.isNotBlank(qualifier.value())) {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(qualifier.value());
                } else {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            }

            if (fieldBean != null) {
                //设置Field对象的Accessible的访问标志位为Ture，就可以通过反射获取私有变量的值，在访问时会忽略访问修饰符的检查
                field.setAccessible(true);
                try {
                    field.set(instance, fieldBean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}