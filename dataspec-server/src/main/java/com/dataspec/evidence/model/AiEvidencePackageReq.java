package com.dataspec.evidence.model;

import com.dataspec.coverage.model.FieldCoverageReport;
import jakarta.validation.constraints.NotNull;

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
 */
public record AiEvidencePackageReq(
        Long projectId,
        @NotNull(message = "sourceType 不能为空") EvidenceSourceType sourceType,
        Long sourceId,
        String sourceTitle,
        FieldCoverageReport coverageReport,
        AiEvidenceStandardSnapshot standardSnapshot,
        Map<String, Object> payloadSummary,
        Map<String, Object> postCheckSummary
) {
    public AiEvidencePackageReq(Long projectId,
                                EvidenceSourceType sourceType,
                                Long sourceId,
                                String sourceTitle,
                                FieldCoverageReport coverageReport,
                                AiEvidenceStandardSnapshot standardSnapshot,
                                Map<String, Object> payloadSummary) {
        this(projectId, sourceType, sourceId, sourceTitle, coverageReport, standardSnapshot, payloadSummary, null);
    }
}
