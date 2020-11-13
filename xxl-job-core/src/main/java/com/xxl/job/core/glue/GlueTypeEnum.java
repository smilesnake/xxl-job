package com.xxl.job.core.glue;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 运行模式.
 *
 * @author xuxueli on 17/4/26.
 */
@Getter
@AllArgsConstructor
public enum GlueTypeEnum {
    /**
     * BEAN模式：任务以JobHandler方式维护在执行器端；需要结合 "JobHandler" 属性匹配执行器中任务；
     */
    BEAN("BEAN", false, null, null),
    /**
     * GLUE模式(Java)：任务以源码方式维护在调度中心；该模式的任务实际上是一段继承自IJobHandler的Java类代码并 "groovy" 源码方式维护，它在执行器项目中运行，可使用@Resource/@Autowire注入执行器里中的其他服务；
     */
    GLUE_GROOVY("GLUE(Java)", false, null, null),
    /**
     * GLUE模式(Shell)：任务以源码方式维护在调度中心；该模式的任务实际上是一段 "shell" 脚本；
     */
    GLUE_SHELL("GLUE(Shell)", true, "bash", ".sh"),
    /**
     * GLUE模式(Python)：任务以源码方式维护在调度中心；该模式的任务实际上是一段 "python" 脚本；
     */
    GLUE_PYTHON("GLUE(Python)", true, "python", ".py"),
    /**
     * GLUE模式(PHP)：任务以源码方式维护在调度中心；该模式的任务实际上是一段 "php" 脚本；
     */
    GLUE_PHP("GLUE(PHP)", true, "php", ".php"),
    /**
     * GLUE模式(NodeJS)：任务以源码方式维护在调度中心；该模式的任务实际上是一段 "nodejs" 脚本；
     */
    GLUE_NODEJS("GLUE(Nodejs)", true, "node", ".js"),
    /**
     * GLUE模式(PowerShell)：任务以源码方式维护在调度中心；该模式的任务实际上是一段 "PowerShell" 脚本；
     */
    GLUE_POWERSHELL("GLUE(PowerShell)", true, "powershell", ".ps1");
    /**
     * 描述.
     */
    private String desc;
    /**
     * 是否为脚本.
     */
    private boolean isScript;
    /**
     * 命令类型.
     */
    private String cmd;
    /**
     * 后缀.
     */
    private String suffix;

    public static GlueTypeEnum match(String name) {
        for (GlueTypeEnum item : GlueTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }
}
