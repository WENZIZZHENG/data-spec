package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 数据库 schema plan 中的字段级变更动作。动作只描述 dry-run 预览，不代表 DataSpec 会执行数据库迁移。
 */
@Schema(description = "数据库 schema plan 中的字段级变更动作；仅描述 dry-run 预览，不代表 DataSpec 会执行数据库迁移。")
public enum DatabaseSchemaChangeAction {
    /** 补齐或更新字段注释。 */
    @Schema(description = "补齐或更新字段注释的低风险草案动作。")
    ALTER_COMMENT,
    /** 修改字段类型、是否可空或默认值等结构属性。 */
    @Schema(description = "字段类型、是否可空或默认值等结构属性变更；第一版只输出 REVIEW 草案。")
    ALTER_COLUMN,
    /** 未纳管字段的删除候选；第一版只作为人工确认项，不生成可执行 DROP。 */
    @Schema(description = "未纳管字段的删除候选；只作为人工确认项，不生成可执行 DROP。")
    DROP_CANDIDATE
}
