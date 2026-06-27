package com.dataspec.ruleexemption;

import com.dataspec.common.exception.BizException;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.Severity;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.repository.RuleExemptionRepository;
import com.dataspec.ruleexemption.service.impl.RuleExemptionServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleExemptionServiceImplTest {

    @Test
    void create_requiresReasonAndScopedTarget() {
        RuleExemptionRepository repository = mock(RuleExemptionRepository.class);
        RuleExemptionServiceImpl service = new RuleExemptionServiceImpl(repository);

        RuleExemption noReason = baseExemption();
        noReason.setReason(" ");
        BizException reasonError = assertThrows(BizException.class, () -> service.create(noReason));
        assertTrue(reasonError.getMessage().contains("豁免原因"));

        RuleExemption noScope = baseExemption();
        noScope.setTableName(" ");
        noScope.setColumnName(null);
        BizException scopeError = assertThrows(BizException.class, () -> service.create(noScope));
        assertTrue(scopeError.getMessage().contains("表名或字段名"));
    }

    @Test
    void create_trimsFieldsAndDefaultsEnabled() {
        RuleExemptionRepository repository = mock(RuleExemptionRepository.class);
        RuleExemptionServiceImpl service = new RuleExemptionServiceImpl(repository);
        RuleExemption exemption = baseExemption();
        exemption.setRuleCode(" field_naming_snake_case ");
        exemption.setTableName(" users ");
        exemption.setColumnName(" username ");
        exemption.setReason(" 历史字段兼容 ");

        RuleExemption saved = service.create(exemption);

        assertTrue(saved.getEnabled());
        assertEquals("field_naming_snake_case", saved.getRuleCode());
        assertEquals("users", saved.getTableName());
        assertEquals("username", saved.getColumnName());
        assertEquals("历史字段兼容", saved.getReason());
        verify(repository).insert(saved);
    }

    @Test
    void applySuppressions_marksMatchingEnabledUnexpiredIssue() {
        RuleExemptionRepository repository = mock(RuleExemptionRepository.class);
        RuleExemption exemption = baseExemption();
        exemption.setId(9L);
        when(repository.findByProjectId(1L)).thenReturn(List.of(exemption));
        RuleExemptionServiceImpl service = new RuleExemptionServiceImpl(repository);
        LintIssue issue = LintIssue.builder()
                .severity(Severity.ERROR)
                .ruleCode("table_naming_snake_case")
                .tableName("UserOrder")
                .build();

        service.applySuppressions(1L, List.of(issue));

        assertTrue(issue.getSuppressed());
        assertEquals(9L, issue.getSuppressionId());
        assertEquals("历史表兼容", issue.getSuppressionReason());
    }

    @Test
    void applySuppressions_ignoresDisabledAndExpiredExemptions() {
        RuleExemptionRepository repository = mock(RuleExemptionRepository.class);
        RuleExemption disabled = baseExemption();
        disabled.setEnabled(false);
        RuleExemption expired = baseExemption();
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repository.findByProjectId(1L)).thenReturn(List.of(disabled, expired));
        RuleExemptionServiceImpl service = new RuleExemptionServiceImpl(repository);
        LintIssue issue = LintIssue.builder()
                .severity(Severity.ERROR)
                .ruleCode("table_naming_snake_case")
                .tableName("UserOrder")
                .build();

        service.applySuppressions(1L, List.of(issue));

        assertNull(issue.getSuppressed());
        assertNull(issue.getSuppressionId());
    }

    @Test
    void disable_setsEnabledFalse() {
        RuleExemptionRepository repository = mock(RuleExemptionRepository.class);
        RuleExemption exemption = baseExemption();
        exemption.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(exemption));
        RuleExemptionServiceImpl service = new RuleExemptionServiceImpl(repository);

        service.disable(5L);

        assertFalse(exemption.getEnabled());
        verify(repository).update(exemption);
    }

    private RuleExemption baseExemption() {
        RuleExemption exemption = new RuleExemption();
        exemption.setProjectId(1L);
        exemption.setRuleCode("table_naming_snake_case");
        exemption.setTableName("UserOrder");
        exemption.setReason("历史表兼容");
        exemption.setEnabled(true);
        return exemption;
    }
}
