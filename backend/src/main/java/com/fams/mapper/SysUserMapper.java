package com.fams.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fams.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT u.*, d.dept_name as dept_name FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id WHERE u.username = #{username} AND u.deleted = 0")
    SysUser selectUserWithDept(String username);
}
