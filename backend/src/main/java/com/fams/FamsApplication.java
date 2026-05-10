package com.fams;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.fams.mapper")
@EnableScheduling
public class FamsApplication {
    public static void main(String[] args) {
        SpringApplication.run(FamsApplication.class, args);
        System.out.println("========================================");
        System.out.println("  固定资产管理系统后端启动成功!");
        System.out.println("  端口: 10026");
        System.out.println("========================================");
    }
}
