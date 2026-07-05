package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 应用标准复用包请求。
 */
@Schema(description = "将标准复用包 dry-run 或确认应用到目标项目的请求。")
public record StandardReusePackApplyReq(
        @Schema(description = "标准复用包 ID。") @NotNull(message = "复用包 ID 不能为空") Long packId,
        @Schema(description = "目标项目 ID。") @NotNull(message = "目标项目 ID 不能为空") Long targetProjectId,
        @Schema(description = "是否允许覆盖。第一版不执行破坏性覆盖，仅用于报告本地覆盖项。") Boolean overwrite
) {
}
