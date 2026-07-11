package com.dataspec.fieldsemantic;

import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dataspec.fieldsemantic.entity.FieldSemanticRule;
import com.dataspec.fieldsemantic.mapper.FieldSemanticRuleMapper;
import com.dataspec.fieldsemantic.repository.impl.FieldSemanticRuleRepositoryImpl;
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

class FieldSemanticRuleRepositoryImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(FieldSemanticRule.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MybatisMapperBuilderAssistant(new Configuration(), FieldSemanticRuleMapper.class.getName()),
                    FieldSemanticRule.class);
        }
    }

    @Test
    void findByProject_appliesDefaultBoundedLimit() {
        FieldSemanticRuleMapper mapper = mock(FieldSemanticRuleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        FieldSemanticRuleRepositoryImpl repository = new FieldSemanticRuleRepositoryImpl(mapper);

        repository.findByProject(1L, null, null, null);

        LambdaQueryWrapper<FieldSemanticRule> wrapper = capturedWrapper(mapper);
        assertThat(wrapper.getSqlSegment()).contains("LIMIT 20");
    }

    @Test
    void findRelatedToFields_matchesTargetAndSourceFieldsWithLimit() {
        FieldSemanticRuleMapper mapper = mock(FieldSemanticRuleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        FieldSemanticRuleRepositoryImpl repository = new FieldSemanticRuleRepositoryImpl(mapper);

        repository.findRelatedToFields(1L, List.of(10L, 11L), 5);

        LambdaQueryWrapper<FieldSemanticRule> wrapper = capturedWrapper(mapper);
        String sql = wrapper.getSqlSegment();
        assertThat(sql).contains("fieldId", "sourceFieldId", "LIMIT 5");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1L, 10L, 11L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<FieldSemanticRule> capturedWrapper(FieldSemanticRuleMapper mapper) {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }
}
