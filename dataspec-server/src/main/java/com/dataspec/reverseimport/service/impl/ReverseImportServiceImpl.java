package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.perf.PerformanceProbe;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.DatabaseImportResult;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.MissingCommentIssue;
import com.dataspec.reverseimport.model.NonStandardField;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportCompareSummary;
import com.dataspec.reverseimport.model.ReverseImportFieldChange;
import com.dataspec.reverseimport.model.ReverseImportFieldDiff;
import com.dataspec.reverseimport.model.ReverseImportFieldStatus;
import com.dataspec.reverseimport.model.ReverseImportTableDiff;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.model.ReverseImportSummary;
import com.dataspec.reverseimport.service.ReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL 反向导入预览服务实现。
 */
@Service
@RequiredArgsConstructor
public class ReverseImportServiceImpl implements ReverseImportService {

    private static final long REVERSE_COMPARE_WARN_MS = 1_000;

    private final SqlParserService sqlParserService;
    private final FieldService fieldService;
    private final ReverseImportSourceService reverseImportSourceService;
    private final SqlDialectCompatibilityService dialectCompatibilityService = new SqlDialectCompatibilityService();

    @Override
    public ReverseImportPreview preview(Long projectId, String sql) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (sql == null || sql.isBlank()) {
            throw new BizException("SQL 不能为空");
        }

        List<TableDef> tables = sqlParserService.parse(sql);
        ReverseImportPreview preview = previewTables(projectId, tables);
        preview.setDialectDiagnostics(dialectCompatibilityService.diagnoseSql(sql));
        return preview;
    }

    @Override
    public ReverseImportPreview previewTables(Long projectId, List<TableDef> tables) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (tables == null || tables.isEmpty()) {
            throw new BizException("未读取到可导入的表结构");
        }
        Map<String, Field> standardFieldIndex = standardFieldIndex(projectId);

        ReverseImportPreview preview = new ReverseImportPreview();
        preview.setTables(tables);
        int columnCount = 0;

        for (TableDef table : tables) {
            if (isBlank(table.getComment())) {
                preview.getMissingComments().add(new MissingCommentIssue(table.getName(), null, "table"));
            }
            for (ColumnDef column : table.getColumns()) {
                columnCount++;
                if (isBlank(column.getComment())) {
                    preview.getMissingComments().add(new MissingCommentIssue(table.getName(), column.getName(), "column"));
                }
                if (!standardFieldIndex.containsKey(normalize(column.getName()))) {
                    preview.getFieldCandidates().add(new FieldCandidate(
                            table.getName(),
                            column.getName(),
                            column.getDataType(),
                            column.isNullable(),
                            column.getDefaultValue(),
                            column.getComment()));
                    preview.getNonStandardFields().add(new NonStandardField(
                            table.getName(),
                            column.getName(),
                            column.getDataType(),
                            column.getName(),
                            "未命中标准字段名或别名"));
                }
            }
        }

        ReverseImportSummary summary = new ReverseImportSummary();
        summary.setTableCount(tables.size());
        summary.setColumnCount(columnCount);
        summary.setCandidateCount(preview.getFieldCandidates().size());
        summary.setMissingCommentCount(preview.getMissingComments().size());
        summary.setNonStandardFieldCount(preview.getNonStandardFields().size());
        preview.setSummary(summary);
        return preview;
    }

    @Override
    public ReverseImportCompareResult compareTables(Long projectId, List<TableDef> tables) {
        return PerformanceProbe.measure("reverse-import.compareTables", REVERSE_COMPARE_WARN_MS,
                "反向导入 compare 变慢时优先减少本次表选择或检查字段库规模",
                () -> compareTablesMeasured(projectId, tables));
    }

    private ReverseImportCompareResult compareTablesMeasured(Long projectId, List<TableDef> tables) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (tables == null || tables.isEmpty()) {
            throw new BizException("未读取到可比对的表结构");
        }

        Map<String, Field> standardFieldIndex = standardFieldIndex(projectId);
        ReverseImportCompareResult result = new ReverseImportCompareResult();
        ReverseImportCompareSummary summary = new ReverseImportCompareSummary();
        int columnCount = 0;
        int matchedCount = 0;
        int changedCount = 0;
        int newCount = 0;
        int missingCommentCount = 0;
        int nonStandardCount = 0;

        for (TableDef table : tables) {
            ReverseImportTableDiff tableDiff = new ReverseImportTableDiff();
            tableDiff.setTableName(table.getName());
            tableDiff.setComment(table.getComment());

            List<ColumnDef> columns = table.getColumns() == null ? List.of() : table.getColumns();
            for (ColumnDef column : columns) {
                columnCount++;
                Field standardField = standardFieldIndex.get(normalize(column.getName()));
                ReverseImportFieldDiff fieldDiff = compareColumn(table, column, standardField);
                tableDiff.getFieldDiffs().add(fieldDiff);

                if (isBlank(column.getComment())) {
                    missingCommentCount++;
                }
                if (standardField == null) {
                    newCount++;
                    nonStandardCount++;
                    continue;
                }
                matchedCount++;
                if (ReverseImportFieldStatus.CHANGED.equals(fieldDiff.getStatus())) {
                    changedCount++;
                }
            }
            result.getTableDiffs().add(tableDiff);
        }

        summary.setTableCount(tables.size());
        summary.setColumnCount(columnCount);
        summary.setMatchedCount(matchedCount);
        summary.setChangedCount(changedCount);
        summary.setNewCount(newCount);
        summary.setMissingCommentCount(missingCommentCount);
        summary.setNonStandardCount(nonStandardCount);
        result.setSummary(summary);
        return result;
    }

    @Override
    @Transactional
    public DatabaseImportResult importCandidates(DatabaseImportReq req) {
        if (req == null || req.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (req.getCandidates() == null || req.getCandidates().isEmpty()) {
            throw new BizException("导入候选不能为空");
        }

        Map<String, Field> standardFieldIndex = standardFieldIndex(req.getProjectId());
        DatabaseImportResult result = new DatabaseImportResult();
        List<ImportedFieldSource> importedSources = new ArrayList<>();
        for (FieldCandidate candidate : req.getCandidates()) {
            if (candidate == null || isBlank(candidate.getColumnName())) {
                continue;
            }
            String normalizedName = normalize(candidate.getColumnName());
            if (standardFieldIndex.containsKey(normalizedName)) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                result.getSkippedFields().add(candidate.getColumnName());
                continue;
            }
            Field field = new Field();
            field.setProjectId(req.getProjectId());
            field.setName(candidate.getColumnName());
            field.setDisplayName(isBlank(candidate.getComment()) ? candidate.getColumnName() : candidate.getComment());
            field.setDataType(candidate.getDataType());
            field.setNullable(candidate.getNullable());
            field.setDefaultValue(candidate.getDefaultValue());
            field.setComment(candidate.getComment());
            field.setCategory(candidate.getTableName());
            fieldService.create(field);
            importedSources.add(new ImportedFieldSource(field, candidate));
            standardFieldIndex.put(normalizedName, field);
            result.setImportedCount(result.getImportedCount() + 1);
            result.getImportedFields().add(candidate.getColumnName());
        }
        if (!importedSources.isEmpty()) {
            var batch = reverseImportSourceService.createDatabaseBatch(
                    req,
                    result.getImportedCount(),
                    result.getSkippedCount());
            for (ImportedFieldSource imported : importedSources) {
                reverseImportSourceService.recordFieldSource(batch, imported.field(), imported.candidate());
            }
        }
        return result;
    }

    private ReverseImportFieldDiff compareColumn(TableDef table, ColumnDef column, Field standardField) {
        ReverseImportFieldDiff diff = new ReverseImportFieldDiff();
        diff.setTableName(table.getName());
        diff.setColumnName(column.getName());
        diff.setDataType(column.getDataType());
        diff.setNullable(column.isNullable());
        diff.setDefaultValue(column.getDefaultValue());
        diff.setComment(column.getComment());

        if (standardField == null) {
            diff.setStatus(ReverseImportFieldStatus.NEW);
            diff.setNonStandard(true);
            diff.setReason("未命中标准字段名或别名，建议确认后导入标准字段库");
            return diff;
        }

        diff.setStandardFieldName(standardField.getName());
        diff.setStandardDisplayName(standardField.getDisplayName());
        collectChanges(column, standardField, diff.getChanges());
        boolean missingComment = isBlank(column.getComment());
        boolean hasNonCommentChange = diff.getChanges().stream()
                .anyMatch(change -> !"comment".equals(change.getProperty()));

        if (hasNonCommentChange || (!missingComment && !diff.getChanges().isEmpty())) {
            diff.setStatus(ReverseImportFieldStatus.CHANGED);
            diff.setReason("命中标准字段，但字段属性与标准不一致");
        } else if (missingComment) {
            diff.setStatus(ReverseImportFieldStatus.MISSING_COMMENT);
            diff.setReason("命中标准字段，但数据库字段缺少注释");
        } else {
            diff.setStatus(ReverseImportFieldStatus.MATCHED);
            diff.setReason("字段名或别名命中标准，属性与标准一致");
        }
        return diff;
    }

    private void collectChanges(ColumnDef column, Field standardField, List<ReverseImportFieldChange> changes) {
        addChangeIfDifferent(changes, "dataType", column.getDataType(), standardDataType(standardField), true);
        if (standardField.getNullable() != null
                && column.isNullable() != standardField.getNullable()) {
            changes.add(new ReverseImportFieldChange(
                    "nullable",
                    Boolean.toString(column.isNullable()),
                    standardField.getNullable().toString()));
        }
        addChangeIfDifferent(changes, "defaultValue", column.getDefaultValue(), standardField.getDefaultValue(), false);
        if (!isBlank(standardField.getComment()) || isBlank(column.getComment())) {
            addChangeIfDifferent(changes, "comment", column.getComment(), standardField.getComment(), false);
        }
    }

    private void addChangeIfDifferent(List<ReverseImportFieldChange> changes,
                                      String property,
                                      String currentValue,
                                      String standardValue,
                                      boolean typeValue) {
        String normalizedCurrent = typeValue ? normalizeType(currentValue) : normalizeComparable(currentValue);
        String normalizedStandard = typeValue ? normalizeType(standardValue) : normalizeComparable(standardValue);
        if (!normalizedCurrent.equals(normalizedStandard)) {
            changes.add(new ReverseImportFieldChange(property, currentValue, standardValue));
        }
    }

    private String standardDataType(Field field) {
        if (isBlank(field.getDataType())) {
            return "";
        }
        String dataType = field.getDataType().trim();
        if (dataType.contains("(")) {
            return dataType;
        }
        if (field.getPrecisionVal() != null && isDecimalType(dataType)) {
            if (field.getScaleVal() != null && field.getScaleVal() > 0) {
                return dataType + "(" + field.getPrecisionVal() + "," + field.getScaleVal() + ")";
            }
            return dataType + "(" + field.getPrecisionVal() + ")";
        }
        if (field.getLength() != null && isSizedType(dataType)) {
            return dataType + "(" + field.getLength() + ")";
        }
        return dataType;
    }

    private boolean isSizedType(String dataType) {
        String normalized = normalizeTypeName(dataType);
        return normalized.contains("char")
                || normalized.contains("text")
                || normalized.contains("numeric")
                || normalized.contains("decimal")
                || normalized.contains("number");
    }

    private boolean isDecimalType(String dataType) {
        String normalized = normalizeTypeName(dataType);
        return normalized.contains("numeric")
                || normalized.contains("decimal")
                || normalized.contains("number");
    }

    private String normalizeType(String value) {
        return normalizeComparable(value)
                .replaceAll("\\s+", "")
                .replace("integer", "int4");
    }

    private String normalizeTypeName(String value) {
        return normalizeComparable(value).replaceAll("\\s+", " ");
    }

    private String normalizeComparable(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Field> standardFieldIndex(Long projectId) {
        Map<String, Field> index = new HashMap<>();
        for (Field field : fieldService.listByProject(projectId)) {
            putIfPresent(index, field.getName(), field);
            for (String alias : splitCsv(field.getAliases())) {
                putIfPresent(index, alias, field);
            }
        }
        return index;
    }

    private void putIfPresent(Map<String, Field> index, String value, Field field) {
        if (!isBlank(value)) {
            index.putIfAbsent(normalize(value), field);
        }
    }

    private List<String> splitCsv(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ImportedFieldSource(Field field, FieldCandidate candidate) {
    }
}
