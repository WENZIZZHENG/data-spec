package com.dataspec.standardhealth.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 可复制的标准健康改进计划。
 */
@Data
public class StandardHealthPlan {

    private Long projectId;

    private Long snapshotId;

    private String markdown;

    private List<StandardHealthAction> topActions = new ArrayList<>();

    private List<String> nextActions = new ArrayList<>();
}
