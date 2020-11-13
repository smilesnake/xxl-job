package com.xxl.job.admin.controller;


import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.AdminBizEnum;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 远程api调用接口
 *
 * @author xuxueli
 * @date 17/5/10
 */
@RestController
@RequestMapping("/api")
public class JobApiController {

  @Resource
  private AdminBiz adminBiz;

  /**
   * 远程api调用
   *
   * @param uri  请求类型
   * @param data 请求数据
   * @return
   * @see AdminBizClient#callback(List)
   * @see AdminBizClient#registry(RegistryParam)
   * @see AdminBizClient#registryRemove(RegistryParam)
   */
  @PostMapping("/{uri}")
  @PermissionLimit(limit = false)
  public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri,
      @RequestBody(required = false) String data) {

    // 验证
    if (StringUtils.isBlank(uri)) {
      return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
    }
    if (StringUtils.isNotBlank(XxlJobAdminConfig.getAdminConfig().getAccessToken())
        && !XxlJobAdminConfig.getAdminConfig().getAccessToken()
        .equals(request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN))) {
      return new ReturnT<>(ReturnT.FAIL_CODE, "The access token is wrong.");
    }

    // 服务映射
    if (AdminBizEnum.CALLBACK.getType().equals(uri)) {
      List<HandleCallbackParam> callbackParamList = GsonTool
          .fromJson(data, List.class, HandleCallbackParam.class);
      return adminBiz.callback(callbackParamList);
    } else if (AdminBizEnum.REGISTRY.getType().equals(uri)) {
      RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
      return adminBiz.registry(registryParam);
    } else if (AdminBizEnum.REGISTRY_REMOVE.getType().equals(uri)) {
      RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
      return adminBiz.registryRemove(registryParam);
    } else {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          "invalid request, uri-mapping(" + uri + ") not found.");
    }
  }
}