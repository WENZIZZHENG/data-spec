package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 metadata 浏览器中的字段行，合并 schema metadata、标准匹配和候选选择状态。
 */
@Data
public class DatabaseMetadataBrowserColumn {

    /** 字段所属 schema；MySQL 场景可能为空。 */
    private String schemaName;

    /** 字段所属表名。 */
    private String tableName;

    /** 数据库字段名。 */
    private String columnName;

    /** 数据库字段类型，按 JDBC metadata 规范化后的展示值。 */
    private String dataType;

    /** true 表示数据库字段允许 NULL。 */
    private Boolean nullable;

    /** 数据库字段默认值；仅保存 metadata，不保存业务数据。 */
    private String defaultValue;

    /** 数据库字段注释。 */
    private String comment;

    /** 命中的标准字段名；未命中时为空。 */
    private String standardFieldName;

    /** 命中的标准字段展示名；未命中时为空。 */
    private String standardDisplayName;

    /** 浏览器状态，如 MATCHED、CHANGED、NEW、MISSING_COMMENT、UNMANAGED。 */
    private String matchStatus;

    /** 标准匹配、差异或候选生成原因。 */
    private String matchReason;

    /** 前端候选选择使用的稳定 key，格式为 table.column。 */
    private String candidateKey;

    /** true 表示该字段是可加入标准库的导入候选。 */
    private boolean importCandidate;

    /** true 表示浏览器首次展示时默认勾选该候选。 */
    private boolean selectedByDefault;

    /** true 表示该字段或所在表缺少注释。 */
    private boolean missingComment;

    /** true 表示字段类型与命中的标准字段存在差异。 */
    private boolean typeChanged;

    /** true 表示当前项目标准字段库尚未纳管该字段。 */
    private boolean unmanaged;

    /** 覆盖该字段的索引名列表。 */
    private List<String> indexNames = new ArrayList<>();

    /** 字段与标准之间的属性差异明细。 */
    private List<ReverseImportFieldChange> changes = new ArrayList<>();
}
