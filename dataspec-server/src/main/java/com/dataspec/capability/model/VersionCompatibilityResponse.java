package com.dataspec.capability.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DataSpec 服务端、CLI、MCP 和 AI agent 共享的版本兼容握手响应。
 *
 * <p>该响应只包含公开能力元数据和兼容建议，不读取项目业务数据、不连接源数据库、不写入 DataSpec 状态。</p>
 */
@Schema(description = "DataSpec 版本兼容握手响应；只读、可机器解析、不包含敏感明文。")
public record VersionCompatibilityResponse(
        @Schema(description = "响应类型，固定为 dataspec-version-compatibility。")
        String kind,
        @Schema(description = "握手响应 schema 版本；字段向后兼容新增时递增。")
        int schemaVersion,
        @Schema(description = "当前 DataSpec 服务端版本。")
        String serverVersion,
        @Schema(description = "当前公开 API/AI capability 契约摘要 hash，用于发现契约漂移。")
        String apiSchemaHash,
        @Schema(description = "当前服务端推荐的最小 CLI 版本。")
        String minCliVersion,
        @Schema(description = "服务端声明支持的能力摘要；只描述能力，不执行能力。")
        List<VersionSupportedCapability> supportedCapabilities,
        @Schema(description = "仍可返回但已废弃的字段说明；第一版可能为空。")
        List<VersionDeprecatedField> deprecatedFields,
        @Schema(description = "调用方客户端版本与当前服务端要求的兼容判断。")
        VersionCompatibilityStatus compatibility,
        @Schema(description = "面向用户和 AI 的升级、降级、诊断或停止建议。")
        List<String> upgradeHints,
        @Schema(description = "响应生成时间，本地服务端时间。")
        LocalDateTime generatedAt
) {
}
