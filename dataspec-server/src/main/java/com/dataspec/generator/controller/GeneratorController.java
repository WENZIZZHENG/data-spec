package com.dataspec.generator.controller;

import com.dataspec.common.result.R;
import com.dataspec.generator.model.DdlGenerateResult;
import com.dataspec.generator.service.DdlGeneratorService;
import com.dataspec.generator.service.MarkdownGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * 生成器 API —— 数据字典生成与下载
 */

@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final MarkdownGeneratorService markdownGeneratorService;
    private final DdlGeneratorService ddlGeneratorService;

    /**
     * 预览 Markdown 数据字典
     */
    @GetMapping("/markdown/preview")
    public R<String> previewMarkdown(@RequestParam Long projectId) {
        return R.ok(markdownGeneratorService.generateDataDictionary(projectId));
    }

    /**
     * 预览表模板生成的 PostgreSQL DDL
     */
    @GetMapping("/ddl/preview")
    public R<DdlGenerateResult> previewDdl(@RequestParam Long projectId,
                                           @RequestParam Long templateId,
                                           @RequestParam String tableName) {
        return R.ok(ddlGeneratorService.generateFromTemplate(projectId, templateId, tableName));
    }

    /**
     * 下载 Markdown 数据字典
     */
    @GetMapping("/markdown/download")
    public ResponseEntity<byte[]> downloadMarkdown(@RequestParam Long projectId) {
        String content = markdownGeneratorService.generateDataDictionary(projectId);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data-dictionary.md")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(bytes);
    }
}
