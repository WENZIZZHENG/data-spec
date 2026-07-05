package com.dataspec.lint.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.FixPolicy;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.SqlCheckReplay;
import com.dataspec.lint.model.SqlLintDebugResult;
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
        LintResult result = sqlLintService.lint(req.sql(), req.projectId(), req.fixPolicy(), req.profileId(), req.taskType());
        return R.ok(result);
    }

    /**
     * 调试 SQL lint 规则执行过程。
     * <p>
     * 该接口只读执行规则，不保存 SQL 检查记录，也不创建 AI replay。
     */
    @PostMapping("/debug")
    public R<SqlLintDebugResult> debug(@Valid @RequestBody LintRequest req) {
        SqlLintDebugResult result = sqlLintService.debug(req.sql(), req.projectId(), req.fixPolicy(), req.profileId(), req.taskType());
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
        SqlCheckReplay replay = sqlCheckRecordService.buildReplay(record);
        return R.ok(new RecordDetail(record, issues, replay));
    }

    /**
     * SQL lint 请求。
     *
     * @param sql 待校验 SQL，不能为空。
     * @param projectId 项目 ID；为空时使用内置规则，不读取项目配置。
     * @param profileId AI task profile 标识；可为空，显式 fixPolicy 优先。
     * @param taskType AI 任务类型；当 profileId 为空时可用于解析默认 fixPolicy。
     * @param fixPolicy 请求级 fixedSql 策略；为空时使用 profile 或系统默认策略。
     */
    public record LintRequest(
            @NotBlank(message = "SQL 不能为空") String sql,
            Long projectId,
            String profileId,
            String taskType,
            FixPolicy fixPolicy
    ) {}

    /** 检查记录详情:记录本身 + 反序列化后的结构化问题 */
    public record RecordDetail(SqlCheckRecord record, List<LintIssue> issues, SqlCheckReplay replay) {}
}
