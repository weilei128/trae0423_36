package com.fams.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fams.common.PageResult;
import com.fams.common.Result;
import com.fams.entity.*;
import com.fams.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/asset")
public class AssetController {

    @Autowired
    private AssetInfoMapper assetInfoMapper;
    
    @Autowired
    private AssetCategoryMapper categoryMapper;
    
    @Autowired
    private SysDeptMapper deptMapper;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private AssetAllocationMapper allocationMapper;
    
    @Autowired
    private AssetTransferMapper transferMapper;
    
    @Autowired
    private AssetMaintenanceMapper maintenanceMapper;
    
    @Autowired
    private AssetDepreciationMapper depreciationMapper;

    // ==================== 分类管理 ====================
    @GetMapping("/category/list")
    public Result<List<AssetCategory>> getCategoryList() {
        List<AssetCategory> list = categoryMapper.selectList(
            new QueryWrapper<AssetCategory>()
                .eq("deleted", 0)
                .orderByAsc("sort")
        );
        return Result.success(list);
    }
    
    @PostMapping("/category")
    public Result<AssetCategory> addCategory(@RequestBody AssetCategory category) {
        if (category.getResidualRate() == null) {
            category.setResidualRate(new BigDecimal("5.00"));
        }
        if (category.getDepreciationPeriod() == null) {
            category.setDepreciationPeriod(60);
        }
        categoryMapper.insert(category);
        return Result.success(category);
    }
    
    @PutMapping("/category/{id}")
    public Result<AssetCategory> updateCategory(@PathVariable Long id, @RequestBody AssetCategory category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success(category);
    }
    
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 资产档案管理 ====================
    @GetMapping("/list")
    public Result<PageResult<AssetInfo>> getAssetList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {
        
        Page<AssetInfo> page = new Page<>(current, size);
        QueryWrapper<AssetInfo> wrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like("asset_code", keyword)
                .or().like("asset_name", keyword)
                .or().like("brand", keyword)
                .or().like("model", keyword)
            );
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        
        wrapper.orderByDesc("id");
        
        Page<AssetInfo> resultPage = assetInfoMapper.selectPage(page, wrapper);
        
        // 补充额外信息
        for (AssetInfo asset : resultPage.getRecords()) {
            if (asset.getCategoryId() != null) {
                AssetCategory cat = categoryMapper.selectById(asset.getCategoryId());
                if (cat != null) {
                    asset.setCategoryName(cat.getCategoryName());
                }
            }
            if (asset.getResponsibleUserId() != null) {
                SysUser user = userMapper.selectById(asset.getResponsibleUserId());
                if (user != null) {
                    asset.setResponsibleUserName(user.getRealName());
                }
            }
            if (asset.getResponsibleDeptId() != null) {
                SysDept dept = deptMapper.selectById(asset.getResponsibleDeptId());
                if (dept != null) {
                    asset.setResponsibleDeptName(dept.getDeptName());
                }
            }
        }
        
        return Result.success(PageResult.of(
            resultPage.getRecords(),
            resultPage.getTotal(),
            resultPage.getCurrent(),
            resultPage.getSize()
        ));
    }
    
    @GetMapping("/{id}")
    public Result<AssetInfo> getAssetDetail(@PathVariable Long id) {
        AssetInfo asset = assetInfoMapper.selectById(id);
        if (asset == null) {
            return Result.error("资产不存在");
        }
        
        if (asset.getCategoryId() != null) {
            AssetCategory cat = categoryMapper.selectById(asset.getCategoryId());
            if (cat != null) asset.setCategoryName(cat.getCategoryName());
        }
        if (asset.getResponsibleUserId() != null) {
            SysUser user = userMapper.selectById(asset.getResponsibleUserId());
            if (user != null) asset.setResponsibleUserName(user.getRealName());
        }
        if (asset.getResponsibleDeptId() != null) {
            SysDept dept = deptMapper.selectById(asset.getResponsibleDeptId());
            if (dept != null) asset.setResponsibleDeptName(dept.getDeptName());
        }
        
        return Result.success(asset);
    }
    
    @PostMapping
    public Result<AssetInfo> addAsset(@RequestBody AssetInfo asset) {
        String assetCode = generateAssetCode(asset.getCategoryId());
        asset.setAssetCode(assetCode);
        asset.setStatus("in_stock");
        asset.setDepreciationStatus("normal");
        
        if (asset.getCurrentValue() == null && asset.getPurchasePrice() != null) {
            if (asset.getPurchaseDate() != null) {
                AssetCategory category = categoryMapper.selectById(asset.getCategoryId());
                if (category == null) {
                    category = new AssetCategory();
                    category.setDepreciationPeriod(60);
                    category.setResidualRate(new BigDecimal("5.00"));
                }
                
                int months = category.getDepreciationPeriod();
                BigDecimal residualRate = category.getResidualRate().divide(new BigDecimal("100"));
                BigDecimal residualValue = asset.getPurchasePrice().multiply(residualRate);
                BigDecimal depreciableValue = asset.getPurchasePrice().subtract(residualValue);
                BigDecimal monthlyAmount = depreciableValue.divide(new BigDecimal(months), 2, BigDecimal.ROUND_HALF_UP);
                
                LocalDate purchaseDate = asset.getPurchaseDate();
                if (purchaseDate == null) {
                    purchaseDate = LocalDate.now();
                }
                LocalDate today = LocalDate.now();
                
                long monthsDiff = java.time.temporal.ChronoUnit.MONTHS.between(purchaseDate, today);
                if (monthsDiff > 0) {
                    int passedMonths = Math.min((int) monthsDiff, months);
                    BigDecimal accumulatedDepreciation = monthlyAmount.multiply(new BigDecimal(passedMonths));
                    if (accumulatedDepreciation.compareTo(depreciableValue) > 0) {
                        accumulatedDepreciation = depreciableValue;
                    }
                    
                    BigDecimal currentValue = asset.getPurchasePrice().subtract(accumulatedDepreciation);
                    if (currentValue.compareTo(residualValue) < 0) {
                        currentValue = residualValue;
                        accumulatedDepreciation = asset.getPurchasePrice().subtract(residualValue);
                    }
                    
                    asset.setCurrentValue(currentValue);
                    asset.setAccumulatedDepreciation(accumulatedDepreciation);
                } else {
                    asset.setCurrentValue(asset.getPurchasePrice());
                    asset.setAccumulatedDepreciation(BigDecimal.ZERO);
                }
            } else {
                asset.setCurrentValue(asset.getPurchasePrice());
                asset.setAccumulatedDepreciation(BigDecimal.ZERO);
            }
        }
        
        if (asset.getAccumulatedDepreciation() == null) {
            asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        }
        
        assetInfoMapper.insert(asset);
        return Result.success("资产入库成功", asset);
    }
    
    @PutMapping("/{id}")
    public Result<AssetInfo> updateAsset(@PathVariable Long id, @RequestBody AssetInfo asset) {
        asset.setId(id);
        assetInfoMapper.updateById(asset);
        return Result.success(asset);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteAsset(@PathVariable Long id) {
        assetInfoMapper.deleteById(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/scrap")
    public Result<Void> scrapAsset(@PathVariable Long id) {
        assetInfoMapper.update(null,
            new UpdateWrapper<AssetInfo>()
                .eq("id", id)
                .set("status", "scrapped")
        );
        return Result.success();
    }

    // ==================== 资产分配管理 ====================
    @GetMapping("/allocation/list")
    public Result<PageResult<AssetAllocation>> getAllocationList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {
        
        Page<AssetAllocation> page = new Page<>(current, size);
        QueryWrapper<AssetAllocation> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (userId != null) {
            wrapper.eq("apply_user_id", userId);
        }
        
        wrapper.orderByDesc("id");
        
        Page<AssetAllocation> resultPage = allocationMapper.selectPage(page, wrapper);
        fillAllocationInfo(resultPage.getRecords());
        
        return Result.success(PageResult.of(
            resultPage.getRecords(),
            resultPage.getTotal(),
            resultPage.getCurrent(),
            resultPage.getSize()
        ));
    }
    
    @PostMapping("/allocation")
    @Transactional
    public Result<AssetAllocation> createAllocation(@RequestBody AssetAllocation allocation) {
        AssetInfo asset = assetInfoMapper.selectById(allocation.getAssetId());
        if (asset == null) {
            return Result.error("资产不存在");
        }
        if (!"in_stock".equals(asset.getStatus())) {
            return Result.error("资产当前不可分配");
        }
        
        allocation.setStatus("pending");
        allocationMapper.insert(allocation);
        return Result.success("申请已提交", allocation);
    }
    
    @PostMapping("/allocation/{id}/approve")
    @Transactional
    public Result<Void> approveAllocation(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        AssetAllocation allocation = allocationMapper.selectById(id);
        if (allocation == null) {
            return Result.error("申请不存在");
        }
        
        Long approverId = params.get("approverId") != null ? Long.valueOf(params.get("approverId").toString()) : null;
        String remark = (String) params.get("remark");
        Boolean approved = (Boolean) params.get("approved");
        
        allocation.setApproverId(approverId);
        allocation.setApproveTime(LocalDateTime.now());
        allocation.setApproveRemark(remark);
        
        if (Boolean.TRUE.equals(approved)) {
            allocation.setStatus("approved");
            
            assetInfoMapper.update(null,
                new UpdateWrapper<AssetInfo>()
                    .eq("id", allocation.getAssetId())
                    .set("status", "in_use")
                    .set("responsible_user_id", allocation.getAssignUserId() != null ? allocation.getAssignUserId() : allocation.getApplyUserId())
                    .set("responsible_dept_id", allocation.getAssignDeptId() != null ? allocation.getAssignDeptId() : allocation.getApplyDeptId())
            );
        } else {
            allocation.setStatus("rejected");
        }
        
        allocationMapper.updateById(allocation);
        return Result.success();
    }
    
    @PostMapping("/allocation/{id}/return")
    @Transactional
    public Result<Void> returnAllocation(@PathVariable Long id) {
        AssetAllocation allocation = allocationMapper.selectById(id);
        if (allocation == null) {
            return Result.error("申请不存在");
        }
        
        allocation.setStatus("returned");
        allocation.setActualReturnDate(LocalDate.now());
        allocationMapper.updateById(allocation);
        
        assetInfoMapper.update(null,
            new UpdateWrapper<AssetInfo>()
                .eq("id", allocation.getAssetId())
                .set("status", "in_stock")
                .set("responsible_user_id", null)
                .set("responsible_dept_id", null)
        );
        
        return Result.success();
    }

    // ==================== 资产变动管理 ====================
    @GetMapping("/transfer/list")
    public Result<PageResult<AssetTransfer>> getTransferList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        
        Page<AssetTransfer> page = new Page<>(current, size);
        QueryWrapper<AssetTransfer> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        
        wrapper.orderByDesc("id");
        
        Page<AssetTransfer> resultPage = transferMapper.selectPage(page, wrapper);
        fillTransferInfo(resultPage.getRecords());
        
        return Result.success(PageResult.of(
            resultPage.getRecords(),
            resultPage.getTotal(),
            resultPage.getCurrent(),
            resultPage.getSize()
        ));
    }
    
    @PostMapping("/transfer")
    @Transactional
    public Result<AssetTransfer> createTransfer(@RequestBody AssetTransfer transfer) {
        AssetInfo asset = assetInfoMapper.selectById(transfer.getAssetId());
        if (asset == null) {
            return Result.error("资产不存在");
        }
        
        transfer.setFromUserId(asset.getResponsibleUserId());
        transfer.setFromDeptId(asset.getResponsibleDeptId());
        transfer.setStatus("pending");
        
        transferMapper.insert(transfer);
        return Result.success("调拨申请已提交", transfer);
    }
    
    @PostMapping("/transfer/{id}/approve")
    @Transactional
    public Result<Void> approveTransfer(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        AssetTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) {
            return Result.error("申请不存在");
        }
        
        Long approverId = params.get("approverId") != null ? Long.valueOf(params.get("approverId").toString()) : null;
        Boolean approved = (Boolean) params.get("approved");
        
        transfer.setApproverId(approverId);
        transfer.setApproveTime(LocalDateTime.now());
        
        if (Boolean.TRUE.equals(approved)) {
            transfer.setStatus("approved");
            transfer.setTransferDate(LocalDate.now());
            
            assetInfoMapper.update(null,
                new UpdateWrapper<AssetInfo>()
                    .eq("id", transfer.getAssetId())
                    .set("responsible_user_id", transfer.getToUserId())
                    .set("responsible_dept_id", transfer.getToDeptId())
            );
        } else {
            transfer.setStatus("rejected");
        }
        
        transferMapper.updateById(transfer);
        return Result.success();
    }

    // ==================== 维修管理 ====================
    @GetMapping("/maintenance/list")
    public Result<PageResult<AssetMaintenance>> getMaintenanceList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        
        Page<AssetMaintenance> page = new Page<>(current, size);
        QueryWrapper<AssetMaintenance> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        
        wrapper.orderByDesc("id");
        
        Page<AssetMaintenance> resultPage = maintenanceMapper.selectPage(page, wrapper);
        
        for (AssetMaintenance m : resultPage.getRecords()) {
            AssetInfo asset = assetInfoMapper.selectById(m.getAssetId());
            if (asset != null) {
                m.setAssetCode(asset.getAssetCode());
                m.setAssetName(asset.getAssetName());
            }
            if (m.getReportUserId() != null) {
                SysUser user = userMapper.selectById(m.getReportUserId());
                if (user != null) {
                    m.setReportUserName(user.getRealName());
                }
            }
        }
        
        return Result.success(PageResult.of(
            resultPage.getRecords(),
            resultPage.getTotal(),
            resultPage.getCurrent(),
            resultPage.getSize()
        ));
    }
    
    @PostMapping("/maintenance")
    @Transactional
    public Result<AssetMaintenance> createMaintenance(@RequestBody AssetMaintenance maintenance) {
        AssetInfo asset = assetInfoMapper.selectById(maintenance.getAssetId());
        if (asset == null) {
            return Result.error("资产不存在");
        }
        
        maintenance.setReportTime(LocalDateTime.now());
        maintenance.setStatus("pending");
        if (maintenance.getMaintenanceCost() == null) maintenance.setMaintenanceCost(BigDecimal.ZERO);
        if (maintenance.getPartsCost() == null) maintenance.setPartsCost(BigDecimal.ZERO);
        if (maintenance.getLaborCost() == null) maintenance.setLaborCost(BigDecimal.ZERO);
        
        maintenanceMapper.insert(maintenance);
        
        assetInfoMapper.update(null,
            new UpdateWrapper<AssetInfo>()
                .eq("id", maintenance.getAssetId())
                .set("status", "maintenance")
        );
        
        return Result.success("报修申请已提交", maintenance);
    }
    
    @PostMapping("/maintenance/{id}/start")
    public Result<Void> startMaintenance(@PathVariable Long id) {
        maintenanceMapper.update(null,
            new UpdateWrapper<AssetMaintenance>()
                .eq("id", id)
                .set("status", "repairing")
                .set("maintenance_start_time", LocalDateTime.now())
        );
        return Result.success();
    }
    
    @PostMapping("/maintenance/{id}/complete")
    @Transactional
    public Result<Void> completeMaintenance(@PathVariable Long id, @RequestBody AssetMaintenance maintenance) {
        AssetMaintenance m = maintenanceMapper.selectById(id);
        if (m == null) {
            return Result.error("记录不存在");
        }
        
        m.setStatus("completed");
        m.setMaintenanceEndTime(LocalDateTime.now());
        m.setMaintenanceContent(maintenance.getMaintenanceContent());
        m.setMaintenanceCost(maintenance.getMaintenanceCost());
        m.setPartsCost(maintenance.getPartsCost());
        m.setLaborCost(maintenance.getLaborCost());
        m.setMaintenancePerson(maintenance.getMaintenancePerson());
        m.setRemark(maintenance.getRemark());
        
        maintenanceMapper.updateById(m);
        
        assetInfoMapper.update(null,
            new UpdateWrapper<AssetInfo>()
                .eq("id", m.getAssetId())
                .set("status", "in_use")
        );
        
        return Result.success();
    }

    // ==================== 折旧管理 ====================
    @GetMapping("/depreciation/list")
    public Result<PageResult<AssetDepreciation>> getDepreciationList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String month) {
        
        Page<AssetDepreciation> page = new Page<>(current, size);
        QueryWrapper<AssetDepreciation> wrapper = new QueryWrapper<>();
        
        if (month != null && !month.isEmpty()) {
            wrapper.eq("depreciation_month", month);
        }
        
        wrapper.orderByDesc("id");
        
        Page<AssetDepreciation> resultPage = depreciationMapper.selectPage(page, wrapper);
        
        for (AssetDepreciation d : resultPage.getRecords()) {
            AssetInfo asset = assetInfoMapper.selectById(d.getAssetId());
            if (asset != null) {
                d.setAssetCode(asset.getAssetCode());
                d.setAssetName(asset.getAssetName());
                AssetCategory cat = categoryMapper.selectById(asset.getCategoryId());
                if (cat != null) {
                    d.setCategoryName(cat.getCategoryName());
                }
            }
        }
        
        return Result.success(PageResult.of(
            resultPage.getRecords(),
            resultPage.getTotal(),
            resultPage.getCurrent(),
            resultPage.getSize()
        ));
    }
    
    @PostMapping("/depreciation/calculate")
    @Transactional
    public Result<Map<String, Object>> calculateDepreciation(@RequestBody Map<String, String> params) {
        String month = params.get("month");
        if (month == null || month.isEmpty()) {
            month = DateUtil.format(new Date(), "yyyy-MM");
        }
        
        List<AssetInfo> assets = assetInfoMapper.selectList(
            new QueryWrapper<AssetInfo>()
                .eq("deleted", 0)
                .ne("status", "scrapped")
                .isNotNull("purchase_price")
        );
        
        int count = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (AssetInfo asset : assets) {
            if (asset.getPurchasePrice() == null || asset.getPurchasePrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            AssetCategory category = categoryMapper.selectById(asset.getCategoryId());
            if (category == null) {
                category = new AssetCategory();
                category.setDepreciationPeriod(60);
                category.setResidualRate(new BigDecimal("5.00"));
            }
            
            QueryWrapper<AssetDepreciation> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("asset_id", asset.getId())
                       .eq("depreciation_month", month);
            if (depreciationMapper.selectCount(checkWrapper) > 0) {
                continue;
            }
            
            int months = category.getDepreciationPeriod();
            BigDecimal residualRate = category.getResidualRate().divide(new BigDecimal("100"));
            BigDecimal residualValue = asset.getPurchasePrice().multiply(residualRate);
            BigDecimal depreciableValue = asset.getPurchasePrice().subtract(residualValue);
            BigDecimal monthlyAmount = depreciableValue.divide(new BigDecimal(months), 2, BigDecimal.ROUND_HALF_UP);
            
            BigDecimal currentValueBefore = asset.getCurrentValue() != null ? asset.getCurrentValue() : asset.getPurchasePrice();
            BigDecimal accumulatedDepreciation = asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO;
            BigDecimal currentValueAfter = currentValueBefore.subtract(monthlyAmount);
            if (currentValueAfter.compareTo(residualValue) < 0) {
                monthlyAmount = currentValueBefore.subtract(residualValue);
                currentValueAfter = residualValue;
            }
            
            AssetDepreciation depreciation = new AssetDepreciation();
            depreciation.setAssetId(asset.getId());
            depreciation.setDepreciationMonth(month);
            depreciation.setOriginalValue(asset.getPurchasePrice());
            depreciation.setCurrentValueBefore(currentValueBefore);
            depreciation.setDepreciationAmount(monthlyAmount);
            depreciation.setAccumulatedDepreciation(accumulatedDepreciation.add(monthlyAmount));
            depreciation.setCurrentValueAfter(currentValueAfter);
            depreciation.setDepreciationRate(monthlyAmount.divide(asset.getPurchasePrice(), 4, BigDecimal.ROUND_HALF_UP));
            
            depreciationMapper.insert(depreciation);
            
            assetInfoMapper.update(null,
                new UpdateWrapper<AssetInfo>()
                    .eq("id", asset.getId())
                    .set("accumulated_depreciation", depreciation.getAccumulatedDepreciation())
                    .set("current_value", currentValueAfter)
            );
            
            count++;
            totalAmount = totalAmount.add(monthlyAmount);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("totalAmount", totalAmount);
        result.put("month", month);
        
        return Result.success("折旧计算完成", result);
    }

    // ==================== 报表统计 ====================
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        
        List<AssetInfo> allAssets = assetInfoMapper.selectList(
            new QueryWrapper<AssetInfo>().eq("deleted", 0)
        );
        
        Map<String, Object> stats = new HashMap<>();
        int total = allAssets.size();
        int inStock = 0, assigned = 0, inUse = 0, maintenance = 0, scrapped = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal netValue = BigDecimal.ZERO;
        
        for (AssetInfo asset : allAssets) {
            String status = asset.getStatus();
            if ("in_stock".equals(status)) inStock++;
            else if ("assigned".equals(status)) assigned++;
            else if ("in_use".equals(status)) inUse++;
            else if ("maintenance".equals(status)) maintenance++;
            else if ("scrapped".equals(status)) scrapped++;
            
            if (asset.getPurchasePrice() != null) {
                totalValue = totalValue.add(asset.getPurchasePrice());
            }
            if (asset.getCurrentValue() != null) {
                netValue = netValue.add(asset.getCurrentValue());
            }
        }
        
        stats.put("total", total);
        stats.put("inStock", inStock);
        stats.put("assigned", assigned);
        stats.put("inUse", inUse);
        stats.put("maintenance", maintenance);
        stats.put("scrapped", scrapped);
        stats.put("totalValue", totalValue);
        stats.put("netValue", netValue);
        
        data.put("stats", stats);
        
        Map<Long, String> deptMap = new HashMap<>();
        List<SysDept> depts = deptMapper.selectList(new QueryWrapper<SysDept>().eq("deleted", 0));
        for (SysDept d : depts) {
            deptMap.put(d.getId(), d.getDeptName());
        }
        
        Map<Long, Integer> deptCount = new HashMap<>();
        for (AssetInfo asset : allAssets) {
            Long deptId = asset.getResponsibleDeptId();
            if (deptId != null) {
                deptCount.put(deptId, deptCount.getOrDefault(deptId, 0) + 1);
            }
        }
        
        List<Map<String, Object>> byDept = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : deptCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", deptMap.getOrDefault(entry.getKey(), "未分配"));
            item.put("value", entry.getValue());
            byDept.add(item);
        }
        byDept.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));
        data.put("byDept", byDept);
        
        Map<Long, String> catMap = new HashMap<>();
        List<AssetCategory> cats = categoryMapper.selectList(new QueryWrapper<AssetCategory>().eq("deleted", 0));
        for (AssetCategory c : cats) {
            catMap.put(c.getId(), c.getCategoryName());
        }
        
        Map<Long, Integer> catCount = new HashMap<>();
        for (AssetInfo asset : allAssets) {
            Long catId = asset.getCategoryId();
            if (catId != null) {
                catCount.put(catId, catCount.getOrDefault(catId, 0) + 1);
            }
        }
        
        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : catCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", catMap.getOrDefault(entry.getKey(), "未分类"));
            item.put("value", entry.getValue());
            byCategory.add(item);
        }
        byCategory.sort((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")));
        data.put("byCategory", byCategory);
        
        data.put("expiring", new ArrayList<>());
        
        return Result.success(data);
    }
    
    @GetMapping("/report/summary")
    public Result<Map<String, Object>> getSummaryReport() {
        Map<String, Object> data = new HashMap<>();
        
        long total = assetInfoMapper.selectCount(
            new QueryWrapper<AssetInfo>().eq("deleted", 0)
        );
        
        Map<String, Object> statusCount = new HashMap<>();
        String[] statuses = {"in_stock", "assigned", "in_use", "maintenance", "scrapped"};
        for (String s : statuses) {
            long c = assetInfoMapper.selectCount(
                new QueryWrapper<AssetInfo>().eq("deleted", 0).eq("status", s)
            );
            statusCount.put(s, c);
        }
        
        List<Map<String, Object>> deptAssets = new ArrayList<>();
        List<SysDept> depts = deptMapper.selectList(new QueryWrapper<SysDept>().eq("deleted", 0));
        for (SysDept dept : depts) {
            Map<String, Object> item = new HashMap<>();
            item.put("deptId", dept.getId());
            item.put("deptName", dept.getDeptName());
            
            long count = assetInfoMapper.selectCount(
                new QueryWrapper<AssetInfo>()
                    .eq("deleted", 0)
                    .eq("responsible_dept_id", dept.getId())
            );
            item.put("assetCount", count);
            deptAssets.add(item);
        }
        
        data.put("totalAssets", total);
        data.put("statusCount", statusCount);
        data.put("deptAssets", deptAssets);
        
        return Result.success(data);
    }
    
    @GetMapping("/report/expiring")
    public Result<List<Map<String, Object>>> getExpiringReport() {
        List<AssetInfo> assets = assetInfoMapper.selectList(
            new QueryWrapper<AssetInfo>()
                .eq("deleted", 0)
                .ne("status", "scrapped")
                .orderByAsc("purchase_date")
        );
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssetInfo asset : assets) {
            if (asset.getPurchaseDate() == null) continue;
            
            AssetCategory cat = categoryMapper.selectById(asset.getCategoryId());
            int months = cat != null ? cat.getDepreciationPeriod() : 60;
            
            LocalDate scrapDate = asset.getPurchaseDate().plusMonths(months);
            long daysToScrap = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), scrapDate);
            
            if (daysToScrap <= 90) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", asset.getId());
                item.put("assetCode", asset.getAssetCode());
                item.put("assetName", asset.getAssetName());
                item.put("purchaseDate", asset.getPurchaseDate());
                item.put("scrapDate", scrapDate);
                item.put("daysToScrap", daysToScrap);
                item.put("status", asset.getStatus());
                item.put("currentValue", asset.getCurrentValue());
                result.add(item);
            }
        }
        
        return Result.success(result);
    }

    // ==================== 辅助方法 ====================
    private String generateAssetCode(Long categoryId) {
        String prefix = "AST";
        if (categoryId != null) {
            AssetCategory cat = categoryMapper.selectById(categoryId);
            if (cat != null && cat.getCategoryCode() != null) {
                prefix = cat.getCategoryCode().toUpperCase();
            }
        }
        
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        
        long count = assetInfoMapper.selectCount(
            new QueryWrapper<AssetInfo>()
                .likeRight("asset_code", prefix + dateStr)
        );
        
        return prefix + dateStr + String.format("%04d", count + 1);
    }
    
    private void fillAllocationInfo(List<AssetAllocation> list) {
        for (AssetAllocation item : list) {
            if (item.getAssetId() != null) {
                AssetInfo asset = assetInfoMapper.selectById(item.getAssetId());
                if (asset != null) {
                    item.setAssetCode(asset.getAssetCode());
                    item.setAssetName(asset.getAssetName());
                }
            }
            if (item.getApplyUserId() != null) {
                SysUser user = userMapper.selectById(item.getApplyUserId());
                if (user != null) item.setApplyUserName(user.getRealName());
            }
            if (item.getApproverId() != null) {
                SysUser user = userMapper.selectById(item.getApproverId());
                if (user != null) item.setApproverName(user.getRealName());
            }
            if (item.getAssignUserId() != null) {
                SysUser user = userMapper.selectById(item.getAssignUserId());
                if (user != null) item.setAssignUserName(user.getRealName());
            }
        }
    }
    
    private void fillTransferInfo(List<AssetTransfer> list) {
        for (AssetTransfer item : list) {
            if (item.getAssetId() != null) {
                AssetInfo asset = assetInfoMapper.selectById(item.getAssetId());
                if (asset != null) {
                    item.setAssetCode(asset.getAssetCode());
                    item.setAssetName(asset.getAssetName());
                }
            }
            if (item.getFromUserId() != null) {
                SysUser user = userMapper.selectById(item.getFromUserId());
                if (user != null) item.setFromUserName(user.getRealName());
            }
            if (item.getToUserId() != null) {
                SysUser user = userMapper.selectById(item.getToUserId());
                if (user != null) item.setToUserName(user.getRealName());
            }
            if (item.getFromDeptId() != null) {
                SysDept dept = deptMapper.selectById(item.getFromDeptId());
                if (dept != null) item.setFromDeptName(dept.getDeptName());
            }
            if (item.getToDeptId() != null) {
                SysDept dept = deptMapper.selectById(item.getToDeptId());
                if (dept != null) item.setToDeptName(dept.getDeptName());
            }
        }
    }
}
