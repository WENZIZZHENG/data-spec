package com.dataspec.aifeedback;

import com.dataspec.aifeedback.model.AiFeedbackReport;
import com.dataspec.aifeedback.service.impl.AiFeedbackServiceImpl;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.repository.RuleExemptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiFeedbackServiceImplTest {

    @Test
    void buildReport_aggregatesExistingSignalsAndSanitizesEvidence() {
        AiJobRecordRepository aiJobRecordRepository = mock(AiJobRecordRepository.class);
        SqlCheckRecordRepository sqlCheckRecordRepository = mock(SqlCheckRecordRepository.class);
        RuleExemptionRepository ruleExemptionRepository = mock(RuleExemptionRepository.class);
        FieldSourceRepository fieldSourceRepository = mock(FieldSourceRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);

        Field userId = field(10L, "user_id", "用户ID", "uid,用户编号");
        Field id = field(11L, "id", "ID", "no");
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of(userId, id));
        when(aiJobRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of(aiJob(
                100L,
                "SQL_FIX",
                "AI 修复引用 user_id",
                "{\"prompt\":\"use user_id token=ds_secret\"}",
                "{\"sql\":\"select user_id from users where jdbc:postgresql://localhost/db\"}"
        )));
        when(sqlCheckRecordRepository.findRecentByProjectId(1L, 100)).thenReturn(List.of(sqlRecord(
                200L,
                "CREATE TABLE UserOrder(uid bigint);",
                "CREATE TABLE user_order(user_id bigint);",
                """
                        [
                          {"severity":"ERROR","ruleCode":"table_naming_snake_case","ruleName":"表名 snake_case","message":"Bearer abc","tableName":"UserOrder","suggestion":"改为 user_order"},
                          {"severity":"WARNING","ruleCode":"recommended_field_name","ruleName":"推荐字段名","columnName":"uid","replacement":"user_id","suggestion":"改为 user_id"}
                        ]
                        """
        )));
        RuleExemption exemption = new RuleExemption();
        exemption.setId(300L);
        exemption.setProjectId(1L);
        exemption.setRuleCode("recommended_field_name");
        exemption.setReason("legacy password='secret'");
        when(ruleExemptionRepository.findByProjectId(1L)).thenReturn(List.of(exemption));
        FieldSource source = new FieldSource();
        source.setId(400L);
        source.setProjectId(1L);
        source.setFieldId(10L);
        source.setColumnName("uid");
        source.setTableName("users");
        when(fieldSourceRepository.findByProjectId(1L)).thenReturn(List.of(source));

        AiFeedbackServiceImpl service = new AiFeedbackServiceImpl(
                aiJobRecordRepository,
                sqlCheckRecordRepository,
                ruleExemptionRepository,
                fieldSourceRepository,
                fieldRepository,
                new ObjectMapper().findAndRegisterModules()
        );

        AiFeedbackReport report = service.buildReport(1L);

        assertThat(report.projectId()).isEqualTo(1L);
        assertThat(report.summary().aiJobCount()).isEqualTo(1);
        assertThat(report.summary().sqlCheckCount()).isEqualTo(1);
        assertThat(report.summary().fixedSqlAvailableCount()).isEqualTo(1);
        assertThat(report.summary().insufficientSuggestionHistory()).isTrue();
        assertThat(report.sampleSize().fields()).isEqualTo(2);
        assertThat(report.fieldSignals())
                .anySatisfy(signal -> {
                    assertThat(signal.title()).contains("user_id");
                    assertThat(signal.count()).isGreaterThanOrEqualTo(2);
                    assertThat(signal.targetRoute()).contains("/fields");
                });
        assertThat(report.fieldSignals())
                .noneMatch(signal -> signal.title().startsWith("id 出现"));
        assertThat(report.ruleSignals())
                .anySatisfy(signal -> {
                    assertThat(signal.title()).contains("recommended_field_name");
                    assertThat(signal.count()).isEqualTo(2);
                    assertThat(signal.targetRoute()).contains("/rule-exemptions");
                });
        assertThat(report.fixedSqlSignals())
                .anySatisfy(signal -> assertThat(signal.suggestedAction()).contains("fixedSql"));
        assertThat(report.unmanagedSignals())
                .anySatisfy(signal -> assertThat(signal.title()).contains("反向导入"));
        assertThat(report.nextActions())
                .anySatisfy(action -> assertThat(action.targetRoute()).contains("/fields"));
        assertThat(report.toString()).doesNotContain("ds_secret", "password='secret'", "Bearer abc", "jdbc:postgresql://localhost/db");
    }

    @Test
    void buildReport_rejectsMissingProjectIdBeforeRepositoryCalls() {
        AiJobRecordRepository aiJobRecordRepository = mock(AiJobRecordRepository.class);
        AiFeedbackServiceImpl service = new AiFeedbackServiceImpl(
                aiJobRecordRepository,
                mock(SqlCheckRecordRepository.class),
                mock(RuleExemptionRepository.class),
                mock(FieldSourceRepository.class),
                mock(FieldRepository.class),
                new ObjectMapper().findAndRegisterModules()
        );

        assertThrows(RuntimeException.class, () -> service.buildReport(null));
        verify(aiJobRecordRepository, org.mockito.Mockito.never()).findRecentByProjectId(org.mockito.Mockito.any(), org.mockito.Mockito.anyInt());
    }

    private Field field(Long id, String name, String displayName, String aliases) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setAliases(aliases);
        field.setStatus("enabled");
        return field;
    }

    private AiJobRecord aiJob(Long id, String type, String title, String input, String output) {
        AiJobRecord record = new AiJobRecord();
        record.setId(id);
        record.setProjectId(1L);
        record.setJobType(type);
        record.setTitle(title);
        record.setInputPayloadJson(input);
        record.setOutputPayloadJson(output);
        record.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 30));
        return record;
    }

    private SqlCheckRecord sqlRecord(Long id, String originalSql, String fixedSql, String issuesJson) {
        SqlCheckRecord record = new SqlCheckRecord();
        record.setId(id);
        record.setProjectId(1L);
        record.setOriginalSql(originalSql);
        record.setFixedSql(fixedSql);
        record.setErrorCount(1);
        record.setWarningCount(1);
        record.setSuggestionCount(0);
        record.setIssuesJson(issuesJson);
        record.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 31));
        return record;
    }
}
