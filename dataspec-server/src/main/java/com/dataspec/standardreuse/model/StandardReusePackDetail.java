package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准复用包详情。
 */
@Schema(description = "标准复用包详情，包含摘要和确定性 payload JSON。")
public record StandardReusePackDetail(
        @Schema(description = "复用包摘要。") StandardReusePackInfo info,
        @Schema(description = "确定性 payload JSON，不包含数据库 ID 或源库行值。") String payloadJson
) {
}
