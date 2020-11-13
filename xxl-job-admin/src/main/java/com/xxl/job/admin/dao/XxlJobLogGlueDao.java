package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务脚本信息 Mapper层
 *
 * @author xuxueli 2016-5-19 18:04:56
 */
@Mapper
public interface XxlJobLogGlueDao {
    /**
     * 保存任务脚本信息
     *
     * @param xxlJobLogGlue 任务脚本信息
     * @return 脚本id
     */
    public int save(XxlJobLogGlue xxlJobLogGlue);

    /**
     * 找到指定任务id的任务脚本信息
     *
     * @param jobId 任务id
     * @return 任务脚本信息列表
     */
    public List<XxlJobLogGlue> findByJobId(@Param("jobId") int jobId);

    /**
     * 删除旧的任务脚本信息.
     *
     * @param jobId 任务id
     * @param limit 条数
     * @return 影响的行数
     */
    public int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

    /**
     * 删除指定任务的任务脚本信息.
     *
     * @param jobId 任务id
     * @return 影响的行数
     */
    public int deleteByJobId(@Param("jobId") int jobId);

}
