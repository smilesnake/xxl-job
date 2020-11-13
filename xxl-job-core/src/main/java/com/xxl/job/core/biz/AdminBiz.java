package com.xxl.job.core.biz;

import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;

import java.util.List;

/**
 * 调度中心
 *
 * @author xuxueli 2017-07-27 21:52:49
 */
public interface AdminBiz {


    // ---------------------- callback ----------------------

    /**
     * RPC回调
     *
     * @param callbackParamList 回调参数列表
     * @return 成功，ReturnT.SUCCESS.否则,ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);


    // ---------------------- registry ----------------------

    /**
     * RPC 注册.
     *
     * @param registryParam 注册参数
     * @return 成功，ReturnT.SUCCESS.否则,ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<String> registry(RegistryParam registryParam);

    /**
     * RPC 移除注册.
     *
     * @param registryParam 注册参数
     * @return 成功，ReturnT.SUCCESS.否则,ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<String> registryRemove(RegistryParam registryParam);


    // ---------------------- biz (custome) ----------------------
    // group、job ... manage

}
