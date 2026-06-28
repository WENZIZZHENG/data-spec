package com.dataspec.evidence.model;

import com.dataspec.coverage.model.FieldCoverageReport;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AiEvidencePackageReq(
        Long projectId,
        @NotNull(message = "sourceType 不能为空") EvidenceSourceType sourceType,
        Long sourceId,
        String sourceTitle,
        FieldCoverageReport coverageReport,
        AiEvidenceStandardSnapshot standardSnapshot,
        Map<String, Object> payloadSummary
) {
}
