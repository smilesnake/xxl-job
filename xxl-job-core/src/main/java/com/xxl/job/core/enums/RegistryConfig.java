package com.xxl.job.core.enums;

/**
 * 注册基本配置.
 *
 * @author xuxueli on 17/5/10.
 */
public class RegistryConfig {

    /**
     * 心跳检测超时时间（s）.
     */
    public static final int BEAT_TIMEOUT = 30;
    /**
     * 死亡检测超时时间（s）.
     */
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    /**
     * 注册的类型.
     */
    public enum RegistryType {
        /**
         * 执行器注册.
         */
        EXECUTOR,
        /**
         * 管理员注册.
         */
        ADMIN
    }

}
