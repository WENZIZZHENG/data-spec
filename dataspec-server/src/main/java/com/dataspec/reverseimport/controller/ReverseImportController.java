package com.dataspec.reverseimport.controller;

import com.dataspec.common.result.R;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
import com.dataspec.reverseimport.model.DatabaseConnectionReq;
import com.dataspec.reverseimport.model.DatabaseConnectionResult;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.DatabaseImportResult;
import com.dataspec.reverseimport.model.DatabaseMetadataBrowser;
import com.dataspec.reverseimport.model.DatabaseMetadataScanReq;
import com.dataspec.reverseimport.model.DatabaseMetadataScanResult;
import com.dataspec.reverseimport.model.DatabaseSchemaDump;
import com.dataspec.reverseimport.model.DatabaseSchemaDumpReq;
import com.dataspec.reverseimport.model.DatabaseTableInfo;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.DatabaseReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportService;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.security.context.ProjectAccessGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final ReverseImportSourceService reverseImportSourceService;

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

    /**
     * 只读分页扫描数据库表级 metadata，供大库按 cursor 分批浏览和恢复；不保存连接密码或写入字段库。
     */
    @PostMapping("/database/scan")
    public R<DatabaseMetadataScanResult> scanDatabaseMetadata(@Valid @RequestBody DatabaseMetadataScanReq req) {
        return R.ok(databaseReverseImportService.scan(req));
    }

    @PostMapping("/database/dump")
    public R<DatabaseSchemaDump> exportDatabaseDump(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.exportDump(req));
    }

    /**
     * 只读浏览所选数据库表的 schema metadata，返回结构摘要、候选状态和 AI 可读上下文；不写源库或标准字段库。
     */
    @PostMapping("/database/browser")
    public R<DatabaseMetadataBrowser> browseDatabaseMetadata(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.browse(req));
    }

    @PostMapping("/database/preview")
    public R<ReverseImportPreview> previewDatabase(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.preview(req));
    }

    @PostMapping("/dump/preview")
    public R<ReverseImportPreview> previewDump(@Valid @RequestBody DatabaseSchemaDumpReq req) {
        return R.ok(databaseReverseImportService.previewDump(req));
    }

    @PostMapping("/database/compare")
    public R<ReverseImportCompareResult> compareDatabase(@Valid @RequestBody DatabaseConnectionReq req) {
        return R.ok(databaseReverseImportService.compare(req));
    }

    @PostMapping("/dump/compare")
    public R<ReverseImportCompareResult> compareDump(@Valid @RequestBody DatabaseSchemaDumpReq req) {
        return R.ok(databaseReverseImportService.compareDump(req));
    }

    @PostMapping("/database/import")
    public R<DatabaseImportResult> importDatabaseCandidates(
            @Valid @RequestBody DatabaseImportReq req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return R.ok(reverseImportService.importCandidates(req, idempotencyKey));
    }

    @GetMapping("/decisions")
    public R<List<ReverseImportDecision>> listMappingDecisions(
            @RequestParam @NotNull(message = "项目ID不能为空") Long projectId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return R.ok(reverseImportSourceService.listDecisions(projectId, batchId, limit));
    }

    public record ReverseImportReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "SQL 不能为空") String sql
    ) {}
}
