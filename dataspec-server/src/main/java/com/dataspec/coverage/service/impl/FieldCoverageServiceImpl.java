package com.dataspec.coverage.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.coverage.model.FieldCoverageItem;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.model.FieldCoverageStatus;
import com.dataspec.coverage.model.FieldCoverageSummary;
import com.dataspec.coverage.model.FieldCoverageTable;
import com.dataspec.coverage.model.UnmanagedFieldRanking;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.DatabaseMetadataScanFailureSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataScanPartialResult;
import com.dataspec.reverseimport.model.DatabaseSchemaColumn;
import com.dataspec.reverseimport.model.DatabaseSchemaTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 即时字段覆盖率报告实现。
 */
@Service
@RequiredArgsConstructor
public class FieldCoverageServiceImpl implements FieldCoverageService {

    private final FieldService fieldService;
    private final SqlParserService sqlParserService;

    @Override
    public FieldCoverageReport reportSql(Long projectId, String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BizException("SQL 不能为空");
        }
        return reportTables(projectId, sqlParserService.parse(sql));
    }

    @Override
    public FieldCoverageReport reportTables(Long projectId, List<TableDef> tables) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (tables == null || tables.isEmpty()) {
            throw new BizException("未读取到可分析的表结构");
        }

        StandardIndex standardIndex = buildStandardIndex(projectId);
        FieldCoverageReport report = new FieldCoverageReport();
        RankingAccumulator ranking = new RankingAccumulator();

        for (TableDef table : tables) {
            FieldCoverageTable tableReport = analyzeTable(projectId, table, standardIndex, ranking);
            report.getTables().add(tableReport);
            accumulate(report.getSummary(), tableReport);
        }
        report.getSummary().setTableCount(report.getTables().size());
        report.getSummary().setCoverageRate(coverageRate(report.getSummary().getCoveredCount(), report.getSummary().getColumnCount()));
        report.setUnmanagedRankings(ranking.toRankings());
        return report;
    }

    @Override
    public FieldCoverageReport reportScanPartial(Long projectId,
                                                 DatabaseMetadataScanPartialResult partialResult,
                                                 DatabaseMetadataScanFailureSummary failureSummary,
                                                 String scanStatus) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (partialResult == null) {
            throw new BizException("采集作业部分结果不能为空");
        }
        List<DatabaseSchemaTable> successfulTables = partialResult.getSuccessfulTables() == null
                ? List.of()
                : partialResult.getSuccessfulTables();
        if (successfulTables.isEmpty()) {
            throw new BizException("未读取到可分析的成功表结构");
        }

        FieldCoverageReport report = reportTables(projectId, successfulTables.stream()
                .map(this::toTableDef)
                .toList());
        int failedCount = failedTableCount(partialResult, failureSummary);
        int skippedCount = partialResult.getSkippedTableNames() == null ? 0 : partialResult.getSkippedTableNames().size();
        report.setFailedTableCount(failedCount);
        report.setSkippedTableCount(skippedCount);
        report.setInputStatus(coverageInputStatus(scanStatus, partialResult, failedCount, skippedCount));
        if (!"COMPLETE".equals(report.getInputStatus())) {
            // partial coverage 的核心约束：只统计 successful tables，失败/跳过/未扫描表不能被误认为已覆盖。
            report.getNextActions().add("覆盖率只包含 successful partial tables；failed/skipped/not-yet-scanned 表不会视为已覆盖。");
        }
        if (failureSummary != null && failureSummary.getSafeNextActions() != null) {
            for (String action : failureSummary.getSafeNextActions()) {
                report.getNextActions().add(sanitizeCoverageAction(action));
            }
        }
        deduplicateNextActions(report);
        return report;
    }

    private TableDef toTableDef(DatabaseSchemaTable table) {
        List<DatabaseSchemaColumn> columns = table.getColumns() == null ? List.of() : table.getColumns();
        return TableDef.builder()
                .name(table.getTableName())
                .comment(table.getComment())
                .columns(columns.stream()
                        .map(column -> ColumnDef.builder()
                                .name(column.getColumnName())
                                .dataType(column.getDataType())
                                .nullable(Boolean.TRUE.equals(column.getNullable()))
                                .defaultValue(column.getDefaultValue())
                                .comment(column.getComment())
                                .build())
                        .toList())
                .build();
    }

    private int failedTableCount(DatabaseMetadataScanPartialResult partialResult,
                                 DatabaseMetadataScanFailureSummary failureSummary) {
        if (failureSummary != null && failureSummary.getFailedTableCount() > 0) {
            return failureSummary.getFailedTableCount();
        }
        return partialResult.getFailedTableNames() == null ? 0 : partialResult.getFailedTableNames().size();
    }

    private String coverageInputStatus(String scanStatus,
                                       DatabaseMetadataScanPartialResult partialResult,
                                       int failedCount,
                                       int skippedCount) {
        String normalized = normalize(scanStatus).toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(normalized) || "FAILED".equals(normalized)) {
            return normalized;
        }
        if (!partialResult.isComplete() || failedCount > 0 || skippedCount > 0 || "PARTIAL".equals(normalized)) {
            return "PARTIAL";
        }
        return "COMPLETE";
    }

    private void deduplicateNextActions(FieldCoverageReport report) {
        Set<String> actions = new LinkedHashSet<>();
        for (String action : report.getNextActions()) {
            if (!isBlank(action)) {
                actions.add(action);
            }
        }
        report.setNextActions(List.copyOf(actions));
    }

    private String sanitizeCoverageAction(String action) {
        if (action == null) {
            return null;
        }
        return SensitiveDataSanitizer.redactText(action)
                .replaceAll("(?i)authorization\\s*[:=]\\s*[^\\s,;]+(?:\\s+[^\\s,;]+)?", "[REDACTED]");
    }

    private FieldCoverageTable analyzeTable(
            Long projectId,
            TableDef table,
            StandardIndex standardIndex,
            RankingAccumulator ranking
    ) {
        FieldCoverageTable tableReport = new FieldCoverageTable();
        tableReport.setTableName(table.getName());
        tableReport.setComment(table.getComment());
        List<ColumnDef> columns = table.getColumns() == null ? List.of() : table.getColumns();
        for (ColumnDef column : columns) {
            FieldCoverageItem item = analyzeColumn(projectId, table.getName(), column, standardIndex);
            tableReport.getFields().add(item);
            tableReport.setColumnCount(tableReport.getColumnCount() + 1);
            if (item.isCovered()) {
                tableReport.setCoveredCount(tableReport.getCoveredCount() + 1);
            } else {
                tableReport.setUnmanagedCount(tableReport.getUnmanagedCount() + 1);
                ranking.add(item);
            }
            if (isBlank(item.getComment())) {
                tableReport.setMissingCommentCount(tableReport.getMissingCommentCount() + 1);
            }
            if (FieldCoverageStatus.POSSIBLE_DUPLICATE.equals(item.getStatus())) {
                tableReport.setPossibleDuplicateCount(tableReport.getPossibleDuplicateCount() + 1);
            }
        }
        tableReport.setCoverageRate(coverageRate(tableReport.getCoveredCount(), tableReport.getColumnCount()));
        return tableReport;
    }

    private FieldCoverageItem analyzeColumn(
            Long projectId,
            String tableName,
            ColumnDef column,
            StandardIndex standardIndex
    ) {
        FieldCoverageItem item = baseItem(tableName, column);
        IndexedField indexedField = standardIndex.find(column.getName());
        if (indexedField != null) {
            applyMatchedField(item, indexedField, isBlank(column.getComment()));
            return item;
        }

        FieldSuggestion suggestion = bestExistingSuggestion(projectId, column.getName());
        if (suggestion != null) {
            item.setStatus(FieldCoverageStatus.POSSIBLE_DUPLICATE);
            item.setRecommendedFieldName(suggestion.recommendedName());
            item.setStandardFieldName(suggestion.field().getName());
            item.setStandardDisplayName(suggestion.field().getDisplayName());
            item.setReason("未直接命中标准字段，但存在疑似重复字段: " + suggestion.matchReason());
            return item;
        }

        item.setStatus(FieldCoverageStatus.UNMANAGED);
        item.setReason("未命中标准字段名或别名");
        return item;
    }

    private FieldCoverageItem baseItem(String tableName, ColumnDef column) {
        FieldCoverageItem item = new FieldCoverageItem();
        item.setTableName(tableName);
        item.setColumnName(column.getName());
        item.setDataType(column.getDataType());
        item.setComment(column.getComment());
        return item;
    }

    private void applyMatchedField(FieldCoverageItem item, IndexedField indexedField, boolean missingComment) {
        Field standardField = indexedField.field();
        item.setCovered(true);
        item.setStandardFieldName(standardField.getName());
        item.setStandardDisplayName(standardField.getDisplayName());
        item.setMatchType(indexedField.matchType());
        if (missingComment) {
            item.setStatus(FieldCoverageStatus.MISSING_COMMENT);
            item.setReason("已命中标准字段，但数据库字段缺少注释");
        } else if ("alias".equals(indexedField.matchType())) {
            item.setStatus(FieldCoverageStatus.ALIAS_MATCH);
            item.setReason("字段别名命中标准字段");
        } else {
            item.setStatus(FieldCoverageStatus.STANDARD_MATCH);
            item.setReason("字段名命中标准字段");
        }
    }

    private FieldSuggestion bestExistingSuggestion(Long projectId, String columnName) {
        if (isBlank(columnName)) {
            return null;
        }
        try {
            List<FieldSuggestion> suggestions = fieldService.suggest(projectId, columnName, 1);
            if (suggestions == null) {
                return null;
            }
            return suggestions.stream()
                    .filter(FieldSuggestion::existing)
                    .filter(suggestion -> suggestion.field() != null)
                    .findFirst()
                    .orElse(null);
        } catch (BizException ignored) {
            return null;
        }
    }

    private StandardIndex buildStandardIndex(Long projectId) {
        StandardIndex index = new StandardIndex();
        for (Field field : fieldService.listByProject(projectId)) {
            if (!isEnabled(field)) {
                continue;
            }
            index.putName(field.getName(), field);
            for (String alias : splitCsv(field.getAliases())) {
                index.putAlias(alias, field);
            }
        }
        return index;
    }

    private void accumulate(FieldCoverageSummary summary, FieldCoverageTable table) {
        summary.setColumnCount(summary.getColumnCount() + table.getColumnCount());
        summary.setCoveredCount(summary.getCoveredCount() + table.getCoveredCount());
        summary.setUnmanagedCount(summary.getUnmanagedCount() + table.getUnmanagedCount());
        summary.setMissingCommentCount(summary.getMissingCommentCount() + table.getMissingCommentCount());
        summary.setPossibleDuplicateCount(summary.getPossibleDuplicateCount() + table.getPossibleDuplicateCount());
    }

    private double coverageRate(int covered, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((covered * 1000.0) / total) / 10.0;
    }

    private boolean isEnabled(Field field) {
        return isBlank(field.getStatus()) || "enabled".equalsIgnoreCase(field.getStatus());
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

    private record IndexedField(Field field, String matchType) {
    }

    private class StandardIndex {
        private final Map<String, IndexedField> names = new HashMap<>();
        private final Map<String, IndexedField> aliases = new HashMap<>();

        void putName(String name, Field field) {
            if (!isBlank(name)) {
                names.putIfAbsent(normalize(name), new IndexedField(field, "name"));
            }
        }

        void putAlias(String alias, Field field) {
            if (!isBlank(alias)) {
                aliases.putIfAbsent(normalize(alias), new IndexedField(field, "alias"));
            }
        }

        IndexedField find(String columnName) {
            String key = normalize(columnName);
            IndexedField nameMatch = names.get(key);
            return nameMatch != null ? nameMatch : aliases.get(key);
        }
    }

    private class RankingAccumulator {
        private final Map<String, UnmanagedFieldRanking> rankings = new LinkedHashMap<>();

        void add(FieldCoverageItem item) {
            String key = normalize(item.getColumnName());
            if (key.isBlank()) {
                return;
            }
            UnmanagedFieldRanking ranking = rankings.computeIfAbsent(key, ignored -> {
                UnmanagedFieldRanking created = new UnmanagedFieldRanking();
                created.setColumnName(item.getColumnName());
                return created;
            });
            ranking.setCount(ranking.getCount() + 1);
            if (!ranking.getTables().contains(item.getTableName())) {
                ranking.getTables().add(item.getTableName());
            }
            if (ranking.getRecommendedFieldName() == null && item.getRecommendedFieldName() != null) {
                ranking.setRecommendedFieldName(item.getRecommendedFieldName());
                ranking.setReason(item.getReason());
            }
        }

        List<UnmanagedFieldRanking> toRankings() {
            return rankings.values().stream()
                    .sorted(Comparator
                            .comparingInt(UnmanagedFieldRanking::getCount).reversed()
                            .thenComparing(UnmanagedFieldRanking::getColumnName, Comparator.nullsLast(String::compareTo)))
                    .toList();
        }
    }
}
