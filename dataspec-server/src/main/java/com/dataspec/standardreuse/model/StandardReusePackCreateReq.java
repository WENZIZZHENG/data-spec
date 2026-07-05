package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建标准复用包请求。
 */
@Schema(description = "从源项目创建标准复用包的请求。")
public record StandardReusePackCreateReq(
        @Schema(description = "源项目 ID。") @NotNull(message = "源项目 ID 不能为空") Long projectId,
        @Schema(description = "项目内稳定包 key，如 shared_core。") @NotBlank(message = "复用包 key 不能为空") String packKey,
        @Schema(description = "用户可读复用包名称。") @NotBlank(message = "复用包名称不能为空") String packName,
        @Schema(description = "用户定义共享包版本。") @NotBlank(message = "复用包版本不能为空") String basePackVersion,
        @Schema(description = "复用包说明，可为空。") String description
) {
}
