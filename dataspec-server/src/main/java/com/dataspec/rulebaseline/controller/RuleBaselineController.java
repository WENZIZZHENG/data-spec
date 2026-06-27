package com.dataspec.rulebaseline.controller;

import com.dataspec.common.result.R;
import com.dataspec.rulebaseline.model.RuleBaselineApplyReq;
import com.dataspec.rulebaseline.model.RuleBaselineApplyResult;
import com.dataspec.rulebaseline.model.RuleBaselineImportReq;
import com.dataspec.rulebaseline.model.RuleBaselineInfo;
import com.dataspec.rulebaseline.model.RuleBaselinePackage;
import com.dataspec.rulebaseline.model.RuleBaselineTemplate;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rule-baselines")
@RequiredArgsConstructor
public class RuleBaselineController {

    private final RuleBaselineService ruleBaselineService;

    @GetMapping("/templates")
    public R<List<RuleBaselineTemplate>> listTemplates() {
        return R.ok(ruleBaselineService.listTemplates());
    }

    @GetMapping("/current")
    public R<RuleBaselineInfo> current(@RequestParam Long projectId) {
        return R.ok(ruleBaselineService.currentBaseline(projectId));
    }

    @PostMapping("/apply")
    public R<RuleBaselineApplyResult> apply(@Valid @RequestBody RuleBaselineApplyReq req) {
        return R.ok(ruleBaselineService.applyBuiltInBaseline(
                req.projectId(),
                req.baselineKey(),
                Boolean.TRUE.equals(req.overwrite())));
    }

    @GetMapping("/export")
    public R<RuleBaselinePackage> export(@RequestParam Long projectId) {
        return R.ok(ruleBaselineService.exportBaseline(projectId));
    }

    @PostMapping("/import")
    public R<RuleBaselineApplyResult> importBaseline(@Valid @RequestBody RuleBaselineImportReq req) {
        return R.ok(ruleBaselineService.importBaseline(
                req.projectId(),
                req.baselinePackage(),
                Boolean.TRUE.equals(req.overwrite())));
    }
}
