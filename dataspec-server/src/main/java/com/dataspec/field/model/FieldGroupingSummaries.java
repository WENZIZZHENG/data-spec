package com.dataspec.field.model;

import com.dataspec.field.entity.Field;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 字段分组摘要构建工具。
 */
public final class FieldGroupingSummaries {

    private static final int SAMPLE_LIMIT = 5;

    private FieldGroupingSummaries() {
    }

    public static FieldGroupSummary fromFields(Long projectId, List<Field> fields) {
        Map<String, GroupAccumulator> groups = new LinkedHashMap<>();
        int ungroupedCount = 0;
        for (Field field : fields == null ? List.<Field>of() : fields) {
            boolean grouped = false;
            if (field.getDomainId() != null) {
                add(groups, "domain", String.valueOf(field.getDomainId()), String.valueOf(field.getDomainId()), field, false);
                grouped = true;
            }
            String category = normalizeText(field.getCategory());
            if (category != null) {
                add(groups, "category", category, category, field, false);
                grouped = true;
            }
            for (String tag : splitTags(field.getTags())) {
                add(groups, "tag", tag, tag, field, false);
                grouped = true;
            }
            if (!grouped) {
                ungroupedCount += 1;
                add(groups, "ungrouped", "ungrouped", "未分组", field, true);
            }
        }
        List<FieldGroupItem> items = groups.values().stream()
                .map(GroupAccumulator::toItem)
                .sorted(Comparator
                        .comparing(FieldGroupItem::groupType)
                        .thenComparing(FieldGroupItem::groupKey))
                .toList();
        return new FieldGroupSummary(projectId, fields == null ? 0 : fields.size(), ungroupedCount, items);
    }

    public static List<String> splitTags(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return List.of();
        }
        TreeSet<String> tags = new TreeSet<>();
        for (String part : normalized.split("[,，]")) {
            String tag = normalizeText(part);
            if (tag != null) {
                tags.add(tag);
            }
        }
        return List.copyOf(tags);
    }

    public static String normalizeTags(String value) {
        List<String> tags = splitTags(value);
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void add(Map<String, GroupAccumulator> groups, String type, String key, String name, Field field, boolean ungrouped) {
        String groupKey = type + ":" + key;
        groups.computeIfAbsent(groupKey, ignored -> new GroupAccumulator(type, key, name, ungrouped))
                .add(field);
    }

    private static final class GroupAccumulator {
        private final String type;
        private final String key;
        private final String name;
        private final boolean ungrouped;
        private int count;
        private final List<String> sampleFields = new ArrayList<>();

        private GroupAccumulator(String type, String key, String name, boolean ungrouped) {
            this.type = type;
            this.key = key;
            this.name = name;
            this.ungrouped = ungrouped;
        }

        private void add(Field field) {
            count += 1;
            String fieldName = normalizeText(field.getName());
            if (fieldName != null && sampleFields.size() < SAMPLE_LIMIT) {
                sampleFields.add(fieldName);
            }
        }

        private FieldGroupItem toItem() {
            return new FieldGroupItem(type, key, name, count, List.copyOf(sampleFields), ungrouped);
        }
    }
}
