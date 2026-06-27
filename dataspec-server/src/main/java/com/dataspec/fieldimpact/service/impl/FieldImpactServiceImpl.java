package com.dataspec.fieldimpact.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldimpact.model.FieldEditWarning;
import com.dataspec.fieldimpact.model.FieldImpactItem;
import com.dataspec.fieldimpact.model.FieldImpactReport;
import com.dataspec.fieldimpact.model.FieldImpactSeverity;
import com.dataspec.fieldimpact.model.FieldImpactSummary;
import com.dataspec.fieldimpact.model.FieldImpactType;
import com.dataspec.fieldimpact.service.FieldImpactService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.reverseimport.model.FieldSourceDetail;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 基于现有记录实时聚合字段影响，不写入任何业务表。
 */
@Service
@RequiredArgsConstructor
public class FieldImpactServiceImpl implements FieldImpactService {

    private static final int RECENT_SQL_CHECK_LIMIT = 20;

    private final FieldService fieldService;
    private final TemplateRepository templateRepository;
    private final ReverseImportSourceService reverseImportSourceService;
    private final SqlCheckRecordService sqlCheckRecordService;
    private final StandardSnapshotService standardSnapshotService;

    @Override
    public FieldImpactReport report(Long projectId, Long fieldId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (fieldId == null) {
            throw new BizException("字段ID不能为空");
        }

        Field field = fieldService.getById(fieldId);
        if (field == null) {
            throw new BizException("字段不存在: " + fieldId);
        }
        if (!Objects.equals(field.getProjectId(), projectId)) {
            throw new BizException("字段不属于当前项目");
        }

        FieldImpactReport report = baseReport(projectId, field);
        addTemplateImpacts(report, field);
        addImportSourceImpacts(report, field);
        addSqlCheckImpacts(report, field);
        addSnapshotImpact(report, projectId);
        addCodeSetImpact(report, field);
        buildSummary(report);
        buildWarnings(report);
        return report;
    }

    private FieldImpactReport baseReport(Long projectId, Field field) {
        FieldImpactReport report = new FieldImpactReport();
        report.setProjectId(projectId);
        report.setFieldId(field.getId());
        report.setFieldName(field.getName());
        report.setDisplayName(field.getDisplayName());
        return report;
    }

    private void addTemplateImpacts(FieldImpactReport report, Field field) {
        List<Template> templates = templateRepository.findByProjectId(report.getProjectId());
        for (Template template : templates) {
            List<TemplateField> matches = templateRepository.findFieldsByTemplateId(template.getId()).stream()
                    .filter(templateField -> referencesField(templateField, field))
                    .toList();
            if (matches.isEmpty()) {
                continue;
            }
            FieldImpactItem item = item(
                    FieldImpactType.TEMPLATE,
                    FieldImpactSeverity.WARNING,
                    template.getId(),
                    template.getName(),
                    matches.size(),
                    "字段被表模板引用，修改名称或类型会影响后续 DDL 生成"
            );
            item.getMetadata().put("templateId", template.getId());
            item.getMetadata().put("templateName", template.getName());
            item.getMetadata().put("templateFieldIds", matches.stream().map(TemplateField::getId).toList());
            report.getImpacts().add(item);
        }
    }

    private boolean referencesField(TemplateField templateField, Field field) {
        return Objects.equals(templateField.getFieldId(), field.getId())
                || Objects.equals(nullToEmpty(templateField.getName()), nullToEmpty(field.getName()));
    }

    private void addImportSourceImpacts(FieldImpactReport report, Field field) {
        for (FieldSourceDetail detail : reverseImportSourceService.listByFieldId(field.getId())) {
            if (detail.source() == null || !Objects.equals(detail.source().getProjectId(), report.getProjectId())) {
                continue;
            }
            String sourceName = List.of(
                            nullToEmpty(detail.source().getSchemaName()),
                            nullToEmpty(detail.source().getTableName()),
                            nullToEmpty(detail.source().getColumnName()))
                    .stream()
                    .filter(part -> !part.isBlank())
                    .reduce((left, right) -> left + "." + right)
                    .orElse("");
            FieldImpactItem item = item(
                    FieldImpactType.IMPORT_SOURCE,
                    FieldImpactSeverity.INFO,
                    detail.source().getId(),
                    sourceName.isBlank() ? "数据库反向导入来源" : sourceName,
                    1,
                    "字段来自数据库反向导入，修改前可回看来源字段"
            );
            item.getMetadata().put("databaseType", detail.batch() == null ? null : detail.batch().getDatabaseType());
            item.getMetadata().put("databaseName", detail.batch() == null ? null : detail.batch().getDatabaseName());
            item.getMetadata().put("schemaName", detail.source().getSchemaName());
            item.getMetadata().put("tableName", detail.source().getTableName());
            item.getMetadata().put("columnName", detail.source().getColumnName());
            item.getMetadata().put("latestImportTime", detail.source().getCreatedAt());
            report.getImpacts().add(item);
        }
    }

    private void addSqlCheckImpacts(FieldImpactReport report, Field field) {
        IPage<SqlCheckRecord> page = sqlCheckRecordService.listByProject(
                report.getProjectId(),
                1,
                RECENT_SQL_CHECK_LIMIT
        );
        for (SqlCheckRecord record : page.getRecords()) {
            if (!recordMentionsField(record, field)) {
                continue;
            }
            FieldImpactItem item = item(
                    FieldImpactType.SQL_CHECK,
                    FieldImpactSeverity.INFO,
                    record.getId(),
                    "SQL 检查记录 " + record.getId(),
                    1,
                    "最近 SQL 检查记录疑似引用该字段，修改后可重新校验相关 SQL"
            );
            item.setPossibleReference(true);
            item.getMetadata().put("createdAt", record.getCreatedAt());
            item.getMetadata().put("errorCount", record.getErrorCount());
            item.getMetadata().put("warningCount", record.getWarningCount());
            report.getImpacts().add(item);
        }
    }

    private boolean recordMentionsField(SqlCheckRecord record, Field field) {
        String text = String.join(" ",
                nullToEmpty(record.getOriginalSql()),
                nullToEmpty(record.getFixedSql()),
                nullToEmpty(record.getIssuesJson())
        ).toLowerCase(Locale.ROOT);
        return containsToken(text, field.getName()) || containsToken(text, field.getDisplayName());
    }

    private boolean containsToken(String text, String value) {
        String normalized = nullToEmpty(value).toLowerCase(Locale.ROOT);
        return !normalized.isBlank() && text.contains(normalized);
    }

    private void addSnapshotImpact(FieldImpactReport report, Long projectId) {
        List<StandardSnapshotInfo> snapshots = standardSnapshotService.listSnapshots(projectId);
        if (snapshots.isEmpty()) {
            return;
        }
        StandardSnapshotInfo latest = snapshots.stream()
                .max(Comparator.comparing(
                        StandardSnapshotInfo::createdAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ))
                .orElse(snapshots.getFirst());
        FieldImpactItem item = item(
                FieldImpactType.STANDARD_SNAPSHOT,
                FieldImpactSeverity.INFO,
                latest.snapshotId(),
                latest.specVersion(),
                snapshots.size(),
                "项目存在标准快照或 AI Context 版本引用，修改字段后建议创建新快照"
        );
        item.getMetadata().put("specVersion", latest.specVersion());
        item.getMetadata().put("specHash", latest.specHash());
        item.getMetadata().put("createdAt", latest.createdAt());
        report.getImpacts().add(item);
    }

    private void addCodeSetImpact(FieldImpactReport report, Field field) {
        if (field.getCodeSetId() == null) {
            return;
        }
        FieldImpactItem item = item(
                FieldImpactType.CODE_SET,
                FieldImpactSeverity.INFO,
                field.getCodeSetId(),
                "代码集 " + field.getCodeSetId(),
                1,
                "字段关联代码集，修改类型或状态前需确认枚举语义仍匹配"
        );
        item.getMetadata().put("codeSetId", field.getCodeSetId());
        report.getImpacts().add(item);
    }

    private FieldImpactItem item(
            FieldImpactType type,
            FieldImpactSeverity severity,
            Long sourceId,
            String sourceName,
            int count,
            String description
    ) {
        FieldImpactItem item = new FieldImpactItem();
        item.setImpactType(type);
        item.setSeverity(severity);
        item.setSourceId(sourceId);
        item.setSourceName(sourceName);
        item.setCount(count);
        item.setDescription(description);
        item.setMetadata(new LinkedHashMap<>());
        return item;
    }

    private void buildSummary(FieldImpactReport report) {
        FieldImpactSummary summary = report.getSummary();
        summary.setTotalImpactCount(report.getImpacts().size());
        for (FieldImpactItem item : report.getImpacts()) {
            incrementImpactCount(summary, item.getImpactType());
        }
    }

    private void incrementImpactCount(FieldImpactSummary summary, FieldImpactType type) {
        if (FieldImpactType.TEMPLATE.equals(type)) {
            summary.setTemplateImpactCount(summary.getTemplateImpactCount() + 1);
        } else if (FieldImpactType.IMPORT_SOURCE.equals(type)) {
            summary.setImportSourceImpactCount(summary.getImportSourceImpactCount() + 1);
        } else if (FieldImpactType.SQL_CHECK.equals(type)) {
            summary.setSqlCheckImpactCount(summary.getSqlCheckImpactCount() + 1);
        } else if (FieldImpactType.STANDARD_SNAPSHOT.equals(type)) {
            summary.setSnapshotImpactCount(summary.getSnapshotImpactCount() + 1);
        } else if (FieldImpactType.CODE_SET.equals(type)) {
            summary.setCodeSetImpactCount(summary.getCodeSetImpactCount() + 1);
        }
    }

    private void buildWarnings(FieldImpactReport report) {
        if (report.getImpacts().isEmpty()) {
            return;
        }
        report.getEditWarnings().addAll(List.of(
                warning("name", "字段名变更会影响模板、历史 SQL 和 AI Context 中的字段引用"),
                warning("dataType", "字段类型变更会影响 DDL 生成和 SQL 校验判断"),
                warning("status", "字段状态变更会影响 AI 是否继续推荐该字段"),
                warning("codeSetId", "代码集变更会影响枚举/状态语义"),
                warning("sensitive", "敏感标记变更会影响 AI Context 和导出提示")
        ));
        report.getSummary().setWarningCount(report.getEditWarnings().size());
    }

    private FieldEditWarning warning(String attribute, String message) {
        return new FieldEditWarning(attribute, FieldImpactSeverity.WARNING, message);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
