package com.dataspec.fieldimpact.controller;

import com.dataspec.common.result.R;
import com.dataspec.fieldimpact.model.FieldImpactReport;
import com.dataspec.fieldimpact.service.FieldImpactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准字段影响分析 API。
 */
@RestController
@RequestMapping("/api/fields/{id}/impact")
@RequiredArgsConstructor
public class FieldImpactController {

    private final FieldImpactService fieldImpactService;

    @GetMapping
    public R<FieldImpactReport> report(@PathVariable Long id, @RequestParam Long projectId) {
        return R.ok(fieldImpactService.report(projectId, id));
    }
}
