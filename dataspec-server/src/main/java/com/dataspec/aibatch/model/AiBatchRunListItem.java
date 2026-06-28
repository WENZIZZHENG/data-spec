package com.dataspec.aibatch.model;

import com.dataspec.aibatch.entity.AiBatchRun;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * 批量任务列表项，只暴露摘要，避免列表接口返回完整 payload。
 */
public record AiBatchRunListItem(
        Long id,
        Long projectId,
        String batchType,
        String source,
        String status,
        AiBatchSummary summary,
        String operatorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    public static AiBatchRunListItem from(AiBatchRun run) {
        return new AiBatchRunListItem(
                run.getId(),
                run.getProjectId(),
                run.getBatchType(),
                run.getSource(),
                run.getStatus(),
                parseSummary(run.getSummaryJson()),
                run.getOperatorName(),
                run.getCreatedAt(),
                run.getUpdatedAt()
        );
    }

    private static AiBatchSummary parseSummary(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) {
            return AiBatchSummary.empty();
        }
        try {
            return OBJECT_MAPPER.readValue(summaryJson, AiBatchSummary.class);
        } catch (Exception ignored) {
            return AiBatchSummary.empty();
        }
    }
}
