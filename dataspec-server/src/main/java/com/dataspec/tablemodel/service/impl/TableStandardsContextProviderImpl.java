package com.dataspec.tablemodel.service.impl;

import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.tablemodel.model.BusinessObjectStandardResp;
import com.dataspec.tablemodel.model.TableRelationSummary;
import com.dataspec.tablemodel.service.TableStructureJsonCodec;
import com.dataspec.tablemodel.service.BusinessObjectStandardService;
import com.dataspec.tablemodel.service.TableStandardsContextProvider;
import com.dataspec.template.entity.Template;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于业务对象标准和表模板生成 AI Context 表结构标准。
 */
@Service
@RequiredArgsConstructor
public class TableStandardsContextProviderImpl implements TableStandardsContextProvider {

    private static final String EXACT_TEMPLATE_ID_PREFIX = "template-id:";
    private static final String EXACT_BUSINESS_OBJECT_PREFIX = "business-object:";

    private final BusinessObjectStandardService businessObjectStandardService;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    public String generateTableStandardsJson(Long projectId) {
        return generateTableStandardsJson(projectId, AiContextScopeOptions.full());
    }

    @Override
    public String generateTableStandardsJson(Long projectId, AiContextScopeOptions options) {
        try {
            List<BusinessObjectStandardResp> objects = businessObjectStandardService.listByProject(projectId);
            List<Template> templates = templateService.listByProject(projectId);
            TableRelationSummary relationSummary = businessObjectStandardService.relationSummary(projectId);
            TableScope tableScope = applyScope(objects, templates, options);
            ObjectNode root = objectMapper.createObjectNode();
            root.put("kind", "dataspec-table-standards");
            root.put("schemaVersion", 1);
            root.put("projectId", projectId);
            root.put("generatedAt", Instant.now().toString());
            ObjectNode contextScope = root.putObject("contextScope");
            contextScope.put("scope", tableScope.scope());
            if (tableScope.query() != null) {
                contextScope.put("query", safe(tableScope.query()));
            }
            contextScope.put("matchedObjectCount", objects.size());
            contextScope.put("returnedObjectCount", tableScope.objects().size());
            contextScope.put("matchedTemplateCount", templates.size());
            contextScope.put("returnedTemplateCount", tableScope.templates().size());
            contextScope.put("truncated", tableScope.truncated());
            ArrayNode warnings = contextScope.putArray("warnings");
            tableScope.warnings().forEach(warning -> warnings.add(safe(warning)));
            root.set("businessObjects", objectMapper.valueToTree(tableScope.objects().stream()
                    .filter(item -> Boolean.TRUE.equals(item.contextExport()))
                    .toList()));
            ArrayNode templateNodes = root.putArray("templates");
            for (Template template : tableScope.templates()) {
                ObjectNode node = templateNodes.addObject();
                node.put("id", template.getId());
                node.put("projectId", template.getProjectId());
                node.put("name", safe(template.getName()));
                node.put("description", safe(template.getDescription()));
                node.put("tablePrefix", safe(template.getTablePrefix()));
                node.put("businessObjectId", template.getBusinessObjectId());
                node.set("structure", templateStructureSummary(template));
            }
            Set<String> allowedNodeIds = tableScope.allowedNodeIds();
            root.set("relations", objectMapper.valueToTree(relationSummary.edges().stream()
                    .filter(edge -> allowedNodeIds.isEmpty()
                            || allowedNodeIds.contains(edge.source())
                            || allowedNodeIds.contains(edge.target()))
                    .toList()));
            ObjectNode summary = root.putObject("summary");
            summary.put("businessObjectCount", objects.size());
            summary.put("exportedBusinessObjectCount", tableScope.objects().stream().filter(item -> Boolean.TRUE.equals(item.contextExport())).count());
            summary.put("templateCount", templates.size());
            summary.put("relationEdgeCount", relationSummary.edges().size());
            ObjectNode safety = root.putObject("safety");
            safety.put("readOnly", true);
            safety.put("writesProject", false);
            safety.put("generatesDdl", false);
            safety.put("connectsDatabase", false);
            safety.putArray("sensitiveInputs");
            ArrayNode nextActions = root.putArray("nextActions");
            nextActions.add("Inspect structure standards before generating DDL.");
            nextActions.add("Do not execute generated DDL without user confirmation.");
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("生成 table-standards.json 失败", e);
        }
    }

    @Override
    public String generateTableStandardsMarkdown(Long projectId) {
        List<BusinessObjectStandardResp> objects = businessObjectStandardService.listByProject(projectId);
        if (objects.isEmpty()) {
            return "";
        }
        StringBuilder md = new StringBuilder();
        md.append("## 表结构标准\n\n");
        for (BusinessObjectStandardResp item : objects) {
            if (!Boolean.TRUE.equals(item.contextExport())) {
                continue;
            }
            md.append("- `").append(safe(item.objectKey())).append("`：")
                    .append(safe(item.entityName()));
            if (item.tablePattern() != null && !item.tablePattern().isBlank()) {
                md.append("；表名模式: ").append(safe(item.tablePattern()));
            }
            if (item.templateId() != null) {
                md.append("；模板: ").append(item.templateId());
            }
            md.append("\n");
            if (item.requiredFields() != null && !item.requiredFields().isEmpty()) {
                md.append("  - 必选字段: ").append(String.join(", ", item.requiredFields().stream().map(this::safe).toList())).append("\n");
            }
            if (item.foreignKeyHints() != null && !item.foreignKeyHints().isEmpty()) {
                md.append("  - 外键提示: ").append(item.foreignKeyHints().size()).append(" 条，只作为 preview/guidance 使用。\n");
            }
            if (item.commonPitfalls() != null && !item.commonPitfalls().isEmpty()) {
                md.append("  - 常见反模式: ").append(String.join("；", item.commonPitfalls().stream().map(this::safe).toList())).append("\n");
            }
        }
        md.append("\n> 表结构标准只用于 DDL preview、lint 和 AI guidance；不要在未获用户确认时执行生成的 DDL。\n\n");
        return md.toString();
    }

    private TableScope applyScope(List<BusinessObjectStandardResp> objects, List<Template> templates, AiContextScopeOptions rawOptions) {
        AiContextScopeOptions options = rawOptions == null ? AiContextScopeOptions.full() : rawOptions;
        String scope = options.scope();
        String query = options.query();
        ScopeQuery scopeQuery = parseScopeQuery(query);
        List<String> warnings = new java.util.ArrayList<>();
        List<BusinessObjectStandardResp> scopedObjects = objects;
        List<Template> scopedTemplates = templates;
        if ("business-object".equals(scope) && query != null) {
            scopedObjects = objects.stream()
                    .filter(item -> scopeQuery.exactBusinessObject()
                            ? matchesExactObject(item, scopeQuery.value())
                            : matchesObject(item, scopeQuery.value()))
                    .toList();
            List<BusinessObjectStandardResp> matchedObjects = scopedObjects;
            Set<Long> templateIds = scopedObjects.stream()
                    .map(BusinessObjectStandardResp::templateId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            scopedTemplates = templates.stream()
                    .filter(template -> (template.getBusinessObjectId() != null && matchedObjects.stream()
                            .anyMatch(item -> template.getBusinessObjectId().equals(item.id())))
                            || templateIds.contains(template.getId()))
                    .toList();
        } else if ("table-template".equals(scope) && query != null) {
            scopedTemplates = templates.stream()
                    .filter(template -> scopeQuery.exactTemplateId()
                            ? matchesExactTemplate(template, scopeQuery.value())
                            : matchesTemplate(template, scopeQuery.value()))
                    .toList();
            Set<Long> templateIds = scopedTemplates.stream()
                    .map(Template::getId)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            Set<Long> objectIds = scopedTemplates.stream()
                    .map(Template::getBusinessObjectId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            scopedObjects = objects.stream()
                    .filter(item -> objectIds.contains(item.id()) || templateIds.contains(item.templateId()))
                    .toList();
        } else if (("business-object".equals(scope) || "table-template".equals(scope)) && query == null) {
            warnings.add("scope=" + scope + " 需要 query 才能裁剪表结构标准，已返回完整表结构上下文。");
        }
        boolean truncated = false;
        if (options.limit() != null && scopedObjects.size() > options.limit()) {
            scopedObjects = scopedObjects.subList(0, options.limit());
            truncated = true;
        }
        if (options.limit() != null && scopedTemplates.size() > options.limit()) {
            scopedTemplates = scopedTemplates.subList(0, options.limit());
            truncated = true;
        }
        Set<String> nodeIds = new java.util.LinkedHashSet<>();
        scopedObjects.forEach(item -> nodeIds.add("object:" + item.objectKey()));
        List<BusinessObjectStandardResp> returnedObjects = scopedObjects;
        scopedTemplates.forEach(template -> {
            nodeIds.add("template:" + template.getId());
            if (template.getBusinessObjectId() != null) {
                returnedObjects.stream()
                        .filter(item -> template.getBusinessObjectId().equals(item.id()))
                        .findFirst()
                        .ifPresent(item -> nodeIds.add("object:" + item.objectKey()));
            }
        });
        if (truncated) {
            warnings.add("表结构标准已按 limit=" + options.limit() + " 截断，请收窄 query 或提高 limit。");
        }
        return new TableScope(scope, scopeQuery.value(), scopedObjects, scopedTemplates, nodeIds, truncated, List.copyOf(warnings));
    }

    private ScopeQuery parseScopeQuery(String query) {
        if (query == null) {
            return new ScopeQuery(null, false, false);
        }
        if (query.startsWith(EXACT_TEMPLATE_ID_PREFIX)) {
            return new ScopeQuery(query.substring(EXACT_TEMPLATE_ID_PREFIX.length()), true, false);
        }
        if (query.startsWith(EXACT_BUSINESS_OBJECT_PREFIX)) {
            return new ScopeQuery(query.substring(EXACT_BUSINESS_OBJECT_PREFIX.length()), false, true);
        }
        return new ScopeQuery(query, false, false);
    }

    private boolean matchesExactObject(BusinessObjectStandardResp item, String query) {
        return equalsIgnoreCase(item.objectKey(), query)
                || equalsIgnoreCase(item.entityName(), query)
                || equalsIgnoreCase(item.tablePattern(), query);
    }

    private boolean matchesObject(BusinessObjectStandardResp item, String query) {
        return contains(item.objectKey(), query)
                || contains(item.entityName(), query)
                || contains(item.tablePattern(), query)
                || contains(item.aiUsageNotes(), query)
                || item.requiredFields().stream().anyMatch(value -> contains(value, query))
                || item.optionalFields().stream().anyMatch(value -> contains(value, query));
    }

    private boolean matchesExactTemplate(Template template, String query) {
        return String.valueOf(template.getId()).equals(query);
    }

    private boolean matchesTemplate(Template template, String query) {
        return contains(String.valueOf(template.getId()), query)
                || contains(template.getName(), query)
                || contains(template.getDescription(), query)
                || contains(template.getTablePrefix(), query)
                || contains(template.getAiUsageNotes(), query);
    }

    private boolean equalsIgnoreCase(String value, String query) {
        return value != null && query != null && value.equalsIgnoreCase(query);
    }

    private boolean contains(String value, String query) {
        return value != null && query != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private ObjectNode templateStructureSummary(Template template) {
        ObjectNode node = objectMapper.createObjectNode();
        copyJson(node, "primaryKey", template.getPrimaryKeyJson());
        copyJson(node, "uniqueKeys", template.getUniqueKeysJson());
        copyJson(node, "indexes", template.getIndexesJson());
        copyJson(node, "foreignKeys", template.getForeignKeysJson());
        copyJson(node, "checkHints", template.getCheckHintsJson());
        copyJson(node, "auditPolicy", template.getAuditPolicyJson());
        copyJson(node, "softDeletePolicy", template.getSoftDeletePolicyJson());
        copyJson(node, "dialectNotes", template.getDialectNotesJson());
        node.put("aiUsageNotes", safe(template.getAiUsageNotes()));
        return node;
    }

    private void copyJson(ObjectNode node, String fieldName, String json) {
        if (json == null || json.isBlank()) {
            node.putNull(fieldName);
            return;
        }
        try {
            node.set(fieldName, objectMapper.readTree(SensitiveDataSanitizer.redactText(json)));
        } catch (Exception ex) {
            node.put(fieldName, "[unreadable]");
        }
    }

    private String safe(String value) {
        return SensitiveDataSanitizer.redactText(value);
    }

    private record TableScope(
            String scope,
            String query,
            List<BusinessObjectStandardResp> objects,
            List<Template> templates,
            Set<String> allowedNodeIds,
            boolean truncated,
            List<String> warnings
    ) {
    }

    private record ScopeQuery(
            String value,
            boolean exactTemplateId,
            boolean exactBusinessObject
    ) {
    }
}
