package com.dataspec.ruleexemption.controller;

import com.dataspec.common.result.R;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则豁免管理 API。
 */
@RestController
@RequestMapping("/api/rule-exemptions")
@RequiredArgsConstructor
public class RuleExemptionController {

    private final RuleExemptionService ruleExemptionService;

    @GetMapping
    public R<List<RuleExemption>> list(@RequestParam Long projectId) {
        return R.ok(ruleExemptionService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<RuleExemption> getById(@PathVariable Long id) {
        return R.ok(ruleExemptionService.getById(id));
    }

    @PostMapping
    public R<RuleExemption> create(@Valid @RequestBody RuleExemptionReq req) {
        RuleExemption exemption = new RuleExemption();
        exemption.setProjectId(req.projectId());
        exemption.setRuleCode(req.ruleCode());
        exemption.setTableName(req.tableName());
        exemption.setColumnName(req.columnName());
        exemption.setReason(req.reason());
        exemption.setExpiresAt(req.expiresAt());
        return R.ok(ruleExemptionService.create(exemption));
    }

    @PatchMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        ruleExemptionService.disable(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        ruleExemptionService.delete(id);
        return R.ok();
    }

    public record RuleExemptionReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "规则编码不能为空") String ruleCode,
            String tableName,
            String columnName,
            @NotBlank(message = "豁免原因不能为空") String reason,
            LocalDateTime expiresAt
    ) {
    }
}
