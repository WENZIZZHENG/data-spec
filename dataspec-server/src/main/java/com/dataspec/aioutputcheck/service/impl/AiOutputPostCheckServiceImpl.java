package com.dataspec.aioutputcheck.service.impl;

import com.dataspec.aioutputcheck.model.AiOutputContentType;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckIssue;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckIssueSeverity;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckStatus;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckSummary;
import com.dataspec.aioutputcheck.service.AiOutputPostCheckService;
import com.dataspec.aioutputcheck.service.AiOutputPostCheckReceipt;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolution;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolutionStatus;
import com.dataspec.evidenceclaim.service.EvidenceClaimResolver;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.TableDef;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.reviewfinding.model.ReviewFinding;
import com.dataspec.reviewfinding.model.ReviewFindingSeverity;
import com.dataspec.reviewfinding.model.ReviewFindingSource;
import com.dataspec.reviewfinding.model.ReviewFindingSubject;
import com.dataspec.reviewfinding.model.ReviewFindingWaiver;
import com.dataspec.reviewfinding.service.ReviewFindingAdapter;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import com.dataspec.standardref.service.StandardReferenceFormatter;
import com.dataspec.standardref.model.StandardReferenceResolveRequest;
import com.dataspec.standardref.model.StandardReferenceResolutionResult;
import com.dataspec.standardref.model.StandardReferenceResolutionStatus;
import com.dataspec.standardref.model.StandardReferenceType;
import com.dataspec.standardref.service.StandardReferenceResolutionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 输出后置校验实现。
 *
 * <p>实现只做本地确定性检查：从 AI 文本中提取显式 stableRef、DDL/SQL 字段 identifier、
 * Markdown 反引号字段、快照声明和 evidence ref，再复用标准引用解析服务判断 current/stale/unknown。
 * 该流程不调用外部 LLM，不保存 raw content，也不修改任何项目或业务文件。</p>
 */
@Service
@RequiredArgsConstructor
public class AiOutputPostCheckServiceImpl implements AiOutputPostCheckService {

    private static final int MAX_CONTENT_LENGTH = 20_000;
    private static final int MAX_FINDINGS = 100;
    private static final int EXCERPT_RADIUS = 60;
    private static final Pattern STABLE_REF_PATTERN = Pattern.compile(
            "(?i)\\b(field|enum|rule|snapshot):\\d+:[A-Za-z0-9_.-]+");
    private static final Pattern BACKTICK_PATTERN = Pattern.compile("`([A-Za-z_][A-Za-z0-9_]*)`");
    private static final Pattern EVIDENCE_PATTERN = Pattern.compile("dataspec://evidence/[^\\s`\"')]+");
    private static final String EVIDENCE_TRAILING_PUNCTUATION = ".,;:!?，。；：！？)]}>）】》";
    private static final Pattern SQL_STRING_LITERAL_PATTERN = Pattern.compile("'(?:''|[^'])*'");
    private static final Pattern SQL_SELECT_FROM_PATTERN = Pattern.compile("(?is)\\bselect\\b(.+?)\\bfrom\\b");
    private static final Pattern SQL_ALIAS_PATTERN = Pattern.compile("(?i)\\s+as\\s+[A-Za-z_][A-Za-z0-9_]*\\s*$");
    private static final Pattern SQL_TRAILING_ALIAS_PATTERN = Pattern.compile(
            "(?is)^(.*\\S)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*$");
    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("(?i)^[A-Za-z_][A-Za-z0-9_]*$");
    private static final Pattern SQL_SELECT_IDENTIFIER_PATTERN = Pattern.compile(
            "(?i)(?:\\b[A-Za-z_][A-Za-z0-9_]*\\.)?\\b([A-Za-z_][A-Za-z0-9_]*)\\b(?!\\s*\\()");
    private static final Pattern SQL_CONDITION_IDENTIFIER_PATTERN = Pattern.compile(
            "(?i)(?:\\b[A-Za-z_][A-Za-z0-9_]*\\.)?\\b([A-Za-z_][A-Za-z0-9_]*)\\b\\s*(?:=|<>|!=|<=|>=|<|>|\\bis\\b|\\bin\\b|\\blike\\b)");
    private static final Pattern ENUM_VALUE_ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)\\b(enum:\\d+:\\d+)\\s*=\\s*([A-Za-z0-9_.-]+)");
    private static final Pattern JSON_ENUM_VALUE_PATTERN = Pattern.compile(
            "(?is)\"enumRef\"\\s*:\\s*\"(enum:\\d+:\\d+)\"\\s*,\\s*\"enumValue\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RULE_CODE_CLAIM_PATTERN = Pattern.compile(
            "(?is)\"ruleCode\"\\s*:\\s*\"([A-Za-z0-9_.-]+)\"|\\bruleCode\\s*=\\s*([A-Za-z0-9_.-]+)");
    private static final Set<String> SQL_KEYWORDS = Set.of(
            "select", "from", "where", "and", "or", "is", "not", "null", "as", "on", "join", "left",
            "right", "inner", "outer", "create", "table", "if", "exists", "primary", "key", "default",
            "varchar", "bigint", "int", "integer", "text", "timestamp", "date", "numeric", "decimal",
            "boolean", "true", "false", "constraint", "references", "unique", "with", "group", "by",
            "order", "limit", "offset", "insert", "update", "delete", "values", "set", "in", "having");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final StandardReferenceResolutionService referenceResolutionService;
    private final EvidenceClaimResolver evidenceClaimResolver;
    private final SqlParserService sqlParserService;
    private final EnumDictRepository enumDictRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final StandardSnapshotRepository standardSnapshotRepository;

    @Override
    public AiOutputPostCheckResult check(AiOutputPostCheckRequest request) {
        validate(request);
        ProjectAccessGuard.requireProjectAccess(request.projectId());

        List<Occurrence> occurrences = extractOccurrences(request);
        List<EvidenceClaimCheck> evidenceChecks = resolveEvidenceClaims(request.projectId(), request.content());
        List<AiOutputPostCheckIssue> evidenceIssues = evidenceClaimIssues(request.content(), evidenceChecks);
        List<StandardReferenceResolutionResult> resolvedRefs = resolveOccurrences(request.projectId(), occurrences);
        ExternalFindingCheck externalFindingCheck = checkExternalFindings(request.projectId(), request.findings());
        List<AiOutputPostCheckIssue> issues = new ArrayList<>(evidenceIssues);
        issues.addAll(explicitClaimIssues(request));
        issues.addAll(referenceIssues(resolvedRefs, occurrences));
        issues.addAll(externalFindingCheck.issues());

        List<ReviewFinding> findings = new ArrayList<>(externalFindingCheck.findings());
        findings.addAll(ReviewFindingAdapter.fromPostCheckIssues(issues, request.projectId()));
        findings = ReviewFindingAdapter.deduplicate(findings);

        AiOutputPostCheckSummary summary = summary(resolvedRefs, issues);
        AiOutputPostCheckStatus status = status(issues);
        boolean safeToUse = status == AiOutputPostCheckStatus.PASS;
        String verificationReceipt = AiOutputPostCheckReceipt.issue(
                request.projectId(),
                status,
                safeToUse,
                externalFindingCheck.findings(),
                JSON_MAPPER);
        return new AiOutputPostCheckResult(
                AiOutputPostCheckResult.KIND,
                AiOutputPostCheckResult.SCHEMA_VERSION,
                request.projectId(),
                status,
                safeToUse,
                summary,
                issues,
                findings,
                resolvedRefs,
                suggestedFixes(issues),
                evidenceLinks(resolvedRefs, evidenceChecks, externalFindingCheck.findings()),
                nextActions(status),
                verificationReceipt);
    }

    private void validate(AiOutputPostCheckRequest request) {
        if (request == null || request.projectId() == null) {
            throw new BizException("projectId 不能为空");
        }
        if (request.contentType() == null) {
            throw new BizException("contentType 不能为空");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new BizException("content 不能为空");
        }
        if (request.content().codePointCount(0, request.content().length()) > MAX_CONTENT_LENGTH) {
            throw new BizException("content 不能超过 " + MAX_CONTENT_LENGTH + " 字符");
        }
        if (request.content().indexOf('\0') >= 0) {
            throw new BizException("content 包含不支持的二进制字符");
        }
        if (request.findings().size() > MAX_FINDINGS) {
            throw new BizException("findings 不能超过 " + MAX_FINDINGS + " 条");
        }
        for (int index = 0; index < request.findings().size(); index++) {
            ReviewFinding finding = request.findings().get(index);
            if (finding == null) {
                throw new BizException("findings[" + index + "] 不能为空");
            }
            if (finding.code() == null || finding.code().isBlank()) {
                throw new BizException("findings[" + index + "].code 不能为空");
            }
        }
    }

    private ExternalFindingCheck checkExternalFindings(Long projectId, List<ReviewFinding> submittedFindings) {
        if (submittedFindings == null || submittedFindings.isEmpty()) {
            return new ExternalFindingCheck(List.of(), List.of());
        }
        List<ReviewFinding> findings = new ArrayList<>();
        List<AiOutputPostCheckIssue> issues = new ArrayList<>();
        for (ReviewFinding submitted : submittedFindings) {
            boolean highImpact = submitted.severity() == ReviewFindingSeverity.ERROR
                    || (submitted.confidence() != null && submitted.confidence() >= 80)
                    || submitted.autoFixSafe();
            LinkedHashSet<String> verifiedRefs = new LinkedHashSet<>();
            if (submitted.evidenceRefs().isEmpty()) {
                issues.add(findingEvidenceIssue(
                        "MISSING_FINDING_EVIDENCE_REFERENCE",
                        highImpact,
                        submitted,
                        null,
                        "结构化 finding 未提供可验证 evidence ref。"));
            } else {
                for (String evidenceRef : submitted.evidenceRefs()) {
                    EvidenceClaimResolution resolution = evidenceClaimResolver.resolve(projectId, evidenceRef);
                    if (resolution != null && resolution.status() == EvidenceClaimResolutionStatus.VERIFIED
                            && resolution.canonicalRef() != null) {
                        verifiedRefs.add(resolution.canonicalRef());
                        continue;
                    }
                    EvidenceClaimResolutionStatus resolutionStatus = resolution == null
                            ? EvidenceClaimResolutionStatus.UNVERIFIABLE : resolution.status();
                    String code = switch (resolutionStatus) {
                        case MISSING -> "MISSING_FINDING_EVIDENCE_REFERENCE";
                        case CROSS_PROJECT -> "CROSS_PROJECT_FINDING_EVIDENCE_REFERENCE";
                        case UNVERIFIABLE, VERIFIED -> "UNVERIFIABLE_FINDING_EVIDENCE_REFERENCE";
                    };
                    String message = switch (resolutionStatus) {
                        case MISSING -> "结构化 finding 引用的 Evidence 来源不存在。";
                        case CROSS_PROJECT -> "结构化 finding 引用了其他项目的 Evidence 来源。";
                        case UNVERIFIABLE, VERIFIED -> "结构化 finding 的 Evidence ref 无法确定性验证。";
                    };
                    issues.add(findingEvidenceIssue(code, highImpact, submitted,
                            resolution == null ? evidenceRef : resolution.inputRef(), message));
                }
            }

            ReviewFindingSubject subject = submitted.subject() == null
                    ? new ReviewFindingSubject(projectId, "AI_OUTPUT", null, null, null, null)
                    : new ReviewFindingSubject(projectId, submitted.subject().kind(), submitted.subject().name(),
                            submitted.subject().tableName(), submitted.subject().columnName(), submitted.subject().stableRef());
            findings.add(new ReviewFinding(
                    ReviewFindingSource.EXTERNAL_AI,
                    null,
                    submitted.code(),
                    submitted.severity(),
                    subject,
                    submitted.location(),
                    submitted.trigger(),
                    submitted.expected(),
                    submitted.observed(),
                    List.copyOf(verifiedRefs),
                    submitted.confidence(),
                    submitted.suggestedFix(),
                    false,
                    ReviewFindingWaiver.NONE));
        }
        return new ExternalFindingCheck(
                ReviewFindingAdapter.deduplicate(findings),
                List.copyOf(issues));
    }

    private AiOutputPostCheckIssue findingEvidenceIssue(
            String code,
            boolean highImpact,
            ReviewFinding finding,
            String evidenceRef,
            String message
    ) {
        return new AiOutputPostCheckIssue(
                code,
                highImpact ? AiOutputPostCheckIssueSeverity.FAIL : AiOutputPostCheckIssueSeverity.WARN,
                null,
                sanitize(evidenceRef),
                sanitize(message),
                sanitize(finding.observed()),
                null,
                List.of(),
                highImpact
                        ? List.of("补充当前项目可解析的 canonical evidence ref 后重新执行 post-check。")
                        : List.of("补充 evidence ref，或人工确认该低影响 finding。"));
    }

    private List<Occurrence> extractOccurrences(AiOutputPostCheckRequest request) {
        Map<String, Occurrence> occurrences = new LinkedHashMap<>();
        addStableRefs(request.content(), occurrences);
        switch (request.contentType()) {
            case DDL -> addDdlFieldIdentifiers(request.content(), occurrences);
            case SQL -> addSqlFieldIdentifiers(request.content(), occurrences);
            case MARKDOWN -> addBacktickFields(request.content(), occurrences);
            case JSON, TEXT, PLAIN_TEXT -> {
                // JSON 与普通文本第一版只信任显式 stableRef，避免把业务值误判为字段名。
            }
        }
        if (request.snapshotRef() != null && !request.snapshotRef().isBlank()) {
            addOccurrence(occurrences, StandardReferenceType.SNAPSHOT, request.snapshotRef(), 0, request.snapshotRef().length(), request.content(), true);
        }
        return occurrences.values().stream()
                .sorted(Comparator.comparing(Occurrence::refType).thenComparing(Occurrence::ref))
                .toList();
    }

    private void addStableRefs(String content, Map<String, Occurrence> occurrences) {
        Matcher matcher = STABLE_REF_PATTERN.matcher(content);
        while (matcher.find()) {
            StandardReferenceType refType = switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
                case "field" -> StandardReferenceType.FIELD;
                case "enum" -> StandardReferenceType.ENUM;
                case "rule" -> StandardReferenceType.RULE;
                case "snapshot" -> StandardReferenceType.SNAPSHOT;
                default -> null;
            };
            if (refType != null) {
                addOccurrence(occurrences, refType, matcher.group(), matcher.start(), matcher.end(), content, true);
            }
        }
    }

    private void addDdlFieldIdentifiers(String content, Map<String, Occurrence> occurrences) {
        try {
            for (TableDef table : sqlParserService.parse(content)) {
                if (table.getColumns() == null) {
                    continue;
                }
                table.getColumns().forEach(column -> {
                    String name = column.getName();
                    if (name != null && !name.isBlank()) {
                        int start = content.indexOf(name);
                        addOccurrence(occurrences, StandardReferenceType.FIELD, name, start, start + name.length(), content, true);
                    }
                });
            }
        } catch (IllegalArgumentException ex) {
            addSqlFieldIdentifiers(content, occurrences);
        }
    }

    private void addSqlFieldIdentifiers(String content, Map<String, Occurrence> occurrences) {
        String comparable = stripSqlStringLiterals(content);
        Matcher selectMatcher = SQL_SELECT_FROM_PATTERN.matcher(comparable);
        while (selectMatcher.find()) {
            addSqlSelectListIdentifiers(content, comparable, selectMatcher.start(1), selectMatcher.end(1), occurrences);
        }

        Matcher conditionMatcher = SQL_CONDITION_IDENTIFIER_PATTERN.matcher(comparable);
        while (conditionMatcher.find()) {
            String identifier = conditionMatcher.group(1);
            addSqlIdentifierOccurrence(content, occurrences, identifier, conditionMatcher.start(1), conditionMatcher.end(1));
        }
    }

    private void addSqlSelectListIdentifiers(
            String originalContent,
            String comparableContent,
            int selectStart,
            int selectEnd,
            Map<String, Occurrence> occurrences
    ) {
        String selectList = comparableContent.substring(selectStart, selectEnd);
        for (String expression : selectList.split(",")) {
            String withoutAlias = removeSqlSelectAlias(expression);
            Matcher identifierMatcher = SQL_SELECT_IDENTIFIER_PATTERN.matcher(withoutAlias);
            while (identifierMatcher.find()) {
                String identifier = identifierMatcher.group(1);
                int start = originalContent.indexOf(identifier, selectStart);
                addSqlIdentifierOccurrence(originalContent, occurrences, identifier, start, start + identifier.length());
            }
        }
    }

    private String removeSqlSelectAlias(String expression) {
        String withoutAsAlias = SQL_ALIAS_PATTERN.matcher(expression).replaceAll("");
        Matcher trailingAlias = SQL_TRAILING_ALIAS_PATTERN.matcher(withoutAsAlias.trim());
        if (!trailingAlias.matches()) {
            return withoutAsAlias;
        }
        String expressionBody = trailingAlias.group(1).trim();
        String alias = trailingAlias.group(2);
        if (isSqlKeyword(alias) || isSqlKeyword(expressionBody)) {
            return withoutAsAlias;
        }
        if (shouldDropImplicitSqlAlias(expressionBody)) {
            return expressionBody;
        }
        return withoutAsAlias;
    }

    private boolean shouldDropImplicitSqlAlias(String expressionBody) {
        if (SQL_IDENTIFIER_PATTERN.matcher(expressionBody).matches()) {
            return true;
        }
        return expressionBody.contains(".")
                || expressionBody.contains(")")
                || expressionBody.contains("]")
                || expressionBody.contains("'")
                || expressionBody.contains("\"")
                || expressionBody.contains("+")
                || expressionBody.contains("-")
                || expressionBody.contains("*")
                || expressionBody.contains("/")
                || expressionBody.contains("|");
    }

    private void addSqlIdentifierOccurrence(
            String content,
            Map<String, Occurrence> occurrences,
            String identifier,
            int start,
            int end
    ) {
        if (identifier == null || isSqlKeyword(identifier)) {
            return;
        }
        addOccurrence(occurrences, StandardReferenceType.FIELD, identifier, start, end, content, true);
    }

    private boolean isSqlKeyword(String value) {
        return value != null && SQL_KEYWORDS.contains(value.toLowerCase(Locale.ROOT));
    }

    private String stripSqlStringLiterals(String content) {
        Matcher matcher = SQL_STRING_LITERAL_PATTERN.matcher(content);
        StringBuilder builder = new StringBuilder(content);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                builder.setCharAt(i, ' ');
            }
        }
        return builder.toString();
    }

    private List<AiOutputPostCheckIssue> explicitClaimIssues(AiOutputPostCheckRequest request) {
        List<AiOutputPostCheckIssue> issues = new ArrayList<>();
        issues.addAll(enumValueClaimIssues(request));
        issues.addAll(ruleCodeClaimIssues(request));
        issues.addAll(snapshotDriftIssues(request));
        return issues;
    }

    private List<AiOutputPostCheckIssue> enumValueClaimIssues(AiOutputPostCheckRequest request) {
        List<AiOutputPostCheckIssue> issues = new ArrayList<>();
        addEnumValueClaimIssues(request, issues, ENUM_VALUE_ASSIGNMENT_PATTERN.matcher(request.content()), 1, 2);
        if (request.contentType() == AiOutputContentType.JSON) {
            addJsonEnumValueClaimIssues(request, issues);
        } else {
            addEnumValueClaimIssues(request, issues, JSON_ENUM_VALUE_PATTERN.matcher(request.content()), 1, 2);
        }
        return issues;
    }

    private void addEnumValueClaimIssues(
            AiOutputPostCheckRequest request,
            List<AiOutputPostCheckIssue> issues,
            Matcher matcher,
            int enumRefGroup,
            int valueGroup
    ) {
        while (matcher.find()) {
            String enumRef = matcher.group(enumRefGroup);
            String enumValue = matcher.group(valueGroup);
            addEnumValueClaimIssue(request, issues, enumRef, enumValue, matcher.start(), matcher.end());
        }
    }

    private void addJsonEnumValueClaimIssues(
            AiOutputPostCheckRequest request,
            List<AiOutputPostCheckIssue> issues
    ) {
        try {
            collectJsonEnumValueClaims(request, issues, JSON_MAPPER.readTree(request.content()));
        } catch (JsonProcessingException ex) {
            addEnumValueClaimIssues(request, issues, JSON_ENUM_VALUE_PATTERN.matcher(request.content()), 1, 2);
        }
    }

    private void collectJsonEnumValueClaims(
            AiOutputPostCheckRequest request,
            List<AiOutputPostCheckIssue> issues,
            JsonNode node
    ) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode enumRef = node.get("enumRef");
            JsonNode enumValue = node.get("enumValue");
            if (enumRef != null && enumRef.isTextual() && enumValue != null && enumValue.isTextual()) {
                int start = request.content().indexOf(enumRef.asText());
                int valueStart = request.content().indexOf(enumValue.asText(), Math.max(start, 0));
                int end = valueStart >= 0 ? valueStart + enumValue.asText().length() : start + enumRef.asText().length();
                addEnumValueClaimIssue(request, issues, enumRef.asText(), enumValue.asText(), start, end);
            }
            node.fields().forEachRemaining(entry -> collectJsonEnumValueClaims(request, issues, entry.getValue()));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectJsonEnumValueClaims(request, issues, child));
        }
    }

    private void addEnumValueClaimIssue(
            AiOutputPostCheckRequest request,
            List<AiOutputPostCheckIssue> issues,
            String enumRef,
            String enumValue,
            int start,
            int end
    ) {
        if (!isKnownEnumValue(request.projectId(), enumRef, enumValue)) {
            issues.add(claimIssue(
                    "INVALID_ENUM_VALUE",
                    AiOutputPostCheckIssueSeverity.FAIL,
                    StandardReferenceType.ENUM,
                    enumRef + "=" + enumValue,
                    "枚举值未在当前项目标准中声明。",
                    excerpt(request.content(), start, end),
                    List.of("改用当前枚举值，或先补充枚举标准后再采纳。")));
        }
    }

    private boolean isKnownEnumValue(Long projectId, String enumRef, String enumValue) {
        Optional<StandardReferenceFormatter.ParsedStableReference> parsed = StandardReferenceFormatter.parse(enumRef);
        if (parsed.isEmpty()
                || parsed.get().type() != StandardReferenceType.ENUM
                || !Objects.equals(projectId, parsed.get().projectId())) {
            return true;
        }
        Optional<Long> enumId = parseLong(parsed.get().objectKey());
        if (enumId.isEmpty()) {
            return true;
        }
        Optional<EnumDict> dict = safeOptional(enumDictRepository.findDictById(enumId.get()))
                .filter(item -> Objects.equals(projectId, item.getProjectId()));
        if (dict.isEmpty()) {
            return true;
        }
        var enumValues = enumDictRepository.findValuesByEnumId(enumId.get());
        if (enumValues == null) {
            return false;
        }
        return enumValues.stream()
                .anyMatch(value -> Objects.equals(value.getValue(), enumValue));
    }

    private List<AiOutputPostCheckIssue> ruleCodeClaimIssues(AiOutputPostCheckRequest request) {
        List<AiOutputPostCheckIssue> issues = new ArrayList<>();
        Matcher matcher = RULE_CODE_CLAIM_PATTERN.matcher(request.content());
        while (matcher.find()) {
            String ruleCode = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (ruleCodeRepositoryMisses(request.projectId(), ruleCode)) {
                issues.add(claimIssue(
                        "UNKNOWN_RULE_CODE",
                        AiOutputPostCheckIssueSeverity.FAIL,
                        StandardReferenceType.RULE,
                        ruleCode,
                        "规则编码未在当前项目中声明。",
                        excerpt(request.content(), matcher.start(), matcher.end()),
                        List.of("改用当前规则编码，或先补充项目规则配置。")));
            }
        }
        return issues;
    }

    private boolean ruleCodeRepositoryMisses(Long projectId, String ruleCode) {
        return safeOptional(ruleConfigRepository.findByCodeAndProjectId(ruleCode, projectId)).isEmpty();
    }

    private List<AiOutputPostCheckIssue> snapshotDriftIssues(AiOutputPostCheckRequest request) {
        List<AiOutputPostCheckIssue> issues = new ArrayList<>();
        Matcher matcher = STABLE_REF_PATTERN.matcher(request.content());
        Optional<StandardSnapshot> latest = Optional.empty();
        while (matcher.find()) {
            if (!"snapshot".equalsIgnoreCase(matcher.group(1))) {
                continue;
            }
            String snapshotRef = matcher.group();
            if (request.snapshotRef() != null && !request.snapshotRef().isBlank()) {
                if (!sameStableSnapshotRef(request.projectId(), snapshotRef, request.snapshotRef())) {
                    issues.add(claimIssue(
                            "SNAPSHOT_DRIFT",
                            AiOutputPostCheckIssueSeverity.FAIL,
                            StandardReferenceType.SNAPSHOT,
                            snapshotRef,
                            "AI 输出声明的标准快照与请求快照不一致。",
                            excerpt(request.content(), matcher.start(), matcher.end()),
                            List.of("使用请求快照重新生成，或确认输出确实应基于该旧快照。")));
                }
            } else {
                latest = latest.isPresent() ? latest : safeOptional(standardSnapshotRepository.findLatestByProjectId(request.projectId()));
                if (latest.isPresent() && !matchesSnapshot(latest.get(), request.projectId(), snapshotRef)) {
                    issues.add(claimIssue(
                            "SNAPSHOT_DRIFT",
                            AiOutputPostCheckIssueSeverity.FAIL,
                            StandardReferenceType.SNAPSHOT,
                            snapshotRef,
                            "AI 输出引用的标准快照不是当前项目最新快照。",
                            excerpt(request.content(), matcher.start(), matcher.end()),
                            List.of("基于最新标准快照重新生成，或显式传入允许的 snapshotRef。")));
                }
            }
        }
        return issues;
    }

    private void addBacktickFields(String content, Map<String, Occurrence> occurrences) {
        Matcher matcher = BACKTICK_PATTERN.matcher(content);
        while (matcher.find()) {
            addOccurrence(occurrences, StandardReferenceType.FIELD, matcher.group(1), matcher.start(1), matcher.end(1), content, true);
        }
    }

    private void addOccurrence(
            Map<String, Occurrence> occurrences,
            StandardReferenceType refType,
            String ref,
            int start,
            int end,
            String content,
            boolean highConfidence
    ) {
        if (ref == null || ref.isBlank()) {
            return;
        }
        String key = refType + "\n" + ref;
        occurrences.putIfAbsent(key, new Occurrence(refType, ref, excerpt(content, start, end), highConfidence));
    }

    private List<StandardReferenceResolutionResult> resolveOccurrences(Long projectId, List<Occurrence> occurrences) {
        List<StandardReferenceResolutionResult> results = new ArrayList<>();
        Map<StandardReferenceType, List<String>> refsByType = new LinkedHashMap<>();
        for (Occurrence occurrence : occurrences) {
            refsByType.computeIfAbsent(occurrence.refType(), ignored -> new ArrayList<>()).add(occurrence.ref());
        }
        for (Map.Entry<StandardReferenceType, List<String>> entry : refsByType.entrySet()) {
            results.addAll(referenceResolutionService.resolve(new StandardReferenceResolveRequest(
                    projectId,
                    entry.getKey(),
                    entry.getValue())).results());
        }
        return results;
    }

    private List<AiOutputPostCheckIssue> referenceIssues(
            List<StandardReferenceResolutionResult> resolvedRefs,
            List<Occurrence> occurrences
    ) {
        Map<String, Occurrence> occurrenceMap = new LinkedHashMap<>();
        for (Occurrence occurrence : occurrences) {
            occurrenceMap.put(occurrence.refType() + "\n" + occurrence.ref(), occurrence);
        }
        List<AiOutputPostCheckIssue> issues = new ArrayList<>();
        for (StandardReferenceResolutionResult result : resolvedRefs) {
            Occurrence occurrence = occurrenceMap.get(result.refType() + "\n" + result.inputRef());
            String excerpt = occurrence == null ? null : occurrence.excerpt();
            boolean highConfidence = occurrence == null || occurrence.highConfidence();
            switch (result.resolutionStatus()) {
                case CURRENT -> {
                }
                case STALE -> issues.add(issue(
                        "STALE_STANDARD_REFERENCE",
                        AiOutputPostCheckIssueSeverity.WARN,
                        result,
                        "引用已过期或已被替代。",
                        excerpt,
                        result.replacementRef(),
                        List.of("改用 replacementRef 或人工确认兼容性。")));
                case AMBIGUOUS -> issues.add(issue(
                        "AMBIGUOUS_STANDARD_REFERENCE",
                        AiOutputPostCheckIssueSeverity.WARN,
                        result,
                        "引用命中多个标准对象。",
                        excerpt,
                        null,
                        List.of("改用 stableRef 或更精确名称。")));
                case UNKNOWN -> issues.add(issue(
                        "UNKNOWN_STANDARD_REFERENCE",
                        highConfidence ? AiOutputPostCheckIssueSeverity.FAIL : AiOutputPostCheckIssueSeverity.WARN,
                        result,
                        "引用未在当前项目标准中解析。",
                        excerpt,
                        null,
                        List.of("补充标准、改用当前字段名或 stableRef。")));
                case CROSS_PROJECT -> issues.add(issue(
                        "CROSS_PROJECT_STANDARD_REFERENCE",
                        AiOutputPostCheckIssueSeverity.FAIL,
                        result,
                        "引用指向其他项目，不能在当前项目直接采纳。",
                        excerpt,
                        null,
                        List.of("改用当前项目 stableRef 或先建立复用映射。")));
            }
        }
        return issues;
    }

    private AiOutputPostCheckIssue claimIssue(
            String code,
            AiOutputPostCheckIssueSeverity severity,
            StandardReferenceType refType,
            String inputRef,
            String message,
            String excerpt,
            List<String> nextActions
    ) {
        return new AiOutputPostCheckIssue(
                code,
                severity,
                refType,
                sanitize(inputRef),
                sanitize(message),
                sanitize(excerpt),
                null,
                List.of(),
                sanitizeList(nextActions));
    }

    private List<EvidenceClaimCheck> resolveEvidenceClaims(Long projectId, String content) {
        Map<String, EvidenceClaimCheck> checks = new LinkedHashMap<>();
        Matcher matcher = EVIDENCE_PATTERN.matcher(content);
        while (matcher.find()) {
            String rawRef = stripEvidenceTrailingPunctuation(matcher.group());
            int refEnd = matcher.start() + rawRef.length();
            checks.computeIfAbsent(rawRef, ignored -> new EvidenceClaimCheck(
                    matcher.start(),
                    refEnd,
                    evidenceClaimResolver.resolve(projectId, rawRef)));
        }
        return List.copyOf(checks.values());
    }

    private String stripEvidenceTrailingPunctuation(String rawRef) {
        int end = rawRef.length();
        while (end > 0 && EVIDENCE_TRAILING_PUNCTUATION.indexOf(rawRef.charAt(end - 1)) >= 0) {
            end--;
        }
        return rawRef.substring(0, end);
    }

    private List<AiOutputPostCheckIssue> evidenceClaimIssues(
            String content,
            List<EvidenceClaimCheck> evidenceChecks
    ) {
        List<AiOutputPostCheckIssue> issues = new ArrayList<>();
        for (EvidenceClaimCheck check : evidenceChecks) {
            EvidenceClaimResolution resolution = check.resolution();
            if (resolution.status() == EvidenceClaimResolutionStatus.VERIFIED) {
                continue;
            }
            String code;
            AiOutputPostCheckIssueSeverity severity;
            String message;
            List<String> nextActions;
            if (resolution.status() == EvidenceClaimResolutionStatus.MISSING) {
                code = "MISSING_EVIDENCE_REFERENCE";
                severity = AiOutputPostCheckIssueSeverity.WARN;
                message = "AI 输出声明的 Evidence 来源不存在，不能作为已验证证据。";
                nextActions = List.of("重新生成 Evidence Package，并使用其 source.evidenceRef。", "确认来源记录未被删除。");
            } else if (resolution.status() == EvidenceClaimResolutionStatus.CROSS_PROJECT) {
                code = "CROSS_PROJECT_EVIDENCE_REFERENCE";
                severity = AiOutputPostCheckIssueSeverity.FAIL;
                message = "AI 输出声明的 Evidence 来源不属于当前项目，已拒绝采信。";
                nextActions = List.of("改用当前项目 Evidence Package 的 source.evidenceRef。", "不要复制其他项目的 evidence ref。");
            } else {
                code = "UNVERIFIABLE_EVIDENCE_REFERENCE";
                severity = AiOutputPostCheckIssueSeverity.WARN;
                message = "AI 输出声明的 Evidence ref 格式或来源不受支持，无法确定性验证。";
                nextActions = List.of("使用持久化 Evidence Package 返回的 canonical source.evidenceRef。", "人工复核后再采纳该声明。");
            }
            issues.add(new AiOutputPostCheckIssue(
                    code,
                    severity,
                    null,
                    resolution.inputRef(),
                    message,
                    excerpt(content, check.start(), check.end()),
                    null,
                    List.of(),
                    nextActions));
        }
        return List.copyOf(issues);
    }

    private AiOutputPostCheckIssue issue(
            String code,
            AiOutputPostCheckIssueSeverity severity,
            StandardReferenceResolutionResult result,
            String message,
            String excerpt,
            String replacementRef,
            List<String> nextActions
    ) {
        return new AiOutputPostCheckIssue(
                code,
                severity,
                result.refType(),
                sanitize(result.inputRef()),
                sanitize(message),
                sanitize(excerpt),
                sanitize(replacementRef),
                sanitizeList(result.evidenceLinks()),
                sanitizeList(nextActions));
    }

    private AiOutputPostCheckSummary summary(List<StandardReferenceResolutionResult> resolvedRefs, List<AiOutputPostCheckIssue> issues) {
        int current = 0;
        int stale = 0;
        int unknown = 0;
        int ambiguous = 0;
        int crossProject = 0;
        for (StandardReferenceResolutionResult result : resolvedRefs) {
            StandardReferenceResolutionStatus status = result.resolutionStatus();
            if (status == StandardReferenceResolutionStatus.CURRENT) {
                current++;
            } else if (status == StandardReferenceResolutionStatus.STALE) {
                stale++;
            } else if (status == StandardReferenceResolutionStatus.UNKNOWN) {
                unknown++;
            } else if (status == StandardReferenceResolutionStatus.AMBIGUOUS) {
                ambiguous++;
            } else if (status == StandardReferenceResolutionStatus.CROSS_PROJECT) {
                crossProject++;
            }
        }
        return new AiOutputPostCheckSummary(
                resolvedRefs.size(),
                current,
                stale,
                unknown,
                ambiguous,
                crossProject,
                issues.size());
    }

    private AiOutputPostCheckStatus status(List<AiOutputPostCheckIssue> issues) {
        if (issues.stream().anyMatch(issue -> issue.severity() == AiOutputPostCheckIssueSeverity.FAIL)) {
            return AiOutputPostCheckStatus.FAIL;
        }
        return issues.isEmpty() ? AiOutputPostCheckStatus.PASS : AiOutputPostCheckStatus.WARN;
    }

    private List<String> suggestedFixes(List<AiOutputPostCheckIssue> issues) {
        List<String> fixes = new ArrayList<>();
        for (AiOutputPostCheckIssue issue : issues) {
            if (issue.replacementRef() != null) {
                fixes.add("将 `" + issue.inputRef() + "` 替换为 `" + issue.replacementRef() + "`。");
            } else if ("UNKNOWN_STANDARD_REFERENCE".equals(issue.code())) {
                fixes.add("修复未知引用 `" + issue.inputRef() + "`，或先补充标准对象。");
            }
        }
        return sanitizeList(fixes);
    }

    private List<String> evidenceLinks(
            List<StandardReferenceResolutionResult> resolvedRefs,
            List<EvidenceClaimCheck> evidenceChecks,
            List<ReviewFinding> findings
    ) {
        Set<String> links = new LinkedHashSet<>();
        for (StandardReferenceResolutionResult result : resolvedRefs) {
            links.addAll(result.evidenceLinks());
        }
        for (EvidenceClaimCheck check : evidenceChecks) {
            EvidenceClaimResolution resolution = check.resolution();
            if (resolution.status() == EvidenceClaimResolutionStatus.VERIFIED
                    && resolution.canonicalRef() != null) {
                links.add(resolution.canonicalRef());
            }
        }
        for (ReviewFinding finding : findings) {
            links.addAll(finding.evidenceRefs());
        }
        return sanitizeList(new ArrayList<>(links));
    }

    private List<String> nextActions(AiOutputPostCheckStatus status) {
        return switch (status) {
            case PASS -> List.of("可以复制或下载该 AI 产物。");
            case WARN -> List.of("人工确认过期、歧义或证据缺口后再使用 AI 产物。");
            case FAIL -> List.of("修复未知引用、跨项目引用或无效规则后再复制、下载或执行 AI 产物。");
        };
    }

    private boolean sameStableSnapshotRef(Long projectId, String leftRef, String rightRef) {
        Optional<StandardReferenceFormatter.ParsedStableReference> left = StandardReferenceFormatter.parse(leftRef);
        Optional<StandardReferenceFormatter.ParsedStableReference> right = StandardReferenceFormatter.parse(rightRef);
        if (left.isEmpty()
                || right.isEmpty()
                || left.get().type() != StandardReferenceType.SNAPSHOT
                || right.get().type() != StandardReferenceType.SNAPSHOT
                || !Objects.equals(projectId, left.get().projectId())
                || !Objects.equals(projectId, right.get().projectId())) {
            return false;
        }
        Optional<SnapshotIdentity> leftIdentity = snapshotIdentity(projectId, left.get().objectKey());
        Optional<SnapshotIdentity> rightIdentity = snapshotIdentity(projectId, right.get().objectKey());
        if (leftIdentity.isPresent() && rightIdentity.isPresent()) {
            return Objects.equals(leftIdentity.get().id(), rightIdentity.get().id());
        }
        return left.get().objectKey().equalsIgnoreCase(right.get().objectKey());
    }

    private boolean matchesSnapshot(StandardSnapshot snapshot, Long projectId, String snapshotRef) {
        Optional<StandardReferenceFormatter.ParsedStableReference> parsed = StandardReferenceFormatter.parse(snapshotRef);
        if (parsed.isEmpty()
                || parsed.get().type() != StandardReferenceType.SNAPSHOT
                || !Objects.equals(projectId, parsed.get().projectId())) {
            return true;
        }
        String objectKey = parsed.get().objectKey();
        Optional<SnapshotIdentity> identity = snapshotIdentity(projectId, objectKey);
        if (identity.isPresent()) {
            return Objects.equals(snapshot.getId(), identity.get().id());
        }
        return Objects.equals(String.valueOf(snapshot.getId()), objectKey)
                || objectKey.equalsIgnoreCase(Objects.toString(snapshot.getVersion(), ""));
    }

    private Optional<SnapshotIdentity> snapshotIdentity(Long projectId, String objectKey) {
        Optional<StandardSnapshot> snapshot = parseLong(objectKey)
                .flatMap(id -> safeOptional(standardSnapshotRepository.findByProjectIdAndId(projectId, id)));
        if (snapshot.isEmpty()) {
            snapshot = safeOptional(standardSnapshotRepository.findByProjectIdAndVersion(projectId, objectKey));
        }
        return snapshot.map(item -> new SnapshotIdentity(item.getId(), item.getVersion()));
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private <T> Optional<T> safeOptional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }

    private String excerpt(String content, int start, int end) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int safeStart = Math.max(0, start < 0 ? 0 : start - EXCERPT_RADIUS);
        int safeEnd = Math.min(content.length(), end < 0 ? Math.min(content.length(), EXCERPT_RADIUS) : end + EXCERPT_RADIUS);
        return sanitize(content.substring(safeStart, safeEnd));
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            String text = sanitize(value);
            if (text != null && !text.isBlank()) {
                sanitized.add(text);
            }
        }
        return List.copyOf(sanitized);
    }

    private String sanitize(String value) {
        return SensitiveDataSanitizer.redactText(value, 240);
    }

    private record Occurrence(StandardReferenceType refType, String ref, String excerpt, boolean highConfidence) {
    }

    private record EvidenceClaimCheck(int start, int end, EvidenceClaimResolution resolution) {
    }

    private record ExternalFindingCheck(
            List<ReviewFinding> findings,
            List<AiOutputPostCheckIssue> issues
    ) {
    }

    private record SnapshotIdentity(Long id, String version) {
    }
}
