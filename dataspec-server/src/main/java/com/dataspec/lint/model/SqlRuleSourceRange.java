package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL 规则调试中指向源 SQL 文本的范围。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlRuleSourceRange {

    /** 1-based 起始行号；无法定位时为空。 */
    private Integer line;

    /** 1-based 起始列号；无法定位时为空。 */
    private Integer column;

    /** 1-based 结束行号；无法定位时为空。 */
    private Integer lineEnd;

    /** 1-based 结束列号(不含)；无法定位时为空。 */
    private Integer columnEnd;

    /** 0-based 起始偏移；无法定位时为空。 */
    private Integer sourceStart;

    /** 0-based 结束偏移(不含)；无法定位时为空。 */
    private Integer sourceEnd;

    /** 定位类型，例如 table、column、comment_column；无法定位时为空。 */
    private String locationKind;

    /** source range 对应的表名；无法定位到具体表时为空。 */
    private String tableName;

    /** source range 对应的字段名；表级问题或无法定位时为空。 */
    private String columnName;
}
