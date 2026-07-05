package com.dataspec.capability.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 版本兼容握手中的能力摘要。
 *
 * <p>该摘要只描述服务端声明支持的能力，不会触发能力执行，也不代表当前 token 已获得业务授权。</p>
 */
@Schema(description = "版本兼容握手中的能力摘要；只用于兼容判断和 AI 工具选择，不执行能力。")
public record VersionSupportedCapability(
        @Schema(description = "稳定能力 ID，与 capability catalog 中的 id 保持一致。")
        String id,
        @Schema(description = "能力状态，例如 AVAILABLE；调用方仍需根据具体 API/CLI/MCP 返回判断实际可用性。")
        String status,
        @Schema(description = "推荐的最小客户端版本；为空表示该能力不需要额外的客户端版本约束。")
        String minClientVersion
) {
}
