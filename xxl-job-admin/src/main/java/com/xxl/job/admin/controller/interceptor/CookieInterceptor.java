package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.core.util.FtlUtil;
import com.xxl.job.admin.core.util.I18nUtil;
import java.util.HashMap;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;


/**
 * 存放cookies到模型的cookieMap中去(push cookies to model as cookieMap).
 *
 * @author xuxueli 2015-12-12 18:09:04
 */
@Component
public class CookieInterceptor extends HandlerInterceptorAdapter {

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) throws Exception {

    // cookie
    if (modelAndView != null && ArrayUtils.isNotEmpty(request.getCookies())) {
      HashMap<String, Cookie> cookieMap = new HashMap<>();
      for (Cookie ck : request.getCookies()) {
        cookieMap.put(ck.getName(), ck);
      }
      modelAndView.addObject("cookieMap", cookieMap);
    }

    // 如果没有模型，那么生成TemplateHashModel对象
    if (modelAndView != null) {
      modelAndView.addObject("I18nUtil", FtlUtil.generateStaticModel(I18nUtil.class.getName()));
    }

    super.postHandle(request, response, handler, modelAndView);
  }

}
