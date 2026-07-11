package com.dataspec.contract.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.contract.model.DeprecatedContractField;
import com.dataspec.contract.model.SchemaCompatibilityPolicy;
import com.dataspec.contract.model.SchemaContract;
import com.dataspec.contract.model.SchemaContractSummary;
import com.dataspec.contract.model.SchemaRegistryCatalog;
import com.dataspec.contract.service.SchemaRegistryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 内置标准契约注册表。
 * <p>
 * 第一版只描述 AI 可依赖的输出结构，不提供运行时编辑能力，也不作为权限边界。
 */
@Service
public class SchemaRegistryServiceImpl implements SchemaRegistryService {

    public static final String REGISTRY_KIND = "dataspec-schema-registry";
    public static final int REGISTRY_SCHEMA_VERSION = 1;
    public static final String REGISTRY_VERSION = "2026.07.11";
    public static final String CONTRACT_SCHEMA_VERSION = "1.0";

    private final SchemaCompatibilityPolicy compatibilityPolicy = SchemaCompatibilityPolicy.builder()
            .level("stable-ai-contract")
            .compatibleSince("0.1.0")
            .additiveFieldPolicy("新增可选字段默认兼容；AI 客户端必须忽略未知字段。")
            .breakingChangePolicy("删除、改名、类型变化或语义变化必须提升 contract schemaVersion，并同步 fixtures、README 和迁移说明。")
            .deprecationPolicy("废弃字段必须继续输出一个兼容窗口，并声明 replacement/removalAfter/reason。")
            .compatibilityWindow("至少一个 P6 小版本或一次明确 migration note。")
            .build();
    private final Map<String, SchemaContract> contracts = builtIns();

    @Override
    public SchemaRegistryCatalog getCatalog() {
        return SchemaRegistryCatalog.builder()
                .kind(REGISTRY_KIND)
                .schemaVersion(REGISTRY_SCHEMA_VERSION)
                .registryVersion(REGISTRY_VERSION)
                .compatibilityPolicy(compatibilityPolicy)
                .contracts(contracts.values().stream().map(this::summary).toList())
                .requiredContractIds(requiredContractIds())
                .nextActions(List.of(
                        "AI 在读取 field-catalog、lint result 或 task profile 前，先确认 contractId 与 schemaVersion。",
                        "如果发现 stableFields 缺失或 schemaVersion 变化，先查看 docsRef 并重新生成上下文。"
                ))
                .build();
    }

    @Override
    public SchemaContract getContract(String contractId) {
        String normalized = normalize(contractId);
        SchemaContract contract = normalized == null ? null : contracts.get(normalized);
        if (contract == null) {
            throw new BizException(404, "未知标准契约: " + contractId + "。支持的 contractId: " + String.join(", ", contracts.keySet()));
        }
        return contract;
    }

    @Override
    public List<String> requiredContractIds() {
        return List.copyOf(contracts.keySet());
    }

    @Override
    public Map<String, Object> manifestSummary() {
        return orderedMap(
                "schemaVersion", REGISTRY_SCHEMA_VERSION,
                "registryVersion", REGISTRY_VERSION,
                "file", REGISTRY_FILE,
                "contractIds", requiredContractIds()
        );
    }

    private SchemaContractSummary summary(SchemaContract contract) {
        return SchemaContractSummary.builder()
                .contractId(contract.getContractId())
                .displayName(contract.getDisplayName())
                .description(contract.getDescription())
                .schemaVersion(contract.getSchemaVersion())
                .jsonSchemaRef(contract.getJsonSchemaRef())
                .stableFields(contract.getStableFields())
                .deprecatedFields(contract.getDeprecatedFields())
                .compatibility(contract.getCompatibility())
                .docsRef(contract.getDocsRef())
                .build();
    }

    private Map<String, SchemaContract> builtIns() {
        Map<String, SchemaContract> map = new LinkedHashMap<>();
        add(map, contract(
                "field",
                "标准字段",
                "字段库和 AI Context 中的标准字段结构。",
                List.of("stableRef", "canonicalRef", "aliasHistory[]", "replacementRef",
                        "name", "dataType", "nullable", "comment", "displayName", "category", "tags",
                        "codeSetId", "sensitive", "status", "replacementFieldId", "replacementReason",
                        "example", "aliases[]", "matchReasons[]"),
                List.of(),
                objectSchema("DataSpec Field", List.of("name", "dataType"), orderedMap(
                        "stableRef", describedStringProp("项目内稳定字段引用，格式为 field:<projectId>:<fieldId>。"),
                        "canonicalRef", describedStringProp("当前推荐字段引用；废弃字段有替代时指向 replacementRef。"),
                        "aliasHistory", arrayOf(objectSchema("Field Alias History", List.of("alias", "source"), orderedMap(
                                "alias", describedStringProp("已脱敏别名或历史名。"),
                                "source", describedStringProp("别名来源，如 current-alias。"),
                                "confidence", describedStringProp("别名派生置信度。")
                        ))),
                        "replacementRef", describedStringProp("废弃、停用或合并字段的替代 stableRef。"),
                        "name", stringProp(),
                        "dataType", stringProp(),
                        "nullable", booleanProp(),
                        "comment", stringProp(),
                        "displayName", stringProp(),
                        "category", stringProp(),
                        "tags", arrayOf(stringProp()),
                        "codeSetId", integerProp(),
                        "sensitive", booleanProp(),
                        "status", enumProp("draft", "enabled", "disabled", "deprecated"),
                        "replacementFieldId", integerProp(),
                        "replacementReason", stringProp(),
                        "example", stringProp(),
                        "aliases", arrayOf(stringProp()),
                        "matchReasons", arrayOf(stringProp())
                )),
                List.of(orderedMap(
                        "name", "mobile_no",
                        "dataType", "varchar(20)",
                        "nullable", false,
                        "status", "enabled",
                        "stableRef", "field:1:100",
                        "canonicalRef", "field:1:100",
                        "aliases", List.of("phone", "mobile")
                ))
        ));
        add(map, contract(
                "standard-field-merge",
                "标准字段合并",
                "正式标准字段合并预览和确认结果结构。",
                List.of("kind", "schemaVersion", "projectId", "recommendedTargetFieldId",
                        "target", "source", "targetAfter", "sourceAfter", "changes[]", "risks[]",
                        "impactItems[]", "rollbackHints[]", "nextActions[]", "applied", "preview",
                        "risks[].code", "risks[].blocking", "rollbackHints[].targetPath"),
                List.of(),
                objectSchema("DataSpec Standard Field Merge", List.of("kind", "schemaVersion", "projectId"), orderedMap(
                        "kind", enumProp("standard_field_merge_preview", "standard_field_merge_result"),
                        "schemaVersion", integerProp(),
                        "projectId", integerProp(),
                        "recommendedTargetFieldId", integerProp(),
                        "target", objectProp(),
                        "source", objectProp(),
                        "targetAfter", objectProp(),
                        "sourceAfter", objectProp(),
                        "changes", arrayOf(objectProp()),
                        "risks", arrayOf(objectSchema("Merge Risk", List.of("code", "blocking"), orderedMap(
                                "severity", enumProp("ERROR", "WARNING", "INFO"),
                                "code", stringProp(),
                                "message", stringProp(),
                                "blocking", booleanProp(),
                                "manualAction", stringProp()
                        ))),
                        "impactItems", arrayOf(objectProp()),
                        "rollbackHints", arrayOf(objectSchema("Merge Rollback Hint", List.of("targetPath"), orderedMap(
                                "type", stringProp(),
                                "action", stringProp(),
                                "description", stringProp(),
                                "targetPath", stringProp()
                        ))),
                        "nextActions", arrayOf(stringProp()),
                        "applied", booleanProp(),
                        "preview", objectProp()
                )),
                List.of(orderedMap(
                        "kind", "standard_field_merge_preview",
                        "schemaVersion", 1,
                        "projectId", 1,
                        "target", orderedMap("name", "mobile_no"),
                        "source", orderedMap("name", "user_mobile"),
                        "risks", List.of(orderedMap("code", "NULLABILITY_MISMATCH", "blocking", false)),
                        "rollbackHints", List.of(orderedMap("targetPath", "/api/fields/10/undo?logId=<changeLogId>"))
                ), orderedMap(
                        "kind", "standard_field_merge_result",
                        "schemaVersion", 1,
                        "projectId", 1,
                        "applied", true,
                        "preview", orderedMap("kind", "standard_field_merge_preview"),
                        "rollbackHints", List.of(orderedMap("targetPath", "/api/fields/10/undo?logId=<changeLogId>"))
                ))
        ));
        add(map, contract(
                "enum-dict",
                "枚举字典",
                "项目枚举字典及其枚举值结构。",
                List.of("stableRef", "canonicalRef", "code", "name", "valueType", "values[].value", "values[].label", "values[].sortOrder"),
                List.of(),
                objectSchema("DataSpec Enum Dict", List.of("code", "name", "values"), orderedMap(
                        "stableRef", describedStringProp("项目内稳定枚举代码集引用，格式为 enum:<projectId>:<codeSetId>。"),
                        "canonicalRef", describedStringProp("当前规范枚举代码集引用；第一版等于 stableRef。"),
                        "code", stringProp(),
                        "name", stringProp(),
                        "valueType", stringProp(),
                        "values", arrayOf(objectSchema("Enum Value", List.of("value", "label"), orderedMap(
                                "value", stringProp(),
                                "label", stringProp(),
                                "sortOrder", integerProp()
                        )))
                )),
                List.of(orderedMap(
                        "code", "order_status",
                        "name", "订单状态",
                        "valueType", "string",
                        "stableRef", "enum:1:10",
                        "canonicalRef", "enum:1:10",
                        "values", List.of(orderedMap("value", "PAID", "label", "已支付", "sortOrder", 1))
                ))
        ));
        add(map, contract(
                "rule-config",
                "规则配置",
                "SQL lint 与 AI Context rules.yaml 使用的规则配置结构。",
                List.of("ruleCode", "ruleName", "severity", "enabled", "paramsJson"),
                List.of(),
                objectSchema("DataSpec Rule Config", List.of("ruleCode", "ruleName", "severity"), orderedMap(
                        "ruleCode", stringProp(),
                        "ruleName", stringProp(),
                        "severity", enumProp("ERROR", "WARNING", "SUGGESTION"),
                        "enabled", booleanProp(),
                        "paramsJson", stringProp()
                )),
                List.of(orderedMap(
                        "ruleCode", "field_naming_snake_case",
                        "ruleName", "字段 snake_case",
                        "severity", "ERROR",
                        "enabled", true
                ))
        ));
        add(map, contract(
                "template",
                "表模板",
                "DDL 生成和数据字典使用的表模板结构。",
                List.of("id", "projectId", "name", "tableName", "description", "fields[].fieldName",
                        "fields[].dataType", "fields[].nullable", "fields[].comment"),
                List.of(),
                objectSchema("DataSpec Template", List.of("name", "tableName"), orderedMap(
                        "id", integerProp(),
                        "projectId", integerProp(),
                        "name", stringProp(),
                        "tableName", stringProp(),
                        "description", stringProp(),
                        "fields", arrayOf(objectSchema("Template Field", List.of("fieldName", "dataType"), orderedMap(
                                "fieldName", stringProp(),
                                "dataType", stringProp(),
                                "nullable", booleanProp(),
                                "comment", stringProp()
                        )))
                )),
                List.of(orderedMap("name", "用户表模板", "tableName", "users"))
        ));
        add(map, contract(
                "standard-snapshot",
                "标准快照",
                "标准字段、枚举和规则的可复现版本元数据。",
                List.of("snapshotId", "projectId", "specVersion", "specHash", "source", "versioned"),
                List.of(),
                objectSchema("DataSpec Standard Snapshot", List.of("specVersion", "versioned"), orderedMap(
                        "snapshotId", integerProp(),
                        "projectId", integerProp(),
                        "specVersion", stringProp(),
                        "specHash", stringProp(),
                        "source", enumProp("current", "snapshot", "unversioned"),
                        "versioned", booleanProp()
                )),
                List.of(orderedMap("specVersion", "v2026.06.28", "specHash", "hash", "versioned", true))
        ));
        add(map, contract(
                "lint-result",
                "SQL 校验结果",
                "SQL lint、fixedSql、修复计划和方言诊断输出结构。",
                List.of("tables[]", "issues[]", "errorCount", "warningCount", "suggestionCount",
                        "fixedSql", "fixedSqlDiff", "fixPolicy", "fixChanges[]", "dialectDiagnostics[]"),
                List.of(),
                objectSchema("DataSpec Lint Result", List.of("issues", "errorCount", "warningCount", "suggestionCount"), orderedMap(
                        "tables", arrayOf(objectProp()),
                        "issues", arrayOf(objectSchema("Lint Issue", List.of("severity", "ruleCode", "message"), orderedMap(
                                "severity", enumProp("ERROR", "WARNING", "SUGGESTION"),
                                "ruleCode", stringProp(),
                                "ruleName", stringProp(),
                                "message", stringProp(),
                                "tableName", stringProp(),
                                "columnName", stringProp(),
                                "suggestion", stringProp()
                        ))),
                        "errorCount", integerProp(),
                        "warningCount", integerProp(),
                        "suggestionCount", integerProp(),
                        "fixedSql", stringProp(),
                        "fixedSqlDiff", stringProp(),
                        "fixPolicy", objectProp(),
                        "fixChanges", arrayOf(objectProp()),
                        "dialectDiagnostics", arrayOf(objectProp())
                )),
                List.of(orderedMap("errorCount", 1, "warningCount", 0, "suggestionCount", 0))
        ));
        add(map, contract(
                "sql-rule-debug-result",
                "SQL 规则调试结果",
                "SQL 规则启用、匹配 trace、source range、fixedSql 策略和豁免状态的只读解释结构。",
                List.of("debugVersion", "lintResult", "rules[]", "debugNotes[]",
                        "rules[].ruleCode", "rules[].enabled", "rules[].paramsSnapshot",
                        "rules[].matchTrace[]", "rules[].sourceRange", "rules[].fixStrategy",
                        "rules[].suppressionStatus"),
                List.of(),
                objectSchema("DataSpec SQL Rule Debug Result", List.of("debugVersion", "lintResult", "rules"), orderedMap(
                        "debugVersion", stringProp(),
                        "lintResult", objectProp(),
                        "rules", arrayOf(objectSchema("SQL Rule Debug Trace", List.of("ruleCode", "ruleName", "enabled", "matchTrace"), orderedMap(
                                "ruleCode", stringProp(),
                                "ruleName", stringProp(),
                                "enabled", booleanProp(),
                                "severity", enumProp("ERROR", "WARNING", "SUGGESTION"),
                                "paramsSnapshot", objectProp(),
                                "matchTrace", arrayOf(objectSchema("SQL Rule Match Trace", List.of("status", "message"), orderedMap(
                                        "status", enumProp("MATCHED", "NO_MATCH", "DISABLED", "UNPARSED", "ERROR"),
                                        "message", stringProp(),
                                        "severity", enumProp("ERROR", "WARNING", "SUGGESTION"),
                                        "issueMessage", stringProp(),
                                        "tableName", stringProp(),
                                        "columnName", stringProp(),
                                        "sourceRange", objectProp(),
                                        "fixStatus", enumProp("APPLIED", "PLANNED", "SKIPPED"),
                                        "fixReasonCode", stringProp(),
                                        "suppressionId", integerProp()
                                ))),
                                "sourceRange", objectProp(),
                                "fixStrategy", objectProp(),
                                "suppressionStatus", objectProp(),
                                "debugNotes", arrayOf(stringProp())
                        ))),
                        "debugNotes", arrayOf(stringProp())
                )),
                List.of(orderedMap(
                        "debugVersion", "sql-rule-debug@1",
                        "lintResult", orderedMap("errorCount", 1, "warningCount", 0, "suggestionCount", 0),
                        "rules", List.of(orderedMap(
                                "ruleCode", "table_naming_snake_case",
                                "ruleName", "表名 snake_case",
                                "enabled", true,
                                "matchTrace", List.of(orderedMap("status", "MATCHED", "message", "规则命中 lint issue。")),
                                "suppressionStatus", orderedMap("activeIssueCount", 1, "suppressedIssueCount", 0)
                        ))
                ))
        ));
        add(map, contract(
                "ai-evidence-package",
                "AI 执行证据包",
                "AI 任务交付、复盘和下游续跑使用的只读 evidence package 结构。",
                List.of("kind", "schemaVersion", "packageId", "projectId", "generatedAt", "source",
                        "standardSnapshot", "inputsSummary", "outputsSummary", "validationSummary",
                        "postCheckSummary", "postCheckSummary.status", "postCheckSummary.safeToUse",
                        "postCheckSummary.issueCounts", "postCheckSummary.blockingRefs[]",
                        "postCheckSummary.replacementRefs[]", "postCheckSummary.evidenceLinks[]",
                        "postCheckSummary.suggestedCheckCommand",
                        "artifacts[]", "nextActions[]", "suggestedCommands[]", "diagnostics[]"),
                List.of(),
                objectSchema("DataSpec AI Evidence Package",
                        List.of("kind", "schemaVersion", "source", "validationSummary", "artifacts", "nextActions", "suggestedCommands"),
                        orderedMap(
                                "kind", stringProp(),
                                "schemaVersion", integerProp(),
                                "packageId", stringProp(),
                                "projectId", integerProp(),
                                "generatedAt", stringProp(),
                                "source", objectSchema("Evidence Source", List.of("sourceType", "persisted"), orderedMap(
                                        "sourceType", enumProp("AI_JOB", "SQL_CHECK", "COVERAGE_REPORT", "AI_BATCH_RUN", "AI_TASK_RUN"),
                                        "sourceId", integerProp(),
                                        "sourceTitle", stringProp(),
                                        "status", stringProp(),
                                        "persisted", booleanProp()
                                )),
                                "standardSnapshot", objectProp(),
                                "inputsSummary", objectProp(),
                                "outputsSummary", objectProp(),
                                "validationSummary", objectProp(),
                                "postCheckSummary", objectSchema("Evidence Post-check Summary", List.of(), orderedMap(
                                        "status", describedEnumProp("AI 输出后置校验状态。", "PASS", "WARN", "FAIL"),
                                        "safeToUse", describedBooleanProp("是否可直接复制、下载、应用或执行。"),
                                        "issueCounts", objectProp(),
                                        "blockingRefs", arrayOf(describedStringProp("阻断继续使用的脱敏 stableRef 或输入引用。")),
                                        "replacementRefs", arrayOf(describedStringProp("建议替代的 stableRef。")),
                                        "evidenceLinks", arrayOf(describedStringProp("只读证据链接，secret-safe。")),
                                        "suggestedCheckCommand", describedStringProp("建议复跑后置校验的 CLI 命令。")
                                )),
                                "artifacts", arrayOf(objectProp()),
                                "nextActions", arrayOf(stringProp()),
                                "suggestedCommands", arrayOf(stringProp()),
                                "diagnostics", arrayOf(objectProp())
                        )),
                List.of(orderedMap(
                        "kind", "dataspec-ai-evidence-package",
                        "schemaVersion", 1,
                        "source", orderedMap("sourceType", "SQL_CHECK", "sourceId", 42, "persisted", true),
                        "validationSummary", orderedMap("status", "COMPLETED"),
                        "postCheckSummary", orderedMap(
                                "status", "WARN",
                                "safeToUse", false,
                                "blockingRefs", List.of(),
                                "replacementRefs", List.of("field:1:100"),
                                "suggestedCheckCommand", "dataspec ai-output check --project 1 --type SQL --file output.sql --format json"
                        ),
                        "artifacts", List.of(orderedMap("artifactType", "fixed-sql", "format", "sql")),
                        "nextActions", List.of("复核 fixedSql 后再应用补丁。"),
                        "suggestedCommands", List.of("dataspec evidence export --source-type SQL_CHECK --source-id 42 --format zip --output evidence.zip")
                ))
        ));
        add(map, contract(
                "ai-context-manifest",
                "AI Context Manifest",
                "AI Context zip 的入口 manifest，用于描述标准版本、文件清单、命令和契约版本。",
                List.of("kind", "schemaVersion", "projectId", "standard", "contextScope", "contracts", "files[]",
                        "commands", "commands.postCheck"),
                List.of(),
                objectSchema("DataSpec AI Context Manifest", List.of("kind", "schemaVersion", "projectId", "files"), orderedMap(
                        "kind", stringProp(),
                        "schemaVersion", integerProp(),
                        "projectId", integerProp(),
                        "standard", objectProp(),
                        "contextScope", objectProp(),
                        "contracts", objectProp(),
                        "files", arrayOf(stringProp()),
                        "commands", objectProp()
                )),
                List.of(orderedMap("kind", "dataspec-ai-context", "schemaVersion", 1, "projectId", 1))
        ));
        add(map, contract(
                "ai-context-field-catalog",
                "AI Context Field Catalog",
                "AI Context 中的字段目录和按需裁剪元数据。",
                List.of("projectId", "standard", "contextScope", "fields[]", "enums[]",
                        "fields[].stableRef", "fields[].canonicalRef", "fields[].aliasHistory[]", "fields[].replacementRef",
                        "fields[].name", "fields[].dataType", "fields[].status",
                        "enums[].stableRef", "enums[].canonicalRef",
                        "usageExamples[]", "usageExampleSummary"),
                List.of(),
                objectSchema("DataSpec AI Context Field Catalog", List.of("projectId", "fields", "enums"), orderedMap(
                        "projectId", integerProp(),
                        "standard", objectProp(),
                        "contextScope", objectProp(),
                        "fields", arrayOf(objectProp()),
                        "enums", arrayOf(objectProp()),
                        "usageExamples", arrayOf(objectProp()),
                        "usageExampleSummary", objectProp()
                )),
                List.of(orderedMap(
                        "projectId", 1,
                        "fields", List.of(orderedMap("name", "mobile_no")),
                        "enums", List.of(),
                        "usageExamples", List.of(orderedMap("scope", "FIELD", "exampleType", "GOOD")),
                        "usageExampleSummary", orderedMap("totalExamples", 1)
                ))
        ));
        add(map, contract(
                "ai-task-profile",
                "AI 任务模式",
                "AI task profile 的上下文范围、规则集、fixedSql 策略和输出格式。",
                List.of("profileId", "taskType", "contextScope", "ruleset", "fixedSqlPolicy",
                        "outputFormat", "recommendedCommands[]", "nextActions[]"),
                List.of(),
                objectSchema("DataSpec AI Task Profile", List.of("profileId", "taskType"), orderedMap(
                        "profileId", stringProp(),
                        "taskType", stringProp(),
                        "contextScope", objectProp(),
                        "ruleset", objectProp(),
                        "fixedSqlPolicy", objectProp(),
                        "outputFormat", objectProp(),
                        "recommendedCommands", arrayOf(stringProp()),
                        "nextActions", arrayOf(stringProp())
                )),
                List.of(orderedMap("profileId", "sql-fix", "taskType", "SQL_FIX"))
        ));
        add(map, contract(
                "standard-reference-resolution",
                "标准引用解析结果",
                "标准对象 stableRef、canonicalRef、生命周期状态和别名解析结果；契约只读、project-scoped 且 secret-safe。",
                List.of("kind", "schemaVersion", "projectId", "results[]", "warnings[]",
                        "results[].inputRef", "results[].refType", "results[].resolutionStatus",
                        "results[].stableRef", "results[].canonicalRef", "results[].objectId",
                        "results[].currentName", "results[].matchedAlias", "results[].lifecycleStatus",
                        "results[].replacementRef", "results[].confidence", "results[].evidenceLinks[]",
                        "results[].warnings[]"),
                List.of(),
                objectSchema("DataSpec Standard Reference Resolution",
                        List.of("kind", "schemaVersion", "projectId", "results"),
                        orderedMap(
                                "kind", describedStringProp("稳定响应类型标识，固定为 dataspec-standard-reference-resolution。"),
                                "schemaVersion", describedIntegerProp("响应 schema 版本；breaking 变更必须升级。"),
                                "projectId", describedIntegerProp("当前解析所在项目。"),
                                "results", arrayOf(objectSchema("Reference Resolution Result",
                                        List.of("inputRef", "refType", "resolutionStatus", "confidence"),
                                        orderedMap(
                                                "inputRef", describedStringProp("脱敏后的输入引用；secret-safe，不得包含 token、password、Authorization、JDBC URL 或 DSN。"),
                                                "refType", describedEnumProp("标准对象类型。", "FIELD", "ENUM", "RULE", "SNAPSHOT"),
                                                "resolutionStatus", describedEnumProp("解析状态；UNKNOWN/AMBIGUOUS/CROSS_PROJECT 时不得猜测 canonical 对象。",
                                                        "CURRENT", "STALE", "AMBIGUOUS", "UNKNOWN", "CROSS_PROJECT"),
                                                "stableRef", describedStringProp("命中的 project-scoped stableRef。"),
                                                "canonicalRef", describedStringProp("当前推荐 canonical stableRef。"),
                                                "objectId", describedIntegerProp("当前项目对象 ID；规则或不可暴露对象可为空。"),
                                                "currentName", describedStringProp("当前标准名称、编码、规则码或快照版本，输出前已脱敏。"),
                                                "matchedAlias", describedStringProp("命中的别名或历史名，输出前已脱敏。"),
                                                "lifecycleStatus", describedStringProp("字段生命周期或对象可用状态。"),
                                                "replacementRef", describedStringProp("废弃、停用或合并对象的替代 stableRef。"),
                                                "confidence", describedEnumProp("解析置信度；stableRef 和精确名称通常为 HIGH，派生别名或文本提示可降为 MEDIUM/LOW。",
                                                        "HIGH", "MEDIUM", "LOW"),
                                                "evidenceLinks", arrayOf(describedStringProp("只读证据链接或对象定位符，secret-safe。")),
                                                "warnings", arrayOf(describedStringProp("脱敏诊断和下一步提示。"))
                                        ))),
                                "warnings", arrayOf(describedStringProp("批量解析级别脱敏提示。"))
                        )),
                List.of(orderedMap(
                        "kind", "dataspec-standard-reference-resolution",
                        "schemaVersion", 1,
                        "projectId", 1,
                        "results", List.of(orderedMap(
                                "inputRef", "phone",
                                "refType", "FIELD",
                                "resolutionStatus", "CURRENT",
                                "stableRef", "field:1:100",
                                "canonicalRef", "field:1:100",
                                "objectId", 100,
                                "currentName", "mobile_no",
                                "matchedAlias", "phone",
                                "confidence", "MEDIUM",
                                "evidenceLinks", List.of("dataspec://fields/100")
                        ))
                ))
        ));
        add(map, contract(
                "ai-output-post-check-result",
                "AI 输出后置校验结果",
                "AI 生成 SQL、DDL、Markdown、JSON 或文本后的确定性后置校验结果；只读、bounded 且 secret-safe。",
                List.of("kind", "schemaVersion", "projectId", "status", "safeToUse", "summary",
                        "issues[]", "resolvedRefs[]", "suggestedFixes[]", "evidenceLinks[]", "nextActions[]",
                        "issues[].code", "issues[].severity", "issues[].inputRef", "issues[].replacementRef"),
                List.of(),
                objectSchema("DataSpec AI Output Post-check Result",
                        List.of("kind", "schemaVersion", "projectId", "status", "safeToUse", "summary", "issues", "resolvedRefs"),
                        orderedMap(
                                "kind", describedStringProp("稳定响应类型标识，固定为 dataspec-ai-output-postcheck。"),
                                "schemaVersion", describedIntegerProp("结果 schema 版本；breaking 变更必须升级。"),
                                "projectId", describedIntegerProp("当前项目 ID。"),
                                "status", describedEnumProp("PASS/WARN/FAIL 总体状态；只有 PASS 可自动继续。",
                                        "PASS", "WARN", "FAIL"),
                                "safeToUse", describedBooleanProp("是否可直接复制、下载、应用或执行；只有 PASS 为 true。"),
                                "summary", objectSchema("Post-check Summary", List.of("totalRefCount", "issueCount"), orderedMap(
                                        "totalRefCount", describedIntegerProp("提取到并进入标准解析的引用总数。"),
                                        "currentCount", describedIntegerProp("当前可用标准对象引用数。"),
                                        "staleCount", describedIntegerProp("废弃、停用或替代对象引用数。"),
                                        "unknownCount", describedIntegerProp("未知引用数。"),
                                        "ambiguousCount", describedIntegerProp("歧义引用数。"),
                                        "crossProjectCount", describedIntegerProp("跨项目引用数。"),
                                        "issueCount", describedIntegerProp("问题总数，包含标准引用问题和证据缺口。")
                                )),
                                "issues", arrayOf(objectSchema("Post-check Issue",
                                        List.of("code", "severity", "message"),
                                        orderedMap(
                                                "code", describedStringProp("稳定问题码，如 UNKNOWN_STANDARD_REFERENCE、STALE_STANDARD_REFERENCE 或 EVIDENCE_GAP。"),
                                                "severity", describedEnumProp("问题级别；FAIL 会阻断使用，WARN 需要人工确认或补证据。",
                                                        "WARN", "FAIL"),
                                                "refType", describedEnumProp("相关标准对象类型。", "FIELD", "ENUM", "RULE", "SNAPSHOT"),
                                                "inputRef", describedStringProp("脱敏后的输入引用；secret-safe。"),
                                                "message", describedStringProp("脱敏诊断说明。"),
                                                "excerpt", describedStringProp("有界脱敏原文片段，不保存 raw AI output。"),
                                                "replacementRef", describedStringProp("可用替代 stableRef。"),
                                                "evidenceLinks", arrayOf(describedStringProp("相关只读证据链接。")),
                                                "nextActions", arrayOf(describedStringProp("建议的下一步动作。"))
                                        ))),
                                "resolvedRefs", arrayOf(objectProp()),
                                "suggestedFixes", arrayOf(describedStringProp("脱敏修复建议摘要。")),
                                "evidenceLinks", arrayOf(describedStringProp("可复核的只读证据链接。")),
                                "nextActions", arrayOf(describedStringProp("AI/用户下一步动作。"))
                        )),
                List.of(orderedMap(
                        "kind", "dataspec-ai-output-postcheck",
                        "schemaVersion", 1,
                        "projectId", 1,
                        "status", "WARN",
                        "safeToUse", false,
                        "summary", orderedMap(
                                "totalRefCount", 1,
                                "currentCount", 0,
                                "staleCount", 1,
                                "unknownCount", 0,
                                "ambiguousCount", 0,
                                "crossProjectCount", 0,
                                "issueCount", 1),
                        "issues", List.of(orderedMap(
                                "code", "STALE_STANDARD_REFERENCE",
                                "severity", "WARN",
                                "inputRef", "old_mobile_no",
                                "replacementRef", "field:1:100"
                        )),
                        "resolvedRefs", List.of(orderedMap("stableRef", "field:1:99", "canonicalRef", "field:1:100")),
                        "nextActions", List.of("先替换为 canonicalRef 后再复制或执行。")
                ))
        ));
        add(map, contract(
                "standard-query-dsl-request",
                "Standard Query DSL 请求",
                "项目内只读 Standard Query DSL 请求结构；v1 仅执行 FIELD，并要求所有 query/filter 输入 secret-safe。",
                List.of("projectId", "target", "text", "filters[]", "filters[].field", "filters[].op", "filters[].value",
                        "sort[]", "limit", "strict", "explain"),
                List.of(),
                objectSchema("DataSpec Standard Query DSL Request", List.of("projectId"), orderedMap(
                        "projectId", describedIntegerProp("当前 DataSpec 项目 ID；查询必须 project-scoped。"),
                        "target", describedEnumProp("标准对象类型；v1 仅支持 FIELD，缺省按 FIELD 处理。", "FIELD"),
                        "text", describedStringProp("自然语言或字段名检索文本；视为敏感输入，输出摘要和错误必须脱敏，secret-safe。"),
                        "filters", arrayOf(objectSchema("Standard Query Filter", List.of("field"), orderedMap(
                                "field", describedEnumProp("allowlist 过滤字段。", "category", "tag", "status", "sensitive", "sourceBatchId", "stableRef", "canonicalRef", "hasExample", "updatedSince"),
                                "op", describedEnumProp("allowlist 操作符；字段默认操作符可省略。", "eq", "contains", "gte"),
                                "value", describedValueProp("过滤值；按敏感输入处理，错误和摘要只能输出脱敏值。")
                        ))),
                        "sort", arrayOf(describedStringProp("预留排序字段；v1 保持字段搜索既有排序。")),
                        "limit", describedIntegerProp("返回上限；v1 允许 1 到 50。bounds: min=1, max=50。"),
                        "strict", describedBooleanProp("严格模式；true 时不支持 target/filter/op/value 会在执行前失败。"),
                        "explain", describedBooleanProp("是否请求解释；v1 始终返回 querySummary、appliedFilters、ignoredFilters、nextQueryHints。"),
                        "supportedFields", arrayOf(describedStringProp("支持字段说明；用于 AI 发现可用筛选。")),
                        "bounds", describedStringProp("输入边界摘要，包括最大文本长度、最大过滤数量、最大 limit 和 secret-safety 约束。")
                )),
                List.of(orderedMap(
                        "projectId", 1,
                        "target", "FIELD",
                        "text", "订单金额",
                        "filters", List.of(
                                orderedMap("field", "category", "op", "eq", "value", "money"),
                                orderedMap("field", "sensitive", "op", "eq", "value", false)
                        ),
                        "limit", 20,
                        "strict", false,
                        "explain", true
                ))
        ));
        add(map, contract(
                "standard-query-dsl-result",
                "Standard Query DSL 结果",
                "项目内只读 Standard Query DSL 查询结果；包含归一化查询、可解释摘要、应用/忽略过滤和字段命中。",
                List.of("projectId", "normalizedQuery", "querySummary", "appliedFilters[]", "ignoredFilters[]",
                        "resultCount", "returnedCount", "truncated", "nextQueryHints[]", "fields[]",
                        "validationError.supportedFields", "validationError.bounds"),
                List.of(),
                objectSchema("DataSpec Standard Query DSL Result", List.of("projectId", "normalizedQuery", "querySummary"), orderedMap(
                        "projectId", describedIntegerProp("当前 DataSpec 项目 ID。"),
                        "normalizedQuery", objectSchema("Normalized Standard Query", List.of("target", "limit"), orderedMap(
                                "target", describedEnumProp("归一化标准对象类型；v1 为 FIELD。", "FIELD"),
                                "text", describedStringProp("归一化并脱敏后的检索文本。"),
                                "filters", arrayOf(objectProp()),
                                "sort", arrayOf(describedStringProp("已接受的排序字段；v1 为空列表。")),
                                "limit", describedIntegerProp("生效返回上限。"),
                                "strict", describedBooleanProp("是否严格校验。"),
                                "explain", describedBooleanProp("是否返回解释信息。")
                        )),
                        "querySummary", objectSchema("Standard Query Summary", List.of("target", "resultCount", "returnedCount", "truncated"), orderedMap(
                                "target", describedEnumProp("查询目标。", "FIELD"),
                                "text", describedStringProp("脱敏后的检索文本。"),
                                "resultCount", describedIntegerProp("命中总数。"),
                                "returnedCount", describedIntegerProp("返回条数。"),
                                "truncated", describedBooleanProp("是否因 limit 截断。"),
                                "nextQueryHints", arrayOf(describedStringProp("下一步查询建议；secret-safety: 不得包含 raw secret。"))
                        )),
                        "appliedFilters", arrayOf(objectSchema("Applied Filter", List.of("field", "op", "redactedValue"), orderedMap(
                                "field", describedStringProp("生效 allowlist 字段名。"),
                                "op", describedStringProp("生效操作符。"),
                                "redactedValue", describedStringProp("脱敏后的过滤值。"),
                                "description", describedStringProp("过滤语义说明。")
                        ))),
                        "ignoredFilters", arrayOf(objectSchema("Ignored Filter", List.of("field", "reason"), orderedMap(
                                "field", describedStringProp("原始过滤字段名。"),
                                "op", describedStringProp("原始操作符。"),
                                "redactedValue", describedStringProp("脱敏后的过滤值。"),
                                "reason", describedStringProp("脱敏后的忽略原因。")
                        ))),
                        "resultCount", describedIntegerProp("命中总数。"),
                        "returnedCount", describedIntegerProp("返回条数。"),
                        "truncated", describedBooleanProp("是否因 limit 截断。"),
                        "nextQueryHints", arrayOf(describedStringProp("下一步查询建议。")),
                        "fields", arrayOf(objectProp()),
                        "validationError", objectSchema("Standard Query Validation Error", List.of("code", "message"), orderedMap(
                                "code", describedStringProp("稳定错误码。"),
                                "message", describedStringProp("脱敏后的错误信息。"),
                                "supportedFields", arrayOf(describedStringProp("当前支持字段。")),
                                "supportedOperators", arrayOf(describedStringProp("当前支持操作符。")),
                                "bounds", describedStringProp("bounds 和 secret-safety 约束摘要。")
                        ))
                )),
                List.of(orderedMap(
                        "projectId", 1,
                        "normalizedQuery", orderedMap("target", "FIELD", "text", "订单金额", "limit", 20),
                        "querySummary", orderedMap("target", "FIELD", "resultCount", 1, "returnedCount", 1, "truncated", false),
                        "appliedFilters", List.of(orderedMap("field", "category", "op", "eq", "redactedValue", "money")),
                        "ignoredFilters", List.of(),
                        "resultCount", 1,
                        "returnedCount", 1,
                        "truncated", false,
                        "nextQueryHints", List.of()
                ))
        ));
        add(map, contract(
                "business-object-standard",
                "业务对象标准",
                "项目级业务对象与表模板依赖标准；所有文本字段必须 secret-safe，不保存业务数据行或连接凭据。",
                List.of("id", "projectId", "objectKey", "entityName", "tablePattern", "templateId",
                        "requiredFields[]", "optionalFields[]", "relations[]", "foreignKeyHints[]",
                        "auditFields", "commonPitfalls[]", "aiUsageNotes", "contextExport", "status"),
                List.of(),
                objectSchema("DataSpec Business Object Standard", List.of("projectId", "objectKey", "entityName"), orderedMap(
                        "id", describedIntegerProp("业务对象标准 ID。nullable: false on persisted response。"),
                        "projectId", describedIntegerProp("所属项目 ID；必须按项目授权边界读写。"),
                        "objectKey", describedStringProp("项目内唯一业务对象键；safe identifier，不含 secret 或业务数据行。"),
                        "entityName", describedStringProp("人可读业务实体名称；项目内唯一，输出前按敏感文本规则脱敏。"),
                        "tablePattern", describedStringProp("推荐表名模式或前缀；nullable，只作为 DDL preview/AI guidance。"),
                        "templateId", describedIntegerProp("可选关联表模板 ID；nullable，必须属于同一项目。"),
                        "requiredFields", arrayOf(describedStringProp("必选字段名或 stableRef；secret-safe。")),
                        "optionalFields", arrayOf(describedStringProp("可选字段名或 stableRef；secret-safe。")),
                        "relations", arrayOf(objectProp()),
                        "foreignKeyHints", arrayOf(objectProp()),
                        "auditFields", objectProp(),
                        "commonPitfalls", arrayOf(describedStringProp("常见反模式说明；不得包含 raw SQL secret、token、JDBC URL 或 DSN。")),
                        "aiUsageNotes", describedStringProp("AI 使用说明；按不可信业务内容处理并脱敏。"),
                        "contextExport", describedBooleanProp("是否默认导出到 AI Context table-standards.json。"),
                        "status", describedEnumProp("对象状态。", "ENABLED", "DISABLED")
                )),
                List.of(orderedMap(
                        "id", 10,
                        "projectId", 1,
                        "objectKey", "order",
                        "entityName", "订单",
                        "tablePattern", "biz_order",
                        "templateId", 20,
                        "requiredFields", List.of("id", "order_no", "created_at"),
                        "contextExport", true,
                        "status", "ENABLED"
                ))
        ));
        add(map, contract(
                "table-structure-standard",
                "表结构标准",
                "表模板上的主键、唯一键、索引、外键、check guidance、审计和软删除策略结构。",
                List.of("primaryKey", "uniqueKeys[]", "indexes[]", "foreignKeys[]", "checkHints[]",
                        "auditPolicy", "softDeletePolicy", "dialectNotes[]", "aiUsageNotes"),
                List.of(),
                objectSchema("DataSpec Table Structure Standard", List.of(), orderedMap(
                        "primaryKey", objectSchema("Table Primary Key", List.of("columns"), orderedMap(
                                "name", describedStringProp("约束名；safe identifier，nullable。"),
                                "columns", arrayOf(describedStringProp("模板字段名；必须存在于同一模板。"))
                        )),
                        "uniqueKeys", arrayOf(objectSchema("Table Unique Key", List.of("columns"), orderedMap(
                                "name", describedStringProp("唯一键名；safe identifier，nullable。"),
                                "columns", arrayOf(describedStringProp("参与唯一键的模板字段名。"))
                        ))),
                        "indexes", arrayOf(objectProp()),
                        "foreignKeys", arrayOf(objectProp()),
                        "checkHints", arrayOf(describedStringProp("CHECK 或校验提示；默认只读 guidance，不拼 raw SQL。")),
                        "auditPolicy", objectProp(),
                        "softDeletePolicy", objectProp(),
                        "dialectNotes", arrayOf(describedStringProp("方言差异说明；只作为 guidance。")),
                        "aiUsageNotes", describedStringProp("AI 使用说明；不得包含 token、password、Authorization、JDBC URL 或 DSN。")
                )),
                List.of(orderedMap(
                        "primaryKey", orderedMap("name", "pk_order", "columns", List.of("id")),
                        "uniqueKeys", List.of(orderedMap("name", "uk_order_no", "columns", List.of("order_no"))),
                        "indexes", List.of(orderedMap("name", "idx_order_user", "columns", List.of("user_id")))
                ))
        ));
        add(map, contract(
                "table-relation-hint",
                "表关系提示",
                "业务对象之间或表之间的轻量关系边；不保存 raw SQL、凭据或业务数据样本。",
                List.of("sourceObjectKey", "targetObjectKey", "relationType", "sourceColumns[]",
                        "targetColumns[]", "optional", "confidence", "notes"),
                List.of(),
                objectSchema("DataSpec Table Relation Hint", List.of("targetObjectKey"), orderedMap(
                        "sourceObjectKey", describedStringProp("来源业务对象 key；nullable 时默认当前对象。"),
                        "targetObjectKey", describedStringProp("目标业务对象 key；secret-safe。"),
                        "relationType", describedEnumProp("关系类型。", "ONE_TO_ONE", "ONE_TO_MANY", "MANY_TO_ONE", "MANY_TO_MANY", "RELATES_TO"),
                        "sourceColumns", arrayOf(describedStringProp("来源列名；safe identifier。")),
                        "targetColumns", arrayOf(describedStringProp("目标列名；safe identifier。")),
                        "optional", describedBooleanProp("关系是否可选。"),
                        "confidence", describedEnumProp("关系置信度。", "HIGH", "MEDIUM", "LOW"),
                        "notes", describedStringProp("脱敏证据或说明；不得包含 raw secret。")
                )),
                List.of(orderedMap(
                        "sourceObjectKey", "order",
                        "targetObjectKey", "customer",
                        "relationType", "MANY_TO_ONE",
                        "sourceColumns", List.of("customer_id"),
                        "targetColumns", List.of("id"),
                        "confidence", "HIGH"
                ))
        ));
        add(map, contract(
                "table-index-standard",
                "表索引标准",
                "表模板索引标准；DDL preview 只接受 safe identifier 与结构化枚举选项。",
                List.of("name", "columns[]", "unique", "method", "whereHint", "notes"),
                List.of(),
                objectSchema("DataSpec Table Index Standard", List.of("columns"), orderedMap(
                        "name", describedStringProp("索引名；safe identifier，nullable。"),
                        "columns", arrayOf(describedStringProp("索引字段名；必须存在于模板字段。")),
                        "unique", describedBooleanProp("是否唯一索引。"),
                        "method", describedEnumProp("索引方法；v1 PostgreSQL preview 仅支持 btree 或为空，其他方法作为诊断返回。", "btree"),
                        "whereHint", describedStringProp("部分索引提示；v1 默认只作为 guidance，不拼 raw SQL。"),
                        "notes", describedStringProp("脱敏说明。")
                )),
                List.of(orderedMap("name", "idx_order_customer", "columns", List.of("customer_id"), "method", "btree"))
        ));
        add(map, contract(
                "table-foreign-key-standard",
                "表外键标准",
                "表模板外键标准；advisoryOnly 或非法引用只能作为诊断返回，不进入 raw SQL。",
                List.of("name", "columns[]", "targetTable", "targetColumns[]", "onDelete", "onUpdate",
                        "advisoryOnly", "confidence", "notes"),
                List.of(),
                objectSchema("DataSpec Table Foreign Key Standard", List.of("columns", "targetTable", "targetColumns"), orderedMap(
                        "name", describedStringProp("外键名；safe identifier，nullable。"),
                        "columns", arrayOf(describedStringProp("当前模板列名。")),
                        "targetTable", describedStringProp("目标表名；safe identifier。"),
                        "targetColumns", arrayOf(describedStringProp("目标列名；safe identifier。")),
                        "onDelete", describedEnumProp("删除动作；值直接进入受控 DDL 片段。", "NO ACTION", "RESTRICT", "CASCADE", "SET NULL"),
                        "onUpdate", describedEnumProp("更新动作；值直接进入受控 DDL 片段。", "NO ACTION", "RESTRICT", "CASCADE", "SET NULL"),
                        "advisoryOnly", describedBooleanProp("true 时只作为 relation guidance，不生成外键约束。"),
                        "confidence", describedEnumProp("外键建议置信度。", "HIGH", "MEDIUM", "LOW"),
                        "notes", describedStringProp("脱敏说明。")
                )),
                List.of(orderedMap(
                        "name", "fk_order_customer",
                        "columns", List.of("customer_id"),
                        "targetTable", "customer",
                        "targetColumns", List.of("id"),
                        "onDelete", "NO ACTION",
                        "onUpdate", "NO ACTION",
                        "advisoryOnly", false
                ))
        ));
        add(map, contract(
                "table-policy-standard",
                "表策略标准",
                "审计、软删除、checkHints 和方言说明的只读 AI/lint guidance 契约。",
                List.of("auditPolicy", "softDeletePolicy", "checkHints[]", "dialectNotes[]",
                        "policyNotes[]", "redactionBoundary"),
                List.of(),
                objectSchema("DataSpec Table Policy Standard", List.of(), orderedMap(
                        "auditPolicy", objectProp(),
                        "softDeletePolicy", objectProp(),
                        "checkHints", arrayOf(describedStringProp("只读校验提示；不得拼接为 raw SQL。")),
                        "dialectNotes", arrayOf(describedStringProp("方言差异说明。")),
                        "policyNotes", arrayOf(describedStringProp("DDL preview 返回的脱敏策略说明。")),
                        "redactionBoundary", describedStringProp("token/password/Authorization/JDBC URL/DSN/业务数据行必须脱敏或拒绝。")
                )),
                List.of(orderedMap(
                        "auditPolicy", orderedMap("createdAtColumn", "created_at", "updatedAtColumn", "updated_at"),
                        "softDeletePolicy", orderedMap("column", "is_deleted", "activeValue", false),
                        "redactionBoundary", "secret-safe"
                ))
        ));
        add(map, contract(
                "ai-context-table-standards",
                "AI Context 表结构标准",
                "AI Context 包中的 .dataspec/table-standards.json 结构。支持 business-object/table-template scope 裁剪。",
                List.of("kind", "schemaVersion", "projectId", "contextScope", "contextScope.scope",
                        "contextScope.matchedObjectCount", "contextScope.returnedObjectCount",
                        "contextScope.matchedTemplateCount", "contextScope.returnedTemplateCount",
                        "businessObjects[]", "templates[]", "templates[].structure", "relations[]", "summary"),
                List.of(),
                objectSchema("DataSpec AI Context Table Standards", List.of("kind", "schemaVersion", "projectId"), orderedMap(
                        "kind", describedStringProp("固定为 dataspec-table-standards。"),
                        "schemaVersion", describedIntegerProp("table standards context schema 版本。"),
                        "projectId", describedIntegerProp("项目 ID。"),
                        "contextScope", objectSchema("Table Standards Context Scope", List.of("scope"), orderedMap(
                                "scope", describedEnumProp("表结构标准裁剪范围。", "all", "business-object", "table-template"),
                                "query", describedStringProp("脱敏后的查询文本；nullable。"),
                                "matchedObjectCount", describedIntegerProp("匹配业务对象数。"),
                                "returnedObjectCount", describedIntegerProp("返回业务对象数。"),
                                "matchedTemplateCount", describedIntegerProp("匹配模板数。"),
                                "returnedTemplateCount", describedIntegerProp("返回模板数。"),
                                "truncated", describedBooleanProp("是否因 limit 截断。"),
                                "warnings", arrayOf(describedStringProp("脱敏警告。"))
                        )),
                        "businessObjects", arrayOf(objectProp()),
                        "templates", arrayOf(objectSchema("Table Standards Template", List.of("id", "name"), orderedMap(
                                "id", describedIntegerProp("模板 ID。"),
                                "projectId", describedIntegerProp("项目 ID。"),
                                "name", describedStringProp("模板名称。"),
                                "businessObjectId", describedIntegerProp("关联业务对象 ID；nullable。"),
                                "structure", objectProp()
                        ))),
                        "relations", arrayOf(objectProp()),
                        "summary", objectProp()
                )),
                List.of(orderedMap(
                        "kind", "dataspec-table-standards",
                        "schemaVersion", 1,
                        "projectId", 1,
                        "contextScope", orderedMap("scope", "business-object", "returnedObjectCount", 1, "returnedTemplateCount", 1),
                        "businessObjects", List.of(orderedMap("objectKey", "order", "entityName", "订单")),
                        "templates", List.of(orderedMap("id", 20, "name", "订单表模板", "structure", orderedMap("primaryKey", orderedMap("columns", List.of("id"))))),
                        "relations", List.of(),
                        "summary", orderedMap("businessObjectCount", 1, "templateCount", 1)
                ))
        ));
        return map;
    }

    private void add(Map<String, SchemaContract> map, SchemaContract contract) {
        map.put(contract.getContractId(), contract);
    }

    private SchemaContract contract(String id, String displayName, String description,
                                    List<String> stableFields,
                                    List<DeprecatedContractField> deprecatedFields,
                                    Map<String, Object> jsonSchema,
                                    List<Map<String, Object>> examples) {
        String jsonSchemaRef = "dataspec://contracts/" + id + "@" + CONTRACT_SCHEMA_VERSION;
        return SchemaContract.builder()
                .contractId(id)
                .displayName(displayName)
                .description(description)
                .schemaVersion(CONTRACT_SCHEMA_VERSION)
                .jsonSchemaRef(jsonSchemaRef)
                .jsonSchema(jsonSchema)
                .stableFields(stableFields)
                .deprecatedFields(deprecatedFields)
                .compatibility(compatibilityPolicy)
                .docsRef("docs/ai-contracts.md#" + id)
                .examples(examples)
                .build();
    }

    private static Map<String, Object> objectSchema(String title, List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = orderedMap(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "title", title,
                "properties", properties,
                "additionalProperties", true
        );
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map<String, Object> stringProp() {
        return orderedMap("type", "string");
    }

    private static Map<String, Object> describedStringProp(String description) {
        return orderedMap("type", "string", "description", description);
    }

    private static Map<String, Object> integerProp() {
        return orderedMap("type", "integer");
    }

    private static Map<String, Object> describedIntegerProp(String description) {
        return orderedMap("type", "integer", "description", description);
    }

    private static Map<String, Object> booleanProp() {
        return orderedMap("type", "boolean");
    }

    private static Map<String, Object> describedBooleanProp(String description) {
        return orderedMap("type", "boolean", "description", description);
    }

    private static Map<String, Object> objectProp() {
        return orderedMap("type", "object", "additionalProperties", true);
    }

    private static Map<String, Object> describedValueProp(String description) {
        return orderedMap("description", description, "oneOf", List.of(
                orderedMap("type", "string"),
                orderedMap("type", "number"),
                orderedMap("type", "integer"),
                orderedMap("type", "boolean")
        ));
    }

    private static Map<String, Object> enumProp(String... values) {
        return orderedMap("type", "string", "enum", List.of(values));
    }

    private static Map<String, Object> describedEnumProp(String description, String... values) {
        return orderedMap("type", "string", "description", description, "enum", List.of(values));
    }

    private static Map<String, Object> arrayOf(Map<String, Object> items) {
        return orderedMap("type", "array", "items", items);
    }

    private static Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
