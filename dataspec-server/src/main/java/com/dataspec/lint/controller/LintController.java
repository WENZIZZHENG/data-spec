package com.dataspec.lint.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.service.SqlCheckRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SQL 校验 API
 */

@RestController
@RequestMapping("/api/lint")
@RequiredArgsConstructor
public class LintController {

    private final SqlLintService sqlLintService;
    private final SqlCheckRecordService sqlCheckRecordService;

    /**
     * 校验 SQL
     */
    @PostMapping
    public R<LintResult> lint(@Valid @RequestBody LintRequest req) {
        LintResult result = sqlLintService.lint(req.sql(), req.projectId());
        return R.ok(result);
    }

    /**
     * 获取所有可用规则
     */
    @GetMapping("/rules")
    public R<List<Map<String, String>>> listRules() {
        return R.ok(sqlLintService.listAvailableRules());
    }

    /**
     * 分页查询 SQL 检查记录
     */
    @GetMapping("/records")
    public R<PageResult<SqlCheckRecord>> listRecords(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        IPage<SqlCheckRecord> page = sqlCheckRecordService.listByProject(projectId, current, size);
        return R.ok(PageResult.of(page));
    }

    /**
     * 查询检查记录详情(含反序列化的结构化问题)
     */
    @GetMapping("/records/{id}")
    public R<RecordDetail> getRecord(@PathVariable Long id) {
        SqlCheckRecord record = sqlCheckRecordService.getById(id);
        List<LintIssue> issues = sqlCheckRecordService.parseIssues(record);
        return R.ok(new RecordDetail(record, issues));
    }

    public record LintRequest(
            @NotBlank(message = "SQL 不能为空") String sql,
            Long projectId
    ) {}

    /** 检查记录详情:记录本身 + 反序列化后的结构化问题 */
    public record RecordDetail(SqlCheckRecord record, List<LintIssue> issues) {}
}
