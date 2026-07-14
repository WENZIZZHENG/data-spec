package com.dataspec.evidence.model;

import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.common.validation.CodePointSize;
import com.dataspec.reviewfinding.model.ReviewFinding;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI evidence package 生成请求。
 *
 * @param projectId 可选项目 ID；持久化来源会优先使用来源记录中的项目。
 * @param sourceType 证据来源类型。
 * @param sourceId 持久化来源 ID；payload 型来源可为空。
 * @param sourceTitle payload 型来源的人读标题。
 * @param coverageReport 即时覆盖率报告 payload，仅 COVERAGE_REPORT 使用。
 * @param standardSnapshot 标准快照摘要；payload 型来源未提供时视为未版本化。
 * @param payloadSummary 输入 payload 的脱敏摘要。
 * @param postCheckSummary AI 输出后置校验摘要；只允许携带状态、issue 计数、引用摘要和建议命令，不存 raw AI output。
 * @param postCheckReceipt post-check PASS 时签发的进程内 receipt；外部 findings 非空时必需，不绑定 package 来源、
 *                         同一进程内可为同项目和同 findings 重复使用，且不会写入 evidence package。
 * @param findings 可选已通过 AI output post-check 的外部 findings；最多 100 条，打包前再次验证 evidence refs。
 */
@Schema(description = "AI Evidence Package 生成请求；不接收 raw AI output 或可复用凭据。")
public record AiEvidencePackageReq(
        @Schema(description = "当前项目 ID；持久化来源以来源记录的项目 ID 为准。")
        Long projectId,
        @NotNull(message = "sourceType 不能为空")
        @Schema(description = "证据来源类型；决定 sourceId 或 payload 的解析方式。")
        EvidenceSourceType sourceType,
        @Schema(description = "持久化来源 ID；payload-only COVERAGE_REPORT 可为空。")
        Long sourceId,
        @Schema(description = "payload-only 来源的人读标题；输出前脱敏。")
        String sourceTitle,
        @Schema(description = "即时字段覆盖率报告；仅 COVERAGE_REPORT 使用。")
        FieldCoverageReport coverageReport,
        @Schema(description = "标准快照摘要；payload-only 来源未提供时标记为未版本化。")
        AiEvidenceStandardSnapshot standardSnapshot,
        @Schema(description = "输入 payload 的有界脱敏摘要；不得包含 raw secret。")
        Map<String, Object> payloadSummary,
        @Schema(description = "AI output post-check 摘要；外部 findings 要求 status=PASS 且 safeToUse=true。")
        Map<String, Object> postCheckSummary,
        @CodePointSize(max = 4096, message = "postCheckReceipt 不能超过 4096 个 Unicode code point")
        @Schema(description = "post-check PASS 时签发的进程内 HMAC receipt；外部 findings 非空时必须匹配当前项目和完整规范化 findings，不绑定 package 来源，同一进程内可为相同输入重复使用，服务重启后失效。",
                maxLength = 4096, nullable = true)
        String postCheckReceipt,
        @Valid
        @Size(max = 100, message = "findings 不能超过 100 条")
        @ArraySchema(
                maxItems = 100,
                arraySchema = @Schema(description = "已通过 post-check 的外部 findings；打包前再次验证 evidenceRefs。"),
                schema = @Schema(implementation = ReviewFinding.class))
        List<@NotNull(message = "findings 元素不能为空") @Valid ReviewFinding> findings
) {
    public AiEvidencePackageReq {
        findings = findings == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(findings));
    }

    /** 兼容已有 post-check summary 调用方。 */
    public AiEvidencePackageReq(Long projectId,
                                EvidenceSourceType sourceType,
                                Long sourceId,
                                String sourceTitle,
                                FieldCoverageReport coverageReport,
                                AiEvidenceStandardSnapshot standardSnapshot,
                                Map<String, Object> payloadSummary,
                                Map<String, Object> postCheckSummary) {
        this(projectId, sourceType, sourceId, sourceTitle, coverageReport, standardSnapshot,
                payloadSummary, postCheckSummary, null, List.of());
    }

    /** 兼容已提交 findings、但尚未携带 receipt 的调用方；服务端会在 findings 非空时拒绝。 */
    public AiEvidencePackageReq(Long projectId,
                                EvidenceSourceType sourceType,
                                Long sourceId,
                                String sourceTitle,
                                FieldCoverageReport coverageReport,
                                AiEvidenceStandardSnapshot standardSnapshot,
                                Map<String, Object> payloadSummary,
                                Map<String, Object> postCheckSummary,
                                List<ReviewFinding> findings) {
        this(projectId, sourceType, sourceId, sourceTitle, coverageReport, standardSnapshot,
                payloadSummary, postCheckSummary, null, findings);
    }

    public AiEvidencePackageReq(Long projectId,
                                EvidenceSourceType sourceType,
                                Long sourceId,
                                String sourceTitle,
                                FieldCoverageReport coverageReport,
                                AiEvidenceStandardSnapshot standardSnapshot,
                                Map<String, Object> payloadSummary) {
        this(projectId, sourceType, sourceId, sourceTitle, coverageReport, standardSnapshot,
                payloadSummary, null, null, List.of());
    }
}
