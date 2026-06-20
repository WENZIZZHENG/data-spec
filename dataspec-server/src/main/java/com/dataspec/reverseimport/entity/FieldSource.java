package com.dataspec.reverseimport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标准字段来源记录。
 */
@Data
@TableName("ds_field_source")
public class FieldSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long fieldId;
    private Long batchId;
    private String sourceType;
    private String schemaName;
    private String tableName;
    private String columnName;
    private String dataType;
    private Boolean nullable;
    private String defaultValue;
    private String comment;
    private String metadataJson;
    private LocalDateTime createdAt;
}
