package com.dataspec.aireplay.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成与修复决策回放记录。
 */
@Data
@TableName("ds_ai_job_record")
public class AiJobRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String jobType;
    private String title;
    private String inputSummary;
    private String promptVersion;
    private String status;
    private String inputPayloadJson;
    private String outputPayloadJson;
    private Long sqlCheckRecordId;
    private Long standardSnapshotId;
    private String standardSnapshotVersion;
    private String standardSnapshotHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
