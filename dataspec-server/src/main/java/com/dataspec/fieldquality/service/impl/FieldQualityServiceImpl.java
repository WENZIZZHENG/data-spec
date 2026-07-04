package com.dataspec.fieldquality.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldquality.model.FieldQualityIssue;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityLevel;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.model.FieldQualitySeverity;
import com.dataspec.fieldquality.model.FieldQualitySummary;
import com.dataspec.fieldquality.service.FieldQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 基于现有字段元数据实时计算质量评分，不写入字段库。
 */
@Service
@RequiredArgsConstructor
public class FieldQualityServiceImpl implements FieldQualityService {

    private static final int MAX_SCORE = 100;
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "phone", "mobile", "tel", "email", "mail", "id_card", "idcard", "sfzh", "cert",
            "password", "passwd", "pwd", "token", "secret", "address", "addr",
            "手机", "电话", "邮箱", "邮件", "身份证", "证件", "密码", "令牌", "地址"
    );
    private static final Set<String> CODE_SET_KEYWORDS = Set.of(
            "status", "state", "type", "category", "kind", "flag", "level", "enum",
            "状态", "类型", "类别", "分类", "标记", "等级", "枚举"
    );
    private static final Set<String> REPLACEMENT_KEYWORDS = Set.of(
            "replace", "replacement", "alternative", "migration", "migrate", "instead",
            "替代", "迁移", "改用", "使用", "新字段"
    );
    private static final Set<String> FORMAT_SENSITIVE_KEYWORDS = Set.of(
            "amount", "money", "price", "fee", "cent", "yuan", "currency",
            "phone", "mobile", "tel", "email", "mail", "time", "timestamp", "date", "datetime",
            "json", "status", "state", "type", "category", "kind", "flag", "level", "enum", "code",
            "金额", "价格", "费用", "分", "元", "币种", "手机", "电话", "邮箱", "邮件",
            "时间", "日期", "时区", "状态", "类型", "类别", "分类", "标记", "等级", "枚举", "编码"
    );

    private final FieldService fieldService;

    @Override
    public FieldQualityReport report(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        List<Field> fields = fieldService.listByProject(projectId);
        FieldQualityReport report = new FieldQualityReport();
        for (Field field : fields) {
            report.getFields().add(scoreField(field));
        }
        report.getFields().sort(Comparator
                .comparingInt(FieldQualityItem::getScore)
                .thenComparing(item -> nullToEmpty(item.getName())));
        buildSummary(report);
        return report;
    }

    private FieldQualityItem scoreField(Field field) {
        FieldQualityItem item = baseItem(field);
        List<FieldQualityIssue> issues = item.getIssues();

        addIf(issues, isBlank(field.getDisplayName()), issue(
                "display_name_missing",
                FieldQualitySeverity.SUGGESTION,
                "字段缺少显示名称，AI 难以理解业务含义",
                "补充字段显示名称",
                10));
        addIf(issues, isBlank(field.getComment()), issue(
                "comment_missing",
                FieldQualitySeverity.WARNING,
                "字段缺少注释，AI 生成 SQL 时缺少语义依据",
                "补充字段注释",
                20));
        addIf(issues, isBlank(field.getAliases()), issue(
                "aliases_missing",
                FieldQualitySeverity.SUGGESTION,
                "字段缺少别名，历史字段名或自然语言描述不易命中",
                "补充常见别名",
                10));
        addIf(issues, isBlank(field.getExampleValue()), issue(
                "example_missing",
                FieldQualitySeverity.SUGGESTION,
                "字段缺少示例值，AI 难以判断值形态",
                "补充示例值",
                10));
        addIf(issues, isBlank(field.getCategory()) && isBlank(field.getTags()), issue(
                "classification_missing",
                FieldQualitySeverity.SUGGESTION,
                "字段缺少分类或标签，按业务域裁剪 AI Context 时不易定位",
                "补充分类或标签",
                10));
        addIf(issues, looksSensitive(field) && !Boolean.TRUE.equals(field.getSensitive()), issue(
                "sensitive_not_marked",
                FieldQualitySeverity.ERROR,
                "字段疑似敏感数据但未标记 sensitive",
                "确认是否敏感并设置敏感标记",
                20));
        addIf(issues, looksCodeSetField(field) && field.getCodeSetId() == null, issue(
                "code_set_missing",
                FieldQualitySeverity.WARNING,
                "字段疑似枚举、状态或类型字段但未关联代码集",
                "关联代码集或补充枚举说明",
                15));
        addIf(issues, isDeprecatedOrDisabled(field) && lacksReplacementGuidance(field), issue(
                "deprecated_without_replacement",
                FieldQualitySeverity.WARNING,
                "字段已废弃或停用，但缺少替代字段或迁移说明",
                "为废弃字段补充替代字段或迁移说明",
                10));
        addIf(issues, looksFormatSensitive(field) && lacksFormatExamples(field), issue(
                "format_examples_missing",
                FieldQualitySeverity.SUGGESTION,
                "字段疑似需要稳定值格式，但缺少格式约束或正反例样例",
                "补充格式类型、单位、正则、时区、空值策略或正反例样例",
                10));

        int score = MAX_SCORE - issues.stream().mapToInt(FieldQualityIssue::getScorePenalty).sum();
        item.setScore(Math.max(0, score));
        item.setLevel(levelOf(item.getScore()));
        item.setSuggestions(issues.stream()
                .map(FieldQualityIssue::getSuggestedAction)
                .distinct()
                .toList());
        return item;
    }

    private FieldQualityItem baseItem(Field field) {
        FieldQualityItem item = new FieldQualityItem();
        item.setFieldId(field.getId());
        item.setName(field.getName());
        item.setDisplayName(field.getDisplayName());
        item.setDataType(field.getDataType());
        item.setStatus(field.getStatus());
        item.setSensitive(field.getSensitive());
        item.setCodeSetId(field.getCodeSetId());
        return item;
    }

    private void buildSummary(FieldQualityReport report) {
        FieldQualitySummary summary = report.getSummary();
        summary.setTotalFieldCount(report.getFields().size());
        if (report.getFields().isEmpty()) {
            summary.setAverageScore(0);
            return;
        }

        int totalScore = 0;
        for (FieldQualityItem item : report.getFields()) {
            totalScore += item.getScore();
            if (FieldQualityLevel.GOOD.equals(item.getLevel())) {
                summary.setGoodCount(summary.getGoodCount() + 1);
            } else if (FieldQualityLevel.WARNING.equals(item.getLevel())) {
                summary.setWarningCount(summary.getWarningCount() + 1);
            } else {
                summary.setPoorCount(summary.getPoorCount() + 1);
            }
            for (FieldQualityIssue issue : item.getIssues()) {
                incrementIssueCount(summary, issue.getSeverity());
            }
        }
        summary.setAverageScore(Math.round((float) totalScore / report.getFields().size()));
        summary.setLowQualityCount(summary.getWarningCount() + summary.getPoorCount());
    }

    private void incrementIssueCount(FieldQualitySummary summary, FieldQualitySeverity severity) {
        if (FieldQualitySeverity.ERROR.equals(severity)) {
            summary.setErrorIssueCount(summary.getErrorIssueCount() + 1);
        } else if (FieldQualitySeverity.WARNING.equals(severity)) {
            summary.setWarningIssueCount(summary.getWarningIssueCount() + 1);
        } else {
            summary.setSuggestionIssueCount(summary.getSuggestionIssueCount() + 1);
        }
    }

    private void addIf(List<FieldQualityIssue> issues, boolean condition, FieldQualityIssue issue) {
        if (condition) {
            issues.add(issue);
        }
    }

    private FieldQualityIssue issue(
            String code,
            FieldQualitySeverity severity,
            String message,
            String suggestedAction,
            int scorePenalty
    ) {
        return new FieldQualityIssue(code, severity, message, suggestedAction, scorePenalty);
    }

    private FieldQualityLevel levelOf(int score) {
        if (score >= 85) {
            return FieldQualityLevel.GOOD;
        }
        if (score >= 65) {
            return FieldQualityLevel.WARNING;
        }
        return FieldQualityLevel.POOR;
    }

    private boolean looksSensitive(Field field) {
        return containsKeyword(combinedText(field), SENSITIVE_KEYWORDS);
    }

    private boolean looksCodeSetField(Field field) {
        return containsKeyword(combinedText(field), CODE_SET_KEYWORDS);
    }

    private boolean looksFormatSensitive(Field field) {
        return containsKeyword(combinedText(field) + " " + nullToEmpty(field.getDataType()), FORMAT_SENSITIVE_KEYWORDS);
    }

    private boolean lacksFormatExamples(Field field) {
        return isBlank(field.getFormatType())
                && isBlank(field.getFormatPattern())
                && isBlank(field.getFormatUnit())
                && isBlank(field.getFormatPrecision())
                && isBlank(field.getFormatTimezone())
                && isBlank(field.getFormatNullPolicy())
                && isBlank(field.getValidExamplesJson())
                && isBlank(field.getInvalidExamplesJson())
                && isBlank(field.getFormatNotes());
    }

    private boolean isDeprecatedOrDisabled(Field field) {
        String status = nullToEmpty(field.getStatus()).toLowerCase(Locale.ROOT);
        return "deprecated".equals(status) || "disabled".equals(status);
    }

    private boolean lacksReplacementGuidance(Field field) {
        if (field.getReplacementFieldId() != null || !isBlank(field.getReplacementReason())) {
            return false;
        }
        return !containsKeyword(combinedText(field), REPLACEMENT_KEYWORDS);
    }

    private boolean containsKeyword(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String combinedText(Field field) {
        return String.join(" ",
                nullToEmpty(field.getName()),
                nullToEmpty(field.getDisplayName()),
                nullToEmpty(field.getComment()),
                nullToEmpty(field.getAliases()),
                nullToEmpty(field.getCategory()),
                nullToEmpty(field.getTags())
        ).toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
