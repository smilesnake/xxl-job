package com.xxl.job.admin.core.util;

import org.apache.commons.lang.ArrayUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cookie工具类.
 *
 * @author xuxueli 2015-12-12 18:01:06
 */
public class CookieUtil {

    private CookieUtil() {
    }

    /**
     * 默认缓存时间,单位/秒, 2H.
     */
    private static final int COOKIE_MAX_AGE = Integer.MAX_VALUE;
    /**
     * 保存路径,根路径.
     */
    private static final String COOKIE_PATH = "/";

    /**
     * 保存.
     *
     * @param response   response实体
     * @param key        缓存key
     * @param value      缓存value
     * @param ifRemember 是否记住密码
     */
    public static void set(HttpServletResponse response, String key, String value, boolean ifRemember) {
        int age = ifRemember ? COOKIE_MAX_AGE : -1;
        set(response, key, value, null, COOKIE_PATH, age, true);
    }

    /**
     * 设置Cookie.
     *
     * @param response   response实体
     * @param key        缓存key
     * @param value      缓存value
     * @param domain     域
     * @param path       路径
     * @param maxAge     最大缓存时间
     * @param isHttpOnly 是否为http
     */
    private static void set(HttpServletResponse response, String key, String value, String domain, String path, int maxAge, boolean isHttpOnly) {
        Cookie cookie = new Cookie(key, value);
        if (domain != null) {
            cookie.setDomain(domain);
        }
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(isHttpOnly);
        response.addCookie(cookie);
    }

    /**
     * 查询value.
     *
     * @param request request实体
     * @param key     缓存key
     * @return 缓存value
     */
    public static String getValue(HttpServletRequest request, String key) {
        Cookie cookie = get(request, key);
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * 查询Cookie.
     *
     * @param request request实体
     * @param key     缓存key
     */
    private static Cookie get(HttpServletRequest request, String key) {
        Cookie[] arrCookie = request.getCookies();
        if (ArrayUtils.isNotEmpty(arrCookie)) {
            for (Cookie cookie : arrCookie) {
                if (cookie.getName().equals(key)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * 删除Cookie
     *
     * @param request  request实体
     * @param response response实体
     * @param key      缓存key
     */
    public static void remove(HttpServletRequest request, HttpServletResponse response, String key) {
        Cookie cookie = get(request, key);
        if (cookie != null) {
            set(response, key, "", null, COOKIE_PATH, 0, true);
        }
    }

}