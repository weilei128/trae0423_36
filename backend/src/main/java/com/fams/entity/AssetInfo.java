package com.fams.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("asset_info")
public class AssetInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String brand;
    private String model;
    private String spec;
    private String sn;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private String supplier;
    private String location;
    private String description;
    private String status;
    private String depreciationStatus;
    private BigDecimal currentValue;
    private BigDecimal accumulatedDepreciation;
    private Long responsibleUserId;
    private Long responsibleDeptId;
    private Long createUserId;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String responsibleUserName;
    @TableField(exist = false)
    private String responsibleDeptName;
    @TableField(exist = false)
    private Integer daysToScrap;
}
