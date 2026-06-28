package com.dataspec.standardchange;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldimpact.model.FieldImpactItem;
import com.dataspec.fieldimpact.model.FieldImpactReport;
import com.dataspec.fieldimpact.model.FieldImpactSeverity;
import com.dataspec.fieldimpact.model.FieldImpactType;
import com.dataspec.fieldimpact.service.FieldImpactService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.standardchange.model.FieldChangePreviewReq;
import com.dataspec.standardchange.model.RuleChangePreviewReq;
import com.dataspec.standardchange.service.impl.StandardChangePreviewServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandardChangePreviewServiceImplTest {

    @Test
    void previewFieldUpdate_returnsHighRiskWithImpactAndRollbackHints() {
        FieldService fieldService = mock(FieldService.class);
        FieldImpactService impactService = mock(FieldImpactService.class);
        RuleConfigService ruleService = mock(RuleConfigService.class);
        StandardSnapshotService snapshotService = mock(StandardSnapshotService.class);
        Field field = field(10L, 1L);
        when(fieldService.getById(10L)).thenReturn(field);
        when(impactService.report(1L, 10L)).thenReturn(impactReport());
        when(snapshotService.getCurrentSnapshot(1L)).thenReturn(snapshot());
        StandardChangePreviewServiceImpl service = new StandardChangePreviewServiceImpl(
                fieldService, impactService, ruleService, snapshotService);

        var preview = service.previewFieldUpdate(10L, new FieldChangePreviewReq(
                1L,
                "user_identifier",
                "用户标识",
                "varchar",
                64,
                null,
                null,
                false,
                null,
                "用户标识",
                null,
                null,
                "uid,user_id",
                "user",
                null,
                false,
                "enabled",
                null));

        assertThat(preview.riskLevel()).isEqualTo("HIGH");
        assertThat(preview.requiresConfirmation()).isTrue();
        assertThat(preview.changes()).extracting("attribute").contains("name", "dataType", "nullable", "aliases");
        assertThat(preview.impacts()).extracting("impactType").contains("TEMPLATE", "STANDARD_SNAPSHOT");
        assertThat(preview.validationCommands()).anySatisfy(command -> assertThat(command).contains("dataspec-cli.mjs lint"));
        assertThat(preview.rollbackHints()).extracting("type").contains("CHANGE_LOG", "SNAPSHOT");
        assertThat(preview.currentSnapshot().specVersion()).isEqualTo("v1");
    }

    @Test
    void previewRuleUpdate_marksDisablingErrorRuleAsHighRisk() {
        var service = serviceWithRule(rule(true, "ERROR"));

        var preview = service.previewRuleUpdate(7L, new RuleChangePreviewReq(
                1L,
                "字段命名",
                "ERROR",
                false,
                "{}"));

        assertThat(preview.riskLevel()).isEqualTo("HIGH");
        assertThat(preview.requiresConfirmation()).isTrue();
        assertThat(preview.changes()).extracting("attribute").contains("enabled");
        assertThat(preview.impacts()).extracting("impactType").contains("SQL_LINT", "AI_CONTEXT", "RULE_BASELINE");
        assertThat(preview.rollbackHints()).extracting("type").contains("CHANGE_LOG");
    }

    @Test
    void previewRuleUpdate_returnsInfoWhenNothingChanges() {
        var service = serviceWithRule(rule(true, "WARNING"));

        var preview = service.previewRuleUpdate(7L, new RuleChangePreviewReq(
                1L,
                "字段命名",
                "WARNING",
                true,
                "{}"));

        assertThat(preview.riskLevel()).isEqualTo("INFO");
        assertThat(preview.requiresConfirmation()).isFalse();
        assertThat(preview.changes()).isEmpty();
        assertThat(preview.impacts()).isEmpty();
        assertThat(preview.summary()).contains("没有检测到");
    }

    private StandardChangePreviewServiceImpl serviceWithRule(RuleConfig rule) {
        FieldService fieldService = mock(FieldService.class);
        FieldImpactService impactService = mock(FieldImpactService.class);
        RuleConfigService ruleService = mock(RuleConfigService.class);
        StandardSnapshotService snapshotService = mock(StandardSnapshotService.class);
        when(ruleService.getById(7L)).thenReturn(rule);
        when(snapshotService.getCurrentSnapshot(1L)).thenReturn(StandardSnapshotInfo.unversioned(1L));
        return new StandardChangePreviewServiceImpl(fieldService, impactService, ruleService, snapshotService);
    }

    private Field field(Long id, Long projectId) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName("user_id");
        field.setDisplayName("用户 ID");
        field.setDataType("bigint");
        field.setLength(null);
        field.setNullable(true);
        field.setAliases("uid");
        field.setCategory("user");
        field.setSensitive(false);
        field.setStatus("enabled");
        return field;
    }

    private RuleConfig rule(boolean enabled, String severity) {
        RuleConfig rule = new RuleConfig();
        rule.setId(7L);
        rule.setProjectId(1L);
        rule.setRuleCode("field_naming_snake_case");
        rule.setRuleName("字段命名");
        rule.setSeverity(severity);
        rule.setEnabled(enabled);
        rule.setParamsJson("{}");
        return rule;
    }

    private FieldImpactReport impactReport() {
        FieldImpactReport report = new FieldImpactReport();
        report.getImpacts().add(impact(FieldImpactType.TEMPLATE, FieldImpactSeverity.WARNING, "模板引用"));
        report.getImpacts().add(impact(FieldImpactType.STANDARD_SNAPSHOT, FieldImpactSeverity.INFO, "标准快照"));
        report.getSummary().setTotalImpactCount(2);
        report.getSummary().setTemplateImpactCount(1);
        report.getSummary().setSnapshotImpactCount(1);
        return report;
    }

    private FieldImpactItem impact(FieldImpactType type, FieldImpactSeverity severity, String sourceName) {
        FieldImpactItem item = new FieldImpactItem();
        item.setImpactType(type);
        item.setSeverity(severity);
        item.setSourceId(99L);
        item.setSourceName(sourceName);
        item.setCount(1);
        item.setDescription(sourceName);
        item.setMetadata(new LinkedHashMap<>());
        return item;
    }

    private StandardSnapshotInfo snapshot() {
        return new StandardSnapshotInfo(3L, 1L, "v1", "基线", null, "hash", LocalDateTime.now(), true);
    }
}
