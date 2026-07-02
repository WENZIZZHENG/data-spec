package com.dataspec.standardhealth.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集健康快照时可选传入的最近一次覆盖率摘要。
 */
@Data
public class StandardHealthCoverageInput {

    private Double coverageRate;

    private Integer unmanagedFieldCount;

    private Integer missingCommentCount;

    private Integer possibleDuplicateCount;

    private List<String> topUnmanagedFields = new ArrayList<>();
}
