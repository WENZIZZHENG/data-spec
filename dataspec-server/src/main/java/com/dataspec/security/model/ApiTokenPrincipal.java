package com.dataspec.security.model;

import java.util.Set;

/**
 * 当前请求的 token 身份。
 *
 * @param tokenName token 名称
 * @param operatorName 操作者名称
 * @param allProjects 是否允许访问全部项目
 * @param projectIds 授权项目集合
 */
public record ApiTokenPrincipal(
        String tokenName,
        String operatorName,
        boolean allProjects,
        Set<Long> projectIds
) {

    public static ApiTokenPrincipal local() {
        return new ApiTokenPrincipal("local", "local", true, Set.of());
    }

    public boolean canAccessProject(Long projectId) {
        if (projectId == null) {
            return true;
        }
        return allProjects || projectIds.contains(projectId);
    }
}
