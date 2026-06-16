package com.dataspec.aicontext.controller;

import com.dataspec.aicontext.service.AiContextExportService;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ai-context")
@RequiredArgsConstructor
public class AiContextController {

    private final AiContextExportService aiContextExportService;

    @GetMapping("/database-rules")
    public R<String> previewDatabaseRules(@RequestParam Long projectId) {
        return R.ok(aiContextExportService.generateDatabaseRules(projectId));
    }

    @GetMapping("/database-rules/download")
    public ResponseEntity<byte[]> downloadDatabaseRules(@RequestParam Long projectId) {
        String content = aiContextExportService.generateDatabaseRules(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DATABASE_RULES.md")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/field-catalog")
    public R<String> previewFieldCatalog(@RequestParam Long projectId) {
        return R.ok(aiContextExportService.generateFieldCatalogJson(projectId));
    }

    @GetMapping("/field-catalog/download")
    public ResponseEntity<byte[]> downloadFieldCatalog(@RequestParam Long projectId) {
        String content = aiContextExportService.generateFieldCatalogJson(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=field-catalog.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/rules-yaml")
    public R<String> previewRulesYaml(@RequestParam Long projectId) {
        return R.ok(aiContextExportService.generateRulesYaml(projectId));
    }

    @GetMapping("/rules-yaml/download")
    public ResponseEntity<byte[]> downloadRulesYaml(@RequestParam Long projectId) {
        String content = aiContextExportService.generateRulesYaml(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rules.yaml")
                .contentType(MediaType.parseMediaType("text/yaml"))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
