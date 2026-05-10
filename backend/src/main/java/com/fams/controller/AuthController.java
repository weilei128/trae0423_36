package com.fams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fams.common.Result;
import com.fams.entity.SysUser;
import com.fams.mapper.SysUserMapper;
import com.fams.util.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
            new QueryWrapper<SysUser>()
                .eq("username", request.getUsername())
                .eq("deleted", 0)
        );
        
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("用户已被禁用");
        }
        
        if (!user.getPassword().equals(request.getPassword())) {
            return Result.error("密码错误");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("deptId", user.getDeptId());
        
        return Result.success("登录成功", result);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);
            
            SysUser user = sysUserMapper.selectById(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("username", username);
            result.put("realName", user != null ? user.getRealName() : username);
            result.put("role", role);
            result.put("avatar", "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(401, "Token无效");
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
