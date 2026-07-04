package com.dataspec.standardusageexample.controller;

import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.model.StandardUsageExampleSaveReq;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准字段/规则/模板使用示例 API，供前端维护和 AI Context 裁剪导出复用。
 */
@RestController
@RequestMapping("/api/usage-examples")
@RequiredArgsConstructor
public class StandardUsageExampleController {

    private final StandardUsageExampleService standardUsageExampleService;

    @GetMapping
    public R<PageResult<StandardUsageExample>> page(
            @RequestParam Long projectId,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String exampleType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(standardUsageExampleService.page(projectId, scope, exampleType, status, query, current, size));
    }

    @PostMapping
    public R<StandardUsageExample> create(@RequestBody StandardUsageExampleSaveReq req) {
        return R.ok(standardUsageExampleService.create(req));
    }

    @PutMapping("/{id}")
    public R<StandardUsageExample> update(@PathVariable Long id, @RequestBody StandardUsageExampleSaveReq req) {
        return R.ok(standardUsageExampleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestParam Long projectId, @PathVariable Long id) {
        standardUsageExampleService.delete(projectId, id);
        return R.ok();
    }
}
