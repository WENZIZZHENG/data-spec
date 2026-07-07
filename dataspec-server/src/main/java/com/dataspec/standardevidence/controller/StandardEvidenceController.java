package com.dataspec.standardevidence.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardevidence.model.StandardEvidenceReport;
import com.dataspec.standardevidence.service.StandardEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准对象跨来源证据只读 API。
 */
@RestController
@RequestMapping("/api/standard-evidence")
@RequiredArgsConstructor
public class StandardEvidenceController {

    private final StandardEvidenceService standardEvidenceService;

    /**
     * 查询单个标准对象的跨来源证据视图。
     *
     * @param projectId 项目 ID，服务层会校验访问边界。
     * @param subjectType 目标对象类型，第一版仅支持 FIELD。
     * @param subjectId 目标对象 ID，FIELD 时为标准字段 ID。
     * @return 脱敏后的跨来源证据视图。
     */
    @GetMapping
    public R<StandardEvidenceReport> report(
            @RequestParam Long projectId,
            @RequestParam String subjectType,
            @RequestParam Long subjectId
    ) {
        return R.ok(standardEvidenceService.report(projectId, subjectType, subjectId));
    }
}
