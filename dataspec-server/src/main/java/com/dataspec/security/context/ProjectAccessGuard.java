package com.dataspec.security.context;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.model.ApiTokenPrincipal;

/**
 * 项目访问边界检查。
 */
public final class ProjectAccessGuard {

    private ProjectAccessGuard() {
    }

    public static boolean canAccessProject(Long projectId) {
        return DataSpecSecurityContext.get().canAccessProject(projectId);
    }

    public static void requireProjectAccess(Long projectId) {
        ApiTokenPrincipal principal = DataSpecSecurityContext.get();
        if (!principal.canAccessProject(projectId)) {
            throw new BizException(403, "无权访问项目: " + projectId);
        }
    }

    public static void requireAllProjects(String message) {
        if (!DataSpecSecurityContext.get().allProjects()) {
            throw new BizException(403, message);
        }
    }
}
