package com.dataspec.coverage.controller;

import com.dataspec.common.result.R;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.service.FieldCoverageService;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseMetadataScanFailureSummary;
import com.dataspec.reverseimport.model.DatabaseMetadataScanPartialResult;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @PostMapping("/dump")
    public R<FieldCoverageReport> reportDump(@Valid @RequestBody DatabaseSchemaDumpReq req) {
        return R.ok(databaseReverseImportService.coverageDump(req));
    }

    @PostMapping("/scan-partial")
    public R<FieldCoverageReport> reportScanPartial(@Valid @RequestBody ScanPartialCoverageReq req) {
        return R.ok(fieldCoverageService.reportScanPartial(
                req.projectId(),
                req.partialResult(),
                req.failureSummary(),
                req.scanStatus()));
    }

    public record SqlCoverageReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "SQL 不能为空") String sql
    ) {
    }

    @Schema(description = "基于 metadata scan job schema-only partial result 生成字段覆盖率的请求；不包含数据库密码或连接串。")
    public record ScanPartialCoverageReq(
            @NotNull(message = "项目ID不能为空")
            @Schema(description = "DataSpec 项目 ID。")
            Long projectId,
            @Valid
            @NotNull(message = "采集作业部分结果不能为空")
            @Schema(description = "metadata scan job 返回的 schema-only partialResult；覆盖率只统计 successfulTables。")
            DatabaseMetadataScanPartialResult partialResult,
            @Valid
            @Schema(description = "metadata scan job 返回的失败摘要；用于标记 failed 表边界和安全下一步。")
            DatabaseMetadataScanFailureSummary failureSummary,
            @Schema(description = "metadata scan job 状态，如 PARTIAL/CANCELLED/FAILED/COMPLETED，用于标记报告输入完整性。")
            String scanStatus
    ) {
    }
}
