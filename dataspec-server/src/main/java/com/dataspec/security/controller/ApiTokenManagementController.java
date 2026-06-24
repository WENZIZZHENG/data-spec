package com.dataspec.security.controller;

import com.dataspec.common.result.R;
import com.dataspec.security.dto.ApiTokenCreateReq;
import com.dataspec.security.dto.ApiTokenCreateResp;
import com.dataspec.security.dto.ApiTokenInfo;
import com.dataspec.security.service.ApiTokenManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API token 管理 API。
 */
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class ApiTokenManagementController {

    private final ApiTokenManagementService apiTokenManagementService;

    @GetMapping
    public R<List<ApiTokenInfo>> listTokens() {
        return R.ok(apiTokenManagementService.listTokens());
    }

    @PostMapping
    public R<ApiTokenCreateResp> createToken(@RequestBody ApiTokenCreateReq req) {
        return R.ok(apiTokenManagementService.createToken(req));
    }

    @PatchMapping("/{id}/disable")
    public R<ApiTokenInfo> disableToken(@PathVariable Long id) {
        return R.ok(apiTokenManagementService.disableToken(id));
    }
}
