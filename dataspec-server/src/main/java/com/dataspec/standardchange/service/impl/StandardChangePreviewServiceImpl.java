package com.dataspec.standardchange.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.fieldimpact.model.FieldImpactItem;
import com.dataspec.fieldimpact.model.FieldImpactReport;
import com.dataspec.fieldimpact.model.FieldImpactSummary;
import com.dataspec.fieldimpact.service.FieldImpactService;
import com.dataspec.field.service.FieldService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.standardchange.model.FieldChangePreviewReq;
import com.dataspec.standardchange.model.RuleChangePreviewReq;
import com.dataspec.standardchange.model.StandardChangePreview;
import com.dataspec.standardchange.model.StandardChangePreviewChange;
import com.dataspec.standardchange.model.StandardChangePreviewImpact;
import com.dataspec.standardchange.model.StandardChangeRollbackHint;
import com.dataspec.standardchange.service.StandardChangePreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 标准变更 what-if 预览实现。该服务只读聚合现有标准、影响分析和快照信息，不写入变更日志。
 */
@Service
@RequiredArgsConstructor
public class StandardChangePreviewServiceImpl implements StandardChangePreviewService {

    private static final String TARGET_FIELD = "field";
    private static final String TARGET_RULE = "rule";
    private static final String OPERATION_FIELD_UPDATE = "FIELD_UPDATE";
    private static final String OPERATION_RULE_UPDATE = "RULE_UPDATE";
    private static final String OPERATION_RULE_TOGGLE = "RULE_TOGGLE";
    private static final String RISK_INFO = "INFO";
    private static final String RISK_WARNING = "WARNING";
    private static final String RISK_HIGH = "HIGH";
    private static final Set<String> CRITICAL_FIELD_ATTRIBUTES = Set.of(
            "name", "dataType", "status", "aliases", "codeSetId", "sensitive");

    private final FieldService fieldService;
    private final FieldImpactService fieldImpactService;
    private final RuleConfigService ruleConfigService;
    private final StandardSnapshotService standardSnapshotService;

    @Override
    public StandardChangePreview previewFieldUpdate(Long fieldId, FieldChangePreviewReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (fieldId == null) {
            throw new BizException("字段ID不能为空");
        }
        Field existing = fieldService.getById(fieldId);
        if (!Objects.equals(existing.getProjectId(), req.projectId())) {
            throw new BizException("字段不属于当前项目");
        }

        FieldImpactReport report = fieldImpactService.report(req.projectId(), fieldId);
        List<StandardChangePreviewChange> changes = fieldChanges(existing, req);
        List<StandardChangePreviewImpact> impacts = changes.isEmpty()
                ? List.of()
                : report.getImpacts().stream().map(this::impactFromField).toList();
        StandardSnapshotInfo snapshot = standardSnapshotService.getCurrentSnapshot(req.projectId());
        String risk = fieldRisk(changes, report.getSummary());
        return new StandardChangePreview(
                req.projectId(),
                TARGET_FIELD,
                fieldId,
                existing.getName(),
                OPERATION_FIELD_UPDATE,
                risk,
                shouldConfirm(risk),
                fieldSummary(changes, report.getSummary()),
                changes,
                impacts,
                validationCommands(req.projectId()),
                fieldRollbackHints(fieldId, snapshot),
                snapshot
        );
    }

    @Override
    public StandardChangePreview previewRuleUpdate(Long ruleId, RuleChangePreviewReq req) {
        if (req == null || req.projectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (ruleId == null) {
            throw new BizException("规则ID不能为空");
        }
        RuleConfig existing = ruleConfigService.getById(ruleId);
        if (!Objects.equals(existing.getProjectId(), req.projectId())) {
            throw new BizException("规则不属于当前项目");
        }
        List<StandardChangePreviewChange> changes = ruleChanges(existing, req);
        StandardSnapshotInfo snapshot = standardSnapshotService.getCurrentSnapshot(req.projectId());
        String risk = ruleRisk(existing, req, changes);
        return new StandardChangePreview(
                req.projectId(),
                TARGET_RULE,
                ruleId,
                existing.getRuleCode(),
                OPERATION_RULE_UPDATE,
                risk,
                shouldConfirm(risk),
                ruleSummary(changes, risk),
                changes,
                changes.isEmpty() ? List.of() : ruleImpacts(existing.getId(), existing.getRuleCode(), risk),
                validationCommands(req.projectId()),
                ruleRollbackHints(ruleId, snapshot),
                snapshot
        );
    }

    @Override
    public StandardChangePreview previewRuleToggle(Long ruleId, Long projectId, boolean enabled) {
        RuleConfig existing = ruleConfigService.getById(ruleId);
        return previewRuleUpdate(ruleId, new RuleChangePreviewReq(
                projectId,
                existing.getRuleName(),
                existing.getSeverity(),
                enabled,
                existing.getParamsJson()));
    }

    private List<StandardChangePreviewChange> fieldChanges(Field existing, FieldChangePreviewReq req) {
        List<StandardChangePreviewChange> changes = new ArrayList<>();
        addChange(changes, "name", existing.getName(), req.name(), "字段名会影响模板、SQL 和 AI Context 的字段引用");
        addChange(changes, "displayName", existing.getDisplayName(), req.displayName(), "显示名会影响用户和 AI 的字段理解");
        addChange(changes, "dataType", existing.getDataType(), req.dataType(), "数据类型会影响 DDL 生成和 SQL lint 判断");
        addChange(changes, "length", existing.getLength(), req.length(), "长度会影响 DDL 输出");
        addChange(changes, "precisionVal", existing.getPrecisionVal(), req.precisionVal(), "精度会影响数值字段 DDL 输出");
        addChange(changes, "scaleVal", existing.getScaleVal(), req.scaleVal(), "小数位会影响数值字段 DDL 输出");
        addChange(changes, "nullable", existing.getNullable(), req.nullable(), "空值约束会影响 DDL 和 AI 建表建议");
        addChange(changes, "defaultValue", existing.getDefaultValue(), req.defaultValue(), "默认值会影响 DDL 输出");
        addChange(changes, "comment", existing.getComment(), req.comment(), "注释会影响数据字典和 AI Context");
        addChange(changes, "domainId", existing.getDomainId(), req.domainId(), "数据域会影响字段分组和检索");
        addChange(changes, "tags", existing.getTags(), req.tags(), "标签会影响字段分组、检索和 AI Context 裁剪");
        addChange(changes, "aliases", existing.getAliases(), req.aliases(), "别名会影响字段推荐和标准命中");
        addChange(changes, "category", existing.getCategory(), req.category(), "分类会影响字段分组和检索");
        addChange(changes, "codeSetId", existing.getCodeSetId(), req.codeSetId(), "代码集会影响枚举语义和 AI 输出");
        addChange(changes, "sensitive", existing.getSensitive(), req.sensitive(), "敏感标记会影响 AI Context 和导出提示");
        addChange(changes, "status", existing.getStatus(), req.status(), "状态会影响 AI 是否继续推荐该字段");
        addChange(changes, "exampleValue", existing.getExampleValue(), req.exampleValue(), "示例值会影响 AI 示例和数据字典");
        return changes;
    }

    private List<StandardChangePreviewChange> ruleChanges(RuleConfig existing, RuleChangePreviewReq req) {
        List<StandardChangePreviewChange> changes = new ArrayList<>();
        addRuleChange(changes, "ruleName", existing.getRuleName(), req.ruleName(), "规则名称会影响前端和 AI 解释");
        addRuleChange(changes, "severity", existing.getSeverity(), req.severity(), "规则级别会影响 SQL lint 阻断程度");
        addRuleChange(changes, "enabled", existing.getEnabled(), req.enabled(), "规则启停会影响 SQL lint 结果");
        addRuleChange(changes, "paramsJson", existing.getParamsJson(), req.paramsJson(), "规则参数会影响命中条件和 fixedSql 建议");
        return changes;
    }

    private void addChange(List<StandardChangePreviewChange> changes, String attribute, Object before, Object after, String description) {
        if (sameValue(before, after)) {
            return;
        }
        changes.add(new StandardChangePreviewChange(
                attribute,
                before,
                after,
                CRITICAL_FIELD_ATTRIBUTES.contains(attribute) ? RISK_WARNING : RISK_INFO,
                description));
    }

    private void addRuleChange(List<StandardChangePreviewChange> changes, String attribute, Object before, Object after, String description) {
        if (sameValue(before, after)) {
            return;
        }
        changes.add(new StandardChangePreviewChange(attribute, before, after, RISK_WARNING, description));
    }

    private boolean sameValue(Object before, Object after) {
        if (before instanceof String || after instanceof String) {
            return Objects.equals(trimToNull(before), trimToNull(after));
        }
        return Objects.equals(before, after);
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private StandardChangePreviewImpact impactFromField(FieldImpactItem item) {
        return new StandardChangePreviewImpact(
                item.getImpactType() == null ? "UNKNOWN" : item.getImpactType().name(),
                item.getSeverity() == null ? RISK_INFO : item.getSeverity().name(),
                item.getSourceId(),
                item.getSourceName(),
                item.getCount(),
                item.getDescription(),
                item.getMetadata() == null ? Map.of() : item.getMetadata()
        );
    }

    private List<StandardChangePreviewImpact> ruleImpacts(Long ruleId, String ruleCode, String risk) {
        List<StandardChangePreviewImpact> impacts = new ArrayList<>();
        impacts.add(ruleImpact("SQL_LINT", risk, ruleId, "SQL 校验", "规则变更会影响后续 SQL lint 结果和错误级别", ruleCode));
        impacts.add(ruleImpact("AI_CONTEXT", RISK_WARNING, ruleId, "AI Context", "规则变更会影响导出的规则约束和 AI 修 SQL 提示", ruleCode));
        impacts.add(ruleImpact("RULE_BASELINE", RISK_INFO, ruleId, "规则基线", "自定义规则变更可能使项目规则偏离当前基线", ruleCode));
        return impacts;
    }

    private StandardChangePreviewImpact ruleImpact(
            String impactType,
            String severity,
            Long ruleId,
            String title,
            String description,
            String ruleCode
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ruleId", ruleId);
        metadata.put("ruleCode", ruleCode);
        return new StandardChangePreviewImpact(impactType, severity, ruleId, title, 1, description, metadata);
    }

    private String fieldRisk(List<StandardChangePreviewChange> changes, FieldImpactSummary summary) {
        if (changes.isEmpty()) {
            return RISK_INFO;
        }
        boolean criticalChanged = changes.stream().anyMatch(change -> CRITICAL_FIELD_ATTRIBUTES.contains(change.attribute()));
        boolean highImpact = summary != null && (
                summary.getTemplateImpactCount() > 0 || summary.getSnapshotImpactCount() > 0);
        if (criticalChanged && highImpact) {
            return RISK_HIGH;
        }
        return criticalChanged ? RISK_WARNING : RISK_INFO;
    }

    private String ruleRisk(RuleConfig existing, RuleChangePreviewReq req, List<StandardChangePreviewChange> changes) {
        if (changes.isEmpty()) {
            return RISK_INFO;
        }
        boolean disablesErrorRule = Boolean.TRUE.equals(existing.getEnabled())
                && Boolean.FALSE.equals(req.enabled())
                && "ERROR".equalsIgnoreCase(existing.getSeverity());
        boolean becomesError = "ERROR".equalsIgnoreCase(req.severity())
                && !"ERROR".equalsIgnoreCase(existing.getSeverity());
        if (disablesErrorRule || becomesError) {
            return RISK_HIGH;
        }
        return RISK_WARNING;
    }

    private boolean shouldConfirm(String risk) {
        return RISK_HIGH.equals(risk) || RISK_WARNING.equals(risk);
    }

    private String fieldSummary(List<StandardChangePreviewChange> changes, FieldImpactSummary summary) {
        if (changes.isEmpty()) {
            return "没有检测到有效字段变更";
        }
        int totalImpact = summary == null ? 0 : summary.getTotalImpactCount();
        return "将修改 " + changes.size() + " 个字段属性，已知影响 " + totalImpact + " 项";
    }

    private String ruleSummary(List<StandardChangePreviewChange> changes, String risk) {
        if (changes.isEmpty()) {
            return "没有检测到有效规则变更";
        }
        return "将修改 " + changes.size() + " 个规则属性，风险等级 " + risk;
    }

    private List<String> validationCommands(Long projectId) {
        return List.of(
                "node tools/dataspec-cli.mjs lint <sql-file> --project " + projectId + " --format text",
                "node tools/dataspec-cli.mjs export-context --project " + projectId + " --cache"
        );
    }

    private List<StandardChangeRollbackHint> fieldRollbackHints(Long fieldId, StandardSnapshotInfo snapshot) {
        List<StandardChangeRollbackHint> hints = new ArrayList<>();
        hints.add(new StandardChangeRollbackHint(
                "CHANGE_LOG",
                "FIELD_UNDO",
                "保存后可在字段变更日志中查看最近 update 记录，并从兼容日志执行回退。",
                "/fields?fieldId=" + fieldId
        ));
        addSnapshotHint(hints, snapshot);
        return hints;
    }

    private List<StandardChangeRollbackHint> ruleRollbackHints(Long ruleId, StandardSnapshotInfo snapshot) {
        List<StandardChangeRollbackHint> hints = new ArrayList<>();
        hints.add(new StandardChangeRollbackHint(
                "CHANGE_LOG",
                "RULE_CHANGE_LOG",
                "保存后可在规则变更日志中查看 before/after；第一版不自动回滚规则。",
                "/rules?ruleId=" + ruleId
        ));
        addSnapshotHint(hints, snapshot);
        return hints;
    }

    private void addSnapshotHint(List<StandardChangeRollbackHint> hints, StandardSnapshotInfo snapshot) {
        if (snapshot == null || !snapshot.versioned()) {
            return;
        }
        hints.add(new StandardChangeRollbackHint(
                "SNAPSHOT",
                "CREATE_NEW_SNAPSHOT",
                "当前可信快照为 " + snapshot.specVersion() + "；接受变更后建议创建新快照作为后续 AI Context 基线。",
                "/snapshots"
        ));
    }
}
