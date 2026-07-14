package com.dataspec.standardcandidate.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 命名证据候选确认写入结果。
 *
 * @param kind          稳定响应类型
 * @param schemaVersion 响应 schema 版本
 * @param created       本次是否插入新候选
 * @param deduplicated  本次是否命中既有同一事实
 * @param candidate     新建或已存在的同一候选
 * @param nextActions   进入既有候选决策流程的下一步
 */
@Schema(description = "命名证据候选确认写入结果；不会自动采纳或修改标准字段。")
public record TokenEvidenceCandidateApplyResult(
        @Schema(description = "稳定类型，固定为 dataspec.token-evidence-candidate-apply-result。", requiredMode = Schema.RequiredMode.REQUIRED) String kind,
        @Schema(description = "响应 schema 版本，当前为 1。", requiredMode = Schema.RequiredMode.REQUIRED) int schemaVersion,
        @Schema(description = "本次是否插入新候选。", requiredMode = Schema.RequiredMode.REQUIRED) boolean created,
        @Schema(description = "本次是否因完整事实已存在而返回既有候选。", requiredMode = Schema.RequiredMode.REQUIRED) boolean deduplicated,
        @Schema(description = "新建或已存在的 TOKEN_EVIDENCE 候选安全视图。", requiredMode = Schema.RequiredMode.REQUIRED)
        TokenEvidenceCandidateView candidate,
        @ArraySchema(
                arraySchema = @Schema(description = "进入既有候选决策流程的下一步。", requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(description = "单条下一步建议。"))
        List<String> nextActions
) {
}
