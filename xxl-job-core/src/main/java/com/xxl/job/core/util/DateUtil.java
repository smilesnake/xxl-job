package com.xxl.job.core.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 日期工具类.
 *
 * @author xuxueli 2018-08-19 01:24:11
 */
@Slf4j
public class DateUtil {
    private DateUtil() {
    }

    // ---------------------- format parse ----------------------
    /**
     * 日期格式化表达式
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    /**
     * 日期时间格式化表达式
     */
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期格式化对象-线程缓存.
     */
    private static final ThreadLocal<Map<String, DateFormat>> DATE_FORMAT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 得到日期格式化对象
     *
     * @param pattern 格式表达式
     * @return 日期格式化对象
     */
    private static DateFormat getDateFormat(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            throw new IllegalArgumentException("pattern cannot be empty.");
        }

        Map<String, DateFormat> dateFormatMap = DATE_FORMAT_THREAD_LOCAL.get();
        if (dateFormatMap != null && dateFormatMap.containsKey(pattern)) {
            return dateFormatMap.get(pattern);
        }

        synchronized (DATE_FORMAT_THREAD_LOCAL) {
            if (dateFormatMap == null) {
                dateFormatMap = new HashMap<>();
            }
            dateFormatMap.put(pattern, new SimpleDateFormat(pattern));
            DATE_FORMAT_THREAD_LOCAL.set(dateFormatMap);
        }

        return dateFormatMap.get(pattern);
    }

    /**
     * 格式化日期. 例如 "yyyy-MM-dd"
     *
     * @param date 日期
     * @return 解析得到的日期字符串
     */
    public static String formatDate(LocalDateTime date) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return df.format(date);
    }

    /**
     * 格式化日期. 例如 "yyyy-MM-dd HH:mm:ss"
     *
     * @param date 日期
     * @return 解析得到的日期字符串
     */
    public static String formatDateTime(Date date) {
        return format(date, DATETIME_FORMAT);
    }

    /**
     * 格式化日期
     *
     * @param date   日期
     * @param patten 表达式
     * @return 解析得到的日期字符串
     */
    public static String format(Date date, String patten) {
        return getDateFormat(patten).format(date);
    }

    /**
     * 解析日期字符串, like "yyyy-MM-dd HH:mm:ss"
     *
     * @param dateString 日期字符串
     * @return 解析得到的日期
     */
    public static Date parseDateTime(String dateString) {
        return parse(dateString, DATETIME_FORMAT);
    }

    /**
     * 解析日期
     *
     * @param dateString 日期字符串
     * @param pattern    表达式
     * @return 解析得到的日期
     */
    public static Date parse(String dateString, String pattern) {
        try {
            return getDateFormat(pattern).parse(dateString);
        } catch (ParseException e) {
            log.warn("parse date error, dateString = {}, pattern={}; errorMsg = {}", dateString, pattern, e.getMessage());
            return null;
        }
    }


    // ---------------------- add date ----------------------

    /**
     * 添加年.
     *
     * @param date   待添加的日期
     * @param amount 数额
     * @return 增加后的日期
     */
    public static Date addYears(final Date date, final int amount) {
        return add(date, Calendar.YEAR, amount);
    }

    /**
     * 添加月.
     *
     * @param date   待添加的日期
     * @param amount 数额
     * @return 增加后的日期
     */
    public static Date addMonths(final Date date, final int amount) {
        return add(date, Calendar.MONTH, amount);
    }

    /**
     * 添加日.
     *
     * @param date   待添加的日期
     * @param amount 数额
     * @return 增加后的日期
     */
    public static LocalDateTime addDays(final LocalDateTime date, final int amount) {
        return date.plusDays(amount);
    }

    /**
     * 添加分.
     *
     * @param date   待添加的日期
     * @param amount 数额
     * @return 增加后的日期
     */
    public static Date addMinutes(final Date date, final int amount) {
        return add(date, Calendar.MINUTE, amount);
    }

    /**
     * 添加秒.
     *
     * @param date          待添加的年日期
     * @param calendarField 待添加的类型
     * @param amount        数额
     * @return 增加后的日期
     */
    private static Date add(final Date date, final int calendarField, final int amount) {
        if (date == null) {
            return null;
        }
        final Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }

}