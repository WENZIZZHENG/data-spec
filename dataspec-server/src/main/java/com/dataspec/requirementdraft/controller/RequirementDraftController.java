package com.dataspec.requirementdraft.controller;

import com.dataspec.common.result.R;
import com.dataspec.requirementdraft.model.RequirementDraftReq;
import com.dataspec.requirementdraft.model.RequirementDraftResult;
import com.dataspec.requirementdraft.service.RequirementDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自然语言需求草案 API。
 */
@RestController
@RequestMapping("/api/requirement-drafts")
@RequiredArgsConstructor
public class RequirementDraftController {

    private final RequirementDraftService requirementDraftService;

    @PostMapping
    public R<RequirementDraftResult> draft(@Valid @RequestBody RequirementDraftReq req) {
        return R.ok(requirementDraftService.draft(req));
    }
}
