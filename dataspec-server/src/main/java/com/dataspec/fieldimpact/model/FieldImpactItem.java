package com.dataspec.fieldimpact.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单条字段影响来源。
 */
@Data
public class FieldImpactItem {

    private FieldImpactType impactType;
    private FieldImpactSeverity severity;
    private Long sourceId;
    private String sourceName;
    private int count;
    private boolean possibleReference;
    private String description;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
