package com.dataspec.syntheticexample.model;

import java.util.List;
import java.util.Map;

/**
 * 合成 SQL 样例，既可表示推荐用法，也可表示带预期诊断的反例。
 *
 * @param id 稳定 case id。
 * @param title 人读标题。
 * @param scenario 业务场景，如 user、order、payment、audit。
 * @param sql 脱敏后的 SQL/DDL 文本。
 * @param usedFieldNames 样例引用的标准字段名。
 * @param expectedDiagnosticIds bad SQL 反例预期命中的诊断 id；good SQL 为空。
 * @param description 面向 AI 的样例说明。
 * @param source 来源摘要，说明字段来自项目标准还是内置 fallback。
 */
public record SyntheticSqlCase(
        String id,
        String title,
        String scenario,
        String sql,
        List<String> usedFieldNames,
        List<String> expectedDiagnosticIds,
        String description,
        Map<String, Object> source
) {
}
