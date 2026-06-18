package com.dataspec.dashboard;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.dashboard.model.DashboardSummary;
import com.dataspec.dashboard.service.impl.DashboardServiceImpl;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 个人工作台汇总测试。
 */
class DashboardServiceTest {

    @Test
    void getSummary_calculatesCountsHitRateAndTrend() {
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        SqlCheckRecordService sqlCheckRecordService = mock(SqlCheckRecordService.class);
        DashboardServiceImpl service = new DashboardServiceImpl(
                fieldService,
                enumDictService,
                ruleConfigService,
                sqlCheckRecordService,
                new ObjectMapper());

        RuleConfig forbiddenRule = new RuleConfig();
        forbiddenRule.setRuleCode("forbidden_field_name");
        forbiddenRule.setParamsJson("{\"forbiddenNames\":[\"tmp\",\"flag1\"]}");
        RuleConfig snakeCaseRule = new RuleConfig();
        snakeCaseRule.setRuleCode("field_naming_snake_case");

        SqlCheckRecord cleanRecord = record(1L, 0, 0, 0, LocalDateTime.of(2026, 6, 18, 10, 0));
        SqlCheckRecord fieldIssueRecord = record(2L, 1, 0, 0, LocalDateTime.of(2026, 6, 18, 11, 0));
        Page<SqlCheckRecord> page = new Page<>(1, 20);
        page.setTotal(2);
        page.setRecords(List.of(fieldIssueRecord, cleanRecord));

        when(fieldService.listByProject(1L)).thenReturn(List.of(new Field(), new Field()));
        when(enumDictService.listByProject(1L)).thenReturn(List.of(new EnumDict()));
        when(ruleConfigService.listByProject(1L)).thenReturn(List.of(forbiddenRule, snakeCaseRule));
        when(sqlCheckRecordService.listByProject(1L, 1, 20)).thenReturn(page);
        when(sqlCheckRecordService.parseIssues(cleanRecord)).thenReturn(List.of());
        when(sqlCheckRecordService.parseIssues(fieldIssueRecord)).thenReturn(List.of(
                LintIssue.builder().ruleCode("field_naming_snake_case").build()));

        DashboardSummary summary = service.getSummary(1L);

        assertThat(summary.getFieldCount()).isEqualTo(2);
        assertThat(summary.getEnumDictCount()).isEqualTo(1);
        assertThat(summary.getRuleCount()).isEqualTo(2);
        assertThat(summary.getForbiddenTermCount()).isEqualTo(2);
        assertThat(summary.getRecentCheckCount()).isEqualTo(2);
        assertThat(summary.getFieldHitRate()).isEqualTo(50);
        assertThat(summary.getRecentChecks()).hasSize(2);
        assertThat(summary.getTrend()).hasSize(2);
    }

    private SqlCheckRecord record(Long id, int errors, int warnings, int suggestions, LocalDateTime createdAt) {
        SqlCheckRecord record = new SqlCheckRecord();
        record.setId(id);
        record.setProjectId(1L);
        record.setErrorCount(errors);
        record.setWarningCount(warnings);
        record.setSuggestionCount(suggestions);
        record.setCreatedAt(createdAt);
        return record;
    }
}
