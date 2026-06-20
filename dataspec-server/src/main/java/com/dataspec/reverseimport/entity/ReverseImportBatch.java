package com.dataspec.reverseimport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库反向导入批次。
 */
@Data
@TableName("ds_reverse_import_batch")
public class ReverseImportBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String sourceType;
    private String databaseType;
    private String databaseName;
    private String schemaName;
    private String tableNamesJson;
    private Integer importedCount;
    private Integer skippedCount;
    private String operatorName;
    private LocalDateTime createdAt;
}
