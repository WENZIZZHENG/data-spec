package com.dataspec.security.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API token 元数据响应。
 * <p>
 * 不能包含 tokenHash 或 plainToken，避免管理列表泄漏可认证凭据。
 */
public record ApiTokenInfo(
        Long id,
        String name,
        String operatorName,
        boolean allProjects,
        List<Long> projectIds,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime disabledAt,
        LocalDateTime lastUsedAt
) {
}
