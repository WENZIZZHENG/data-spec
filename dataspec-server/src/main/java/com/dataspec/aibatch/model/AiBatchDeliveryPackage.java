package com.dataspec.aibatch.model;

import com.dataspec.aitaskrun.model.AiTaskResumeInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 批量任务交付包稳定契约。
 */
public record AiBatchDeliveryPackage(
        String packageVersion,
        String batchId,
        Long projectId,
        String batchType,
        String source,
        String status,
        AiBatchSummary summary,
        List<AiBatchItemResult> items,
        AiBatchIssueSummary issueSummary,
        AiBatchFixedSqlSummary fixedSqlSummary,
        List<String> unmanagedHints,
        List<AiBatchEvidence> evidence,
        List<String> nextActions,
        LocalDateTime createdAt,
        AiTaskResumeInfo taskRun
) {
}
