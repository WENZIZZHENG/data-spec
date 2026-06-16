package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字段定义（从 SQL 解析而来）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDef {
    private String name;
    private String dataType;
    private boolean nullable;
    private String defaultValue;
    private String comment;
}
