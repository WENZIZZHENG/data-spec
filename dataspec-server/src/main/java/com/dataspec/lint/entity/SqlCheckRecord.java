package com.dataspec.lint.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SQL 检查记录 —— 保存每次校验的原 SQL、修正 SQL、问题统计与结构化结果
 */
@Data
@TableName("ds_sql_check_record")
public class SqlCheckRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目(匿名校验时为 null) */
    private Long projectId;

    /** 原始 SQL 文本 */
    private String originalSql;

    /** 修正后的 SQL,无法重建时为 null */
    private String fixedSql;

    /** 错误数量 */
    private Integer errorCount;

    /** 警告数量 */
    private Integer warningCount;

    /** 建议数量 */
    private Integer suggestionCount;

    /** 结构化校验问题 JSON(序列化后的 List<LintIssue>) */
    private String issuesJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
