package com.dataspec.generator.service;

import com.dataspec.common.exception.BizException;
import com.dataspec.generator.model.DdlGenerateResult;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * PostgreSQL DDL 生成器。
 */
@Service
@RequiredArgsConstructor
public class DdlGeneratorService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern DATA_TYPE_PATTERN = Pattern.compile(
            "[a-zA-Z][a-zA-Z0-9_ ]*(\\([0-9]+(\\s*,\\s*[0-9]+)?\\))?(\\[\\])?");
    private static final Pattern DISALLOWED_DATA_TYPE_KEYWORDS = Pattern.compile(
            "(?i).*(\\bnot\\b|\\bnull\\b|\\bdefault\\b|\\bconstraint\\b|\\bprimary\\b|\\bunique\\b|\\bcheck\\b|\\breferences\\b).*");
    private static final Pattern DEFAULT_VALUE_PATTERN = Pattern.compile(
            "(?i)(null|true|false|current_timestamp|current_date|current_time|[+-]?\\d+(\\.\\d+)?|'([^']|'')*'|[a-z_][a-z0-9_]*\\(\\))");

    private final TemplateService templateService;
    private final SqlLintService sqlLintService;
    private final StandardSnapshotService standardSnapshotService;

    /**
     * 基于表模板生成 PostgreSQL CREATE TABLE DDL，并使用现有 lint 入口做自检。
     */
    public DdlGenerateResult generateFromTemplate(Long projectId, Long templateId, String tableName) {
        if (projectId == null) {
            throw new BizException("项目 ID 不能为空");
        }
        if (templateId == null) {
            throw new BizException("模板 ID 不能为空");
        }
        String normalizedTableName = normalizeIdentifier(tableName, "表名");

        Template template = templateService.getById(templateId);
        if (!Objects.equals(projectId, template.getProjectId())) {
            throw new BizException("模板不属于当前项目");
        }

        List<TemplateField> fields = templateService.listFields(templateId).stream()
                .sorted(Comparator
                        .comparing(TemplateField::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TemplateField::getId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(TemplateField::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
        if (fields.isEmpty()) {
            throw new BizException("模板字段不能为空");
        }

        String ddl = buildDdl(template, fields, normalizedTableName);
        LintResult lintResult = sqlLintService.lint(ddl, projectId);
        return new DdlGenerateResult(ddl, lintResult, standardSnapshotService.getCurrentSnapshot(projectId));
    }

    private String buildDdl(Template template, List<TemplateField> fields, String tableName) {
        List<DdlColumn> columns = fields.stream()
                .map(this::toDdlColumn)
                .toList();
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(tableName).append(" (\n");
        for (int i = 0; i < columns.size(); i++) {
            DdlColumn column = columns.get(i);
            ddl.append("    ").append(column.name()).append(" ").append(column.dataType());
            if (column.notNull()) {
                ddl.append(" NOT NULL");
            }
            if (hasText(column.defaultValue())) {
                ddl.append(" DEFAULT ").append(column.defaultValue());
            }
            if (i < columns.size() - 1) {
                ddl.append(",");
            }
            ddl.append("\n");
        }
        ddl.append(");\n");

        String tableComment = firstText(template.getDescription(), template.getName());
        if (hasText(tableComment)) {
            ddl.append("\n")
                    .append("COMMENT ON TABLE ").append(tableName)
                    .append(" IS '").append(escapeSqlLiteral(tableComment)).append("';\n");
        }
        for (DdlColumn column : columns) {
            if (hasText(column.comment())) {
                ddl.append("COMMENT ON COLUMN ").append(tableName).append(".").append(column.name())
                        .append(" IS '").append(escapeSqlLiteral(column.comment())).append("';\n");
            }
        }
        return ddl.toString();
    }

    private DdlColumn toDdlColumn(TemplateField field) {
        String defaultValue = hasText(field.getDefaultValue())
                ? normalizeDefaultValue(field.getDefaultValue())
                : null;
        return new DdlColumn(
                normalizeIdentifier(field.getName(), "字段名"),
                normalizeDataType(field.getDataType()),
                Boolean.FALSE.equals(field.getNullable()) || Boolean.TRUE.equals(field.getIsRequired()),
                defaultValue,
                hasText(field.getComment()) ? field.getComment().trim() : ""
        );
    }

    private String normalizeIdentifier(String identifier, String label) {
        // 生成器不自动 quote 标识符，统一限制为 snake_case，避免生成可执行的危险 SQL 片段。
        String normalized = hasText(identifier) ? identifier.trim() : "";
        if (!IDENTIFIER_PATTERN.matcher(normalized).matches()) {
            throw new BizException(label + "必须是 snake_case 标识符");
        }
        return normalized;
    }

    private String normalizeDataType(String dataType) {
        String normalized = hasText(dataType) ? dataType.trim() : "";
        if (!hasText(normalized)) {
            throw new BizException("字段类型不能为空");
        }
        // 类型片段必须保持为“类型”本身，不能携带约束或额外列定义。
        if (!DATA_TYPE_PATTERN.matcher(normalized).matches()
                || DISALLOWED_DATA_TYPE_KEYWORDS.matcher(normalized).matches()) {
            throw new BizException("字段类型包含不支持的 SQL 片段");
        }
        return normalized;
    }

    private String normalizeDefaultValue(String defaultValue) {
        String normalized = hasText(defaultValue) ? defaultValue.trim() : "";
        if (!hasText(normalized)) {
            throw new BizException("字段默认值不能为空");
        }
        // 默认值第一版只允许常见字面量或零参函数，避免把逗号、约束或语句片段拼进 DDL。
        if (!DEFAULT_VALUE_PATTERN.matcher(normalized).matches()) {
            throw new BizException("字段默认值包含不支持的 SQL 片段");
        }
        return normalized;
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private String firstText(String primary, String fallback) {
        if (hasText(primary)) {
            return primary.trim();
        }
        return hasText(fallback) ? fallback.trim() : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record DdlColumn(
            String name,
            String dataType,
            boolean notNull,
            String defaultValue,
            String comment
    ) {
    }
}
