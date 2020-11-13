package com.xxl.job.core.log;

import com.xxl.job.core.biz.model.LogResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 调度任务日志文件追加器.
 * 存储调度日志至每一个日志文件
 *
 * @author xuxueli 2016-3-12 19:25:12
 */
@Slf4j
public class XxlJobFileAppender {
    private XxlJobFileAppender() {
    }

    /**
     * 日志基地址.
     * <pre>
     * strut like:
     * ---/
     * ---/gluesource/
     * ---/gluesource/10_1514171108000.js
     * ---/gluesource/10_1514171108000.js
     * ---/2017-12-25/
     * ---/2017-12-25/639.log
     * ---/2017-12-25/821.log
     * </pre>
     */
    @Getter
    private static String logBasePath = "/data/applogs/xxl-job/jobhandler";
    /**
     * glue脚本源码存放地址.
     */
    @Getter
    private static String glueSrcPath = logBasePath.concat("/gluesource");

    /**
     * 初始化日志存放路径，任务执行bean内部日志输出到该路径下
     *
     * @param logPath 日志路径
     */
    public static void initLogPath(String logPath) {
        // 初始化
        if (logPath != null && logPath.trim().length() > 0) {
            logBasePath = logPath;
        }
        // 生成基文件夹
        File logPathDir = new File(logBasePath);
        if (!logPathDir.exists()) {
            logPathDir.mkdirs();
        }
        logBasePath = logPathDir.getPath();

        // 生成脚本目录
        File glueBaseDir = new File(logPathDir, "gluesource");
        if (!glueBaseDir.exists()) {
            glueBaseDir.mkdirs();
        }
        glueSrcPath = glueBaseDir.getPath();
    }

    /**
     * 生成日志文件名称, 例如 "logPath/yyyy-MM-dd/9999.log"
     *
     * @param triggerDate 调度的日期
     * @param logId       日志id
     * @return 日志文件名称
     */
    public static String makeLogFileName(LocalDate triggerDate, long logId) {

        // filePath/yyyy-MM-dd
        //避免并发问题，不能一成不变
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        File logFilePath = new File(getLogBasePath(), dtf.format(triggerDate));
        if (!logFilePath.exists()) {
            logFilePath.mkdir();
        }

        // filePath/yyyy-MM-dd/9999.log
        return logFilePath.getPath() + File.separator + logId + ".log";
    }

    /**
     * 追加日志
     *
     * @param logFileName 日志名称
     * @param appendLog   追加的日志
     */
    static void appendLog(String logFileName, String appendLog) {

        // log file
        if (StringUtils.isBlank(logFileName)) {
            return;
        }
        File logFile = new File(logFileName);

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return;
            }
        }

        // log
        if (appendLog == null) {
            appendLog = "";
        }
        appendLog += "\r\n";

        //追加文件内容
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write(appendLog.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 支持读取日志文件.
     *
     * @param logFileName 日志文件名
     * @param fromLineNum 开始行号
     * @return 日志内容
     */
    public static LogResult readLog(String logFileName, int fromLineNum) {

        // valid log file
        if (StringUtils.isBlank(logFileName)) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        File logFile = new File(logFileName);

        if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }

        // 读取文件内容
        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // [from, to], start as 1
                toLineNum = reader.getLineNumber();
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        // 日志结果
        return new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);

		/*
        // it will return the number of characters actually skipped
        reader.skip(Long.MAX_VALUE);
        int maxLineNum = reader.getLineNumber();
        maxLineNum++;	// 最大行号
        */
    }

    /**
     * 读取日志数据
     *
     * @param logFile 日志文件
     * @return 日志行内容
     */
    public static String readLines(File logFile) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
