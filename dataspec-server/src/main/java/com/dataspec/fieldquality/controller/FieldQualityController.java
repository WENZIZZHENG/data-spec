package com.dataspec.fieldquality.controller;

import com.dataspec.common.result.R;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.service.FieldQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准字段质量评分 API。
 */
@RestController
@RequestMapping("/api/fields/quality")
@RequiredArgsConstructor
public class FieldQualityController {

    private final FieldQualityService fieldQualityService;

    @GetMapping
    public R<FieldQualityReport> report(@RequestParam Long projectId) {
        return R.ok(fieldQualityService.report(projectId));
    }
}
