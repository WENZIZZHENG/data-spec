package com.dataspec.security.web;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.config.SecurityProperties;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.security.service.ApiTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 轻量 API token 拦截器。
 */
@Component
@RequiredArgsConstructor
public class ApiTokenAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityProperties securityProperties;
    private final ApiTokenService apiTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        DataSpecSecurityContext.clear();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!securityProperties.isEnabled()) {
            DataSpecSecurityContext.set(ApiTokenPrincipal.local());
            return true;
        }

        ApiTokenPrincipal principal = apiTokenService.authenticate(extractBearerToken(request));
        DataSpecSecurityContext.set(principal);
        String projectIdParam = request.getParameter("projectId");
        if (projectIdParam != null && !projectIdParam.isBlank()) {
            ProjectAccessGuard.requireProjectAccess(parseProjectId(projectIdParam));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        DataSpecSecurityContext.clear();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BizException(401, "缺少 Authorization Bearer token");
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    private Long parseProjectId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BizException(400, "projectId 参数无效: " + value);
        }
    }
}
