package com.dataspec.reverseimport.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 数据库 metadata 结构缓存记录。该实体只保存 schema metadata 快照和 hash，不保存密码、JDBC URL 或业务数据行。
 */
@Getter
@Setter
@TableName("ds_database_metadata_cache")
public class DatabaseMetadataCacheEntry {

    /** 缓存记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 DataSpec 项目 ID。 */
    private Long projectId;

    /** 可选数据库连接预设 ID；为空时使用 sourceScopeHash 隔离来源。 */
    private Long presetId;

    /** 由非密码连接字段规范化后计算的 SHA-256 hash，不可反推出凭据。 */
    private String sourceScopeHash;

    /** 数据库类型，如 POSTGRESQL/MYSQL。 */
    private String databaseType;

    /** 数据库名，仅保存非敏感名称并在写入前脱敏。 */
    private String databaseName;

    /** schema 名；MySQL 场景可能为空。 */
    private String schemaName;

    /** 表名。 */
    private String tableName;

    /** 当前表结构规范化后的 SHA-256 fingerprint。 */
    private String tableFingerprint;

    /** 表结构 metadata JSON，仅包含表、字段、索引和注释等 schema 信息。 */
    private String metadataJson;

    /** 源数据库产品名，已脱敏。 */
    private String sourceProductName;

    /** 源数据库版本，已脱敏。 */
    private String sourceProductVersion;

    /** 首次看见该表结构来源的时间。 */
    private LocalDateTime firstSeenAt;

    /** 最近一次从源库刷新或确认该表结构的时间。 */
    private LocalDateTime lastSeenAt;

    /** 缓存过期时间。 */
    private LocalDateTime expiresAt;

    /** 最近一次写入缓存使用的刷新策略。 */
    private String refreshMode;

    /** 最近一次刷新生成的结构变化摘要 JSON，不包含业务数据行。 */
    private String changeSummaryJson;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记。 */
    @TableLogic
    private Boolean isDeleted;
}
