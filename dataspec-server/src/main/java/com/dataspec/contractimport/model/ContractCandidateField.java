package com.dataspec.contractimport.model;

import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 单个契约字段候选，保留来源证据和建议动作但不触发持久化写入。
 *
 * @param candidateKey 稳定候选 key，由来源类型和规范化字段名组成。
 * @param candidateName 规范化后的标准候选字段名，优先使用 snake_case。
 * @param displayName 候选人读名称，来自契约 description、comment 或原字段名。
 * @param dataType DataSpec 可理解的数据类型摘要。
 * @param required 契约是否声明该字段必填。
 * @param enumValues 脱敏枚举值摘要。
 * @param exampleValues 脱敏示例值摘要。
 * @param sourcePath 字段在契约中的稳定来源路径。
 * @param schemaVersion 候选字段输出 schema 版本。
 * @param confidence 候选置信度，复杂 schema 会降低。
 * @param conflictReasons 匹配已有标准、重复来源或复杂 schema 的脱敏原因。
 * @param recommendedAction 建议动作：CREATE_CANDIDATE、MERGE_EXISTING 或 REVIEW_REQUIRED。
 * @param inboxPayload 可复用现有标准候选创建语义的只读 payload。
 */
@Schema(description = "单个契约字段候选；保留脱敏来源证据和建议动作，但不触发持久化写入。")
public record ContractCandidateField(
        @Schema(description = "稳定候选 key，由来源类型和规范化字段名组成。")
        String candidateKey,
        @Schema(description = "规范化后的标准候选字段名，优先使用 snake_case。")
        String candidateName,
        @Schema(description = "候选人读名称，来自契约 description、comment 或原字段名，输出前已脱敏。")
        String displayName,
        @Schema(description = "DataSpec 可理解的数据类型摘要，例如 bigint、decimal、timestamp 或 json。")
        String dataType,
        @Schema(description = "契约是否声明该字段必填。")
        boolean required,
        @Schema(description = "脱敏枚举值摘要；为空表示契约未声明枚举或枚举被截断。")
        List<String> enumValues,
        @Schema(description = "脱敏示例值摘要；最多保留少量示例用于人工复核。")
        List<String> exampleValues,
        @Schema(description = "字段在契约中的脱敏稳定来源路径。")
        String sourcePath,
        @Schema(description = "候选字段输出 schema 版本。")
        int schemaVersion,
        @Schema(description = "候选置信度；复杂 schema 或需要人工复核时会降低。")
        int confidence,
        @Schema(description = "匹配已有标准、重复来源或复杂 schema 的脱敏原因。")
        List<String> conflictReasons,
        @Schema(description = "建议动作：CREATE_CANDIDATE、MERGE_EXISTING 或 REVIEW_REQUIRED。")
        String recommendedAction,
        @Schema(description = "兼容现有标准候选创建语义的只读 payload；仅供人工复核后另行提交，不会由预览自动持久化。")
        StandardCandidateCreateReq inboxPayload
) {
}
