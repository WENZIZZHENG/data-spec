package com.dataspec.metric;

import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dataspec.metric.entity.MetricDefinition;
import com.dataspec.metric.mapper.MetricDefinitionMapper;
import com.dataspec.metric.repository.impl.MetricDefinitionRepositoryImpl;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricDefinitionRepositoryImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(MetricDefinition.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MybatisMapperBuilderAssistant(new Configuration(), MetricDefinitionMapper.class.getName()),
                    MetricDefinition.class);
        }
    }

    @Test
    void findByProject_appliesDefaultBoundedLimit() {
        MetricDefinitionMapper mapper = mock(MetricDefinitionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        MetricDefinitionRepositoryImpl repository = new MetricDefinitionRepositoryImpl(mapper);

        repository.findByProject(1L, null, null);

        LambdaQueryWrapper<MetricDefinition> wrapper = capturedWrapper(mapper);
        assertThat(wrapper.getSqlSegment()).contains("LIMIT 20");
    }

    @Test
    void findByProject_appliesMetricKeyFieldAndExplicitLimitFilters() {
        MetricDefinitionMapper mapper = mock(MetricDefinitionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        MetricDefinitionRepositoryImpl repository = new MetricDefinitionRepositoryImpl(mapper);

        repository.findByProject(1L, null, "enabled", 10L, "order_amount", 5);

        LambdaQueryWrapper<MetricDefinition> wrapper = capturedWrapper(mapper);
        String sql = wrapper.getSqlSegment();
        assertThat(sql).contains("metricKey", "measureFieldsJson", "dimensionFieldsJson", "LIMIT 5");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1L, "enabled", "order_amount");
    }

    @Test
    void findRelatedToFields_matchesAnyMeasureOrDimensionFieldWithLimit() {
        MetricDefinitionMapper mapper = mock(MetricDefinitionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        MetricDefinitionRepositoryImpl repository = new MetricDefinitionRepositoryImpl(mapper);

        repository.findRelatedToFields(1L, List.of(10L, 11L), 5);

        LambdaQueryWrapper<MetricDefinition> wrapper = capturedWrapper(mapper);
        String sql = wrapper.getSqlSegment();
        assertThat(sql).contains("measureFieldsJson", "dimensionFieldsJson", "LIMIT 5");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(
                        1L,
                        "%[10]%",
                        "%[10,%",
                        "%,10,%",
                        "%,10]%",
                        "%[11]%",
                        "%[11,%",
                        "%,11,%",
                        "%,11]%");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<MetricDefinition> capturedWrapper(MetricDefinitionMapper mapper) {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }
}
