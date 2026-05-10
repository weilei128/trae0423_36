package com.fams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fams.common.Result;
import com.fams.entity.SysDept;
import com.fams.entity.SysUser;
import com.fams.mapper.SysDeptMapper;
import com.fams.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private SysDeptMapper deptMapper;

    @GetMapping("/users")
    public Result<List<SysUser>> getUserList(
            @RequestParam(required = false) Long deptId) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0).eq("status", 1);
        if (deptId != null) {
            wrapper.eq("dept_id", deptId);
        }
        wrapper.orderByAsc("id");
        List<SysUser> list = userMapper.selectList(wrapper);
        return Result.success(list);
    }
    
    @GetMapping("/depts")
    public Result<List<SysDept>> getDeptList() {
        List<SysDept> list = deptMapper.selectList(
            new QueryWrapper<SysDept>()
                .eq("deleted", 0)
                .eq("status", 1)
                .orderByAsc("sort")
        );
        return Result.success(list);
    }
    
    @GetMapping("/user/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
    
    @GetMapping("/dept/{id}")
    public Result<SysDept> getDeptById(@PathVariable Long id) {
        return Result.success(deptMapper.selectById(id));
    }
}
