package com.dataspec.standardhealth.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 标准健康快照前端/AI 视图。
 */
@Data
public class StandardHealthSnapshotView {

    private Long id;

    private Long projectId;

    private LocalDateTime capturedAt;

    private String source;

    private StandardHealthMetrics metrics = new StandardHealthMetrics();

    private List<StandardHealthAction> topActions = new ArrayList<>();

    private String planMarkdown;
}
