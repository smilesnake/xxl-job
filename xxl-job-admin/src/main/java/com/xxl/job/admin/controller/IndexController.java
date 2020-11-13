package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 运行报表及登录管理
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
public class IndexController {

    @Resource
    private XxlJobService xxlJobService;
    @Resource
    private LoginService loginService;

    /**
     * 首页
     *
     * @param model 视图
     * @return 指定的页面视图
     */
    @GetMapping("/")
    public String index(Model model) {

        Map<String, Object> dashboardMap = xxlJobService.dashboardInfo();
        model.addAllAttributes(dashboardMap);
        return "index";
    }

    /**
     * 首页图表信息.
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 统计信息
     */
    @ResponseBody
    @PostMapping("/chartInfo")
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        return xxlJobService.chartInfo(startDate, endDate);
    }

    /**
     * 去登录.
     *
     * @param request      request请求实体
     * @param response     response请求实体
     * @param modelAndView 视图
     * @return 已登录页面或登录页面
     */
    @GetMapping("/toLogin")
    @PermissionLimit(limit = false)
    public ModelAndView toLogin(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView) {
        if (loginService.ifLogin(request, response) != null) {
            modelAndView.setView(new RedirectView("/", true, false));
            return modelAndView;
        }
        return new ModelAndView("login");
    }

    /**
     * 登录.
     *
     * @param userName   用户名
     * @param password   密码
     * @param ifRemember 是否记住密码
     * @return 登录成功或失败
     */
    @PostMapping(value = "login")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> loginDo(HttpServletResponse response, String userName, String password, String ifRemember) {
        boolean ifRem = ifRemember != null && ifRemember.trim().length() > 0 && "on".equals(ifRemember);
        return loginService.login(response, userName, password, ifRem);
    }

    /**
     * 退出
     *
     * @param request  request请求实体
     * @param response request响应实体
     * @return 退出
     */
    @PostMapping(value = "logout")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return loginService.logout(request, response);
    }

    /**
     * 帮助页
     *
     * @return 帮助页
     */
    @GetMapping("/help")
    public String help() {
        return "help";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
