package com.dataspec.aicontext.service;

import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.aiprofile.model.AiTaskContextScope;
import com.dataspec.aiprofile.model.AiTaskProfile;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.businessglossary.model.BusinessGlossaryContextExport;
import com.dataspec.businessglossary.model.BusinessGlossaryContextItem;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.capability.service.AiCapabilityCatalogService;
import com.dataspec.common.perf.PerformanceProbe;
import com.dataspec.contract.service.SchemaRegistryService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldGroupItem;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingSummaries;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldconflict.model.FieldConflictGroup;
import com.dataspec.fieldconflict.model.FieldConflictReport;
import com.dataspec.fieldconflict.model.FieldConflictType;
import com.dataspec.fieldconflict.service.FieldConflictService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.rulebaseline.model.RuleBaselineInfo;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.dto.StandardSnapshotPayload;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * AI 规则导出服务 —— 生成供 AI 编程工具使用的标准文件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiContextExportService {

    private static final String STARTER_KIT_TAG_PREFIX = "starter:";

    private static final String PACKAGE_FILE_NAME = "dataspec-ai-context.zip";
    private static final int DATASPEC_CONTEXT_SCHEMA_VERSION = 1;
    private static final long FIELD_CATALOG_WARN_MS = 1_000;
    private static final long CONTEXT_PACKAGE_WARN_MS = 1_500;
    private static final int GLOSSARY_CONTEXT_LIMIT = 200;
    private static final int USAGE_EXAMPLE_CONTEXT_LIMIT = 8;
    private static final List<String> DEFAULT_REQUIRED_COLUMNS = List.of("id", "created_at", "updated_at", "is_deleted");
    private static final List<String> DEFAULT_FORBIDDEN_NAMES = List.of(
            "uid", "create_time", "update_time", "del_flag", "ctime", "mtime", "is_del", "tmp", "test", "flag1", "type1"
    );
    private static final Map<String, String> DEFAULT_RECOMMENDATIONS = orderedStringMap(
            "create_time", "created_at",
            "update_time", "updated_at",
            "delete_time", "deleted_at",
            "user_name", "username",
            "pass_word", "password",
            "is_delete", "is_deleted",
            "phone_number", "phone",
            "email_address", "email"
    );
    private static final Map<String, List<String>> DEFAULT_SUFFIX_TYPES = orderedStringListMap(
            Map.entry("_id", List.of("bigint", "integer", "bigserial")),
            Map.entry("_at", List.of("timestamp", "timestamp with time zone", "datetime")),
            Map.entry("_no", List.of("varchar", "char", "text")),
            Map.entry("_count", List.of("integer", "bigint"))
    );
    private static final Map<String, List<String>> DEFAULT_PREFIX_TYPES = orderedStringListMap(
            Map.entry("is_", List.of("boolean"))
    );

    private final RuleConfigService ruleConfigService;
    private final FieldService fieldService;
    private final EnumDictService enumDictService;
    private final StandardSnapshotService standardSnapshotService;
    private final SqlLintService sqlLintService;
    private final ObjectMapper objectMapper;
    private final AiJobRecordService aiJobRecordService;
    private final RuleExemptionService ruleExemptionService;
    private final RuleBaselineService ruleBaselineService;
    private final PromptTemplateRegistry promptTemplateRegistry;
    private final AiTaskProfileService aiTaskProfileService;
    private final SchemaRegistryService schemaRegistryService;
    private final AiCapabilityCatalogService capabilityCatalogService;
    private final BusinessGlossaryService businessGlossaryService;
    private final FieldConflictService fieldConflictService;
    private final StandardUsageExampleService standardUsageExampleService;

    /**
     * 生成 DATABASE_RULES.md —— 给 AI 工具使用的数据库规范文档
     */
    public String generateDatabaseRules(Long projectId) {
        return generateDatabaseRules(projectId, AiContextScopeOptions.full());
    }

    public String generateDatabaseRules(Long projectId, AiContextScopeOptions options) {
        return generateDatabaseRules(projectId, buildScopedFields(projectId, options));
    }

    public String generateDatabaseRules(Long projectId, AiContextScopeOptions options, Long snapshotId, String snapshotVersion) {
        StandardSnapshotPayload snapshotPayload = resolveSnapshotPayload(projectId, snapshotId, snapshotVersion);
        if (snapshotPayload == null) {
            return generateDatabaseRules(projectId, options);
        }
        return generateDatabaseRules(projectId, snapshotPayload);
    }

    private String generateDatabaseRules(Long projectId, ScopedFields scopedFields) {
        StringBuilder md = new StringBuilder();
        md.append("# Database Rules\n\n");
        md.append("<!-- 此文件由 DataSpec 自动生成，供 AI 编程工具参考 -->\n\n");
        appendScopeMarkdown(md, scopedFields.summary());

        // 内置规则说明
        List<Map<String, String>> rules = sqlLintService.listAvailableRules();
        md.append("## 校验规则\n\n");
        for (Map<String, String> rule : rules) {
            md.append(String.format("- **%s** (%s)\n", rule.get("name"), rule.get("code")));
        }
        md.append("\n");

        // 项目自定义规则
        List<RuleConfig> configs = ruleConfigService.listEnabledByProject(projectId);
        if (!configs.isEmpty()) {
            md.append("## 项目规则配置\n\n");
            for (RuleConfig cfg : configs) {
                md.append(String.format("- `%s` [%s] %s\n",
                        cfg.getRuleCode(), cfg.getSeverity(), cfg.getRuleName()));
                if (cfg.getParamsJson() != null && !cfg.getParamsJson().isBlank()) {
                    md.append(String.format("  - 参数: %s\n", cfg.getParamsJson()));
                }
            }
            md.append("\n");
        }
        appendRuleExemptionsMarkdown(md, projectId);
        appendNamingRisksMarkdown(md, projectId, scopedFields.fields().stream().map(FieldMatch::field).toList());

        // 标准字段
        List<FieldMatch> fields = scopedFields.fields();
        if (!fields.isEmpty()) {
            md.append("## 标准字段\n\n");
            for (FieldMatch match : fields) {
                Field f = match.field();
                md.append(String.format("- `%s` %s", f.getName(), f.getDataType()));
                if (f.getComment() != null) {
                    md.append(" — ").append(f.getComment());
                }
                md.append("\n");
                String formatSummary = fieldFormatSummary(f);
                if (formatSummary != null) {
                    md.append("  - 值格式: ").append(formatSummary).append("\n");
                }
            }
            md.append("\n");
        }

        return md.toString();
    }

    private void appendNamingRisksMarkdown(StringBuilder md, Long projectId, List<Field> fields) {
        FieldConflictReport report = fieldConflictService.report(projectId, fields);
        List<FieldConflictGroup> risks = report.getGroups().stream()
                .filter(group -> isNamingRiskType(group.getConflictType()))
                .limit(20)
                .toList();
        if (risks.isEmpty()) {
            return;
        }
        md.append("## 字段命名风险\n\n");
        md.append("AI 生成新 DDL/SQL 时应避让以下高风险字段名或歧义 alias；历史字段必须保留时需要显式确认引用策略。\n\n");
        for (FieldConflictGroup risk : risks) {
            md.append(String.format("- **%s** [%s] %s\n",
                    risk.getTitle(),
                    risk.getConflictType(),
                    risk.getDescription()));
            List<String> fieldNames = risk.getFields().stream()
                    .map(field -> field.getName() == null ? "" : field.getName())
                    .filter(name -> !name.isBlank())
                    .toList();
            if (!fieldNames.isEmpty()) {
                md.append("  - 字段: ").append(String.join(", ", fieldNames)).append("\n");
            }
            if (!risk.getEvidence().isEmpty()) {
                md.append("  - 证据: ").append(String.join("；", risk.getEvidence())).append("\n");
            }
            if (risk.getSuggestedAction() != null && !risk.getSuggestedAction().isBlank()) {
                md.append("  - 建议: ").append(risk.getSuggestedAction()).append("\n");
            }
        }
        md.append("\n");
    }

    private boolean isNamingRiskType(FieldConflictType type) {
        return FieldConflictType.RESERVED_WORD.equals(type)
                || FieldConflictType.DANGEROUS_SQL_NAME.equals(type)
                || FieldConflictType.CASE_COLLISION.equals(type)
                || FieldConflictType.AMBIGUOUS_ALIAS.equals(type);
    }

    /**
     * 生成 field-catalog.json —— 字段目录 JSON
     */
    public String generateFieldCatalogJson(Long projectId) {
        return generateFieldCatalogJson(projectId, AiContextScopeOptions.full());
    }

    public String generateFieldCatalogJson(Long projectId, AiContextScopeOptions options) {
        return PerformanceProbe.measure("ai-context.fieldCatalog", FIELD_CATALOG_WARN_MS,
                "AI Context 字段目录变慢时优先使用 scope/query/status/limit 按需裁剪",
                () -> generateFieldCatalogJson(projectId, currentSnapshot(projectId), buildScopedFields(projectId, options)));
    }

    public String generateFieldCatalogJson(Long projectId, AiContextScopeOptions options, Long snapshotId, String snapshotVersion) {
        StandardSnapshotPayload snapshotPayload = resolveSnapshotPayload(projectId, snapshotId, snapshotVersion);
        if (snapshotPayload == null) {
            return generateFieldCatalogJson(projectId, options);
        }
        return generateFieldCatalogJson(projectId, snapshotPayload);
    }

    private String generateFieldCatalogJson(Long projectId, StandardSnapshotInfo snapshot) {
        return generateFieldCatalogJson(projectId, snapshot, buildScopedFields(projectId, AiContextScopeOptions.full()));
    }

    private String generateFieldCatalogJson(Long projectId, StandardSnapshotInfo snapshot, ScopedFields scopedFields) {
        try {
            ObjectMapper mapper = objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT);

            ObjectNode root = mapper.createObjectNode();
            root.put("projectId", projectId);
            root.set("standard", standardNode(mapper, snapshot));
            BusinessGlossaryContextExport glossaryExport = businessGlossaryService.contextExport(projectId, GLOSSARY_CONTEXT_LIMIT);
            ScopeSummary scopeSummary = scopeSummaryWithGlossaryWarnings(scopedFields.summary(), glossaryExport);
            if (scopeSummary.includeMetadata()) {
                root.set("contextScope", contextScopeNode(mapper, scopeSummary));
            }

            // 字段目录
            ArrayNode fieldsNode = mapper.createArrayNode();
            for (FieldMatch match : scopedFields.fields()) {
                Field f = match.field();
                ObjectNode fn = mapper.createObjectNode();
                fn.put("name", f.getName());
                fn.put("dataType", f.getDataType());
                fn.put("nullable", f.getNullable());
                fn.put("sensitive", Boolean.TRUE.equals(f.getSensitive()));
                fn.put("status", fieldStatusForExport(f.getStatus()));
                if (f.getComment() != null) fn.put("comment", f.getComment());
                if (f.getDefaultValue() != null) fn.put("defaultValue", f.getDefaultValue());
                if (f.getDisplayName() != null) fn.put("displayName", f.getDisplayName());
                if (f.getCategory() != null) fn.put("category", f.getCategory());
                if (f.getTags() != null) fn.put("tags", f.getTags());
                ArrayNode starterKitSourcesNode = starterKitSourcesToArrayNode(mapper, f.getTags());
                if (!starterKitSourcesNode.isEmpty()) fn.set("starterKitSources", starterKitSourcesNode);
                if (f.getCodeSetId() != null) fn.put("codeSetId", f.getCodeSetId());
                if (f.getExampleValue() != null) fn.put("example", f.getExampleValue());
                if (f.getReplacementFieldId() != null) fn.put("replacementFieldId", f.getReplacementFieldId());
                if (f.getReplacementReason() != null) fn.put("replacementReason", f.getReplacementReason());
                ObjectNode formatNode = formatNode(mapper, f);
                if (formatNode.size() > 0) fn.set("format", formatNode);
                ArrayNode aliasesNode = aliasesToArrayNode(mapper, f.getAliases());
                if (!aliasesNode.isEmpty()) fn.set("aliases", aliasesNode);
                if (scopedFields.summary().includeMetadata() && !match.reasons().isEmpty()) {
                    ArrayNode reasonsNode = mapper.createArrayNode();
                    match.reasons().forEach(reasonsNode::add);
                    fn.set("matchReasons", reasonsNode);
                }
                fieldsNode.add(fn);
            }
            root.set("fields", fieldsNode);

            // 枚举目录
            ArrayNode enumsNode = mapper.createArrayNode();
            Set<Long> scopedCodeSetIds = codeSetIds(scopedFields.fields());
            for (EnumDict e : enumDictService.listByProject(projectId)) {
                if (scopedFields.summary().includeMetadata()
                        && (e.getId() == null || !scopedCodeSetIds.contains(e.getId()))) {
                    continue;
                }
                ObjectNode en = mapper.createObjectNode();
                en.put("code", e.getCode());
                en.put("name", e.getName());
                en.put("valueType", e.getValueType());

                ArrayNode valuesNode = mapper.createArrayNode();
                for (EnumValue v : enumDictService.listValues(e.getId())) {
                    ObjectNode vn = mapper.createObjectNode();
                    vn.put("value", v.getValue());
                    vn.put("label", v.getLabel());
                    valuesNode.add(vn);
                }
                en.set("values", valuesNode);
                enumsNode.add(en);
            }
            root.set("enums", enumsNode);
            root.set("glossary", glossaryNode(mapper, glossaryExport));
            UsageExampleExport usageExamples = usageExampleExport(projectId, scopedFields, false);
            root.set("usageExamples", usageExamplesNode(mapper, usageExamples.examples()));
            root.set("usageExampleSummary", usageExampleSummaryNode(mapper, usageExamples.summary()));

            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("生成 field-catalog.json 失败", e);
        }
    }

    private String generateFieldCatalogJson(Long projectId, StandardSnapshotPayload snapshotPayload) {
        try {
            ObjectMapper mapper = objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            ObjectNode root = mapper.createObjectNode();
            root.put("projectId", projectId);
            root.set("standard", standardNode(mapper, snapshotPayload.standard()));

            ArrayNode fieldsNode = mapper.createArrayNode();
            for (JsonNode field : snapshotArray(snapshotPayload, "fields")) {
                ObjectNode fn = mapper.createObjectNode();
                copyText(fn, field, "name", "name");
                copyText(fn, field, "dataType", "dataType");
                copyBoolean(fn, field, "nullable", "nullable");
                copyBoolean(fn, field, "sensitive", "sensitive");
                copyText(fn, field, "status", "status");
                copyText(fn, field, "comment", "comment");
                copyText(fn, field, "defaultValue", "defaultValue");
                copyText(fn, field, "displayName", "displayName");
                copyText(fn, field, "category", "category");
                copyText(fn, field, "tags", "tags");
                ArrayNode starterKitSourcesNode = starterKitSourcesToArrayNode(mapper, field.path("tags").asText(null));
                if (!starterKitSourcesNode.isEmpty()) {
                    fn.set("starterKitSources", starterKitSourcesNode);
                }
                copyLong(fn, field, "codeSetId", "codeSetId");
                copyText(fn, field, "exampleValue", "example");
                copyLong(fn, field, "replacementFieldId", "replacementFieldId");
                copyText(fn, field, "replacementReason", "replacementReason");
                ObjectNode formatNode = formatNode(mapper, field);
                if (formatNode.size() > 0) {
                    fn.set("format", formatNode);
                }
                ArrayNode aliasesNode = aliasesToArrayNode(mapper, field.path("aliases").asText(null));
                if (!aliasesNode.isEmpty()) {
                    fn.set("aliases", aliasesNode);
                }
                fieldsNode.add(fn);
            }
            root.set("fields", fieldsNode);

            ArrayNode enumsNode = mapper.createArrayNode();
            for (JsonNode enumNode : snapshotArray(snapshotPayload, "enums")) {
                ObjectNode en = mapper.createObjectNode();
                copyText(en, enumNode, "code", "code");
                copyText(en, enumNode, "name", "name");
                copyText(en, enumNode, "valueType", "valueType");
                ArrayNode valuesNode = mapper.createArrayNode();
                JsonNode values = enumNode.path("values");
                if (values.isArray()) {
                    for (JsonNode value : values) {
                        ObjectNode vn = mapper.createObjectNode();
                        copyText(vn, value, "value", "value");
                        copyText(vn, value, "label", "label");
                        valuesNode.add(vn);
                    }
                }
                en.set("values", valuesNode);
                enumsNode.add(en);
            }
            root.set("enums", enumsNode);
            UsageExampleExport usageExamples = usageExampleExport(projectId, List.of(), null, ScopeSummary.full(), true);
            root.set("usageExamples", usageExamplesNode(mapper, usageExamples.examples()));
            root.set("usageExampleSummary", usageExampleSummaryNode(mapper, usageExamples.summary()));
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("生成历史 field-catalog.json 失败", e);
        }
    }

    /**
     * 生成 rules.yaml —— 规则配置 YAML
     */
    public String generateRulesYaml(Long projectId) {
        return generateRulesYaml(projectId, currentSnapshot(projectId));
    }

    public String generateRulesYaml(Long projectId, Long snapshotId, String snapshotVersion) {
        StandardSnapshotPayload snapshotPayload = resolveSnapshotPayload(projectId, snapshotId, snapshotVersion);
        if (snapshotPayload == null) {
            return generateRulesYaml(projectId);
        }
        return generateRulesYaml(snapshotPayload);
    }

    private String generateRulesYaml(Long projectId, StandardSnapshotInfo snapshot) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# DataSpec 规则配置\n");
        yaml.append("# 此文件由 DataSpec 自动生成\n\n");
        appendStandardYaml(yaml, snapshot);

        List<RuleConfig> configs = ruleConfigService.listByProject(projectId);
        appendBaselineYaml(yaml, ruleBaselineService.currentBaseline(projectId), configs.size());
        appendStructuredNamingRules(yaml, configs);

        yaml.append("rules:\n");
        for (RuleConfig cfg : configs) {
            yaml.append(String.format("  - code: %s\n", cfg.getRuleCode()));
            yaml.append(String.format("    name: %s\n", cfg.getRuleName()));
            yaml.append(String.format("    severity: %s\n", cfg.getSeverity()));
            yaml.append(String.format("    enabled: %s\n", cfg.getEnabled()));
            if (cfg.getParamsJson() != null && !cfg.getParamsJson().isBlank()) {
                yaml.append(String.format("    params: %s\n", cfg.getParamsJson()));
            }
        }
        appendRuleExemptionsYaml(yaml, projectId);

        return yaml.toString();
    }

    private String generateRulesYaml(StandardSnapshotPayload snapshotPayload) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# DataSpec 规则配置\n");
        yaml.append("# 此文件由 DataSpec 历史标准快照生成\n\n");
        appendStandardYaml(yaml, snapshotPayload.standard());

        List<RuleConfig> configs = snapshotRuleConfigs(snapshotPayload);
        appendStructuredNamingRules(yaml, configs);

        yaml.append("rules:\n");
        for (RuleConfig cfg : configs) {
            yaml.append(String.format("  - code: %s\n", cfg.getRuleCode()));
            yaml.append(String.format("    name: %s\n", cfg.getRuleName()));
            yaml.append(String.format("    severity: %s\n", cfg.getSeverity()));
            yaml.append(String.format("    enabled: %s\n", cfg.getEnabled()));
            if (cfg.getParamsJson() != null && !cfg.getParamsJson().isBlank()) {
                yaml.append(String.format("    params: %s\n", cfg.getParamsJson()));
            }
        }
        return yaml.toString();
    }

    /**
     * 生成建表 Prompt —— 用户复制给 AI 后即可按当前项目标准产出 PostgreSQL DDL。
     */
    public String generateCreateTablePrompt(Long projectId, String businessDescription) {
        String description = businessDescription == null || businessDescription.isBlank()
                ? "请根据后续业务描述设计数据表。"
                : businessDescription.trim();
        StandardSnapshotInfo snapshot = currentSnapshot(projectId);
        String fieldCatalogJson = generateFieldCatalogJson(projectId, snapshot);
        String rulesYaml = generateRulesYaml(projectId, snapshot);
        String databaseRules = generateDatabaseRules(projectId);
        String promptVersion = promptTemplateRegistry.promptVersion(PromptTemplateRegistry.CREATE_TABLE);
        String prompt = """
                # DataSpec 建表 Prompt

                ## Prompt Metadata

                - templateKey: %s
                - promptVersion: %s
                - scenario: CREATE_TABLE
                - outputFormat: POSTGRESQL_DDL_WITH_COMMENTS

                你是数据库设计助手。请严格依据 DataSpec 字段目录和命名规则生成 PostgreSQL DDL。

                ## 业务需求

                %s

                ## 字段目录 field-catalog.json

                ```json
                %s
                ```

                ## 命名规则 rules.yaml

                ```yaml
                %s
                ```

                ## 数据库规则 DATABASE_RULES.md

                ```markdown
                %s
                ```

                ## 输出要求

                - 优先复用 field-catalog.json 中已有标准字段，包含别名、敏感标记、代码集和示例值。
                - 表名、字段名必须遵守 rules.yaml 中的 naming 规则。
                - 输出 PostgreSQL CREATE TABLE，并补全 COMMENT ON TABLE / COMMENT ON COLUMN。
                - 必须包含必含列；新增非标准字段时说明命名理由和建议是否加入标准字段库。
                """.formatted(
                PromptTemplateRegistry.CREATE_TABLE,
                promptVersion,
                description,
                fieldCatalogJson,
                rulesYaml,
                databaseRules
        );
        recordPromptJob(
                projectId,
                "CREATE_TABLE_PROMPT",
                "建表 Prompt",
                description,
                promptVersion,
                orderedMap("businessDescription", description),
                orderedMap("prompt", prompt),
                snapshot
        );
        return prompt;
    }

    /**
     * 生成 SQL 修正 Prompt —— 先跑 lint，再把结构化问题和当前标准一起交给 AI。
     */
    public String generateFixSqlPrompt(Long projectId, String sql) {
        LintResult lintResult = sqlLintService.lint(sql, projectId);
        StandardSnapshotInfo snapshot = currentSnapshot(projectId);
        String issuesJson = writePrettyJson(lintResult.getIssues());
        String fieldCatalogJson = generateFieldCatalogJson(projectId, snapshot);
        String rulesYaml = generateRulesYaml(projectId, snapshot);
        String promptVersion = promptTemplateRegistry.promptVersion(PromptTemplateRegistry.FIX_SQL);
        String prompt = """
                # DataSpec SQL 修正 Prompt

                ## Prompt Metadata

                - templateKey: %s
                - promptVersion: %s
                - scenario: FIX_SQL
                - outputFormat: MARKDOWN_WITH_FIXED_SQL

                你是 DataSpec SQL Review 助手。请根据 lint 问题、字段目录和命名规则修正 SQL。

                ## 原始 SQL

                ```sql
                %s
                ```

                ## Lint 统计

                - errors: %d
                - warnings: %d
                - suggestions: %d

                ## Lint issues

                ```json
                %s
                ```

                ## 字段目录 field-catalog.json

                ```json
                %s
                ```

                ## 命名规则 rules.yaml

                ```yaml
                %s
                ```

                ## 输出要求

                - 先列出每个问题的修正理由。
                - 输出一份修正后的 SQL，包含 CREATE TABLE 和 COMMENT ON 语句。
                - 优先复用标准字段；无法复用时说明新增字段建议。
                """.formatted(
                PromptTemplateRegistry.FIX_SQL,
                promptVersion,
                sql,
                lintResult.getErrorCount(),
                lintResult.getWarningCount(),
                lintResult.getSuggestionCount(),
                issuesJson,
                fieldCatalogJson,
                rulesYaml
        );
        recordPromptJob(
                projectId,
                "FIX_SQL_PROMPT",
                "SQL 修正 Prompt",
                summary(sql),
                promptVersion,
                orderedMap(
                        "sql", sql,
                        "lintSummary", lintSummary(lintResult)
                ),
                orderedMap("prompt", prompt),
                snapshot
        );
        return prompt;
    }

    private void recordPromptJob(
            Long projectId,
            String jobType,
            String title,
            String inputSummary,
            String promptVersion,
            Map<String, Object> inputPayload,
            Map<String, Object> outputPayload,
            StandardSnapshotInfo snapshot
    ) {
        if (projectId == null) {
            return;
        }
        try {
            aiJobRecordService.create(new AiJobRecordCreateReq(
                    projectId,
                    jobType,
                    title,
                    inputSummary,
                    promptVersion,
                    "SUCCESS",
                    inputPayload,
                    outputPayload,
                    snapshot == null ? null : snapshot.snapshotId(),
                    snapshot == null ? null : snapshot.specVersion(),
                    snapshot == null ? null : snapshot.specHash(),
                    null
            ));
        } catch (Exception e) {
            log.warn("保存 AI Prompt 回放记录失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> lintSummary(LintResult lintResult) {
        return orderedMap(
                "errorCount", lintResult.getErrorCount(),
                "warningCount", lintResult.getWarningCount(),
                "suggestionCount", lintResult.getSuggestionCount(),
                "issues", lintResult.getIssues()
        );
    }

    private Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private String summary(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String writePrettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("生成 prompt JSON 片段失败", e);
        }
    }

    private void appendStructuredNamingRules(StringBuilder yaml, List<RuleConfig> configs) {
        Map<String, RuleConfig> configByCode = new LinkedHashMap<>();
        for (RuleConfig config : configs) {
            configByCode.put(config.getRuleCode(), config);
        }

        Map<String, Object> requiredParams = readRuleParams(configByCode.get("required_columns"));
        Map<String, Object> forbiddenParams = readRuleParams(configByCode.get("forbidden_field_name"));
        Map<String, Object> recommendedParams = readRuleParams(configByCode.get("recommended_field_name"));
        Map<String, Object> suffixParams = readRuleParams(configByCode.get("field_suffix_type"));

        yaml.append("naming:\n");
        yaml.append("  table_case: snake_case\n");
        yaml.append("  field_case: snake_case\n");
        appendYamlList(yaml, "  required_columns", stringListParam(requiredParams, "requiredColumns", DEFAULT_REQUIRED_COLUMNS));
        appendYamlList(yaml, "  forbidden_names", stringListParam(forbiddenParams, "forbiddenNames", DEFAULT_FORBIDDEN_NAMES));
        appendYamlStringMap(yaml, "  recommendations", stringMapParam(recommendedParams, "recommendations", DEFAULT_RECOMMENDATIONS));
        appendYamlStringListMap(yaml, "  suffix_types", stringListMapParam(suffixParams, "suffixTypes", DEFAULT_SUFFIX_TYPES));
        appendYamlStringListMap(yaml, "  prefix_types", stringListMapParam(suffixParams, "prefixTypes", DEFAULT_PREFIX_TYPES));
        yaml.append("\n");
    }

    private void appendBaselineYaml(StringBuilder yaml, RuleBaselineInfo baseline, int fallbackRuleCount) {
        yaml.append("baseline:\n");
        if (baseline == null) {
            yaml.append("  key: custom\n");
            yaml.append("  name: 自定义规则\n");
            yaml.append("  version: unversioned\n");
            yaml.append("  source: inferred\n");
            yaml.append(String.format("  rule_count: %d\n\n", fallbackRuleCount));
            return;
        }
        yaml.append(String.format("  key: %s\n", yamlScalar(baseline.key())));
        yaml.append(String.format("  name: %s\n", yamlScalar(baseline.name())));
        yaml.append(String.format("  version: %s\n", yamlScalar(baseline.version())));
        yaml.append(String.format("  source: %s\n", yamlScalar(baseline.source())));
        if (baseline.appliedAt() != null) {
            yaml.append(String.format("  applied_at: %s\n", baseline.appliedAt()));
        }
        yaml.append(String.format("  rule_count: %d\n\n",
                baseline.ruleCount() == null ? fallbackRuleCount : baseline.ruleCount()));
    }

    private Map<String, Object> readRuleParams(RuleConfig config) {
        if (config == null || config.getParamsJson() == null || config.getParamsJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(config.getParamsJson(), new TypeReference<>() {});
        } catch (Exception ignored) {
            // AI 导出不应因单条规则参数格式错误失败；此处回退到内置默认规则。
            return Map.of();
        }
    }

    private List<String> stringListParam(Map<String, Object> params, String key, List<String> defaults) {
        Object value = params.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return defaults;
    }

    private Map<String, String> stringMapParam(Map<String, Object> params, String key, Map<String, String> defaults) {
        Object value = params.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            return result.isEmpty() ? defaults : result;
        }
        return defaults;
    }

    private Map<String, List<String>> stringListMapParam(
            Map<String, Object> params,
            String key,
            Map<String, List<String>> defaults
    ) {
        Object value = params.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, List<String>> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                Object entryValue = entry.getValue();
                if (entryValue instanceof List<?> list) {
                    result.put(entry.getKey().toString(), list.stream().map(Object::toString).toList());
                } else if (entryValue instanceof String text) {
                    result.put(entry.getKey().toString(), List.of(text));
                }
            }
            return result.isEmpty() ? defaults : result;
        }
        return defaults;
    }

    private void appendYamlList(StringBuilder yaml, String key, List<String> values) {
        yaml.append(key).append(":\n");
        for (String value : values) {
            yaml.append("    - ").append(value).append("\n");
        }
    }

    private void appendYamlStringMap(StringBuilder yaml, String key, Map<String, String> values) {
        yaml.append(key).append(":\n");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            yaml.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
    }

    private void appendYamlStringListMap(StringBuilder yaml, String key, Map<String, List<String>> values) {
        yaml.append(key).append(":\n");
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            yaml.append("    ").append(entry.getKey()).append(":\n");
            for (String value : entry.getValue()) {
                yaml.append("      - ").append(value).append("\n");
            }
        }
    }

    private String yamlScalar(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("\r", " ").replace("\n", " ").trim();
    }

    private void appendRuleExemptionsMarkdown(StringBuilder md, Long projectId) {
        List<RuleExemption> exemptions = activeRuleExemptions(projectId);
        if (exemptions.isEmpty()) {
            return;
        }
        md.append("## 项目规则例外\n\n");
        md.append("> 以下例外仅用于兼容历史表、第三方字段或框架约定，不是新建表和新增字段的推荐标准。\n\n");
        for (RuleExemption exemption : exemptions) {
            md.append("- `").append(exemption.getRuleCode()).append("`");
            md.append(" scope: ").append(ruleExemptionScope(exemption));
            md.append("；原因: ").append(exemption.getReason());
            if (exemption.getExpiresAt() != null) {
                md.append("；过期: ").append(exemption.getExpiresAt());
            }
            md.append("\n");
        }
        md.append("\n");
    }

    private void appendRuleExemptionsYaml(StringBuilder yaml, Long projectId) {
        List<RuleExemption> exemptions = activeRuleExemptions(projectId);
        yaml.append("\nrule_exemptions:\n");
        yaml.append("  note: ").append(yamlQuote("These are legacy exceptions, not recommended standards for new schema.")).append("\n");
        yaml.append("  items:\n");
        for (RuleExemption exemption : exemptions) {
            yaml.append("    - ruleCode: ").append(yamlQuote(exemption.getRuleCode())).append("\n");
            yaml.append("      scope: ").append(yamlQuote(ruleExemptionScope(exemption))).append("\n");
            yaml.append("      reason: ").append(yamlQuote(exemption.getReason())).append("\n");
            if (exemption.getTableName() != null) {
                yaml.append("      tableName: ").append(yamlQuote(exemption.getTableName())).append("\n");
            }
            if (exemption.getColumnName() != null) {
                yaml.append("      columnName: ").append(yamlQuote(exemption.getColumnName())).append("\n");
            }
            if (exemption.getExpiresAt() != null) {
                yaml.append("      expiresAt: ").append(yamlQuote(exemption.getExpiresAt().toString())).append("\n");
            }
        }
    }

    private List<RuleExemption> activeRuleExemptions(Long projectId) {
        List<RuleExemption> exemptions = ruleExemptionService.listActiveByProject(projectId);
        return exemptions == null ? List.of() : exemptions;
    }

    private String ruleExemptionScope(RuleExemption exemption) {
        String table = valueOrDash(exemption.getTableName());
        String column = valueOrDash(exemption.getColumnName());
        return "table=" + table + ", column=" + column;
    }

    private String yamlQuote(String value) {
        String safe = value == null ? "" : value.replace("'", "''");
        return "'" + safe + "'";
    }

    /**
     * 生成 AI Context zip 包 —— 可直接复制到业务项目供 AI 编程工具读取
     */
    public byte[] generateAiContextPackage(Long projectId) {
        return generateAiContextPackage(projectId, AiContextScopeOptions.full());
    }

    public byte[] generateAiContextPackage(Long projectId, AiContextScopeOptions options) {
        return PerformanceProbe.measure("ai-context.package", CONTEXT_PACKAGE_WARN_MS,
                "AI Context zip 变慢时优先导出按需包，或检查字段/规则/例外数量",
                () -> {
                    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                         ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                        StandardSnapshotInfo snapshot = currentSnapshot(projectId);
                        ScopedFields scopedFields = buildScopedFields(projectId, options);
                        addTextEntry(zip, ".dataspec/DATABASE_RULES.md", generateDatabaseRules(projectId, scopedFields));
                        addTextEntry(zip, ".dataspec/field-catalog.json", generateFieldCatalogJson(projectId, snapshot, scopedFields));
                        addTextEntry(zip, ".dataspec/field-catalog.schema.json", generateFieldCatalogSchemaJson());
                        addTextEntry(zip, SchemaRegistryService.REGISTRY_FILE, generateSchemaRegistryJson());
                        addTextEntry(zip, ".dataspec/capabilities.json", generateCapabilitiesJson(projectId));
                        addTextEntry(zip, ".dataspec/usage-examples.json", generateUsageExamplesJson(projectId, scopedFields, false));
                        addTextEntry(zip, ".dataspec/manifest.json", generateManifestJson(projectId, snapshot, scopedFields.summary()));
                        addTextEntry(zip, ".dataspec/README.md", generateDataspecReadme(projectId, scopedFields.summary()));
                        addTextEntry(zip, ".dataspec/rules.yaml", generateRulesYaml(projectId, snapshot));
                        addTextEntry(zip, ".dataspec/prompts.md", generatePromptsMarkdown());
                        addTextEntry(zip, ".dataspec/workflows.md", generateWorkflowsMarkdown(projectId));
                        addTextEntry(zip, ".dataspec/examples/good.sql", loadExampleSql(
                                "../examples/good-example.sql",
                                "examples/good-example.sql",
                                defaultGoodExampleSql()
                        ));
                        addTextEntry(zip, ".dataspec/examples/bad.sql", loadExampleSql(
                                "../examples/bad-example.sql",
                                "examples/bad-example.sql",
                                defaultBadExampleSql()
                        ));
                        addTextEntry(zip, "AGENTS.md.fragment", generateAgentsFragment(projectId));
                        zip.finish();
                        return output.toByteArray();
                    } catch (IOException e) {
                        throw new RuntimeException("生成 " + PACKAGE_FILE_NAME + " 失败", e);
                    }
                });
    }

    public byte[] generateAiContextPackage(Long projectId, AiContextScopeOptions options, Long snapshotId, String snapshotVersion) {
        StandardSnapshotPayload snapshotPayload = resolveSnapshotPayload(projectId, snapshotId, snapshotVersion);
        if (snapshotPayload == null) {
            return generateAiContextPackage(projectId, options);
        }
        return PerformanceProbe.measure("ai-context.package", CONTEXT_PACKAGE_WARN_MS,
                "AI Context zip 变慢时优先导出按需包，或检查字段/规则/例外数量",
                () -> {
                    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                         ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                        ScopeSummary scopeSummary = ScopeSummary.full();
                        addTextEntry(zip, ".dataspec/DATABASE_RULES.md", generateDatabaseRules(projectId, snapshotPayload));
                        addTextEntry(zip, ".dataspec/field-catalog.json", generateFieldCatalogJson(projectId, snapshotPayload));
                        addTextEntry(zip, ".dataspec/field-catalog.schema.json", generateFieldCatalogSchemaJson());
                        addTextEntry(zip, SchemaRegistryService.REGISTRY_FILE, generateSchemaRegistryJson());
                        addTextEntry(zip, ".dataspec/capabilities.json", generateCapabilitiesJson(projectId));
                        addTextEntry(zip, ".dataspec/usage-examples.json", generateUsageExamplesJson(projectId, List.of(), null, scopeSummary, true));
                        addTextEntry(zip, ".dataspec/manifest.json", generateManifestJson(projectId, snapshotPayload.standard(), scopeSummary));
                        addTextEntry(zip, ".dataspec/README.md", generateDataspecReadme(projectId, scopeSummary));
                        addTextEntry(zip, ".dataspec/rules.yaml", generateRulesYaml(snapshotPayload));
                        addTextEntry(zip, ".dataspec/prompts.md", generatePromptsMarkdown());
                        addTextEntry(zip, ".dataspec/workflows.md", generateWorkflowsMarkdown(projectId));
                        addTextEntry(zip, ".dataspec/examples/good.sql", loadExampleSql(
                                "../examples/good-example.sql",
                                "examples/good-example.sql",
                                defaultGoodExampleSql()
                        ));
                        addTextEntry(zip, ".dataspec/examples/bad.sql", loadExampleSql(
                                "../examples/bad-example.sql",
                                "examples/bad-example.sql",
                                defaultBadExampleSql()
                        ));
                        addTextEntry(zip, "AGENTS.md.fragment", generateAgentsFragment(projectId));
                        zip.finish();
                        return output.toByteArray();
                    } catch (IOException e) {
                        throw new RuntimeException("生成历史 " + PACKAGE_FILE_NAME + " 失败", e);
                    }
                });
    }

    private String generateManifestJson(Long projectId, StandardSnapshotInfo snapshot) {
        return generateManifestJson(projectId, snapshot, ScopeSummary.full());
    }

    private String generateManifestJson(Long projectId, StandardSnapshotInfo snapshot, ScopeSummary scopeSummary) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schemaVersion", DATASPEC_CONTEXT_SCHEMA_VERSION);
            root.put("kind", "dataspec-ai-context");
            root.put("projectId", projectId);
            root.set("standard", standardNode(objectMapper, snapshot));
            root.put("generatedAt", Instant.now().toString());
            root.set("contracts", objectMapper.valueToTree(schemaRegistryService.manifestSummary()));
            if (scopeSummary.includeMetadata()) {
                root.set("contextScope", contextScopeNode(objectMapper, scopeSummary));
            }

            ArrayNode files = root.putArray("files");
            files.add(".dataspec/manifest.json");
            files.add(".dataspec/README.md");
            files.add(".dataspec/DATABASE_RULES.md");
            files.add(".dataspec/field-catalog.json");
            files.add(".dataspec/field-catalog.schema.json");
            files.add(SchemaRegistryService.REGISTRY_FILE);
            files.add(".dataspec/capabilities.json");
            files.add(".dataspec/usage-examples.json");
            files.add(".dataspec/rules.yaml");
            files.add(".dataspec/prompts.md");
            files.add(".dataspec/workflows.md");
            files.add(".dataspec/examples/good.sql");
            files.add(".dataspec/examples/bad.sql");
            files.add("AGENTS.md.fragment");

            ObjectNode commands = root.putObject("commands");
            commands.put("lint", "dataspec lint <path|-> --project " + projectId + " --format json");
            commands.put("exportContext", exportContextCommand(projectId, scopeSummary));
            commands.put("contractList", "dataspec contract list --project " + projectId + " --format json");
            commands.put("capabilityList", "dataspec capability list --project " + projectId + " --format json");
            commands.put("workflowList", "dataspec workflow list --format json");
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("生成 .dataspec/manifest.json 失败", e);
        }
    }

    private String generateDataspecReadme(Long projectId) {
        return generateDataspecReadme(projectId, ScopeSummary.full());
    }

    private String generateSchemaRegistryJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaRegistryService.getCatalog());
        } catch (Exception e) {
            throw new RuntimeException("生成 " + SchemaRegistryService.REGISTRY_FILE + " 失败", e);
        }
    }

    private String generateCapabilitiesJson(Long projectId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(capabilityCatalogService.getCatalog(projectId));
        } catch (Exception e) {
            throw new RuntimeException("生成 .dataspec/capabilities.json 失败", e);
        }
    }

    private String generateUsageExamplesJson(Long projectId, ScopedFields scopedFields, boolean snapshotExport) {
        return generateUsageExamplesJson(
                projectId,
                usageExampleFieldIds(scopedFields),
                usageExampleQuery(scopedFields),
                scopedFields.summary(),
                snapshotExport);
    }

    private String generateUsageExamplesJson(Long projectId,
                                             List<Long> fieldIds,
                                             String query,
                                             ScopeSummary scopeSummary,
                                             boolean snapshotExport) {
        try {
            ObjectMapper mapper = objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            UsageExampleExport export = usageExampleExport(projectId, fieldIds, query, scopeSummary, snapshotExport);
            ObjectNode root = mapper.createObjectNode();
            root.put("schemaVersion", DATASPEC_CONTEXT_SCHEMA_VERSION);
            root.put("projectId", projectId);
            root.put("generatedAt", Instant.now().toString());
            root.put("snapshotBound", false);
            root.put("source", snapshotExport ? "current_project_metadata_for_snapshot_context" : "current_project_metadata");
            root.set("contextScope", contextScopeNode(mapper, scopeSummary));
            root.set("summary", usageExampleSummaryNode(mapper, export.summary()));
            root.set("examples", usageExamplesNode(mapper, export.examples()));
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("生成 .dataspec/usage-examples.json 失败", e);
        }
    }

    private UsageExampleExport usageExampleExport(Long projectId, ScopedFields scopedFields, boolean snapshotExport) {
        return usageExampleExport(
                projectId,
                usageExampleFieldIds(scopedFields),
                usageExampleQuery(scopedFields),
                scopedFields.summary(),
                snapshotExport);
    }

    private UsageExampleExport usageExampleExport(Long projectId,
                                                  List<Long> fieldIds,
                                                  String query,
                                                  ScopeSummary scopeSummary,
                                                  boolean snapshotExport) {
        List<StandardUsageExample> selectedExamples = standardUsageExampleService == null
                ? List.of()
                : standardUsageExampleService.selectForAiContext(projectId, fieldIds, query, USAGE_EXAMPLE_CONTEXT_LIMIT + 1);
        boolean truncated = selectedExamples.size() > USAGE_EXAMPLE_CONTEXT_LIMIT;
        List<StandardUsageExample> examples = truncated
                ? selectedExamples.subList(0, USAGE_EXAMPLE_CONTEXT_LIMIT)
                : selectedExamples;
        int good = 0;
        int bad = 0;
        for (StandardUsageExample example : examples) {
            if ("GOOD".equalsIgnoreCase(example.getExampleType())) {
                good++;
            }
            if ("BAD".equalsIgnoreCase(example.getExampleType())) {
                bad++;
            }
        }
        UsageExampleSummary summary = new UsageExampleSummary(
                examples.size(),
                good,
                bad,
                USAGE_EXAMPLE_CONTEXT_LIMIT,
                truncated,
                snapshotExport ? "current_project_metadata_for_snapshot_context" : "current_project_metadata",
                scopeSummary.includeMetadata() ? scopeSummary.scope() : "all"
        );
        return new UsageExampleExport(List.copyOf(examples), summary);
    }

    private List<Long> usageExampleFieldIds(ScopedFields scopedFields) {
        if (!scopedFields.summary().includeMetadata()) {
            return List.of();
        }
        return scopedFields.fields().stream()
                .map(FieldMatch::field)
                .map(Field::getId)
                .filter(id -> id != null)
                .toList();
    }

    private String usageExampleQuery(ScopedFields scopedFields) {
        return scopedFields.summary().includeMetadata() ? scopedFields.summary().query() : null;
    }

    private ArrayNode usageExamplesNode(ObjectMapper mapper, List<StandardUsageExample> examples) {
        ArrayNode node = mapper.createArrayNode();
        for (StandardUsageExample example : examples) {
            ObjectNode item = mapper.createObjectNode();
            if (example.getId() != null) {
                item.put("id", example.getId());
            }
            item.put("scope", text(example.getScope()));
            item.put("exampleType", text(example.getExampleType()));
            if (example.getFieldId() != null) {
                item.put("fieldId", example.getFieldId());
            }
            putSanitizedText(item, "ruleCode", example.getRuleCode());
            if (example.getTemplateId() != null) {
                item.put("templateId", example.getTemplateId());
            }
            putSanitizedText(item, "input", example.getInput());
            putSanitizedText(item, "expectedOutput", example.getExpectedOutput());
            putSanitizedText(item, "antiPattern", example.getAntiPattern());
            putSanitizedText(item, "reason", example.getReason());
            item.set("tags", stringsToArrayNode(mapper, splitCsv(example.getTags())));
            if (example.getPriority() != null) {
                item.put("priority", example.getPriority());
            }
            putSanitizedText(item, "status", example.getStatus());
            node.add(item);
        }
        return node;
    }

    private ObjectNode usageExampleSummaryNode(ObjectMapper mapper, UsageExampleSummary summary) {
        ObjectNode node = mapper.createObjectNode();
        node.put("totalExamples", summary.totalExamples());
        node.put("goodExamples", summary.goodExamples());
        node.put("badExamples", summary.badExamples());
        node.put("limit", summary.limit());
        node.put("truncated", summary.truncated());
        node.put("source", summary.source());
        node.put("scope", summary.scope());
        return node;
    }

    private void putSanitizedText(ObjectNode node, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            node.put(fieldName, SensitiveDataSanitizer.redactText(value));
        }
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String generateDataspecReadme(Long projectId, ScopeSummary scopeSummary) {
        String scopeText = scopeSummary.includeMetadata()
                ? """

                ## 当前包裁剪条件

                - scope: %s
                - query: %s
                - status: %s
                - limit: %s
                - fields: %d / %d returned, %d matched before limit

                这是按需包，适合当前建表、修 SQL 或字段设计任务。若任务涉及跨域模型、全库字段梳理或标准治理，请重新导出完整包。
                """.formatted(
                        scopeSummary.scope(),
                        valueOrDash(scopeSummary.query()),
                        valueOrDash(scopeSummary.status()),
                        scopeSummary.limit() == null ? "-" : scopeSummary.limit(),
                        scopeSummary.returnedFieldCount(),
                        scopeSummary.totalFieldCount(),
                        scopeSummary.matchedFieldCount()
                )
                : """

                ## 完整包与按需包

                - 完整包适合初始化业务仓库、全局建模和标准盘点。
                - 按需包适合单个建表、修 SQL、字段命名或当前需求上下文，能减少 AI 读取无关字段。
                - 可通过 `dataspec export-context --scope field --query <关键词> --output dataspec-ai-context.zip` 导出按需包。
                """;
        return """
                # .dataspec

                本目录由 DataSpec 导出，供 AI 编程工具和开发者在业务项目中读取数据库字段标准。
                %s

                ## 文件约定

                - `.dataspec/manifest.json`：上下文包元数据，包含 schemaVersion、projectId、生成时间、文件清单和推荐命令。
                - `.dataspec/manifest.json` 中的 `standard`：当前标准快照版本和 hash；`unversioned` 表示尚未创建快照。
                - `.dataspec/DATABASE_RULES.md`：数据库命名、类型、注释和公共字段规则。
                - `.dataspec/field-catalog.json`：标准字段目录，包含字段名、类型、别名、敏感标记、状态、代码集和示例值。
                - `.dataspec/field-catalog.schema.json`：字段目录 JSON Schema。
                - `.dataspec/schema-registry.json`：AI 可消费输出契约 registry，包含契约版本、稳定字段、JSON Schema 和兼容策略。
                - `.dataspec/capabilities.json`：DataSpec 面向 AI 的能力清单，说明 API/CLI/MCP/前端入口、前置检查、writeRisk 和下一步建议。
                - `.dataspec/usage-examples.json`：标准字段、规则和模板的结构化正例/反例，AI 应优先模仿 `GOOD`，避开匹配 scope 的 `BAD`。
                - `.dataspec/rules.yaml`：结构化命名规则和项目规则配置。
                - `.dataspec/prompts.md`：建表和 SQL Review 的 AI prompt 模板。
                - `.dataspec/workflows.md`：常见 AI/DataSpec 任务 recipe，说明输入、步骤、命令、产物和失败恢复。
                - `.dataspec/examples/good.sql`：符合标准的 SQL 示例。
                - `.dataspec/examples/bad.sql`：不符合标准的 SQL 反例。
                - `AGENTS.md.fragment`：可复制到业务项目 `AGENTS.md` 的 DataSpec 指令片段。

                ## 使用约定

                - 创建或修改 SQL、migration、ORM entity 前，先读取 `.dataspec/manifest.json` 和 `.dataspec/capabilities.json`，再读取字段目录、schema registry 和规则文件。
                - 需要稳定字段名或兼容策略时，读取 `.dataspec/schema-registry.json`，不要依赖未列入 stableFields 的内部字段。
                - 需要选择 API、CLI 或 MCP 入口时，优先读取 `.dataspec/capabilities.json` 的 writeRisk、preflightChecks 和 nextActions。
                - 不确定任务步骤时，先读取 `.dataspec/workflows.md`，选择合适 recipe 后再显式执行其中的命令。
                - 生成字段、DDL 或 prompt 前读取 `.dataspec/usage-examples.json`，优先模仿 `GOOD` 示例，避免复用 `BAD` 反例中的 antiPattern。
                - 检查 SQL 时运行：

                ```bash
                dataspec lint <path|-> --project %d --format json
                ```

                - 如果使用当前仓库内的 Node CLI，可将命令替换为：

                ```bash
                node tools/dataspec-cli.mjs lint <path|-> --project %d --format json
                ```

                ## 更新约定

                当 DataSpec 中的字段、规则、枚举或 prompt 更新后，重新下载 AI Context 包，并整体替换业务项目中的 `.dataspec/` 目录和 `AGENTS.md.fragment`。
                """.formatted(scopeText, projectId, projectId);
    }

    private String generateWorkflowsMarkdown(Long projectId) {
        return """
                # DataSpec Workflow Recipes

                这些 recipe 是给 AI agent 和开发者读取的任务计划，只说明推荐步骤和命令，不会自动执行工作流，也不会调用外部 LLM。

                ## create-table：新增建表 SQL

                - 目标：为新业务表先读取 DataSpec 标准，再生成或整理符合规则的 `CREATE TABLE` SQL。
                - 输入：projectId=%d、业务描述、可选 tableName、可选 templateId。
                - 前置检查：`dataspec doctor --project %d --format json`。
                - 步骤：
                  1. `dataspec export-context --project %d --scope field --query "<业务描述>" --output dataspec-ai-context.zip`
                  2. `dataspec suggest-field "<业务描述>" --project %d --format json`
                  3. `dataspec generate-ddl --project %d --template <templateId> --table <tableName> --format json`
                  4. `dataspec lint <sql-file|-> --project %d --format json`
                - 产物：最终 SQL、lint JSON、使用的标准版本和字段推荐摘要。
                - 失败恢复：doctor 失败先修服务/token/projectId；字段无命中时先标记候选；lint 有 ERROR 时先修复再交付。

                ## review-pr-sql：PR SQL Review

                - 目标：在 Pull Request 中扫描 SQL/DDL 变更，输出 DataSpec 校验结果。
                - 输入：projectId=%d、paths、repo、pr、GITHUB_TOKEN。
                - 前置检查：`dataspec doctor --project %d --format json`。
                - 步骤：
                  1. `dataspec lint-files <paths...> --project %d --format json`
                  2. `dataspec review-pr <paths...> --project %d --repo <owner/name> --pr <number> --token "$GITHUB_TOKEN"`
                  3. 修复后重新运行 `dataspec lint-files <paths...> --project %d --format json`
                - 产物：PR Review 评论、最终 lint-files JSON、未修复建议说明。
                - 失败恢复：GitHub 401/403 检查 token 和 repo/pr；未扫描到 SQL 时检查 paths 或 defaultPaths。

                ## reverse-import-standards：数据库反向导入补标准

                - 目标：从已有数据库只读抽取 metadata，生成字段标准候选并由用户确认导入。
                - 输入：projectId=%d、databaseType、host、port、databaseName、schemaName、只读账号和表范围。
                - 前置检查：`dataspec doctor --project %d --format json`；确认数据库账号只读。
                - 步骤：
                  1. `POST /api/reverse-import/database/test`
                  2. `POST /api/reverse-import/database/tables`
                  3. `POST /api/reverse-import/database/preview`
                  4. `POST /api/reverse-import/database/import`
                - 产物：反向导入预览、确认导入字段、导入批次和字段来源。
                - 失败恢复：连接失败先修 schema/权限；候选过多时按表分批；字段冲突时先人工确认别名。
                - 安全边界：不保存 password、token 或完整连接串，不修改源数据库。

                ## export-min-context：导出最小 AI Context

                - 目标：为当前建表、修 SQL 或字段设计任务导出尽量小但可复现的上下文包。
                - 输入：projectId=%d、scope、query、limit。
                - 前置检查：`dataspec doctor --project %d --format json`。
                - 步骤：
                  1. `dataspec export-context --project %d --scope <scope> --query "<query>" --limit <limit> --output dataspec-ai-context.zip`
                  2. 读取 `.dataspec/manifest.json` 和 `.dataspec/README.md`
                  3. 读取 `.dataspec/workflows.md` 和 `.dataspec/field-catalog.json`
                - 产物：dataspec-ai-context.zip、contextScope 摘要、standard.specVersion/specHash。
                - 失败恢复：字段过少时扩大 scope/query；包过大时收窄 scope/query/status；unversioned 需要在交付中说明。
                """.formatted(
                projectId, projectId, projectId, projectId, projectId, projectId,
                projectId, projectId, projectId, projectId, projectId,
                projectId, projectId,
                projectId, projectId, projectId
        );
    }

    private void addTextEntry(ZipOutputStream zip, String entryName, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private ScopedFields buildScopedFields(Long projectId, AiContextScopeOptions rawOptions) {
        List<String> warnings = new ArrayList<>();
        AiContextScopeOptions options = resolveProfileScopeOptions(rawOptions, warnings);
        List<Field> allFields = fieldService.listByProject(projectId);
        String effectiveScope = options.scopeSupported() ? options.scope() : "all";
        if (!options.scopeSupported()) {
            warnings.add("未知 scope=" + options.scope() + "，已按完整字段文本匹配处理。");
        }
        if ("changed".equals(effectiveScope)) {
            warnings.add("changed 第一版基于 query 做任务相关裁剪，尚未启用快照 diff。");
        }
        if (!"all".equals(effectiveScope) && options.query() == null && options.status() == null) {
            warnings.add("scope=" + effectiveScope + " 需要 query 或 status 才能确定裁剪范围。");
        }

        List<FieldMatch> matchedFields = new ArrayList<>();
        for (Field field : allFields) {
            List<String> reasons = new ArrayList<>();
            if (!matchesStatus(field, options.status())) {
                continue;
            }
            if (options.status() != null) {
                reasons.add("状态匹配: " + fieldStatusForExport(field.getStatus()));
            }

            boolean includeByText = includeByTextScope(field, effectiveScope, options.query(), reasons);
            boolean includeByStatusOnly = options.query() == null && options.status() != null;
            boolean includeFull = "all".equals(effectiveScope) && options.query() == null;
            if (includeFull || includeByText || includeByStatusOnly) {
                matchedFields.add(new FieldMatch(field, List.copyOf(reasons)));
            }
        }

        int matchedCount = matchedFields.size();
        List<FieldMatch> returnedFields = matchedFields;
        if (options.limit() != null && matchedFields.size() > options.limit()) {
            returnedFields = matchedFields.subList(0, options.limit());
            warnings.add("命中字段已按 limit=" + options.limit() + " 截断，请缩小 query 或提高 limit。");
        }

        FieldGroupSummary groupSummary = FieldGroupingSummaries.fromFields(
                projectId,
                returnedFields.stream().map(FieldMatch::field).toList());
        if (options.scoped() && groupSummary.ungroupedFieldCount() > 0) {
            warnings.add("返回字段中有 " + groupSummary.ungroupedFieldCount() + " 个未分组字段，请按数据域、分类或标签补齐。");
        }

        ScopeSummary summary = new ScopeSummary(
                options.scoped() || !warnings.isEmpty(),
                effectiveScope,
                options.query(),
                options.status(),
                options.limit(),
                options.profileId(),
                options.taskType(),
                allFields.size(),
                matchedCount,
                returnedFields.size(),
                List.copyOf(warnings),
                groupSummary
        );
        return new ScopedFields(List.copyOf(returnedFields), summary);
    }

    private AiContextScopeOptions resolveProfileScopeOptions(AiContextScopeOptions rawOptions, List<String> warnings) {
        AiContextScopeOptions options = rawOptions == null ? AiContextScopeOptions.full() : rawOptions;
        String requestedProfile = options.profileId() != null ? options.profileId() : options.taskType();
        if (requestedProfile == null) {
            return options;
        }
        if (aiTaskProfileService == null) {
            warnings.add("已请求 AI profile=" + requestedProfile + "，但当前服务未启用 profile registry，已按显式 scope 参数处理。");
            return options;
        }
        return aiTaskProfileService.findProfile(requestedProfile)
                .map(profile -> mergeProfileScope(options, profile))
                .orElseGet(() -> {
                    warnings.add("未知 AI profile 或 taskType=" + requestedProfile + "，已按显式 scope 参数处理。");
                    return options;
                });
    }

    private AiContextScopeOptions mergeProfileScope(AiContextScopeOptions options, AiTaskProfile profile) {
        AiTaskContextScope defaults = profile.getContextScope();
        if (defaults == null) {
            return options;
        }
        // profile 只是默认建议，显式 scope/query/status/limit 仍保持优先。
        String effectiveScope = options.scopeExplicit() ? options.scope() : firstText(defaults.getScope(), options.scope());
        String effectiveQuery = options.query() != null ? options.query() : defaults.getQuery();
        String effectiveStatus = options.status() != null ? options.status() : defaults.getStatus();
        Integer effectiveLimit = options.limit() != null ? options.limit() : defaults.getLimit();
        String profileId = options.profileId() != null ? options.profileId() : profile.getProfileId();
        String taskType = options.taskType() != null ? options.taskType() : profile.getTaskType();
        return new AiContextScopeOptions(
                effectiveScope,
                effectiveQuery,
                effectiveStatus,
                effectiveLimit,
                profileId,
                taskType,
                options.scopeExplicit());
    }

    private String firstText(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private boolean includeByTextScope(Field field, String scope, String query, List<String> reasons) {
        if (query == null) {
            return false;
        }
        return switch (scope) {
            case "domain" -> collectDomainReasons(field, query, reasons);
            case "tag" -> collectTagReasons(field, query, reasons);
            case "table", "changed", "field", "all" -> collectGeneralTextReasons(field, query, reasons);
            default -> collectGeneralTextReasons(field, query, reasons);
        };
    }

    private boolean collectGeneralTextReasons(Field field, String query, List<String> reasons) {
        addReasonIfContains(reasons, "字段名匹配", field.getName(), query);
        addReasonIfContains(reasons, "显示名匹配", field.getDisplayName(), query);
        addReasonIfContains(reasons, "别名匹配", field.getAliases(), query);
        addReasonIfContains(reasons, "注释匹配", field.getComment(), query);
        addReasonIfContains(reasons, "分类匹配", field.getCategory(), query);
        addReasonIfContains(reasons, "标签匹配", field.getTags(), query);
        addReasonIfContains(reasons, "类型匹配", field.getDataType(), query);
        addReasonIfContains(reasons, "示例匹配", field.getExampleValue(), query);
        addReasonIfContains(reasons, "状态匹配", fieldStatusForExport(field.getStatus()), query);
        return hasTextReason(reasons);
    }

    private boolean collectDomainReasons(Field field, String query, List<String> reasons) {
        addReasonIfContains(reasons, "数据域匹配", field.getCategory(), query);
        if (field.getDomainId() != null && containsIgnoreCase(String.valueOf(field.getDomainId()), query)) {
            reasons.add("数据域ID匹配: " + field.getDomainId());
        }
        addReasonIfContains(reasons, "标签匹配", field.getTags(), query);
        addReasonIfContains(reasons, "显示名匹配", field.getDisplayName(), query);
        addReasonIfContains(reasons, "注释匹配", field.getComment(), query);
        return hasTextReason(reasons);
    }

    private boolean collectTagReasons(Field field, String query, List<String> reasons) {
        addReasonIfContains(reasons, "标签匹配", field.getTags(), query);
        addReasonIfContains(reasons, "别名匹配", field.getAliases(), query);
        addReasonIfContains(reasons, "显示名匹配", field.getDisplayName(), query);
        addReasonIfContains(reasons, "分类匹配", field.getCategory(), query);
        addReasonIfContains(reasons, "注释匹配", field.getComment(), query);
        return hasTextReason(reasons);
    }

    private boolean hasTextReason(List<String> reasons) {
        return reasons.stream().anyMatch(reason -> !reason.startsWith("状态匹配:"));
    }

    private void addReasonIfContains(List<String> reasons, String label, String value, String query) {
        if (containsIgnoreCase(value, query)) {
            reasons.add(label + ": " + value);
        }
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (value == null || query == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean matchesStatus(Field field, String status) {
        return status == null || fieldStatusForExport(field.getStatus()).equalsIgnoreCase(status);
    }

    private Set<Long> codeSetIds(List<FieldMatch> fields) {
        Set<Long> ids = new LinkedHashSet<>();
        for (FieldMatch match : fields) {
            Long codeSetId = match.field().getCodeSetId();
            if (codeSetId != null) {
                ids.add(codeSetId);
            }
        }
        return ids;
    }

    private void appendScopeMarkdown(StringBuilder md, ScopeSummary scopeSummary) {
        if (!scopeSummary.includeMetadata()) {
            return;
        }
        md.append("## 上下文裁剪\n\n");
        md.append("- scope: ").append(scopeSummary.scope()).append("\n");
        if (scopeSummary.profileId() != null) {
            md.append("- profileId: ").append(scopeSummary.profileId()).append("\n");
        }
        if (scopeSummary.taskType() != null) {
            md.append("- taskType: ").append(scopeSummary.taskType()).append("\n");
        }
        md.append("- query: ").append(valueOrDash(scopeSummary.query())).append("\n");
        md.append("- status: ").append(valueOrDash(scopeSummary.status())).append("\n");
        md.append("- fields: ")
                .append(scopeSummary.returnedFieldCount())
                .append(" returned / ")
                .append(scopeSummary.matchedFieldCount())
                .append(" matched / ")
                .append(scopeSummary.totalFieldCount())
                .append(" total\n");
        for (String warning : scopeSummary.warnings()) {
            md.append("- warning: ").append(warning).append("\n");
        }
        if (scopeSummary.groupSummary() != null) {
            md.append("- ungroupedFields: ").append(scopeSummary.groupSummary().ungroupedFieldCount()).append("\n");
        }
        md.append("\n");
    }

    private ObjectNode contextScopeNode(ObjectMapper mapper, ScopeSummary summary) {
        ObjectNode node = mapper.createObjectNode();
        node.put("scope", summary.scope());
        if (summary.query() != null) {
            node.put("query", summary.query());
        }
        if (summary.status() != null) {
            node.put("status", summary.status());
        }
        if (summary.limit() != null) {
            node.put("limit", summary.limit());
        }
        if (summary.profileId() != null) {
            node.put("profileId", summary.profileId());
        }
        if (summary.taskType() != null) {
            node.put("taskType", summary.taskType());
        }
        node.put("totalFieldCount", summary.totalFieldCount());
        node.put("matchedFieldCount", summary.matchedFieldCount());
        node.put("returnedFieldCount", summary.returnedFieldCount());
        if (summary.groupSummary() != null) {
            node.set("groupSummary", groupSummaryNode(mapper, summary.groupSummary()));
        }
        ArrayNode warningsNode = mapper.createArrayNode();
        summary.warnings().forEach(warningsNode::add);
        node.set("warnings", warningsNode);
        return node;
    }

    private ScopeSummary scopeSummaryWithGlossaryWarnings(ScopeSummary summary, BusinessGlossaryContextExport glossaryExport) {
        if (!glossaryExport.truncated()) {
            return summary;
        }
        List<String> warnings = new ArrayList<>(summary.warnings());
        warnings.add("业务术语表已按 " + glossaryExport.returnedCount() + "/" + glossaryExport.totalCount() + " 条截断，请收窄项目术语或按需导出。");
        return new ScopeSummary(
                true,
                summary.scope(),
                summary.query(),
                summary.status(),
                summary.limit(),
                summary.profileId(),
                summary.taskType(),
                summary.totalFieldCount(),
                summary.matchedFieldCount(),
                summary.returnedFieldCount(),
                List.copyOf(warnings),
                summary.groupSummary());
    }

    private ArrayNode glossaryNode(ObjectMapper mapper, BusinessGlossaryContextExport glossaryExport) {
        ArrayNode node = mapper.createArrayNode();
        for (BusinessGlossaryContextItem item : glossaryExport.items()) {
            ObjectNode entry = mapper.createObjectNode();
            entry.put("term", item.term());
            entry.set("synonyms", stringsToArrayNode(mapper, item.synonyms()));
            entry.set("rootTerms", stringsToArrayNode(mapper, item.rootTerms()));
            entry.set("abbreviations", stringsToArrayNode(mapper, item.abbreviations()));
            entry.set("disabledTerms", stringsToArrayNode(mapper, item.disabledTerms()));
            if (item.canonicalFieldName() != null) {
                entry.put("canonicalFieldName", item.canonicalFieldName());
            }
            if (item.scopeType() != null) {
                entry.put("scopeType", item.scopeType());
            }
            if (item.scopeValue() != null) {
                entry.put("scopeValue", item.scopeValue());
            }
            entry.set("exampleFields", stringsToArrayNode(mapper, item.exampleFields()));
            node.add(entry);
        }
        return node;
    }

    private ArrayNode stringsToArrayNode(ObjectMapper mapper, List<String> values) {
        ArrayNode node = mapper.createArrayNode();
        if (values != null) {
            values.forEach(node::add);
        }
        return node;
    }

    private ObjectNode groupSummaryNode(ObjectMapper mapper, FieldGroupSummary summary) {
        ObjectNode node = mapper.createObjectNode();
        node.put("totalFieldCount", summary.totalFieldCount());
        node.put("ungroupedFieldCount", summary.ungroupedFieldCount());
        ArrayNode groupsNode = mapper.createArrayNode();
        for (FieldGroupItem group : summary.groups()) {
            ObjectNode groupNode = mapper.createObjectNode();
            groupNode.put("groupType", group.groupType());
            groupNode.put("groupKey", group.groupKey());
            groupNode.put("groupName", group.groupName());
            groupNode.put("fieldCount", group.fieldCount());
            groupNode.put("ungrouped", group.ungrouped());
            ArrayNode sampleFieldsNode = mapper.createArrayNode();
            group.sampleFields().forEach(sampleFieldsNode::add);
            groupNode.set("sampleFields", sampleFieldsNode);
            groupsNode.add(groupNode);
        }
        node.set("groups", groupsNode);
        return node;
    }

    private String exportContextCommand(Long projectId, ScopeSummary scopeSummary) {
        StringBuilder command = new StringBuilder("dataspec export-context --project ")
                .append(projectId)
                .append(" --output dataspec-ai-context.zip");
        if (scopeSummary.includeMetadata()) {
            if (scopeSummary.profileId() != null) {
                command.append(" --profile ").append(scopeSummary.profileId());
            } else if (scopeSummary.taskType() != null) {
                command.append(" --task-type ").append(scopeSummary.taskType());
            }
            command.append(" --scope ").append(scopeSummary.scope());
            if (scopeSummary.query() != null) {
                command.append(" --query \"").append(scopeSummary.query()).append("\"");
            }
            if (scopeSummary.status() != null) {
                command.append(" --status ").append(scopeSummary.status());
            }
            if (scopeSummary.limit() != null) {
                command.append(" --limit ").append(scopeSummary.limit());
            }
        }
        return command.toString();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String generateFieldCatalogSchemaJson() {
        return """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "title": "DataSpec Field Catalog",
                  "type": "object",
                  "additionalProperties": false,
                  "required": ["projectId", "fields", "enums"],
                    "properties": {
                    "projectId": { "type": "integer" },
                    "standard": {
                      "type": "object",
                      "additionalProperties": false,
                      "required": ["specVersion", "versioned"],
                      "properties": {
                        "snapshotId": { "type": "integer" },
                        "specVersion": { "type": "string" },
                        "specHash": { "type": "string" },
                        "name": { "type": "string" },
                        "versioned": { "type": "boolean" },
                        "source": { "type": "string", "enum": ["current", "snapshot", "unversioned"] }
                      }
                    },
                    "contextScope": {
                      "type": "object",
                      "additionalProperties": false,
                      "required": ["scope", "totalFieldCount", "matchedFieldCount", "returnedFieldCount", "warnings"],
                      "properties": {
                        "scope": { "type": "string", "enum": ["all", "field", "domain", "tag", "table", "changed"] },
                        "query": { "type": "string" },
                        "status": { "type": "string" },
                        "limit": { "type": "integer" },
                        "profileId": { "type": "string" },
                        "taskType": { "type": "string" },
                        "totalFieldCount": { "type": "integer" },
                        "matchedFieldCount": { "type": "integer" },
                        "returnedFieldCount": { "type": "integer" },
                        "warnings": {
                          "type": "array",
                          "items": { "type": "string" }
                        },
                        "groupSummary": {
                          "type": "object",
                          "additionalProperties": false,
                          "required": ["totalFieldCount", "ungroupedFieldCount", "groups"],
                          "properties": {
                            "totalFieldCount": { "type": "integer" },
                            "ungroupedFieldCount": { "type": "integer" },
                            "groups": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "additionalProperties": false,
                                "required": ["groupType", "groupKey", "groupName", "fieldCount", "sampleFields", "ungrouped"],
                                "properties": {
                                  "groupType": { "type": "string", "enum": ["category", "domain", "tag", "ungrouped"] },
                                  "groupKey": { "type": "string" },
                                  "groupName": { "type": "string" },
                                  "fieldCount": { "type": "integer" },
                                  "sampleFields": {
                                    "type": "array",
                                    "items": { "type": "string" }
                                  },
                                  "ungrouped": { "type": "boolean" }
                                }
                              }
                            }
                          }
                        }
                      }
                    },
                    "usageExampleSummary": {
                      "type": "object",
                      "additionalProperties": false,
                      "required": ["totalExamples", "goodExamples", "badExamples", "limit", "truncated", "source", "scope"],
                      "properties": {
                        "totalExamples": { "type": "integer" },
                        "goodExamples": { "type": "integer" },
                        "badExamples": { "type": "integer" },
                        "limit": { "type": "integer" },
                        "truncated": { "type": "boolean" },
                        "source": { "type": "string" },
                        "scope": { "type": "string" }
                      }
                    },
                    "usageExamples": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["scope", "exampleType", "input", "reason", "tags"],
                        "properties": {
                          "id": { "type": "integer" },
                          "scope": { "type": "string", "enum": ["FIELD", "RULE", "TEMPLATE", "GENERAL"] },
                          "exampleType": { "type": "string", "enum": ["GOOD", "BAD"] },
                          "fieldId": { "type": "integer" },
                          "ruleCode": { "type": "string" },
                          "templateId": { "type": "integer" },
                          "input": { "type": "string" },
                          "expectedOutput": { "type": "string" },
                          "antiPattern": { "type": "string" },
                          "reason": { "type": "string" },
                          "tags": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "priority": { "type": "integer" },
                          "status": { "type": "string" }
                        }
                      }
                    },
                    "fields": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["name", "dataType", "nullable"],
                        "properties": {
                          "name": { "type": "string" },
                          "dataType": { "type": "string" },
                          "nullable": { "type": "boolean" },
                          "comment": { "type": "string" },
                          "defaultValue": { "type": "string" },
                          "displayName": { "type": "string" },
                          "aliases": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "category": { "type": "string" },
                          "codeSetId": { "type": "integer" },
                          "sensitive": { "type": "boolean" },
                          "status": {
                            "type": "string",
                            "enum": ["draft", "enabled", "disabled", "deprecated"]
                          },
                          "replacementFieldId": { "type": "integer" },
                          "replacementReason": { "type": "string" },
                          "tags": { "type": "string" },
                          "starterKitSources": {
                            "type": "array",
                            "items": {
                              "type": "object",
                              "additionalProperties": false,
                              "required": ["kitKey", "kitVersion"],
                              "properties": {
                                "kitKey": { "type": "string" },
                                "kitVersion": { "type": "string" }
                              }
                            }
                          },
                          "matchReasons": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "example": { "type": "string" },
                          "format": {
                            "type": "object",
                            "additionalProperties": false,
                            "properties": {
                              "type": { "type": "string" },
                              "pattern": { "type": "string" },
                              "unit": { "type": "string" },
                              "precision": { "type": "string" },
                              "timezone": { "type": "string" },
                              "nullPolicy": { "type": "string" },
                              "validExamples": {
                                "type": "array",
                                "items": { "type": "string" }
                              },
                              "invalidExamples": {
                                "type": "array",
                                "items": { "type": "string" }
                              },
                              "notes": { "type": "string" }
                            }
                          }
                        }
                      }
                    },
                    "enums": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["code", "name", "valueType", "values"],
                        "properties": {
                          "code": { "type": "string" },
                          "name": { "type": "string" },
                          "valueType": { "type": "string" },
                          "values": {
                            "type": "array",
                            "items": {
                              "type": "object",
                              "additionalProperties": false,
                              "required": ["value", "label"],
                              "properties": {
                                "value": { "type": "string" },
                                "label": { "type": "string" }
                              }
                            }
                          }
                        }
                      }
                    },
                    "glossary": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["term", "synonyms", "rootTerms", "abbreviations", "disabledTerms", "exampleFields"],
                        "properties": {
                          "term": { "type": "string" },
                          "synonyms": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "rootTerms": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "abbreviations": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "disabledTerms": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "canonicalFieldName": { "type": "string" },
                          "scopeType": { "type": "string" },
                          "scopeValue": { "type": "string" },
                          "exampleFields": {
                            "type": "array",
                            "items": { "type": "string" }
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private ArrayNode aliasesToArrayNode(ObjectMapper mapper, String aliases) {
        ArrayNode node = mapper.createArrayNode();
        if (aliases == null || aliases.isBlank()) {
            return node;
        }
        Arrays.stream(aliases.split(","))
                .map(String::trim)
                .filter(alias -> !alias.isBlank())
                .distinct()
                .forEach(node::add);
        return node;
    }

    private ObjectNode formatNode(ObjectMapper mapper, Field field) {
        ObjectNode node = mapper.createObjectNode();
        putText(node, "type", field.getFormatType());
        putText(node, "pattern", field.getFormatPattern());
        putText(node, "unit", field.getFormatUnit());
        putText(node, "precision", field.getFormatPrecision());
        putText(node, "timezone", field.getFormatTimezone());
        putText(node, "nullPolicy", field.getFormatNullPolicy());
        putText(node, "notes", field.getFormatNotes());
        putExamples(node, "validExamples", field.getValidExamplesJson());
        putExamples(node, "invalidExamples", field.getInvalidExamplesJson());
        return node;
    }

    private ObjectNode formatNode(ObjectMapper mapper, JsonNode field) {
        ObjectNode node = mapper.createObjectNode();
        copyText(node, field, "formatType", "type");
        copyText(node, field, "formatPattern", "pattern");
        copyText(node, field, "formatUnit", "unit");
        copyText(node, field, "formatPrecision", "precision");
        copyText(node, field, "formatTimezone", "timezone");
        copyText(node, field, "formatNullPolicy", "nullPolicy");
        copyText(node, field, "formatNotes", "notes");
        putExamples(node, "validExamples", field.path("validExamplesJson").asText(null));
        putExamples(node, "invalidExamples", field.path("invalidExamplesJson").asText(null));
        return node;
    }

    private void putText(ObjectNode node, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            node.put(fieldName, value);
        }
    }

    private void putExamples(ObjectNode node, String fieldName, String examplesJson) {
        ArrayNode examples = examplesToArrayNode(objectMapper.createArrayNode(), examplesJson);
        if (!examples.isEmpty()) {
            node.set(fieldName, examples);
        }
    }

    private ArrayNode examplesToArrayNode(ArrayNode node, String examplesJson) {
        if (examplesJson == null || examplesJson.isBlank()) {
            return node;
        }
        try {
            JsonNode parsed = objectMapper.readTree(examplesJson);
            if (!parsed.isArray()) {
                return node;
            }
            for (JsonNode item : parsed) {
                if (item.isTextual()) {
                    node.add(item.asText());
                }
            }
        } catch (Exception ignored) {
            // 历史快照可能来自旧版本或人工导入，格式样例解析失败时只跳过该可选字段。
        }
        return node;
    }

    private String fieldFormatSummary(Field field) {
        return fieldFormatSummary(
                field.getFormatType(),
                field.getFormatPattern(),
                field.getFormatUnit(),
                field.getFormatPrecision(),
                field.getFormatTimezone(),
                field.getFormatNullPolicy(),
                field.getValidExamplesJson(),
                field.getInvalidExamplesJson(),
                field.getFormatNotes());
    }

    private String fieldFormatSummary(JsonNode field) {
        return fieldFormatSummary(
                field.path("formatType").asText(null),
                field.path("formatPattern").asText(null),
                field.path("formatUnit").asText(null),
                field.path("formatPrecision").asText(null),
                field.path("formatTimezone").asText(null),
                field.path("formatNullPolicy").asText(null),
                field.path("validExamplesJson").asText(null),
                field.path("invalidExamplesJson").asText(null),
                field.path("formatNotes").asText(null));
    }

    private String fieldFormatSummary(
            String type,
            String pattern,
            String unit,
            String precision,
            String timezone,
            String nullPolicy,
            String validExamplesJson,
            String invalidExamplesJson,
            String notes
    ) {
        List<String> parts = new ArrayList<>();
        addFormatPart(parts, "type", type);
        addFormatPart(parts, "pattern", pattern);
        addFormatPart(parts, "unit", unit);
        addFormatPart(parts, "precision", precision);
        addFormatPart(parts, "timezone", timezone);
        addFormatPart(parts, "nullPolicy", nullPolicy);
        List<String> validExamples = exampleTexts(validExamplesJson);
        if (!validExamples.isEmpty()) {
            parts.add("validExamples=" + joinExampleTexts(validExamples));
        }
        List<String> invalidExamples = exampleTexts(invalidExamplesJson);
        if (!invalidExamples.isEmpty()) {
            parts.add("invalidExamples=" + joinExampleTexts(invalidExamples));
        }
        addFormatPart(parts, "notes", notes);
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private String joinExampleTexts(List<String> values) {
        List<String> formatted = new ArrayList<>();
        for (String value : values) {
            formatted.add(formatExampleText(value));
        }
        return String.join("/", formatted);
    }

    private String formatExampleText(String value) {
        return value != null && !value.isEmpty() && value.equals(value.trim())
                ? value
                : toJsonString(value);
    }

    private String toJsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value == null ? "" : value);
        } catch (Exception ignored) {
            return "\"\"";
        }
    }

    private void addFormatPart(List<String> parts, String label, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(label + "=" + value);
        }
    }

    private List<String> exampleTexts(String examplesJson) {
        ArrayNode node = examplesToArrayNode(objectMapper.createArrayNode(), examplesJson);
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private ArrayNode starterKitSourcesToArrayNode(ObjectMapper mapper, String tags) {
        ArrayNode node = mapper.createArrayNode();
        if (tags == null || tags.isBlank()) {
            return node;
        }
        Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .filter(tag -> tag.startsWith(STARTER_KIT_TAG_PREFIX))
                .distinct()
                .forEach(tag -> addStarterKitSource(node, tag));
        return node;
    }

    private void addStarterKitSource(ArrayNode node, String tag) {
        String marker = tag.substring(STARTER_KIT_TAG_PREFIX.length());
        int versionSeparator = marker.lastIndexOf('@');
        if (versionSeparator <= 0 || versionSeparator == marker.length() - 1) {
            return;
        }
        ObjectNode source = node.objectNode();
        source.put("kitKey", marker.substring(0, versionSeparator));
        source.put("kitVersion", marker.substring(versionSeparator + 1));
        node.add(source);
    }

    private String fieldStatusForExport(String status) {
        return status == null || status.isBlank() ? "enabled" : status;
    }

    private static Map<String, String> orderedStringMap(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    @SafeVarargs
    private static Map<String, List<String>> orderedStringListMap(Map.Entry<String, List<String>>... entries) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private String generatePromptsMarkdown() {
        return """
                # DataSpec Prompts

                ## 创建表

                你是数据库设计助手。请先阅读 `.dataspec/DATABASE_RULES.md`、`.dataspec/field-catalog.json` 和 `.dataspec/rules.yaml`，再根据业务需求生成 PostgreSQL `CREATE TABLE` 语句和 `COMMENT ON` 注释。

                要求:
                - 优先复用字段目录中的标准字段名、类型、默认值和注释。
                - 表名和字段名必须符合 DataSpec 命名规则。
                - 必须包含公共字段 `id`、`created_at`、`updated_at`、`is_deleted`。
                - 输出 SQL 后，说明每个非标准字段的命名理由。

                ## 评审并修正 SQL

                你是 DataSpec SQL Review 助手。请读取 `.dataspec/` 目录中的规则和字段目录，检查给定 SQL 是否符合标准，并输出:
                1. 问题列表，包含表名、字段名、规则和原因。
                2. 推荐字段名或类型。
                3. 一份修正后的 SQL 草案。
                """;
    }

    private String generateAgentsFragment(Long projectId) {
        return """
                # DataSpec 数据库规范

                在创建或修改数据库 schema、SQL migration、ORM entity 或数据字典前，必须先阅读:

                - `.dataspec/manifest.json`
                - `.dataspec/capabilities.json`
                - `.dataspec/DATABASE_RULES.md`
                - `.dataspec/field-catalog.json`
                - `.dataspec/usage-examples.json`
                - `.dataspec/schema-registry.json`
                - `.dataspec/rules.yaml`

                工作要求:
                - 先检查 `.dataspec/manifest.json` 的 `standard.specVersion` 和 `standard.specHash`，在输出说明中标明使用的标准版本。
                - 先读取 `.dataspec/capabilities.json`，确认当前任务应使用的 API、CLI、MCP resource/tool、preflightChecks 和 writeRisk。
                - 需要稳定输出字段或兼容策略时，读取 `.dataspec/schema-registry.json`，并以 stableFields/schemaVersion 为准。
                - 优先使用 `.dataspec/field-catalog.json` 中已有标准字段。
                - 字段、DDL 或 prompt 有相似 scope 时，优先模仿 `GOOD` 示例，避免复用 `.dataspec/usage-examples.json` 中的 `BAD` antiPattern。
                - 新增表必须符合 `.dataspec/DATABASE_RULES.md` 的命名、类型、注释和公共字段规则。
                - 生成 SQL 时参考 `.dataspec/examples/good.sql`，避免 `.dataspec/examples/bad.sql` 中的反例。
                - 提交 SQL 变更前运行 `dataspec lint <path|-> --project %d --format json`，并修复 ERROR 级问题。
                - 不确定字段命名时，先在字段目录中查找相同业务含义，再提出新增标准字段建议。
                """.formatted(projectId);
    }

    private String loadExampleSql(String primaryPath, String fallbackPath, String defaultContent) {
        try {
            Path primary = Path.of(primaryPath);
            if (Files.exists(primary)) {
                return Files.readString(primary, StandardCharsets.UTF_8);
            }
            Path fallback = Path.of(fallbackPath);
            if (Files.exists(fallback)) {
                return Files.readString(fallback, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // 示例 SQL 仅用于 AI 上下文包，读取失败时使用内置保底内容。
        }
        return defaultContent;
    }

    private String defaultGoodExampleSql() {
        return """
                CREATE TABLE users (
                    id bigserial PRIMARY KEY,
                    username varchar(50) NOT NULL,
                    created_at timestamp with time zone NOT NULL DEFAULT now(),
                    updated_at timestamp with time zone NOT NULL DEFAULT now(),
                    is_deleted boolean NOT NULL DEFAULT false
                );

                COMMENT ON TABLE users IS '用户表';
                COMMENT ON COLUMN users.id IS '用户ID';
                COMMENT ON COLUMN users.username IS '用户名';
                COMMENT ON COLUMN users.created_at IS '创建时间';
                COMMENT ON COLUMN users.updated_at IS '更新时间';
                COMMENT ON COLUMN users.is_deleted IS '软删除标记';
                """;
    }

    private String defaultBadExampleSql() {
        return """
                CREATE TABLE UserOrder (
                    userId bigint NOT NULL,
                    uid bigint NOT NULL,
                    create_time datetime NOT NULL,
                    update_time datetime NOT NULL,
                    totalAmount float NOT NULL,
                    status int NOT NULL
                );
                """;
    }

    private StandardSnapshotInfo currentSnapshot(Long projectId) {
        return standardSnapshotService.getCurrentSnapshot(projectId);
    }

    private StandardSnapshotPayload resolveSnapshotPayload(Long projectId, Long snapshotId, String snapshotVersion) {
        if (snapshotId != null) {
            return standardSnapshotService.getSnapshotPayload(projectId, snapshotId);
        }
        if (snapshotVersion != null && !snapshotVersion.isBlank()) {
            return standardSnapshotService.getSnapshotPayloadByVersion(projectId, snapshotVersion);
        }
        return null;
    }

    private ObjectNode standardNode(ObjectMapper mapper, StandardSnapshotInfo snapshot) {
        ObjectNode node = mapper.createObjectNode();
        if (snapshot.snapshotId() != null) {
            node.put("snapshotId", snapshot.snapshotId());
        }
        node.put("specVersion", snapshot.specVersion());
        if (snapshot.specHash() != null) {
            node.put("specHash", snapshot.specHash());
        }
        if (snapshot.name() != null) {
            node.put("name", snapshot.name());
        }
        node.put("versioned", snapshot.versioned());
        node.put("source", snapshot.source());
        return node;
    }

    private void appendStandardYaml(StringBuilder yaml, StandardSnapshotInfo snapshot) {
        yaml.append("standard:\n");
        if (snapshot.snapshotId() != null) {
            yaml.append("  snapshot_id: ").append(snapshot.snapshotId()).append("\n");
        }
        yaml.append("  spec_version: ").append(snapshot.specVersion()).append("\n");
        if (snapshot.specHash() != null) {
            yaml.append("  spec_hash: ").append(snapshot.specHash()).append("\n");
        }
        yaml.append("  versioned: ").append(snapshot.versioned()).append("\n");
        yaml.append("  source: ").append(snapshot.source()).append("\n\n");
    }

    private Iterable<JsonNode> snapshotArray(StandardSnapshotPayload snapshotPayload, String fieldName) {
        JsonNode node = snapshotPayload.payload().path(fieldName);
        return node.isArray() ? node : objectMapper.createArrayNode();
    }

    private List<RuleConfig> snapshotRuleConfigs(StandardSnapshotPayload snapshotPayload) {
        List<RuleConfig> configs = new ArrayList<>();
        for (JsonNode ruleNode : snapshotArray(snapshotPayload, "rules")) {
            RuleConfig rule = new RuleConfig();
            rule.setId(ruleNode.path("id").isNumber() ? ruleNode.path("id").asLong() : null);
            rule.setRuleCode(ruleNode.path("ruleCode").asText(null));
            rule.setRuleName(ruleNode.path("ruleName").asText(null));
            rule.setSeverity(ruleNode.path("severity").asText(null));
            rule.setEnabled(ruleNode.path("enabled").isMissingNode() || ruleNode.path("enabled").asBoolean());
            rule.setParamsJson(ruleNode.path("paramsJson").asText(null));
            configs.add(rule);
        }
        return configs;
    }

    private String generateDatabaseRules(Long projectId, StandardSnapshotPayload snapshotPayload) {
        StringBuilder md = new StringBuilder();
        md.append("# Database Rules\n\n");
        md.append("<!-- 此文件由 DataSpec 历史标准快照生成，供 AI 编程工具参考 -->\n\n");

        List<Map<String, String>> rules = sqlLintService.listAvailableRules();
        md.append("## 校验规则\n\n");
        for (Map<String, String> rule : rules) {
            md.append(String.format("- **%s** (%s)\n", rule.get("name"), rule.get("code")));
        }
        md.append("\n");

        List<RuleConfig> configs = snapshotRuleConfigs(snapshotPayload);
        if (!configs.isEmpty()) {
            md.append("## 项目规则配置\n\n");
            for (RuleConfig cfg : configs) {
                md.append(String.format("- `%s` [%s] %s\n",
                        cfg.getRuleCode(), cfg.getSeverity(), cfg.getRuleName()));
                if (cfg.getParamsJson() != null && !cfg.getParamsJson().isBlank()) {
                    md.append(String.format("  - 参数: %s\n", cfg.getParamsJson()));
                }
            }
            md.append("\n");
        }

        JsonNode fields = snapshotPayload.payload().path("fields");
        if (fields.isArray() && !fields.isEmpty()) {
            md.append("## 标准字段\n\n");
            for (JsonNode field : fields) {
                md.append(String.format("- `%s` %s",
                        field.path("name").asText(""),
                        field.path("dataType").asText("")));
                if (!field.path("comment").asText("").isBlank()) {
                    md.append(" — ").append(field.path("comment").asText());
                }
                md.append("\n");
                String formatSummary = fieldFormatSummary(field);
                if (formatSummary != null) {
                    md.append("  - 值格式: ").append(formatSummary).append("\n");
                }
            }
            md.append("\n");
        }
        return md.toString();
    }

    private void copyText(ObjectNode target, JsonNode source, String sourceName, String targetName) {
        JsonNode node = source.path(sourceName);
        if (!node.isMissingNode() && !node.isNull() && !node.asText("").isBlank()) {
            target.put(targetName, node.asText());
        }
    }

    private void copyBoolean(ObjectNode target, JsonNode source, String sourceName, String targetName) {
        JsonNode node = source.path(sourceName);
        if (!node.isMissingNode() && !node.isNull()) {
            target.put(targetName, node.asBoolean());
        }
    }

    private void copyLong(ObjectNode target, JsonNode source, String sourceName, String targetName) {
        JsonNode node = source.path(sourceName);
        if (!node.isMissingNode() && !node.isNull() && node.canConvertToLong()) {
            target.put(targetName, node.asLong());
        }
    }

    private record ScopedFields(List<FieldMatch> fields, ScopeSummary summary) {
    }

    private record FieldMatch(Field field, List<String> reasons) {
    }

    private record ScopeSummary(
            boolean includeMetadata,
            String scope,
            String query,
            String status,
            Integer limit,
            String profileId,
            String taskType,
            int totalFieldCount,
            int matchedFieldCount,
            int returnedFieldCount,
            List<String> warnings,
            FieldGroupSummary groupSummary
    ) {
        static ScopeSummary full() {
            return new ScopeSummary(false, "all", null, null, null, null, null, 0, 0, 0, List.of(), null);
        }
    }

    private record UsageExampleExport(List<StandardUsageExample> examples, UsageExampleSummary summary) {
    }

    private record UsageExampleSummary(
            int totalExamples,
            int goodExamples,
            int badExamples,
            int limit,
            boolean truncated,
            String source,
            String scope
    ) {
    }
}
