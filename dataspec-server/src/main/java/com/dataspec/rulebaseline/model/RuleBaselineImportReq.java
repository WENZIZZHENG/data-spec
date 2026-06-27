package com.dataspec.rulebaseline.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RuleBaselineImportReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        Boolean overwrite,
        @Valid @NotNull(message = "基线包不能为空") RuleBaselinePackage baselinePackage
) {
}
