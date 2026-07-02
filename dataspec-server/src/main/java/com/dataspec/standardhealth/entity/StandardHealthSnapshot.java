package com.dataspec.standardhealth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级标准健康快照。
 */
@Data
@TableName("ds_standard_health_snapshot")
public class StandardHealthSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private LocalDateTime capturedAt;

    private String source;

    private Integer averageQualityScore;

    private Integer lowQualityFieldCount;

    private Integer totalFieldCount;

    private String coverageStatus;

    private Double coverageRate;

    private Integer unmanagedFieldCount;

    private Integer missingCommentCount;

    private Integer possibleDuplicateCount;

    private Integer ruleIssueCount;

    private Integer ruleExemptionCount;

    private Integer aiFeedbackSignalCount;

    private Integer pendingCandidateCount;

    private Integer adoptedCandidateCount;

    private Integer fixedSqlAvailableCount;

    private String topActionsJson;

    private String payloadJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
