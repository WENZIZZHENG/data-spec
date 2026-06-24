package com.dataspec.security;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.dto.ApiTokenCreateReq;
import com.dataspec.security.dto.ApiTokenCreateResp;
import com.dataspec.security.dto.ApiTokenInfo;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.security.repository.ApiTokenRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.service.impl.ApiTokenManagementServiceImpl;
import com.dataspec.security.service.impl.ApiTokenServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiTokenManagementServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void createToken_returnsPlainTokenOnceAndStoresOnlyHash() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiTokenServiceImpl authService = new ApiTokenServiceImpl(repository);
        ApiTokenManagementServiceImpl service = new ApiTokenManagementServiceImpl(repository, authService);

        ApiTokenCreateResp response = service.createToken(new ApiTokenCreateReq(
                "cli-main",
                "alice",
                false,
                List.of(1L, 2L)));

        ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);
        verify(repository).save(captor.capture());
        ApiToken saved = captor.getValue();
        assertTrue(response.plainToken().startsWith("ds_"));
        assertEquals(67, response.plainToken().length());
        assertEquals(authService.hashToken(response.plainToken()), saved.getTokenHash());
        assertNotEquals(response.plainToken(), saved.getTokenHash());
        assertEquals("cli-main", saved.getName());
        assertEquals("alice", saved.getOperatorName());
        assertEquals("1,2", saved.getProjectIds());
        assertTrue(saved.getEnabled());
        assertEquals("cli-main", response.token().name());
        assertFalse(response.token().allProjects());
        assertEquals(List.of(1L, 2L), response.token().projectIds());
    }

    @Test
    void createToken_rejectsEmptyRequestBody() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiTokenManagementServiceImpl service = new ApiTokenManagementServiceImpl(repository, new ApiTokenServiceImpl(repository));

        BizException ex = assertThrows(BizException.class, () -> service.createToken(null));

        assertEquals(400, ex.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void listTokens_requiresAllProjectPrincipalAndHidesHash() {
        DataSpecSecurityContext.set(new ApiTokenPrincipal("scoped", "bob", false, Set.of(1L)));
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiTokenServiceImpl authService = new ApiTokenServiceImpl(repository);
        ApiTokenManagementServiceImpl service = new ApiTokenManagementServiceImpl(repository, authService);

        BizException ex = assertThrows(BizException.class, service::listTokens);

        assertEquals(403, ex.getCode());
        verify(repository, never()).findAllActiveRows();
    }

    @Test
    void listTokens_returnsMetadataWithoutHashOrPlainToken() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiToken token = token(9L, "agent", "carol", "*", true);
        token.setTokenHash("hash-should-not-leak");
        token.setLastUsedAt(LocalDateTime.parse("2026-06-20T10:15:30"));
        when(repository.findAllActiveRows()).thenReturn(List.of(token));
        ApiTokenManagementServiceImpl service = new ApiTokenManagementServiceImpl(repository, new ApiTokenServiceImpl(repository));

        ApiTokenInfo info = service.listTokens().get(0);

        assertEquals(9L, info.id());
        assertEquals("agent", info.name());
        assertEquals("carol", info.operatorName());
        assertTrue(info.allProjects());
        assertEquals(List.of(), info.projectIds());
        assertTrue(info.enabled());
        assertEquals(LocalDateTime.parse("2026-06-20T10:15:30"), info.lastUsedAt());
    }

    @Test
    void disableToken_marksTokenDisabledAndKeepsMetadata() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiToken token = token(7L, "mcp", "dave", "3", true);
        when(repository.findById(7L)).thenReturn(Optional.of(token));
        ApiTokenManagementServiceImpl service = new ApiTokenManagementServiceImpl(repository, new ApiTokenServiceImpl(repository));

        ApiTokenInfo info = service.disableToken(7L);

        assertFalse(token.getEnabled());
        assertNotNull(token.getDisabledAt());
        assertFalse(info.enabled());
        assertEquals(List.of(3L), info.projectIds());
        verify(repository).update(token);
    }

    private ApiToken token(Long id, String name, String operatorName, String projectIds, boolean enabled) {
        ApiToken token = new ApiToken();
        token.setId(id);
        token.setName(name);
        token.setTokenHash("hash");
        token.setOperatorName(operatorName);
        token.setProjectIds(projectIds);
        token.setEnabled(enabled);
        return token;
    }
}
