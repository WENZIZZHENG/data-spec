package com.dataspec.aicontext.controller;

import com.dataspec.aicontext.model.AiContextBudgetPlan;
import com.dataspec.aicontext.model.AiContextBudgetPlanRequest;
import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.aicontext.service.AiContextBudgetPlannerService;
import com.dataspec.aicontext.service.AiContextExportService;
import com.dataspec.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * AI 规则导出 API —— 生成供 AI 编程工具使用的规范文件
 */

@RestController
@RequestMapping("/api/ai-context")
@RequiredArgsConstructor
public class AiContextController {

    private final AiContextExportService aiContextExportService;
    private final AiContextBudgetPlannerService aiContextBudgetPlannerService;

    /** 预览 DATABASE_RULES.md */

    @GetMapping("/database-rules")
    public R<String> previewDatabaseRules(@RequestParam Long projectId,
                                          @RequestParam(required = false) String scope,
                                          @RequestParam(required = false) String query,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Integer limit,
                                          @RequestParam(required = false) String profileId,
                                          @RequestParam(required = false) String taskType,
                                          @RequestParam(required = false) Long snapshotId,
                                          @RequestParam(required = false) String snapshotVersion) {
        return R.ok(aiContextExportService.generateDatabaseRules(
                projectId,
                scopeOptions(scope, query, status, limit, profileId, taskType),
                snapshotId,
                snapshotVersion));
    }

    /** 下载 DATABASE_RULES.md */

    @GetMapping("/database-rules/download")
    public ResponseEntity<byte[]> downloadDatabaseRules(@RequestParam Long projectId,
                                                        @RequestParam(required = false) String scope,
                                                        @RequestParam(required = false) String query,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Integer limit,
                                                        @RequestParam(required = false) String profileId,
                                                        @RequestParam(required = false) String taskType,
                                                        @RequestParam(required = false) Long snapshotId,
                                                        @RequestParam(required = false) String snapshotVersion) {
        String content = aiContextExportService.generateDatabaseRules(
                projectId,
                scopeOptions(scope, query, status, limit, profileId, taskType),
                snapshotId,
                snapshotVersion);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DATABASE_RULES.md")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    /** 预览 field-catalog.json */

    @GetMapping("/field-catalog")
    public R<String> previewFieldCatalog(@RequestParam Long projectId,
                                         @RequestParam(required = false) String scope,
                                         @RequestParam(required = false) String query,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) String profileId,
                                         @RequestParam(required = false) String taskType,
                                         @RequestParam(required = false) Long snapshotId,
                                         @RequestParam(required = false) String snapshotVersion) {
        return R.ok(aiContextExportService.generateFieldCatalogJson(
                projectId,
                scopeOptions(scope, query, status, limit, profileId, taskType),
                snapshotId,
                snapshotVersion));
    }

    /** 下载 field-catalog.json */

    @GetMapping("/field-catalog/download")
    public ResponseEntity<byte[]> downloadFieldCatalog(@RequestParam Long projectId,
                                                       @RequestParam(required = false) String scope,
                                                       @RequestParam(required = false) String query,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Integer limit,
                                                       @RequestParam(required = false) String profileId,
                                                       @RequestParam(required = false) String taskType,
                                                       @RequestParam(required = false) Long snapshotId,
                                                       @RequestParam(required = false) String snapshotVersion) {
        String content = aiContextExportService.generateFieldCatalogJson(
                projectId,
                scopeOptions(scope, query, status, limit, profileId, taskType),
                snapshotId,
                snapshotVersion);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=field-catalog.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    /** 预览 rules.yaml */

    @GetMapping("/rules-yaml")
    public R<String> previewRulesYaml(@RequestParam Long projectId,
                                      @RequestParam(required = false) Long snapshotId,
                                      @RequestParam(required = false) String snapshotVersion) {
        return R.ok(aiContextExportService.generateRulesYaml(projectId, snapshotId, snapshotVersion));
    }

    /** 生成 AI 建表 Prompt */

    @PostMapping("/prompts/create-table")
    public R<String> generateCreateTablePrompt(@Valid @RequestBody CreateTablePromptReq req) {
        return R.ok(aiContextExportService.generateCreateTablePrompt(req.projectId(), req.businessDescription()));
    }

    /** 生成 SQL 修正 Prompt */

    @PostMapping("/prompts/fix-sql")
    public R<String> generateFixSqlPrompt(@Valid @RequestBody FixSqlPromptReq req) {
        return R.ok(aiContextExportService.generateFixSqlPrompt(req.projectId(), req.sql()));
    }

    /**
     * 生成 AI Context 预算计划。
     *
     * <p>该接口是导出前只读 preflight，只返回摘要、估算和建议，不创建 zip 或缓存文件。</p>
     */
    @PostMapping("/budget/plan")
    public R<AiContextBudgetPlan> planBudget(@Valid @RequestBody AiContextBudgetPlanRequest req) {
        return R.ok(aiContextBudgetPlannerService.plan(req));
    }

    /** 下载 rules.yaml */

    @GetMapping("/rules-yaml/download")
    public ResponseEntity<byte[]> downloadRulesYaml(@RequestParam Long projectId,
                                                    @RequestParam(required = false) Long snapshotId,
                                                    @RequestParam(required = false) String snapshotVersion) {
        String content = aiContextExportService.generateRulesYaml(projectId, snapshotId, snapshotVersion);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rules.yaml")
                .contentType(MediaType.parseMediaType("text/yaml"))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    /** 下载 AI Context zip 包 */

    @GetMapping("/package/download")
    public ResponseEntity<byte[]> downloadAiContextPackage(@RequestParam Long projectId,
                                                           @RequestParam(required = false) String scope,
                                                           @RequestParam(required = false) String query,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Integer limit,
                                                           @RequestParam(required = false) String profileId,
                                                           @RequestParam(required = false) String taskType,
                                                           @RequestParam(required = false) Long snapshotId,
                                                           @RequestParam(required = false) String snapshotVersion) {
        byte[] content = aiContextExportService.generateAiContextPackage(
                projectId,
                scopeOptions(scope, query, status, limit, profileId, taskType),
                snapshotId,
                snapshotVersion);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dataspec-ai-context.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(content);
    }

    public record CreateTablePromptReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            String businessDescription
    ) {}

    public record FixSqlPromptReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "SQL 不能为空") String sql
    ) {}

    private AiContextScopeOptions scopeOptions(String scope, String query, String status, Integer limit,
                                               String profileId, String taskType) {
        return new AiContextScopeOptions(scope, query, status, limit, profileId, taskType);
    }
}
