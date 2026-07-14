package com.dataspec.common;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.repository.ProjectFieldNameReservationRepository;
import com.dataspec.common.service.ProjectFieldNameReservationGuard;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ProjectFieldNameReservationGuardTest {

    @Test
    void reserveAllLocksDistinctNamesInStableOrderBeforeCheckingCandidates() {
        ProjectFieldNameReservationRepository repository = mock(ProjectFieldNameReservationRepository.class);
        ProjectFieldNameReservationGuard guard = new ProjectFieldNameReservationGuard(repository);

        guard.reserveAll(1L, List.of("z_field", "a_field", "z_field"));

        InOrder order = inOrder(repository);
        order.verify(repository).lock(1L, "a_field");
        order.verify(repository).lock(1L, "z_field");
        order.verify(repository).existsActiveCandidate(1L, "a_field", null);
        order.verify(repository).existsActiveCandidate(1L, "z_field", null);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void reserveAllRejectsAnyNameHeldByActiveCandidate() {
        ProjectFieldNameReservationRepository repository = mock(ProjectFieldNameReservationRepository.class);
        when(repository.existsActiveCandidate(1L, "z_field", null)).thenReturn(true);
        ProjectFieldNameReservationGuard guard = new ProjectFieldNameReservationGuard(repository);

        assertThatThrownBy(() -> guard.reserveAll(1L, List.of("z_field", "a_field")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("z_field");
    }
}
