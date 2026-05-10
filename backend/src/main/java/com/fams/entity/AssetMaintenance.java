package com.fams.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asset_maintenance")
public class AssetMaintenance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private Long reportUserId;
    private LocalDateTime reportTime;
    private String faultDescription;
    private String faultLocation;
    private String status;
    private String maintenancePerson;
    private LocalDateTime maintenanceStartTime;
    private LocalDateTime maintenanceEndTime;
    private String maintenanceContent;
    private BigDecimal maintenanceCost;
    private BigDecimal partsCost;
    private BigDecimal laborCost;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String assetCode;
    @TableField(exist = false)
    private String assetName;
    @TableField(exist = false)
    private String reportUserName;
}
