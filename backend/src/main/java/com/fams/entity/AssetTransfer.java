package com.fams.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("asset_transfer")
public class AssetTransfer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String transferType;
    private Long fromUserId;
    private Long fromDeptId;
    private Long toUserId;
    private Long toDeptId;
    private String transferReason;
    private String status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveRemark;
    private LocalDate transferDate;
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
    private String fromUserName;
    @TableField(exist = false)
    private String toUserName;
    @TableField(exist = false)
    private String fromDeptName;
    @TableField(exist = false)
    private String toDeptName;
}
