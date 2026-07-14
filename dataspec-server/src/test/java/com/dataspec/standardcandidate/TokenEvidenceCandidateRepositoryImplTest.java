package com.dataspec.standardcandidate;

import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.mapper.StandardCandidateMapper;
import com.dataspec.standardcandidate.repository.impl.TokenEvidenceCandidateRepositoryImpl;
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

class TokenEvidenceCandidateRepositoryImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(StandardCandidate.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MybatisMapperBuilderAssistant(new Configuration(), StandardCandidateMapper.class.getName()),
                    StandardCandidate.class);
        }
    }

    @Test
    void findByFactKey_usesAllDedupeFieldsAndStableLimit() {
        StandardCandidateMapper mapper = mock(StandardCandidateMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        TokenEvidenceCandidateRepositoryImpl repository = new TokenEvidenceCandidateRepositoryImpl(mapper);

        repository.findByFactKey(1L, "ord_amt", "TOKEN_EVIDENCE", "field:orders.ord_amt");

        LambdaQueryWrapper<StandardCandidate> wrapper = capturedWrapper(mapper);
        String sql = wrapper.getSqlSegment();
        assertThat(wrapper.getParamNameValuePairs().values()).contains(
                1L,
                "ord_amt",
                "TOKEN_EVIDENCE",
                "field:orders.ord_amt");
        assertThat(sql).contains("ORDER BY", "LIMIT 1");
    }

    @Test
    void findActiveByName_filtersDecisionStatesAndOrdersNewestFirst() {
        StandardCandidateMapper mapper = mock(StandardCandidateMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        TokenEvidenceCandidateRepositoryImpl repository = new TokenEvidenceCandidateRepositoryImpl(mapper);

        repository.findActiveByName(1L, "ord_amt");

        LambdaQueryWrapper<StandardCandidate> wrapper = capturedWrapper(mapper);
        String sql = wrapper.getSqlSegment();
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1L, "ord_amt", "PENDING", "POSTPONED");
        assertThat(sql).contains("ORDER BY", "LIMIT 1");
    }

    @Test
    void insertIfAbsent_delegatesAtomicMapperInsert() {
        StandardCandidateMapper mapper = mock(StandardCandidateMapper.class);
        StandardCandidate candidate = new StandardCandidate();
        when(mapper.insertTokenEvidenceIfAbsent(candidate)).thenReturn(1);
        TokenEvidenceCandidateRepositoryImpl repository = new TokenEvidenceCandidateRepositoryImpl(mapper);

        assertThat(repository.insertIfAbsent(candidate)).isEqualTo(1);
        verify(mapper).insertTokenEvidenceIfAbsent(candidate);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private LambdaQueryWrapper<StandardCandidate> capturedWrapper(StandardCandidateMapper mapper) {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }
}
