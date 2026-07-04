package com.dataspec.aitaskrun.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 任务运行状态，用于失败重试与断点恢复诊断。
 */
@Data
@TableName("ds_ai_task_run")
public class AiTaskRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String taskType;
    private String sourceType;
    private Long sourceId;
    private String status;
    private String inputHash;
    private String idempotencyKey;
    private String stepStatusJson;
    private Boolean retryable;
    private String failedStep;
    private String resumeCommand;
    private String nextAction;
    private String partialArtifactsJson;
    private String metadataJson;
    private String operatorName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
