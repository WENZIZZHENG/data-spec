package com.dataspec.standardcandidate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标准候选 Inbox 记录。
 */
@Data
@TableName("ds_standard_candidate")
public class StandardCandidate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String candidateName;

    private String displayName;

    private String dataType;

    private String comment;

    private String sourceType;

    private String sourceRef;

    private String evidenceJson;

    private Integer confidence;

    private String status;

    private Long targetFieldId;

    private String decisionReason;

    private LocalDateTime decidedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
