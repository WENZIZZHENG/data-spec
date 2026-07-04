package com.dataspec.standardusageexample;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.mapper.StandardUsageExampleMapper;
import com.dataspec.standardusageexample.repository.StandardUsageExampleRepository;
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

class StandardUsageExampleRepositoryTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(StandardUsageExample.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MybatisMapperBuilderAssistant(new Configuration(), StandardUsageExampleMapper.class.getName()),
                    StandardUsageExample.class);
        }
    }

    @Test
    void findForAiContext_fullExportDoesNotApplyFieldScopeFilter() {
        StandardUsageExampleMapper mapper = mock(StandardUsageExampleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        StandardUsageExampleRepository repository = new StandardUsageExampleRepository(mapper);

        repository.findForAiContext(1L, List.of(), null, 8);

        LambdaQueryWrapper<StandardUsageExample> wrapper = capturedWrapper(mapper);
        String sql = wrapper.getSqlSegment();
        assertThat(sql).contains("projectId");
        assertThat(sql).contains("status");
        assertThat(sql).doesNotContain("fieldId");
        assertThat(sql).doesNotContain("scope");
        assertThat(wrapper.getParamNameValuePairs().values()).containsExactlyInAnyOrder(1L, "enabled");
    }

    @Test
    void findForAiContext_scopedFieldRequiresMatchedFieldOrQueryMatchedNonFieldExample() {
        StandardUsageExampleMapper mapper = mock(StandardUsageExampleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        StandardUsageExampleRepository repository = new StandardUsageExampleRepository(mapper);

        repository.findForAiContext(1L, List.of(10L), "手机号", 8);

        LambdaQueryWrapper<StandardUsageExample> wrapper = capturedWrapper(mapper);
        String sql = wrapper.getSqlSegment();
        assertThat(sql).contains("scope", "fieldId");
        assertThat(wrapper.getParamNameValuePairs().values()).contains("FIELD", "GENERAL", "RULE", "TEMPLATE", "%手机号%");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<StandardUsageExample> capturedWrapper(StandardUsageExampleMapper mapper) {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }
}
