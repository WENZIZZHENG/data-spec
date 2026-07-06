package com.dataspec.fieldprovenance.controller;

import com.dataspec.common.result.R;
import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceReport;
import com.dataspec.fieldprovenance.service.FieldProvenanceConfidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字段来源可信度与 AI 置信度只读 API。
 */
@RestController
@RequestMapping("/api/fields/provenance-confidence")
@RequiredArgsConstructor
public class FieldProvenanceConfidenceController {

    private final FieldProvenanceConfidenceService fieldProvenanceConfidenceService;

    /**
     * 查询项目字段来源可信度摘要。
     *
     * @param projectId 项目 ID，服务层会校验访问边界。
     * @return 字段来源可信度报告。
     */
    @GetMapping
    public R<FieldProvenanceConfidenceReport> report(@RequestParam Long projectId) {
        return R.ok(fieldProvenanceConfidenceService.report(projectId));
    }
}
