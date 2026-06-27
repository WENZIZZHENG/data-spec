package com.dataspec.coverage.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 未纳管字段聚合排行，帮助用户优先补齐高频标准字段。
 */
@Data
public class UnmanagedFieldRanking {

    private String columnName;
    private int count;
    private List<String> tables = new ArrayList<>();
    private String recommendedFieldName;
    private String reason;
}
