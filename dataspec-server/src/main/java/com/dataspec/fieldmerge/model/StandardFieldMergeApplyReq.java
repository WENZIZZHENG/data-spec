package com.dataspec.fieldmerge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准字段合并确认请求。
 *
 * @param projectId     DataSpec 项目 ID，必须与两个字段归属一致。
 * @param targetFieldId 保留字段 ID。
 * @param sourceFieldId 来源字段 ID。
 * @param reason        用户确认合并的业务原因，会写入来源字段 replacementReason。
 */
public record StandardFieldMergeApplyReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotNull(message = "保留字段ID不能为空") Long targetFieldId,
        @NotNull(message = "来源字段ID不能为空") Long sourceFieldId,
        @NotBlank(message = "合并原因不能为空") String reason
) {
}
