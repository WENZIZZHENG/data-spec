package com.dataspec.aicontext.service;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
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
        yaml.append("rules:\n");

        List<RuleConfig> configs = ruleConfigService.listByProject(projectId);
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
