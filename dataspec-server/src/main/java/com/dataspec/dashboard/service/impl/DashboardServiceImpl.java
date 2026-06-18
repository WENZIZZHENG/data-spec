package com.dataspec.dashboard.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.dashboard.model.DashboardSummary;
import com.dataspec.dashboard.model.IssueTrendPoint;
import com.dataspec.dashboard.model.RecentSqlCheck;
import com.dataspec.dashboard.service.DashboardService;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 个人工作台汇总服务实现。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int DEFAULT_FORBIDDEN_TERM_COUNT = 11;
    private static final Set<String> FIELD_STANDARD_RULES = Set.of(
            "field_naming_snake_case",
            "forbidden_field_name",
            "recommended_field_name",
            "field_suffix_type",
            "amount_field_unit"
    );

    private final FieldService fieldService;
    private final EnumDictService enumDictService;
    private final RuleConfigService ruleConfigService;
    private final SqlCheckRecordService sqlCheckRecordService;
    private final ObjectMapper objectMapper;

    @Override
    public DashboardSummary getSummary(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }

        List<RuleConfig> rules = ruleConfigService.listByProject(projectId);
        IPage<SqlCheckRecord> checksPage = sqlCheckRecordService.listByProject(projectId, 1, 20);
        List<SqlCheckRecord> records = checksPage.getRecords() != null ? checksPage.getRecords() : List.of();

        DashboardSummary summary = new DashboardSummary();
        summary.setFieldCount(fieldService.listByProject(projectId).size());
        summary.setEnumDictCount(enumDictService.listByProject(projectId).size());
        summary.setRuleCount(rules.size());
        summary.setForbiddenTermCount(resolveForbiddenTermCount(rules));
        summary.setRecentCheckCount(checksPage.getTotal());
        summary.setFieldHitRate(calculateFieldHitRate(records));
        summary.setRecentChecks(toRecentChecks(records.stream().limit(5).toList()));
        summary.setTrend(toTrend(records.stream().limit(10).toList()));
        return summary;
    }

    private Integer calculateFieldHitRate(List<SqlCheckRecord> records) {
        if (records.isEmpty()) {
            return null;
        }
        long hitCount = records.stream()
                .filter(record -> sqlCheckRecordService.parseIssues(record).stream().noneMatch(this::isFieldStandardIssue))
                .count();
        return Math.toIntExact(Math.round(hitCount * 100.0 / records.size()));
    }

    private boolean isFieldStandardIssue(LintIssue issue) {
        return issue != null && FIELD_STANDARD_RULES.contains(issue.getRuleCode());
    }

    private int resolveForbiddenTermCount(List<RuleConfig> rules) {
        return rules.stream()
                .filter(rule -> "forbidden_field_name".equals(rule.getRuleCode()))
                .findFirst()
                .map(this::forbiddenTermCount)
                .orElse(DEFAULT_FORBIDDEN_TERM_COUNT);
    }

    @SuppressWarnings("unchecked")
    private int forbiddenTermCount(RuleConfig rule) {
        if (rule.getParamsJson() == null || rule.getParamsJson().isBlank()) {
            return DEFAULT_FORBIDDEN_TERM_COUNT;
        }
        try {
            Map<String, Object> params = objectMapper.readValue(rule.getParamsJson(), Map.class);
            Object forbiddenNames = params.get("forbiddenNames");
            if (forbiddenNames instanceof List<?> list) {
                return list.size();
            }
            return DEFAULT_FORBIDDEN_TERM_COUNT;
        } catch (Exception e) {
            return DEFAULT_FORBIDDEN_TERM_COUNT;
        }
    }

    private List<RecentSqlCheck> toRecentChecks(List<SqlCheckRecord> records) {
        List<RecentSqlCheck> result = new ArrayList<>();
        for (SqlCheckRecord record : records) {
            result.add(new RecentSqlCheck(
                    record.getId(),
                    record.getCreatedAt(),
                    count(record.getErrorCount()),
                    count(record.getWarningCount()),
                    count(record.getSuggestionCount()),
                    issueCount(record)));
        }
        return result;
    }

    private List<IssueTrendPoint> toTrend(List<SqlCheckRecord> records) {
        return records.stream()
                .sorted(Comparator.comparing(
                        SqlCheckRecord::getCreatedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(record -> new IssueTrendPoint(
                        record.getId(),
                        record.getCreatedAt(),
                        issueCount(record),
                        count(record.getErrorCount()),
                        count(record.getWarningCount()),
                        count(record.getSuggestionCount())))
                .toList();
    }

    private int issueCount(SqlCheckRecord record) {
        return count(record.getErrorCount()) + count(record.getWarningCount()) + count(record.getSuggestionCount());
    }

    private int count(Integer value) {
        return value != null ? value : 0;
    }
}
