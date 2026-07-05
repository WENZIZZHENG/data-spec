package com.dataspec.capability.service.impl;

import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityDiagnostic;
import com.dataspec.capability.model.AiCapabilityEntry;
import com.dataspec.capability.model.AiCapabilityExample;
import com.dataspec.capability.service.AiCapabilityCatalogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.security.context.ProjectAccessGuard;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 面向 AI agent 的内置能力清单。
 *
 * <p>能力清单是只读目录，不执行任务、不授予权限；实际权限仍由具体 API/CLI/MCP 调用校验。</p>
 */
@Service
public class AiCapabilityCatalogServiceImpl implements AiCapabilityCatalogService {

    public static final String KIND = "dataspec-ai-capability-catalog";
    public static final int SCHEMA_VERSION = 1;
    public static final String CATALOG_VERSION = "2026.07.05";

    private final Map<String, AiCapabilityEntry> capabilities = builtIns();

    @Override
    public AiCapabilityCatalog getCatalog(Long projectId) {
        validateProjectAccess(projectId);
        return new AiCapabilityCatalog(
                KIND,
                SCHEMA_VERSION,
                CATALOG_VERSION,
                LocalDateTime.now(),
                projectId,
                capabilities.values().stream().toList(),
                requiredCapabilityIds(),
                recommendedFirstActions(projectId),
                diagnostics(projectId)
        );
    }

    @Override
    public AiCapabilityEntry getCapability(String capabilityId, Long projectId) {
        validateProjectAccess(projectId);
        String normalized = normalize(capabilityId);
        AiCapabilityEntry entry = normalized == null ? null : capabilities.get(normalized);
        if (entry == null) {
            throw new BizException(404, "未知 DataSpec capability: " + capabilityId
                    + "。请先读取 /api/capabilities 获取可用能力。");
        }
        return entry;
    }

    private void validateProjectAccess(Long projectId) {
        if (projectId != null) {
            ProjectAccessGuard.requireProjectAccess(projectId);
        }
    }

    private List<String> requiredCapabilityIds() {
        return capabilities.keySet().stream().toList();
    }

    private List<String> recommendedFirstActions(Long projectId) {
        if (projectId == null) {
            return List.of(
                    "先运行 dataspec doctor --format json 确认服务、token 和项目配置。",
                    "读取 capability-catalog 后，根据任务选择 workflow/profile/context/lint 等能力。",
                    "需要项目数据的能力应先确定 projectId。"
            );
        }
        return List.of(
                "先运行 dataspec doctor --project " + projectId + " --format json。",
                "需要建表或修 SQL 时先读取 export-ai-context、workflow-recipes 和 ai-task-profiles。",
                "执行写入型能力前确认 writeRisk、preflightChecks 和人工确认步骤。"
        );
    }

    private List<AiCapabilityDiagnostic> diagnostics(Long projectId) {
        if (projectId == null) {
            return List.of(diagnostic(
                    "MISSING_PROJECT",
                    "warn",
                    "未提供 projectId，catalog 仅返回全局能力说明。",
                    "选择项目后重新读取 /api/capabilities?projectId=<id> 获取项目级建议。"
            ));
        }
        return List.of(diagnostic(
                "CATALOG_READY",
                "pass",
                "capability catalog 可用于当前项目。",
                "从 recommendedFirstActions 开始选择下一步能力。"
        ));
    }

    private AiCapabilityDiagnostic diagnostic(String code, String status, String message, String nextAction) {
        return new AiCapabilityDiagnostic(code, status, message, nextAction);
    }

    private static Map<String, AiCapabilityEntry> builtIns() {
        Map<String, AiCapabilityEntry> map = new LinkedHashMap<>();
        add(map, cap(
                "session-bootstrap", "discovery", "AI 会话启动包",
                "新会话第一跳，聚合当前项目、标准版本、可用能力、推荐命令、风险提示和结构化 nextActions。",
                false, "READ_ONLY",
                list(), list("projectId", "server"),
                list("ai-session-bootstrap"),
                list("GET /api/bootstrap/session"),
                list("dataspec bootstrap --project <id> --format json"),
                list("dataspec://project/<id>/session-bootstrap"),
                list("get_session_bootstrap"),
                list(), list(), list(), list(),
                examples("CLI", "dataspec bootstrap --project 1 --format json", null),
                list("不执行 lint、导出 Context、反向导入、DDL 生成或写入操作", "如传 projectId，token 需有项目访问权"),
                list("AI 新会话先读取启动包，再按 recommendedCommands 调用 doctor/context/lint/workflow。"),
                "README.md#ai-会话启动包"
        ));
        add(map, cap(
                "capability-catalog", "discovery", "AI 能力清单",
                "列出 DataSpec 面向 AI/CLI/MCP 的稳定能力、入口、契约和下一步建议。",
                false, "READ_ONLY",
                list(), list("projectId"),
                list("ai-capability-catalog"),
                list("GET /api/capabilities", "GET /api/capabilities/{id}"),
                list("dataspec capability list --format json", "dataspec capability show lint-sql --format json"),
                list("dataspec://project/<id>/capability-catalog"),
                list(), list(), list(), list(), list(),
                examples("CLI", "dataspec capability list --project 1 --format json", null),
                list("服务可访问", "如传 projectId，token 需有项目访问权"),
                list("先读取 catalog，再决定调用 doctor、workflow、context 或 lint。"),
                "README.md#ai-能力清单"
        ));
        add(map, cap(
                "doctor", "diagnostic", "环境自检",
                "检查 DataSpec 服务、项目、token、OpenAPI、AI profile 和离线 Context 缓存状态。",
                false, "READ_ONLY",
                list(), list("projectId", "profile", "taskType", "checkOpenapi"),
                list("dataspec-doctor-result"),
                list("GET /api/auth/me", "GET /api/projects/{id}", "GET /api-docs"),
                list("dataspec doctor --project <id> --format json"),
                list(), list(), list(), list(), list(), list(),
                examples("CLI", "dataspec doctor --project 1 --format json", null),
                list("Node.js 18+", "DataSpec server 地址已配置"),
                list("doctor 失败时先修服务/token/projectId，再调用其他能力。"),
                "README.md#cli"
        ));
        add(map, cap(
                "export-ai-context", "context", "导出 AI Context",
                "导出字段目录、规则、workflow、prompt、schema registry 和离线缓存包。",
                true, "READ_ONLY",
                list("projectId"), list("scope", "query", "status", "limit", "snapshotId", "profileId"),
                list("ai-context-manifest", "ai-context-field-catalog", "rule-config"),
                list("GET /api/ai-context/package/download", "GET /api/ai-context/field-catalog"),
                list("dataspec export-context --project <id> --cache"),
                list("dataspec://project/<id>/field-catalog", "dataspec://project/<id>/database-rules"),
                list("get_field_catalog", "search_field_catalog"),
                list("/ai-export"),
                list("ai-context-manifest", "ai-context-field-catalog"),
                list("export-min-context"),
                list("create-table", "minimal-context"),
                examples("CLI", "dataspec export-context --project 1 --scope field --query 用户手机号 --cache", null),
                list("先确认 projectId", "大项目优先使用 scope/query 裁剪上下文"),
                list("让 AI 读取 .dataspec/capabilities.json 和 field-catalog.json 后再生成 SQL。"),
                "README.md#ai-context-导出包"
        ));
        add(map, cap(
                "lint-sql", "sql", "SQL 校验与 fixedSql",
                "校验建表 SQL，返回问题、方言诊断、fixedSql、dry-run 修复计划和检查记录。",
                true, "WRITES_DATASPEC_RECORD",
                list("projectId", "sql"), list("fixPolicy", "profileId", "taskType"),
                list("lint-result"),
                list("POST /api/lint"),
                list("dataspec lint <file.sql> --project <id> --format json"),
                list(), list("lint_sql"),
                list("/sql-lint"),
                list("lint-result"),
                list("review-pr-sql"),
                list("sql-fix", "pr-review"),
                examples("CLI", "dataspec lint db/schema.sql --project 1 --profile sql-fix --format json", null),
                list("先读取字段目录和规则", "fixedSql 需要人工确认 diff"),
                list("有 ERROR 时先修 SQL；交付前可导出 evidence package。"),
                "README.md#sql-规范闭环"
        ));
        add(map, cap(
                "search-fields", "field", "字段标准检索",
                "按关键词、分类、标签、状态等检索标准字段，返回命中原因和下一步建议。",
                true, "READ_ONLY",
                list("projectId"), list("query", "category", "tag", "status", "sensitive", "sourceBatchId", "limit"),
                list("field-standard-search"),
                list("GET /api/fields/search"),
                list("dataspec search-fields 用户手机号 --project <id> --format json"),
                list(), list("search_fields"),
                list("/fields"),
                list("field"),
                list(), list("create-table", "minimal-context"),
                examples("CLI", "dataspec search-fields 支付金额 --project 1 --limit 20 --format json", null),
                list("不要无 query 拉取整个大字段库", "优先结合 category/tag/status 收窄范围"),
                list("命中不足时创建标准候选或使用反向导入补标准。"),
                "README.md#标准字段模型"
        ));
        add(map, cap(
                "suggest-fields", "field", "字段推荐",
                "根据业务描述或候选字段名推荐标准字段，返回分数、原因和 fallback。",
                true, "READ_ONLY",
                list("projectId", "query"), list("limit"),
                list("field-suggestion"),
                list("GET /api/fields/suggest"),
                list("dataspec suggest-field 用户手机号 --project <id> --format json"),
                list(), list("suggest_fields"),
                list("/fields"),
                list("field"),
                list(), list("create-table"),
                examples("CLI", "dataspec suggest-field 订单总金额 --project 1 --format json", null),
                list("先确认字段库已有基础标准", "敏感字段需读取 sensitive 标记"),
                list("低置信度推荐不要自动采纳，先进入候选或人工确认。"),
                "README.md#字段推荐"
        ));
        add(map, cap(
                "generate-ddl", "sql", "生成 DDL",
                "基于表模板生成 PostgreSQL DDL，并运行 DataSpec lint 自检。",
                true, "WRITES_DATASPEC_RECORD",
                list("projectId", "templateId", "tableName"), list(),
                list("ddl-generate-result", "lint-result"),
                list("GET /api/generator/ddl/preview"),
                list("dataspec generate-ddl --project <id> --template <id> --table <name> --format json"),
                list(), list("generate_table_ddl"),
                list("/generator"),
                list("template", "lint-result"),
                list("create-table"),
                list("create-table"),
                examples("CLI", "dataspec generate-ddl --project 1 --template 5 --table user_order --format json", null),
                list("表模板字段应先维护完整", "生成后查看 lint 自检结果"),
                list("将 DDL 交付前导出 AI Context 或 evidence package。"),
                "README.md#生成与报告"
        ));
        add(map, cap(
                "reverse-import", "database", "数据库反向导入",
                "从现有 PostgreSQL/MySQL metadata 分页扫描、浏览、预览字段候选、差异，并确认导入标准候选。",
                true, "WRITES_DATASPEC_STANDARD",
                list("projectId", "databaseType", "host", "databaseName", "username"),
                list("schemaName", "tableNames", "pageSize", "cursor", "scanId", "cancel"),
                list("database-metadata-scan-result", "database-metadata-browser", "reverse-import-preview", "reverse-import-compare-result"),
                list("POST /api/reverse-import/database/scan", "POST /api/reverse-import/database/browser",
                        "POST /api/reverse-import/database/preview", "POST /api/reverse-import/database/compare",
                        "POST /api/reverse-import/database/import"),
                list("dataspec workflow show reverse-import-standards --format json"),
                list(), list(),
                list("/reverse-import"),
                list("database-metadata-scan-plan", "database-metadata-browser"), list("reverse-import-standards"),
                list("reverse-import"),
                examples("Workflow", "dataspec workflow show reverse-import-standards --format json", null),
                list("使用只读数据库账号", "不要保存密码到仓库", "大库先 scan/browser，再选择当前批次表预览", "确认导入前查看候选和来源"),
                list("大库先调用 scan 获取 cursor 和当前批次表，再 browser/preview/compare，最后把候选纳入标准候选 Inbox 或确认导入。"),
                "README.md#sql-规范闭环"
        ));
        add(map, cap(
                "coverage-report", "database", "字段覆盖率报告",
                "基于 SQL、数据库 metadata 或 schema dump 统计标准覆盖、未纳管和缺注释字段。",
                true, "READ_ONLY",
                list("projectId"), list("sql", "databaseConnection", "dump"),
                list("field-coverage-report"),
                list("POST /api/coverage/sql", "POST /api/coverage/database", "POST /api/coverage/dump"),
                list(), list(), list(),
                list("/coverage"),
                list(), list(), list(),
                examples("API", null, "POST /api/coverage/sql"),
                list("数据库模式使用只读账号", "报告结果不扫描业务数据行"),
                list("根据未纳管 Top 列表跳转反向导入或标准候选。"),
                "README.md#sql-规范闭环"
        ));
        add(map, cap(
                "schema-registry", "contract", "Schema Registry",
                "列出 AI 可依赖的数据契约、稳定字段、兼容策略和废弃字段。",
                false, "READ_ONLY",
                list(), list("contractId"),
                list("schema-registry-catalog", "schema-contract"),
                list("GET /api/contracts", "GET /api/contracts/{contractId}"),
                list("dataspec contract list --format json", "dataspec contract check --format json"),
                list("dataspec://project/<id>/schema-registry"),
                list(), list(),
                list("schema-registry"),
                list(), list(),
                examples("CLI", "dataspec contract check --format json", null),
                list("读取 AI 输出前先确认 contractId/schemaVersion"),
                list("发现契约变化时先更新本地 schema/types，再执行自动化任务。"),
                "README.md#schema-registry-与-ai-契约"
        ));
        add(map, cap(
                "export-evidence-package", "evidence", "导出 AI 执行证据包",
                "从 SQL 检查、AI job、批量任务或覆盖率报告生成 JSON/zip 证据包。",
                true, "READ_ONLY",
                list("projectId", "sourceType"), list("sourceId", "payload", "format"),
                list("ai-evidence-package"),
                list("POST /api/evidence-packages", "POST /api/evidence-packages/download"),
                list("dataspec evidence export --project <id> --source-type SQL_CHECK --source-id 42 --format json"),
                list(), list("export_evidence_package"),
                list("/sql-lint", "/coverage", "/ai-batch"),
                list("ai-evidence-package"),
                list(), list(),
                examples("CLI", "dataspec evidence export --project 1 --source-type SQL_CHECK --source-id 42 --format json", null),
                list("证据包只读", "zip 输出必须指定安全路径"),
                list("完成 SQL 修复、覆盖率或批量任务后导出证据包交付。"),
                "README.md#ai-与自动化"
        ));
        add(map, cap(
                "workflow-recipes", "workflow", "任务化 Workflow Recipes",
                "提供 create-table、review-pr-sql、reverse-import-standards 等 AI 任务步骤和失败恢复。",
                false, "READ_ONLY",
                list(), list("recipeId"),
                list("workflow-recipe"),
                list(),
                list("dataspec workflow list --format json", "dataspec workflow show create-table --format json"),
                list("dataspec://project/<id>/workflow-recipes"),
                list(), list(), list(), list("create-table", "review-pr-sql", "reverse-import-standards", "export-min-context"), list(),
                examples("CLI", "dataspec workflow show create-table --format json", null),
                list("先运行 doctor", "按 recipe 步骤逐项执行，不要跳过失败恢复"),
                list("不确定下一步时先读取 workflow recipe。"),
                "README.md#cli"
        ));
        add(map, cap(
                "ai-task-profiles", "profile", "AI 任务模式",
                "描述不同 AI 任务的上下文范围、ruleset、fixedSql 策略、输出格式和推荐命令。",
                true, "READ_ONLY",
                list("projectId"), list("profileId", "taskType"),
                list("ai-task-profile"),
                list("GET /api/ai-profiles", "GET /api/ai-profiles/{profileOrTaskType}"),
                list("dataspec profile list --project <id> --format json"),
                list("dataspec://project/<id>/ai-task-profiles"),
                list(), list("/ai-profile"),
                list("ai-task-profile"),
                list(), list("create-table", "sql-fix", "reverse-import", "pr-review", "minimal-context"),
                examples("CLI", "dataspec profile show sql-fix --project 1 --format json", null),
                list("profile 只是默认建议，不是权限或审批", "显式参数优先于 profile 默认值"),
                list("根据任务类型选择 profile 后再导出 Context 或 lint。"),
                "README.md#ai-任务模式"
        ));
        add(map, cap(
                "domain-starter-kits", "project", "领域 Starter Kit",
                "把用户、订单、支付、库存和审计等内置领域包应用到项目，快速初始化标准字段。",
                true, "WRITES_DATASPEC_STANDARD",
                list("projectId", "kitKey"), list("kitVersion"),
                list("domain-starter-kit"),
                list("GET /api/starter-kits", "POST /api/starter-kits/apply", "GET /api/starter-kits/installations"),
                list(), list(), list(),
                list("/projects"),
                list("field", "template", "enum-dict"),
                list(), list("create-table"),
                examples("API", null, "POST /api/starter-kits/apply"),
                list("重复应用只补缺失项", "应用前确认目标项目"),
                list("新项目可先应用 starter kit，再导出 AI Context。"),
                "README.md#标准维护"
        ));
        return map;
    }

    private static void add(Map<String, AiCapabilityEntry> map, AiCapabilityEntry entry) {
        map.put(entry.id(), entry);
    }

    private static AiCapabilityEntry cap(
            String id,
            String category,
            String title,
            String summary,
            boolean requiresProject,
            String writeRisk,
            List<String> requiredInputs,
            List<String> optionalInputs,
            List<String> outputContracts,
            List<String> apiEndpoints,
            List<String> cliCommands,
            List<String> mcpResources,
            List<String> mcpTools,
            List<String> frontendRoutes,
            List<String> contractIds,
            List<String> workflowIds,
            List<String> profileIds,
            List<AiCapabilityExample> examples,
            List<String> preflightChecks,
            List<String> nextActions,
            String docsRef
    ) {
        return new AiCapabilityEntry(
                id,
                category,
                title,
                summary,
                "AVAILABLE",
                "stable-ai",
                requiresProject,
                writeRisk,
                requiredInputs,
                optionalInputs,
                outputContracts,
                apiEndpoints,
                cliCommands,
                mcpResources,
                mcpTools,
                frontendRoutes,
                contractIds,
                workflowIds,
                profileIds,
                examples,
                preflightChecks,
                nextActions,
                docsRef
        );
    }

    private static List<String> list(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private static List<AiCapabilityExample> examples(String surface, String command, String request) {
        return List.of(new AiCapabilityExample(
                surface + " 示例",
                surface,
                command,
                request,
                "可复制给 AI 或终端执行前再补齐真实 projectId/token/路径。"
        ));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
