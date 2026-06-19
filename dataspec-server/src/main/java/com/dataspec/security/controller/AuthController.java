package com.dataspec.security.controller;

import com.dataspec.common.result.R;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证状态 API。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public R<AuthMe> me() {
        ApiTokenPrincipal principal = DataSpecSecurityContext.get();
        return R.ok(new AuthMe(
                principal.operatorName(),
                principal.allProjects(),
                principal.projectIds().stream().sorted().toList()));
    }

    public record AuthMe(
            String operatorName,
            boolean allProjects,
            List<Long> projectIds
    ) {
    }
}
