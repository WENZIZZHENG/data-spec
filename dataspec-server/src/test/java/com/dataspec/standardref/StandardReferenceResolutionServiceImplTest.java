package com.dataspec.standardref;

import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.fieldhistory.model.FieldHistoricalAlias;
import com.dataspec.fieldhistory.service.FieldHistoricalAliasService;
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
import com.dataspec.standardref.service.impl.StandardReferenceResolutionServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class StandardReferenceResolutionServiceImplTest {

    @Test
    void resolvesFieldStableRefAliasAndDeprecatedReplacement() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        Field current = field(10L, 1L, "mobile_no", "phone,mobile_phone", "enabled", null);
        Field legacy = field(11L, 1L, "legacy_mobile", "old_phone", "deprecated", 10L);
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(current, legacy));
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(current));
        var service = service(fieldRepository);

        StandardReferenceResolveResponse response = service.resolve(new StandardReferenceResolveRequest(
                1L,
                StandardReferenceType.FIELD,
                List.of("field:1:10", "mobile_phone", "legacy_mobile")
        ));

        assertEquals("dataspec-standard-reference-resolution", response.kind());
        assertEquals(1, response.schemaVersion());
        assertEquals(1L, response.projectId());

        StandardReferenceResolutionResult direct = response.results().get(0);
        assertEquals(StandardReferenceResolutionStatus.CURRENT, direct.resolutionStatus());
        assertEquals("field:1:10", direct.stableRef());
        assertEquals("field:1:10", direct.canonicalRef());
        assertEquals(10L, direct.objectId());
        assertEquals("mobile_no", direct.currentName());

        StandardReferenceResolutionResult alias = response.results().get(1);
        assertEquals(StandardReferenceResolutionStatus.CURRENT, alias.resolutionStatus());
        assertEquals("mobile_phone", alias.matchedAlias());
        assertEquals("field:1:10", alias.stableRef());
        assertEquals(StandardReferenceConfidence.HIGH, alias.confidence());

        StandardReferenceResolutionResult stale = response.results().get(2);
        assertEquals(StandardReferenceResolutionStatus.STALE, stale.resolutionStatus());
        assertEquals("field:1:11", stale.stableRef());
        assertEquals("field:1:10", stale.canonicalRef());
        assertEquals("field:1:10", stale.replacementRef());
        assertEquals("deprecated", stale.lifecycleStatus());
        assertTrue(stale.warnings().getFirst().contains("替代"));
    }

    @Test
    void resolvesAuditableHistoricalNamesAndKeepsAmbiguousHistoryUnresolved() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldHistoricalAliasService historyService = mock(FieldHistoricalAliasService.class);
        Field mobile = field(10L, 1L, "mobile_no", "mobile_phone", "enabled", null);
        Field backup = field(12L, 1L, "backup_mobile", null, "enabled", null);
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(mobile, backup));
        when(historyService.load(1L, List.of(mobile, backup))).thenReturn(Map.of(
                10L, List.of(
                        new FieldHistoricalAlias(10L, "legacy_phone", 100L),
                        new FieldHistoricalAlias(10L, "former_phone", 101L)),
                12L, List.of(new FieldHistoricalAlias(12L, "former_phone", 102L))));
        var service = service(fieldRepository, historyService);

        StandardReferenceResolveResponse response = service.resolve(new StandardReferenceResolveRequest(
                1L,
                StandardReferenceType.FIELD,
                List.of("legacy_phone", "former_phone")
        ));

        StandardReferenceResolutionResult historical = response.results().get(0);
        assertEquals(StandardReferenceResolutionStatus.CURRENT, historical.resolutionStatus());
        assertEquals("field:1:10", historical.stableRef());
        assertEquals("legacy_phone", historical.matchedAlias());
        assertEquals(StandardReferenceConfidence.MEDIUM, historical.confidence());
        assertTrue(historical.evidenceLinks().contains("dataspec://change-logs/100"));

        StandardReferenceResolutionResult ambiguous = response.results().get(1);
        assertEquals(StandardReferenceResolutionStatus.AMBIGUOUS, ambiguous.resolutionStatus());
        assertNull(ambiguous.canonicalRef());
        assertTrue(ambiguous.evidenceLinks().containsAll(List.of(
                "dataspec://change-logs/101",
                "dataspec://change-logs/102")));
    }

    @Test
    void returnsAmbiguousUnknownCrossProjectAndRedactedResultsWithoutGuessing() {
        FieldRepository fieldRepository = mock(FieldRepository.class);
        Field orderPhone = field(10L, 1L, "order_phone", "phone", "enabled", null);
        Field userPhone = field(12L, 1L, "user_phone", "phone", "enabled", null);
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(orderPhone, userPhone));
        var service = service(fieldRepository);

        StandardReferenceResolveResponse response = service.resolve(new StandardReferenceResolveRequest(
                1L,
                StandardReferenceType.FIELD,
                List.of("phone", "missing_field", "field:2:99", "password=secret123")
        ));

        assertEquals(StandardReferenceResolutionStatus.AMBIGUOUS, response.results().get(0).resolutionStatus());
        assertNull(response.results().get(0).canonicalRef());
        assertTrue(response.results().get(0).warnings().getFirst().contains("多个"));

        assertEquals(StandardReferenceResolutionStatus.UNKNOWN, response.results().get(1).resolutionStatus());
        assertNull(response.results().get(1).stableRef());

        assertEquals(StandardReferenceResolutionStatus.CROSS_PROJECT, response.results().get(2).resolutionStatus());
        assertNull(response.results().get(2).objectId());
        assertNull(response.results().get(2).currentName());

        StandardReferenceResolutionResult redacted = response.results().get(3);
        assertEquals(StandardReferenceResolutionStatus.UNKNOWN, redacted.resolutionStatus());
        assertEquals("password=[REDACTED]", redacted.inputRef());
        assertFalse(redacted.toString().contains("secret123"));
    }

    @Test
    void resolvesEnumRuleAndSnapshotStableReferences() {
        EnumDictRepository enumRepository = mock(EnumDictRepository.class);
        RuleConfigRepository ruleRepository = mock(RuleConfigRepository.class);
        StandardSnapshotRepository snapshotRepository = mock(StandardSnapshotRepository.class);
        EnumDict status = enumDict(20L, 1L, "order_status");
        RuleConfig rule = rule(1L, "field_naming_snake_case");
        StandardSnapshot snapshot = snapshot(7L, 1L, "v2026.07");
        when(enumRepository.findDictsByProjectId(1L)).thenReturn(List.of(status));
        when(ruleRepository.findByProjectId(1L)).thenReturn(List.of(rule));
        when(snapshotRepository.findByProjectId(1L)).thenReturn(List.of(snapshot));
        var service = service(mock(FieldRepository.class), enumRepository, ruleRepository, snapshotRepository);

        StandardReferenceResolveResponse enumResponse = service.resolve(new StandardReferenceResolveRequest(
                1L,
                StandardReferenceType.ENUM,
                List.of("enum:1:20", "order_status")
        ));
        assertEquals("enum:1:20", enumResponse.results().get(0).stableRef());
        assertEquals("enum:1:20", enumResponse.results().get(1).canonicalRef());

        StandardReferenceResolveResponse ruleResponse = service.resolve(new StandardReferenceResolveRequest(
                1L,
                StandardReferenceType.RULE,
                List.of("rule:1:field_naming_snake_case")
        ));
        assertEquals(StandardReferenceResolutionStatus.CURRENT, ruleResponse.results().getFirst().resolutionStatus());
        assertEquals("rule:1:field_naming_snake_case", ruleResponse.results().getFirst().canonicalRef());

        StandardReferenceResolveResponse snapshotResponse = service.resolve(new StandardReferenceResolveRequest(
                1L,
                StandardReferenceType.SNAPSHOT,
                List.of("snapshot:1:7", "snapshot:1:v2026.07")
        ));
        assertEquals("snapshot:1:7", snapshotResponse.results().get(0).stableRef());
        assertEquals("snapshot:1:7", snapshotResponse.results().get(1).canonicalRef());
    }

    private StandardReferenceResolutionServiceImpl service(FieldRepository fieldRepository) {
        FieldHistoricalAliasService historyService = mock(FieldHistoricalAliasService.class);
        when(historyService.load(anyLong(), anyList())).thenReturn(Map.of());
        return service(fieldRepository, historyService);
    }

    private StandardReferenceResolutionServiceImpl service(
            FieldRepository fieldRepository,
            FieldHistoricalAliasService historyService
    ) {
        return service(
                fieldRepository,
                mock(EnumDictRepository.class),
                mock(RuleConfigRepository.class),
                mock(StandardSnapshotRepository.class),
                historyService);
    }

    private StandardReferenceResolutionServiceImpl service(
            FieldRepository fieldRepository,
            EnumDictRepository enumRepository,
            RuleConfigRepository ruleRepository,
            StandardSnapshotRepository snapshotRepository
    ) {
        FieldHistoricalAliasService historyService = mock(FieldHistoricalAliasService.class);
        when(historyService.load(anyLong(), anyList())).thenReturn(Map.of());
        return service(fieldRepository, enumRepository, ruleRepository, snapshotRepository, historyService);
    }

    private StandardReferenceResolutionServiceImpl service(
            FieldRepository fieldRepository,
            EnumDictRepository enumRepository,
            RuleConfigRepository ruleRepository,
            StandardSnapshotRepository snapshotRepository,
            FieldHistoricalAliasService historyService
    ) {
        return new StandardReferenceResolutionServiceImpl(
                fieldRepository,
                enumRepository,
                ruleRepository,
                snapshotRepository,
                historyService);
    }

    private Field field(Long id, Long projectId, String name, String aliases, String status, Long replacementFieldId) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName(name);
        field.setAliases(aliases);
        field.setStatus(status);
        field.setReplacementFieldId(replacementFieldId);
        return field;
    }

    private EnumDict enumDict(Long id, Long projectId, String code) {
        EnumDict dict = new EnumDict();
        dict.setId(id);
        dict.setProjectId(projectId);
        dict.setCode(code);
        dict.setName(code);
        return dict;
    }

    private RuleConfig rule(Long projectId, String ruleCode) {
        RuleConfig rule = new RuleConfig();
        rule.setProjectId(projectId);
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleCode);
        rule.setEnabled(true);
        return rule;
    }

    private StandardSnapshot snapshot(Long id, Long projectId, String version) {
        StandardSnapshot snapshot = new StandardSnapshot();
        snapshot.setId(id);
        snapshot.setProjectId(projectId);
        snapshot.setVersion(version);
        snapshot.setName(version);
        return snapshot;
    }
}
