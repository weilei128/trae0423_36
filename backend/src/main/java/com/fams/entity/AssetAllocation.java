package com.fams.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("asset_allocation")
public class AssetAllocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private Long applyUserId;
    private Long applyDeptId;
    private String applyReason;
    private String applyType;
    private String status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveRemark;
    private Long assignUserId;
    private Long assignDeptId;
    private LocalDate startDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
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
    private String applyUserName;
    @TableField(exist = false)
    private String approverName;
    @TableField(exist = false)
    private String assignUserName;
    @TableField(exist = false)
    private String categoryName;
}
