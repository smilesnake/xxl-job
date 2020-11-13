package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户信息 Mapper层.
 *
 * @author xuxueli 2019-05-04 16:44:59
 */
@Mapper
public interface XxlJobUserDao {

  /**
   * 分页.
   *
   * @param offset   页码
   * @param pagesize 页面大小
   * @param username 用户名
   * @param role     角色（0-普通用户、1-管理员）
   * @return 用户信息列表
   */
  public List<XxlJobUser> pageList(@Param("offset") int offset, @Param("pagesize") int pagesize,
      @Param("username") String username, @Param("role") int role);

  /**
   * 分页总数.
   *
   * @param username 用户名
   * @param role     角色（0-普通用户、1-管理员）
   * @return 分页总数
   */
  public int pageListCount(@Param("username") String username, @Param("role") int role);

  /**
   * 通过用户名查询用户信息.
   *
   * @param username 用户名
   * @return 用户信息
   */
  public XxlJobUser loadByUserName(@Param("username") String username);

  /**
   * 保存用户信息.
   *
   * @param xxlJobUser 用户信息
   * @return 保存好的用户id
   */
  public int save(XxlJobUser xxlJobUser);

  /**
   * 更新用户信息.
   *
   * @param xxlJobUser 用户信息
   * @return 影响的行数
   */
  public int update(XxlJobUser xxlJobUser);

  /**
   * 根据用户id删除用户信息.
   *
   * @param id 用户id
   * @return 影响的行数
   */
  public int delete(@Param("id") int id);

}
