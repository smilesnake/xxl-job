package com.xxl.job.core.handler.impl;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.core.util.ScriptUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 脚本任务处理器，处理 glue非GLUE_GROOVY的所有类型.
 *
 * @author xuxueli
 * @date 17/4/27
 */
@Slf4j
public class ScriptJobHandler extends IJobHandler {
    /**
     * 任务id.
     */
    private int jobId;
    /**
     * GLUE更新时间.
     */
    @Getter
    private long glueUpdateTime;
    /**
     * GLUE源码.
     */
    private String glueSource;
    /**
     * GLUE类型.
     *
     * @see com.xxl.job.core.glue.GlueTypeEnum
     */
    private GlueTypeEnum glueType;

    /**
     * 创建脚本任务处理器.
     *
     * @param jobId          任务id
     * @param glueUpdateTime GLUE更新时间
     * @param glueSource     GLUE源码
     * @param glueType       GLUE类型
     */
    public ScriptJobHandler(int jobId, long glueUpdateTime, String glueSource, GlueTypeEnum glueType) {
        this.jobId = jobId;
        this.glueUpdateTime = glueUpdateTime;
        this.glueSource = glueSource;
        this.glueType = glueType;

        // 清理旧的脚本文件
        File glueSrcPath = new File(XxlJobFileAppender.getGlueSrcPath());
        if (glueSrcPath.exists()) {
            File[] glueSrcFileList = glueSrcPath.listFiles();
            if (ArrayUtils.isNotEmpty(glueSrcFileList)) {
                for (File glueSrcFileItem : glueSrcFileList) {
                    if (glueSrcFileItem.getName().startsWith(jobId + "_")) {
                        try {
                            Files.delete(glueSrcFileItem.toPath());
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }

    }

    @Override
    public ReturnT<String> execute(String param) throws IOException {
        //非脚本不执行
        if (!glueType.isScript()) {
            return new ReturnT<>(IJobHandler.FAIL.getCode(), "glueType[" + glueType + "] invalid.");
        }

        // cmd
        String cmd = glueType.getCmd();

        //  生成脚本文件
        String scriptFileName = XxlJobFileAppender.getGlueSrcPath()
                .concat(File.separator)
                .concat(String.valueOf(jobId))
                .concat("_")
                .concat(String.valueOf(glueUpdateTime))
                .concat(glueType.getSuffix());
        File scriptFile = new File(scriptFileName);
        if (!scriptFile.exists()) {
            ScriptUtil.markScriptFile(scriptFileName, glueSource);
        }

        //日志文件
        String logFileName = XxlJobContext.getXxlJobContext().getJobLogFileName();

        // 脚本参数：0=param、1=分片序号、2=分片总数
        String[] scriptParams = new String[3];
        scriptParams[0] = param;
        scriptParams[1] = String.valueOf(XxlJobContext.getXxlJobContext().getShardIndex());
        scriptParams[2] = String.valueOf(XxlJobContext.getXxlJobContext().getShardTotal());

        // 调用脚本
        XxlJobLogger.log("----------- script file:" + scriptFileName + " -----------");
        int exitValue = ScriptUtil.execToFile(cmd, scriptFileName, logFileName, scriptParams);

        if (exitValue == 0) {
            return IJobHandler.SUCCESS;
        } else {
            return new ReturnT<>(IJobHandler.FAIL.getCode(), "script exit value(" + exitValue + ") is failed");
        }

    }

}
