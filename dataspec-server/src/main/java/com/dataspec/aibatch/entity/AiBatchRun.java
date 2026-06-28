package com.dataspec.aibatch.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 批量任务运行记录。摘要用于列表，payload 保存可下载的稳定交付包。
 */
@Data
@TableName("ds_ai_batch_run")
public class AiBatchRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String batchType;
    private String source;
    private String status;
    private String summaryJson;
    private String payloadJson;
    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
