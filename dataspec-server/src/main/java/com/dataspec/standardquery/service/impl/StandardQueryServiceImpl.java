package com.dataspec.standardquery.service.impl;

import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSearchSummary;
import com.dataspec.field.service.FieldService;
import com.dataspec.standardquery.exception.StandardQueryValidationException;
import com.dataspec.standardquery.model.StandardQueryAppliedFilter;
import com.dataspec.standardquery.model.StandardQueryFilter;
import com.dataspec.standardquery.model.StandardQueryIgnoredFilter;
import com.dataspec.standardquery.model.StandardQueryNormalized;
import com.dataspec.standardquery.model.StandardQueryRequest;
import com.dataspec.standardquery.model.StandardQueryResult;
import com.dataspec.standardquery.model.StandardQuerySummary;
import com.dataspec.standardquery.model.StandardQueryValidationError;
import com.dataspec.standardquery.service.StandardQueryService;
import com.dataspec.standardref.model.StandardReferenceType;
import com.dataspec.standardref.service.StandardReferenceFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Standard Query DSL 只读执行器。
 *
 * <p>v1 只允许 FIELD target，并把 allowlist 过滤条件映射到既有字段搜索参数；
 * 这样可以复用字段搜索的排序、稳定引用、字段生命周期和安全边界，同时避免 DSL 变成任意表达式执行器。</p>
 */
@Service
@RequiredArgsConstructor
public class StandardQueryServiceImpl implements StandardQueryService {

    private static final String TARGET_FIELD = "FIELD";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_FILTER_COUNT = 20;
    private static final int MAX_TEXT_LENGTH = 500;
    private static final String VALIDATION_ERROR_CODE = "STANDARD_QUERY_DSL_INVALID";
    private static final List<String> SUPPORTED_OPERATORS = List.of("eq", "contains", "gte");
    private static final String BOUNDS = "text<=500, filters<=20, limit=1..50";
    private static final List<String> SUPPORTED_FIELDS = List.of(
            "category",
            "tag",
            "status",
            "sensitive",
            "sourceBatchId",
            "stableRef",
            "canonicalRef",
            "hasExample",
            "updatedSince");

    private final FieldService fieldService;

    @Override
    public StandardQueryResult search(StandardQueryRequest request) {
        QueryPlan plan = buildPlan(request);
        if (!plan.hasExecutableCriteria()) {
            return emptyResult(plan);
        }
        FieldSearchResult fieldResult = fieldService.search(plan.toFieldSearchReq());
        return toStandardQueryResult(plan, fieldResult);
    }

    @Override
    public StandardQueryResult searchFieldsFromLegacyParameters(
            Long projectId,
            String query,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            Integer limit) {
        List<StandardQueryFilter> filters = new ArrayList<>();
        addLegacyFilter(filters, "category", category);
        addLegacyFilter(filters, "tag", tag);
        addLegacyFilter(filters, "status", status);
        addLegacyFilter(filters, "sensitive", sensitive);
        addLegacyFilter(filters, "sourceBatchId", sourceBatchId);
        return search(new StandardQueryRequest(projectId, TARGET_FIELD, query, filters, null, limit, true, false));
    }

    private QueryPlan buildPlan(StandardQueryRequest request) {
        if (request == null || request.projectId() == null) {
            throw validationError("项目ID不能为空");
        }
        boolean strict = Boolean.TRUE.equals(request.strict());
        boolean explain = request.explain() == null || Boolean.TRUE.equals(request.explain());
        String target = normalizeTarget(request.target());
        String unsupportedTarget = null;
        if (!TARGET_FIELD.equals(target)) {
            String message = "Standard Query v1 暂不支持 target=" + safeText(target) + "，仅支持 FIELD";
            if (strict) {
                throw validationError(message);
            }
            unsupportedTarget = target;
            target = TARGET_FIELD;
        }
        String text = normalizeText(request.text());
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            throw validationError("Standard Query text 超过最大长度 " + MAX_TEXT_LENGTH);
        }
        int limit = normalizeLimit(request.limit());
        List<StandardQueryFilter> filters = request.filters() == null ? List.of() : request.filters();
        if (filters.size() > MAX_FILTER_COUNT) {
            throw validationError("Standard Query 过滤条件数量不能超过 " + MAX_FILTER_COUNT);
        }
        QueryPlan plan = new QueryPlan(request.projectId(), target, text, limit, explain, strict);
        if (unsupportedTarget != null) {
            plan.ignored.add(new StandardQueryIgnoredFilter(
                    "target",
                    "eq",
                    safeText(unsupportedTarget),
                    "Standard Query v1 暂不支持该 target，已按 FIELD 执行"));
        }
        for (StandardQueryFilter filter : filters) {
            applyFilter(plan, filter);
        }
        return plan;
    }

    private void applyFilter(QueryPlan plan, StandardQueryFilter filter) {
        String field = filter == null ? null : normalizeField(filter.field());
        String op = filter == null ? null : normalizeOp(filter.op());
        Object value = filter == null ? null : filter.value();
        if (field == null || !SUPPORTED_FIELDS.contains(field)) {
            ignoreOrFail(plan, filter, "不支持的过滤字段；支持字段: " + String.join(", ", SUPPORTED_FIELDS));
            return;
        }
        switch (field) {
            case "category" -> applyStringFilter(plan, filter, "category", op, value, "字段分类");
            case "tag" -> applyStringFilter(plan, filter, "tag", op == null ? "contains" : op, value, "字段标签");
            case "status" -> applyStringFilter(plan, filter, "status", op, value, "字段状态");
            case "sensitive" -> applyBooleanFilter(plan, filter, op, value);
            case "sourceBatchId" -> applyLongFilter(plan, filter, op, value);
            case "stableRef", "canonicalRef" -> applyRefFilter(plan, filter, field, op, value);
            case "hasExample" -> applyHasExampleFilter(plan, filter, op, value);
            case "updatedSince" -> applyUpdatedSinceFilter(plan, filter, op, value);
            default -> ignoreOrFail(plan, filter, "不支持的过滤字段");
        }
    }

    private void applyStringFilter(QueryPlan plan, StandardQueryFilter filter, String field, String op,
                                   Object value, String label) {
        String actualOp = op == null ? "eq" : op;
        if (!"eq".equals(actualOp) && !("tag".equals(field) && "contains".equals(actualOp))) {
            ignoreOrFail(plan, filter, field + " 仅支持 " + ("tag".equals(field) ? "eq/contains" : "eq"));
            return;
        }
        String textValue = normalizeText(stringValue(value));
        if (textValue == null) {
            ignoreOrFail(plan, filter, field + " 过滤值不能为空");
            return;
        }
        if ("category".equals(field)) {
            plan.category = textValue;
        } else if ("tag".equals(field)) {
            plan.tag = textValue;
        } else {
            plan.status = textValue;
        }
        plan.applied.add(new StandardQueryAppliedFilter(field, actualOp, safeText(textValue), label + " = " + safeText(textValue)));
    }

    private void applyBooleanFilter(QueryPlan plan, StandardQueryFilter filter, String op, Object value) {
        String actualOp = op == null ? "eq" : op;
        if (!"eq".equals(actualOp)) {
            ignoreOrFail(plan, filter, "sensitive 仅支持 eq");
            return;
        }
        Boolean parsed = booleanValue(value);
        if (parsed == null) {
            ignoreOrFail(plan, filter, "sensitive 过滤值必须是布尔值");
            return;
        }
        plan.sensitive = parsed;
        plan.applied.add(new StandardQueryAppliedFilter("sensitive", actualOp, String.valueOf(parsed), "敏感标记 = " + parsed));
    }

    private void applyLongFilter(QueryPlan plan, StandardQueryFilter filter, String op, Object value) {
        String actualOp = op == null ? "eq" : op;
        if (!"eq".equals(actualOp)) {
            ignoreOrFail(plan, filter, "sourceBatchId 仅支持 eq");
            return;
        }
        Long parsed = longValue(value);
        if (parsed == null || parsed <= 0) {
            ignoreOrFail(plan, filter, "sourceBatchId 必须是正整数");
            return;
        }
        plan.sourceBatchId = parsed;
        plan.applied.add(new StandardQueryAppliedFilter("sourceBatchId", actualOp, String.valueOf(parsed), "来源批次 = " + parsed));
    }

    private void applyRefFilter(QueryPlan plan, StandardQueryFilter filter, String field, String op, Object value) {
        String actualOp = op == null ? "eq" : op;
        if (!"eq".equals(actualOp)) {
            ignoreOrFail(plan, filter, field + " 仅支持 eq");
            return;
        }
        String ref = normalizeText(stringValue(value));
        if (ref == null) {
            ignoreOrFail(plan, filter, field + " 过滤值不能为空");
            return;
        }
        Long id = parseFieldRefId(plan.projectId, ref);
        if (id == null) {
            ignoreOrFail(plan, filter, field + " 需要形如 field:<projectId>:<fieldId> 且 projectId 匹配当前查询项目的引用");
            return;
        }
        plan.refField = field;
        plan.refValue = ref;
        plan.refFieldId = id;
        plan.applied.add(new StandardQueryAppliedFilter(field, actualOp, safeText(ref), field + " = " + safeText(ref)));
    }

    private void applyHasExampleFilter(QueryPlan plan, StandardQueryFilter filter, String op, Object value) {
        String actualOp = op == null ? "eq" : op;
        if (!"eq".equals(actualOp)) {
            ignoreOrFail(plan, filter, "hasExample 仅支持 eq");
            return;
        }
        Boolean parsed = booleanValue(value);
        if (parsed == null) {
            ignoreOrFail(plan, filter, "hasExample 过滤值必须是布尔值");
            return;
        }
        plan.hasExample = parsed;
        plan.applied.add(new StandardQueryAppliedFilter("hasExample", actualOp, String.valueOf(parsed), "是否有示例 = " + parsed));
    }

    private void applyUpdatedSinceFilter(QueryPlan plan, StandardQueryFilter filter, String op, Object value) {
        String actualOp = op == null ? "gte" : op;
        if (!"gte".equals(actualOp)) {
            ignoreOrFail(plan, filter, "updatedSince 仅支持 gte");
            return;
        }
        String textValue = stringValue(value);
        try {
            plan.updatedSince = LocalDateTime.parse(textValue);
            plan.applied.add(new StandardQueryAppliedFilter(
                    "updatedSince",
                    actualOp,
                    safeText(textValue),
                    "更新时间 >= " + safeText(textValue)));
        } catch (DateTimeParseException | NullPointerException ex) {
            ignoreOrFail(plan, filter, "updatedSince 必须是 ISO-8601 LocalDateTime");
        }
    }

    private void ignoreOrFail(QueryPlan plan, StandardQueryFilter filter, String reason) {
        if (plan.strict) {
            throw validationError(reason + "；字段=" + safeText(filter == null ? null : filter.field())
                    + "，值=" + safeText(stringValue(filter == null ? null : filter.value())));
        }
        plan.ignored.add(new StandardQueryIgnoredFilter(
                filter == null ? null : filter.field(),
                filter == null ? null : filter.op(),
                safeText(stringValue(filter == null ? null : filter.value())),
                safeText(reason)));
    }

    private StandardQueryResult toStandardQueryResult(QueryPlan plan, FieldSearchResult fieldResult) {
        List<StandardQueryAppliedFilter> applied = new ArrayList<>(plan.applied);
        if (fieldResult.summary() != null && fieldResult.summary().appliedFilters() != null) {
            for (Map.Entry<String, Object> entry : fieldResult.summary().appliedFilters().entrySet()) {
                if ("limit".equals(entry.getKey())) {
                    continue;
                }
                if (applied.stream().noneMatch(filter -> filter.field().equals(entry.getKey()))) {
                    applied.add(new StandardQueryAppliedFilter(
                            entry.getKey(),
                            "eq",
                            safeText(stringValue(entry.getValue())),
                            "legacy 字段搜索过滤: " + entry.getKey()));
                }
            }
        }
        FieldSearchSummary fieldSummary = fieldResult.summary();
        int resultCount = fieldSummary == null ? fieldResult.items().size() : fieldSummary.matchedCount();
        int returnedCount = fieldSummary == null ? fieldResult.items().size() : fieldSummary.returnedCount();
        boolean truncated = fieldSummary != null && fieldSummary.truncated();
        List<String> hints = new ArrayList<>();
        if (!plan.ignored.isEmpty()) {
            hints.add("部分过滤条件未应用；支持字段: " + String.join(", ", SUPPORTED_FIELDS));
        }
        if (fieldSummary != null && fieldSummary.hints() != null) {
            hints.addAll(fieldSummary.hints().stream()
                    .map(StandardQueryServiceImpl::safeText)
                    .toList());
        }
        StandardQuerySummary querySummary = new StandardQuerySummary(
                plan.target,
                safeText(plan.text),
                resultCount,
                returnedCount,
                truncated,
                List.copyOf(hints));
        StandardQueryNormalized normalized = new StandardQueryNormalized(
                plan.target,
                safeText(plan.text),
                List.copyOf(applied),
                List.of(),
                plan.limit,
                plan.explain,
                plan.strict);
        return new StandardQueryResult(
                plan.projectId,
                normalized,
                querySummary,
                List.copyOf(applied),
                List.copyOf(plan.ignored),
                resultCount,
                returnedCount,
                truncated,
                List.copyOf(hints),
                fieldResult.items());
    }

    private StandardQueryResult emptyResult(QueryPlan plan) {
        List<String> hints = new ArrayList<>();
        if (!plan.ignored.isEmpty()) {
            hints.add("部分过滤条件未应用；支持字段: " + String.join(", ", SUPPORTED_FIELDS));
        }
        hints.add("未提供可执行查询条件；请补充 text 或支持的过滤字段。");
        StandardQuerySummary querySummary = new StandardQuerySummary(
                plan.target,
                safeText(plan.text),
                0,
                0,
                false,
                List.copyOf(hints));
        StandardQueryNormalized normalized = new StandardQueryNormalized(
                plan.target,
                safeText(plan.text),
                List.copyOf(plan.applied),
                List.of(),
                plan.limit,
                plan.explain,
                plan.strict);
        return new StandardQueryResult(
                plan.projectId,
                normalized,
                querySummary,
                List.copyOf(plan.applied),
                List.copyOf(plan.ignored),
                0,
                0,
                false,
                List.copyOf(hints),
                List.of());
    }

    private static void addLegacyFilter(List<StandardQueryFilter> filters, String field, Object value) {
        if (value != null) {
            filters.add(new StandardQueryFilter(field, null, value));
        }
    }

    private static String normalizeTarget(String target) {
        String normalized = normalizeText(target);
        return normalized == null ? TARGET_FIELD : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeField(String field) {
        String normalized = normalizeText(field);
        return normalized == null ? null : switch (normalized.toLowerCase(Locale.ROOT)) {
            case "sourcebatchid" -> "sourceBatchId";
            case "stableref" -> "stableRef";
            case "canonicalref" -> "canonicalRef";
            case "hasexample" -> "hasExample";
            case "updatedsince" -> "updatedSince";
            default -> normalized;
        };
    }

    private static String normalizeOp(String op) {
        String normalized = normalizeText(op);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw validationError("Standard Query limit 必须在 1 到 " + MAX_LIMIT + " 之间");
        }
        return limit;
    }

    private static Long parseFieldRefId(Long projectId, String ref) {
        return StandardReferenceFormatter.parse(ref)
                .filter(parsed -> parsed.type() == StandardReferenceType.FIELD)
                .filter(parsed -> parsed.projectId().equals(projectId))
                .map(StandardReferenceFormatter.ParsedStableReference::objectKey)
                .flatMap(StandardQueryServiceImpl::positiveLong)
                .orElse(null);
    }

    private static java.util.Optional<Long> positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? java.util.Optional.of(parsed) : java.util.Optional.empty();
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = normalizeText(stringValue(value));
        if (text == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(stringValue(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String safeText(String value) {
        return SensitiveDataSanitizer.redactText(value);
    }

    private static StandardQueryValidationException validationError(String message) {
        return new StandardQueryValidationException(new StandardQueryValidationError(
                VALIDATION_ERROR_CODE,
                SensitiveDataSanitizer.redactText(message),
                SUPPORTED_FIELDS,
                SUPPORTED_OPERATORS,
                BOUNDS));
    }

    private final class QueryPlan {
        private final Long projectId;
        private final String target;
        private final String text;
        private final int limit;
        private final boolean explain;
        private final boolean strict;
        private final List<StandardQueryAppliedFilter> applied = new ArrayList<>();
        private final List<StandardQueryIgnoredFilter> ignored = new ArrayList<>();
        private String category;
        private String tag;
        private String status;
        private Boolean sensitive;
        private Long sourceBatchId;
        private String refField;
        private String refValue;
        private Long refFieldId;
        private Boolean hasExample;
        private LocalDateTime updatedSince;

        private QueryPlan(Long projectId, String target, String text, int limit, boolean explain, boolean strict) {
            this.projectId = projectId;
            this.target = target;
            this.text = text;
            this.limit = limit;
            this.explain = explain;
            this.strict = strict;
        }

        private FieldSearchReq toFieldSearchReq() {
            Map<String, Object> extraFilters = new LinkedHashMap<>();
            if (refField != null) {
                extraFilters.put(refField, refValue);
            }
            if (hasExample != null) {
                extraFilters.put("hasExample", hasExample);
            }
            if (updatedSince != null) {
                extraFilters.put("updatedSince", updatedSince);
            }
            return new FieldSearchReq(
                    projectId,
                    text,
                    category,
                    tag,
                    status,
                    sensitive,
                    sourceBatchId,
                    limit,
                    extraFilters);
        }

        private boolean hasExecutableCriteria() {
            return text != null
                    || category != null
                    || tag != null
                    || status != null
                    || sensitive != null
                    || sourceBatchId != null
                    || refFieldId != null
                    || hasExample != null
                    || updatedSince != null;
        }
    }
}
