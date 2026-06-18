package com.dataspec.changelog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准变更记录 API。
 */
@RestController
@RequestMapping("/api/change-logs")
@RequiredArgsConstructor
public class StandardChangeLogController {

    private final StandardChangeLogService standardChangeLogService;

    @GetMapping
    public R<PageResult<StandardChangeLog>> page(
            @RequestParam Long projectId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        IPage<StandardChangeLog> page = standardChangeLogService.page(projectId, targetType, targetId, current, size);
        return R.ok(PageResult.of(page));
    }
}
