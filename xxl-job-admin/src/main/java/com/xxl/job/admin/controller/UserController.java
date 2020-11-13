package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.core.biz.model.ReturnT;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 用户管理
 *
 * @author xuxueli 2019-05-04 16:39:50
 */
@Controller
@RequestMapping("/user")
public class UserController {

  @Resource
  private XxlJobUserDao xxlJobUserDao;
  @Resource
  private XxlJobGroupDao xxlJobGroupDao;

  /**
   * 用户管理界面
   *
   * @param model 视图
   * @return 页面地址
   */
  @RequestMapping
  @PermissionLimit(adminUser = true)
  public String index(Model model) {

    // 执行器列表
    List<XxlJobGroup> groupList = xxlJobGroupDao.findAll();
    model.addAttribute("groupList", groupList);

    return "user/user.index";
  }

  /**
   * 分页
   *
   * @param start    页码
   * @param length   页面大小
   * @param username 用户名
   * @param role     角色(0-普通用户、1-管理员)
   * @return 用户分页信息
   */
  @PostMapping("/pageList")
  @ResponseBody
  @PermissionLimit(adminUser = true)
  public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
      @RequestParam(required = false, defaultValue = "10") int length,
      String username, int role) {

    // 分页列表
    List<XxlJobUser> list = xxlJobUserDao.pageList(start, length, username, role);
    int count = xxlJobUserDao.pageListCount(username, role);
    // 过滤
    if (!CollectionUtils.isEmpty(list)) {
      for (XxlJobUser item : list) {
        item.setPassword(null);
      }
    }

    // 结果包装
    Map<String, Object> maps = new HashMap<>();
    // 总记录数
    maps.put("recordsTotal", count);
    // 过滤后的总记录数
    maps.put("recordsFiltered", count);
    // 分页列表
    maps.put("data", list);
    return maps;
  }

  /**
   * 添加用户
   *
   * @param xxlJobUser 用户信息
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/add")
  @ResponseBody
  @PermissionLimit(adminUser = true)
  public ReturnT<String> add(XxlJobUser xxlJobUser) {

    // 验证用户名
    if (!StringUtils.hasText(xxlJobUser.getUsername())) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username"));
    }
    xxlJobUser.setUsername(xxlJobUser.getUsername().trim());
    if (!(xxlJobUser.getUsername().length() >= 4 && xxlJobUser.getUsername().length() <= 20)) {
      return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
    }
    //  验证密码
    if (!StringUtils.hasText(xxlJobUser.getPassword())) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          I18nUtil.getString("system_please_input") + I18nUtil.getString("user_password"));
    }
    xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
    if (!(xxlJobUser.getPassword().length() >= 4 && xxlJobUser.getPassword().length() <= 20)) {
      return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
    }
    // 加密后的密码
    xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));

    // 检查是否存在用户
    XxlJobUser existUser = xxlJobUserDao.loadByUserName(xxlJobUser.getUsername());
    if (existUser != null) {
      return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat"));
    }

    // 保存
    xxlJobUserDao.save(xxlJobUser);
    return ReturnT.SUCCESS;
  }

  /**
   * 更新用户信息
   *
   * @param request    request请求
   * @param xxlJobUser 用户信息
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/update")
  @ResponseBody
  @PermissionLimit(adminUser = true)
  public ReturnT<String> update(HttpServletRequest request, XxlJobUser xxlJobUser) {

    // 确保为当前用户
    XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
    if (loginUser.getUsername().equals(xxlJobUser.getUsername())) {
      return new ReturnT<>(ReturnT.FAIL.getCode(),
          I18nUtil.getString("user_update_loginuser_limit"));
    }

    // 验证密码
    if (StringUtils.hasText(xxlJobUser.getPassword())) {
      xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
      if (!(xxlJobUser.getPassword().length() >= 4 && xxlJobUser.getPassword().length() <= 20)) {
        return new ReturnT<>(ReturnT.FAIL_CODE,
            I18nUtil.getString("system_lengh_limit") + "[4-20]");
      }
      // md5加密密码
      xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));
    } else {
      xxlJobUser.setPassword(null);
    }

    // write
    xxlJobUserDao.update(xxlJobUser);
    return ReturnT.SUCCESS;
  }

  /**
   * 删除用户
   *
   * @param request request请求
   * @param id      用户id
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/remove")
  @ResponseBody
  @PermissionLimit(adminUser = true)
  public ReturnT<String> remove(HttpServletRequest request, int id) {

    // 确保为当前用户
    XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
    if (loginUser.getId() == id) {
      return new ReturnT<>(ReturnT.FAIL.getCode(),
          I18nUtil.getString("user_update_loginuser_limit"));
    }

    xxlJobUserDao.delete(id);
    return ReturnT.SUCCESS;
  }

  /**
   * 更新密码
   *
   * @param request  request请求
   * @param password 密码
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/updatePwd")
  @ResponseBody
  public ReturnT<String> updatePwd(HttpServletRequest request, String password) {

    // 验证密码
    if (password == null || password.trim().length() == 0) {
      return new ReturnT<>(ReturnT.FAIL.getCode(), "密码不可为空");
    }
    password = password.trim();
    if (!(password.length() >= 4 && password.length() <= 20)) {
      return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
    }

    // md5加密密码
    String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

    // 更新密码
    XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
    XxlJobUser existUser = xxlJobUserDao.loadByUserName(loginUser.getUsername());
    existUser.setPassword(md5Password);
    xxlJobUserDao.update(existUser);
    return ReturnT.SUCCESS;
  }
}