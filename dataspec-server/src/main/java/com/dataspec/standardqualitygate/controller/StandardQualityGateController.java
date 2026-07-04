package com.dataspec.standardqualitygate.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardqualitygate.model.StandardQualityGateConfig;
import com.dataspec.standardqualitygate.model.StandardQualityGateEvaluateReq;
import com.dataspec.standardqualitygate.model.StandardQualityGateResult;
import com.dataspec.standardqualitygate.model.StandardQualityGateSaveReq;
import com.dataspec.standardqualitygate.service.StandardQualityGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目标准质量门禁 API。第一版只提供显式配置和评估，不默认阻断编辑。
 */
@RestController
@RequestMapping("/api/quality-gate")
@RequiredArgsConstructor
public class StandardQualityGateController {

    private final StandardQualityGateService standardQualityGateService;

    @GetMapping("/config")
    public R<StandardQualityGateConfig> getConfig(@RequestParam Long projectId) {
        return R.ok(standardQualityGateService.getConfig(projectId));
    }

    @PutMapping("/config")
    public R<StandardQualityGateConfig> saveConfig(@RequestBody StandardQualityGateSaveReq req) {
        return R.ok(standardQualityGateService.saveConfig(req));
    }

    @PostMapping("/evaluate")
    public R<StandardQualityGateResult> evaluate(@RequestBody StandardQualityGateEvaluateReq req) {
        return R.ok(standardQualityGateService.evaluate(req));
    }
}
