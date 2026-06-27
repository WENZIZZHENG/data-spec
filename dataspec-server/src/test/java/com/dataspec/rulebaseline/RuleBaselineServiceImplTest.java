package com.dataspec.rulebaseline;

import com.dataspec.common.exception.BizException;
import com.dataspec.lint.model.LintRule;
import com.dataspec.lint.rules.AmountFieldRule;
import com.dataspec.lint.rules.CommentMissingRule;
import com.dataspec.lint.rules.FieldNamingSnakeCaseRule;
import com.dataspec.lint.rules.FieldSuffixTypeRule;
import com.dataspec.lint.rules.ForbiddenFieldNameRule;
import com.dataspec.lint.rules.RecommendedFieldNameRule;
import com.dataspec.lint.rules.RequiredColumnsRule;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.rulebaseline.entity.RuleBaseline;
import com.dataspec.rulebaseline.model.RuleBaselineApplyResult;
import com.dataspec.rulebaseline.model.RuleBaselinePackage;
import com.dataspec.rulebaseline.model.RuleBaselineRule;
import com.dataspec.rulebaseline.repository.RuleBaselineRepository;
import com.dataspec.rulebaseline.service.BuiltInRuleBaselines;
import com.dataspec.rulebaseline.service.impl.RuleBaselineServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RuleBaselineServiceImplTest {

    @Test
    void builtInTemplates_areValidAndIncludeExpectedSuites() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Set<String> knownRuleCodes = List.<LintRule>of(
                        new TableNameSnakeCaseRule(),
                        new FieldNamingSnakeCaseRule(),
                        new CommentMissingRule(),
                        new RequiredColumnsRule(),
                        new ForbiddenFieldNameRule(),
                        new RecommendedFieldNameRule(),
                        new FieldSuffixTypeRule(),
                        new AmountFieldRule())
                .stream()
                .map(LintRule::getCode)
                .collect(Collectors.toSet());

        assertEquals(3, BuiltInRuleBaselines.list().size());
        assertTrue(BuiltInRuleBaselines.find(BuiltInRuleBaselines.PERSONAL_DEFAULT).isPresent());
        assertTrue(BuiltInRuleBaselines.find(BuiltInRuleBaselines.STRICT).isPresent());
        assertTrue(BuiltInRuleBaselines.find(BuiltInRuleBaselines.LEGACY_COMPATIBLE).isPresent());
        for (var template : BuiltInRuleBaselines.list()) {
            assertEquals(template.rules().size(), template.ruleCount());
            for (RuleBaselineRule rule : template.rules()) {
                assertTrue(knownRuleCodes.contains(rule.ruleCode()));
                mapper.readTree(rule.paramsJson());
            }
        }
    }

    @Test
    void applyBuiltInBaseline_withoutOverwriteCreatesMissingAndSkipsExisting() throws Exception {
        RuleBaselineRepository baselineRepository = mock(RuleBaselineRepository.class);
        RuleConfigRepository ruleConfigRepository = mock(RuleConfigRepository.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(ruleConfigRepository.findByCodeAndProjectId(eq("table_naming_snake_case"), eq(1L)))
                .thenReturn(Optional.of(existingRule(7L, "table_naming_snake_case")));
        when(ruleConfigRepository.findByProjectId(1L)).thenReturn(List.of(existingRule(7L, "table_naming_snake_case")));
        when(ruleConfigService.create(any(RuleConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(baselineRepository.findByProjectId(1L)).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            RuleBaseline baseline = invocation.getArgument(0);
            baseline.setId(10L);
            return 1;
        }).when(baselineRepository).insert(any(RuleBaseline.class));
        RuleBaselineServiceImpl service = new RuleBaselineServiceImpl(
                baselineRepository,
                ruleConfigRepository,
                ruleConfigService,
                new ObjectMapper());

        RuleBaselineApplyResult result = service.applyBuiltInBaseline(1L, BuiltInRuleBaselines.PERSONAL_DEFAULT, false);

        assertEquals(7, result.created());
        assertEquals(0, result.updated());
        assertEquals(1, result.skipped());
        assertTrue(result.skippedRuleCodes().contains("table_naming_snake_case"));
        verify(ruleConfigService, times(7)).create(any(RuleConfig.class));
        verify(ruleConfigService, never()).update(anyLong(), any(RuleConfig.class));
        ArgumentCaptor<RuleBaseline> baselineCaptor = ArgumentCaptor.forClass(RuleBaseline.class);
        verify(baselineRepository).insert(baselineCaptor.capture());
        RuleBaseline saved = baselineCaptor.getValue();
        RuleBaselinePackage savedPackage = new ObjectMapper().findAndRegisterModules()
                .readValue(saved.getRulesJson(), RuleBaselinePackage.class);
        assertEquals(saved.getAppliedAt(), savedPackage.baseline().appliedAt());
        assertEquals(saved.getAppliedAt(), savedPackage.exportedAt());
    }

    @Test
    void applyBuiltInBaseline_withOverwriteUpdatesExistingRules() {
        RuleBaselineRepository baselineRepository = mock(RuleBaselineRepository.class);
        RuleConfigRepository ruleConfigRepository = mock(RuleConfigRepository.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(ruleConfigRepository.findByCodeAndProjectId(anyString(), eq(1L)))
                .thenReturn(Optional.of(existingRule(7L, "rule")));
        when(ruleConfigRepository.findByProjectId(1L)).thenReturn(List.of(existingRule(7L, "rule")));
        when(baselineRepository.findByProjectId(1L)).thenReturn(Optional.empty());
        RuleBaselineServiceImpl service = new RuleBaselineServiceImpl(
                baselineRepository,
                ruleConfigRepository,
                ruleConfigService,
                new ObjectMapper());

        RuleBaselineApplyResult result = service.applyBuiltInBaseline(1L, BuiltInRuleBaselines.PERSONAL_DEFAULT, true);

        assertEquals(0, result.created());
        assertEquals(8, result.updated());
        assertEquals(0, result.skipped());
        verify(ruleConfigService, times(8)).update(eq(7L), any(RuleConfig.class));
    }

    @Test
    void exportBaseline_omitsRuleIdsAndUsesStoredMetadata() {
        RuleBaselineRepository baselineRepository = mock(RuleBaselineRepository.class);
        RuleConfigRepository ruleConfigRepository = mock(RuleConfigRepository.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        RuleBaseline baseline = new RuleBaseline();
        baseline.setProjectId(1L);
        baseline.setBaselineKey(BuiltInRuleBaselines.PERSONAL_DEFAULT);
        baseline.setBaselineName("个人默认规则基线");
        baseline.setBaselineVersion("1.0.0");
        baseline.setSource("built_in");
        baseline.setAppliedAt(LocalDateTime.parse("2026-06-27T10:00:00"));
        when(baselineRepository.findByProjectId(1L)).thenReturn(Optional.of(baseline));
        when(ruleConfigRepository.findByProjectId(1L)).thenReturn(List.of(existingRule(7L, "field_naming_snake_case")));
        RuleBaselineServiceImpl service = new RuleBaselineServiceImpl(
                baselineRepository,
                ruleConfigRepository,
                ruleConfigService,
                new ObjectMapper());

        RuleBaselinePackage pkg = service.exportBaseline(1L);

        assertEquals(1, pkg.schemaVersion());
        assertEquals(BuiltInRuleBaselines.PERSONAL_DEFAULT, pkg.baseline().key());
        assertEquals(1, pkg.rules().size());
        assertEquals("field_naming_snake_case", pkg.rules().getFirst().ruleCode());
    }

    @Test
    void importBaseline_rejectsInvalidJsonBeforeWriting() {
        RuleBaselineRepository baselineRepository = mock(RuleBaselineRepository.class);
        RuleConfigRepository ruleConfigRepository = mock(RuleConfigRepository.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        RuleBaselineServiceImpl service = new RuleBaselineServiceImpl(
                baselineRepository,
                ruleConfigRepository,
                ruleConfigService,
                new ObjectMapper());
        RuleBaselinePackage pkg = new RuleBaselinePackage(
                BuiltInRuleBaselines.SCHEMA_VERSION,
                null,
                LocalDateTime.now(),
                List.of(new RuleBaselineRule("field_naming_snake_case", "字段 snake_case", "ERROR", true, "{bad")));

        assertThrows(BizException.class, () -> service.importBaseline(1L, pkg, false));
        verify(ruleConfigService, never()).create(any(RuleConfig.class));
        verify(ruleConfigService, never()).update(anyLong(), any(RuleConfig.class));
    }

    @Test
    void importBaseline_rejectsInvalidSeverityBeforeWriting() {
        RuleBaselineRepository baselineRepository = mock(RuleBaselineRepository.class);
        RuleConfigRepository ruleConfigRepository = mock(RuleConfigRepository.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        RuleBaselineServiceImpl service = new RuleBaselineServiceImpl(
                baselineRepository,
                ruleConfigRepository,
                ruleConfigService,
                new ObjectMapper());
        RuleBaselinePackage pkg = new RuleBaselinePackage(
                BuiltInRuleBaselines.SCHEMA_VERSION,
                null,
                LocalDateTime.now(),
                List.of(new RuleBaselineRule("field_naming_snake_case", "字段 snake_case", "BLOCKER", true, "{}")));

        assertThrows(BizException.class, () -> service.importBaseline(1L, pkg, false));
        verify(ruleConfigService, never()).create(any(RuleConfig.class));
        verify(ruleConfigService, never()).update(anyLong(), any(RuleConfig.class));
    }

    private RuleConfig existingRule(Long id, String code) {
        RuleConfig rule = new RuleConfig();
        rule.setId(id);
        rule.setProjectId(1L);
        rule.setRuleCode(code);
        rule.setRuleName(code);
        rule.setSeverity("ERROR");
        rule.setEnabled(true);
        rule.setParamsJson("{}");
        return rule;
    }
}
