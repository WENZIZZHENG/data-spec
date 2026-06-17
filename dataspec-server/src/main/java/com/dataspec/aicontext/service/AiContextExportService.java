package com.dataspec.aicontext.service;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * AI 规则导出服务 —— 生成供 AI 编程工具使用的标准文件
 */
@Service
@RequiredArgsConstructor
public class AiContextExportService {

    private static final String PACKAGE_FILE_NAME = "dataspec-ai-context.zip";
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
    private final SqlLintService sqlLintService;
    private final ObjectMapper objectMapper;

    /**
     * 生成 DATABASE_RULES.md —— 给 AI 工具使用的数据库规范文档
     */
    public String generateDatabaseRules(Long projectId) {
        StringBuilder md = new StringBuilder();
        md.append("# Database Rules\n\n");
        md.append("<!-- 此文件由 DataSpec 自动生成，供 AI 编程工具参考 -->\n\n");

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

        // 标准字段
        List<Field> fields = fieldService.listByProject(projectId);
        if (!fields.isEmpty()) {
            md.append("## 标准字段\n\n");
            for (Field f : fields) {
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
        try {
            ObjectMapper mapper = objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT);

            ObjectNode root = mapper.createObjectNode();
            root.put("projectId", projectId);

            // 字段目录
            ArrayNode fieldsNode = mapper.createArrayNode();
            for (Field f : fieldService.listByProject(projectId)) {
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
                if (f.getCodeSetId() != null) fn.put("codeSetId", f.getCodeSetId());
                if (f.getExampleValue() != null) fn.put("example", f.getExampleValue());
                ArrayNode aliasesNode = aliasesToArrayNode(mapper, f.getAliases());
                if (!aliasesNode.isEmpty()) fn.set("aliases", aliasesNode);
                fieldsNode.add(fn);
            }
            root.set("fields", fieldsNode);

            // 枚举目录
            ArrayNode enumsNode = mapper.createArrayNode();
            for (EnumDict e : enumDictService.listByProject(projectId)) {
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
        StringBuilder yaml = new StringBuilder();
        yaml.append("# DataSpec 规则配置\n");
        yaml.append("# 此文件由 DataSpec 自动生成\n\n");

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

        return yaml.toString();
    }

    /**
     * 生成建表 Prompt —— 用户复制给 AI 后即可按当前项目标准产出 PostgreSQL DDL。
     */
    public String generateCreateTablePrompt(Long projectId, String businessDescription) {
        String description = businessDescription == null || businessDescription.isBlank()
                ? "请根据后续业务描述设计数据表。"
                : businessDescription.trim();
        return """
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
                generateFieldCatalogJson(projectId),
                generateRulesYaml(projectId),
                generateDatabaseRules(projectId)
        );
    }

    /**
     * 生成 SQL 修正 Prompt —— 先跑 lint，再把结构化问题和当前标准一起交给 AI。
     */
    public String generateFixSqlPrompt(Long projectId, String sql) {
        LintResult lintResult = sqlLintService.lint(sql, projectId);
        return """
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
                writePrettyJson(lintResult.getIssues()),
                generateFieldCatalogJson(projectId),
                generateRulesYaml(projectId)
        );
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

    /**
     * 生成 AI Context zip 包 —— 可直接复制到业务项目供 AI 编程工具读取
     */
    public byte[] generateAiContextPackage(Long projectId) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            addTextEntry(zip, ".dataspec/DATABASE_RULES.md", generateDatabaseRules(projectId));
            addTextEntry(zip, ".dataspec/field-catalog.json", generateFieldCatalogJson(projectId));
            addTextEntry(zip, ".dataspec/field-catalog.schema.json", generateFieldCatalogSchemaJson());
            addTextEntry(zip, ".dataspec/rules.yaml", generateRulesYaml(projectId));
            addTextEntry(zip, ".dataspec/prompts.md", generatePromptsMarkdown());
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
            addTextEntry(zip, "AGENTS.md.fragment", generateAgentsFragment());
            zip.finish();
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成 " + PACKAGE_FILE_NAME + " 失败", e);
        }
    }

    private void addTextEntry(ZipOutputStream zip, String entryName, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
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

    private String generateAgentsFragment() {
        return """
                # DataSpec 数据库规范

                在创建或修改数据库 schema、SQL migration、ORM entity 或数据字典前，必须先阅读:

                - `.dataspec/DATABASE_RULES.md`
                - `.dataspec/field-catalog.json`
                - `.dataspec/rules.yaml`

                工作要求:
                - 优先使用 `.dataspec/field-catalog.json` 中已有标准字段。
                - 新增表必须符合 `.dataspec/DATABASE_RULES.md` 的命名、类型、注释和公共字段规则。
                - 生成 SQL 时参考 `.dataspec/examples/good.sql`，避免 `.dataspec/examples/bad.sql` 中的反例。
                - 不确定字段命名时，先在字段目录中查找相同业务含义，再提出新增标准字段建议。
                """;
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
}
