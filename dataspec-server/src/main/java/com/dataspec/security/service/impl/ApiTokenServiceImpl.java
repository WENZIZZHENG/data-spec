package com.dataspec.security.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.security.repository.ApiTokenRepository;
import com.dataspec.security.service.ApiTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * API token 认证服务实现。
 */
@Service
@RequiredArgsConstructor
public class ApiTokenServiceImpl implements ApiTokenService {

    private static final String ALL_PROJECTS = "*";

    private final ApiTokenRepository apiTokenRepository;

    @Override
    public ApiTokenPrincipal authenticate(String rawToken) {
        String normalizedToken = normalizeRawToken(rawToken);
        String tokenHash = hashToken(normalizedToken);
        ApiToken token = apiTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BizException(401, "API token 无效"));
        if (!Boolean.TRUE.equals(token.getEnabled())) {
            throw new BizException(401, "API token 已停用");
        }
        Scope scope = parseProjectScope(token.getProjectIds());
        return new ApiTokenPrincipal(
                token.getName(),
                token.getOperatorName(),
                scope.allProjects(),
                scope.projectIds());
    }

    @Override
    public String hashToken(String rawToken) {
        String normalizedToken = normalizeRawToken(rawToken);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalizedToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    private String normalizeRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BizException(401, "API token 不能为空");
        }
        return rawToken.trim();
    }

    private Scope parseProjectScope(String projectIdsText) {
        if (projectIdsText == null || projectIdsText.isBlank() || ALL_PROJECTS.equals(projectIdsText.trim())) {
            return new Scope(true, Set.of());
        }
        Set<Long> projectIds = new LinkedHashSet<>();
        for (String part : projectIdsText.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            try {
                projectIds.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                throw new BizException(400, "API token 项目授权配置无效: " + projectIdsText);
            }
        }
        return new Scope(false, Set.copyOf(projectIds));
    }

    private record Scope(boolean allProjects, Set<Long> projectIds) {
    }
}
