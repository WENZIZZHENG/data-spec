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
    public static final String REGISTRY_VERSION = "2026.07.10";
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
