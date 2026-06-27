package com.dataspec.aireplay.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.model.AiJobRecordListItem;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 生成与修复决策回放 API。
 */
@RestController
@RequestMapping("/api/ai-jobs")
@RequiredArgsConstructor
public class AiJobRecordController {

    private final AiJobRecordService aiJobRecordService;

    @GetMapping
    public R<PageResult<AiJobRecordListItem>> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) String jobType,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size
    ) {
        IPage<AiJobRecord> page = aiJobRecordService.listByProject(projectId, jobType, current, size);
        PageResult<AiJobRecordListItem> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(AiJobRecordListItem::from).toList());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public R<AiJobRecordDetail> detail(@PathVariable Long id) {
        return R.ok(aiJobRecordService.getDetail(id));
    }
}
