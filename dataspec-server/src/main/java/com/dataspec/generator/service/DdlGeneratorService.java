package com.dataspec.generator.service;

import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.dialect.model.DialectDiagnostic;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import com.dataspec.generator.model.DdlGenerateResult;
import com.dataspec.generator.model.DdlStructureSummary;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.tablemodel.model.TableAuditPolicy;
import com.dataspec.tablemodel.model.TableForeignKeyStandard;
import com.dataspec.tablemodel.model.TableIndexStandard;
import com.dataspec.tablemodel.model.TablePrimaryKeyStandard;
import com.dataspec.tablemodel.model.TableSoftDeletePolicy;
import com.dataspec.tablemodel.model.TableUniqueKeyStandard;
import com.dataspec.tablemodel.service.TableStructureJsonCodec;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PostgreSQL DDL 生成器。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DdlGeneratorService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern DATA_TYPE_PATTERN = Pattern.compile(
            "[a-zA-Z][a-zA-Z0-9_ ]*(\\([0-9]+(\\s*,\\s*[0-9]+)?\\))?(\\[\\])?");
    private static final Pattern DISALLOWED_DATA_TYPE_KEYWORDS = Pattern.compile(
            "(?i).*(\\bnot\\b|\\bnull\\b|\\bdefault\\b|\\bconstraint\\b|\\bprimary\\b|\\bunique\\b|\\bcheck\\b|\\breferences\\b).*");
    private static final Pattern DEFAULT_VALUE_PATTERN = Pattern.compile(
            "(?i)(null|true|false|current_timestamp|current_date|current_time|[+-]?\\d+(\\.\\d+)?|'([^']|'')*'|[a-z_][a-z0-9_]*\\(\\))");
    private static final Set<String> FK_ACTIONS = Set.of("CASCADE", "RESTRICT", "SET NULL", "NO ACTION");
    private static final ObjectMapper STRUCTURE_MAPPER = new ObjectMapper();

    private final TemplateService templateService;
    private final SqlLintService sqlLintService;
    private final StandardSnapshotService standardSnapshotService;
    private final AiJobRecordService aiJobRecordService;
    private final PromptTemplateRegistry promptTemplateRegistry;
    private final SqlDialectCompatibilityService dialectCompatibilityService = new SqlDialectCompatibilityService();

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

        DdlRenderResult renderResult = buildDdl(template, fields, normalizedTableName);
        String ddl = renderResult.ddl();
        LintResult lintResult = sqlLintService.lint(ddl, projectId);
        StandardSnapshotInfo snapshot = standardSnapshotService.getCurrentSnapshot(projectId);
        List<DialectDiagnostic> dialectDiagnostics = dialectCompatibilityService.diagnoseGeneratedPostgresqlDdl();
        recordDdlPreview(projectId, templateId, normalizedTableName, ddl, lintResult, snapshot, dialectDiagnostics,
                renderResult.structureSummary());
        return new DdlGenerateResult(ddl, lintResult, snapshot, dialectDiagnostics, renderResult.structureSummary());
    }

    private void recordDdlPreview(
            Long projectId,
            Long templateId,
            String tableName,
            String ddl,
            LintResult lintResult,
            StandardSnapshotInfo snapshot,
            List<DialectDiagnostic> dialectDiagnostics,
            DdlStructureSummary structureSummary
    ) {
        try {
            aiJobRecordService.create(new AiJobRecordCreateReq(
                    projectId,
                    "DDL_PREVIEW",
                    "DDL 预览",
                    tableName,
                    promptTemplateRegistry.promptVersion(PromptTemplateRegistry.DDL_PREVIEW),
                    "SUCCESS",
                    SensitiveDataSanitizer.sanitizeValue(orderedMap(
                            "templateId", templateId,
                            "tableName", tableName
                    )),
                    SensitiveDataSanitizer.sanitizeValue(orderedMap(
                            "ddl", ddl,
                            "lintSummary", orderedMap(
                                    "errorCount", lintResult.getErrorCount(),
                                    "warningCount", lintResult.getWarningCount(),
                                    "suggestionCount", lintResult.getSuggestionCount()
                            ),
                            "dialectDiagnostics", dialectDiagnostics
                            ,
                            "structureSummary", structureSummary
                    )),
                    snapshot == null ? null : snapshot.snapshotId(),
                    snapshot == null ? null : snapshot.specVersion(),
                    snapshot == null ? null : snapshot.specHash(),
                    null
            ));
        } catch (Exception e) {
            log.warn("保存 AI DDL 回放记录失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private DdlRenderResult buildDdl(Template template, List<TemplateField> fields, String tableName) {
        List<DdlColumn> columns = fields.stream()
                .map(this::toDdlColumn)
                .toList();
        Set<String> columnNames = columns.stream()
                .map(DdlColumn::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        StructureRender structure = renderStructure(template, tableName, columnNames);
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(tableName).append(" (\n");
        List<String> tableLines = new ArrayList<>();
        for (DdlColumn column : columns) {
            StringBuilder line = new StringBuilder();
            line.append("    ").append(column.name()).append(" ").append(column.dataType());
            if (column.notNull()) {
                line.append(" NOT NULL");
            }
            if (hasText(column.defaultValue())) {
                line.append(" DEFAULT ").append(column.defaultValue());
            }
            tableLines.add(line.toString());
        }
        structure.tableConstraints().forEach(item -> tableLines.add("    " + item));
        for (int i = 0; i < tableLines.size(); i++) {
            ddl.append(tableLines.get(i));
            if (i < tableLines.size() - 1) {
                ddl.append(",");
            }
            ddl.append("\n");
        }
        ddl.append(");\n");
        for (String indexStatement : structure.indexStatements()) {
            ddl.append(indexStatement).append("\n");
        }
        if (!structure.indexStatements().isEmpty()) {
            ddl.append("\n");
        }

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
        return new DdlRenderResult(ddl.toString(), structure.structureSummary());
    }

    private StructureRender renderStructure(Template template, String tableName, Set<String> columnNames) {
        TableStructureJsonCodec codec = new TableStructureJsonCodec(STRUCTURE_MAPPER);
        List<String> tableConstraints = new ArrayList<>();
        List<String> indexStatements = new ArrayList<>();
        List<String> applied = new ArrayList<>();
        List<String> generatedIndexes = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> policyNotes = new ArrayList<>();
        List<String> evidence = new ArrayList<>();

        TablePrimaryKeyStandard primaryKey = codec.read(template.getPrimaryKeyJson(),
                TablePrimaryKeyStandard.class, null, "主键标准");
        if (primaryKey != null) {
            List<String> columns = safeColumns(primaryKey.columns(), columnNames, skipped, "主键标准");
            if (!columns.isEmpty()) {
                String constraintName = normalizeOrDefault(primaryKey.name(), tableName + "_pk", "主键约束名", skipped);
                tableConstraints.add("CONSTRAINT " + constraintName + " PRIMARY KEY (" + String.join(", ", columns) + ")");
                applied.add("primaryKey:" + constraintName + "(" + String.join(",", columns) + ")");
            }
        }

        for (TableUniqueKeyStandard uniqueKey : codec.readList(template.getUniqueKeysJson(),
                TableUniqueKeyStandard.class, "唯一键标准")) {
            List<String> columns = safeColumns(uniqueKey.columns(), columnNames, skipped, "唯一键标准");
            if (columns.isEmpty()) {
                continue;
            }
            String suffix = String.join("_", columns);
            String constraintName = normalizeOrDefault(uniqueKey.name(), tableName + "_" + suffix + "_uk",
                    "唯一键约束名", skipped);
            tableConstraints.add("CONSTRAINT " + constraintName + " UNIQUE (" + String.join(", ", columns) + ")");
            applied.add("uniqueKey:" + constraintName + "(" + String.join(",", columns) + ")");
        }

        for (TableForeignKeyStandard foreignKey : codec.readList(template.getForeignKeysJson(),
                TableForeignKeyStandard.class, "外键标准")) {
            if (Boolean.TRUE.equals(foreignKey.advisoryOnly())) {
                skipped.add(safeNote("外键提示仅 advisory: " + firstText(foreignKey.relationName(), foreignKey.name())));
                continue;
            }
            List<String> columns = safeColumns(foreignKey.columns(), columnNames, skipped, "外键标准");
            List<String> targetColumns = safeIdentifierList(foreignKey.targetColumns(), skipped, "外键目标列");
            String targetTable = safeIdentifier(foreignKey.targetTable(), skipped, "外键目标表名");
            if (columns.isEmpty() || targetColumns.isEmpty() || !hasText(targetTable)) {
                skipped.add("外键提示缺少可安全生成的本表列、目标表或目标列");
                continue;
            }
            String suffix = String.join("_", columns);
            String constraintName = normalizeOrDefault(foreignKey.name(), tableName + "_" + suffix + "_fk",
                    "外键约束名", skipped);
            StringBuilder line = new StringBuilder();
            line.append("CONSTRAINT ").append(constraintName)
                    .append(" FOREIGN KEY (").append(String.join(", ", columns)).append(")")
                    .append(" REFERENCES ").append(targetTable)
                    .append(" (").append(String.join(", ", targetColumns)).append(")");
            appendForeignKeyAction(line, " ON DELETE ", foreignKey.onDelete(), skipped);
            appendForeignKeyAction(line, " ON UPDATE ", foreignKey.onUpdate(), skipped);
            tableConstraints.add(line.toString());
            applied.add("foreignKey:" + constraintName + "(" + String.join(",", columns) + "->" + targetTable + ")");
        }

        for (TableIndexStandard index : codec.readList(template.getIndexesJson(), TableIndexStandard.class, "索引标准")) {
            List<String> columns = safeColumns(index.columns(), columnNames, skipped, "索引标准");
            if (columns.isEmpty()) {
                continue;
            }
            if (hasText(index.method()) && !"btree".equalsIgnoreCase(index.method().trim())) {
                skipped.add(safeNote("索引方法暂不支持: " + index.method()));
                continue;
            }
            String suffix = String.join("_", columns);
            String indexName = normalizeOrDefault(index.name(), "idx_" + tableName + "_" + suffix, "索引名", skipped);
            String statement = "CREATE " + (Boolean.TRUE.equals(index.unique()) ? "UNIQUE " : "")
                    + "INDEX " + indexName + " ON " + tableName + " (" + String.join(", ", columns) + ");";
            indexStatements.add(statement);
            generatedIndexes.add("index:" + indexName + "(" + String.join(",", columns) + ")");
        }

        addPolicyNotes(codec, template, policyNotes);
        if (template.getBusinessObjectId() != null) {
            evidence.add("businessObjectId:" + template.getBusinessObjectId());
        }
        if (!applied.isEmpty() || !generatedIndexes.isEmpty() || !policyNotes.isEmpty()) {
            evidence.add("template:" + template.getId());
        }
        return new StructureRender(tableConstraints, indexStatements,
                new DdlStructureSummary(List.copyOf(applied), List.copyOf(generatedIndexes),
                        List.copyOf(skipped), List.copyOf(policyNotes), List.copyOf(evidence)));
    }

    private void addPolicyNotes(TableStructureJsonCodec codec, Template template, List<String> policyNotes) {
        for (String hint : codec.readList(template.getCheckHintsJson(), String.class, "CHECK 提示")) {
            policyNotes.add(safeNote("checkHint:" + hint));
        }
        TableAuditPolicy auditPolicy = codec.read(template.getAuditPolicyJson(), TableAuditPolicy.class, null, "审计策略");
        if (auditPolicy != null) {
            policyNotes.add(safeNote("auditPolicy:" + firstText(auditPolicy.notes(), String.join(",", nullToEmpty(auditPolicy.requiredFields())))));
        }
        TableSoftDeletePolicy softDeletePolicy = codec.read(
                template.getSoftDeletePolicyJson(), TableSoftDeletePolicy.class, null, "软删除策略");
        if (softDeletePolicy != null) {
            policyNotes.add(safeNote("softDeletePolicy:" + firstText(softDeletePolicy.fieldName(), softDeletePolicy.notes())));
        }
        for (String note : codec.readList(template.getDialectNotesJson(), String.class, "方言说明")) {
            policyNotes.add(safeNote("dialectNote:" + note));
        }
        if (hasText(template.getAiUsageNotes())) {
            policyNotes.add(safeNote("aiUsageNotes:" + template.getAiUsageNotes()));
        }
    }

    private List<String> safeColumns(List<String> rawColumns, Set<String> allowedColumns, List<String> skipped, String label) {
        List<String> columns = safeIdentifierList(rawColumns, skipped, label);
        List<String> accepted = new ArrayList<>();
        for (String column : columns) {
            if (allowedColumns.contains(column)) {
                accepted.add(column);
            } else {
                skipped.add(safeNote(label + "引用了模板中不存在的字段: " + column));
            }
        }
        return accepted;
    }

    private List<String> safeIdentifierList(List<String> values, List<String> skipped, String label) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String identifier = safeIdentifier(value, skipped, label);
            if (hasText(identifier)) {
                result.add(identifier);
            }
        }
        return result;
    }

    private String safeIdentifier(String value, List<String> skipped, String label) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (!IDENTIFIER_PATTERN.matcher(normalized).matches()) {
            skipped.add(safeNote(label + "不是安全 snake_case 标识符: " + value));
            return null;
        }
        return normalized;
    }

    private String normalizeOrDefault(String value, String fallback, String label, List<String> skipped) {
        String candidate = hasText(value) ? value.trim() : fallback;
        if (!IDENTIFIER_PATTERN.matcher(candidate).matches()) {
            skipped.add(safeNote(label + "不安全，已使用默认名称: " + fallback));
            return fallback;
        }
        return candidate;
    }

    private void appendForeignKeyAction(StringBuilder line, String prefix, String action, List<String> skipped) {
        if (!hasText(action)) {
            return;
        }
        String normalized = action.trim().toUpperCase(java.util.Locale.ROOT);
        if (FK_ACTIONS.contains(normalized)) {
            line.append(prefix).append(normalized);
        } else {
            skipped.add(safeNote("外键动作暂不支持: " + action));
        }
    }

    private String safeNote(String value) {
        return SensitiveDataSanitizer.redactText(value);
    }

    private List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
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

    private record DdlRenderResult(
            String ddl,
            DdlStructureSummary structureSummary
    ) {
    }

    private record StructureRender(
            List<String> tableConstraints,
            List<String> indexStatements,
            DdlStructureSummary structureSummary
    ) {
    }
}
