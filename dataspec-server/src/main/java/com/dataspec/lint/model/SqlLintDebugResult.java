package com.dataspec.lint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SQL 规则调试接口响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlLintDebugResult {

    /** 调试响应版本，用于 AI 和 CLI 判断字段兼容性。 */
    private String debugVersion;

    /** 常规 lint 结果快照；字段与 /api/lint 保持兼容。 */
    private LintResult lintResult;

    /** 每条已知 lint 规则的执行或跳过 trace。 */
    private List<SqlRuleDebugTrace> rules;

    /** 本次调试的全局说明，例如只读、不保存记录或 SQL 未解析。 */
    private List<String> debugNotes;
}
