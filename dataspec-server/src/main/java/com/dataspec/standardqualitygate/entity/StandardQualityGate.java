package com.dataspec.standardqualitygate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级标准质量门禁配置。门禁只用于显式检查，不直接阻断字段编辑。
 */
@Data
@TableName("ds_standard_quality_gate")
public class StandardQualityGate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Boolean enabled;
    private Integer minCoverage;
    private Integer minAverageFieldScore;
    private Integer maxErrorIssues;
    private Integer maxNewUnmanagedFields;
    private Boolean requiredSensitiveMarking;
    private String configJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
