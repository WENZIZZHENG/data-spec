package com.dataspec.aioutputcheck;

import com.dataspec.aioutputcheck.model.AiOutputContentType;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckStatus;
import com.dataspec.aioutputcheck.service.impl.AiOutputPostCheckServiceImpl;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        assertTrue(markdown.issues().stream().anyMatch(issue -> "EVIDENCE_GAP".equals(issue.code())));

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
        assertTrue(result.issues().stream().anyMatch(issue -> "EVIDENCE_GAP".equals(issue.code())));
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

    private AiOutputPostCheckServiceImpl service(StandardReferenceResolutionService resolver) {
        return service(resolver, defaultEnumRepository(), defaultRuleRepository(), defaultSnapshotRepository());
    }

    private AiOutputPostCheckServiceImpl service(
            StandardReferenceResolutionService resolver,
            EnumDictRepository enumRepository,
            RuleConfigRepository ruleRepository,
            StandardSnapshotRepository snapshotRepository
    ) {
        return new AiOutputPostCheckServiceImpl(
                resolver,
                new SqlParserService(),
                enumRepository,
                ruleRepository,
                snapshotRepository);
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
