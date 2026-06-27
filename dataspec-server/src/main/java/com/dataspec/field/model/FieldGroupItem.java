package com.dataspec.field.model;

import java.util.List;

/**
 * 字段分组摘要项。
 */
public record FieldGroupItem(
        String groupType,
        String groupKey,
        String groupName,
        int fieldCount,
        List<String> sampleFields,
        boolean ungrouped
) {
}
