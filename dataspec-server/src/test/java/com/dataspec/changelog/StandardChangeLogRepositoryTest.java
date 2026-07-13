package com.dataspec.changelog;

import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.mapper.StandardChangeLogMapper;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardChangeLogRepositoryTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(StandardChangeLog.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MybatisMapperBuilderAssistant(new Configuration(), StandardChangeLogMapper.class.getName()),
                    StandardChangeLog.class);
        }
    }

    @Test
    void fieldHistoryQueryIsProjectScopedAndFieldOnlyInSingleRoundTrip() {
        StandardChangeLogMapper mapper = mock(StandardChangeLogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        StandardChangeLogRepository repository = new StandardChangeLogRepository(mapper);

        repository.findFieldHistoryByProjectId(1L);

        LambdaQueryWrapper<StandardChangeLog> wrapper = capturedWrapper(mapper);
        assertThat(wrapper.getSqlSegment()).contains("projectId", "targetType", "changedAt", "id");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1L, "field");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<StandardChangeLog> capturedWrapper(StandardChangeLogMapper mapper) {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper, times(1)).selectList(captor.capture());
        return captor.getValue();
    }
}
