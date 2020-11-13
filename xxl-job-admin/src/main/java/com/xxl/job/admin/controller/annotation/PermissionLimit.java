package com.xxl.job.admin.controller.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限限制注解
 *
 * @author xuxueli 2015-12-12 18:29:02
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionLimit {

    /**
     * 是否需要登录 (默认需要)
     */
    boolean limit() default true;

    /**
     * 是否要求管理员权限
     */
    boolean adminUser() default false;

}