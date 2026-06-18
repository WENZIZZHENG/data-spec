package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.MissingCommentIssue;
import com.dataspec.reverseimport.model.NonStandardField;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.model.ReverseImportSummary;
import com.dataspec.reverseimport.service.ReverseImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL 反向导入预览服务实现。
 */
@Service
@RequiredArgsConstructor
public class ReverseImportServiceImpl implements ReverseImportService {

    private final SqlParserService sqlParserService;
    private final FieldService fieldService;

    @Override
    public ReverseImportPreview preview(Long projectId, String sql) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (sql == null || sql.isBlank()) {
            throw new BizException("SQL 不能为空");
        }

        List<TableDef> tables = sqlParserService.parse(sql);
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
}
