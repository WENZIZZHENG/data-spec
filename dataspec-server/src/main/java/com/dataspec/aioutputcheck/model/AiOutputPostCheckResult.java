package com.dataspec.aioutputcheck.model;

import com.dataspec.standardref.model.StandardReferenceResolutionResult;
import com.dataspec.reviewfinding.model.ReviewFinding;
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
 * @param findings 经脱敏、去重和 evidence gating 的共享 findings。
 * @param resolvedRefs 标准引用解析详情。
 * @param suggestedFixes 脱敏修复建议摘要。
 * @param evidenceLinks 可复核的只读证据链接。
 * @param nextActions AI/用户下一步动作。
 * @param verificationReceipt PASS 时签发的进程内验证 receipt；用于把同一组规范化 findings 交给 Evidence Package，
 *                            不绑定 package 来源且同一进程内可重复使用。
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
        @ArraySchema(
                arraySchema = @Schema(description = "经脱敏、去重和 evidence gating 的共享 findings；空结果返回空数组。"),
                schema = @Schema(implementation = ReviewFinding.class))
        List<ReviewFinding> findings,
        @ArraySchema(schema = @Schema(description = "标准引用解析详情。"))
        List<StandardReferenceResolutionResult> resolvedRefs,
        @ArraySchema(schema = @Schema(description = "脱敏修复建议摘要。"))
        List<String> suggestedFixes,
        @ArraySchema(schema = @Schema(description = "可复核的只读证据链接。"))
        List<String> evidenceLinks,
        @ArraySchema(schema = @Schema(description = "AI/用户下一步动作。"))
        List<String> nextActions,
        @Schema(description = "仅 PASS 时签发的进程内 HMAC receipt；绑定项目、状态和完整规范化 findings 摘要，不绑定 Evidence Package 来源，同一进程内可为相同输入重复使用，服务重启后失效。",
                nullable = true)
        String verificationReceipt
) {
    public static final String KIND = "dataspec-ai-output-postcheck";
    public static final int SCHEMA_VERSION = 1;

    public AiOutputPostCheckResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        findings = findings == null ? List.of() : List.copyOf(findings);
        resolvedRefs = resolvedRefs == null ? List.of() : List.copyOf(resolvedRefs);
        suggestedFixes = suggestedFixes == null ? List.of() : List.copyOf(suggestedFixes);
        evidenceLinks = evidenceLinks == null ? List.of() : List.copyOf(evidenceLinks);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }

    /** 兼容未消费共享 findings 的既有服务与测试构造。 */
    public AiOutputPostCheckResult(
            String kind,
            int schemaVersion,
            Long projectId,
            AiOutputPostCheckStatus status,
            boolean safeToUse,
            AiOutputPostCheckSummary summary,
            List<AiOutputPostCheckIssue> issues,
            List<StandardReferenceResolutionResult> resolvedRefs,
            List<String> suggestedFixes,
            List<String> evidenceLinks,
            List<String> nextActions
    ) {
        this(kind, schemaVersion, projectId, status, safeToUse, summary, issues, List.of(), resolvedRefs,
                suggestedFixes, evidenceLinks, nextActions, null);
    }

    /** 兼容已显式构造 findings、但尚未消费 verificationReceipt 的调用方。 */
    public AiOutputPostCheckResult(
            String kind,
            int schemaVersion,
            Long projectId,
            AiOutputPostCheckStatus status,
            boolean safeToUse,
            AiOutputPostCheckSummary summary,
            List<AiOutputPostCheckIssue> issues,
            List<ReviewFinding> findings,
            List<StandardReferenceResolutionResult> resolvedRefs,
            List<String> suggestedFixes,
            List<String> evidenceLinks,
            List<String> nextActions
    ) {
        this(kind, schemaVersion, projectId, status, safeToUse, summary, issues, findings, resolvedRefs,
                suggestedFixes, evidenceLinks, nextActions, null);
    }
}
