package com.dataspec.aireplay.model;

/**
 * 创建 AI 作业回放记录的内部请求。
 */
public record AiJobRecordCreateReq(
        Long projectId,
        String jobType,
        String title,
        String inputSummary,
        String promptVersion,
        String status,
        Object inputPayload,
        Object outputPayload,
        Long standardSnapshotId,
        String standardSnapshotVersion,
        String standardSnapshotHash,
        Long sqlCheckRecordId
) {
}
