package com.dataspec.rulebaseline.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleBaselineApplyReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotBlank(message = "基线编码不能为空") String baselineKey,
        Boolean overwrite
) {
}
