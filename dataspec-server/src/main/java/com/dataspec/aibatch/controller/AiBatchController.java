package com.dataspec.aibatch.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.model.AiBatchDeliveryPackage;
import com.dataspec.aibatch.model.AiBatchRunDetail;
import com.dataspec.aibatch.model.AiBatchRunListItem;
import com.dataspec.aibatch.model.AiBatchSqlLintReq;
import com.dataspec.aibatch.service.AiBatchService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 批量任务交付包 API。
 */
@RestController
@RequestMapping("/api/ai-batches")
@RequiredArgsConstructor
public class AiBatchController {

    private static final ObjectMapper DOWNLOAD_OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final AiBatchService aiBatchService;

    @PostMapping("/sql-lint")
    public R<AiBatchDeliveryPackage> createSqlLintBatch(
            @Valid @RequestBody AiBatchSqlLintReq req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return R.ok(aiBatchService.createSqlLintBatch(req, idempotencyKey));
    }

    @GetMapping
    public R<PageResult<AiBatchRunListItem>> list(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size
    ) {
        IPage<AiBatchRun> page = aiBatchService.listByProject(projectId, current, size);
        PageResult<AiBatchRunListItem> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(AiBatchRunListItem::from).toList());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public R<AiBatchRunDetail> detail(@PathVariable Long id) {
        return R.ok(aiBatchService.getDetail(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<String> download(@PathVariable Long id) {
        AiBatchDeliveryPackage deliveryPackage = aiBatchService.getPackage(id);
        try {
            String json = DOWNLOAD_OBJECT_MAPPER.writeValueAsString(deliveryPackage);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dataspec-ai-batch-" + id + ".json\"")
                    .body(json);
        } catch (Exception e) {
            throw new BizException("AI 批量任务交付包下载失败: " + e.getMessage());
        }
    }
}
