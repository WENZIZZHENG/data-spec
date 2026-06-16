package com.dataspec.importexport.controller;

import com.dataspec.common.result.R;
import com.dataspec.importexport.service.ImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/import-export")
@RequiredArgsConstructor
public class ImportExportController {

    private final ImportExportService importExportService;

    @GetMapping("/fields/export")
    public ResponseEntity<byte[]> exportFields(@RequestParam Long projectId) {
        String json = importExportService.exportFields(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fields-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/fields/import")
    public R<Integer> importFields(@RequestParam Long projectId,
                                   @RequestParam("file") MultipartFile file) {
        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            int count = importExportService.importFields(projectId, json);
            return R.ok(count);
        } catch (Exception e) {
            return R.fail("导入失败: " + e.getMessage());
        }
    }
}
