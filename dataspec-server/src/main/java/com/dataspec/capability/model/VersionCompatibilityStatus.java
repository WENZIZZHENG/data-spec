package com.dataspec.capability.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 客户端版本与当前服务端最小兼容要求的判断结果。
 *
 * <p>{@code UNKNOWN} 表示服务端无法可靠比较版本，第一版不会把未知版本误判为不兼容。</p>
 */
@Schema(description = "客户端版本兼容判断结果，供 AI 决定继续、升级、降级或停止。")
public record VersionCompatibilityStatus(
        @Schema(description = "兼容状态：COMPATIBLE、INCOMPATIBLE 或 UNKNOWN。")
        String status,
        @Schema(description = "调用方传入的客户端版本；缺失时为空。")
        String clientVersion,
        @Schema(description = "是否允许继续执行。UNKNOWN 状态下第一版保持非阻塞，避免误杀无法比较的客户端。")
        boolean compatible,
        @Schema(description = "状态原因，必须可读且不包含 token、密码、连接串或业务数据。")
        List<String> reasons,
        @Schema(description = "建议下一步，例如升级 CLI、运行 doctor、读取 capability catalog 或停止自动化。")
        List<String> nextActions
) {
}
