package com.dataspec.aicontext.service;

import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.fasterxml.jackson.core.type.TypeReference;
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

    private static final String CREATE_TABLE_PROMPT_VERSION = "create-table-prompt@1";
    private static final String FIX_SQL_PROMPT_VERSION = "fix-sql-prompt@1";
    private static final String PACKAGE_FILE_NAME = "dataspec-ai-context.zip";
    private static final int DATASPEC_CONTEXT_SCHEMA_VERSION = 1;
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

    /**
     * 生成 DATABASE_RULES.md —— 给 AI 工具使用的数据库规范文档
     */
    public String generateDatabaseRules(Long projectId) {
        return generateDatabaseRules(projectId, AiContextScopeOptions.full());
    }

    public String generateDatabaseRules(Long projectId, AiContextScopeOptions options) {
        return generateDatabaseRules(projectId, buildScopedFields(projectId, options));
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
            }
            md.append("\n");
        }

        return md.toString();
    }

    /**
     * 生成 field-catalog.json —— 字段目录 JSON
     */
    public String generateFieldCatalogJson(Long projectId) {
        return generateFieldCatalogJson(projectId, AiContextScopeOptions.full());
    }

    public String generateFieldCatalogJson(Long projectId, AiContextScopeOptions options) {
        return generateFieldCatalogJson(projectId, currentSnapshot(projectId), buildScopedFields(projectId, options));
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
            if (scopedFields.summary().includeMetadata()) {
                root.set("contextScope", contextScopeNode(mapper, scopedFields.summary()));
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
                if (f.getCodeSetId() != null) fn.put("codeSetId", f.getCodeSetId());
                if (f.getExampleValue() != null) fn.put("example", f.getExampleValue());
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

            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("生成 field-catalog.json 失败", e);
        }
    }

    /**
     * 生成 rules.yaml —— 规则配置 YAML
     */
    public String generateRulesYaml(Long projectId) {
        return generateRulesYaml(projectId, currentSnapshot(projectId));
    }

    private String generateRulesYaml(Long projectId, StandardSnapshotInfo snapshot) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# DataSpec 规则配置\n");
        yaml.append("# 此文件由 DataSpec 自动生成\n\n");
        appendStandardYaml(yaml, snapshot);

        List<RuleConfig> configs = ruleConfigService.listByProject(projectId);
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
        String prompt = """
                # DataSpec 建表 Prompt

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
                CREATE_TABLE_PROMPT_VERSION,
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
        String prompt = """
                # DataSpec SQL 修正 Prompt

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
                FIX_SQL_PROMPT_VERSION,
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
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            StandardSnapshotInfo snapshot = currentSnapshot(projectId);
            ScopedFields scopedFields = buildScopedFields(projectId, options);
            addTextEntry(zip, ".dataspec/DATABASE_RULES.md", generateDatabaseRules(projectId, scopedFields));
            addTextEntry(zip, ".dataspec/field-catalog.json", generateFieldCatalogJson(projectId, snapshot, scopedFields));
            addTextEntry(zip, ".dataspec/field-catalog.schema.json", generateFieldCatalogSchemaJson());
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
            if (scopeSummary.includeMetadata()) {
                root.set("contextScope", contextScopeNode(objectMapper, scopeSummary));
            }

            ArrayNode files = root.putArray("files");
            files.add(".dataspec/manifest.json");
            files.add(".dataspec/README.md");
            files.add(".dataspec/DATABASE_RULES.md");
            files.add(".dataspec/field-catalog.json");
            files.add(".dataspec/field-catalog.schema.json");
            files.add(".dataspec/rules.yaml");
            files.add(".dataspec/prompts.md");
            files.add(".dataspec/workflows.md");
            files.add(".dataspec/examples/good.sql");
            files.add(".dataspec/examples/bad.sql");
            files.add("AGENTS.md.fragment");

            ObjectNode commands = root.putObject("commands");
            commands.put("lint", "dataspec lint <path|-> --project " + projectId + " --format json");
            commands.put("exportContext", exportContextCommand(projectId, scopeSummary));
            commands.put("workflowList", "dataspec workflow list --format json");
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("生成 .dataspec/manifest.json 失败", e);
        }
    }

    private String generateDataspecReadme(Long projectId) {
        return generateDataspecReadme(projectId, ScopeSummary.full());
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
                - `.dataspec/rules.yaml`：结构化命名规则和项目规则配置。
                - `.dataspec/prompts.md`：建表和 SQL Review 的 AI prompt 模板。
                - `.dataspec/workflows.md`：常见 AI/DataSpec 任务 recipe，说明输入、步骤、命令、产物和失败恢复。
                - `.dataspec/examples/good.sql`：符合标准的 SQL 示例。
                - `.dataspec/examples/bad.sql`：不符合标准的 SQL 反例。
                - `AGENTS.md.fragment`：可复制到业务项目 `AGENTS.md` 的 DataSpec 指令片段。

                ## 使用约定

                - 创建或修改 SQL、migration、ORM entity 前，先读取 `.dataspec/manifest.json`、字段目录和规则文件。
                - 不确定任务步骤时，先读取 `.dataspec/workflows.md`，选择合适 recipe 后再显式执行其中的命令。
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
        AiContextScopeOptions options = rawOptions == null ? AiContextScopeOptions.full() : rawOptions;
        List<Field> allFields = fieldService.listByProject(projectId);
        List<String> warnings = new ArrayList<>();
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

        ScopeSummary summary = new ScopeSummary(
                options.scoped() || !warnings.isEmpty(),
                effectiveScope,
                options.query(),
                options.status(),
                options.limit(),
                allFields.size(),
                matchedCount,
                returnedFields.size(),
                List.copyOf(warnings)
        );
        return new ScopedFields(List.copyOf(returnedFields), summary);
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
        node.put("totalFieldCount", summary.totalFieldCount());
        node.put("matchedFieldCount", summary.matchedFieldCount());
        node.put("returnedFieldCount", summary.returnedFieldCount());
        ArrayNode warningsNode = mapper.createArrayNode();
        summary.warnings().forEach(warningsNode::add);
        node.set("warnings", warningsNode);
        return node;
    }

    private String exportContextCommand(Long projectId, ScopeSummary scopeSummary) {
        StringBuilder command = new StringBuilder("dataspec export-context --project ")
                .append(projectId)
                .append(" --output dataspec-ai-context.zip");
        if (scopeSummary.includeMetadata()) {
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
                        "versioned": { "type": "boolean" }
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
                        "totalFieldCount": { "type": "integer" },
                        "matchedFieldCount": { "type": "integer" },
                        "returnedFieldCount": { "type": "integer" },
                        "warnings": {
                          "type": "array",
                          "items": { "type": "string" }
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
                            "enum": ["enabled", "disabled", "deprecated"]
                          },
                          "tags": { "type": "string" },
                          "matchReasons": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "example": { "type": "string" }
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
                - `.dataspec/DATABASE_RULES.md`
                - `.dataspec/field-catalog.json`
                - `.dataspec/rules.yaml`

                工作要求:
                - 先检查 `.dataspec/manifest.json` 的 `standard.specVersion` 和 `standard.specHash`，在输出说明中标明使用的标准版本。
                - 优先使用 `.dataspec/field-catalog.json` 中已有标准字段。
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
        yaml.append("  versioned: ").append(snapshot.versioned()).append("\n\n");
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
            int totalFieldCount,
            int matchedFieldCount,
            int returnedFieldCount,
            List<String> warnings
    ) {
        static ScopeSummary full() {
            return new ScopeSummary(false, "all", null, null, null, 0, 0, 0, List.of());
        }
    }
}
