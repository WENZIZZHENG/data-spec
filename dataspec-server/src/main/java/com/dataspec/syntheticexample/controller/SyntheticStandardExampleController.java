package com.dataspec.syntheticexample.controller;

import com.dataspec.common.result.R;
import com.dataspec.syntheticexample.model.SyntheticStandardExamplePackage;
import com.dataspec.syntheticexample.service.SyntheticStandardExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合成标准样例 API，提供只读、确定性的 SQL/DDL/Prompt 场景素材。
 */
@RestController
@RequestMapping("/api/synthetic-examples")
@RequiredArgsConstructor
public class SyntheticStandardExampleController {

    private final SyntheticStandardExampleService syntheticStandardExampleService;

    /**
     * 生成指定业务场景的合成标准样例包，不持久化生成结果。
     */
    @GetMapping("/generate")
    public R<SyntheticStandardExamplePackage> generate(
            @RequestParam Long projectId,
            @RequestParam String scenario,
            @RequestParam(required = false) Integer maxCases) {
        return R.ok(syntheticStandardExampleService.generate(projectId, scenario, maxCases));
    }
}
