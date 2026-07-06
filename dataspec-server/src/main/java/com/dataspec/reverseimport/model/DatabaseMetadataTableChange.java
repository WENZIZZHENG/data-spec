package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * metadata cache 表级结构变化摘要，不包含源库业务数据行。
 */
@Data
public class DatabaseMetadataTableChange {

    /** 表所在 schema；MySQL 场景可能为空。 */
    private String schemaName;

    /** 发生变化的表名。 */
    private String tableName;

    /** 表变化类型：ADDED/REMOVED/CHANGED/UNCHANGED。 */
    private String changeType;

    /** 旧缓存表结构 fingerprint；新增表可能为空。 */
    private String oldFingerprint;

    /** 刷新后表结构 fingerprint；删除表可能为空。 */
    private String newFingerprint;

    /** 新增字段名列表，已限制在 schema-only metadata 范围内。 */
    private List<String> addedColumns = new ArrayList<>();

    /** 删除字段名列表，已限制在 schema-only metadata 范围内。 */
    private List<String> removedColumns = new ArrayList<>();

    /** 字段属性变化列表。 */
    private List<DatabaseMetadataColumnChange> changedColumns = new ArrayList<>();
}
