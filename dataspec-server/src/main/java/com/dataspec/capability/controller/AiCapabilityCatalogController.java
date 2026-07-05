package com.dataspec.capability.controller;

import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityEntry;
import com.dataspec.capability.model.VersionCompatibilityResponse;
import com.dataspec.capability.service.AiCapabilityCatalogService;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/capabilities")
@RequiredArgsConstructor
public class AiCapabilityCatalogController {

    private final AiCapabilityCatalogService capabilityCatalogService;

    @GetMapping
    public R<AiCapabilityCatalog> catalog(@RequestParam(required = false) Long projectId) {
        return R.ok(capabilityCatalogService.getCatalog(projectId));
    }

    /**
     * 返回服务端与 CLI/MCP/AI 客户端的只读版本兼容握手。
     *
     * <p>该接口不读取业务项目数据、不连接外部数据库、不写入 DataSpec 状态，仅用于启动前诊断。</p>
     */
    @GetMapping("/version")
    public R<VersionCompatibilityResponse> versionCompatibility(
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String clientVersion
    ) {
        return R.ok(capabilityCatalogService.getVersionCompatibility(client, clientVersion));
    }

    @GetMapping("/{capabilityId}")
    public R<AiCapabilityEntry> capability(
            @PathVariable String capabilityId,
            @RequestParam(required = false) Long projectId
    ) {
        return R.ok(capabilityCatalogService.getCapability(capabilityId, projectId));
    }
}
