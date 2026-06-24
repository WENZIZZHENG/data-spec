package com.dataspec.security.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.security.dto.ApiTokenCreateReq;
import com.dataspec.security.dto.ApiTokenCreateResp;
import com.dataspec.security.dto.ApiTokenInfo;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.repository.ApiTokenRepository;
import com.dataspec.security.service.ApiTokenManagementService;
import com.dataspec.security.service.ApiTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * API token 管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ApiTokenManagementServiceImpl implements ApiTokenManagementService {

    private static final String ALL_PROJECTS = "*";
    private static final String TOKEN_PREFIX = "ds_";
    private static final int TOKEN_RANDOM_BYTES = 32;
    private static final int TOKEN_TEXT_MAX_LENGTH = 100;

    private final ApiTokenRepository apiTokenRepository;
    private final ApiTokenService apiTokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public List<ApiTokenInfo> listTokens() {
        requireTokenAdmin();
        return apiTokenRepository.findAllActiveRows().stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public ApiTokenCreateResp createToken(ApiTokenCreateReq req) {
        requireTokenAdmin();
        if (req == null) {
            throw new BizException(400, "创建 token 请求不能为空");
        }
        String name = requiredText(req.name(), "token 名称不能为空");
        String operatorName = requiredText(req.operatorName(), "操作者名称不能为空");
        String projectIds = serializeProjectScope(req);
        String plainToken = generatePlainToken();

        ApiToken token = new ApiToken();
        token.setName(name);
        token.setOperatorName(operatorName);
        token.setProjectIds(projectIds);
        token.setTokenHash(apiTokenService.hashToken(plainToken));
        token.setEnabled(true);
        apiTokenRepository.save(token);

        return new ApiTokenCreateResp(plainToken, toInfo(token));
    }

    @Override
    public ApiTokenInfo disableToken(Long id) {
        requireTokenAdmin();
        if (id == null) {
            throw new BizException(400, "token ID 不能为空");
        }
        ApiToken token = apiTokenRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "API token 不存在"));
        token.setEnabled(false);
        token.setDisabledAt(LocalDateTime.now());
        apiTokenRepository.update(token);
        return toInfo(token);
    }

    private void requireTokenAdmin() {
        ProjectAccessGuard.requireAllProjects("管理 API token 需要全项目 API token");
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > TOKEN_TEXT_MAX_LENGTH) {
            throw new BizException(400, "token 名称和操作者名称不能超过 100 个字符");
        }
        return trimmed;
    }

    private String serializeProjectScope(ApiTokenCreateReq req) {
        if (Boolean.TRUE.equals(req.allProjects())) {
            return ALL_PROJECTS;
        }
        if (req.projectIds() == null || req.projectIds().isEmpty()) {
            throw new BizException(400, "非全项目 token 必须选择授权项目");
        }
        LinkedHashSet<Long> projectIds = new LinkedHashSet<>();
        for (Long projectId : req.projectIds()) {
            if (projectId == null || projectId <= 0) {
                throw new BizException(400, "授权项目 ID 无效");
            }
            projectIds.add(projectId);
        }
        return String.join(",", projectIds.stream().map(String::valueOf).toList());
    }

    private String generatePlainToken() {
        byte[] bytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return TOKEN_PREFIX + HexFormat.of().formatHex(bytes);
    }

    private ApiTokenInfo toInfo(ApiToken token) {
        Scope scope = parseProjectScope(token.getProjectIds());
        return new ApiTokenInfo(
                token.getId(),
                token.getName(),
                token.getOperatorName(),
                scope.allProjects(),
                scope.projectIds(),
                token.getEnabled(),
                token.getCreatedAt(),
                token.getUpdatedAt(),
                token.getDisabledAt(),
                token.getLastUsedAt());
    }

    private Scope parseProjectScope(String projectIdsText) {
        if (projectIdsText == null || projectIdsText.isBlank() || ALL_PROJECTS.equals(projectIdsText.trim())) {
            return new Scope(true, List.of());
        }
        List<Long> projectIds = new ArrayList<>();
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
        return new Scope(false, List.copyOf(projectIds));
    }

    private record Scope(boolean allProjects, List<Long> projectIds) {
    }
}
