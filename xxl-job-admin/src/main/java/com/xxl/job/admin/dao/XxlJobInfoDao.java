package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * 任务信息 Mapper层.
 *
 * @author xuxueli 2016-1-12 18:03:45
 */
@Mapper
public interface XxlJobInfoDao {

  /**
   * 任务分页列表.
   *
   * @param offset          页码
   * @param pageSize        页面大小
   * @param jobGroup        执行器主键ID
   * @param triggerStatus   调度状态：0-停止，1-运行
   * @param jobDesc         任务描述
   * @param executorHandler 执行器，任务Handler名称
   * @param author          负责人
   * @return 任务分页列表
   */
  List<XxlJobInfo> pageList(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize,
      @Param("jobGroup") Integer jobGroup, @Param("triggerStatus") Integer triggerStatus,
      @Param("jobDesc") String jobDesc, @Param("executorHandler") String executorHandler,
      @Param("author") String author);

  /**
   * 统计分页大小.
   *
   * @param jobGroup        执行器主键ID
   * @param triggerStatus   调度状态：0-停止，1-运行
   * @param jobDesc         任务描述
   * @param executorHandler 执行器，任务Handler名称
   * @param author          负责人
   * @return 分页大小
   */
  Integer pageListCount(@Param("jobGroup") Integer jobGroup,
      @Param("triggerStatus") Integer triggerStatus, @Param("jobDesc") String jobDesc,
      @Param("executorHandler") String executorHandler, @Param("author") String author);

  /**
   * 新增任务信息.
   *
   * @param info 任务信息实体
   * @return 新增的任务id
   */
  Integer save(XxlJobInfo info);

  /**
   * 根据id查询任务信息.
   *
   * @param id 任务信息id
   * @return 任务信息
   */
  XxlJobInfo loadById(@Param("id") Integer id);

  /**
   * 更新任务信息.
   *
   * @param xxlJobInfo 任务信息实体
   * @return 影响的行数
   */
  Integer update(XxlJobInfo xxlJobInfo);

  /**
   * 删除任务信息.
   *
   * @param id 任务信息主键id
   * @return 影响的行数
   */
  Integer delete(@Param("id") Integer id);

  /**
   * 通过执行器id找到任务信息列表.
   *
   * @param jobGroup 执行器id
   * @return 符合条件的任务信息列表
   */
  List<XxlJobInfo> getJobsByGroup(@Param("jobGroup") Integer jobGroup);

  /**
   * 任务总数.
   *
   * @return 任务总数
   */
  Integer findAllCount();

  /**
   * 查看运行中的任务待执行的任务信息.
   *
   * @param maxNextTime 下一次的执行时间
   * @param pageSize    页面大小
   * @return 符合条件的任务信息
   */
  List<XxlJobInfo> scheduleJobQuery(@Param("maxNextTime") Long maxNextTime,
      @Param("pageSize") Integer pageSize);

  /**
   * 更新调度时间.
   *
   * @param xxlJobInfo 任务信息实体
   * @return 影响的行数
   */
  Integer scheduleUpdate(XxlJobInfo xxlJobInfo);


}
