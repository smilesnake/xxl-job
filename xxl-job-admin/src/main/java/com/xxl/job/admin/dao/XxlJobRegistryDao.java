package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.core.enums.RegistryConfig;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 注册信息 Mapper层.
 *
 * @author xuxueli
 * @date 16/9/30
 */
@Mapper
public interface XxlJobRegistryDao {

  /**
   * 查询已经不在线的注册信息，即（查找更新时间小于（当前时间-超时时间）的注册信息）.
   *
   * @param timeout 超时时间
   * @param nowTime 当前时间
   * @return 符合条件的注册信息列表
   */
  List<Integer> findDead(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

  /**
   * 移除不在线的注册信息.
   *
   * @param ids 任务注册id
   * @return 影响的行数
   */
  int removeDead(@Param("ids") List<Integer> ids);

  /**
   * 查找在线的注册信息，查找更新时间大于（当前时间-超时时间）的注册信息.
   *
   * @param timeout 超时时间
   * @param nowTime 当前时间
   * @return 符合条件的注册信息列表
   */
  List<XxlJobRegistry> findAll(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

  /**
   * 更新注册信息.
   *
   * @param registryGroup 注册的执行器
   * @param registryKey   注册的appName
   * @param registryValue 注册的地址
   * @param updateTime    更新的时间
   * @return 影响的行数
   * @see RegistryConfig.RegistryType#EXECUTOR equals registryGroup
   */
  int registryUpdate(@Param("registryGroup") String registryGroup,
      @Param("registryKey") String registryKey, @Param("registryValue") String registryValue,
      @Param("updateTime") Date updateTime);

  /**
   * 保存注册信息.
   *
   * @param registryGroup 注册的执行器
   * @param registryKey   注册的appName
   * @param registryValue 注册的地址
   * @param updateTime    更新的时间
   * @return 影响的行数
   */
  int registrySave(@Param("registryGroup") String registryGroup,
      @Param("registryKey") String registryKey, @Param("registryValue") String registryValue,
      @Param("updateTime") Date updateTime);

  /**
   * 删除注册信息.
   *
   * @param registryGroup 注册的执行器
   * @param registryKey   注册的appName
   * @param registryValue 注册的地址
   * @return 影响的行数
   */
  int registryDelete(@Param("registryGroup") String registryGroup,
      @Param("registryKey") String registryKey, @Param("registryValue") String registryValue);
}