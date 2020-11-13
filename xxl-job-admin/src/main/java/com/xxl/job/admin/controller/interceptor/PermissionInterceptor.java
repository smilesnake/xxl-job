package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.service.LoginService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * 权限拦截器
 *
 * @author xuxueli 2015-12-12 18:09:04
 */
@Component
public class PermissionInterceptor extends HandlerInterceptorAdapter {

  @Resource
  private LoginService loginService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    if (!(handler instanceof HandlerMethod)) {
      return super.preHandle(request, response, handler);
    }

    // 是否需要登录
    boolean needLogin = true;
    //是否要求管理员权限
    boolean needAdminUser = false;
    HandlerMethod method = (HandlerMethod) handler;
    PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
    if (permission != null) {
      needLogin = permission.limit();
      needAdminUser = permission.adminUser();
    }

    if (needLogin) {
      //如果没有找到登录用户，直接跳转到登录页
      XxlJobUser loginUser = loginService.ifLogin(request, response);
      if (loginUser == null) {
        response.setStatus(302);
        response.setHeader("location", request.getContextPath() + "/toLogin");
        return false;
      }
      // 权限不足，直接抛出异常
      if (needAdminUser && loginUser.getRole() != 1) {
        throw new XxlJobException(I18nUtil.getString("system_permission_limit"));
      }
      request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
    }

    return super.preHandle(request, response, handler);
  }

}
