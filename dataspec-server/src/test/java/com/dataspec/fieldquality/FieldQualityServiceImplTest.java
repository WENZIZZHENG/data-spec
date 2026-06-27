package com.dataspec.fieldquality;

import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldquality.model.FieldQualityLevel;
import com.dataspec.fieldquality.model.FieldQualitySeverity;
import com.dataspec.fieldquality.service.impl.FieldQualityServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldQualityServiceImplTest {

    @Test
    void report_returnsEmptySummaryForNoFields() {
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of());
        FieldQualityServiceImpl service = new FieldQualityServiceImpl(fieldService);

        var report = service.report(1L);

        assertThat(report.getSummary().getTotalFieldCount()).isZero();
        assertThat(report.getSummary().getAverageScore()).isZero();
        assertThat(report.getFields()).isEmpty();
        verify(fieldService).listByProject(1L);
    }

    @Test
    void report_scoresCompleteFieldAsGood() {
        FieldService fieldService = mock(FieldService.class);
        Field field = completeField("mobile_no", "手机号", "varchar(20)");
        field.setSensitive(true);
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        FieldQualityServiceImpl service = new FieldQualityServiceImpl(fieldService);

        var report = service.report(1L);

        assertThat(report.getSummary().getTotalFieldCount()).isEqualTo(1);
        assertThat(report.getSummary().getAverageScore()).isEqualTo(100);
        assertThat(report.getSummary().getGoodCount()).isEqualTo(1);
        assertThat(report.getFields()).hasSize(1);
        assertThat(report.getFields().getFirst().getLevel()).isEqualTo(FieldQualityLevel.GOOD);
        assertThat(report.getFields().getFirst().getIssues()).isEmpty();
    }

    @Test
    void report_reportsIncompleteMetadata() {
        FieldService fieldService = mock(FieldService.class);
        Field field = baseField("custom_flag", "自定义标记", "boolean");
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        FieldQualityServiceImpl service = new FieldQualityServiceImpl(fieldService);

        var item = service.report(1L).getFields().getFirst();

        assertThat(item.getLevel()).isEqualTo(FieldQualityLevel.POOR);
        assertThat(item.getScore()).isLessThan(65);
        assertThat(item.getIssues()).extracting("code")
                .contains("comment_missing", "aliases_missing", "example_missing", "classification_missing");
        assertThat(item.getSuggestions()).contains("补充字段注释", "补充常见别名", "补充示例值");
    }

    @Test
    void report_detectsSensitiveAndCodeSetGaps() {
        FieldService fieldService = mock(FieldService.class);
        Field phone = completeField("user_phone", "用户手机号", "varchar(20)");
        phone.setSensitive(false);
        Field status = completeField("order_status", "订单状态", "int");
        status.setCodeSetId(null);
        when(fieldService.listByProject(1L)).thenReturn(List.of(phone, status));
        FieldQualityServiceImpl service = new FieldQualityServiceImpl(fieldService);

        var report = service.report(1L);

        assertThat(report.getFields().get(0).getIssues()).extracting("code")
                .contains("sensitive_not_marked");
        assertThat(report.getFields().get(0).getIssues().getFirst().getSeverity())
                .isEqualTo(FieldQualitySeverity.ERROR);
        assertThat(report.getFields().get(1).getIssues()).extracting("code")
                .contains("code_set_missing");
        assertThat(report.getSummary().getErrorIssueCount()).isEqualTo(1);
        assertThat(report.getSummary().getWarningIssueCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void report_detectsDeprecatedWithoutReplacement() {
        FieldService fieldService = mock(FieldService.class);
        Field field = completeField("old_user_id", "旧用户ID", "bigint");
        field.setStatus("deprecated");
        field.setComment("旧用户字段");
        when(fieldService.listByProject(1L)).thenReturn(List.of(field));
        FieldQualityServiceImpl service = new FieldQualityServiceImpl(fieldService);

        var item = service.report(1L).getFields().getFirst();

        assertThat(item.getIssues()).extracting("code")
                .contains("deprecated_without_replacement");
        assertThat(item.getSuggestions()).contains("为废弃字段补充替代字段或迁移说明");
    }

    private Field completeField(String name, String displayName, String dataType) {
        Field field = baseField(name, displayName, dataType);
        field.setComment(displayName + "，用于核心业务流程");
        field.setAliases(name.replace("_", "") + "," + displayName);
        field.setCategory("business");
        field.setTags("core");
        field.setExampleValue("demo");
        field.setStatus("enabled");
        return field;
    }

    private Field baseField(String name, String displayName, String dataType) {
        Field field = new Field();
        field.setId((long) Math.abs(name.hashCode()));
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setSensitive(false);
        field.setStatus("enabled");
        return field;
    }
}
