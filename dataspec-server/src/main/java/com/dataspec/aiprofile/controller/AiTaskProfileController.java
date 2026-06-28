package com.dataspec.aiprofile.controller;

import com.dataspec.aiprofile.model.AiTaskProfileCatalog;
import com.dataspec.aiprofile.model.AiTaskProfileDetail;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-profiles")
@RequiredArgsConstructor
public class AiTaskProfileController {

    private final AiTaskProfileService aiTaskProfileService;

    @GetMapping
    public R<AiTaskProfileCatalog> listProfiles(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String profile) {
        return R.ok(aiTaskProfileService.listProfiles(projectId, profile));
    }

    @GetMapping("/{profileOrTaskType}")
    public R<AiTaskProfileDetail> getProfile(
            @PathVariable String profileOrTaskType,
            @RequestParam(required = false) Long projectId) {
        return R.ok(aiTaskProfileService.getProfile(projectId, profileOrTaskType));
    }
}
