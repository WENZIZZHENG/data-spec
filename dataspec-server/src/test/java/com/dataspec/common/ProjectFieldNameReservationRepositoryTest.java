package com.dataspec.common;

import com.dataspec.common.mapper.ProjectFieldNameReservationMapper;
import com.dataspec.common.repository.ProjectFieldNameReservationRepository;
import com.dataspec.common.repository.impl.ProjectFieldNameReservationRepositoryImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 项目字段名预留 Repository 分支契约测试。 */
class ProjectFieldNameReservationRepositoryTest {

    @Test
    void delegatesLockAndSelectsCandidateExclusionQuery() {
        ProjectFieldNameReservationMapper mapper = mock(ProjectFieldNameReservationMapper.class);
        ProjectFieldNameReservationRepository repository = new ProjectFieldNameReservationRepositoryImpl(mapper);
        when(mapper.existsActiveCandidate(1L, "ord_amt")).thenReturn(true);
        when(mapper.existsOtherActiveCandidate(1L, "ord_amt", 9L)).thenReturn(false);

        repository.lock(1L, "ord_amt");

        assertThat(repository.existsActiveCandidate(1L, "ord_amt", null)).isTrue();
        assertThat(repository.existsActiveCandidate(1L, "ord_amt", 9L)).isFalse();
        verify(mapper).lock(1L, "ord_amt");
        verify(mapper).existsActiveCandidate(1L, "ord_amt");
        verify(mapper).existsOtherActiveCandidate(1L, "ord_amt", 9L);
    }
}
