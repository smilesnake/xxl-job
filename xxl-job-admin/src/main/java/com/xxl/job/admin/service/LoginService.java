package com.xxl.job.admin.service;

import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.CookieUtil;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.core.util.JacksonUtil;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.core.biz.model.ReturnT;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * 登录Service
 *
 * @author xuxueli 2019-05-04 22:13:264
 */
@Configuration
public class LoginService {
    /**
     * 登录标识key.
     */
    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    /**
     * 解析token
     *
     * @param tokenHex token 16进制码
     * @return 解析后的
     */
    private XxlJobUser parseToken(String tokenHex) {
        XxlJobUser xxlJobUser = null;
        if (tokenHex != null) {
            // username_password(md5)
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());
            xxlJobUser = JacksonUtil.readValue(tokenJson, XxlJobUser.class);
        }
        return xxlJobUser;
    }

    /**
     * 登录
     *
     * @param response   response响应体
     * @param username   用户名
     * @param password   密码
     * @param ifRemember 是否记得密码
     * @return ReturnT.SUCCESS，登录成功，否则，失败
     */
    public ReturnT<String> login(HttpServletResponse response, String username, String password, boolean ifRemember) {

        // param
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_empty"));
        }

        // 验证密码
        XxlJobUser xxlJobUser = xxlJobUserDao.loadByUserName(username);
        if (xxlJobUser == null) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(xxlJobUser.getPassword())) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }

        String tokenJson = JacksonUtil.writeValueAsString(xxlJobUser);
        if (tokenJson != null) {
            String loginToken = new BigInteger(tokenJson.getBytes()).toString(16);
            CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);
            // 登录操作
            return ReturnT.SUCCESS;
        } else {
            return new ReturnT<>(500, I18nUtil.getString("login_fail"));
        }


    }

    /**
     * 登出.
     *
     * @param request  request请求体
     * @param response response响应体
     */
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }

    /**
     * 判断是否登录.
     *
     * @param request  request请求实体
     * @param response response请求实体
     * @return 登录用户
     */
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response) {
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (cookieToken != null) {
            XxlJobUser cookieUser = null;
            try {
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
            }
            if (cookieUser != null) {
                XxlJobUser dbUser = xxlJobUserDao.loadByUserName(cookieUser.getUsername());
                if (dbUser != null && cookieUser.getPassword().equals(dbUser.getPassword())) {
                    return dbUser;

                }
            }
        }
        return null;
    }


}
