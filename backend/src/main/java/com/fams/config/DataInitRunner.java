package com.fams.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'SYS_USER'",
                Integer.class
            );
            
            if (count == null || count == 0) {
                logger.info("数据库表不存在，开始初始化...");
                initDatabase();
                logger.info("数据库初始化完成！");
            } else {
                logger.info("数据库表已存在，跳过初始化");
            }
        } catch (Exception e) {
            logger.error("数据库初始化失败: " + e.getMessage(), e);
        }
    }
    
    private void initDatabase() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE sys_dept (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    dept_name VARCHAR(50) NOT NULL," +
                "    dept_code VARCHAR(20)," +
                "    parent_id BIGINT DEFAULT 0," +
                "    sort INT DEFAULT 0," +
                "    leader VARCHAR(50)," +
                "    phone VARCHAR(20)," +
                "    status INT DEFAULT 1," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE sys_user (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    username VARCHAR(50) NOT NULL UNIQUE," +
                "    password VARCHAR(100) NOT NULL," +
                "    real_name VARCHAR(50) NOT NULL," +
                "    phone VARCHAR(20)," +
                "    email VARCHAR(100)," +
                "    dept_id BIGINT," +
                "    role VARCHAR(20) DEFAULT 'user'," +
                "    status INT DEFAULT 1," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE asset_category (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    category_name VARCHAR(50) NOT NULL," +
                "    category_code VARCHAR(20)," +
                "    parent_id BIGINT DEFAULT 0," +
                "    depreciation_period INT DEFAULT 60," +
                "    residual_rate DECIMAL(5,2) DEFAULT 5.00," +
                "    sort INT DEFAULT 0," +
                "    status INT DEFAULT 1," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE asset_info (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    asset_code VARCHAR(50) NOT NULL UNIQUE," +
                "    asset_name VARCHAR(100) NOT NULL," +
                "    category_id BIGINT NOT NULL," +
                "    brand VARCHAR(50)," +
                "    model VARCHAR(100)," +
                "    spec VARCHAR(200)," +
                "    sn VARCHAR(100)," +
                "    purchase_date DATE," +
                "    purchase_price DECIMAL(12,2)," +
                "    supplier VARCHAR(100)," +
                "    location VARCHAR(200)," +
                "    description VARCHAR(1000)," +
                "    status VARCHAR(20) DEFAULT 'in_stock'," +
                "    depreciation_status VARCHAR(20) DEFAULT 'normal'," +
                "    current_value DECIMAL(12,2)," +
                "    accumulated_depreciation DECIMAL(12,2) DEFAULT 0," +
                "    responsible_user_id BIGINT," +
                "    responsible_dept_id BIGINT," +
                "    create_user_id BIGINT," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE asset_allocation (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    asset_id BIGINT NOT NULL," +
                "    apply_user_id BIGINT NOT NULL," +
                "    apply_dept_id BIGINT," +
                "    apply_reason VARCHAR(1000)," +
                "    apply_type VARCHAR(20) DEFAULT 'receive'," +
                "    status VARCHAR(20) DEFAULT 'pending'," +
                "    approver_id BIGINT," +
                "    approve_time TIMESTAMP," +
                "    approve_remark VARCHAR(1000)," +
                "    assign_user_id BIGINT," +
                "    assign_dept_id BIGINT," +
                "    start_date DATE," +
                "    expected_return_date DATE," +
                "    actual_return_date DATE," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE asset_transfer (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    asset_id BIGINT NOT NULL," +
                "    transfer_type VARCHAR(20) NOT NULL," +
                "    from_user_id BIGINT," +
                "    from_dept_id BIGINT," +
                "    to_user_id BIGINT," +
                "    to_dept_id BIGINT," +
                "    transfer_reason VARCHAR(1000)," +
                "    status VARCHAR(20) DEFAULT 'pending'," +
                "    approver_id BIGINT," +
                "    approve_time TIMESTAMP," +
                "    approve_remark VARCHAR(1000)," +
                "    transfer_date DATE," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE asset_maintenance (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    asset_id BIGINT NOT NULL," +
                "    report_user_id BIGINT," +
                "    report_time TIMESTAMP," +
                "    fault_description VARCHAR(1000)," +
                "    fault_location VARCHAR(100)," +
                "    status VARCHAR(20) DEFAULT 'pending'," +
                "    maintenance_person VARCHAR(50)," +
                "    maintenance_start_time TIMESTAMP," +
                "    maintenance_end_time TIMESTAMP," +
                "    maintenance_content VARCHAR(1000)," +
                "    maintenance_cost DECIMAL(10,2) DEFAULT 0," +
                "    parts_cost DECIMAL(10,2) DEFAULT 0," +
                "    labor_cost DECIMAL(10,2) DEFAULT 0," +
                "    remark VARCHAR(1000)," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE asset_depreciation (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    asset_id BIGINT NOT NULL," +
                "    depreciation_month VARCHAR(7) NOT NULL," +
                "    original_value DECIMAL(12,2)," +
                "    current_value_before DECIMAL(12,2)," +
                "    depreciation_amount DECIMAL(12,2)," +
                "    accumulated_depreciation DECIMAL(12,2)," +
                "    current_value_after DECIMAL(12,2)," +
                "    depreciation_rate DECIMAL(10,4)," +
                "    remark VARCHAR(200)," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE inventory_task (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    task_code VARCHAR(50) NOT NULL UNIQUE," +
                "    task_name VARCHAR(100) NOT NULL," +
                "    inventory_type VARCHAR(20) DEFAULT 'regular'," +
                "    dept_ids VARCHAR(1000)," +
                "    category_ids VARCHAR(1000)," +
                "    status VARCHAR(20) DEFAULT 'draft'," +
                "    inventory_date DATE," +
                "    create_user_id BIGINT," +
                "    total_count INT DEFAULT 0," +
                "    checked_count INT DEFAULT 0," +
                "    profit_count INT DEFAULT 0," +
                "    loss_count INT DEFAULT 0," +
                "    remark VARCHAR(1000)," +
                "    deleted INT DEFAULT 0," +
                "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            jdbcTemplate.execute(
                "CREATE TABLE inventory_detail (" +
                "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "    task_id BIGINT NOT NULL," +
                "    asset_id BIGINT NOT NULL," +
                "    check_status VARCHAR(20) DEFAULT 'pending'," +
                "    check_time TIMESTAMP," +
                "    checker_id BIGINT," +
                "    actual_location VARCHAR(200)," +
                "    actual_status VARCHAR(20)," +
                "    remark VARCHAR(500)" +
                ")"
            );
            
            logger.info("所有表创建完成");
            
            initData();
            
        } catch (Exception e) {
            logger.error("初始化数据库失败: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private void initData() {
        try {
            jdbcTemplate.update(
                "INSERT INTO sys_dept (id, dept_name, dept_code, parent_id, sort, leader, status) VALUES " +
                "(1, '总公司', 'HQ', 0, 0, '张总', 1)," +
                "(2, 'IT部门', 'IT', 1, 1, '李经理', 1)," +
                "(3, '行政部门', 'ADMIN', 1, 2, '王经理', 1)," +
                "(4, '财务部', 'FIN', 1, 3, '赵经理', 1)," +
                "(5, '人力资源部', 'HR', 1, 4, '刘经理', 1)"
            );
            logger.info("部门数据初始化完成");
            
            jdbcTemplate.update(
                "INSERT INTO sys_user (id, username, password, real_name, phone, email, dept_id, role, status) VALUES " +
                "(1, 'admin', '123456', '系统管理员', '13800000000', 'admin@fams.com', 1, 'admin', 1)," +
                "(2, 'manager', '123456', '资产管理员', '13800000001', 'manager@fams.com', 3, 'manager', 1)," +
                "(3, 'ituser', '123456', 'IT员工', '13800000002', 'it@fams.com', 2, 'user', 1)"
            );
            logger.info("用户数据初始化完成");
            
            jdbcTemplate.update(
                "INSERT INTO asset_category (id, category_name, category_code, parent_id, depreciation_period, residual_rate, sort, status) VALUES " +
                "(1, '电子设备', 'EQUIP', 0, 36, 5.00, 1, 1)," +
                "(2, '办公家具', 'FURN', 0, 60, 5.00, 2, 1)," +
                "(3, '车辆', 'CAR', 0, 120, 5.00, 3, 1)," +
                "(4, '房屋建筑', 'BUILD', 0, 360, 5.00, 4, 1)," +
                "(5, '电脑', 'PC', 1, 36, 5.00, 1, 1)," +
                "(6, '打印机', 'PRINT', 1, 36, 5.00, 2, 1)," +
                "(7, '办公桌', 'DESK', 2, 60, 5.00, 1, 1)," +
                "(8, '办公椅', 'CHAIR', 2, 60, 5.00, 2, 1)"
            );
            logger.info("分类数据初始化完成");
            
        } catch (Exception e) {
            logger.warn("初始化数据可能已存在: " + e.getMessage());
        }
    }
}
