package com.dataspec.aioutputcheck.model;

import com.dataspec.standardref.model.StandardReferenceResolutionResult;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AI 输出后置校验结果。
 *
 * @param kind 稳定响应类型标识。
 * @param schemaVersion 结果 schema 版本；breaking 变更必须升级。
 * @param projectId 当前项目 ID。
 * @param status PASS/WARN/FAIL 总体状态。
 * @param safeToUse 产物是否可直接复制、下载或执行；只有 PASS 为 true。
 * @param summary 引用解析与问题计数摘要。
 * @param issues 阻断或需确认的问题列表。
 * @param resolvedRefs 标准引用解析详情。
 * @param suggestedFixes 脱敏修复建议摘要。
 * @param evidenceLinks 可复核的只读证据链接。
 * @param nextActions AI/用户下一步动作。
 */
@Schema(description = "AI 输出后置校验结果；只读、确定性、bounded 且 secret-safe。")
public record AiOutputPostCheckResult(
        @Schema(description = "稳定响应类型标识。", example = "dataspec-ai-output-postcheck")
        String kind,
        @Schema(description = "结果 schema 版本。", example = "1")
        int schemaVersion,
        @Schema(description = "当前项目 ID。", example = "1")
        Long projectId,
        @Schema(description = "PASS/WARN/FAIL 总体状态。")
        AiOutputPostCheckStatus status,
        @Schema(description = "是否可直接使用该 AI 产物；只有 PASS 为 true。")
        boolean safeToUse,
        @Schema(description = "引用解析与问题计数摘要。")
        AiOutputPostCheckSummary summary,
        @ArraySchema(schema = @Schema(description = "阻断或需确认的问题列表。"))
        List<AiOutputPostCheckIssue> issues,
        @ArraySchema(schema = @Schema(description = "标准引用解析详情。"))
        List<StandardReferenceResolutionResult> resolvedRefs,
        @ArraySchema(schema = @Schema(description = "脱敏修复建议摘要。"))
        List<String> suggestedFixes,
        @ArraySchema(schema = @Schema(description = "可复核的只读证据链接。"))
        List<String> evidenceLinks,
        @ArraySchema(schema = @Schema(description = "AI/用户下一步动作。"))
        List<String> nextActions
) {
    public static final String KIND = "dataspec-ai-output-postcheck";
    public static final int SCHEMA_VERSION = 1;

    public AiOutputPostCheckResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        resolvedRefs = resolvedRefs == null ? List.of() : List.copyOf(resolvedRefs);
        suggestedFixes = suggestedFixes == null ? List.of() : List.copyOf(suggestedFixes);
        evidenceLinks = evidenceLinks == null ? List.of() : List.copyOf(evidenceLinks);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
