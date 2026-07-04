package com.dataspec.reverseimport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据库反向导入字段映射决策。
 *
 * <p>该记录只保存 schema metadata 与用户确认理由，不允许写入数据库密码、token、JDBC URL 或业务数据行。</p>
 */
@Data
@TableName("ds_reverse_import_decision")
public class ReverseImportDecision {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long batchId;
    private String sourceType;
    private String schemaName;
    private String tableName;
    private String columnName;
    private String dataType;
    private String decisionType;
    private Long matchedFieldId;
    private String matchedFieldName;
    private String matchReason;
    private BigDecimal confidence;
    private String ignoreReason;
    private String confirmReason;
    private String metadataJson;
    private LocalDateTime createdAt;
}
