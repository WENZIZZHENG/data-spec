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

import java.util.List;
import java.util.Map;

/**
 * AI 规则导出服务 —— 生成供 AI 编程工具使用的标准文件
 */
@Service
@RequiredArgsConstructor
public class AiContextExportService {

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
                if (f.getComment() != null) fn.put("comment", f.getComment());
                if (f.getDefaultValue() != null) fn.put("defaultValue", f.getDefaultValue());
                if (f.getDisplayName() != null) fn.put("displayName", f.getDisplayName());
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
}
