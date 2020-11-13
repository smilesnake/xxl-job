package com.xxl.job.admin.service;


import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.core.biz.model.ReturnT;

import java.util.Date;
import java.util.Map;

/**
 * xxl-job的主要任务行为
 *
 * @author xuxueli 2016-5-28 15:30:33
 */
public interface XxlJobService {

    /**
     * 任务分页列表.
     *
     * @param start           页码
     * @param length          页面大小
     * @param jobGroup        执行器主键ID
     * @param triggerStatus   调度状态：0-停止，1-运行
     * @param jobDesc         任务描述
     * @param executorHandler 执行器，任务Handler名称
     * @param author          负责人
     * @return 任务分页列表
     */
    Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author);

    /**
     * 新增任务信息
     *
     * @param jobInfo 任务信息实体
     * @return 新增的任务id
     */
    ReturnT<String> add(XxlJobInfo jobInfo);

    /**
     * 更新任务信息
     *
     * @param jobInfo 任务信息实体
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    ReturnT<String> update(XxlJobInfo jobInfo);

    /**
     * 删除任务信息
     *
     * @param id 任务信息主键id
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    ReturnT<String> remove(int id);

    /**
     * 启动
     *
     * @param id 任务信息主键id
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    ReturnT<String> start(int id);

    /**
     * 停止
     *
     * @param id 任务信息主键id
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    ReturnT<String> stop(int id);

    /**
     * 仪表板
     *
     * @return 仪表板统计信息
     */
    Map<String, Object> dashboardInfo();

    /**
     * 首页图表信息.
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 统计信息
     */
    ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate);

}
