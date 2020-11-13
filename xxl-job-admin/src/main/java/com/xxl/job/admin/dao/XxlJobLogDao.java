package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.model.extend.LogReportExt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 任务日志 mapper层
 *
 * @author xuxueli 2016-1-12 18:03:06
 */
@Mapper
public interface XxlJobLogDao {

  // exist jobId not use jobGroup, not exist use jobGroup

  /**
   * 分页
   *
   * @param offset           页码
   * @param pageSize         页面大小
   * @param jobGroup         任务执行器
   * @param jobId            任务id
   * @param triggerTimeStart 调度开始时间
   * @param triggerTimeEnd   调度结束时间
   * @param logStatus        日志状态
   * @return 任务日志分页信息
   */
  List<XxlJobLog> pageList(@Param("offset") int offset, @Param("pageSize") int pageSize,
      @Param("jobGroup") int jobGroup, @Param("jobId") int jobId,
      @Param("triggerTimeStart") Date triggerTimeStart,
      @Param("triggerTimeEnd") Date triggerTimeEnd, @Param("logStatus") int logStatus);

  /**
   * 分页总数量
   *
   * @param jobGroup         任务执行器
   * @param jobId            任务id
   * @param triggerTimeStart 调度开始时间
   * @param triggerTimeEnd   调度结束时间
   * @param logStatus        日志状态
   * @return 分页总数量
   */
  int pageListCount(@Param("jobGroup") int jobGroup, @Param("jobId") int jobId,
      @Param("triggerTimeStart") Date triggerTimeStart,
      @Param("triggerTimeEnd") Date triggerTimeEnd, @Param("logStatus") int logStatus);

  /**
   * 根据任务id加载任务日志.
   *
   * @param id 任务id
   * @return 任务日志
   */
  XxlJobLog load(@Param("id") long id);

  /**
   * 保存.
   *
   * @param xxlJobLog 日志对象
   * @return 插入的id
   */
  long save(XxlJobLog xxlJobLog);

  /**
   * 更新调度任务的日志信息
   *
   * @param xxlJobLog 任务日志信息
   * @return 影响的行数
   */
  int updateTriggerInfo(XxlJobLog xxlJobLog);

  /**
   * 更新处理信息
   *
   * @param xxlJobLog 任务日志
   * @return 影响行数
   */
  int updateHandleInfo(XxlJobLog xxlJobLog);

  /**
   * 通过任务id删除任务日志
   *
   * @param jobId 任务id
   * @return 影响的行数
   */
  int delete(@Param("jobId") int jobId);

  /**
   * 统计指定日期范围的日志信息
   *
   * @param from 开始时间
   * @param to   结束时间
   * @return 统计指的日志信息
   */
  LogReportExt findLogReport(@Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

  /**
   * 查询清理的日志id
   *
   * @param jobGroup        执行器id
   * @param jobId           任务id
   * @param clearBeforeTime 清理之前时间
   * @param clearBeforeNum  清理之前的数量
   * @param pageSize        页面大小
   * @return 日志id
   */
  List<Long> findClearLogIds(@Param("jobGroup") int jobGroup, @Param("jobId") int jobId,
      @Param("clearBeforeTime") LocalDateTime clearBeforeTime,
      @Param("clearBeforeNum") int clearBeforeNum, @Param("pageSize") int pageSize);

  /**
   * 清理日志（批量删除）
   *
   * @param logIds 日志id列表
   * @return 影响的行数
   */
  int clearLog(@Param("logIds") List<Long> logIds);

  /**
   * 查询失败的任务日志id
   *
   * @param pageSize 页面大小
   * @return 任务日志id
   */
  List<Long> findFailJobLogIds(@Param("pageSize") int pageSize);

  /**
   * 更新告警状态：（0-默认、1-无需告警、2-告警成功、3-告警失败）
   *
   * @param logId          日志id
   * @param oldAlarmStatus 旧的告警状态
   * @param newAlarmStatus 新的告警状态
   * @return 影响的行数
   */
  int updateAlarmStatus(@Param("logId") long logId, @Param("oldAlarmStatus") int oldAlarmStatus,
      @Param("newAlarmStatus") int newAlarmStatus);

  /**
   * 查看丢失的任务日志id
   *
   * @param losedTime 丢失时间
   * @return 任务日志id
   */
  List<Long> findLostJobIds(@Param("losedTime") LocalDateTime losedTime);

}
