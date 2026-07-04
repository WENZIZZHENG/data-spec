package com.dataspec.bootstrap.controller;

import com.dataspec.bootstrap.model.AiSessionBootstrap;
import com.dataspec.bootstrap.service.AiSessionBootstrapService;
import com.dataspec.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bootstrap")
@RequiredArgsConstructor
public class AiSessionBootstrapController {

    private final AiSessionBootstrapService bootstrapService;

    @GetMapping("/session")
    public R<AiSessionBootstrap> session(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String server,
            HttpServletRequest request
    ) {
        // 这里只传递 token 是否存在，避免启动包把明文凭据变成 AI 上下文的一部分。
        return R.ok(bootstrapService.getBootstrap(projectId, server, hasBearerToken(request)));
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer ") && authorization.length() > "Bearer ".length();
    }
}
