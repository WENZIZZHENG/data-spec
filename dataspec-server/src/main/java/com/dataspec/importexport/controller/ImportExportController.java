package com.dataspec.importexport.controller;

import com.dataspec.common.result.R;
import com.dataspec.importexport.model.ExcelImportPreview;
import com.dataspec.importexport.model.ExcelImportResult;
import com.dataspec.importexport.service.ImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 导入导出 API
 */

@RestController
@RequestMapping("/api/import-export")
@RequiredArgsConstructor
public class ImportExportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ImportExportService importExportService;

    /** 导出项目字段为 JSON */

    @GetMapping("/fields/export")
    public ResponseEntity<byte[]> exportFields(@RequestParam Long projectId) {
        String json = importExportService.exportFields(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fields-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    /** 从 JSON 文件导入字段 */

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

    /** 下载标准字段和代码集 Excel 模板 */
    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> exportExcelTemplate() {
        byte[] bytes = importExportService.exportExcelTemplate();
        return excelResponse(bytes, "dataspec-import-template.xlsx");
    }

    /** 导出项目字段和代码集为 Excel */
    @GetMapping("/excel/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam Long projectId) {
        byte[] bytes = importExportService.exportExcel(projectId);
        return excelResponse(bytes, "dataspec-export.xlsx");
    }

    /** 预览 Excel 导入，不写数据库 */
    @PostMapping(value = "/excel/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ExcelImportPreview> previewExcel(@RequestParam Long projectId,
                                              @RequestParam("file") MultipartFile file) {
        try {
            return R.ok(importExportService.previewExcelImport(projectId, file.getBytes()));
        } catch (Exception e) {
            return R.fail("预览失败: " + e.getMessage());
        }
    }

    /** 确认导入 Excel，预览无错误才会写入 */
    @PostMapping(value = "/excel/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ExcelImportResult> importExcel(@RequestParam Long projectId,
                                            @RequestParam("file") MultipartFile file) {
        try {
            return R.ok(importExportService.importExcel(projectId, file.getBytes()));
        } catch (Exception e) {
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(XLSX_MEDIA_TYPE)
                .body(bytes);
    }
}
