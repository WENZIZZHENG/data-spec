package com.dataspec.capability.controller;

import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityEntry;
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

    @GetMapping("/{capabilityId}")
    public R<AiCapabilityEntry> capability(
            @PathVariable String capabilityId,
            @RequestParam(required = false) Long projectId
    ) {
        return R.ok(capabilityCatalogService.getCapability(capabilityId, projectId));
    }
}
