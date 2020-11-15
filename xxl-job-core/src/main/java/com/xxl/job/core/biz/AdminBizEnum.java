package com.xxl.job.core.biz;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调度中心地址类型枚举.
 *
 * @author smilesnake
 */
@Getter
@AllArgsConstructor
public enum AdminBizEnum {
    /**
     * 回调.
     */
    CALLBACK("callback"),
    /**
     * 注册.
     */
    REGISTRY("registry"),
    /**
     * 移除注册.
     */
    REGISTRY_REMOVE("registryRemove");
    /**
     * 类型.
     */
    private String type;
}
