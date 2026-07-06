package com.dataspec.contractimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.contractimport.model.ContractCandidateDiagnostic;
import com.dataspec.contractimport.model.ContractCandidateField;
import com.dataspec.contractimport.model.ContractCandidatePreviewPackage;
import com.dataspec.contractimport.model.ContractCandidatePreviewReq;
import com.dataspec.contractimport.model.ContractCandidateSafety;
import com.dataspec.contractimport.model.ContractCandidateSummary;
import com.dataspec.contractimport.service.ContractCandidateImportService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认契约候选导入预览服务。
 *
 * <p>第一版只做本地确定性解析和只读预览：不访问外部 URL、不调用 LLM、
 * 不写正式标准字段，也不自动创建标准候选。</p>
 */
@Service
@RequiredArgsConstructor
public class ContractCandidateImportServiceImpl implements ContractCandidateImportService {

    private static final String KIND = "dataspec.contract-candidate-preview";
    private static final int SCHEMA_VERSION = 1;
    private static final int DEFAULT_MAX_CANDIDATES = 100;
    private static final int MAX_CANDIDATES_LIMIT = 500;
    private static final List<String> SUPPORTED_SOURCE_KINDS = List.of("openapi", "json-schema", "protobuf");
    private static final String ACTION_CREATE = "CREATE_CANDIDATE";
    private static final String ACTION_MERGE = "MERGE_EXISTING";
    private static final String ACTION_REVIEW = "REVIEW_REQUIRED";
    private static final Pattern PROTO_MESSAGE_PATTERN = Pattern.compile("\\bmessage\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\{?");
    private static final Pattern PROTO_FIELD_PATTERN = Pattern.compile(
            "^\\s*(?:(optional|required|repeated)\\s+)?([A-Za-z_][A-Za-z0-9_.]*)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(\\d+)\\s*(?:\\[[^]]+])?\\s*;.*$");
    private static final ObjectMapper CONTRACT_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper HASH_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final FieldService fieldService;

    @Override
    public ContractCandidatePreviewPackage preview(ContractCandidatePreviewReq req) {
        RequestContext context = validate(req);
        List<ContractCandidateDiagnostic> diagnostics = new ArrayList<>();
        List<RawCandidate> rawCandidates = switch (context.sourceKind()) {
            case "openapi" -> parseOpenApi(context.rawContractContent(), diagnostics);
            case "json-schema" -> parseJsonSchema(context.rawContractContent(), diagnostics);
            case "protobuf" -> parseProtobuf(context.rawContractContent(), diagnostics);
            default -> throw unsupportedSourceKind(context.sourceKind());
        };

        Map<String, Field> existingFields = existingFields(context.projectId());
        PreviewCandidates previewCandidates = buildCandidates(context, rawCandidates, existingFields);
        List<ContractCandidateField> candidates = previewCandidates.candidates();
        boolean truncated = false;
        if (candidates.size() > context.maxCandidates()) {
            truncated = true;
            diagnostics.add(new ContractCandidateDiagnostic(
                    "MAX_CANDIDATES_TRUNCATED",
                    "WARN",
                    "候选数量超过 maxCandidates，已按稳定排序截断。",
                    context.sourcePath()));
            candidates = candidates.subList(0, context.maxCandidates());
        }

        ContractCandidateSummary summary = new ContractCandidateSummary(
                rawCandidates.size(),
                candidates.size(),
                previewCandidates.duplicateCount(),
                previewCandidates.existingMatchCount(),
                diagnostics.size(),
                truncated);
        ContractCandidateSafety safety = new ContractCandidateSafety(
                true,
                false,
                false,
                false,
                false,
                sensitiveInputs(req));
        String hash = computeContractHash(context, summary, candidates);
        return new ContractCandidatePreviewPackage(
                KIND,
                SCHEMA_VERSION,
                context.projectId(),
                context.sourceKind(),
                context.sourcePath(),
                hash,
                summary,
                candidates,
                List.copyOf(diagnostics),
                safety,
                List.of(
                        "先人工复核 REVIEW_REQUIRED 候选和 diagnostics。",
                        "确认业务语义后，可将单个候选的 inboxPayload 提交到现有标准候选审核流程。",
                        "预览不会自动写入标准字段或候选库。"));
    }

    private RequestContext validate(ContractCandidatePreviewReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("projectId 不能为空");
        }
        String sourceKind = nullToEmpty(req.sourceKind()).trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SOURCE_KINDS.contains(sourceKind)) {
            throw unsupportedSourceKind(req.sourceKind());
        }
        String contractContent = nullToEmpty(req.contractContent());
        if (contractContent.isBlank()) {
            throw new BizException("contractContent 不能为空");
        }
        if (contractContent.length() > 524288) {
            throw new BizException("contractContent 不能超过 512KB");
        }
        int maxCandidates = req.maxCandidates() == null
                ? DEFAULT_MAX_CANDIDATES
                : Math.min(Math.max(req.maxCandidates(), 1), MAX_CANDIDATES_LIMIT);
        return new RequestContext(
                req.projectId(),
                sourceKind,
                safeText(req.sourcePath()),
                safeFullText(req.sourcePath()),
                contractContent,
                safeFullText(contractContent),
                maxCandidates);
    }

    private BizException unsupportedSourceKind(String sourceKind) {
        return new BizException("不支持的契约来源类型: "
                + safeText(sourceKind)
                + "。支持: "
                + String.join(", ", SUPPORTED_SOURCE_KINDS));
    }

    private List<RawCandidate> parseOpenApi(String content, List<ContractCandidateDiagnostic> diagnostics) {
        JsonNode root = readContract(content);
        List<RawCandidate> candidates = new ArrayList<>();
        JsonNode schemas = root.path("components").path("schemas");
        if (schemas.isObject()) {
            schemas.fields().forEachRemaining(entry ->
                    extractSchemaFields(
                            entry.getValue(),
                            "#/components/schemas/" + escapePath(entry.getKey()),
                            candidates,
                            diagnostics));
        }
        collectEmbeddedSchemas(root.path("paths"), "#/paths", candidates, diagnostics);
        collectEmbeddedSchemas(root.path("components").path("requestBodies"), "#/components/requestBodies", candidates, diagnostics);
        collectEmbeddedSchemas(root.path("components").path("responses"), "#/components/responses", candidates, diagnostics);
        return candidates;
    }

    private List<RawCandidate> parseJsonSchema(String content, List<ContractCandidateDiagnostic> diagnostics) {
        JsonNode root = readContract(content);
        List<RawCandidate> candidates = new ArrayList<>();
        extractSchemaFields(root, "#", candidates, diagnostics);
        return candidates;
    }

    private List<RawCandidate> parseProtobuf(String content, List<ContractCandidateDiagnostic> diagnostics) {
        String trimmed = content.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return parseProtobufDescriptor(CONTRACT_MAPPER.readTree(trimmed));
            } catch (JsonProcessingException ex) {
                diagnostics.add(new ContractCandidateDiagnostic(
                        "PROTOBUF_DESCRIPTOR_PARSE_FAILED",
                        "WARN",
                        "Protobuf descriptor JSON 解析失败: " + safeText(ex.getOriginalMessage()),
                        "#"));
                return List.of();
            }
        }
        return parseProtoText(content);
    }

    private void collectEmbeddedSchemas(
            JsonNode node,
            String path,
            List<RawCandidate> candidates,
            List<ContractCandidateDiagnostic> diagnostics) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            if (node.has("schema")) {
                extractSchemaFields(node.path("schema"), path + "/schema", candidates, diagnostics);
            }
            node.fields().forEachRemaining(entry ->
                    collectEmbeddedSchemas(entry.getValue(), path + "/" + escapePath(entry.getKey()), candidates, diagnostics));
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectEmbeddedSchemas(node.get(i), path + "/" + i, candidates, diagnostics);
            }
        }
    }

    private void extractSchemaFields(
            JsonNode schema,
            String basePath,
            List<RawCandidate> candidates,
            List<ContractCandidateDiagnostic> diagnostics) {
        if (schema == null || schema.isMissingNode() || schema.isNull()) {
            return;
        }
        if (hasComposition(schema)) {
            diagnostics.add(compositionDiagnostic(basePath, schema));
        }
        JsonNode items = schema.path("items");
        if (!items.isMissingNode() && !items.isNull()) {
            // 数组响应的字段语义在 items 中，递归抽取可覆盖 OpenAPI 列表接口的常见契约。
            extractSchemaFields(items, basePath + "/items", candidates, diagnostics);
        }
        Set<String> required = requiredNames(schema.path("required"));
        JsonNode properties = schema.path("properties");
        if (!properties.isObject()) {
            return;
        }
        properties.fields().forEachRemaining(entry -> {
            String propertyName = entry.getKey();
            JsonNode property = entry.getValue();
            String sourcePath = safeSourcePath(basePath + "/properties/" + escapePath(propertyName));
            boolean reviewRequired = hasComposition(property) || property.has("properties") || property.has("items");
            List<String> reasons = new ArrayList<>();
            if (reviewRequired) {
                ContractCandidateDiagnostic diagnostic = compositionDiagnostic(sourcePath, property);
                diagnostics.add(diagnostic);
                reasons.add("复杂 schema 需要人工复核");
            }
            candidates.add(new RawCandidate(
                    propertyName,
                    safeText(firstNonBlank(textValue(property, "description"), textValue(property, "title"), propertyName)),
                    resolveJsonDataType(property, reviewRequired),
                    required.contains(propertyName),
                    stringValues(property.path("enum"), 20),
                    exampleValues(property),
                    sourcePath,
                    reviewRequired,
                    List.copyOf(reasons)));
        });
    }

    private List<RawCandidate> parseProtoText(String content) {
        List<RawCandidate> candidates = new ArrayList<>();
        String currentMessage = null;
        String pendingComment = null;
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//")) {
                pendingComment = trimmed.substring(2).trim();
                continue;
            }
            Matcher messageMatcher = PROTO_MESSAGE_PATTERN.matcher(trimmed);
            if (messageMatcher.find()) {
                currentMessage = messageMatcher.group(1);
                pendingComment = null;
                continue;
            }
            if (trimmed.startsWith("}")) {
                currentMessage = null;
                pendingComment = null;
                continue;
            }
            Matcher fieldMatcher = PROTO_FIELD_PATTERN.matcher(line);
            if (currentMessage != null && fieldMatcher.matches()) {
                String label = fieldMatcher.group(1);
                String type = fieldMatcher.group(2);
                String name = fieldMatcher.group(3);
                String number = fieldMatcher.group(4);
                candidates.add(new RawCandidate(
                        name,
                        safeText(firstNonBlank(pendingComment, name)),
                        resolveProtoDataType(type),
                        "required".equals(label),
                        List.of(),
                        List.of(),
                        safeSourcePath("proto://" + currentMessage + "/" + name + "#" + number),
                        false,
                        List.of()));
                pendingComment = null;
            }
        }
        return candidates;
    }

    private List<RawCandidate> parseProtobufDescriptor(JsonNode root) {
        List<RawCandidate> candidates = new ArrayList<>();
        parseDescriptorMessages(root.path("messageType"), candidates);
        JsonNode files = root.path("file");
        if (files.isArray()) {
            for (JsonNode file : files) {
                parseDescriptorMessages(file.path("messageType"), candidates);
            }
        }
        return candidates;
    }

    private void parseDescriptorMessages(JsonNode messages, List<RawCandidate> candidates) {
        if (!messages.isArray()) {
            return;
        }
        for (JsonNode message : messages) {
            String messageName = safePathSegment(firstNonBlank(textValue(message, "name"), "Message"));
            JsonNode fields = message.path("field");
            if (!fields.isArray()) {
                continue;
            }
            for (JsonNode field : fields) {
                String name = textValue(field, "name");
                if (name.isBlank()) {
                    continue;
                }
                String number = textValue(field, "number");
                candidates.add(new RawCandidate(
                        name,
                        safeText(firstNonBlank(textValue(field, "jsonName"), name)),
                        resolveProtoDataType(textValue(field, "type")),
                        false,
                        List.of(),
                        List.of(),
                        safeSourcePath("descriptor://" + messageName + "/" + safePathSegment(name) + "#" + number),
                        false,
                        List.of()));
            }
        }
    }

    private PreviewCandidates buildCandidates(
            RequestContext context,
            List<RawCandidate> rawCandidates,
            Map<String, Field> existingFields) {
        Map<String, CandidateAccumulator> accumulators = new LinkedHashMap<>();
        int duplicateCount = 0;
        for (RawCandidate raw : rawCandidates) {
            String candidateName = normalizeCandidateName(raw.name());
            if (candidateName.isBlank()) {
                continue;
            }
            CandidateAccumulator accumulator = accumulators.computeIfAbsent(candidateName,
                    key -> new CandidateAccumulator(context.sourceKind(), candidateName));
            if (!accumulator.sourcePaths.isEmpty()) {
                duplicateCount++;
            }
            accumulator.merge(raw);
        }

        List<ContractCandidateField> candidates = new ArrayList<>();
        int existingMatchCount = 0;
        for (CandidateAccumulator accumulator : accumulators.values()) {
            Field existing = existingFields.get(accumulator.candidateName);
            if (existing != null) {
                existingMatchCount++;
                accumulator.conflictReasons.add("已有标准字段: " + safeText(existing.getName()));
            }
            String recommendedAction = accumulator.reviewRequired
                    ? ACTION_REVIEW
                    : existing != null ? ACTION_MERGE : ACTION_CREATE;
            int confidence = accumulator.reviewRequired ? 55 : existing != null ? 92 : 82;
            List<String> conflictReasons = accumulator.conflictReasons.stream()
                    .map(this::safeText)
                    .distinct()
                    .toList();
            StandardCandidateCreateReq payload = new StandardCandidateCreateReq(
                    context.projectId(),
                    accumulator.candidateName,
                    accumulator.displayName(),
                    accumulator.dataType(),
                    accumulator.comment(),
                    "CONTRACT_IMPORT",
                    safeSourcePath(context.sourceKind() + ":" + accumulator.primarySourcePath()),
                    evidenceJson(context, accumulator, recommendedAction, conflictReasons),
                    confidence);
            candidates.add(new ContractCandidateField(
                    context.sourceKind() + ":" + accumulator.candidateName,
                    accumulator.candidateName,
                    accumulator.displayName(),
                    accumulator.dataType(),
                    accumulator.required,
                    accumulator.enumValues.stream().toList(),
                    accumulator.exampleValues.stream().toList(),
                    accumulator.primarySourcePath(),
                    SCHEMA_VERSION,
                    confidence,
                    conflictReasons,
                    recommendedAction,
                    payload));
        }
        candidates.sort(Comparator.comparing(ContractCandidateField::candidateName));
        return new PreviewCandidates(List.copyOf(candidates), duplicateCount, existingMatchCount);
    }

    private Map<String, Field> existingFields(Long projectId) {
        List<Field> fields = fieldService.listByProject(projectId);
        if (fields == null || fields.isEmpty()) {
            return Map.of();
        }
        Map<String, Field> result = new LinkedHashMap<>();
        fields.stream()
                .filter(field -> field != null && !Boolean.TRUE.equals(field.getIsDeleted()))
                .filter(field -> !"disabled".equalsIgnoreCase(nullToEmpty(field.getStatus())))
                .sorted(Comparator.comparing((Field field) -> nullToEmpty(field.getName()))
                        .thenComparing(field -> field.getId() == null ? Long.MAX_VALUE : field.getId()))
                .forEach(field -> result.putIfAbsent(normalizeCandidateName(field.getName()), field));
        return result;
    }

    private JsonNode readContract(String content) {
        try {
            return CONTRACT_MAPPER.readTree(content);
        } catch (JsonProcessingException ex) {
            throw new BizException("契约内容解析失败: " + safeText(ex.getOriginalMessage()));
        }
    }

    private String computeContractHash(
            RequestContext context,
            ContractCandidateSummary summary,
            List<ContractCandidateField> candidates) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", SCHEMA_VERSION);
        payload.put("projectId", context.projectId());
        payload.put("sourceKind", context.sourceKind());
        payload.put("sourcePath", context.hashSourcePath());
        payload.put("contractContent", context.hashContractContent());
        payload.put("maxCandidates", context.maxCandidates());
        payload.put("summary", summary);
        payload.put("candidates", candidates.stream().map(candidate -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("candidateName", candidate.candidateName());
            item.put("dataType", candidate.dataType());
            item.put("required", candidate.required());
            item.put("sourcePath", candidate.sourcePath());
            item.put("recommendedAction", candidate.recommendedAction());
            item.put("conflictReasons", candidate.conflictReasons());
            return item;
        }).toList());
        try {
            byte[] json = HASH_MAPPER.writeValueAsBytes(payload);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new BizException(500, "契约候选预览 contractHash 计算失败");
        }
    }

    private String evidenceJson(
            RequestContext context,
            CandidateAccumulator accumulator,
            String recommendedAction,
            List<String> conflictReasons) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("sourceKind", context.sourceKind());
        evidence.put("sourcePath", context.sourcePath());
        evidence.put("fieldSourcePath", accumulator.primarySourcePath());
        evidence.put("sourcePaths", List.copyOf(accumulator.sourcePaths));
        evidence.put("recommendedAction", recommendedAction);
        evidence.put("conflictReasons", conflictReasons);
        try {
            return SensitiveDataSanitizer.redactText(HASH_MAPPER.writeValueAsString(evidence));
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"evidence serialization failed\"}";
        }
    }

    private List<String> sensitiveInputs(ContractCandidatePreviewReq req) {
        List<String> result = new ArrayList<>();
        if (SensitiveDataSanitizer.containsSensitiveText(req.sourcePath())) {
            result.add("sourcePath");
        }
        if (SensitiveDataSanitizer.containsSensitiveText(req.contractContent())) {
            result.add("contractContent");
        }
        return List.copyOf(result);
    }

    private ContractCandidateDiagnostic compositionDiagnostic(String sourcePath, JsonNode node) {
        return new ContractCandidateDiagnostic(
                "UNSUPPORTED_SCHEMA_COMPOSITION",
                "WARN",
                "复杂 schema 需要人工复核: " + safeText(firstNonBlank(textValue(node, "description"), node.toString())),
                safeSourcePath(sourcePath));
    }

    private boolean hasComposition(JsonNode node) {
        return node != null && (node.has("oneOf")
                || node.has("anyOf")
                || node.has("allOf")
                || node.has("$ref"));
    }

    private String resolveJsonDataType(JsonNode property, boolean reviewRequired) {
        if (reviewRequired && !property.has("type")) {
            return "json";
        }
        String type = textValue(property, "type").toLowerCase(Locale.ROOT);
        String format = textValue(property, "format").toLowerCase(Locale.ROOT);
        if ("integer".equals(type)) {
            return "int64".equals(format) ? "bigint" : "int";
        }
        if ("number".equals(type)) {
            return "decimal";
        }
        if ("boolean".equals(type)) {
            return "boolean";
        }
        if ("array".equals(type) || "object".equals(type)) {
            return "json";
        }
        if ("string".equals(type)) {
            if ("date-time".equals(format)) {
                return "timestamp";
            }
            if ("date".equals(format)) {
                return "date";
            }
            if (property.path("enum").isArray()) {
                return "varchar(64)";
            }
        }
        return "varchar(255)";
    }

    private String resolveProtoDataType(String type) {
        String normalized = nullToEmpty(type)
                .replace("TYPE_", "")
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "double", "float", "fixed32", "fixed64", "sfixed32", "sfixed64" -> "decimal";
            case "int32", "uint32", "sint32" -> "int";
            case "int64", "uint64", "sint64" -> "bigint";
            case "bool", "boolean" -> "boolean";
            case "string" -> "varchar(255)";
            case "bytes" -> "binary";
            default -> "json";
        };
    }

    private Set<String> requiredNames(JsonNode requiredNode) {
        Set<String> result = new LinkedHashSet<>();
        if (!requiredNode.isArray()) {
            return result;
        }
        for (JsonNode item : requiredNode) {
            result.add(item.asText(""));
        }
        return result;
    }

    private List<String> exampleValues(JsonNode property) {
        List<String> values = new ArrayList<>();
        JsonNode example = property.path("example");
        if (!example.isMissingNode() && !example.isNull()) {
            values.add(safeText(example.asText(example.toString())));
        }
        JsonNode examples = property.path("examples");
        if (examples.isArray()) {
            for (JsonNode item : examples) {
                values.add(safeText(item.asText(item.toString())));
                if (values.size() >= 5) {
                    break;
                }
            }
        }
        return values.stream().filter(value -> !value.isBlank()).distinct().toList();
    }

    private List<String> stringValues(JsonNode node, int limit) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(safeText(item.asText(item.toString())));
            if (values.size() >= limit) {
                break;
            }
        }
        return values.stream().filter(value -> !value.isBlank()).distinct().toList();
    }

    private String normalizeCandidateName(String value) {
        String safe = SensitiveDataSanitizer.redactText(nullToEmpty(value));
        String snake = safe
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return snake;
    }

    private String safeIdentifier(String value, String fallback) {
        String normalized = normalizeCandidateName(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String safePathSegment(String value) {
        return safeText(value)
                .replace("/", "_")
                .replace("\\", "_")
                .replaceAll("\\s+", "_");
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String escapePath(String value) {
        return nullToEmpty(value).replace("~", "~0").replace("/", "~1");
    }

    private String safeText(String text) {
        return SensitiveDataSanitizer.redactText(nullToEmpty(text), 500);
    }

    private String safeFullText(String text) {
        return SensitiveDataSanitizer.redactText(nullToEmpty(text));
    }

    private String safeSourcePath(String path) {
        return SensitiveDataSanitizer.redactText(nullToEmpty(path), 1000);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record RequestContext(
            Long projectId,
            String sourceKind,
            String sourcePath,
            String hashSourcePath,
            String rawContractContent,
            String hashContractContent,
            int maxCandidates
    ) {
    }

    private record RawCandidate(
            String name,
            String displayName,
            String dataType,
            boolean required,
            List<String> enumValues,
            List<String> exampleValues,
            String sourcePath,
            boolean reviewRequired,
            List<String> reviewReasons
    ) {
    }

    private record PreviewCandidates(
            List<ContractCandidateField> candidates,
            int duplicateCount,
            int existingMatchCount
    ) {
    }

    private static final class CandidateAccumulator {
        private final String sourceKind;
        private final String candidateName;
        private final List<String> sourcePaths = new ArrayList<>();
        private final Set<String> enumValues = new LinkedHashSet<>();
        private final Set<String> exampleValues = new LinkedHashSet<>();
        private final List<String> conflictReasons = new ArrayList<>();
        private String displayName;
        private String dataType;
        private boolean required;
        private boolean reviewRequired;

        private CandidateAccumulator(String sourceKind, String candidateName) {
            this.sourceKind = sourceKind;
            this.candidateName = candidateName;
        }

        private void merge(RawCandidate raw) {
            if (!sourcePaths.isEmpty() && !sourcePaths.contains(raw.sourcePath())) {
                conflictReasons.add("同一契约重复来源: " + raw.sourcePath());
            }
            sourcePaths.add(raw.sourcePath());
            enumValues.addAll(raw.enumValues());
            exampleValues.addAll(raw.exampleValues());
            if (displayName == null || displayName.isBlank() || displayName.equals(candidateName)) {
                displayName = raw.displayName();
            }
            if (dataType == null || dataType.isBlank() || "varchar(255)".equals(dataType)) {
                dataType = raw.dataType();
            }
            required = required || raw.required();
            reviewRequired = reviewRequired || raw.reviewRequired();
            conflictReasons.addAll(raw.reviewReasons());
        }

        private String displayName() {
            return displayName == null || displayName.isBlank() ? candidateName : displayName;
        }

        private String dataType() {
            return dataType == null || dataType.isBlank() ? "varchar(255)" : dataType;
        }

        private String comment() {
            return displayName();
        }

        private String primarySourcePath() {
            return sourcePaths.isEmpty() ? sourceKind + "://" + candidateName : sourcePaths.getFirst();
        }
    }
}
