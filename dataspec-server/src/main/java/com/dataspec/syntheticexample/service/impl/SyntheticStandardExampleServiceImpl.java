package com.dataspec.syntheticexample.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.syntheticexample.model.SyntheticDdlPreviewInput;
import com.dataspec.syntheticexample.model.SyntheticExampleDiagnostic;
import com.dataspec.syntheticexample.model.SyntheticExampleSafety;
import com.dataspec.syntheticexample.model.SyntheticExampleSourceSummary;
import com.dataspec.syntheticexample.model.SyntheticFieldSuggestionQuestion;
import com.dataspec.syntheticexample.model.SyntheticSqlCase;
import com.dataspec.syntheticexample.model.SyntheticStandardExamplePackage;
import com.dataspec.syntheticexample.model.SyntheticStandardQaCase;
import com.dataspec.syntheticexample.service.SyntheticStandardExampleService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 默认合成标准样例生成服务。
 *
 * <p>生成逻辑只读取标准字段和模板摘要，并使用内置场景骨架补齐缺失素材；
 * 不写入标准库、不调用外部模型，也不输出真实业务行数据。</p>
 */
@Service
@RequiredArgsConstructor
public class SyntheticStandardExampleServiceImpl implements SyntheticStandardExampleService {

    private static final String KIND = "dataspec.synthetic-standard-examples";
    private static final int SCHEMA_VERSION = 1;
    private static final int DEFAULT_MAX_CASES = 6;
    private static final int MAX_CASES_LIMIT = 20;
    private static final String SCENARIO_VERSION = "synthetic-standard-examples@1";
    private static final List<String> SUPPORTED_SCENARIOS = List.of("user", "order", "payment", "audit");
    private static final ObjectMapper HASH_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final FieldService fieldService;
    private final TemplateService templateService;

    @Override
    public SyntheticStandardExamplePackage generate(Long projectId, String scenario, Integer maxCases) {
        if (projectId == null) {
            throw new BizException("projectId 不能为空");
        }
        String normalizedScenario = normalizeScenario(scenario);
        int normalizedMaxCases = normalizeMaxCases(maxCases);
        List<Field> projectFields = sortedFields(fieldService.listByProject(projectId));
        List<TemplateSnapshot> templateSnapshots = templateSnapshots(projectId);
        SelectedFields selected = selectFields(projectFields, normalizedScenario, normalizedMaxCases);

        Map<String, Object> generationParams = new LinkedHashMap<>();
        generationParams.put("scenario", normalizedScenario);
        generationParams.put("maxCases", normalizedMaxCases);
        generationParams.put("scenarioVersion", SCENARIO_VERSION);
        generationParams.put("fallbackAllowed", true);

        SyntheticExampleSourceSummary sourceSummary = new SyntheticExampleSourceSummary(
                projectFields.size(),
                templateSnapshots.size(),
                (int) projectFields.stream().filter(field -> field.getCodeSetId() != null).count(),
                selected.fallbackUsed(),
                selected.fields().stream().map(ExampleField::name).toList()
        );
        List<SyntheticExampleDiagnostic> expectedDiagnostics = expectedDiagnostics(normalizedScenario);
        List<SyntheticExampleDiagnostic> diagnostics = selected.fallbackUsed()
                ? List.of(new SyntheticExampleDiagnostic(
                "SYNTHETIC_FALLBACK_USED",
                "INFO",
                "项目标准素材不足，已使用内置合成场景补齐；采纳前请人工复核。"))
                : List.of();

        String specHash = computeSpecHash(
                projectId,
                normalizedScenario,
                generationParams,
                sourceSummary,
                projectFields,
                selected.fields(),
                templateSnapshots);
        return new SyntheticStandardExamplePackage(
                KIND,
                SCHEMA_VERSION,
                projectId,
                normalizedScenario,
                specHash,
                generationParams,
                sourceSummary,
                List.of(goodSqlCase(normalizedScenario, selected.fields())),
                List.of(badSqlCase(normalizedScenario, expectedDiagnostics)),
                List.of(ddlPreviewInput(normalizedScenario, selected.fields(), generationParams)),
                List.of(fieldSuggestionQuestion(normalizedScenario, selected.fields())),
                List.of(standardQaCase(normalizedScenario, selected.fields(), selected.fallbackUsed())),
                expectedDiagnostics,
                diagnostics,
                new SyntheticExampleSafety(true, false, false, false, List.of("dataspec-token", "standard-metadata")),
                List.of(
                        "先把生成包作为 fixture 或 Prompt 评测输入使用。",
                        "人工审核后再把高价值 case 写入标准使用示例库。",
                        "不要把合成 SQL 当作可直接执行的生产 DDL。")
        );
    }

    private String normalizeScenario(String scenario) {
        String normalized = scenario == null ? "" : scenario.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SCENARIOS.contains(normalized)) {
            throw new BizException("不支持的合成样例场景: " + SensitiveDataSanitizer.redactText(String.valueOf(scenario))
                    + "。支持: " + String.join(", ", SUPPORTED_SCENARIOS));
        }
        return normalized;
    }

    private int normalizeMaxCases(Integer maxCases) {
        if (maxCases == null) {
            return DEFAULT_MAX_CASES;
        }
        if (maxCases <= 0) {
            throw new BizException("maxCases 必须大于 0");
        }
        return Math.min(maxCases, MAX_CASES_LIMIT);
    }

    private List<Field> sortedFields(List<Field> fields) {
        if (fields == null) {
            return List.of();
        }
        return fields.stream()
                .filter(field -> field != null && !Boolean.TRUE.equals(field.getIsDeleted()))
                .filter(field -> !"disabled".equalsIgnoreCase(nullToEmpty(field.getStatus())))
                .sorted(Comparator
                        .comparing((Field field) -> nullToEmpty(field.getName()))
                        .thenComparing(field -> field.getId() == null ? Long.MAX_VALUE : field.getId()))
                .toList();
    }

    private List<TemplateSnapshot> templateSnapshots(Long projectId) {
        List<Template> templates = templateService.listByProject(projectId);
        if (templates == null || templates.isEmpty()) {
            return List.of();
        }
        List<TemplateSnapshot> snapshots = new ArrayList<>();
        for (Template template : templates.stream()
                .filter(item -> item != null && item.getId() != null)
                .sorted(Comparator.comparing((Template item) -> nullToEmpty(item.getName()))
                        .thenComparing(Template::getId))
                .limit(5)
                .toList()) {
            List<TemplateField> fields = templateService.listFields(template.getId());
            snapshots.add(new TemplateSnapshot(
                    template.getId(),
                    safeText(template.getName()),
                    safeText(template.getDescription()),
                    fields == null ? List.of() : fields.stream()
                            .filter(item -> item != null)
                            .sorted(Comparator.comparing((TemplateField item) -> item.getSortOrder() == null
                                            ? Integer.MAX_VALUE
                                            : item.getSortOrder())
                                    .thenComparing(item -> nullToEmpty(item.getName())))
                            .map(item -> Map.of(
                                    "name", safeIdentifier(item.getName(), "field"),
                                    "dataType", safeDataType(item.getDataType()),
                                    "comment", safeText(item.getComment())))
                            .toList()));
        }
        return snapshots;
    }

    private SelectedFields selectFields(List<Field> projectFields, String scenario, int maxCases) {
        List<ExampleField> selected = projectFields.stream()
                .filter(field -> matchesScenario(field, scenario))
                .map(field -> new ExampleField(
                        safeIdentifier(field.getName(), fallbackFields(scenario).getFirst().name()),
                        safeDataType(field.getDataType()),
                        safeText(firstNonBlank(field.getDisplayName(), field.getName())),
                        safeText(field.getComment()),
                        safeText(field.getCategory()),
                        "project"))
                .limit(Math.min(maxCases, 6))
                .toList();
        List<ExampleField> merged = new ArrayList<>(selected);
        boolean fallbackUsed = merged.isEmpty();
        Set<String> existingNames = merged.stream().map(ExampleField::name).collect(java.util.stream.Collectors.toSet());
        for (ExampleField fallback : fallbackFields(scenario)) {
            if (merged.size() >= Math.min(maxCases, 4)) {
                break;
            }
            if (existingNames.add(fallback.name())) {
                merged.add(fallback);
                fallbackUsed = true;
            }
        }
        return new SelectedFields(List.copyOf(merged), fallbackUsed);
    }

    private boolean matchesScenario(Field field, String scenario) {
        String haystack = String.join(" ",
                nullToEmpty(field.getName()),
                nullToEmpty(field.getDisplayName()),
                nullToEmpty(field.getComment()),
                nullToEmpty(field.getCategory()),
                nullToEmpty(field.getAliases()))
                .toLowerCase(Locale.ROOT);
        for (String keyword : scenarioKeywords(scenario)) {
            if (haystack.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> scenarioKeywords(String scenario) {
        return switch (scenario) {
            case "user" -> List.of("user", "account", "mobile", "phone", "email", "用户", "账号", "手机");
            case "order" -> List.of("order", "user", "amount", "status", "订单", "用户", "金额", "状态");
            case "payment" -> List.of("payment", "pay", "order", "amount", "channel", "支付", "付款", "订单", "金额");
            case "audit" -> List.of("audit", "operator", "created", "updated", "action", "审计", "操作", "创建", "更新");
            default -> List.of();
        };
    }

    private List<ExampleField> fallbackFields(String scenario) {
        return switch (scenario) {
            case "user" -> List.of(
                    new ExampleField("user_id", "bigint", "用户ID", "用户主键", "user", "fallback"),
                    new ExampleField("user_name", "varchar(64)", "用户名称", "用户显示名称", "user", "fallback"),
                    new ExampleField("mobile_no", "varchar(20)", "手机号", "联系方式", "contact", "fallback"),
                    new ExampleField("created_at", "timestamp", "创建时间", "UTC 时间", "audit", "fallback"));
            case "order" -> List.of(
                    new ExampleField("order_id", "bigint", "订单ID", "订单主键", "order", "fallback"),
                    new ExampleField("user_id", "bigint", "用户ID", "下单用户", "user", "fallback"),
                    new ExampleField("order_status", "varchar(20)", "订单状态", "订单生命周期状态", "status", "fallback"),
                    new ExampleField("total_amount", "numeric(18,2)", "订单金额", "金额单位按项目标准", "money", "fallback"));
            case "payment" -> List.of(
                    new ExampleField("payment_id", "bigint", "支付ID", "支付记录主键", "payment", "fallback"),
                    new ExampleField("order_id", "bigint", "订单ID", "关联订单", "order", "fallback"),
                    new ExampleField("paid_amount", "numeric(18,2)", "支付金额", "金额单位按项目标准", "money", "fallback"),
                    new ExampleField("payment_status", "varchar(20)", "支付状态", "支付生命周期状态", "status", "fallback"));
            case "audit" -> List.of(
                    new ExampleField("audit_id", "bigint", "审计ID", "审计记录主键", "audit", "fallback"),
                    new ExampleField("operator_id", "bigint", "操作人ID", "执行操作的用户", "audit", "fallback"),
                    new ExampleField("action_type", "varchar(40)", "操作类型", "审计动作类型", "status", "fallback"),
                    new ExampleField("created_at", "timestamp", "创建时间", "UTC 时间", "audit", "fallback"));
            default -> List.of();
        };
    }

    private SyntheticSqlCase goodSqlCase(String scenario, List<ExampleField> fields) {
        String tableName = "synthetic_" + scenario + "_example";
        StringBuilder sql = new StringBuilder("CREATE TABLE ")
                .append(tableName)
                .append(" (\n");
        for (int i = 0; i < fields.size(); i++) {
            ExampleField field = fields.get(i);
            sql.append("    ")
                    .append(field.name())
                    .append(" ")
                    .append(field.dataType())
                    .append(" NOT NULL");
            sql.append(i == fields.size() - 1 ? "\n" : ",\n");
        }
        sql.append(");\n");
        sql.append("COMMENT ON TABLE ")
                .append(tableName)
                .append(" IS '")
                .append(sqlComment("合成" + scenarioTitle(scenario) + "场景样例"))
                .append("';\n");
        for (ExampleField field : fields) {
            sql.append("COMMENT ON COLUMN ")
                    .append(tableName)
                    .append(".")
                    .append(field.name())
                    .append(" IS '")
                    .append(sqlComment(firstNonBlank(field.comment(), field.displayName())))
                    .append("';\n");
        }
        return new SyntheticSqlCase(
                scenario + "-good-sql-1",
                scenarioTitle(scenario) + "标准字段建表示例",
                scenario,
                sql.toString(),
                fields.stream().map(ExampleField::name).toList(),
                List.of(),
                "使用场景相关标准字段和 COMMENT，适合作为 good SQL fixture。",
                Map.of("fieldSource", fieldSources(fields)));
    }

    private SyntheticSqlCase badSqlCase(String scenario, List<SyntheticExampleDiagnostic> expectedDiagnostics) {
        String tableName = "synthetic_" + scenario + "_bad";
        String sql = "CREATE TABLE " + tableName + " (\n"
                + "    id bigint,\n"
                + "    status_text varchar(64),\n"
                + "    created_time timestamp\n"
                + ");";
        return new SyntheticSqlCase(
                scenario + "-bad-sql-1",
                scenarioTitle(scenario) + "非标准字段反例",
                scenario,
                sql,
                List.of(),
                expectedDiagnostics.stream().map(SyntheticExampleDiagnostic::id).toList(),
                "故意使用泛化字段名并省略 COMMENT，用于验证规则和标准诊断。",
                Map.of("fieldSource", "synthetic-anti-example"));
    }

    private SyntheticDdlPreviewInput ddlPreviewInput(
            String scenario,
            List<ExampleField> fields,
            Map<String, Object> generationParams) {
        return new SyntheticDdlPreviewInput(
                scenario + "-ddl-preview-1",
                "synthetic_" + scenario + "_preview",
                scenarioTitle(scenario) + "业务对象",
                fields.stream().map(ExampleField::name).toList(),
                generationParams);
    }

    private SyntheticFieldSuggestionQuestion fieldSuggestionQuestion(String scenario, List<ExampleField> fields) {
        return new SyntheticFieldSuggestionQuestion(
                scenario + "-field-question-1",
                "为" + scenarioTitle(scenario) + "场景推荐标准字段：" + fields.getFirst().displayName(),
                List.of(fields.getFirst().name()),
                "应优先命中同名或同义标准字段，fallback 字段需要人工复核。");
    }

    private SyntheticStandardQaCase standardQaCase(String scenario, List<ExampleField> fields, boolean fallbackUsed) {
        return new SyntheticStandardQaCase(
                scenario + "-qa-1",
                scenarioTitle(scenario) + "表应优先使用哪些标准字段？",
                "回答应引用 " + String.join(", ", fields.stream().map(ExampleField::name).toList())
                        + "，并说明这些字段来自项目标准或内置合成场景。",
                fields.stream().map(ExampleField::name).toList(),
                fallbackUsed ? "MEDIUM" : "HIGH",
                fallbackUsed ? "存在内置 fallback，落库前请人工审核。" : "字段来自项目标准摘要。");
    }

    private List<SyntheticExampleDiagnostic> expectedDiagnostics(String scenario) {
        return List.of(
                new SyntheticExampleDiagnostic(
                        scenario + "-NON_STANDARD_FIELD_NAME",
                        "ERROR",
                        "反例包含泛化或非标准字段名，应推荐项目标准字段。"),
                new SyntheticExampleDiagnostic(
                        scenario + "-MISSING_COMMENT",
                        "WARN",
                        "反例缺少表或字段 COMMENT，AI 修复时应补充业务说明。")
        );
    }

    private String computeSpecHash(
            Long projectId,
            String scenario,
            Map<String, Object> generationParams,
            SyntheticExampleSourceSummary sourceSummary,
            List<Field> projectFields,
            List<ExampleField> fields,
            List<TemplateSnapshot> templates) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", SCHEMA_VERSION);
        payload.put("projectId", projectId);
        payload.put("scenario", scenario);
        payload.put("generationParams", generationParams);
        payload.put("sourceSummary", sourceSummary);
        payload.put("standardFieldSummary", fieldHashInputs(projectFields));
        payload.put("fields", fields);
        payload.put("templates", templates);
        try {
            byte[] json = HASH_MAPPER.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new BizException(500, "合成样例 specHash 计算失败");
        }
    }

    private List<Map<String, Object>> fieldHashInputs(List<Field> fields) {
        // Hash 使用脱敏后的标准摘要，确保代码集、字段说明等标准变化能驱动 fixture 更新。
        return fields.stream()
                .map(field -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", field.getId());
                    item.put("name", safeIdentifier(field.getName(), "field"));
                    item.put("dataType", safeDataType(field.getDataType()));
                    item.put("displayName", safeText(field.getDisplayName()));
                    item.put("comment", safeText(field.getComment()));
                    item.put("category", safeText(field.getCategory()));
                    item.put("aliases", safeText(field.getAliases()));
                    item.put("status", safeText(field.getStatus()));
                    item.put("codeSetId", field.getCodeSetId());
                    item.put("sensitive", field.getSensitive());
                    return item;
                })
                .toList();
    }

    private List<String> fieldSources(List<ExampleField> fields) {
        return fields.stream()
                .map(field -> field.name() + ":" + field.source())
                .toList();
    }

    private String scenarioTitle(String scenario) {
        return switch (scenario) {
            case "user" -> "用户";
            case "order" -> "订单";
            case "payment" -> "支付";
            case "audit" -> "审计";
            default -> scenario;
        };
    }

    private String safeText(String text) {
        return SensitiveDataSanitizer.redactText(nullToEmpty(text));
    }

    private String safeIdentifier(String value, String fallback) {
        String sanitized = SensitiveDataSanitizer.redactText(nullToEmpty(value))
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private String safeDataType(String value) {
        String sanitized = SensitiveDataSanitizer.redactText(nullToEmpty(value)).trim().toLowerCase(Locale.ROOT);
        if (sanitized.matches("[a-z][a-z0-9_ ]*(\\([0-9, ]+\\))?")) {
            return sanitized;
        }
        return "varchar(64)";
    }

    private String sqlComment(String value) {
        return safeText(value).replace("'", "''");
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : nullToEmpty(second);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record SelectedFields(List<ExampleField> fields, boolean fallbackUsed) {
    }

    private record ExampleField(
            String name,
            String dataType,
            String displayName,
            String comment,
            String category,
            String source
    ) {
    }

    private record TemplateSnapshot(
            Long id,
            String name,
            String description,
            List<Map<String, String>> fields
    ) {
    }
}
