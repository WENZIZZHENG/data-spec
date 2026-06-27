package com.dataspec.coverage.controller;

import com.dataspec.common.result.R;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字段标准覆盖率报告 API。
 */
@RestController
@RequestMapping("/api/coverage")
@RequiredArgsConstructor
public class FieldCoverageController {

    private final FieldCoverageService fieldCoverageService;
    private final DatabaseReverseImportService databaseReverseImportService;

    @PostMapping("/sql")
    public R<FieldCoverageReport> reportSql(@Valid @RequestBody SqlCoverageReq req) {
        return R.ok(fieldCoverageService.reportSql(req.projectId(), req.sql()));
    }

    @PostMapping("/database")
    public R<FieldCoverageReport> reportDatabase(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.coverage(req));
    }

    public record SqlCoverageReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "SQL 不能为空") String sql
    ) {
    }
}
