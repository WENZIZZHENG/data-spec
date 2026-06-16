package com.dataspec.rule.controller;

import com.dataspec.common.result.R;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleConfigController {

    private final RuleConfigService ruleConfigService;

    @GetMapping
    public R<List<RuleConfig>> list(@RequestParam Long projectId) {
        return R.ok(ruleConfigService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<RuleConfig> getById(@PathVariable Long id) {
        return R.ok(ruleConfigService.getById(id));
    }

    @PostMapping
    public R<RuleConfig> create(@Valid @RequestBody RuleConfigReq req) {
        RuleConfig rc = new RuleConfig();
        rc.setProjectId(req.projectId());
        rc.setRuleCode(req.ruleCode());
        rc.setRuleName(req.ruleName());
        rc.setSeverity(req.severity());
        rc.setEnabled(req.enabled());
        rc.setParamsJson(req.paramsJson());
        return R.ok(ruleConfigService.create(rc));
    }

    @PutMapping("/{id}")
    public R<RuleConfig> update(@PathVariable Long id, @Valid @RequestBody RuleConfigReq req) {
        RuleConfig rc = new RuleConfig();
        rc.setRuleName(req.ruleName());
        rc.setSeverity(req.severity());
        rc.setEnabled(req.enabled());
        rc.setParamsJson(req.paramsJson());
        return R.ok(ruleConfigService.update(id, rc));
    }

    @PatchMapping("/{id}/toggle")
    public R<Void> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        ruleConfigService.toggle(id, enabled);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        ruleConfigService.delete(id);
        return R.ok();
    }

    public record RuleConfigReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "规则编码不能为空") String ruleCode,
            @NotBlank(message = "规则名称不能为空") String ruleName,
            String severity,
            Boolean enabled,
            String paramsJson
    ) {}
}
