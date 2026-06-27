package com.dataspec.field.model;

import java.util.List;

/**
 * 项目内字段分组摘要。
 */
public record FieldGroupSummary(
        Long projectId,
        int totalFieldCount,
        int ungroupedFieldCount,
        List<FieldGroupItem> groups
) {
}
