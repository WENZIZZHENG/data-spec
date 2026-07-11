package com.dataspec.fieldsemantic;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.fieldsemantic.entity.FieldSemanticRule;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleReq;
import com.dataspec.fieldsemantic.model.FieldSemanticRuleResp;
import com.dataspec.fieldsemantic.repository.FieldSemanticRuleRepository;
import com.dataspec.fieldsemantic.service.impl.FieldSemanticRuleServiceImpl;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldSemanticRuleServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void create_normalizesRuleTypeStatusAndEvidenceRefs() {
        FieldSemanticRuleRepository repository = mock(FieldSemanticRuleRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        when(fieldRepository.findById(11L)).thenReturn(Optional.of(field(11L, 1L)));
        doAnswer(invocation -> {
            FieldSemanticRule rule = invocation.getArgument(0);
            rule.setId(31L);
            return 1;
        }).when(repository).insert(any(FieldSemanticRule.class));
        FieldSemanticRuleServiceImpl service = new FieldSemanticRuleServiceImpl(repository, fieldRepository, objectMapper);

        FieldSemanticRuleResp resp = service.create(new FieldSemanticRuleReq(
                1L,
                10L,
                11L,
                " derived-from ",
                "  cent -> yuan  ",
                " sum(amount_cent) / 100 ",
                " day ",
                " 以支付明细 amount_cent 为准 ",
                " 指标统计金额 ",
                " 不要把分当元展示 ",
                List.of(" semantic-doc:amount ", "", "semantic-doc:amount", "decision:metric-boundary"),
                " DRAFT "));

        assertThat(resp.id()).isEqualTo(31L);
        assertThat(resp.ruleType()).isEqualTo("DERIVED_FROM");
        assertThat(resp.unitConversion()).isEqualTo("cent -> yuan");
        assertThat(resp.status()).isEqualTo("draft");
        assertThat(resp.evidenceRefs()).containsExactly("semantic-doc:amount", "decision:metric-boundary");

        ArgumentCaptor<FieldSemanticRule> captor = ArgumentCaptor.forClass(FieldSemanticRule.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(1L);
        assertThat(captor.getValue().getFieldId()).isEqualTo(10L);
        assertThat(captor.getValue().getSourceFieldId()).isEqualTo(11L);
        assertThat(captor.getValue().getEvidenceRefsJson())
                .isEqualTo("[\"semantic-doc:amount\",\"decision:metric-boundary\"]");
    }

    @Test
    void create_rejectsCrossProjectSourceField() {
        FieldSemanticRuleRepository repository = mock(FieldSemanticRuleRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        when(fieldRepository.findById(99L)).thenReturn(Optional.of(field(99L, 2L)));
        FieldSemanticRuleServiceImpl service = new FieldSemanticRuleServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(new FieldSemanticRuleReq(
                1L,
                10L,
                99L,
                "SOURCE_OF_TRUTH",
                null,
                null,
                null,
                "以主数据字段为准",
                null,
                null,
                List.of(),
                "enabled")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("源字段不属于当前项目");

        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsSecretLikeGuidanceTextWithoutEchoingSecret() {
        FieldSemanticRuleRepository repository = mock(FieldSemanticRuleRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        FieldSemanticRuleServiceImpl service = new FieldSemanticRuleServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(new FieldSemanticRuleReq(
                1L,
                10L,
                null,
                "AGGREGATION",
                null,
                "sum(amount_cent)",
                null,
                null,
                "统计支付金额",
                "不要记录 Authorization: Bearer raw.jwt 或 jdbc:postgresql://localhost/app",
                List.of(),
                "enabled")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("antiPatterns 包含敏感连接或凭据信息")
                .hasMessageNotContaining("raw.jwt")
                .hasMessageNotContaining("jdbc:postgresql://localhost/app");

        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsSensitiveRuleTypeAndStatusWithoutEchoingSecret() {
        FieldSemanticRuleRepository repository = mock(FieldSemanticRuleRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        FieldSemanticRuleServiceImpl service = new FieldSemanticRuleServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(new FieldSemanticRuleReq(
                1L,
                10L,
                null,
                "Authorization: Bearer raw-rule-token",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "enabled")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ruleType 包含敏感连接或凭据信息")
                .hasMessageNotContaining("raw-rule-token");

        assertThatThrownBy(() -> service.create(new FieldSemanticRuleReq(
                1L,
                10L,
                null,
                "AGGREGATION",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "token=raw-status-secret")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("status 包含敏感连接或凭据信息")
                .hasMessageNotContaining("raw-status-secret");

        verify(repository, never()).insert(any());
    }

    @Test
    void list_clampsLimitBeforeRepositoryQuery() {
        FieldSemanticRuleRepository repository = mock(FieldSemanticRuleRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldSemanticRule rule = new FieldSemanticRule();
        rule.setId(31L);
        rule.setProjectId(1L);
        rule.setFieldId(10L);
        rule.setRuleType("UNIT_CONVERSION");
        rule.setStatus("enabled");
        when(repository.findByProject(1L, null, null, null, 100)).thenReturn(List.of(rule));
        FieldSemanticRuleServiceImpl service = new FieldSemanticRuleServiceImpl(repository, fieldRepository, objectMapper);

        List<FieldSemanticRuleResp> result = service.list(1L, null, null, null, 500);

        assertThat(result).extracting(FieldSemanticRuleResp::id).containsExactly(31L);
        verify(repository).findByProject(1L, null, null, null, 100);
    }

    @Test
    void listRelatedToFields_queriesTargetAndSourceReferencesWithBoundedLimit() {
        FieldSemanticRuleRepository repository = mock(FieldSemanticRuleRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        when(fieldRepository.findById(11L)).thenReturn(Optional.of(field(11L, 1L)));
        FieldSemanticRule rule = new FieldSemanticRule();
        rule.setId(31L);
        rule.setProjectId(1L);
        rule.setFieldId(10L);
        rule.setSourceFieldId(11L);
        rule.setRuleType("UNIT_CONVERSION");
        rule.setStatus("enabled");
        when(repository.findRelatedToFields(1L, List.of(10L, 11L), 100)).thenReturn(List.of(rule));
        FieldSemanticRuleServiceImpl service = new FieldSemanticRuleServiceImpl(repository, fieldRepository, objectMapper);

        List<FieldSemanticRuleResp> result = service.listRelatedToFields(1L, List.of(10L, 11L, 10L), 500);

        assertThat(result).extracting(FieldSemanticRuleResp::id).containsExactly(31L);
        verify(repository).findRelatedToFields(1L, List.of(10L, 11L), 100);
    }

    private Field field(Long id, Long projectId) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName("field_" + id);
        return field;
    }
}
