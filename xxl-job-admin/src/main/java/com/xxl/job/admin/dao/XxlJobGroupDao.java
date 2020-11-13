package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 执行器信息 Mapper层
 *
 * @author xuxueli
 * @date 16/9/30
 */
@Mapper
public interface XxlJobGroupDao {

  /**
   * 查看所有执行器信息.
   *
   * @return 所有执行器信息
   */
  List<XxlJobGroup> findAll();

  /**
   * 根据执行器地址类型查看执行器信息列表
   *
   * @param addressType 执行器地址类型：0=自动注册、1=手动录入
   * @return 符合条件的执行器信息列表
   */
  List<XxlJobGroup> findByAddressType(@Param("addressType") Integer addressType);

  /**
   * 保存执行器信息.
   *
   * @param xxlJobGroup 要保存的执行器信息
   * @return 影响的行数
   */
  int save(XxlJobGroup xxlJobGroup);

  /**
   * 更新执行器信息.
   *
   * @param xxlJobGroup 待更新的执行器信息
   * @return 影响的行数
   */

  int update(XxlJobGroup xxlJobGroup);

  /**
   * 移除执行器信息
   *
   * @param id 执行器主键id
   * @return 影响的行数
   */
  int remove(@Param("id") Integer id);

  /**
   * 加载执行器信息
   *
   * @param id 执行器id
   * @return 执行器信息
   */
  XxlJobGroup load(@Param("id") Integer id);

  /**
   * 分页
   *
   * @param offset   页码
   * @param pagesize 页面大小
   * @param appname  执行器名称
   * @param title    执行器标题
   * @return 符合条件的执行器信息列表
   */
  List<XxlJobGroup> pageList(@Param("offset") Integer offset, @Param("pagesize") Integer pagesize,
      @Param("appname") String appname, @Param("title") String title);

  /**
   * 统计分页数量.
   *
   * @param offset   页码
   * @param pagesize 页面大小
   * @param appname  执行器名称
   * @param title    执行器标题
   * @return 分页数量
   */
  int pageListCount(@Param("offset") Integer offset, @Param("pagesize") Integer pagesize,
      @Param("appname") String appname, @Param("title") String title);

}
