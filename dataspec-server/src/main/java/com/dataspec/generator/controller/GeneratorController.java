package com.dataspec.generator.controller;

import com.dataspec.common.result.R;
import com.dataspec.generator.model.DdlGenerateResult;
import com.dataspec.generator.service.DdlGeneratorService;
import com.dataspec.generator.service.HtmlDataDictionaryService;
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
    private final HtmlDataDictionaryService htmlDataDictionaryService;
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

    /**
     * 预览 HTML 数据字典
     */
    @GetMapping("/html/preview")
    public R<String> previewHtml(@RequestParam Long projectId) {
        return R.ok(htmlDataDictionaryService.generateHtml(projectId));
    }

    /**
     * 下载 HTML 数据字典
     */
    @GetMapping("/html/download")
    public ResponseEntity<byte[]> downloadHtml(@RequestParam Long projectId) {
        String content = htmlDataDictionaryService.generateHtml(projectId);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data-dictionary.html")
                .contentType(MediaType.TEXT_HTML)
                .body(bytes);
    }

    /**
     * 预览 Mermaid 关系图
     */
    @GetMapping("/erd/preview")
    public R<String> previewErd(@RequestParam Long projectId) {
        return R.ok(htmlDataDictionaryService.generateMermaid(projectId));
    }

    /**
     * 下载 Mermaid 关系图
     */
    @GetMapping("/erd/download")
    public ResponseEntity<byte[]> downloadErd(@RequestParam Long projectId) {
        String content = htmlDataDictionaryService.generateMermaid(projectId);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data-dictionary.mmd")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
}
