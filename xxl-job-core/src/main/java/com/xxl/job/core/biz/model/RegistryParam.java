package com.xxl.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 注册参数.
 *
 * @author xuxueli on 2017-05-10 20:22:42
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistryParam implements Param {

    private static final long serialVersionUID = 42L;
    /**
     * 注册的执行器
     */
    private String registryGroup;
    /**
     * 注册的appName
     */
    private String registryKey;
    /**
     * 注册的地址
     */
    private String registryValue;
}
