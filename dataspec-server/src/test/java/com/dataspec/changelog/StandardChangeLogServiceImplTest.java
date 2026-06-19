package com.dataspec.changelog;

import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.changelog.service.impl.StandardChangeLogServiceImpl;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StandardChangeLogServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void recordChange_writesCurrentOperator() {
        StandardChangeLogRepository repository = mock(StandardChangeLogRepository.class);
        StandardChangeLogService service = new StandardChangeLogServiceImpl(repository, new ObjectMapper());
        DataSpecSecurityContext.set(new ApiTokenPrincipal("cli", "alice", false, Set.of(1L)));

        service.recordChange(1L, "field", 9L, "update", "before", "after");

        verify(repository).insert(argThat(log ->
                "alice".equals(log.getOperatorName())
                        && Long.valueOf(1L).equals(log.getProjectId())
                        && "field".equals(log.getTargetType())));
    }

    @Test
    void recordChange_defaultsToLocalOperator() {
        StandardChangeLogRepository repository = mock(StandardChangeLogRepository.class);
        StandardChangeLogService service = new StandardChangeLogServiceImpl(repository, new ObjectMapper());

        service.recordChange(1L, "rule_config", 3L, "toggle", null, "{}");

        verify(repository).insert(argThat(log -> "local".equals(log.getOperatorName())));
        verify(repository, never()).insert(argThat(log -> log.getOperatorName() == null));
    }
}
