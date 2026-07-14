package com.dataspec.aioutputcheck;

import com.dataspec.aioutputcheck.model.AiOutputContentType;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckStatus;
import com.dataspec.aioutputcheck.service.impl.AiOutputPostCheckServiceImpl;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.evidence.model.EvidenceSourceType;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolution;
import com.dataspec.evidenceclaim.model.EvidenceClaimResolutionStatus;
import com.dataspec.evidenceclaim.service.EvidenceClaimResolver;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.reviewfinding.model.ReviewFinding;
import com.dataspec.reviewfinding.model.ReviewFindingSeverity;
import com.dataspec.reviewfinding.model.ReviewFindingSource;
import com.dataspec.reviewfinding.model.ReviewFindingSubject;
import com.dataspec.reviewfinding.model.ReviewFindingWaiver;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import com.dataspec.standardref.model.StandardReferenceConfidence;
import com.dataspec.standardref.model.StandardReferenceResolveRequest;
import com.dataspec.standardref.model.StandardReferenceResolveResponse;
import com.dataspec.standardref.model.StandardReferenceResolutionResult;
import com.dataspec.standardref.model.StandardReferenceResolutionStatus;
import com.dataspec.standardref.model.StandardReferenceType;
import com.dataspec.standardref.service.StandardReferenceResolutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AiOutputPostCheckServiceImplTest {

    @Test
    void ddlCheckFailsForUnknownFieldWarnsForStaleFieldAndRedactsExcerpts() {
        StandardReferenceResolutionService resolver = mockResolver();
        AiOutputPostCheckServiceImpl service = service(resolver);
        String ddl = """
                CREATE TABLE users (
                  mobile_no varchar(20),
                  legacy_phone varchar(20),
                  missing_field varchar(20),
                  debug_token varchar(200) DEFAULT 'token=secret123'
                );
                """;

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.DDL,
                ddl,
                null
        ));

        assertEquals("dataspec-ai-output-postcheck", result.kind());
        assertEquals(AiOutputPostCheckStatus.FAIL, result.status());
        assertFalse(result.safeToUse());
        assertEquals(1, result.summary().unknownCount());
        assertEquals(1, result.summary().staleCount());
        assertTrue(result.issues().stream().anyMatch(issue -> "UNKNOWN_STANDARD_REFERENCE".equals(issue.code())));
        assertTrue(result.issues().stream().anyMatch(issue -> "STALE_STANDARD_REFERENCE".equals(issue.code())
                && "field:1:12".equals(issue.replacementRef())));
        assertTrue(result.nextActions().stream().anyMatch(action -> action.contains("修复未知引用")));
        assertFalse(result.toString().contains("secret123"));
        assertFalse(result.toString().contains("token=secret123"));

        ArgumentCaptor<StandardReferenceResolveRequest> captor = ArgumentCaptor.forClass(StandardReferenceResolveRequest.class);
        verify(resolver).resolve(captor.capture());
        assertEquals(StandardReferenceType.FIELD, captor.getValue().refType());
        assertTrue(captor.getValue().refs().containsAll(List.of("mobile_no", "legacy_phone", "missing_field", "debug_token")));
    }

    @Test
    void sqlMarkdownJsonAndPlainTextResolveExplicitRefsWithStableStatusSemantics() {
        AiOutputPostCheckServiceImpl service = service(mockResolver());

        AiOutputPostCheckResult sql = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.SQL,
                "select mobile_no from users where missing_field is not null",
                null));
        assertEquals(AiOutputPostCheckStatus.FAIL, sql.status());
        assertTrue(sql.issues().stream().anyMatch(issue -> issue.inputRef().equals("missing_field")));

        AiOutputPostCheckResult markdown = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.MARKDOWN,
                "请使用 `legacy_phone`，遵守 rule:1:unknown_rule，并基于 dataspec://evidence/missing。",
                null));
        assertEquals(AiOutputPostCheckStatus.FAIL, markdown.status());
        assertTrue(markdown.issues().stream().anyMatch(issue -> "UNKNOWN_STANDARD_REFERENCE".equals(issue.code())
                && issue.inputRef().contains("unknown_rule")));
        assertTrue(markdown.issues().stream().anyMatch(issue -> "UNVERIFIABLE_EVIDENCE_REFERENCE".equals(issue.code())));

        AiOutputPostCheckResult json = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.JSON,
                "{\"fieldRef\":\"field:1:10\",\"enumRef\":\"enum:1:20\",\"snapshotRef\":\"snapshot:1:v2026.07\",\"password\":\"secret123\"}",
                "snapshot:1:v2026.07"));
        assertEquals(AiOutputPostCheckStatus.PASS, json.status());
        assertTrue(json.safeToUse());
        assertFalse(json.toString().contains("secret123"));

        AiOutputPostCheckResult plain = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.TEXT,
                "生成结果引用 field:1:10 和 enum:1:20。",
                null));
        assertEquals(AiOutputPostCheckStatus.PASS, plain.status());
    }

    @Test
    void sqlExtractionDoesNotTreatTableAliasesFunctionsOrStringLiteralsAsFields() {
        StandardReferenceResolutionService resolver = mockResolver();
        AiOutputPostCheckServiceImpl service = service(resolver);
        String sql = """
                select count(*) as total_count, u.mobile_no phone, u.email_address email
                from users u
                where u.status_code = 'missing_field'
                  and u.deleted_at is null
                """;

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.SQL,
                sql,
                null));

        assertEquals(AiOutputPostCheckStatus.PASS, result.status());
        ArgumentCaptor<StandardReferenceResolveRequest> captor = ArgumentCaptor.forClass(StandardReferenceResolveRequest.class);
        verify(resolver).resolve(captor.capture());
        assertTrue(captor.getValue().refs().containsAll(List.of("mobile_no", "email_address", "status_code", "deleted_at")));
        assertFalse(captor.getValue().refs().contains("users"));
        assertFalse(captor.getValue().refs().contains("u"));
        assertFalse(captor.getValue().refs().contains("count"));
        assertFalse(captor.getValue().refs().contains("phone"));
        assertFalse(captor.getValue().refs().contains("email"));
        assertFalse(captor.getValue().refs().contains("missing_field"));
    }

    @Test
    void explicitEnumRuleSnapshotAndEvidenceClaimsAreValidatedDeterministically() {
        EnumDictRepository enumRepository = defaultEnumRepository();
        RuleConfigRepository ruleRepository = defaultRuleRepository();
        StandardSnapshotRepository snapshotRepository = defaultSnapshotRepository();
        when(enumRepository.findDictById(20L)).thenReturn(Optional.of(enumDict(20L, 1L, "order_status")));
        when(enumRepository.findValuesByEnumId(20L)).thenReturn(List.of(enumValue("pending"), enumValue("paid")));
        when(ruleRepository.findByCodeAndProjectId("unknown_rule", 1L)).thenReturn(Optional.empty());
        when(snapshotRepository.findLatestByProjectId(1L)).thenReturn(Optional.of(snapshot(8L, 1L, "v2026.08")));
        AiOutputPostCheckServiceImpl service = service(mockResolver(), enumRepository, ruleRepository, snapshotRepository);
        String output = """
                {
                  "enumRef": "enum:1:20",
                  "enumValue": "archived",
                  "ruleCode": "unknown_rule",
                  "snapshotRef": "snapshot:1:7",
                  "evidenceRef": "dataspec://evidence/missing-package"
                }
                """;

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.JSON,
                output,
                "snapshot:1:8"));

        assertEquals(AiOutputPostCheckStatus.FAIL, result.status());
        assertTrue(result.issues().stream().anyMatch(issue -> "INVALID_ENUM_VALUE".equals(issue.code())));
        assertTrue(result.issues().stream().anyMatch(issue -> "UNKNOWN_RULE_CODE".equals(issue.code())));
        assertTrue(result.issues().stream().anyMatch(issue -> "SNAPSHOT_DRIFT".equals(issue.code())));
        assertTrue(result.issues().stream().anyMatch(issue -> "UNVERIFIABLE_EVIDENCE_REFERENCE".equals(issue.code())));
    }

    @Test
    void evidenceClaimsUseProjectScopedResolverStatusesAndOnlyExposeVerifiedLinks() {
        EvidenceClaimResolver evidenceResolver = mock(EvidenceClaimResolver.class);
        when(evidenceResolver.resolve(anyLong(), anyString())).thenAnswer(invocation -> {
            String ref = invocation.getArgument(1);
            if (ref.endsWith("/10")) {
                return evidenceResolution(ref, EvidenceClaimResolutionStatus.VERIFIED, ref);
            }
            if (ref.endsWith("/20")) {
                return evidenceResolution(ref, EvidenceClaimResolutionStatus.MISSING, null);
            }
            if (ref.endsWith("/30")) {
                return evidenceResolution(ref, EvidenceClaimResolutionStatus.CROSS_PROJECT, null);
            }
            return evidenceResolution(ref, EvidenceClaimResolutionStatus.UNVERIFIABLE, null);
        });
        AiOutputPostCheckServiceImpl service = service(mockResolver(), evidenceResolver);
        String output = """
                {
                  "verified": "dataspec://evidence/sql-check/10",
                  "verifiedAgain": "dataspec://evidence/sql-check/10",
                  "missing": "dataspec://evidence/sql-check/20",
                  "crossProject": "dataspec://evidence/sql-check/30",
                  "unsupported": "dataspec://evidence/legacy-package"
                }
                """;

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.JSON,
                output,
                null));

        assertEquals(AiOutputPostCheckStatus.FAIL, result.status());
        assertTrue(result.issues().stream().anyMatch(issue -> "MISSING_EVIDENCE_REFERENCE".equals(issue.code())));
        assertTrue(result.issues().stream().anyMatch(issue ->
                "CROSS_PROJECT_EVIDENCE_REFERENCE".equals(issue.code())
                        && issue.severity().name().equals("FAIL")));
        assertTrue(result.issues().stream().anyMatch(issue -> "UNVERIFIABLE_EVIDENCE_REFERENCE".equals(issue.code())));
        assertEquals(List.of("dataspec://evidence/sql-check/10"), result.evidenceLinks().stream()
                .filter(link -> link.startsWith("dataspec://evidence/"))
                .toList());
    }

    @Test
    void evidenceClaimsIgnoreNaturalLanguageTrailingPunctuation() {
        String canonicalRef = "dataspec://evidence/sql-check/10";
        EvidenceClaimResolver evidenceResolver = mock(EvidenceClaimResolver.class);
        when(evidenceResolver.resolve(anyLong(), anyString())).thenAnswer(invocation -> {
            String ref = invocation.getArgument(1);
            EvidenceClaimResolutionStatus status = canonicalRef.equals(ref)
                    ? EvidenceClaimResolutionStatus.VERIFIED
                    : EvidenceClaimResolutionStatus.UNVERIFIABLE;
            return evidenceResolution(ref, status, status == EvidenceClaimResolutionStatus.VERIFIED ? ref : null);
        });
        AiOutputPostCheckServiceImpl service = service(mockResolver(), evidenceResolver);

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.TEXT,
                "依据 " + canonicalRef + "。\n另见 " + canonicalRef + ",\n句点 " + canonicalRef
                        + ".\n列表 [" + canonicalRef + "]",
                null));

        assertEquals(AiOutputPostCheckStatus.PASS, result.status());
        assertEquals(List.of(canonicalRef), result.evidenceLinks().stream()
                .filter(link -> link.startsWith("dataspec://evidence/"))
                .toList());
        verify(evidenceResolver).resolve(1L, canonicalRef);
        verifyNoMoreInteractions(evidenceResolver);
    }

    @Test
    void jsonEnumClaimsAreValidatedWhenFieldsAreOutOfOrder() {
        EnumDictRepository enumRepository = defaultEnumRepository();
        when(enumRepository.findDictById(20L)).thenReturn(Optional.of(enumDict(20L, 1L, "order_status")));
        when(enumRepository.findValuesByEnumId(20L)).thenReturn(List.of(enumValue("pending"), enumValue("paid")));
        AiOutputPostCheckServiceImpl service = service(
                mockResolver(),
                enumRepository,
                defaultRuleRepository(),
                defaultSnapshotRepository());

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.JSON,
                """
                        {
                          "enumValue": "archived",
                          "reason": "AI reordered fields",
                          "enumRef": "enum:1:20"
                        }
                        """,
                null));

        assertEquals(AiOutputPostCheckStatus.FAIL, result.status());
        assertTrue(result.issues().stream().anyMatch(issue -> "INVALID_ENUM_VALUE".equals(issue.code())
                && "enum:1:20=archived".equals(issue.inputRef())));
    }

    @Test
    void validExplicitClaimsPassAndSnapshotIdVersionRefsAreEquivalent() {
        EnumDictRepository enumRepository = defaultEnumRepository();
        RuleConfigRepository ruleRepository = defaultRuleRepository();
        StandardSnapshotRepository snapshotRepository = defaultSnapshotRepository();
        StandardSnapshot snapshot = snapshot(8L, 1L, "v2026.08");
        when(enumRepository.findDictById(20L)).thenReturn(Optional.of(enumDict(20L, 1L, "order_status")));
        when(enumRepository.findValuesByEnumId(20L)).thenReturn(List.of(enumValue("pending"), enumValue("paid")));
        when(ruleRepository.findByCodeAndProjectId("field_naming_snake_case", 1L))
                .thenReturn(Optional.of(rule("field_naming_snake_case")));
        when(snapshotRepository.findByProjectIdAndId(1L, 8L)).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findByProjectIdAndVersion(1L, "v2026.08")).thenReturn(Optional.of(snapshot));
        AiOutputPostCheckServiceImpl service = service(mockResolver(), enumRepository, ruleRepository, snapshotRepository);

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.JSON,
                """
                        {
                          "enumRef": "enum:1:20",
                          "enumValue": "paid",
                          "ruleCode": "field_naming_snake_case",
                          "snapshotRef": "snapshot:1:v2026.08"
                        }
                        """,
                "snapshot:1:8"));

        assertEquals(AiOutputPostCheckStatus.PASS, result.status());
        assertTrue(result.safeToUse());
    }

    @Test
    void verifiedHighImpactExternalFindingCanPassButCannotRemainAutoFixSafe() {
        String evidenceRef = "dataspec://evidence/sql-check/10";
        EvidenceClaimResolver evidenceResolver = mock(EvidenceClaimResolver.class);
        when(evidenceResolver.resolve(1L, evidenceRef)).thenReturn(
                evidenceResolution(evidenceRef, EvidenceClaimResolutionStatus.VERIFIED, evidenceRef));
        AiOutputPostCheckServiceImpl service = service(mockResolver(), evidenceResolver);
        ReviewFinding submitted = externalFinding(
                ReviewFindingSeverity.ERROR,
                95,
                true,
                List.of(evidenceRef));

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.TEXT,
                "评审已完成。",
                null,
                List.of(submitted)));

        assertEquals(AiOutputPostCheckStatus.PASS, result.status());
        assertEquals(1, result.findings().size());
        ReviewFinding normalized = result.findings().getFirst();
        assertEquals(ReviewFindingSource.EXTERNAL_AI, normalized.source());
        assertEquals(1L, normalized.subject().projectId());
        assertFalse("caller-controlled-key".equals(normalized.findingKey()));
        assertEquals(List.of(evidenceRef), normalized.evidenceRefs());
        assertFalse(normalized.autoFixSafe());
        assertNotNull(result.verificationReceipt());
    }

    @Test
    void invalidHighImpactExternalEvidenceBlocksAndCannotRemainAutoFixSafe() {
        String evidenceRef = "dataspec://evidence/sql-check/30";
        EvidenceClaimResolver evidenceResolver = mock(EvidenceClaimResolver.class);
        when(evidenceResolver.resolve(1L, evidenceRef)).thenReturn(
                evidenceResolution(evidenceRef, EvidenceClaimResolutionStatus.CROSS_PROJECT, null));
        AiOutputPostCheckServiceImpl service = service(mockResolver(), evidenceResolver);

        AiOutputPostCheckResult result = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.TEXT,
                "评审已完成。",
                null,
                List.of(externalFinding(ReviewFindingSeverity.WARNING, 90, true, List.of(evidenceRef)))));

        assertEquals(AiOutputPostCheckStatus.FAIL, result.status());
        assertNull(result.verificationReceipt());
        assertTrue(result.issues().stream().anyMatch(issue ->
                "CROSS_PROJECT_FINDING_EVIDENCE_REFERENCE".equals(issue.code())
                        && issue.severity().name().equals("FAIL")));
        ReviewFinding normalized = result.findings().stream()
                .filter(finding -> "AI_REVIEW_RULE".equals(finding.code()))
                .findFirst()
                .orElseThrow();
        assertTrue(normalized.evidenceRefs().isEmpty());
        assertFalse(normalized.autoFixSafe());
        assertFalse(result.toString().contains("raw-secret-123"));
    }

    @Test
    void lowImpactFindingWithoutEvidenceWarnsAndEmptyFindingsRemainValid() {
        AiOutputPostCheckServiceImpl service = service(mockResolver());

        AiOutputPostCheckResult warned = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.TEXT,
                "普通评审摘要。",
                null,
                List.of(externalFinding(ReviewFindingSeverity.INFO, 30, false, List.of()))));
        AiOutputPostCheckResult empty = service.check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.TEXT,
                "普通评审摘要。",
                null,
                List.of()));

        assertEquals(AiOutputPostCheckStatus.WARN, warned.status());
        assertTrue(warned.issues().stream().anyMatch(issue ->
                "MISSING_FINDING_EVIDENCE_REFERENCE".equals(issue.code())
                        && issue.severity().name().equals("WARN")));
        assertEquals(AiOutputPostCheckStatus.PASS, empty.status());
        assertTrue(empty.findings().isEmpty());
        assertNotNull(empty.verificationReceipt());
    }

    @Test
    void waivedCallerAutoFixClaimStillTriggersHighImpactEvidenceGate() {
        ReviewFinding submitted = new ReviewFinding(
                ReviewFindingSource.EXTERNAL_AI,
                null,
                "AI_REVIEW_RULE",
                ReviewFindingSeverity.INFO,
                new ReviewFindingSubject(1L, "AI_OUTPUT", "review", null, null, null),
                null,
                null,
                null,
                "缺少确定性 evidence",
                List.of(),
                20,
                null,
                true,
                new ReviewFindingWaiver(true, 9L, "调用方自报豁免"));
        assertTrue(submitted.autoFixSafe(), "通用模型不能在 evidence gate 前消除调用方声明");

        AiOutputPostCheckResult result = service(mockResolver()).check(new AiOutputPostCheckRequest(
                1L,
                AiOutputContentType.TEXT,
                "评审完成。",
                null,
                List.of(submitted)));

        assertEquals(AiOutputPostCheckStatus.FAIL, result.status());
        assertTrue(result.issues().stream().anyMatch(issue ->
                "MISSING_FINDING_EVIDENCE_REFERENCE".equals(issue.code())
                        && issue.severity().name().equals("FAIL")));
        assertNull(result.verificationReceipt());
    }

    private ReviewFinding externalFinding(
            ReviewFindingSeverity severity,
            Integer confidence,
            boolean autoFixSafe,
            List<String> evidenceRefs
    ) {
        return new ReviewFinding(
                ReviewFindingSource.SQL_LINT,
                "caller-controlled-key",
                "AI_REVIEW_RULE",
                severity,
                new ReviewFindingSubject(999L, "AI_OUTPUT", "order review", null, null, null),
                null,
                "authorization=Bearer raw-secret-123",
                "符合字段标准",
                "password=raw-secret-123",
                evidenceRefs,
                confidence,
                "人工修复",
                autoFixSafe,
                ReviewFindingWaiver.NONE);
    }

    private AiOutputPostCheckServiceImpl service(StandardReferenceResolutionService resolver) {
        EvidenceClaimResolver evidenceResolver = mock(EvidenceClaimResolver.class);
        when(evidenceResolver.resolve(anyLong(), anyString())).thenAnswer(invocation ->
                evidenceResolution(
                        invocation.getArgument(1),
                        EvidenceClaimResolutionStatus.UNVERIFIABLE,
                        null));
        return service(resolver, evidenceResolver);
    }

    private AiOutputPostCheckServiceImpl service(
            StandardReferenceResolutionService resolver,
            EvidenceClaimResolver evidenceResolver
    ) {
        return service(
                resolver,
                defaultEnumRepository(),
                defaultRuleRepository(),
                defaultSnapshotRepository(),
                evidenceResolver);
    }

    private AiOutputPostCheckServiceImpl service(
            StandardReferenceResolutionService resolver,
            EnumDictRepository enumRepository,
            RuleConfigRepository ruleRepository,
            StandardSnapshotRepository snapshotRepository
    ) {
        EvidenceClaimResolver evidenceResolver = mock(EvidenceClaimResolver.class);
        when(evidenceResolver.resolve(anyLong(), anyString())).thenAnswer(invocation ->
                evidenceResolution(
                        invocation.getArgument(1),
                        EvidenceClaimResolutionStatus.UNVERIFIABLE,
                        null));
        return service(resolver, enumRepository, ruleRepository, snapshotRepository, evidenceResolver);
    }

    private AiOutputPostCheckServiceImpl service(
            StandardReferenceResolutionService resolver,
            EnumDictRepository enumRepository,
            RuleConfigRepository ruleRepository,
            StandardSnapshotRepository snapshotRepository,
            EvidenceClaimResolver evidenceResolver
    ) {
        return new AiOutputPostCheckServiceImpl(
                resolver,
                evidenceResolver,
                new SqlParserService(),
                enumRepository,
                ruleRepository,
                snapshotRepository);
    }

    private EvidenceClaimResolution evidenceResolution(
            String inputRef,
            EvidenceClaimResolutionStatus status,
            String canonicalRef
    ) {
        return new EvidenceClaimResolution(
                inputRef,
                canonicalRef,
                status,
                EvidenceSourceType.SQL_CHECK,
                10L,
                status == EvidenceClaimResolutionStatus.VERIFIED ? 1L : null);
    }

    private EnumDictRepository defaultEnumRepository() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        when(repository.findDictById(any())).thenReturn(Optional.empty());
        return repository;
    }

    private RuleConfigRepository defaultRuleRepository() {
        RuleConfigRepository repository = mock(RuleConfigRepository.class);
        when(repository.findByCodeAndProjectId(any(), any())).thenReturn(Optional.empty());
        return repository;
    }

    private StandardSnapshotRepository defaultSnapshotRepository() {
        StandardSnapshotRepository repository = mock(StandardSnapshotRepository.class);
        when(repository.findLatestByProjectId(any())).thenReturn(Optional.empty());
        return repository;
    }

    private StandardReferenceResolutionService mockResolver() {
        StandardReferenceResolutionService resolver = mock(StandardReferenceResolutionService.class);
        when(resolver.resolve(any(StandardReferenceResolveRequest.class))).thenAnswer(invocation -> {
            StandardReferenceResolveRequest request = invocation.getArgument(0);
            List<StandardReferenceResolutionResult> results = new ArrayList<>();
            for (String ref : request.refs()) {
                results.add(resultFor(request.refType(), ref));
            }
            return new StandardReferenceResolveResponse(
                    StandardReferenceResolveResponse.KIND,
                    1,
                    request.projectId(),
                    results,
                    List.of());
        });
        return resolver;
    }

    private StandardReferenceResolutionResult resultFor(StandardReferenceType type, String ref) {
        if (ref.contains("missing") || ref.contains("unknown")) {
            return new StandardReferenceResolutionResult(
                    ref,
                    type,
                    StandardReferenceResolutionStatus.UNKNOWN,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    StandardReferenceConfidence.LOW,
                    List.of(),
                    List.of("未找到引用"));
        }
        if ("legacy_phone".equals(ref)) {
            return new StandardReferenceResolutionResult(
                    ref,
                    type,
                    StandardReferenceResolutionStatus.STALE,
                    "field:1:11",
                    "field:1:12",
                    11L,
                    "legacy_phone",
                    "legacy_phone",
                    "deprecated",
                    "field:1:12",
                    StandardReferenceConfidence.HIGH,
                    List.of("dataspec://fields/11"),
                    List.of("字段已废弃"));
        }
        String stableRef = switch (type) {
            case FIELD -> ref.startsWith("field:") ? ref : "field:1:10";
            case ENUM -> ref.startsWith("enum:") ? ref : "enum:1:20";
            case RULE -> ref.startsWith("rule:") ? ref : "rule:1:" + ref;
            case SNAPSHOT -> ref.startsWith("snapshot:") ? ref : "snapshot:1:7";
        };
        return new StandardReferenceResolutionResult(
                ref,
                type,
                StandardReferenceResolutionStatus.CURRENT,
                stableRef,
                stableRef,
                10L,
                ref,
                null,
                "enabled",
                null,
                StandardReferenceConfidence.HIGH,
                List.of("dataspec://refs/" + ref),
                List.of());
    }

    private EnumDict enumDict(Long id, Long projectId, String code) {
        EnumDict dict = new EnumDict();
        dict.setId(id);
        dict.setProjectId(projectId);
        dict.setCode(code);
        dict.setName(code);
        return dict;
    }

    private EnumValue enumValue(String value) {
        EnumValue enumValue = new EnumValue();
        enumValue.setValue(value);
        enumValue.setLabel(value);
        return enumValue;
    }

    private StandardSnapshot snapshot(Long id, Long projectId, String version) {
        StandardSnapshot snapshot = new StandardSnapshot();
        snapshot.setId(id);
        snapshot.setProjectId(projectId);
        snapshot.setVersion(version);
        snapshot.setName(version);
        return snapshot;
    }

    private RuleConfig rule(String ruleCode) {
        RuleConfig rule = new RuleConfig();
        rule.setProjectId(1L);
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleCode);
        rule.setEnabled(true);
        return rule;
    }
}
