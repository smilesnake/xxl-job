package com.xxl.job.admin.core.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 执行器信息.
 *
 * @author xuxueli on 16/9/30.
 */
@Setter
public class XxlJobGroup {
    /**
     * 执行器id
     */
    @Getter
    private int id;
    /**
     * 执行器名称
     */
    @Getter
    private String appname;
    /**
     * 执行器标题
     */
    @Getter
    private String title;
    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    @Getter
    private int addressType;
    /**
     * 执行器地址列表，多地址逗号分隔(手动录入)
     */
    @Getter
    private String addressList;
    /**
     * 更新时间
     */
    @Getter
    private Date updateTime;
    /**
     * 执行器地址列表(系统注册)
     */
    private List<String> registryList;

    /**
     * 获取执行器地址列表.
     *
     * @return 执行器地址列表
     */
    public List<String> getRegistryList() {
        if (StringUtils.isNotBlank(addressList)) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(",")));
        }
        return registryList;
    }
}
