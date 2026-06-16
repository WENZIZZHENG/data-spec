package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表定义（从 SQL 解析而来）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDef {
    private String name;
    private String comment;
    private List<ColumnDef> columns;
}
