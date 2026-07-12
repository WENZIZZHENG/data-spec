package com.dataspec.testdata.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.testdata.model.StandardTestDataPackage;
import com.dataspec.testdata.model.StandardTestDataPackageReq;
import com.dataspec.testdata.model.TestDataCase;
import com.dataspec.testdata.model.TestDataCoverageReport;
import com.dataspec.testdata.model.TestDataDiagnostic;
import com.dataspec.testdata.model.TestDataMockPayload;
import com.dataspec.testdata.model.TestDataSafety;
import com.dataspec.testdata.model.TestDataSeedProfile;
import com.dataspec.testdata.model.TestDataSourceSummary;
import com.dataspec.testdata.service.StandardTestDataPackageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 默认标准测试数据包生成服务。
 *
 * <p>实现只读取字段与枚举标准元数据，使用内置确定性规则生成安全样例；
 * SQL seed 为草稿文本且默认不可执行，避免 AI 或工具误写入业务数据库。</p>
 */
@Service
@RequiredArgsConstructor
public class StandardTestDataPackageServiceImpl implements StandardTestDataPackageService {

    private static final String KIND = "dataspec.standard-test-data-package";
    private static final int SCHEMA_VERSION = 1;
    private static final int DEFAULT_MAX_FIELDS = 12;
    private static final int MAX_FIELDS_LIMIT = 50;
    private static final int DEFAULT_CASES_PER_FIELD = 3;
    private static final int MAX_CASES_PER_FIELD = 3;
    private static final int DEFAULT_SEED_ROW_COUNT = 2;
    private static final int MAX_SEED_ROW_COUNT = 20;
    private static final String GENERATOR_VERSION = "standard-test-data-package@1";
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final FieldService fieldService;
    private final EnumDictService enumDictService;

    @Override
    public StandardTestDataPackage generate(StandardTestDataPackageReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("projectId 不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.projectId());
        List<TestDataDiagnostic> diagnostics = new ArrayList<>();
        NormalizedRequest normalized = normalize(req, diagnostics);
        List<Field> projectFields = sortedFields(fieldService.listByProject(req.projectId()));
        rejectUnknownSelectors(projectFields, normalized);
        List<FieldSnapshot> selected = selectFields(projectFields, normalized);
        if (selected.isEmpty()) {
            selected = fallbackFields(normalized.objectScenario(), normalized.maxFields());
            diagnostics.add(diagnostic("TEST_DATA_FALLBACK_USED", "INFO",
                    "项目标准素材不足或筛选无匹配，已使用内置合成字段补齐；采纳前请人工复核。"));
        }

        EnumLookup enumLookup = enumLookup(selected);
        List<TestDataCase> cases = buildCases(selected, enumLookup, normalized, diagnostics);
        List<Map<String, Object>> seedRows = buildSeedRows(selected, cases, normalized.seedRowCount());
        List<TestDataSeedProfile> seedProfiles = buildSeedProfiles(selected, cases, seedRows, normalized);
        List<TestDataMockPayload> mockPayloads = List.of(new TestDataMockPayload(
                deterministicId("mock", normalized.objectScenario()),
                normalized.objectScenario(),
                seedRows.isEmpty() ? Map.of() : seedRows.getFirst(),
                cases.stream().filter(TestDataCase::expectedValidity).map(TestDataCase::caseId).toList(),
                true));

        TestDataSourceSummary sourceSummary = new TestDataSourceSummary(
                projectFields.size(),
                selected.size(),
                enumLookup.totalValueCount(),
                selected.stream().anyMatch(FieldSnapshot::fallback),
                selected.stream().map(FieldSnapshot::name).toList(),
                selected.stream().map(item -> item.fallback() ? "fallback" : "project-field").distinct().toList());
        TestDataCoverageReport coverageReport = coverageReport(selected, cases, normalized);
        Map<String, Object> generationParams = orderedMap(
                "maxFields", normalized.maxFields(),
                "casesPerField", normalized.casesPerField(),
                "seedRowCount", normalized.seedRowCount(),
                "objectScenario", normalized.objectScenario(),
                "dialect", normalized.dialect(),
                "generatorVersion", GENERATOR_VERSION,
                "fieldNames", normalized.fieldNames());
        String specHash = specHash(req.projectId(), generationParams, selected, enumLookup);

        return new StandardTestDataPackage(
                KIND,
                SCHEMA_VERSION,
                req.projectId(),
                specHash,
                generationParams,
                sourceSummary,
                cases,
                seedProfiles,
                mockPayloads,
                coverageReport,
                List.copyOf(diagnostics),
                new TestDataSafety(true, false, false, false, false, false,
                        List.of("standard-metadata", "generation-parameters", "fixture-examples")),
                List.of(
                        "先把 JSON package 作为单测、mock 或 AI 生成用例的输入。",
                        "SQL seed 是可审查草稿，写入任何数据库前必须人工确认。",
                        "coverageReport 中的 missingConstraints 需要结合业务规则补充。")
        );
    }

    private NormalizedRequest normalize(StandardTestDataPackageReq req, List<TestDataDiagnostic> diagnostics) {
        int maxFields = clampPositive(req.maxFields(), DEFAULT_MAX_FIELDS, MAX_FIELDS_LIMIT, "maxFields", diagnostics);
        int casesPerField = clampPositive(req.casesPerField(), DEFAULT_CASES_PER_FIELD, MAX_CASES_PER_FIELD, "casesPerField", diagnostics);
        int seedRowCount = clampPositive(req.seedRowCount(), DEFAULT_SEED_ROW_COUNT, MAX_SEED_ROW_COUNT, "seedRowCount", diagnostics);
        List<String> fieldNames = normalizeFieldNames(req.fieldNames());
        String scenario = safeIdentifier(firstNonBlank(req.objectScenario(), "standard"), "standard");
        String dialect = safeIdentifier(firstNonBlank(req.dialect(), "generic"), "generic");
        return new NormalizedRequest(fieldNames, scenario, maxFields, casesPerField, seedRowCount, dialect);
    }

    private int clampPositive(
            Integer value,
            int defaultValue,
            int maxValue,
            String fieldName,
            List<TestDataDiagnostic> diagnostics) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new BizException(fieldName + " 必须大于 0");
        }
        if (value > maxValue) {
            diagnostics.add(diagnostic("TEST_DATA_BOUND_TRUNCATED", "WARN",
                    fieldName + " 超过安全上限 " + maxValue + "，已裁剪；请减少参数后重试。"));
            return maxValue;
        }
        return value;
    }

    private List<String> normalizeFieldNames(List<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            return List.of();
        }
        if (rawNames.size() > MAX_FIELDS_LIMIT) {
            throw new BizException("fieldNames 不能超过 " + MAX_FIELDS_LIMIT + " 个");
        }
        Set<String> names = new LinkedHashSet<>();
        for (String rawName : rawNames) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            if (SensitiveDataSanitizer.containsSensitiveText(rawName)) {
                throw new BizException("fieldNames 包含敏感片段: " + SensitiveDataSanitizer.redactText(rawName));
            }
            names.add(safeIdentifier(rawName, "field"));
        }
        return List.copyOf(names);
    }

    private List<Field> sortedFields(List<Field> fields) {
        if (fields == null) {
            return List.of();
        }
        return fields.stream()
                .filter(Objects::nonNull)
                .filter(field -> !Boolean.TRUE.equals(field.getIsDeleted()))
                .filter(field -> !"disabled".equalsIgnoreCase(nullToEmpty(field.getStatus())))
                .sorted(Comparator.comparing((Field field) -> nullToEmpty(field.getName()))
                        .thenComparing(field -> field.getId() == null ? Long.MAX_VALUE : field.getId()))
                .toList();
    }

    private void rejectUnknownSelectors(List<Field> fields, NormalizedRequest req) {
        if (req.fieldNames().isEmpty()) {
            return;
        }
        Set<String> available = fields.stream()
                .map(field -> safeIdentifier(field.getName(), "field"))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<String> unknown = req.fieldNames().stream()
                .filter(name -> !available.contains(name))
                .toList();
        if (!unknown.isEmpty()) {
            throw new BizException("fieldNames 不存在于项目标准: " + String.join(", ", unknown));
        }
    }

    private List<FieldSnapshot> selectFields(List<Field> fields, NormalizedRequest req) {
        Set<String> requested = new LinkedHashSet<>(req.fieldNames());
        return fields.stream()
                .filter(field -> requested.isEmpty() || requested.contains(safeIdentifier(field.getName(), "field")))
                .limit(req.maxFields())
                .map(this::snapshot)
                .toList();
    }

    private FieldSnapshot snapshot(Field field) {
        String comment = safeText(field.getComment());
        return new FieldSnapshot(
                field.getId(),
                safeIdentifier(field.getName(), "field"),
                safeText(firstNonBlank(field.getDisplayName(), field.getName())),
                safeDataType(field.getDataType()),
                safeText(field.getCategory()),
                safeText(field.getFormatType()),
                safeText(field.getFormatPattern()),
                safeText(field.getFormatUnit()),
                safeText(field.getFormatPrecision()),
                safeText(field.getFormatTimezone()),
                parseStringArray(field.getValidExamplesJson()),
                parseStringArray(field.getInvalidExamplesJson()),
                field.getCodeSetId(),
                Boolean.TRUE.equals(field.getSensitive()),
                comment,
                false,
                SensitiveDataSanitizer.containsSensitiveText(nullToEmpty(field.getComment()))
        );
    }

    private List<FieldSnapshot> fallbackFields(String scenario, int maxFields) {
        List<FieldSnapshot> fallback = switch (scenario) {
            case "order" -> List.of(
                    fallback("order_id", "bigint", "订单ID", "identifier"),
                    fallback("mobile_no", "varchar(20)", "手机号", "mobile"),
                    fallback("total_amount", "numeric(18,2)", "订单金额", "money"),
                    fallback("order_status", "varchar(20)", "订单状态", "enum"));
            case "audit" -> List.of(
                    fallback("audit_id", "bigint", "审计ID", "identifier"),
                    fallback("operator_id", "bigint", "操作人ID", "identifier"),
                    fallback("action_type", "varchar(40)", "操作类型", "text"),
                    fallback("created_at", "timestamp", "创建时间", "datetime"));
            case "user" -> List.of(
                    fallback("user_id", "bigint", "用户ID", "identifier"),
                    fallback("mobile_no", "varchar(20)", "手机号", "mobile"),
                    fallback("email", "varchar(128)", "邮箱", "email"),
                    fallback("created_at", "timestamp", "创建时间", "datetime"));
            default -> List.of(
                    fallback("id", "bigint", "主键ID", "identifier"),
                    fallback("name", "varchar(64)", "名称", "text"),
                    fallback("status", "varchar(20)", "状态", "enum"),
                    fallback("created_at", "timestamp", "创建时间", "datetime"));
        };
        return fallback.stream().limit(maxFields).toList();
    }

    private FieldSnapshot fallback(String name, String dataType, String displayName, String formatType) {
        return new FieldSnapshot(null, name, displayName, dataType, formatType, formatType,
                null, null, null, null, List.of(), List.of(), null, false,
                "内置合成字段", true, false);
    }

    private EnumLookup enumLookup(List<FieldSnapshot> fields) {
        Map<Long, List<EnumValueSnapshot>> values = new LinkedHashMap<>();
        for (FieldSnapshot field : fields) {
            if (field.codeSetId() == null || values.containsKey(field.codeSetId())) {
                continue;
            }
            List<EnumValue> rawValues = enumDictService.listValues(field.codeSetId());
            List<EnumValueSnapshot> snapshots = rawValues == null ? List.of() : rawValues.stream()
                    .filter(Objects::nonNull)
                    .filter(value -> !"disabled".equalsIgnoreCase(nullToEmpty(value.getStatus())))
                    .sorted(Comparator.comparing((EnumValue value) -> value.getSortOrder() == null
                                    ? Integer.MAX_VALUE
                                    : value.getSortOrder())
                            .thenComparing(value -> nullToEmpty(value.getValue())))
                    .map(value -> new EnumValueSnapshot(
                            value.getId(),
                            safeText(value.getValue()),
                            safeText(value.getLabel()),
                            safeText(value.getStatus())))
                    .toList();
            values.put(field.codeSetId(), snapshots);
        }
        return new EnumLookup(values);
    }

    private List<TestDataCase> buildCases(
            List<FieldSnapshot> fields,
            EnumLookup enumLookup,
            NormalizedRequest req,
            List<TestDataDiagnostic> diagnostics) {
        List<TestDataCase> cases = new ArrayList<>();
        for (FieldSnapshot field : fields) {
            if (field.redacted()) {
                diagnostics.add(diagnostic("TEST_DATA_METADATA_REDACTED", "WARN",
                        "字段 " + field.name() + " 元数据包含敏感片段，已输出为 "
                                + SensitiveDataSanitizer.REDACTION + "。"));
            }
            List<CaseDraft> drafts = List.of(
                    validCase(field, enumLookup),
                    invalidCase(field, enumLookup),
                    boundaryCase(field, enumLookup)
            );
            for (int i = 0; i < Math.min(req.casesPerField(), drafts.size()); i++) {
                CaseDraft draft = drafts.get(i);
                cases.add(new TestDataCase(
                        deterministicId(field.name(), draft.type().toLowerCase(Locale.ROOT)),
                        field.name(),
                        draft.type(),
                        safeText(draft.value()),
                        draft.expectedValidity(),
                        safeText(draft.reason()),
                        sourceRefs(field, draft),
                        draft.requiresBusinessReview() || field.fallback()));
            }
        }
        return List.copyOf(cases);
    }

    private CaseDraft validCase(FieldSnapshot field, EnumLookup enumLookup) {
        String value = enumLookup.firstEnabledValue(field.codeSetId());
        if (value == null) {
            value = firstExample(field.validExamples());
        }
        if (value == null) {
            value = switch (formatKey(field)) {
                case "mobile", "phone" -> "13800138000";
                case "email" -> "synthetic.user@example.invalid";
                case "money", "amount", "decimal", "numeric" -> "88.88";
                case "datetime", "timestamp", "date", "time" -> "2026-01-01T00:00:00Z";
                case "json" -> "{\"synthetic\":true}";
                case "boolean", "bool" -> "true";
                case "identifier", "id", "bigint", "integer" -> "1001";
                case "enum", "status" -> "ENABLED";
                default -> "synthetic_" + field.name();
            };
        }
        if (field.sensitive()) {
            value = "SYNTHETIC_MASKED_" + field.name();
        }
        return new CaseDraft("VALID", value, true, "符合字段格式或标准枚举的合成正例。", false, null);
    }

    private CaseDraft invalidCase(FieldSnapshot field, EnumLookup enumLookup) {
        String value = firstExample(field.invalidExamples());
        if (value == null) {
            value = switch (formatKey(field)) {
                case "mobile", "phone" -> "not-a-phone";
                case "email" -> "not-an-email";
                case "money", "amount", "decimal", "numeric" -> "-1";
                case "datetime", "timestamp", "date", "time" -> "not-a-date";
                case "json" -> "{bad-json";
                case "boolean", "bool" -> "not-bool";
                case "identifier", "id", "bigint", "integer" -> "-1";
                case "enum", "status" -> "__INVALID_" + field.name();
                default -> "";
            };
        }
        if (field.codeSetId() != null && enumLookup.firstEnabledValue(field.codeSetId()) != null) {
            value = "__INVALID_" + field.name();
        }
        return new CaseDraft("INVALID", value, false, "用于验证字段级格式、枚举或必填边界的合成反例。", true, null);
    }

    private CaseDraft boundaryCase(FieldSnapshot field, EnumLookup enumLookup) {
        String deprecatedEnum = enumLookup.firstDeprecatedValue(field.codeSetId());
        if (deprecatedEnum != null) {
            return new CaseDraft("BOUNDARY", deprecatedEnum, true,
                    "枚举值仍存在但生命周期已非首选，使用前需要兼容性复核。", true, field.codeSetId());
        }
        String value = switch (formatKey(field)) {
            case "mobile", "phone" -> "10000000000";
            case "email" -> "a@example.invalid";
            case "money", "amount", "decimal", "numeric" -> "0.00";
            case "datetime", "timestamp", "date", "time" -> "1970-01-01T00:00:00Z";
            case "json" -> "{}";
            case "boolean", "bool" -> "false";
            case "identifier", "id", "bigint", "integer" -> "0";
            default -> "boundary_" + field.name();
        };
        return new CaseDraft("BOUNDARY", value, true, "字段级边界样例；业务采纳前需结合真实业务约束确认。", true, null);
    }

    private List<Map<String, Object>> buildSeedRows(
            List<FieldSnapshot> fields,
            List<TestDataCase> cases,
            int rowCount) {
        Map<String, List<TestDataCase>> validCasesByField = new LinkedHashMap<>();
        for (TestDataCase item : cases) {
            if (item.expectedValidity()) {
                validCasesByField.computeIfAbsent(item.fieldName(), ignored -> new ArrayList<>()).add(item);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (FieldSnapshot field : fields) {
                List<TestDataCase> candidates = validCasesByField.getOrDefault(field.name(), List.of());
                String value = candidates.isEmpty()
                        ? validCase(field, new EnumLookup(Map.of())).value()
                        : candidates.get(rowIndex % candidates.size()).value();
                row.put(field.name(), value);
            }
            rows.add(row);
        }
        return rows;
    }

    private List<TestDataSeedProfile> buildSeedProfiles(
            List<FieldSnapshot> fields,
            List<TestDataCase> cases,
            List<Map<String, Object>> rows,
            NormalizedRequest req) {
        List<String> fieldNames = fields.stream().map(FieldSnapshot::name).toList();
        List<String> sourceCaseIds = cases.stream().filter(TestDataCase::expectedValidity).map(TestDataCase::caseId).toList();
        return List.of(
                new TestDataSeedProfile(
                        deterministicId("seed", "json"),
                        "JSON",
                        "generic",
                        safeText(writeJson(rows)),
                        fieldNames,
                        sourceCaseIds,
                        false,
                        true),
                new TestDataSeedProfile(
                        deterministicId("seed", "csv"),
                        "CSV",
                        "generic",
                        safeText(writeCsv(fieldNames, rows)),
                        fieldNames,
                        sourceCaseIds,
                        false,
                        true),
                new TestDataSeedProfile(
                        deterministicId("seed", "sql"),
                        "SQL",
                        req.dialect(),
                        safeText(writeSql(req.objectScenario(), fieldNames, rows)),
                        fieldNames,
                        sourceCaseIds,
                        false,
                        true)
        );
    }

    private TestDataCoverageReport coverageReport(
            List<FieldSnapshot> fields,
            List<TestDataCase> cases,
            NormalizedRequest req) {
        Set<String> covered = new LinkedHashSet<>();
        for (TestDataCase item : cases) {
            covered.add(item.fieldName());
        }
        List<String> missing = new ArrayList<>();
        for (FieldSnapshot field : fields) {
            if (field.fallback()) {
                missing.add(field.name() + ": 使用内置 fallback，缺少项目级标准约束。");
            }
            if (field.formatType().isBlank() && field.codeSetId() == null && field.validExamples().isEmpty()) {
                missing.add(field.name() + ": 缺少 formatType、枚举或 validExamples，边界用例仅为启发式。");
            }
        }
        String level = "standard".equals(req.objectScenario()) ? "FIELD_ONLY" : "OBJECT_LIGHTWEIGHT";
        return new TestDataCoverageReport(
                fields.size(),
                covered.size(),
                cases.size(),
                level,
                List.copyOf(missing),
                fields.stream().filter(field -> !covered.contains(field.name())).map(FieldSnapshot::name).toList());
    }

    private String specHash(
            Long projectId,
            Map<String, Object> generationParams,
            List<FieldSnapshot> fields,
            EnumLookup enumLookup) {
        Map<String, Object> payload = orderedMap(
                "schemaVersion", SCHEMA_VERSION,
                "projectId", projectId,
                "generationParams", generationParams,
                "fields", fields,
                "enumValues", enumLookup.valuesByCodeSetId());
        try {
            byte[] json = JSON.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new BizException(500, "标准测试数据包 specHash 计算失败");
        }
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = JSON.readValue(json, new TypeReference<>() {
            });
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(this::safeText)
                    .filter(value -> !value.isBlank())
                    .limit(5)
                    .toList();
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<String> sourceRefs(FieldSnapshot field, CaseDraft draft) {
        List<String> refs = new ArrayList<>();
        refs.add(field.id() == null ? "fallback:" + field.name() : "field:" + field.id());
        if (field.codeSetId() != null) {
            refs.add("enum:" + field.codeSetId());
        }
        if (draft.sourceCodeSetId() != null && !draft.sourceCodeSetId().equals(field.codeSetId())) {
            refs.add("enum:" + draft.sourceCodeSetId());
        }
        return List.copyOf(refs);
    }

    private String writeJson(List<Map<String, Object>> rows) {
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
        } catch (JsonProcessingException ex) {
            throw new BizException(500, "测试数据 JSON 草稿生成失败");
        }
    }

    private String writeCsv(List<String> fieldNames, List<Map<String, Object>> rows) {
        StringBuilder csv = new StringBuilder(String.join(",", fieldNames)).append('\n');
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < fieldNames.size(); i++) {
                if (i > 0) {
                    csv.append(',');
                }
                csv.append(csvCell(String.valueOf(row.getOrDefault(fieldNames.get(i), ""))));
            }
            csv.append('\n');
        }
        return csv.toString();
    }

    private String writeSql(String scenario, List<String> fieldNames, List<Map<String, Object>> rows) {
        String tableName = "synthetic_" + safeIdentifier(scenario, "standard") + "_seed";
        StringBuilder sql = new StringBuilder("-- Review before executing. Generated by DataSpec standard test data package.\n");
        for (Map<String, Object> row : rows) {
            sql.append("INSERT INTO ")
                    .append(tableName)
                    .append(" (")
                    .append(String.join(", ", fieldNames))
                    .append(") VALUES (");
            for (int i = 0; i < fieldNames.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(sqlLiteral(String.valueOf(row.getOrDefault(fieldNames.get(i), ""))));
            }
            sql.append(");\n");
        }
        return sql.toString();
    }

    private String csvCell(String value) {
        String safe = safeText(value);
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }

    private String sqlLiteral(String value) {
        String safe = safeText(value);
        if (safe.matches("-?[0-9]+(\\.[0-9]+)?") || "true".equalsIgnoreCase(safe) || "false".equalsIgnoreCase(safe)) {
            return safe;
        }
        return "'" + safe.replace("'", "''") + "'";
    }

    private String firstExample(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private String formatKey(FieldSnapshot field) {
        String joined = String.join(" ",
                field.formatType(),
                field.category(),
                field.dataType(),
                field.name()).toLowerCase(Locale.ROOT);
        if (joined.contains("mobile") || joined.contains("phone") || joined.contains("手机号")) {
            return "mobile";
        }
        if (joined.contains("email")) {
            return "email";
        }
        if (joined.contains("money") || joined.contains("amount") || joined.contains("numeric") || joined.contains("decimal")) {
            return "money";
        }
        if (joined.contains("timestamp") || joined.contains("datetime") || joined.contains("date")) {
            return "datetime";
        }
        if (joined.contains("json")) {
            return "json";
        }
        if (joined.contains("bool")) {
            return "boolean";
        }
        if (joined.endsWith("_id") || joined.contains("bigint") || joined.contains("integer")) {
            return "identifier";
        }
        if (field.codeSetId() != null || joined.contains("enum") || joined.contains("status")) {
            return "enum";
        }
        return "text";
    }

    private TestDataDiagnostic diagnostic(String code, String severity, String message) {
        return new TestDataDiagnostic(code, severity, safeText(message));
    }

    private String safeText(String text) {
        return SensitiveDataSanitizer.redactText(nullToEmpty(text), 500);
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
        String sanitized = SensitiveDataSanitizer.redactText(nullToEmpty(value))
                .trim()
                .toLowerCase(Locale.ROOT);
        if (sanitized.matches("[a-z][a-z0-9_ ]*(\\([0-9, ]+\\))?")) {
            return sanitized;
        }
        return "varchar(64)";
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : nullToEmpty(second);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String deterministicId(String... parts) {
        return String.join("-", parts).replaceAll("[^A-Za-z0-9_-]+", "-");
    }

    private Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private record NormalizedRequest(
            List<String> fieldNames,
            String objectScenario,
            int maxFields,
            int casesPerField,
            int seedRowCount,
            String dialect
    ) {
    }

    private record FieldSnapshot(
            Long id,
            String name,
            String displayName,
            String dataType,
            String category,
            String formatType,
            String formatPattern,
            String formatUnit,
            String formatPrecision,
            String formatTimezone,
            List<String> validExamples,
            List<String> invalidExamples,
            Long codeSetId,
            boolean sensitive,
            String comment,
            boolean fallback,
            boolean redacted
    ) {
    }

    private record EnumValueSnapshot(Long id, String value, String label, String status) {
    }

    private record EnumLookup(Map<Long, List<EnumValueSnapshot>> valuesByCodeSetId) {
        int totalValueCount() {
            return valuesByCodeSetId.values().stream().mapToInt(List::size).sum();
        }

        String firstEnabledValue(Long codeSetId) {
            return valuesByCodeSetId.getOrDefault(codeSetId, List.of()).stream()
                    .filter(value -> !"deprecated".equalsIgnoreCase(nullToEmptyStatic(value.status())))
                    .map(EnumValueSnapshot::value)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        }

        String firstDeprecatedValue(Long codeSetId) {
            return valuesByCodeSetId.getOrDefault(codeSetId, List.of()).stream()
                    .filter(value -> "deprecated".equalsIgnoreCase(nullToEmptyStatic(value.status())))
                    .map(EnumValueSnapshot::value)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        }
    }

    private record CaseDraft(
            String type,
            String value,
            boolean expectedValidity,
            String reason,
            boolean requiresBusinessReview,
            Long sourceCodeSetId
    ) {
    }

    private static String nullToEmptyStatic(String value) {
        return value == null ? "" : value;
    }
}
