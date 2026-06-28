package com.dataspec.starterkit.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StarterKitApplyReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotBlank(message = "Starter Kit 编码不能为空") String kitKey,
        String kitVersion
) {
}
