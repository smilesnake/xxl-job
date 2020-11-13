package com.xxl.job.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常工具类.
 *
 * @author xuxueli 2018-10-20 20:07:26
 */
public class ThrowableUtil {

    /**
     * 将error异常解析为字符串.
     *
     * @param e 异常对象
     * @return 异常解析后的字符串
     */
    public static String toString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String errorMsg = stringWriter.toString();
        return errorMsg;
    }

}
