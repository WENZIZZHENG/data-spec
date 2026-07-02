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
    public static final String REGISTRY_VERSION = "2026.06.28";
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
                List.of("name", "dataType", "nullable", "comment", "displayName", "category", "tags",
                        "codeSetId", "sensitive", "status", "replacementFieldId", "replacementReason",
                        "example", "aliases[]", "matchReasons[]"),
                List.of(),
                objectSchema("DataSpec Field", List.of("name", "dataType"), orderedMap(
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
                        "aliases", List.of("phone", "mobile")
                ))
        ));
        add(map, contract(
                "enum-dict",
                "枚举字典",
                "项目枚举字典及其枚举值结构。",
                List.of("code", "name", "valueType", "values[].value", "values[].label", "values[].sortOrder"),
                List.of(),
                objectSchema("DataSpec Enum Dict", List.of("code", "name", "values"), orderedMap(
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
                "ai-evidence-package",
                "AI 执行证据包",
                "AI 任务交付、复盘和下游续跑使用的只读 evidence package 结构。",
                List.of("kind", "schemaVersion", "packageId", "projectId", "generatedAt", "source",
                        "standardSnapshot", "inputsSummary", "outputsSummary", "validationSummary",
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
                                        "sourceType", enumProp("AI_JOB", "SQL_CHECK", "COVERAGE_REPORT", "AI_BATCH_RUN"),
                                        "sourceId", integerProp(),
                                        "sourceTitle", stringProp(),
                                        "status", stringProp(),
                                        "persisted", booleanProp()
                                )),
                                "standardSnapshot", objectProp(),
                                "inputsSummary", objectProp(),
                                "outputsSummary", objectProp(),
                                "validationSummary", objectProp(),
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
                        "artifacts", List.of(orderedMap("artifactType", "fixed-sql", "format", "sql")),
                        "nextActions", List.of("复核 fixedSql 后再应用补丁。"),
                        "suggestedCommands", List.of("dataspec evidence export --source-type SQL_CHECK --source-id 42 --format zip --output evidence.zip")
                ))
        ));
        add(map, contract(
                "ai-context-manifest",
                "AI Context Manifest",
                "AI Context zip 的入口 manifest，用于描述标准版本、文件清单、命令和契约版本。",
                List.of("kind", "schemaVersion", "projectId", "standard", "contextScope", "contracts", "files[]", "commands"),
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
                        "fields[].name", "fields[].dataType", "fields[].status"),
                List.of(),
                objectSchema("DataSpec AI Context Field Catalog", List.of("projectId", "fields", "enums"), orderedMap(
                        "projectId", integerProp(),
                        "standard", objectProp(),
                        "contextScope", objectProp(),
                        "fields", arrayOf(objectProp()),
                        "enums", arrayOf(objectProp())
                )),
                List.of(orderedMap("projectId", 1, "fields", List.of(orderedMap("name", "mobile_no")), "enums", List.of()))
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

    private static Map<String, Object> integerProp() {
        return orderedMap("type", "integer");
    }

    private static Map<String, Object> booleanProp() {
        return orderedMap("type", "boolean");
    }

    private static Map<String, Object> objectProp() {
        return orderedMap("type", "object", "additionalProperties", true);
    }

    private static Map<String, Object> enumProp(String... values) {
        return orderedMap("type", "string", "enum", List.of(values));
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
