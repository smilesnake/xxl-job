package com.xxl.job.core.handler.annotation;

import java.lang.annotation.*;

/**
 * 任务处理器注解
 *
 * @author xuxueli 2019-12-11 20:50:13
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlJob {

    /**
     * 任务处理器的名称
     */
    String value();

    /**
     * 初始化处理器，当任务线程初始化时调用
     */
    String init() default "";

    /**
     * 销毁处理器,当处理器销毁时调用
     */
    String destroy() default "";

}
