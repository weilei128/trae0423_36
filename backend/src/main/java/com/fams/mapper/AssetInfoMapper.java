package com.fams.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fams.entity.AssetInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AssetInfoMapper extends BaseMapper<AssetInfo> {
    
    @Select("SELECT a.*, c.category_name, " +
            "u.real_name as responsible_user_name, " +
            "d.dept_name as responsible_dept_name " +
            "FROM asset_info a " +
            "LEFT JOIN asset_category c ON a.category_id = c.id " +
            "LEFT JOIN sys_user u ON a.responsible_user_id = u.id " +
            "LEFT JOIN sys_dept d ON a.responsible_dept_id = d.id " +
            "WHERE a.deleted = 0 " +
            "ORDER BY a.id DESC")
    Page<AssetInfo> selectAssetList(Page<AssetInfo> page);
    
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'in_stock' THEN 1 ELSE 0 END) as inStock, " +
            "SUM(CASE WHEN status = 'assigned' THEN 1 ELSE 0 END) as assigned, " +
            "SUM(CASE WHEN status = 'in_use' THEN 1 ELSE 0 END) as inUse, " +
            "SUM(CASE WHEN status = 'maintenance' THEN 1 ELSE 0 END) as maintenance, " +
            "SUM(CASE WHEN status = 'scrapped' THEN 1 ELSE 0 END) as scrapped, " +
            "SUM(purchase_price) as totalValue, " +
            "SUM(current_value) as netValue " +
            "FROM asset_info WHERE deleted = 0")
    Map<String, Object> getDashboardStats();
    
    @Select("SELECT " +
            "d.dept_name as name, " +
            "COUNT(a.id) as value " +
            "FROM asset_info a " +
            "LEFT JOIN sys_dept d ON a.responsible_dept_id = d.id " +
            "WHERE a.deleted = 0 " +
            "GROUP BY a.responsible_dept_id " +
            "ORDER BY value DESC")
    List<Map<String, Object>> getAssetByDept();
    
    @Select("SELECT " +
            "c.category_name as name, " +
            "COUNT(a.id) as value " +
            "FROM asset_info a " +
            "LEFT JOIN asset_category c ON a.category_id = c.id " +
            "WHERE a.deleted = 0 " +
            "GROUP BY a.category_id " +
            "ORDER BY value DESC")
    List<Map<String, Object>> getAssetByCategory();
    
    @Select("SELECT * FROM asset_info " +
            "WHERE deleted = 0 " +
            "AND DATE_ADD(purchase_date, INTERVAL (depreciation_period * 3) MONTH) BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 90 DAY) " +
            "ORDER BY purchase_date ASC " +
            "LIMIT 10")
    List<AssetInfo> getExpiringAssets();
}
