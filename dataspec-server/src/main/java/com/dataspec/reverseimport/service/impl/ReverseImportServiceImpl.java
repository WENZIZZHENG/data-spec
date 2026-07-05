package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.perf.PerformanceProbe;
import com.dataspec.common.safety.DryRunEvidenceSigner;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.dialect.service.SqlDialectCompatibilityService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.idempotency.WriteGuardService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
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
import com.dataspec.security.context.ProjectAccessGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SQL 反向导入预览服务实现。
 */
@Service
@RequiredArgsConstructor
public class ReverseImportServiceImpl implements ReverseImportService {

    private static final long REVERSE_COMPARE_WARN_MS = 1_000;
    private static final String DECISION_EXISTING_MATCH = "EXISTING_MATCH";
    private static final String DECISION_NEW_CANDIDATE = "NEW_CANDIDATE";
    private static final String DECISION_IMPORTED = "IMPORTED";
    private static final String DECISION_SKIPPED_EXISTING = "SKIPPED_EXISTING";
    private static final String DECISION_IGNORED = "IGNORED";

    private final SqlParserService sqlParserService;
    private final FieldService fieldService;
    private final ReverseImportSourceService reverseImportSourceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SqlDialectCompatibilityService dialectCompatibilityService = new SqlDialectCompatibilityService();
    private WriteGuardService writeGuardService = new WriteGuardService();

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
        Map<String, StandardFieldMatch> standardFieldIndex = standardFieldMatchIndex(projectId);

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
                StandardFieldMatch standardMatch = standardFieldIndex.get(normalize(column.getName()));
                if (standardMatch == null) {
                    FieldCandidate candidate = new FieldCandidate(
                            table.getName(),
                            column.getName(),
                            column.getDataType(),
                            column.isNullable(),
                            column.getDefaultValue(),
                            column.getComment());
                    candidate.setDecisionType(DECISION_NEW_CANDIDATE);
                    candidate.setMatchReason("未命中标准字段名或别名，建议确认后导入标准字段库");
                    candidate.setConfidence(0.65);
                    preview.getFieldCandidates().add(candidate);
                    preview.getMappingDecisions().add(decisionFromCandidate(
                            projectId,
                            candidate,
                            DECISION_NEW_CANDIDATE,
                            null,
                            null,
                            candidate.getMatchReason(),
                            candidate.getConfidence(),
                            null,
                            null));
                    preview.getNonStandardFields().add(new NonStandardField(
                            table.getName(),
                            column.getName(),
                            column.getDataType(),
                            column.getName(),
                            "未命中标准字段名或别名"));
                } else {
                    preview.getMappingDecisions().add(existingMatchDecision(projectId, table.getName(), column, standardMatch));
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
        String dryRunToken = reverseImportDryRunToken(projectId, preview);
        preview.setDryRunToken(dryRunToken);
        preview.getFieldCandidates().forEach(candidate -> candidate.setDryRunToken(dryRunToken));
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
        return importCandidates(req, null);
    }

    @Override
    @Transactional
    public DatabaseImportResult importCandidates(DatabaseImportReq req, String idempotencyKey) {
        if (req == null || req.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (req.getCandidates() == null || req.getCandidates().isEmpty()) {
            throw new BizException("导入候选不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.getProjectId());
        return writeGuardService.execute(req.getProjectId(), "reverse-import:database-import", idempotencyKey,
                () -> importCandidatesInternal(req));
    }

    private DatabaseImportResult importCandidatesInternal(DatabaseImportReq req) {
        requireImportDryRunEvidence(req);
        Map<String, Field> standardFieldIndex = standardFieldIndex(req.getProjectId());
        DatabaseImportResult result = new DatabaseImportResult();
        List<ImportedFieldSource> importedSources = new ArrayList<>();
        List<ReverseImportDecision> mappingDecisions = new ArrayList<>();
        Set<String> processedCandidateKeys = new HashSet<>();
        for (FieldCandidate candidate : safeCandidates(req.getCandidates())) {
            if (candidate == null || isBlank(candidate.getColumnName())) {
                continue;
            }
            processedCandidateKeys.add(candidateKey(candidate));
            String normalizedName = normalize(candidate.getColumnName());
            Field existingField = standardFieldIndex.get(normalizedName);
            if (existingField != null) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                result.getSkippedFields().add(candidate.getColumnName());
                mappingDecisions.add(decisionFromCandidate(
                        req.getProjectId(),
                        candidate,
                        DECISION_SKIPPED_EXISTING,
                        existingField.getId(),
                        existingField.getName(),
                        "确认导入时字段名或别名已存在，已跳过创建",
                        fallbackConfidence(candidate, 1.0),
                        "字段已存在于当前项目标准库",
                        candidate.getConfirmReason()));
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
            mappingDecisions.add(decisionFromCandidate(
                    req.getProjectId(),
                    candidate,
                    DECISION_IMPORTED,
                    field.getId(),
                    field.getName(),
                    "用户确认作为新标准字段导入",
                    fallbackConfidence(candidate, 0.85),
                    null,
                    candidate.getConfirmReason()));
        }
        for (FieldCandidate ignored : safeCandidates(req.getIgnoredCandidates())) {
            if (ignored == null || isBlank(ignored.getColumnName())
                    || processedCandidateKeys.contains(candidateKey(ignored))) {
                continue;
            }
            mappingDecisions.add(decisionFromCandidate(
                    req.getProjectId(),
                    ignored,
                    DECISION_IGNORED,
                    ignored.getMatchedFieldId(),
                    ignored.getMatchedFieldName(),
                    fallbackText(ignored.getMatchReason(), "用户本次未选择导入该候选"),
                    fallbackConfidence(ignored, 0.2),
                    fallbackText(ignored.getIgnoreReason(), "本次未选择导入"),
                    ignored.getConfirmReason()));
        }
        if (!mappingDecisions.isEmpty()) {
            var batch = reverseImportSourceService.createDatabaseBatch(
                    req,
                    result.getImportedCount(),
                    result.getSkippedCount());
            result.setBatchId(batch.getId());
            for (ImportedFieldSource imported : importedSources) {
                reverseImportSourceService.recordFieldSource(batch, imported.field(), imported.candidate());
            }
            reverseImportSourceService.recordMappingDecisions(batch, mappingDecisions);
            result.getMappingDecisions().addAll(mappingDecisions);
        }
        return result;
    }

    @Autowired
    void setWriteGuardService(WriteGuardService writeGuardService) {
        this.writeGuardService = writeGuardService;
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

    private Map<String, StandardFieldMatch> standardFieldMatchIndex(Long projectId) {
        Map<String, StandardFieldMatch> index = new HashMap<>();
        for (Field field : fieldService.listByProject(projectId)) {
            putMatchIfPresent(index, field.getName(), field, "name");
            for (String alias : splitCsv(field.getAliases())) {
                putMatchIfPresent(index, alias, field, "alias");
            }
        }
        return index;
    }

    private void putMatchIfPresent(Map<String, StandardFieldMatch> index, String value, Field field, String matchType) {
        if (!isBlank(value)) {
            index.putIfAbsent(normalize(value), new StandardFieldMatch(field, matchType, value));
        }
    }

    private ReverseImportDecision existingMatchDecision(Long projectId,
                                                       String tableName,
                                                       ColumnDef column,
                                                       StandardFieldMatch match) {
        String reason = "alias".equals(match.matchType())
                ? "别名 `" + match.token() + "` 命中标准字段 `" + match.field().getName() + "`"
                : "字段名命中标准字段 `" + match.field().getName() + "`";
        ReverseImportDecision decision = new ReverseImportDecision();
        decision.setProjectId(projectId);
        decision.setTableName(safeDecisionText(tableName));
        decision.setColumnName(safeDecisionText(column.getName()));
        decision.setDataType(safeDecisionText(column.getDataType()));
        decision.setDecisionType(DECISION_EXISTING_MATCH);
        decision.setMatchedFieldId(match.field().getId());
        decision.setMatchedFieldName(safeDecisionText(match.field().getName()));
        decision.setMatchReason(safeDecisionText(reason));
        decision.setConfidence(confidence("alias".equals(match.matchType()) ? 0.92 : 1.0));
        decision.setMetadataJson(writeJson(Map.of(
                "tableName", nullToEmpty(safeDecisionText(tableName)),
                "columnName", nullToEmpty(safeDecisionText(column.getName())),
                "dataType", nullToEmpty(safeDecisionText(column.getDataType())),
                "matchType", safeDecisionText(match.matchType()),
                "matchedFieldName", nullToEmpty(safeDecisionText(match.field().getName()))
        )));
        return decision;
    }

    private ReverseImportDecision decisionFromCandidate(Long projectId,
                                                        FieldCandidate candidate,
                                                        String decisionType,
                                                        Long matchedFieldId,
                                                        String matchedFieldName,
                                                        String matchReason,
                                                        Double confidence,
                                                        String ignoreReason,
                                                        String confirmReason) {
        ReverseImportDecision decision = new ReverseImportDecision();
        decision.setProjectId(projectId);
        decision.setTableName(safeDecisionText(candidate.getTableName()));
        decision.setColumnName(safeDecisionText(candidate.getColumnName()));
        decision.setDataType(safeDecisionText(candidate.getDataType()));
        decision.setDecisionType(decisionType);
        decision.setMatchedFieldId(matchedFieldId);
        decision.setMatchedFieldName(safeDecisionText(matchedFieldName));
        decision.setMatchReason(safeDecisionText(matchReason));
        decision.setConfidence(confidence(confidence == null ? 0.0 : confidence));
        decision.setIgnoreReason(safeDecisionText(ignoreReason));
        decision.setConfirmReason(safeDecisionText(confirmReason));
        decision.setMetadataJson(writeJson(candidateMetadata(candidate)));
        return decision;
    }

    private void requireImportDryRunEvidence(DatabaseImportReq req) {
        String dryRunToken = req.getDryRunToken();
        if (isBlank(dryRunToken)) {
            throw new BizException(400, "缺少 dry-run evidence: operation=reverse-import:database-import");
        }
        JsonNode payload = DryRunEvidenceSigner.verifyPayload("rid", dryRunToken, objectMapper)
                .orElseThrow(() -> new BizException(400, "dry-run evidence 无效: operation=reverse-import:database-import"));
        if (!"reverse-import:database-import".equals(payload.path("operation").asText())
                || !Objects.equals(req.getProjectId(), payload.path("projectId").isMissingNode() ? null : payload.path("projectId").asLong())) {
            throw new BizException(400, "dry-run evidence 与导入项目不匹配: operation=reverse-import:database-import");
        }
        Set<String> allowedCandidateHashes = new HashSet<>();
        payload.path("candidateHashes").forEach(item -> allowedCandidateHashes.add(item.asText()));
        for (FieldCandidate candidate : safeCandidates(req.getCandidates())) {
            if (candidate != null && !Objects.equals(dryRunToken, candidate.getDryRunToken())) {
                throw new BizException(400, "dry-run evidence 与导入候选不匹配: operation=reverse-import:database-import");
            }
            if (candidate != null && !allowedCandidateHashes.contains(candidateEvidenceHash(candidate))) {
                throw new BizException(400, "dry-run evidence 与导入候选不匹配: operation=reverse-import:database-import");
            }
        }
        for (FieldCandidate candidate : safeCandidates(req.getIgnoredCandidates())) {
            if (candidate != null && !Objects.equals(dryRunToken, candidate.getDryRunToken())) {
                throw new BizException(400, "dry-run evidence 与忽略候选不匹配: operation=reverse-import:database-import");
            }
            if (candidate != null && !allowedCandidateHashes.contains(candidateEvidenceHash(candidate))) {
                throw new BizException(400, "dry-run evidence 与忽略候选不匹配: operation=reverse-import:database-import");
            }
        }
    }

    private String reverseImportDryRunToken(Long projectId, ReverseImportPreview preview) {
        List<String> candidateHashes = safeCandidates(preview.getFieldCandidates()).stream()
                .map(this::candidateEvidenceHash)
                .sorted()
                .toList();
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("operation", "reverse-import:database-import");
        payload.put("projectId", projectId);
        payload.put("candidateHashes", candidateHashes);
        return DryRunEvidenceSigner.signPayload("rid", payload, objectMapper);
    }

    private String candidateEvidenceHash(FieldCandidate candidate) {
        try {
            return DryRunEvidenceSigner.sha256Hex(objectMapper.writeValueAsBytes(candidateEvidenceKey(candidate)));
        } catch (Exception e) {
            throw new BizException("计算反向导入候选 dry-run evidence 失败: " + e.getMessage());
        }
    }

    private List<Object> candidateEvidenceKey(FieldCandidate candidate) {
        return List.of(
                normalize(candidate.getTableName()),
                normalize(candidate.getColumnName()),
                nullToEmpty(candidate.getDataType()),
                String.valueOf(candidate.getNullable()),
                nullToEmpty(candidate.getDefaultValue()),
                nullToEmpty(candidate.getComment()));
    }

    private Map<String, Object> candidateMetadata(FieldCandidate candidate) {
        Map<String, Object> metadata = new HashMap<>();
        putMetadata(metadata, "tableName", candidate.getTableName());
        putMetadata(metadata, "columnName", candidate.getColumnName());
        putMetadata(metadata, "dataType", candidate.getDataType());
        putMetadata(metadata, "nullable", candidate.getNullable());
        putMetadata(metadata, "defaultValue", candidate.getDefaultValue());
        putMetadata(metadata, "comment", candidate.getComment());
        putMetadata(metadata, "decisionType", candidate.getDecisionType());
        putMetadata(metadata, "matchedFieldId", candidate.getMatchedFieldId());
        putMetadata(metadata, "matchedFieldName", candidate.getMatchedFieldName());
        putMetadata(metadata, "matchReason", candidate.getMatchReason());
        putMetadata(metadata, "confidence", candidate.getConfidence());
        putMetadata(metadata, "ignoreReason", candidate.getIgnoreReason());
        putMetadata(metadata, "confirmReason", candidate.getConfirmReason());
        return metadata;
    }

    private void putMetadata(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            // 决策快照会持久化给 AI/用户复盘，所有用户可控文本写入前统一脱敏。
            metadata.put(key, SensitiveDataSanitizer.sanitizeValue(value));
        }
    }

    private String safeDecisionText(String value) {
        return SensitiveDataSanitizer.redactText(value);
    }

    private List<FieldCandidate> safeCandidates(List<FieldCandidate> candidates) {
        return candidates == null ? List.of() : candidates;
    }

    private String candidateKey(FieldCandidate candidate) {
        return normalize(candidate.getTableName()) + "::" + normalize(candidate.getColumnName());
    }

    private Double fallbackConfidence(FieldCandidate candidate, double fallback) {
        return candidate.getConfidence() == null ? fallback : candidate.getConfidence();
    }

    private String fallbackText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private BigDecimal confidence(double value) {
        double safeValue = Math.max(0.0, Math.min(value, 1.0));
        return BigDecimal.valueOf(safeValue).setScale(4, RoundingMode.HALF_UP);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("反向导入映射决策序列化失败: " + e.getMessage());
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    private record StandardFieldMatch(Field field, String matchType, String token) {
    }
}
