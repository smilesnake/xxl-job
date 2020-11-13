package com.xxl.job.admin.core.model;

import com.xxl.job.core.enums.RegistryConfig;
import lombok.Data;

import java.time.LocalDate;

/**
 * 注册信息实体.
 *
 * @author xuxueli
 * @date 16/9/30
 */
@Data
public class XxlJobRegistry {
    /**
     * 主键id.
     */
    private int id;
    /**
     * 注册的执行器.如：EXECUTOR
     *
     * @see RegistryConfig.RegistryType
     */
    private String registryGroup;
    /**
     * 注册的名称，appName.如：xxl-job-executor-sample
     */
    private String registryKey;
    /**
     * 注册的服务器url地址，如:http://192.168.58.1:9999/
     */
    private String registryValue;
    /**
     * 更新时间.
     */
    private LocalDate updateTime;
}
