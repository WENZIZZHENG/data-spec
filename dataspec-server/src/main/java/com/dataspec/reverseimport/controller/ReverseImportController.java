package com.dataspec.reverseimport.controller;

import com.dataspec.common.result.R;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.DatabaseImportResult;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SQL 反向导入 API。
 */
@RestController
@RequestMapping("/api/reverse-import")
@RequiredArgsConstructor
public class ReverseImportController {

    private final ReverseImportService reverseImportService;
    private final DatabaseReverseImportService databaseReverseImportService;

    @PostMapping("/preview")
    public R<ReverseImportPreview> preview(@Valid @RequestBody ReverseImportReq req) {
        return R.ok(reverseImportService.preview(req.projectId(), req.sql()));
    }

    @PostMapping("/database/test")
    public R<DatabaseConnectionResult> testDatabaseConnection(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.testConnection(req));
    }

    @PostMapping("/database/tables")
    public R<List<DatabaseTableInfo>> listDatabaseTables(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.listTables(req));
    }

    @PostMapping("/database/preview")
    public R<ReverseImportPreview> previewDatabase(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.preview(req));
    }

    @PostMapping("/database/import")
    public R<DatabaseImportResult> importDatabaseCandidates(@Valid @RequestBody DatabaseImportReq req) {
        return R.ok(reverseImportService.importCandidates(req));
    }

    public record ReverseImportReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "SQL 不能为空") String sql
    ) {}
}
