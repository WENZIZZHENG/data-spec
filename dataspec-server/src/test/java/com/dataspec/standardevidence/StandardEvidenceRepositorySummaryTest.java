package com.dataspec.standardevidence;

import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.mapper.StandardChangeLogMapper;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.mapper.StandardCandidateMapper;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
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

class StandardEvidenceRepositorySummaryTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(StandardCandidate.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MybatisMapperBuilderAssistant(new Configuration(), StandardCandidateMapper.class.getName()),
                    StandardCandidate.class);
        }
        if (TableInfoHelper.getTableInfo(StandardChangeLog.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MybatisMapperBuilderAssistant(new Configuration(), StandardChangeLogMapper.class.getName()),
                    StandardChangeLog.class);
        }
    }

    @Test
    void candidateSummaryQuerySelectsSafeColumnsOnly() {
        StandardCandidateMapper mapper = mock(StandardCandidateMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        StandardCandidateRepository repository = new StandardCandidateRepository(mapper);

        repository.findSummaryByProjectId(1L);

        String sqlSelect = candidateWrapper(mapper).getSqlSelect().toLowerCase();
        assertThat(sqlSelect)
                .contains("candidatename", "sourcetype", "sourceref", "decisionreason")
                .doesNotContain("evidencejson", "comment");
    }

    @Test
    void changeLogSummaryQuerySelectsSafeColumnsOnly() {
        StandardChangeLogMapper mapper = mock(StandardChangeLogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        StandardChangeLogRepository repository = new StandardChangeLogRepository(mapper);

        repository.findSummaryByTarget(1L, "field", 10L, 20);

        String sqlSelect = changeLogWrapper(mapper).getSqlSelect().toLowerCase();
        assertThat(sqlSelect)
                .contains("targettype", "targetid", "action", "operatorname", "changedat")
                .doesNotContain("beforejson", "afterjson");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<StandardCandidate> candidateWrapper(StandardCandidateMapper mapper) {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<StandardChangeLog> changeLogWrapper(StandardChangeLogMapper mapper) {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }
}
