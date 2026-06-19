package com.dataspec.security;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.security.repository.ApiTokenRepository;
import com.dataspec.security.service.impl.ApiTokenServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiTokenServiceImplTest {

    @Test
    void authenticate_acceptsEnabledTokenAndParsesProjectScope() {
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiTokenServiceImpl service = new ApiTokenServiceImpl(repository);
        String hash = service.hashToken("secret-token");
        ApiToken token = token(hash, true, "alice", "1, 2");
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        ApiTokenPrincipal principal = service.authenticate(" secret-token ");

        assertEquals("alice", principal.operatorName());
        assertFalse(principal.allProjects());
        assertTrue(principal.canAccessProject(1L));
        assertTrue(principal.canAccessProject(2L));
        assertFalse(principal.canAccessProject(3L));
    }

    @Test
    void authenticate_rejectsDisabledToken() {
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiTokenServiceImpl service = new ApiTokenServiceImpl(repository);
        String hash = service.hashToken("secret-token");
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(token(hash, false, "alice", "*")));

        BizException ex = assertThrows(BizException.class, () -> service.authenticate("secret-token"));

        assertEquals(401, ex.getCode());
    }

    @Test
    void authenticate_acceptsAllProjectsScope() {
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiTokenServiceImpl service = new ApiTokenServiceImpl(repository);
        String hash = service.hashToken("secret-token");
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(token(hash, true, "agent", "*")));

        ApiTokenPrincipal principal = service.authenticate("secret-token");

        assertTrue(principal.allProjects());
        assertTrue(principal.canAccessProject(999L));
    }

    private ApiToken token(String hash, boolean enabled, String operatorName, String projectIds) {
        ApiToken token = new ApiToken();
        token.setName("test-token");
        token.setTokenHash(hash);
        token.setOperatorName(operatorName);
        token.setProjectIds(projectIds);
        token.setEnabled(enabled);
        return token;
    }
}
