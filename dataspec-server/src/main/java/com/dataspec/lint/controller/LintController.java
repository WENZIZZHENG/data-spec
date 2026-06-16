package com.dataspec.lint.controller;

import com.dataspec.common.result.R;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.model.LintResult;
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

    public record LintRequest(
            @NotBlank(message = "SQL 不能为空") String sql,
            Long projectId
    ) {}
}
