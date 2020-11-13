package com.xxl.job.core.biz.client;

import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.Param;
import com.xxl.job.core.enums.BizUriEnum;
import com.xxl.job.core.util.XxlJobRemotingUtil;

/**
 * 系统api调度客户端
 *
 * @author xuxueli 2017-07-28 22:14:52
 */
public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }

    /**
     * 地址url
     */
    private String addressUrl;
    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 超时时间（秒）
     */
    private final int timeout = 3;


    @Override
    public ReturnT<String> beat() {
        return XxlJobRemotingUtil.postBody(addressUrl + BizUriEnum.BEAT.getUri(), accessToken, timeout, null, String.class);
    }

    @Override
    public ReturnT<String> idleBeat(Param idleBeatParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + BizUriEnum.IDLE_BEAT.getUri(), accessToken, timeout, idleBeatParam, String.class);
    }

    @Override
    public ReturnT<String> run(Param triggerParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + BizUriEnum.RUN.getUri(), accessToken, timeout, triggerParam, String.class);
    }

    @Override
    public ReturnT<String> kill(Param killParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + BizUriEnum.KILL.getUri(), accessToken, timeout, killParam, String.class);
    }

    @Override
    public ReturnT<LogResult> log(Param logParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + BizUriEnum.LOG.getUri(), accessToken, timeout, logParam, LogResult.class);
    }

}
