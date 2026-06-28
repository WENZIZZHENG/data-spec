package com.dataspec.prompt.service;

import com.dataspec.common.exception.BizException;
import com.dataspec.prompt.model.PromptTemplateDefinition;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt 模板注册表。
 *
 * <p>所有 AI job 的 promptVersion 都从这里取，避免各服务自行维护字符串后漂移。</p>
 */
@Service
public class PromptTemplateRegistry {

    public static final String CREATE_TABLE = "create-table-prompt";
    public static final String FIX_SQL = "fix-sql-prompt";
    public static final String SQL_LINT_FIX = "sql-lint-fix";
    public static final String DDL_PREVIEW = "ddl-preview";
    public static final String FIELD_RECOMMENDATION_EXPLANATION = "field-recommendation-explanation";

    private final Map<String, PromptTemplateDefinition> templates;

    public PromptTemplateRegistry() {
        Map<String, PromptTemplateDefinition> definitions = new LinkedHashMap<>();
        register(definitions, new PromptTemplateDefinition(
                CREATE_TABLE,
                CREATE_TABLE + "@1",
                "CREATE_TABLE",
                "建表 Prompt",
                "POSTGRESQL_DDL_WITH_COMMENTS",
                List.of(
                        "# DataSpec 建表 Prompt",
                        "## Prompt Metadata",
                        "## 业务需求",
                        "## 字段目录 field-catalog.json",
                        "## 命名规则 rules.yaml",
                        "## 数据库规则 DATABASE_RULES.md",
                        "## 输出要求"
                ),
                List.of(
                        "promptVersion: " + CREATE_TABLE + "@1",
                        "PostgreSQL CREATE TABLE",
                        "COMMENT ON TABLE / COMMENT ON COLUMN",
                        "优先复用 field-catalog.json"
                ),
                List.of("v1: 初始建表 Prompt，约束标准字段复用、命名规则和 PostgreSQL 注释输出。")
        ));
        register(definitions, new PromptTemplateDefinition(
                FIX_SQL,
                FIX_SQL + "@1",
                "FIX_SQL",
                "SQL 修正 Prompt",
                "MARKDOWN_WITH_FIXED_SQL",
                List.of(
                        "# DataSpec SQL 修正 Prompt",
                        "## Prompt Metadata",
                        "## 原始 SQL",
                        "## Lint 统计",
                        "## Lint issues",
                        "## 字段目录 field-catalog.json",
                        "## 命名规则 rules.yaml",
                        "## 输出要求"
                ),
                List.of(
                        "promptVersion: " + FIX_SQL + "@1",
                        "修正后的 SQL",
                        "优先复用标准字段"
                ),
                List.of("v1: 初始 SQL 修正 Prompt，约束 lint issue、字段目录和修正 SQL 输出。")
        ));
        register(definitions, new PromptTemplateDefinition(
                SQL_LINT_FIX,
                SQL_LINT_FIX + "@1",
                "SQL_LINT_FIX",
                "SQL 检查与 fixedSql",
                "LINT_RESULT_WITH_FIXED_SQL",
                List.of("fixedSql", "fixedSqlDiff", "issues"),
                List.of("ruleCode", "severity", "dialectDiagnostics"),
                List.of("v1: 初始确定性 SQL lint/fixedSql 回放版本。")
        ));
        register(definitions, new PromptTemplateDefinition(
                DDL_PREVIEW,
                DDL_PREVIEW + "@1",
                "DDL_PREVIEW",
                "DDL 预览",
                "POSTGRESQL_DDL_PREVIEW",
                List.of("ddl", "lintSummary", "dialectDiagnostics"),
                List.of("POSTGRESQL_DDL_TARGET", "MYSQL_DDL_CONVERSION_REQUIRED"),
                List.of("v1: 初始 PostgreSQL DDL preview 回放版本。")
        ));
        register(definitions, new PromptTemplateDefinition(
                FIELD_RECOMMENDATION_EXPLANATION,
                FIELD_RECOMMENDATION_EXPLANATION + "@1",
                "FIELD_RECOMMENDATION_EXPLANATION",
                "字段推荐解释",
                "STRUCTURED_EXPLANATION",
                List.of("matchedFields", "reasons", "fallback"),
                List.of("confidence", "recommendedName"),
                List.of("v1: 为后续字段推荐解释预留的稳定模板版本。")
        ));
        this.templates = Collections.unmodifiableMap(definitions);
    }

    public List<PromptTemplateDefinition> listTemplates() {
        return List.copyOf(templates.values());
    }

    public PromptTemplateDefinition getTemplate(String templateKey) {
        PromptTemplateDefinition template = templates.get(templateKey);
        if (template == null) {
            throw new BizException("未知 Prompt 模板: " + templateKey);
        }
        return template;
    }

    public String promptVersion(String templateKey) {
        return getTemplate(templateKey).promptVersion();
    }

    public boolean hasPromptVersion(String promptVersion) {
        return templates.values().stream()
                .anyMatch(template -> template.promptVersion().equals(promptVersion));
    }

    private void register(Map<String, PromptTemplateDefinition> definitions, PromptTemplateDefinition definition) {
        if (definitions.containsKey(definition.templateKey())) {
            throw new IllegalStateException("重复 Prompt 模板 key: " + definition.templateKey());
        }
        if (!definition.promptVersion().startsWith(definition.templateKey() + "@")) {
            throw new IllegalStateException("Prompt 模板版本必须使用 <templateKey>@<version>: " + definition.promptVersion());
        }
        definitions.put(definition.templateKey(), definition);
    }
}
