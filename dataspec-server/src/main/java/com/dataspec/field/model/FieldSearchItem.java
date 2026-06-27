package com.dataspec.field.model;

import com.dataspec.field.entity.Field;

import java.util.List;

/**
 * 单个字段标准检索命中项。
 */
public record FieldSearchItem(
        Field field,
        int score,
        List<String> matchReasons,
        String recommendedUse,
        List<String> nextActions
) {
}
