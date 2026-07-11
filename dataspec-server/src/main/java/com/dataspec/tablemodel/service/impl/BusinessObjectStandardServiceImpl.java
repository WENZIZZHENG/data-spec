package com.dataspec.tablemodel.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.tablemodel.entity.BusinessObjectStandard;
import com.dataspec.tablemodel.model.BusinessObjectStandardReq;
import com.dataspec.tablemodel.model.BusinessObjectStandardResp;
import com.dataspec.tablemodel.model.TableAuditPolicy;
import com.dataspec.tablemodel.model.TableForeignKeyStandard;
import com.dataspec.tablemodel.model.TableRelationHint;
import com.dataspec.tablemodel.model.TableRelationSummary;
import com.dataspec.tablemodel.model.TableRelationSummaryEdge;
import com.dataspec.tablemodel.model.TableRelationSummaryNode;
import com.dataspec.tablemodel.model.TableRelationSummaryStats;
import com.dataspec.tablemodel.repository.BusinessObjectStandardRepository;
import com.dataspec.tablemodel.service.BusinessObjectStandardService;
import com.dataspec.tablemodel.service.TableStructureJsonCodec;
import com.dataspec.template.entity.Template;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 业务对象标准服务实现，第一版以轻量 JSON 结构保存对象关系和表模板依赖。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectStandardServiceImpl implements BusinessObjectStandardService {

    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,99}");
    private static final String STATUS_ENABLED = "ENABLED";

    private final BusinessObjectStandardRepository repository;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    public List<BusinessObjectStandardResp> listByProject(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return repository.findByProjectId(projectId).stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public BusinessObjectStandardResp getById(Long id) {
        return toResp(loadAndCheck(id));
    }

    @Override
    public BusinessObjectStandardResp getByObjectKey(Long projectId, String objectKey) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        String normalizedKey = normalizeObjectKey(objectKey);
        BusinessObjectStandard standard = repository.findByObjectKey(projectId, normalizedKey)
                .orElseThrow(() -> new BizException(404, "业务对象不存在: " + normalizedKey));
        return toResp(standard);
    }

    @Override
    public BusinessObjectStandardResp create(BusinessObjectStandardReq req) {
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        BusinessObjectStandard standard = new BusinessObjectStandard();
        apply(req, standard, null);
        repository.insert(standard);
        return toResp(standard);
    }

    @Override
    public BusinessObjectStandardResp update(Long id, BusinessObjectStandardReq req) {
        BusinessObjectStandard existing = loadAndCheck(id);
        if (!Objects.equals(existing.getProjectId(), req.projectId())) {
            throw new BizException("业务对象不属于请求项目");
        }
        apply(req, existing, id);
        repository.update(existing);
        return toResp(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BusinessObjectStandard existing = loadAndCheck(id);
        repository.deleteById(existing.getId());
    }

    @Override
    public TableRelationSummary relationSummary(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        List<BusinessObjectStandard> standards = repository.findByProjectId(projectId);
        Map<String, TableRelationSummaryNode> nodes = new LinkedHashMap<>();
        List<TableRelationSummaryEdge> edges = new ArrayList<>();
        Map<String, BusinessObjectStandard> byKey = new LinkedHashMap<>();
        for (BusinessObjectStandard standard : standards) {
            byKey.put(standard.getObjectKey(), standard);
            nodes.put(objectNodeId(standard.getObjectKey()), new TableRelationSummaryNode(
                    objectNodeId(standard.getObjectKey()),
                    "BUSINESS_OBJECT",
                    standard.getEntityName(),
                    standard.getId()
            ));
            if (standard.getTemplateId() != null) {
                String templateNode = templateNodeId(standard.getTemplateId());
                nodes.putIfAbsent(templateNode, new TableRelationSummaryNode(
                        templateNode,
                        "TEMPLATE",
                        "template:" + standard.getTemplateId(),
                        standard.getTemplateId()
                ));
                edges.add(new TableRelationSummaryEdge(
                        objectNodeId(standard.getObjectKey()),
                        templateNode,
                        "USES_TEMPLATE",
                        "HIGH",
                        "业务对象关联表模板"
                ));
            }
            for (TableRelationHint relation : codec().readList(standard.getRelationsJson(), TableRelationHint.class, "业务对象关系")) {
                String source = hasText(relation.sourceObjectKey()) ? relation.sourceObjectKey() : standard.getObjectKey();
                String target = relation.targetObjectKey();
                if (!hasText(target)) {
                    continue;
                }
                nodes.putIfAbsent(objectNodeId(source), new TableRelationSummaryNode(
                        objectNodeId(source),
                        "BUSINESS_OBJECT",
                        byKey.containsKey(source) ? byKey.get(source).getEntityName() : source,
                        byKey.containsKey(source) ? byKey.get(source).getId() : null
                ));
                nodes.putIfAbsent(objectNodeId(target), new TableRelationSummaryNode(
                        objectNodeId(target),
                        "BUSINESS_OBJECT",
                        byKey.containsKey(target) ? byKey.get(target).getEntityName() : target,
                        byKey.containsKey(target) ? byKey.get(target).getId() : null
                ));
                edges.add(new TableRelationSummaryEdge(
                        objectNodeId(source),
                        objectNodeId(target),
                        defaultText(relation.relationType(), "RELATES_TO"),
                        defaultText(relation.confidence(), "MEDIUM"),
                        defaultText(relation.notes(), "业务对象关系提示")
                ));
            }
            for (TableForeignKeyStandard foreignKey : codec().readList(
                    standard.getForeignKeyHintsJson(), TableForeignKeyStandard.class, "业务对象外键提示")) {
                if (!hasText(foreignKey.targetTable())) {
                    continue;
                }
                String targetNode = "table:" + foreignKey.targetTable();
                nodes.putIfAbsent(targetNode, new TableRelationSummaryNode(
                        targetNode,
                        "TABLE",
                        foreignKey.targetTable(),
                        null
                ));
                edges.add(new TableRelationSummaryEdge(
                        objectNodeId(standard.getObjectKey()),
                        targetNode,
                        "FOREIGN_KEY_HINT",
                        "MEDIUM",
                        defaultText(foreignKey.notes(), "外键提示")
                ));
            }
        }
        long templateCount = nodes.values().stream().filter(node -> "TEMPLATE".equals(node.type())).count();
        return new TableRelationSummary(
                projectId,
                List.copyOf(nodes.values()),
                edges,
                new TableRelationSummaryStats(standards.size(), (int) templateCount, edges.size())
        );
    }

    private BusinessObjectStandard loadAndCheck(Long id) {
        BusinessObjectStandard standard = repository.findById(id)
                .orElseThrow(() -> new BizException(404, "业务对象不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(standard.getProjectId());
        return standard;
    }

    private void apply(BusinessObjectStandardReq req, BusinessObjectStandard target, Long excludeId) {
        String objectKey = normalizeObjectKey(req.objectKey());
        String entityName = normalizeRequiredText(req.entityName(), "业务对象名称");
        if (repository.existsByObjectKey(req.projectId(), objectKey, excludeId)) {
            throw new BizException("业务对象键已存在: " + objectKey);
        }
        if (repository.existsByEntityName(req.projectId(), entityName, excludeId)) {
            throw new BizException("业务对象名称已存在: " + entityName);
        }
        if (req.templateId() != null) {
            Template template = templateService.getById(req.templateId());
            if (!Objects.equals(template.getProjectId(), req.projectId())) {
                throw new BizException("关联表模板不属于当前项目");
            }
        }
        target.setProjectId(req.projectId());
        target.setObjectKey(objectKey);
        target.setEntityName(entityName);
        target.setTablePattern(trimToNull(req.tablePattern()));
        target.setTemplateId(req.templateId());
        target.setRequiredFieldsJson(codec().write(req.requiredFields(), "必选字段"));
        target.setOptionalFieldsJson(codec().write(req.optionalFields(), "可选字段"));
        target.setRelationsJson(codec().write(req.relations(), "业务对象关系"));
        target.setForeignKeyHintsJson(codec().write(req.foreignKeyHints(), "外键提示"));
        target.setAuditFieldsJson(codec().write(req.auditFields(), "审计字段"));
        target.setCommonPitfallsJson(codec().write(req.commonPitfalls(), "常见反模式"));
        target.setAiUsageNotes(validatePlainText(req.aiUsageNotes(), "AI 使用说明"));
        target.setContextExport(req.contextExport() == null || req.contextExport());
        target.setStatus(normalizeStatus(req.status()));
    }

    private BusinessObjectStandardResp toResp(BusinessObjectStandard standard) {
        return new BusinessObjectStandardResp(
                standard.getId(),
                standard.getProjectId(),
                standard.getObjectKey(),
                standard.getEntityName(),
                standard.getTablePattern(),
                standard.getTemplateId(),
                codec().readList(standard.getRequiredFieldsJson(), String.class, "必选字段"),
                codec().readList(standard.getOptionalFieldsJson(), String.class, "可选字段"),
                codec().readList(standard.getRelationsJson(), TableRelationHint.class, "业务对象关系"),
                codec().readList(standard.getForeignKeyHintsJson(), TableForeignKeyStandard.class, "外键提示"),
                codec().read(standard.getAuditFieldsJson(), TableAuditPolicy.class, null, "审计字段"),
                codec().readList(standard.getCommonPitfallsJson(), String.class, "常见反模式"),
                standard.getAiUsageNotes(),
                standard.getContextExport(),
                standard.getStatus(),
                standard.getCreatedAt(),
                standard.getUpdatedAt()
        );
    }

    private TableStructureJsonCodec codec() {
        return new TableStructureJsonCodec(objectMapper);
    }

    private String normalizeObjectKey(String value) {
        String normalized = normalizeRequiredText(value, "业务对象键");
        if (!OBJECT_KEY_PATTERN.matcher(normalized).matches()) {
            throw new BizException("业务对象键必须以小写字母开头，并且只包含小写字母、数字、下划线或短横线");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BizException(label + "不能为空");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) {
            return STATUS_ENABLED;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ENABLED", "DISABLED", "DRAFT").contains(normalized)) {
            throw new BizException("业务对象状态只支持 ENABLED、DISABLED 或 DRAFT");
        }
        return normalized;
    }

    private String validatePlainText(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String json = codec().write(normalized, label);
        return codec().read(json, String.class, null, label);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String objectNodeId(String objectKey) {
        return "object:" + objectKey;
    }

    private String templateNodeId(Long templateId) {
        return "template:" + templateId;
    }
}
