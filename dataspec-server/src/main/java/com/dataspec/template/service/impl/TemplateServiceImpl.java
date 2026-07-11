package com.dataspec.template.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.tablemodel.entity.BusinessObjectStandard;
import com.dataspec.tablemodel.model.TableForeignKeyStandard;
import com.dataspec.tablemodel.model.TableIndexStandard;
import com.dataspec.tablemodel.model.TablePrimaryKeyStandard;
import com.dataspec.tablemodel.model.TableUniqueKeyStandard;
import com.dataspec.tablemodel.repository.BusinessObjectStandardRepository;
import com.dataspec.tablemodel.service.TableStructureJsonCodec;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 表模板服务实现
 */

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");

    private final TemplateRepository templateRepository;
    private final BusinessObjectStandardRepository businessObjectStandardRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<Template> listByProject(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return templateRepository.findByProjectId(projectId);
    }

    @Override
    public Template getById(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new BizException("表模板不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(template.getProjectId());
        return template;
    }

    @Override
    public Template create(Template template) {
        ProjectAccessGuard.requireProjectAccess(template.getProjectId());
        validateStructureStandard(template, List.of());
        templateRepository.insert(template);
        return template;
    }

    @Override
    public Template update(Long id, Template template) {
        Template existing = getById(id);
        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setTablePrefix(template.getTablePrefix());
        if (hasStructurePayload(template)) {
            existing.setBusinessObjectId(template.getBusinessObjectId());
            existing.setPrimaryKeyJson(template.getPrimaryKeyJson());
            existing.setUniqueKeysJson(template.getUniqueKeysJson());
            existing.setIndexesJson(template.getIndexesJson());
            existing.setForeignKeysJson(template.getForeignKeysJson());
            existing.setCheckHintsJson(template.getCheckHintsJson());
            existing.setAuditPolicyJson(template.getAuditPolicyJson());
            existing.setSoftDeletePolicyJson(template.getSoftDeletePolicyJson());
            existing.setDialectNotesJson(template.getDialectNotesJson());
            existing.setAiUsageNotes(template.getAiUsageNotes());
            validateStructureStandard(existing, templateRepository.findFieldsByTemplateId(id));
        }
        templateRepository.update(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Template existing = getById(id);
        templateRepository.deleteFieldsByTemplateId(id);
        templateRepository.deleteById(id);
    }

    @Override
    public List<TemplateField> listFields(Long templateId) {
        getById(templateId);
        return templateRepository.findFieldsByTemplateId(templateId);
    }

    @Override
    public TemplateField createField(TemplateField field) {
        getById(field.getTemplateId());
        templateRepository.insertField(field);
        return field;
    }

    @Override
    public TemplateField updateField(Long id, TemplateField field) {
        TemplateField existing = getFieldById(id);
        getById(existing.getTemplateId());
        field.setId(id);
        field.setTemplateId(existing.getTemplateId());
        templateRepository.updateField(field);
        return field;
    }

    @Override
    public void deleteField(Long id) {
        TemplateField existing = getFieldById(id);
        getById(existing.getTemplateId());
        templateRepository.deleteFieldById(id);
    }

    private TemplateField getFieldById(Long id) {
        return templateRepository.findFieldById(id)
                .orElseThrow(() -> new BizException("模板字段不存在: " + id));
    }

    private void validateStructureStandard(Template template, List<TemplateField> fields) {
        if (template.getBusinessObjectId() != null) {
            BusinessObjectStandard standard = businessObjectStandardRepository.findById(template.getBusinessObjectId())
                    .orElseThrow(() -> new BizException("关联业务对象不存在: " + template.getBusinessObjectId()));
            if (!Objects.equals(standard.getProjectId(), template.getProjectId())) {
                throw new BizException("关联业务对象不属于当前项目");
            }
        }
        if (SensitiveDataSanitizer.containsSensitiveText(template.getAiUsageNotes())) {
            throw new BizException("模板 AI 使用说明不能包含 token、密码、Authorization、JDBC URL、DSN 或连接串");
        }
        Set<String> columns = fields.stream()
                .map(TemplateField::getName)
                .filter(this::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        if (columns.isEmpty() && hasColumnConstraintPayload(template)) {
            throw new BizException("配置表结构标准前需要先维护模板字段");
        }
        TableStructureJsonCodec codec = codec();
        TablePrimaryKeyStandard primaryKey = codec.read(
                template.getPrimaryKeyJson(), TablePrimaryKeyStandard.class, null, "主键标准");
        validateColumns(primaryKey == null ? List.of() : primaryKey.columns(), columns, "主键标准");
        for (TableUniqueKeyStandard uniqueKey : codec.readList(template.getUniqueKeysJson(), TableUniqueKeyStandard.class, "唯一键标准")) {
            validateColumns(uniqueKey.columns(), columns, "唯一键标准");
            validateOptionalIdentifier(uniqueKey.name(), "唯一键约束名");
        }
        for (TableIndexStandard index : codec.readList(template.getIndexesJson(), TableIndexStandard.class, "索引标准")) {
            validateColumns(index.columns(), columns, "索引标准");
            validateOptionalIdentifier(index.name(), "索引名");
            if (hasText(index.method()) && !"btree".equalsIgnoreCase(index.method().trim())) {
                throw new BizException("第一版索引方法只支持 btree");
            }
        }
        for (TableForeignKeyStandard foreignKey : codec.readList(template.getForeignKeysJson(), TableForeignKeyStandard.class, "外键标准")) {
            validateColumns(foreignKey.columns(), columns, "外键标准");
            validateColumns(foreignKey.targetColumns(), Set.copyOf(foreignKey.targetColumns() == null ? List.of() : foreignKey.targetColumns()), "外键目标列");
            validateOptionalIdentifier(foreignKey.name(), "外键约束名");
            validateOptionalIdentifier(foreignKey.targetTable(), "外键目标表名");
        }
    }

    private void validateColumns(List<String> values, Set<String> allowedColumns, String label) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String value : values) {
            validateOptionalIdentifier(value, label + "字段");
            if (!allowedColumns.contains(value.trim())) {
                throw new BizException(label + "引用了模板中不存在的字段: " + value);
            }
        }
    }

    private void validateOptionalIdentifier(String value, String label) {
        if (!hasText(value)) {
            return;
        }
        if (!IDENTIFIER_PATTERN.matcher(value.trim()).matches()) {
            throw new BizException(label + "必须是 snake_case 标识符");
        }
    }

    private boolean hasStructurePayload(Template template) {
        return template.getBusinessObjectId() != null
                || hasColumnConstraintPayload(template)
                || hasText(template.getCheckHintsJson())
                || hasText(template.getAuditPolicyJson())
                || hasText(template.getSoftDeletePolicyJson())
                || hasText(template.getDialectNotesJson())
                || hasText(template.getAiUsageNotes());
    }

    private boolean hasColumnConstraintPayload(Template template) {
        return hasText(template.getPrimaryKeyJson())
                || hasText(template.getUniqueKeysJson())
                || hasText(template.getIndexesJson())
                || hasText(template.getForeignKeysJson());
    }

    private TableStructureJsonCodec codec() {
        return new TableStructureJsonCodec(objectMapper);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
