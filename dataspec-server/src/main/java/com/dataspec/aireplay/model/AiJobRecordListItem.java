package com.dataspec.aireplay.model;

import com.dataspec.aireplay.entity.AiJobRecord;

import java.time.LocalDateTime;

/**
 * AI 作业列表项，只暴露检索和展示所需元数据，避免列表接口返回大段输入输出 payload。
 */
public record AiJobRecordListItem(
        Long id,
        Long projectId,
        String jobType,
        String title,
        String inputSummary,
        String promptVersion,
        String status,
        Long sqlCheckRecordId,
        Long standardSnapshotId,
        String standardSnapshotVersion,
        String standardSnapshotHash,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AiJobRecordListItem from(AiJobRecord record) {
        return new AiJobRecordListItem(
                record.getId(),
                record.getProjectId(),
                record.getJobType(),
                record.getTitle(),
                record.getInputSummary(),
                record.getPromptVersion(),
                record.getStatus(),
                record.getSqlCheckRecordId(),
                record.getStandardSnapshotId(),
                record.getStandardSnapshotVersion(),
                record.getStandardSnapshotHash(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
