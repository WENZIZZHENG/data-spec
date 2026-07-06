package com.dataspec.reverseimport.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * metadata cache 字段级结构变化摘要，仅描述 schema 属性差异。
 */
@Data
public class DatabaseMetadataColumnChange {

    /** 发生变化的字段名。 */
    private String columnName;

    /** 字段属性变化列表；currentValue 表示旧缓存值，standardValue 表示刷新后的新值。 */
    private List<ReverseImportFieldChange> changes = new ArrayList<>();
}
