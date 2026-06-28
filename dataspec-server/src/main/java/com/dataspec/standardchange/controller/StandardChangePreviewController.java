package com.dataspec.standardchange.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardchange.model.FieldChangePreviewReq;
import com.dataspec.standardchange.model.RuleChangePreviewReq;
import com.dataspec.standardchange.model.StandardChangePreview;
import com.dataspec.standardchange.service.StandardChangePreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准变更保存前预览 API。
 */
@RestController
@RequestMapping("/api/standard-changes/preview")
@RequiredArgsConstructor
public class StandardChangePreviewController {

    private final StandardChangePreviewService previewService;

    @PostMapping("/fields/{id}")
    public R<StandardChangePreview> previewFieldUpdate(
            @PathVariable Long id,
            @RequestBody FieldChangePreviewReq req) {
        return R.ok(previewService.previewFieldUpdate(id, req));
    }

    @PostMapping("/rules/{id}")
    public R<StandardChangePreview> previewRuleUpdate(
            @PathVariable Long id,
            @RequestBody RuleChangePreviewReq req) {
        return R.ok(previewService.previewRuleUpdate(id, req));
    }

    @PostMapping("/rules/{id}/toggle")
    public R<StandardChangePreview> previewRuleToggle(
            @PathVariable Long id,
            @RequestParam Long projectId,
            @RequestParam boolean enabled) {
        return R.ok(previewService.previewRuleToggle(id, projectId, enabled));
    }
}
