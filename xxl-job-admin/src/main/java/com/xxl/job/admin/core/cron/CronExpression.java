/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.xxl.job.admin.core.cron;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Provides a parser and evaluator for unix-like cron expressions. Cron expressions provide the
 * ability to specify complex time combinations such as &quot;At 8:00am every Monday through
 * Friday&quot; or &quot;At 1:30am every last Friday of the month&quot;.
 * <P>
 * Cron expressions are comprised of 6 required fields and one optional field separated by white
 * space. The fields respectively are described as follows:
 *
 * <table cellspacing="8">
 * <tr>
 * <th align="left">Field Name</th>
 * <th align="left">&nbsp;</th>
 * <th align="left">Allowed Values</th>
 * <th align="left">&nbsp;</th>
 * <th align="left">Allowed Special Characters</th>
 * </tr>
 * <tr>
 * <td align="left"><code>Seconds</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-59</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Minutes</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-59</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Hours</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-23</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Day-of-month</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>1-31</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * ? / L W</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Month</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-11 or JAN-DEC</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Day-of-Week</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>1-7 or SUN-SAT</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * ? / L #</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Year (Optional)</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>empty, 1970-2199</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * </table>
 * <P>
 * The '*' character is used to specify all values. For example, &quot;*&quot; in the minute field
 * means &quot;every minute&quot;.
 * <P>
 * The '?' character is allowed for the day-of-month and day-of-week fields. It is used to specify
 * 'no specific value'. This is useful when you need to specify something in one of the two fields,
 * but not the other.
 * <P>
 * The '-' character is used to specify ranges For example &quot;10-12&quot; in the hour field means
 * &quot;the hours 10, 11 and 12&quot;.
 * <P>
 * The ',' character is used to specify additional values. For example &quot;MON,WED,FRI&quot; in
 * the day-of-week field means &quot;the days Monday, Wednesday, and Friday&quot;.
 * <P>
 * The '/' character is used to specify increments. For example &quot;0/15&quot; in the seconds
 * field means &quot;the seconds 0, 15, 30, and 45&quot;. And &quot;5/15&quot; in the seconds field
 * means &quot;the seconds 5, 20, 35, and 50&quot;.  Specifying '*' before the  '/' is equivalent to
 * specifying 0 is the value to start with. Essentially, for each field in the expression, there is
 * a set of numbers that can be turned on or off. For seconds and minutes, the numbers range from 0
 * to 59. For hours 0 to 23, for days of the month 0 to 31, and for months 0 to 11 (JAN to DEC). The
 * &quot;/&quot; character simply helps you turn on every &quot;nth&quot; value in the given set.
 * Thus &quot;7/6&quot; in the month field only turns on month &quot;7&quot;, it does NOT mean every
 * 6th month, please note that subtlety.
 * <P>
 * The 'L' character is allowed for the day-of-month and day-of-week fields. This character is
 * short-hand for &quot;last&quot;, but it has different meaning in each of the two fields. For
 * example, the value &quot;L&quot; in the day-of-month field means &quot;the last day of the
 * month&quot; - day 31 for January, day 28 for February on non-leap years. If used in the
 * day-of-week field by itself, it simply means &quot;7&quot; or &quot;SAT&quot;. But if used in the
 * day-of-week field after another value, it means &quot;the last xxx day of the month&quot; - for
 * example &quot;6L&quot; means &quot;the last friday of the month&quot;. You can also specify an
 * offset from the last day of the month, such as "L-3" which would mean the third-to-last day of
 * the calendar month. <i>When using the 'L' option, it is important not to specify lists, or ranges
 * of values, as you'll get confusing/unexpected results.</i>
 * <P>
 * The 'W' character is allowed for the day-of-month field.  This character is used to specify the
 * weekday (Monday-Friday) nearest the given day.  As an example, if you were to specify
 * &quot;15W&quot; as the value for the day-of-month field, the meaning is: &quot;the nearest
 * weekday to the 15th of the month&quot;. So if the 15th is a Saturday, the trigger will fire on
 * Friday the 14th. If the 15th is a Sunday, the trigger will fire on Monday the 16th. If the 15th
 * is a Tuesday, then it will fire on Tuesday the 15th. However if you specify &quot;1W&quot; as the
 * value for day-of-month, and the 1st is a Saturday, the trigger will fire on Monday the 3rd, as it
 * will not 'jump' over the boundary of a month's days.  The 'W' character can only be specified
 * when the day-of-month is a single day, not a range or list of days.
 * <P>
 * The 'L' and 'W' characters can also be combined for the day-of-month expression to yield 'LW',
 * which translates to &quot;last weekday of the month&quot;.
 * <P>
 * The '#' character is allowed for the day-of-week field. This character is used to specify
 * &quot;the nth&quot; XXX day of the month. For example, the value of &quot;6#3&quot; in the
 * day-of-week field means the third Friday of the month (day 6 = Friday and &quot;#3&quot; = the
 * 3rd one in the month). Other examples: &quot;2#1&quot; = the first Monday of the month and
 * &quot;4#5&quot; = the fifth Wednesday of the month. Note that if you specify &quot;#5&quot; and
 * there is not 5 of the given day-of-week in the month, then no firing will occur that month.  If
 * the '#' character is used, there can only be one expression in the day-of-week field
 * (&quot;3#1,6#3&quot; is not valid, since there are two expressions).
 * <P>
 * <!--The 'C' character is allowed for the day-of-month and day-of-week fields. This character is
 * short-hand for "calendar". This means values are calculated against the associated calendar, if
 * any. If no calendar is associated, then it is equivalent to having an all-inclusive calendar. A
 * value of "5C" in the day-of-month field means "the first day included by the calendar on or after
 * the 5th". A value of "1C" in the day-of-week field means "the first day included by the calendar
 * on or after Sunday".-->
 * <P>
 * The legal characters and the names of months and days of the week are not case sensitive.
 *
 * <p>
 * <b>NOTES:</b>
 * <ul>
 * <li>Support for specifying both a day-of-week and a day-of-month value is
 * not complete (you'll need to use the '?' character in one of these fields).
 * </li>
 * <li>Overflowing ranges is supported - that is, having a larger number on
 * the left hand side than the right. You might do 22-2 to catch 10 o'clock at night until 2 o'clock
 * in the morning, or you might have NOV-FEB. It is very important to note that overuse of
 * overflowing ranges creates ranges that don't make sense and no effort has been made to determine
 * which interpretation CronExpression chooses. An example would be "0 0 14-6 ? * FRI-MON". </li>
 * </ul>
 * </p>
 *
 * @author Sharada Jambula, James House
 * @author Contributions from Mads Henderson
 * @author Refactoring from CronTrigger to CronExpression by Aaron Craven
 * <p>
 * Borrowed from quartz v2.3.1
 */
public final class CronExpression implements Serializable, Cloneable {

    private static final long serialVersionUID = 12423409423L;
    /**
     * 秒
     */
    protected static final int SECOND = 0;
    /**
     * 分
     */
    protected static final int MINUTE = 1;
    /**
     * 小时
     */
    protected static final int HOUR = 2;
    /**
     * 天
     */
    protected static final int DAY_OF_MONTH = 3;
    /**
     * 月
     */
    protected static final int MONTH = 4;
    /**
     * 周
     */
    protected static final int DAY_OF_WEEK = 5;
    /**
     * 年
     */
    protected static final int YEAR = 6;
    /**
     * 星号 ,全部（*）
     */
    protected static final int ALL_SPEC_INT = 99;
    /**
     * 问号,不为该域指定值(?)
     */
    protected static final int NO_SPEC_INT = 98;
    /**
     * 星号
     */
    protected static final Integer ALL_SPEC = ALL_SPEC_INT;
    /**
     * 问号
     */
    protected static final Integer NO_SPEC = NO_SPEC_INT;
    /**
     * 月份集合<英文，表示数字>.
     */
    protected static final Map<String, Integer> monthMap = new HashMap<>(20);
    /**
     * 周集合<星期几的英文表示，数字表示>.
     */
    protected static final Map<String, Integer> dayMap = new HashMap<String, Integer>(60);

    /**
     * 构建静态信息
     */
    static {
        //月
        monthMap.put("JAN", 0);
        monthMap.put("FEB", 1);
        monthMap.put("MAR", 2);
        monthMap.put("APR", 3);
        monthMap.put("MAY", 4);
        monthMap.put("JUN", 5);
        monthMap.put("JUL", 6);
        monthMap.put("AUG", 7);
        monthMap.put("SEP", 8);
        monthMap.put("OCT", 9);
        monthMap.put("NOV", 10);
        monthMap.put("DEC", 11);
        // 周
        dayMap.put("SUN", 1);
        dayMap.put("MON", 2);
        dayMap.put("TUE", 3);
        dayMap.put("WED", 4);
        dayMap.put("THU", 5);
        dayMap.put("FRI", 6);
        dayMap.put("SAT", 7);
    }

    /**
     * cron表达式.
     */
    @Getter
    private final String cronExpression;
    /**
     * 时区.
     */
    @Setter
    private TimeZone timeZone = null;
    /**
     * cron转换后符合表达式的秒列表.
     */
    protected transient TreeSet<Integer> seconds;
    /**
     * cron转换后符合表达式的分列表.
     */
    protected transient TreeSet<Integer> minutes;
    /**
     * cron转换后符合表达式的小时列表.
     */
    protected transient TreeSet<Integer> hours;
    /**
     * cron转换后符合表达式的天列表.
     */
    protected transient TreeSet<Integer> daysOfMonth;
    /**
     * cron转换后符合表达式的月列表.
     */
    protected transient TreeSet<Integer> months;
    /**
     * cron转换后符合表达式的周列表.
     */
    protected transient TreeSet<Integer> daysOfWeek;
    /**
     * cron转换后符合表达式的年列表.
     */
    protected transient TreeSet<Integer> years;
    /**
     * 当前周的最后一天，{日期}占位符如果是"L"，即意味着当前周的最后一天触发(L),此时为true.
     */
    protected transient boolean lastdayOfWeek = false;
    /**
     * 第几周,"#" 用来指定具体的周数，"#"前面代表星期，"#"后面代表本月第几周，比如"2#2"表示本月第二周的星期一，"5#3"表示本月第三周的星期四，因此，"5L"这种形式只不过是"#"的特殊形式而已.#号只能在周域
     */
    protected transient int nthdayOfWeek = 0;
    /**
     * 是否当前最后的一天，{日期}占位符如果是"L"，即意味着当月的最后一天触发(L),此时为true.L只能在日域和周域
     */
    protected transient boolean lastdayOfMonth = false;
    /**
     * W: 表示有效工作日(周一到周五)（"W "意味着在本月内离当天最近的工作日触发，所谓最近工作日，即当天到工作日的前后最短距离，如果当天即为工作日，则距离为0；）.
     */
    protected transient boolean nearestWeekday = false;
    /**
     * 最后天的偏移量.
     */
    protected transient int lastdayOffset = 0;
    /**
     * 表达式是否解析.
     */
    protected transient boolean expressionParsed = false;
    /**
     * 当前日期最大的年，即最大为未来100年.
     */
    public static final int MAX_YEAR = LocalDate.now().plusYears(100).getYear();

    /**
     * 基于指定的参数构造新的<CODE>CronExpression</CODE>.
     *
     * @param cronExpression cron表达式的字符串表示
     * @throws ParseException 如果字符串表达式无法解析为有效的<CODE>CronExpression</CODE>，抛出
     */
    public CronExpression(String cronExpression) throws ParseException {
        if (cronExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null");
        }
        // 转成美语
        this.cronExpression = cronExpression.toUpperCase(Locale.US);

        buildExpression(this.cronExpression);
    }

    /**
     * 构造一个新的<CODE>CronExpression</CODE>作为现有实例的副本.
     *
     * @param expression 要复制的现有cron表达式
     */
    public CronExpression(CronExpression expression) {
        /*
         * We don't call the other constructor here since we need to swallow the
         * ParseException. We also elide some of the sanity checking as it is
         * not logically trippable.
         */
        // 调用构造函数,解析异常直接抛出断言错误
        this.cronExpression = expression.getCronExpression();
        try {
            buildExpression(cronExpression);
        } catch (ParseException ex) {
            throw new AssertionError();
        }
        // 拷贝时区
        if (expression.getTimeZone() != null) {
            setTimeZone((TimeZone) expression.getTimeZone().clone());
        }
    }

    /**
     * 给定日期是否满足cron表达式。
     * <p>请注意，毫秒被忽略，因此两个日期在同一秒的不同毫秒上，在这里总是有相同的结果。
     *
     * @param date 给定日期
     * @return 给定日期满足表达式，true,否则，抛出
     */
    public boolean isSatisfiedBy(Date date) {
        Calendar testDateCal = Calendar.getInstance(getTimeZone());
        testDateCal.setTime(date);
        testDateCal.set(Calendar.MILLISECOND, 0);
        //得到当前时间,毫秒被忽略
        Date originalDate = testDateCal.getTime();

        testDateCal.add(Calendar.SECOND, -1);

        Date timeAfter = getTimeAfter(testDateCal.getTime());

        return ((timeAfter != null) && (timeAfter.equals(originalDate)));
    }

    /**
     * 返回满足cron表达式的给定日期/时间 <I>之后 </I>的下一个日期/时间。
     *
     * @param date 开始搜索下一个有效日期/时间的日期/时间
     * @return 下一个有效日期/时间
     */
    public Date getNextValidTimeAfter(Date date) {
        return getTimeAfter(date);
    }

    /**
     * Returns the next date/time <I>after</I> the given date/time which does
     * <I>not</I> satisfy the expression
     *
     * @param date the date/time at which to begin the search for the next invalid date/time
     * @return the next valid date/time
     */
    public Date getNextInvalidTimeAfter(Date date) {
        long difference = 1000;

        //move back to the nearest second so differences will be accurate
        Calendar adjustCal = Calendar.getInstance(getTimeZone());
        adjustCal.setTime(date);
        adjustCal.set(Calendar.MILLISECOND, 0);
        Date lastDate = adjustCal.getTime();

        Date newDate;

        //FUTURE_TODO: (QUARTZ-481) IMPROVE THIS! The following is a BAD solution to this problem. Performance will be very bad here, depending on the cron expression. It is, however A solution.

        //keep getting the next included time until it's farther than one second
        // apart. At that point, lastDate is the last valid fire time. We return
        // the second immediately following it.
        while (difference == 1000) {
            newDate = getTimeAfter(lastDate);
            if (newDate == null) {
                break;
            }

            difference = newDate.getTime() - lastDate.getTime();

            if (difference == 1000) {
                lastDate = newDate;
            }
        }

        return new Date(lastDate.getTime() + 1000);
    }

    /**
     * 返回此<code>CronExpression</code>将的时区.
     */
    public TimeZone getTimeZone() {
        timeZone = timeZone == null ? TimeZone.getDefault() : timeZone;
        return timeZone;
    }

    /**
     * 返回<CODE>CronExpression</CODE>的字符串表示形式
     *
     * @return <CODE>CronExpression</CODE>的字符串表示
     */
    @Override
    public String toString() {
        return cronExpression;
    }

    /**
     * 验证是否为cron表达式，
     *
     * @param cronExpression 待验证的cron表达式
     * @return true, 给定表达式为有效的cron表达式, 否则，false
     */
    public static boolean isValidExpression(String cronExpression) {
        // 如果能成功构造出CronExpression，那么就说明可以解析
        try {
            new CronExpression(cronExpression);
        } catch (ParseException pe) {
            return false;
        }

        return true;
    }

    /**
     * 验证表达式.
     *
     * @param cronExpression 待验证的表达式
     * @throws ParseException 解析失败，抛出
     */
    public static void validateExpression(String cronExpression) throws ParseException {
        new CronExpression(cronExpression);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Expression Parsing Functions
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * 构建表达式
     *
     * @param expression cron 表达式
     */
    protected void buildExpression(String expression) throws ParseException {
        expressionParsed = true;

        try {
            // 初始化时、分、秒、年、月、日、周
            if (seconds == null) {
                seconds = new TreeSet<>();
            }
            if (minutes == null) {
                minutes = new TreeSet<>();
            }
            if (hours == null) {
                hours = new TreeSet<>();
            }
            if (daysOfMonth == null) {
                daysOfMonth = new TreeSet<>();
            }
            if (months == null) {
                months = new TreeSet<>();
            }
            if (daysOfWeek == null) {
                daysOfWeek = new TreeSet<>();
            }
            if (years == null) {
                years = new TreeSet<>();
            }

            // 表达式起始为秒，表达式类型
            int exprOn = SECOND;

            StringTokenizer exprsTok = new StringTokenizer(expression, " \t",
                    false);
            //存在分隔符且解析到年
            while (exprsTok.hasMoreTokens() && exprOn <= YEAR) {
                String expr = exprsTok.nextToken().trim();

                // 如果L与本月的其他日期一起使用，则抛出异常
                if (exprOn == DAY_OF_MONTH && expr.indexOf('L') != -1 && expr.length() > 1 && expr.contains(",")) {
                    throw new ParseException(
                            "Support for specifying 'L' and 'LW' with other days of the month is not implemented",
                            -1);
                }
                // 如果L与周中的其他日期一起使用，则引发异常
                if (exprOn == DAY_OF_WEEK && expr.indexOf('L') != -1 && expr.length() > 1 && expr
                        .contains(",")) {
                    throw new ParseException(
                            "Support for specifying 'L' with other days of the week is not implemented", -1);
                }
                // 如果#与周中的其他日期一起使用，则引发异常
                if (exprOn == DAY_OF_WEEK && expr.indexOf('#') != -1
                        && expr.indexOf('#', expr.indexOf('#') + 1) != -1) {
                    throw new ParseException(
                            "Support for specifying multiple \"nth\" days is not implemented.", -1);
                }

                // 如果存在指定的日期触发（即存在','号）,则进行拆分，然后递归调用
                StringTokenizer vTok = new StringTokenizer(expr, ",");
                while (vTok.hasMoreTokens()) {
                    String v = vTok.nextToken();
                    //存储表达式的值
                    storeExpressionVals(0, v, exprOn);
                }
                // 秒 -> 分钟 -> 小时 -> 天 -> 月 -> 周 依次判断
                exprOn++;
            }
            //到这里应该为YEAR，如果小于等于周，那么说明表达式不正确
            if (exprOn <= DAY_OF_WEEK) {
                throw new ParseException("Unexpected end of expression.",
                        expression.length());
            }

            if (exprOn <= YEAR) {
                //存储表达式的值（秒 -> 分钟 -> 小时 -> 天 -> 月 -> 周）
                storeExpressionVals(0, "*", YEAR);
            }

            // 得到周的值
            TreeSet<Integer> dow = getSet(DAY_OF_WEEK);
            // 得到天的值
            TreeSet<Integer> dom = getSet(DAY_OF_MONTH);

            // Copying the logic from the UnsupportedOperationException below
            //天数位置上有没有'?'
            boolean dayOfMSpec = !dom.contains(NO_SPEC);
            //周位置上有没有'?'
            boolean dayOfWSpec = !dow.contains(NO_SPEC);

            // 不支持同时指定星期几和月日参数
            if (!dayOfMSpec || dayOfWSpec) {
                if (!dayOfWSpec || dayOfMSpec) {
                    throw new ParseException(
                            "Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.",
                            0);
                }
            }
        } catch (ParseException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ParseException("Illegal cron expression format (" + e.toString() + ")", 0);
        }
    }

    /**
     * 存储表达式的值
     *
     * @param pos  下标
     * @param s    待保存的字符串
     * @param type 字符串在表达式中的类型
     * @return 字符计算的最后下标
     * @throws ParseException 解析失败，抛出
     */
    protected int storeExpressionVals(int pos, String s, int type)
            throws ParseException {

        int incr = 0;
        // 开始解析的下标
        int i = skipWhiteSpace(pos, s);
        // 判断是否超长
        if (i >= s.length()) {
            return i;
        }
        //起始字符
        char c = s.charAt(i);
        //对周进行解析
        if ((c >= 'A') && (c <= 'Z') && (!s.equals("L")) && (!s.equals("LW")) && (!s
                .matches("^L-[0-9]*[W]?"))) {
            //得到周的英文
            String sub = s.substring(i, i + 3);
            // 表示当前类型的第一个数值,没有即-1，如：周几：MON-FRI，此时sval=MON
            int sval = -1;
            // 表示当前类型的第二个数值,没有即-1，如：周几：MON-FRI，此时eval=FRI
            int eval = -1;
            if (type == MONTH) {
                //得到月份的值
                sval = getMonthNumber(sub) + 1;
                // 非法的月份
                if (sval <= 0) {
                    throw new ParseException("Invalid Month value: '" + sub + "'", i);
                }
                //存在两个月值的情况
                if (s.length() > i + 3) {
                    //得到连接两个月份之间的连接符
                    c = s.charAt(i + 3);
                    if (c == '-') {
                        // 跳至第二个月份值下标
                        i += 4;
                        //第二个月份的英文表示
                        sub = s.substring(i, i + 3);
                        //第二个月份的值
                        eval = getMonthNumber(sub) + 1;
                        //错误的月份值
                        if (eval <= 0) {
                            throw new ParseException("Invalid Month value: '" + sub + "'", i);
                        }
                    }
                }
            } else if (type == DAY_OF_WEEK) {
                //得到周的值
                sval = getDayOfWeekNumber(sub);
                // 非法的周值
                if (sval < 0) {
                    throw new ParseException("Invalid Day-of-Week value: '" + sub + "'", i);
                }
                //存在两个周值的情况
                if (s.length() > i + 3) {
                    //得到连接两个周值之间的连接符
                    c = s.charAt(i + 3);
                    //连接符为'-'
                    if (c == '-') {
                        // 跳至第二个周值下标
                        i += 4;
                        // 第二个周值的英文表示
                        sub = s.substring(i, i + 3);
                        //第二个周值
                        eval = getDayOfWeekNumber(sub);
                        if (eval < 0) {
                            throw new ParseException("Invalid Day-of-Week value: '" + sub + "'", i);
                        }
                    } else if (c == '#') {
                        //连接符为'#'
                        try {
                            // 跳至第二个周值下标
                            i += 4;
                            //设置为第N周
                            nthdayOfWeek = Integer.parseInt(s.substring(i));
                            //月份中不存在0周，最多也就四周，月份中没有第5周
                            if (nthdayOfWeek < 1 || nthdayOfWeek > 5) {
                                throw new Exception();
                            }
                        } catch (Exception e) {
                            throw new ParseException("A numeric value between 1 and 5 must follow the '#' option", i);
                        }
                    } else if (c == 'L') {
                        // 连接符为'L',表示星期的的最后一天触发，即星期六触发
                        lastdayOfWeek = true;
                        i++;
                    }
                }

            } else {
                //  此位置的非法字符，抛出
                throw new ParseException("Illegal characters for this position: '" + sub + "'", i);
            }
            if (eval != -1) {
                // 默认增值为1
                incr = 1;
            }
            //添加并设置
            addToSet(sval, eval, incr, type);
            return (i + 3);
        }

        if (c == '?') {
            i++;
            // 如果不为空格，不为制表符，并且有两个值，说明当前字符后为非法字符
            if ((i + 1) < s.length() && (s.charAt(i) != ' ' && s.charAt(i + 1) != '\t')) {
                throw new ParseException("Illegal character after '?': " + s.charAt(i), i);
            }

            // 只能指定月日或星期几
            if (type != DAY_OF_WEEK && type != DAY_OF_MONTH) {
                throw new ParseException("'?' can only be specified for Day-of-Month or Day-of-Week.", i);
            }
            //不是最后一天且类型为周
            if (type == DAY_OF_WEEK && !lastdayOfMonth) {
                int val = daysOfMonth.last();
                // 只能为“月日”或“星期几”指定
                if (val == NO_SPEC_INT) {
                    throw new ParseException("'?' can only be specified for Day-of-Month -OR- Day-of-Week.", i);
                }
            }

            addToSet(NO_SPEC_INT, -1, 0, type);
            return i;
        }
        //对'*'与'/'解析
        if (c == '*' || c == '/') {
            if (c == '*' && (i + 1) >= s.length()) {
                addToSet(ALL_SPEC_INT, -1, incr, type);
                return i + 1;
            } else if (c == '/' && ((i + 1) >= s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t')) {
                throw new ParseException("'/' must be followed by an integer.", i);
            } else if (c == '*') {
                //对字符为'*'的处理，不过还得判断 其后面是否还有增量
                i++;
            }
            c = s.charAt(i);
            //对'/'处理，判断其是否指定增量
            if (c == '/') {
                //下标加1
                i++;
                // 未知的字符串结束符
                if (i >= s.length()) {
                    throw new ParseException("Unexpected end of string.", i);
                }
                // 增量值
                incr = getNumericValue(s, i);

                i++;
                // 如果是两位数,下标还要再加1
                if (incr > 10) {
                    i++;
                }
                checkIncrementRange(incr, type, i);
            } else {
                // 默认增量为1
                incr = 1;
            }
            // 添加并设置
            addToSet(ALL_SPEC_INT, -1, incr, type);
            return i;
        } else if (c == 'L') {
            i++;
            //设置为最后一天
            if (type == DAY_OF_MONTH) {
                lastdayOfMonth = true;
            }
            // 设置最后一个星期N
            if (type == DAY_OF_WEEK) {
                addToSet(7, 7, 0, type);
            }
            // L后面还跟了值
            if (type == DAY_OF_MONTH && s.length() > i) {
                // 后面跟的值
                c = s.charAt(i);
                //范围
                if (c == '-') {
                    //设置最后一天的偏移量
                    ValueSet vs = getValue(0, s, i + 1);
                    lastdayOffset = vs.value;
                    if (lastdayOffset > 30) {
                        throw new ParseException("Offset from last day must be <= 30", i + 1);
                    }
                    i = vs.pos;
                }
                if (s.length() > i) {
                    c = s.charAt(i);
                    //设置为最近的工作日
                    if (c == 'W') {
                        nearestWeekday = true;
                        i++;
                    }
                }
            }
            return i;
        } else if (c >= '0' && c <= '9') {
            //当为指定数字的情况
            //1位数的值
            int val = Integer.parseInt(String.valueOf(c));
            //对下标加1
            i++;
            if (i >= s.length()) {
                addToSet(val, -1, -1, type);
            } else {
                // 两位数的值
                c = s.charAt(i);
                // 数字为两位数的情况
                if (c >= '0' && c <= '9') {
                    // 解析为值列表对象
                    ValueSet vs = getValue(val, s, i);
                    val = vs.value;
                    i = vs.pos;
                }
                // 对数据进行验证
                i = checkNext(i, s, val, type);
                return i;
            }
        } else {
            // 未知的字符,抛出
            throw new ParseException("Unexpected character: " + c, i);
        }
        // 字符计算的最后下标
        return i;
    }

    /**
     * 检查增量的范围
     *
     * @param incr   增量值
     * @param type   类型
     * @param idxPos 当前下标位
     * @throws ParseException 如果超出范围,抛出
     */
    private void checkIncrementRange(int incr, int type, int idxPos) throws ParseException {
        // 秒与分钟范围必须在0~59内
        if (incr > 59 && (type == SECOND || type == MINUTE)) {
            throw new ParseException("Increment > 60 : " + incr, idxPos);
        } else if (incr > 23 && (type == HOUR)) {
            // 小时范围必须在0~23内
            throw new ParseException("Increment > 24 : " + incr, idxPos);
        } else if (incr > 31 && (type == DAY_OF_MONTH)) {
            // 日范围必须在0~31内
            throw new ParseException("Increment > 31 : " + incr, idxPos);
        } else if (incr > 7 && (type == DAY_OF_WEEK)) {
            // 周范围必须在0~7内
            throw new ParseException("Increment > 7 : " + incr, idxPos);
        } else if (incr > 12 && (type == MONTH)) {
            // 月范围必须在0~31内
            throw new ParseException("Increment > 12 : " + incr, idxPos);
        }
    }

    /**
     * 检查下一个值
     *
     * @param pos  字符的下一个下标
     * @param s    cron表达式中的值
     * @param val  字符的值
     * @param type 字符串在表达式中的类型
     * @return 字符的最后下标
     * @throws ParseException 解析异常，抛出
     */
    protected int checkNext(int pos, String s, int val, int type)
            throws ParseException {
        //结束下标
        int end = -1;
        //起始下标
        int i = pos;
        //长度为两位数，或者字符的长度为3的情况
        if (i >= s.length()) {
            //直接添加并设置
            addToSet(val, end, -1, type);
            return i;
        }

        // 数值与数值之间的连接符
        char c = s.charAt(pos);

        // 占位符为"L"，即意味着当前周的最后一天触发(L),此时为true
        if (c == 'L') {
            if (type == DAY_OF_WEEK) {
                // 周的范围只能为1~7
                if (val < 1 || val > 7) {
                    throw new ParseException("Day-of-Week values must be between 1 and 7", -1);
                }
                // 当前周的最后一天
                lastdayOfWeek = true;
            } else {
                // 无效的选项
                throw new ParseException("'L' option is not valid here. (pos=" + i + ")", i);
            }
            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }

        if (c == 'W') {
            if (type == DAY_OF_MONTH) {
                //有效的工作日
                nearestWeekday = true;
            } else {
                // 无效的选项
                throw new ParseException("'W' option is not valid here. (pos=" + i + ")", i);
            }
            // “W”选项对于大于31（一个月的最大天数）的值无效
            if (val > 31) {
                throw new ParseException(
                        "The 'W' option does not make sense with values larger than 31 (max number of days in a month)",
                        i);
            }
            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }

        if (c == '#') {
            // '#'只能与周一起用
            if (type != DAY_OF_WEEK) {
                // 无效的选项
                throw new ParseException("'#' option is not valid here. (pos=" + i + ")", i);
            }
            i++;
            try {
                // 解析为第几周
                nthdayOfWeek = Integer.parseInt(s.substring(i));
                //一个月最多只有四周
                if (nthdayOfWeek < 1 || nthdayOfWeek > 5) {
                    throw new Exception();
                }
            } catch (Exception e) {
                //“#”选项后面必须有一个介于1和5之间的数值
                throw new ParseException(
                        "A numeric value between 1 and 5 must follow the '#' option",
                        i);
            }

            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }

        if (c == '-') {
            //解析范围

            //跳过符号位置
            i++;
            c = s.charAt(i);
            //范围结束值
            int v = Integer.parseInt(String.valueOf(c));
            end = v;
            i++;
            //判断结束值是否结束，此时如果是个位数，那么就结束了
            if (i >= s.length()) {
                addToSet(val, end, 1, type);
                return i;
            }
            c = s.charAt(i);
            // 两位数的情况
            if (c >= '0' && c <= '9') {
                ValueSet vs = getValue(v, s, i);
                end = vs.value;
                i = vs.pos;
            }
            // 存在'/'的情况,如：15-30/5
            if (i < s.length() && ((c = s.charAt(i)) == '/')) {
                i++;
                c = s.charAt(i);
                //第二个value值，如：15-30/5 ，此时为5
                int v2 = Integer.parseInt(String.valueOf(c));
                i++;
                //判断结束值是否结束，此时如果是个位数，那么就结束了
                if (i >= s.length()) {
                    addToSet(val, end, v2, type);
                    return i;
                }
                c = s.charAt(i);
                // 两位数的情况
                if (c >= '0' && c <= '9') {
                    ValueSet vs = getValue(v2, s, i);
                    int v3 = vs.value;
                    addToSet(val, end, v3, type);
                    i = vs.pos;
                    return i;
                } else {
                    addToSet(val, end, v2, type);
                    return i;
                }
            } else {
                //默认的情况 如：15-30
                addToSet(val, end, 1, type);
                return i;
            }
        }

        if (c == '/') {
            // 后面必须跟一个整数
            if ((i + 1) >= s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t') {
                throw new ParseException("'/' must be followed by an integer.", i);
            }

            i++;
            c = s.charAt(i);
            // 第二个value值，如 : 0/5 ，此时为5
            int v2 = Integer.parseInt(String.valueOf(c));
            i++;
            //判断结束值是否结束，此时如果是个位数，那么就结束了
            if (i >= s.length()) {
                // 根据类型检查范围
                checkIncrementRange(v2, type, i);
                addToSet(val, end, v2, type);
                return i;
            }
            c = s.charAt(i);
            // 两位数的情况
            if (c >= '0' && c <= '9') {
                ValueSet vs = getValue(v2, s, i);
                int v3 = vs.value;
                // 根据类型检查范围
                checkIncrementRange(v3, type, i);
                addToSet(val, end, v3, type);
                i = vs.pos;
                return i;
            } else {
                // 未知的字符
                throw new ParseException("Unexpected character '" + c + "' after '/'", i);
            }
        }

        //默认情况直接添加并设置
        addToSet(val, end, 0, type);
        i++;
        return i;
    }

    /**
     * 得到表达式的摘要，有点像 <Code>toString()</Code>.
     *
     * @return 表达式解析后的字符串表示
     */
    public String getExpressionSummary() {
        StringBuilder buf = new StringBuilder();

        buf.append("seconds: ");
        buf.append(getExpressionSetSummary(seconds));
        buf.append("\n");
        buf.append("minutes: ");
        buf.append(getExpressionSetSummary(minutes));
        buf.append("\n");
        buf.append("hours: ");
        buf.append(getExpressionSetSummary(hours));
        buf.append("\n");
        buf.append("daysOfMonth: ");
        buf.append(getExpressionSetSummary(daysOfMonth));
        buf.append("\n");
        buf.append("months: ");
        buf.append(getExpressionSetSummary(months));
        buf.append("\n");
        buf.append("daysOfWeek: ");
        buf.append(getExpressionSetSummary(daysOfWeek));
        buf.append("\n");
        buf.append("lastdayOfWeek: ");
        buf.append(lastdayOfWeek);
        buf.append("\n");
        buf.append("nearestWeekday: ");
        buf.append(nearestWeekday);
        buf.append("\n");
        buf.append("NthDayOfWeek: ");
        buf.append(nthdayOfWeek);
        buf.append("\n");
        buf.append("lastdayOfMonth: ");
        buf.append(lastdayOfMonth);
        buf.append("\n");
        buf.append("years: ");
        buf.append(getExpressionSetSummary(years));
        buf.append("\n");

        return buf.toString();
    }

    /**
     * 得到表达式指定类型Set的摘要。
     *
     * @param set 指定类型Set值
     * @return 指定类型Set所有元素的追加字符串
     */
    protected String getExpressionSetSummary(Set<Integer> set) {
        //替换字符串
        if (set.contains(NO_SPEC)) {
            return "?";
        }
        if (set.contains(ALL_SPEC)) {
            return "*";
        }

        StringBuilder buf = new StringBuilder();
        //遍历追加Set的值
        Iterator<Integer> itr = set.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            Integer iVal = itr.next();
            String val = iVal.toString();
            if (!first) {
                buf.append(",");
            }
            buf.append(val);
            first = false;
        }
        return buf.toString();
    }

    /**
     * 得到表达式指定类型List的摘要
     *
     * @param list 指定类型List值
     * @return 指定类型List所有元素的追加字符串
     */
    protected String getExpressionSetSummary(ArrayList<Integer> list) {

        if (list.contains(NO_SPEC)) {
            return "?";
        }
        if (list.contains(ALL_SPEC)) {
            return "*";
        }

        StringBuilder buf = new StringBuilder();

        Iterator<Integer> itr = list.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            Integer iVal = itr.next();
            String val = iVal.toString();
            if (!first) {
                buf.append(",");
            }
            buf.append(val);
            first = false;
        }

        return buf.toString();
    }

    /**
     * 跳过空格符
     *
     * @param i 下标
     * @param s cron指定类型的字符串
     * @return 跳过的总长度
     */
    protected int skipWhiteSpace(int i, String s) {
        for (; i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t'); i++) {
        }

        return i;
    }

    /**
     * 找到下一个空格
     *
     * @param i 下标
     * @param s cron指定类型的字符串
     * @return 下一个空格的下标
     */
    protected int findNextWhiteSpace(int i, String s) {
        for (; i < s.length() && (s.charAt(i) != ' ' || s.charAt(i) != '\t'); i++) {
        }

        return i;
    }

    /**
     * 添加并且设置.
     *
     * @param val  符号的类型值
     * @param end  结束值
     * @param incr 增量
     * @param type cron指定类型
     */
    protected void addToSet(int val, int end, int incr, int type)
            throws ParseException {
        // 类型列表
        TreeSet<Integer> set = getSet(type);

        // 1. 验证其类型及其类型的相关范围
        if (type == SECOND || type == MINUTE) {
            if ((val < 0 || val > 59 || end > 59) && (val != ALL_SPEC_INT)) {
                throw new ParseException(
                        "Minute and Second values must be between 0 and 59",
                        -1);
            }
        } else if (type == HOUR) {
            if ((val < 0 || val > 23 || end > 23) && (val != ALL_SPEC_INT)) {
                throw new ParseException(
                        "Hour values must be between 0 and 23", -1);
            }
        } else if (type == DAY_OF_MONTH) {
            if ((val < 1 || val > 31 || end > 31) && (val != ALL_SPEC_INT)
                    && (val != NO_SPEC_INT)) {
                throw new ParseException(
                        "Day of month values must be between 1 and 31", -1);
            }
        } else if (type == MONTH) {
            if ((val < 1 || val > 12 || end > 12) && (val != ALL_SPEC_INT)) {
                throw new ParseException(
                        "Month values must be between 1 and 12", -1);
            }
        } else if (type == DAY_OF_WEEK) {
            if ((val == 0 || val > 7 || end > 7) && (val != ALL_SPEC_INT)
                    && (val != NO_SPEC_INT)) {
                throw new ParseException(
                        "Day-of-Week values must be between 1 and 7", -1);
            }
        }
        // 1. 没有增量且不为'*'的情况,
        // 1.1. 如果有类型码就直接往类型集添加类型码,
        // 1.2. 如果没有类型码就直接添加'?'的类型码,也就是NO_SPEC
        // 设置完了就直接退出
        if ((incr == 0 || incr == -1) && val != ALL_SPEC_INT) {
            if (val != -1) {
                set.add(val);
            } else {
                set.add(NO_SPEC);
            }
            return;
        }
        // 开始值.
        int startAt = val;
        // 结束值.
        int stopAt = end;

        //为'*'且没有增加值时的默认设置
        if (val == ALL_SPEC_INT && incr <= 0) {
            incr = 1;
            // 在放置标记值,但也填充值
            set.add(ALL_SPEC);
        }

        // 记录类型的范围
        if (type == SECOND || type == MINUTE) {
            if (stopAt == -1) {
                stopAt = 59;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 0;
            }
        } else if (type == HOUR) {
            if (stopAt == -1) {
                stopAt = 23;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 0;
            }
        } else if (type == DAY_OF_MONTH) {
            if (stopAt == -1) {
                stopAt = 31;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == MONTH) {
            if (stopAt == -1) {
                stopAt = 12;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == DAY_OF_WEEK) {
            if (stopAt == -1) {
                stopAt = 7;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == YEAR) {
            if (stopAt == -1) {
                stopAt = MAX_YEAR;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1970;
            }
        }

        // if the end of the range is before the start, then we need to overflow into
        // the next day, month etc. This is done by adding the maximum amount for that
        // type, and using modulus max to determine the value being added.
        // 如果结束范围在开始范围之前，那么我们需要溢出到第二天、第二个月等。这是通过添加该类型的最大值,并使用计量单位的最大值来确定要添加的值来完成的。
        int max = -1;
        if (stopAt < startAt) {
            switch (type) {
                case SECOND:
                    max = 60;
                    break;
                case MINUTE:
                    max = 60;
                    break;
                case HOUR:
                    max = 24;
                    break;
                case MONTH:
                    max = 12;
                    break;
                case DAY_OF_WEEK:
                    max = 7;
                    break;
                case DAY_OF_MONTH:
                    max = 31;
                    break;
                case YEAR:
                    throw new IllegalArgumentException("Start year must be less than stop year");
                default:
                    throw new IllegalArgumentException("Unexpected type encountered");
            }
            stopAt += max;
        }
        // 往各个TreeSet设置具体值
        for (int i = startAt; i <= stopAt; i += incr) {
            // 最大值没有溢出
            if (max == -1) {
                // ie: there's no max to overflow over
                set.add(i);
            } else {
                // take the modulus to get the real value
                // 取模,得到实际值
                int i2 = i % max;

                // 1-indexed ranges should not include 0, and should include their max
                // 月、周、天是可以有最大值的
                if (i2 == 0 && (type == MONTH || type == DAY_OF_WEEK || type == DAY_OF_MONTH)) {
                    i2 = max;
                }
                set.add(i2);
            }
        }
    }

    /**
     * 获取指定类型列表.
     *
     * @param type cron类型值
     */
    TreeSet<Integer> getSet(int type) {
        switch (type) {
            case SECOND:
                return seconds;
            case MINUTE:
                return minutes;
            case HOUR:
                return hours;
            case DAY_OF_MONTH:
                return daysOfMonth;
            case MONTH:
                return months;
            case DAY_OF_WEEK:
                return daysOfWeek;
            case YEAR:
                return years;
            default:
                return null;
        }
    }

    /**
     * 得到ValueSet类型
     *
     * @param v 值
     * @param s 字符串表示
     * @param i 第二个元素的下标
     * @return 返回拼接好数字的ValueSet对象
     */
    protected ValueSet getValue(int v, String s, int i) {
        // 指定位置的值
        char c = s.charAt(i);
        //拼接字符串的值
        StringBuilder s1 = new StringBuilder(String.valueOf(v));
        //指定位置为数字
        while (c >= '0' && c <= '9') {
            s1.append(c);
            i++;
            //一直追加，直到s的最后下标
            if (i >= s.length()) {
                break;
            }
            //更新字符的值
            c = s.charAt(i);
        }
        ValueSet val = new ValueSet();
        // 位数
        val.pos = (i < s.length()) ? i : i + 1;
        // 当前字符所表示的值
        val.value = Integer.parseInt(s1.toString());
        return val;
    }

    /**
     * 解析后的数值
     *
     * @param s cron指定类型的字符串
     * @param i 下标
     * @return 解析后数值
     */
    protected int getNumericValue(String s, int i) {
        // 值的结束下标
        int endOfVal = findNextWhiteSpace(i, s);
        String val = s.substring(i, endOfVal);
        return Integer.parseInt(val);
    }

    /**
     * 得到月的值.
     *
     * @param s 表示月份的英文
     * @return 表示月份的数字, 没找到返回-1
     */
    protected int getMonthNumber(String s) {
        return monthMap.getOrDefault(s, -1);
    }

    /**
     * 得到周的值.
     *
     * @param s 表示周几的英文
     * @return 表示周几的数字, 没找到返回-1
     */
    protected int getDayOfWeekNumber(String s) {
        return dayMap.getOrDefault(s, -1);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Computation Functions
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * 返回满足cron表达式的给定日期/时间 <I>之后 </I>的下一个日期/时间。
     * <p>
     * 以seconds字段做说明：
     * <ol>
     * <li>先获取当前时间值sec</li>
     * <li>根据sec值，截取TreeSet对象seconds的大于sec值的集合st</li>
     * <li>如果集合st不为空，那么取st的第一个元素作为下次要触发的时刻值</li>
     * <li>如果集合st为空，说明sec值是集合seconds的最大值，那么读取集合seconds的头元素，下一级别字段min需要增加1</li>
     * </ol>
     * <p>Q: 特殊字符*的情况，会放一个99的元素到TreeSet里面，这个元素怎么处理的？
     * <p>A: 在构建TreeSet对象的时候，没有放进去超出该字段范围的值。基于前面，使用TreeSet.tailSet(),不会返回空的st。
     * <p>在没有特殊字符*的情况，seconds的元素为0,25,50。如果当前秒数为55，则会返回空的st。
     *
     * @param afterTime 开始搜索下一个有效日期/时间的日期/时间
     * @return 下一个有效日期/时间
     */
    public Date getTimeAfter(Date afterTime) {

        // Computation is based on Gregorian year only.
        // 计算仅以公历年为基础。
        Calendar cl = new GregorianCalendar(getTimeZone());

        // move ahead one second, since we're computing the time *after* the
        // given time
        afterTime = new Date(afterTime.getTime() + TimeUnit.SECONDS.toMillis(1));
        // CronTrigger does not deal with milliseconds
        //cron不处理毫秒
        cl.setTime(afterTime);
        cl.set(Calendar.MILLISECOND, 0);

        // 标识是否找完了
        boolean gotOne = false;
        // loop until we've computed the next time, or we've past the endTime
        // 循环，直到我们计算完下一次，或者超过结束时间
        while (!gotOne) {

            //防止无休止的循环
            if (cl.get(Calendar.YEAR) > 2999) {
                return null;
            }

            SortedSet<Integer> st = null;
            int t = 0;
            // 当前秒数
            int sec = cl.get(Calendar.SECOND);
            // 当前分钟
            int min = cl.get(Calendar.MINUTE);

            // get second.................................................
            //找到大于当前秒值的数据，取第一条，没找到直接取seconds中的第一条数据
            st = seconds.tailSet(sec);
            if (st != null && st.size() != 0) {
                sec = st.first();
            } else {
                sec = seconds.first();
                //更新为下一分钟
                min++;
                cl.set(Calendar.MINUTE, min);
            }
            cl.set(Calendar.SECOND, sec);
            //更新分钟值
            min = cl.get(Calendar.MINUTE);
            // 当前小时
            int hr = cl.get(Calendar.HOUR_OF_DAY);
            t = -1;

            // get minute.................................................
            //找到大于当前分钟值的数据，取第一条，没找到直接取minutes中的第一条数据
            st = minutes.tailSet(min);
            if (st != null && st.size() != 0) {
                t = min;
                min = st.first();
            } else {
                //此时说明指定分钟已经过完了，因此要从下一分钟开始找
                min = minutes.first();
                hr++;
            }
            //如果没有找到分钟，则不停的找,直到找到为止
            if (min != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, min);
                setCalendarHour(cl, hr);
                continue;
            }

            cl.set(Calendar.MINUTE, min);

            //更新当前分钟
            hr = cl.get(Calendar.HOUR_OF_DAY);
            // 当前日值
            int day = cl.get(Calendar.DAY_OF_MONTH);
            t = -1;

            // get hour...................................................
            //找到大于当前小时值的数据，取第一条，没找到直接取hours中的第一条数据
            st = hours.tailSet(hr);
            if (st != null && st.size() != 0) {
                t = hr;
                hr = st.first();
            } else {
                // 如果小时小于指定的小时，那么天数加1，直到找到指定的小时 （例如找的是22点，但现在是23点，那么跳过今天，向明天看）
                hr = hours.first();
                day++;
            }
            //如果没有找到分钟，则从下一分钟找，不停的找,直到找到为止
            if (hr != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.DAY_OF_MONTH, day);
                setCalendarHour(cl, hr);
                continue;
            }
            cl.set(Calendar.HOUR_OF_DAY, hr);
            // 更新当前日值
            day = cl.get(Calendar.DAY_OF_MONTH);
            // 当前月份
            int mon = cl.get(Calendar.MONTH) + 1;
            // '+ 1' because calendar is 0-based for this field, and we are
            // 1-based
            t = -1;
            int tmon = mon;

            // get day...................................................
            // 标识日不为'?'
            boolean dayOfMSpec = !daysOfMonth.contains(NO_SPEC);
            // 标识周不为'?'
            boolean dayOfWSpec = !daysOfWeek.contains(NO_SPEC);
            // 逐日获取月规则
            if (dayOfMSpec && !dayOfWSpec) {
                // 找到大于当前小时值的数据，取第一条，没找到直接取hours中的第一条数据
                st = daysOfMonth.tailSet(day);
                //为最后一天的情况
                if (lastdayOfMonth) {
                    //不为工作日的情况
                    if (!nearestWeekday) {
                        // 当前日
                        t = day;
                        // 获取指定年月份的最后一天
                        day = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                        //减去的偏差值
                        day -= lastdayOffset;
                        // 月份已经超过了一个月日数的临界值
                        if (t > day) {
                            //月份加1
                            mon++;
                            //如果大于12，那么就说明一年月数的临界值，设置月份为1月
                            if (mon > 12) {
                                mon = 1;
                                //确保mon！=tmon, 以便下面失败
                                tmon = 3333;
                                cl.add(Calendar.YEAR, 1);
                            }
                            day = 1;
                        }
                    } else {
                        // 工作日的情况
                        t = day;
                        // 获取指定年月份的最后一天
                        day = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                        // 减去的偏差值
                        day -= lastdayOffset;

                        //当前月最后一天的日期，yyyy-MM-dd
                        Calendar tcal = Calendar.getInstance(getTimeZone());
                        tcal.set(Calendar.SECOND, 0);
                        tcal.set(Calendar.MINUTE, 0);
                        tcal.set(Calendar.HOUR_OF_DAY, 0);
                        tcal.set(Calendar.DAY_OF_MONTH, day);
                        tcal.set(Calendar.MONTH, mon - 1);
                        tcal.set(Calendar.YEAR, cl.get(Calendar.YEAR));

                        // 当前月份的最后一天
                        int ldom = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                        // 最后一天星期几的值
                        int dow = tcal.get(Calendar.DAY_OF_WEEK);
                        //工作日的计算
                        // 星期六，且日期为1，前进2天
                        if (dow == Calendar.SATURDAY && day == 1) {
                            day += 2;
                        } else if (dow == Calendar.SATURDAY) {
                            //如果为星期六，直接后退1天
                            day -= 1;
                        } else if (dow == Calendar.SUNDAY && day == ldom) {
                            //如果为星期天，并且最后一天为31号且为星期天，那么工作日就应该后退两天
                            day -= 2;
                        } else if (dow == Calendar.SUNDAY) {
                            //如果为星期天，直接前进1天
                            day += 1;
                        }

                        tcal.set(Calendar.SECOND, sec);
                        tcal.set(Calendar.MINUTE, min);
                        tcal.set(Calendar.HOUR_OF_DAY, hr);
                        tcal.set(Calendar.DAY_OF_MONTH, day);
                        tcal.set(Calendar.MONTH, mon - 1);
                        Date nTime = tcal.getTime();
                        //nTime<afterTime,即范围肉已经没有可选值的情况，直接将日期置为1号，月份加1
                        if (nTime.before(afterTime)) {
                            day = 1;
                            mon++;
                        }
                    }
                } else if (nearestWeekday) {
                    // 工作日的情况
                    t = day;
                    // 第一个天数
                    day = daysOfMonth.first();

                    // 比较的日期，例如15w,此时此值为当月15日
                    Calendar tcal = Calendar.getInstance(getTimeZone());
                    tcal.set(Calendar.SECOND, 0);
                    tcal.set(Calendar.MINUTE, 0);
                    tcal.set(Calendar.HOUR_OF_DAY, 0);
                    tcal.set(Calendar.DAY_OF_MONTH, day);
                    tcal.set(Calendar.MONTH, mon - 1);
                    tcal.set(Calendar.YEAR, cl.get(Calendar.YEAR));

                    // 当前月份的最后一天
                    int ldom = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    // 最后一天星期几的值
                    int dow = tcal.get(Calendar.DAY_OF_WEEK);

                    //工作日的计算
                    // 星期六，且日期为1，前进2天
                    if (dow == Calendar.SATURDAY && day == 1) {
                        day += 2;
                    } else if (dow == Calendar.SATURDAY) {
                        //如果为星期六，直接后退1天
                        day -= 1;
                    } else if (dow == Calendar.SUNDAY && day == ldom) {
                        //如果为星期天，并且最后一天为31号且为星期天，那么工作日就应该后退两天
                        day -= 2;
                    } else if (dow == Calendar.SUNDAY) {
                        //如果为星期天，直接前进1天
                        day += 1;
                    }

                    tcal.set(Calendar.SECOND, sec);
                    tcal.set(Calendar.MINUTE, min);
                    tcal.set(Calendar.HOUR_OF_DAY, hr);
                    tcal.set(Calendar.DAY_OF_MONTH, day);
                    tcal.set(Calendar.MONTH, mon - 1);
                    Date nTime = tcal.getTime();
                    //nTime<afterTime,即范围肉已经没有可选值的情况，直接将日期置为1号，月份加1
                    if (nTime.before(afterTime)) {
                        day = daysOfMonth.first();
                        mon++;
                    }
                } else if (st != null && st.size() != 0) {
                    //指定了的日域情况
                    // 当前日
                    t = day;
                    // 第一个天数
                    day = st.first();
                    // make sure we don't over-run a short month, such as february
                    // 确保我们不会在短的一个月内过度运转，比如二月
                    int lastDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    //如果最后一天小于指定的天数，那么月份加1，找到指定的日期 （例如找的是31号，但现在是2号分，那么跳过二月，向三月看）
                    if (day > lastDay) {
                        day = daysOfMonth.first();
                        mon++;
                    }
                } else {
                    //当前日期已经大于指定日期的情况，即本月没有符合条件的情况了
                    day = daysOfMonth.first();
                    mon++;
                }

                // 本月没有符合条件的情况，直接对日，月进行修改，然后再次查询符合条件的日期
                if (day != t || mon != tmon) {
                    cl.set(Calendar.SECOND, 0);
                    cl.set(Calendar.MINUTE, 0);
                    cl.set(Calendar.HOUR_OF_DAY, 0);
                    cl.set(Calendar.DAY_OF_MONTH, day);
                    cl.set(Calendar.MONTH, mon - 1);
                    // '- 1' because calendar is 0-based for this field, and we
                    // are 1-based
                    continue;
                }
            } else if (dayOfWSpec && !dayOfMSpec) { // get day by day of week rule
                //逐日获取周规则
                if (lastdayOfWeek) {
                    // 'L'的情况
                    // the month?
                    // 如果dow值不为1，那么表示这个月的倒数第N天
                    int dow = daysOfWeek.first(); // desired
                    // 下次满足的周
                    int cDow = cl.get(Calendar.DAY_OF_WEEK); // current d-o-w
                    // 要添加的天数
                    int daysToAdd = 0;
                    // 本周没有满足的条件时，追加到下周的天数
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    }
                    //  本周满足的条件时，追加到本周还剩的天数
                    if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }

                    //当前月的最后一天
                    int lDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    //错过了正确值的情况，直到找到符合的值
                    if (day + daysToAdd > lDay) { // did we already miss the
                        // last one?
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // no '- 1' here because we are promoting the month
                        continue;
                    }

                    // 查找本月此日最后一次出现的日期
                    while ((day + daysToAdd + 7) <= lDay) {
                        daysToAdd += 7;
                    }
                    //最后一周的日期
                    day += daysToAdd;
                    // 设置为当前计算的天数，然后再次计算是否还存在符合条件的日期
                    if (daysToAdd > 0) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' here because we are not promoting the month
                        continue;
                    }

                } else if (nthdayOfWeek != 0) {
                    // 'L'的情况

                    int dow = daysOfWeek.first(); // desired
                    // 得到星期几
                    int cDow = cl.get(Calendar.DAY_OF_WEEK); // current d-o-w
                    int daysToAdd = 0;
                    // 本周没有满足的条件时，追加到下周的天数
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    } else if (cDow > dow) {
                        //  本周满足的条件时，追加到本周还剩的天数
                        daysToAdd = dow + (7 - cDow);
                    }
                    //是否移动天数
                    boolean dayShifted = false;
                    if (daysToAdd > 0) {
                        dayShifted = true;
                    }

                    day += daysToAdd;
                    // 计算当前天数为第几周
                    int weekOfMonth = day / 7;
                    //如果有余数周数还得再加1
                    if (day % 7 > 0) {
                        weekOfMonth++;
                    }
                    //计算距离目标周数要追加的天数
                    daysToAdd = (nthdayOfWeek - weekOfMonth) * 7;
                    day += daysToAdd;
                    // 时间过了或者没有追加值的情况
                    if (daysToAdd < 0 || day > getLastDayOfMonth(mon, cl.get(Calendar.YEAR))) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // no '- 1' here because we are promoting the month
                        continue;
                    } else if (daysToAdd > 0 || dayShifted) {
                        // 有追加天数的情况
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' here because we are NOT promoting the month
                        continue;
                    }
                } else {
                    //指定周数的情况，没有其它修饰符
                    //得到星期几
                    int cDow = cl.get(Calendar.DAY_OF_WEEK); // current d-o-w
                    // 表达式设置的值
                    int dow = daysOfWeek.first(); // desired
                    //  // 找到大于设置周值的数据，取第一条，没找到直接取daysOfWeek中的第一条数据
                    st = daysOfWeek.tailSet(cDow);
                    if (st != null && st.size() > 0) {
                        dow = st.first();
                    }

                    int daysToAdd = 0;
                    // 本周没有满足的条件时，追加到下周的天数
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    }
                    //  本周满足的条件时，追加到本周还剩的天数
                    if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }
                    // 当前月的最后一天
                    int lDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));

                    //指定的时间已经超过了，那么便从下个月重新开始
                    if (day + daysToAdd > lDay) { // will we pass the end of
                        // the month?
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        continue;
                    } else if (daysToAdd > 0) {
                        // 设置为当前计算的天数，然后再次计算是否还存在符合条件的日期
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day + daysToAdd);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' because calendar is 0-based for this field,
                        // and we are 1-based
                        continue;
                    }
                }
            } else {
                // 不支持同时指定星期几和月日参数
                throw new UnsupportedOperationException(
                        "Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.");
            }
            //计算的天
            cl.set(Calendar.DAY_OF_MONTH, day);
            //计算的月份
            mon = cl.get(Calendar.MONTH) + 1;
            // '+ 1' because calendar is 0-based for this field, and we are
            // 1-based
            // 计算的年
            int year = cl.get(Calendar.YEAR);
            t = -1;

            // test for expressions that never generate a valid fire date,
            // but keep looping...
            // 超出最大年，返回null
            if (year > MAX_YEAR) {
                return null;
            }

            // get month...................................................
            //找到大于当前月值的数据，取第一条，没找到直接取month中的第一条数据
            st = months.tailSet(mon);
            if (st != null && st.size() != 0) {
                t = mon;
                mon = st.first();
            } else {
                //此时说明指定月份已经过完了，因此要从下一年开始找
                mon = months.first();
                year++;
            }
            //如果没有找到月份，则从下一年找，不停的找,直到找到为止
            if (mon != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.HOUR_OF_DAY, 0);
                cl.set(Calendar.DAY_OF_MONTH, 1);
                cl.set(Calendar.MONTH, mon - 1);
                cl.set(Calendar.YEAR, year);
                continue;
            }
            cl.set(Calendar.MONTH, mon - 1);

            // 更新当前年值
            year = cl.get(Calendar.YEAR);
            t = -1;

            // get year...................................................
            //找到大于当前月值的数据，取第一条，没找到直接取month中的第一条数据
            st = years.tailSet(year);
            if (st != null && st.size() != 0) {
                t = year;
                year = st.first();
            } else {
                //超出年的范围了
                return null;
            }
            // 如果没有找到年，则从1号重新开始找，不停的找,直到找到为止
            if (year != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.HOUR_OF_DAY, 0);
                cl.set(Calendar.DAY_OF_MONTH, 1);
                cl.set(Calendar.MONTH, 0);
                // '- 1' because calendar is 0-based for this field, and we are
                // 1-based
                cl.set(Calendar.YEAR, year);
                continue;
            }
            cl.set(Calendar.YEAR, year);

            gotOne = true;
        } // while( !done )

        //返回符合条件的日期
        return cl.getTime();
    }

    /**
     * 设置日历为特定的小时.
     *
     * @param cal  要操作的日历
     * @param hour 等设置的小时
     */
    protected void setCalendarHour(Calendar cal, int hour) {
        cal.set(Calendar.HOUR_OF_DAY, hour);
        //如果设置的小时不相等，并且不等于24，那么在小时上+1
        if (cal.get(Calendar.HOUR_OF_DAY) != hour && hour != 24) {
            cal.set(Calendar.HOUR_OF_DAY, hour + 1);
        }
    }

    /**
     * 返回CronExpression匹配的给定时间之前的时间
     *
     * @param endTime 结束日期
     * @return 给定时间之前的时间
     */
    public Date getTimeBefore(Date endTime) {
        // FUTURE_TODO: implement QUARTZ-423
        return null;
    }

    /**
     * 返回CronExpression将要匹配的最后时间
     *
     * @return CronExpression将要匹配的最后时间
     */
    public Date getFinalFireTime() {
        // FUTURE_TODO: implement QUARTZ-423
        return null;
    }

    /**
     * 是否为闰年
     *
     * @param year 年份
     * @return true, 闰年，否则，false
     */
    protected boolean isLeapYear(int year) {
        return ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0));
    }

    /**
     * 得到月份中的最后一天
     *
     * @param monthNum 月份
     * @param year     年
     * @return 最后一天的值
     */
    protected int getLastDayOfMonth(int monthNum, int year) {

        switch (monthNum) {
            case 1:
                return 31;
            case 2:
                return (isLeapYear(year)) ? 29 : 28;
            case 3:
                return 31;
            case 4:
                return 30;
            case 5:
                return 31;
            case 6:
                return 30;
            case 7:
                return 31;
            case 8:
                return 31;
            case 9:
                return 30;
            case 10:
                return 31;
            case 11:
                return 30;
            case 12:
                return 31;
            default:
                throw new IllegalArgumentException("Illegal month number: "
                        + monthNum);
        }
    }

    /**
     * 读取对象.
     *
     * @param stream 对象输入流.
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream stream)
            throws java.io.IOException, ClassNotFoundException {
        // 从该流中读取当前类的非静态和非瞬态字段
        stream.defaultReadObject();
        try {
            // 构建对象
            buildExpression(cronExpression);
        } catch (Exception ignore) {
        } // never happens
    }

    /**
     * 拷贝.
     *
     * @return 当前对象
     */
    @Override
    @Deprecated
    public Object clone() {
        return new CronExpression(this);
    }

    /**
     * 值集合.(用于拼接cron表达式中的值).
     */
    class ValueSet {
        /**
         * 拼接好的值
         */
        public int value;
        /**
         * 下一元素的下标
         */
        public int pos;
    }

    public static void main(String[] args) throws ParseException {
        System.out.println(new Date());
        // 每个月最后一个工作日的10点15分0秒触发任务
//        CronExpression cron = new CronExpression("0 15 10 LW * ?");
        // 星期一到星期五的10点15分0秒触发任务
//        CronExpression cron = new CronExpression("0 15 10 ? * MON-FRI");
        // 每15秒，30秒，45秒时触发任务
//        CronExpression cron = new CronExpression("15,30,45 * * * * ?");
        // 每个月最后一天的10点15分0秒触发任务
//        CronExpression cron = new CronExpression("0 15 10 L * ?");
        // 每分钟的15秒到30秒之间开始触发，每隔5秒触发一次
//        CronExpression cron = new CronExpression("15-30/5 * * * * ?");
        // 在每天下午2点到下午2:55期间的每5分钟触发
//        CronExpression cron = new CronExpression("0 0/5 14 * * ?");
        //每个月离15日最近的工作日每5秒执行一次
//        CronExpression cron = new CronExpression("0/5 * * 15W * ? *");
        //每个月最后一个星期执行一次
        CronExpression cron = new CronExpression("0 0 0 ? * 3 ");

        cron.isSatisfiedBy(new Date(2020, 2, 1));
        Date nextValidTime = cron.getNextValidTimeAfter(new Date(System.currentTimeMillis() + 5000));

        System.out.println(nextValidTime);
    }

}