package com.xxl.job.core.biz.client;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.AdminBizEnum;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.XxlJobRemotingUtil;

import java.util.List;

/**
 * 调度远程api客户端(admin api test)
 *
 * @author xuxueli 2017-07-28 22:14:52
 */
public class AdminBizClient implements AdminBiz {
    /**
     * 调度中心部署根地址.
     */
    private String addressUrl;
    /**
     * 访问令牌.
     */
    private String accessToken;
    /**
     * 调度超时时间.
     */
    private int timeout = 3;

    /**
     * 初始化调度中心客户端对象
     *
     * @param addressUrl  调度中心部署根地址
     * @param accessToken 访问令牌
     */
    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        //最后位置必须有'/'
        final String lastSign = "/";
        if (!lastSign.endsWith(this.addressUrl)) {
            this.addressUrl = this.addressUrl + lastSign;
        }
    }

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/" + AdminBizEnum.CALLBACK.getType(), accessToken, timeout, callbackParamList, String.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/" + AdminBizEnum.REGISTRY.getType(), accessToken, timeout, registryParam, String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/" + AdminBizEnum.REGISTRY_REMOVE.getType(), accessToken, timeout, registryParam, String.class);
    }
}
