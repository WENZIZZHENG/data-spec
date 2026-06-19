package com.dataspec.security;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.config.SecurityProperties;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.repository.ApiTokenRepository;
import com.dataspec.security.service.impl.ApiTokenServiceImpl;
import com.dataspec.security.web.ApiTokenAuthInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiTokenAuthInterceptorTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void preHandle_allowsAuthorizedProject() {
        ApiTokenAuthInterceptor interceptor = interceptorForToken("secret-token", "1,2");
        MockHttpServletRequest request = request("secret-token", "1");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals("tester", DataSpecSecurityContext.currentOperator());
    }

    @Test
    void preHandle_rejectsUnauthorizedProject() {
        ApiTokenAuthInterceptor interceptor = interceptorForToken("secret-token", "1");
        MockHttpServletRequest request = request("secret-token", "2");

        BizException ex = assertThrows(
                BizException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        assertEquals(403, ex.getCode());
    }

    @Test
    void preHandle_rejectsMissingBearerTokenWhenEnabled() {
        SecurityProperties properties = new SecurityProperties();
        properties.setEnabled(true);
        ApiTokenAuthInterceptor interceptor = new ApiTokenAuthInterceptor(
                properties,
                new ApiTokenServiceImpl(mock(ApiTokenRepository.class)));

        BizException ex = assertThrows(
                BizException.class,
                () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));

        assertEquals(401, ex.getCode());
    }

    private ApiTokenAuthInterceptor interceptorForToken(String rawToken, String projectIds) {
        SecurityProperties properties = new SecurityProperties();
        properties.setEnabled(true);
        ApiTokenRepository repository = mock(ApiTokenRepository.class);
        ApiTokenServiceImpl service = new ApiTokenServiceImpl(repository);
        String hash = service.hashToken(rawToken);
        ApiToken token = new ApiToken();
        token.setName("test-token");
        token.setTokenHash(hash);
        token.setOperatorName("tester");
        token.setProjectIds(projectIds);
        token.setEnabled(true);
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(token));
        return new ApiTokenAuthInterceptor(properties, service);
    }

    private MockHttpServletRequest request(String rawToken, String projectId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/fields");
        request.addHeader("Authorization", "Bearer " + rawToken);
        request.addParameter("projectId", projectId);
        return request;
    }
}
