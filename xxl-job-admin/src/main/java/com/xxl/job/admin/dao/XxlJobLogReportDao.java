package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 任务日志统计信息 Mapper层
 *
 * @author xuxueli 2019-11-22
 */
@Mapper
public interface XxlJobLogReportDao {

  /**
   * 保存日志报告信息.
   *
   * @param xxlJobLogReport 日志报告信息
   * @return 生成的报告id
   */
  int save(XxlJobLogReport xxlJobLogReport);

  /**
   * 更新日志报告.
   *
   * @param xxlJobLogReport 日志报告信息
   * @return 影响的行数
   */
  int update(XxlJobLogReport xxlJobLogReport);

  /**
   * 查询指定时间内的调用日志.
   *
   * @param triggerDayFrom 触发开始时间
   * @param triggerDayTo   触发结束时间
   * @return xxl-job日志报告列表
   */
  List<XxlJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
      @Param("triggerDayTo") Date triggerDayTo);

  /**
   * 统计运行中-日志数量、执行成功-日志数量、执行失败-日志数量求和.
   *
   * @return xxl-job日志报告
   */
  XxlJobLogReport queryLogReportTotal();

}
