package com.fams.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asset_depreciation")
public class AssetDepreciation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String depreciationMonth;
    private BigDecimal originalValue;
    private BigDecimal currentValueBefore;
    private BigDecimal depreciationAmount;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal currentValueAfter;
    private BigDecimal depreciationRate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(exist = false)
    private String assetCode;
    @TableField(exist = false)
    private String assetName;
    @TableField(exist = false)
    private String categoryName;
}
