package com.dataspec.metric;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.metric.entity.MetricDefinition;
import com.dataspec.metric.model.MetricDefinitionReq;
import com.dataspec.metric.model.MetricDefinitionResp;
import com.dataspec.metric.repository.MetricDefinitionRepository;
import com.dataspec.metric.service.impl.MetricDefinitionServiceImpl;
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

class MetricDefinitionServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void create_normalizesMetricDefinitionAndFieldRefs() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.existsByMetricKey(1L, "order_amount", null)).thenReturn(false);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        when(fieldRepository.findById(11L)).thenReturn(Optional.of(field(11L, 1L)));
        doAnswer(invocation -> {
            MetricDefinition definition = invocation.getArgument(0);
            definition.setId(21L);
            return 1;
        }).when(repository).insert(any(MetricDefinition.class));
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        MetricDefinitionResp resp = service.create(new MetricDefinitionReq(
                1L,
                " order_amount ",
                " 订单金额 ",
                " 支付成功订单实付金额 ",
                List.of(10L, 10L),
                List.of(11L),
                " status = 'PAID' ",
                " sum(amount_cent) / 100 ",
                " day ",
                " 财务口径，以支付成功为准 ",
                " SELECT sum(amount_cent) FROM orders WHERE status = 'PAID' ",
                List.of(" metric-doc:order ", "metric-doc:order", "decision:finance"),
                " DEPRECATED "));

        assertThat(resp.id()).isEqualTo(21L);
        assertThat(resp.metricKey()).isEqualTo("order_amount");
        assertThat(resp.measureFieldIds()).containsExactly(10L);
        assertThat(resp.dimensionFieldIds()).containsExactly(11L);
        assertThat(resp.filterRule()).isEqualTo("status = 'PAID'");
        assertThat(resp.status()).isEqualTo("deprecated");
        assertThat(resp.evidenceRefs()).containsExactly("metric-doc:order", "decision:finance");

        ArgumentCaptor<MetricDefinition> captor = ArgumentCaptor.forClass(MetricDefinition.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue().getMeasureFieldsJson()).isEqualTo("[10]");
        assertThat(captor.getValue().getDimensionFieldsJson()).isEqualTo("[11]");
    }

    @Test
    void create_rejectsCrossProjectMeasureField() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.existsByMetricKey(1L, "order_amount", null)).thenReturn(false);
        when(fieldRepository.findById(99L)).thenReturn(Optional.of(field(99L, 2L)));
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(baseReq(List.of(99L), List.of())))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("measureFieldIds 字段不属于当前项目");

        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsCrossProjectDimensionField() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.existsByMetricKey(1L, "order_amount", null)).thenReturn(false);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        when(fieldRepository.findById(88L)).thenReturn(Optional.of(field(88L, 2L)));
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(baseReq(List.of(10L), List.of(88L))))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("dimensionFieldIds 字段不属于当前项目");

        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsSecretLikeOwnerNotesAndExampleSql() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.existsByMetricKey(1L, "order_amount", null)).thenReturn(false);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(new MetricDefinitionReq(
                1L,
                "order_amount",
                "订单金额",
                "支付成功订单实付金额",
                List.of(10L),
                List.of(),
                null,
                "sum(amount_cent)",
                "day",
                "Authorization: Bearer raw.jwt",
                "SELECT sum(amount_cent) FROM orders",
                List.of(),
                "enabled")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ownerNotes 包含敏感连接或凭据信息")
                .hasMessageNotContaining("raw.jwt");

        assertThatThrownBy(() -> service.create(new MetricDefinitionReq(
                1L,
                "order_amount",
                "订单金额",
                "支付成功订单实付金额",
                List.of(10L),
                List.of(),
                null,
                "sum(amount_cent)",
                "day",
                "只读说明",
                "jdbc:postgresql://localhost/app?password=secret",
                List.of(),
                "enabled")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("exampleSql 包含敏感连接或凭据信息")
                .hasMessageNotContaining("password=secret");

        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsInvalidMetricKeyBoundary() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(new MetricDefinitionReq(
                1L,
                "OrderAmount",
                "订单金额",
                "支付成功订单实付金额",
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "enabled")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("指标键必须为 2-100 位 snake_case");

        verify(repository, never()).insert(any());
    }

    @Test
    void create_rejectsSensitiveStatusWithoutEchoingSecret() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.existsByMetricKey(1L, "order_amount", null)).thenReturn(false);
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        assertThatThrownBy(() -> service.create(new MetricDefinitionReq(
                1L,
                "order_amount",
                "订单金额",
                "支付成功订单实付金额",
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "Authorization: Bearer raw-metric-status")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("status 包含敏感连接或凭据信息")
                .hasMessageNotContaining("raw-metric-status");

        verify(repository, never()).insert(any());
    }

    @Test
    void list_filtersDefinitionsByReferencedFieldId() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        MetricDefinition measureMetric = metric(1L, "order_amount", "[10]", "[11]");
        MetricDefinition dimensionMetric = metric(2L, "order_count_by_amount", "[12]", "[10]");
        MetricDefinition unrelatedMetric = metric(3L, "user_count", "[13]", "[14]");
        when(repository.findByProject(1L, null, null, 10L, null, 20))
                .thenReturn(List.of(measureMetric, dimensionMetric, unrelatedMetric));
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        List<MetricDefinitionResp> result = service.list(1L, null, null, 10L);

        assertThat(result).extracting(MetricDefinitionResp::metricKey)
                .containsExactly("order_amount", "order_count_by_amount");
    }

    @Test
    void list_forwardsMetricKeyAndClampedLimitToRepository() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        MetricDefinition metric = metric(1L, "order_amount", "[10]", "[]");
        when(repository.findByProject(1L, "订单", "enabled", null, "order_amount", 100))
                .thenReturn(List.of(metric));
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        List<MetricDefinitionResp> result = service.list(1L, "订单", "enabled", null, " order_amount ", 500);

        assertThat(result).extracting(MetricDefinitionResp::metricKey).containsExactly("order_amount");
        verify(repository).findByProject(1L, "订单", "enabled", null, "order_amount", 100);
    }

    @Test
    void listRelatedToFields_queriesAnyReferencedFieldWithBoundedLimit() {
        MetricDefinitionRepository repository = mock(MetricDefinitionRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(10L)).thenReturn(Optional.of(field(10L, 1L)));
        when(fieldRepository.findById(11L)).thenReturn(Optional.of(field(11L, 1L)));
        MetricDefinition metric = metric(1L, "order_amount", "[10]", "[11]");
        when(repository.findRelatedToFields(1L, List.of(10L, 11L), 100)).thenReturn(List.of(metric));
        MetricDefinitionServiceImpl service = new MetricDefinitionServiceImpl(repository, fieldRepository, objectMapper);

        List<MetricDefinitionResp> result = service.listRelatedToFields(1L, List.of(10L, 11L, 10L), 500);

        assertThat(result).extracting(MetricDefinitionResp::metricKey).containsExactly("order_amount");
        verify(repository).findRelatedToFields(1L, List.of(10L, 11L), 100);
    }

    private MetricDefinitionReq baseReq(List<Long> measureFieldIds, List<Long> dimensionFieldIds) {
        return new MetricDefinitionReq(
                1L,
                "order_amount",
                "订单金额",
                "支付成功订单实付金额",
                measureFieldIds,
                dimensionFieldIds,
                null,
                "sum(amount_cent)",
                "day",
                "只读说明",
                "SELECT sum(amount_cent) FROM orders",
                List.of(),
                "enabled");
    }

    private MetricDefinition metric(Long id, String metricKey, String measureJson, String dimensionJson) {
        MetricDefinition metric = new MetricDefinition();
        metric.setId(id);
        metric.setProjectId(1L);
        metric.setMetricKey(metricKey);
        metric.setDisplayName(metricKey);
        metric.setDefinition("指标定义");
        metric.setMeasureFieldsJson(measureJson);
        metric.setDimensionFieldsJson(dimensionJson);
        metric.setStatus("enabled");
        return metric;
    }

    private Field field(Long id, Long projectId) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName("field_" + id);
        return field;
    }
}
