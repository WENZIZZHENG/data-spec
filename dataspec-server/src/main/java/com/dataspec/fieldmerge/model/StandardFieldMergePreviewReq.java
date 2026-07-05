package com.dataspec.fieldmerge.model;

import jakarta.validation.constraints.NotNull;

/**
 * 标准字段合并预览请求。
 *
 * @param projectId     DataSpec 项目 ID，合并只能在同一项目内执行。
 * @param targetFieldId 保留字段 ID，apply 时仅迁移安全 metadata 到该字段。
 * @param sourceFieldId 来源字段 ID，apply 后会被标记为 deprecated 并指向保留字段。
 */
public record StandardFieldMergePreviewReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotNull(message = "保留字段ID不能为空") Long targetFieldId,
        @NotNull(message = "来源字段ID不能为空") Long sourceFieldId
) {
}
