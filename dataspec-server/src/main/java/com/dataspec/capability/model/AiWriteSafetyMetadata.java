package com.dataspec.capability.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AI 写入安全协议元数据。
 *
 * <p>该对象只描述调用前的安全约束，不执行操作、不授予权限，也不携带 raw secret。AI、CLI、MCP 和前端
 * 应优先读取这些布尔字段，再决定是否需要 dry-run、幂等 key、人工确认或证据导出。</p>
 */
@Schema(description = "AI/CLI/MCP/前端共享的写入安全元数据；只描述约束，不包含敏感明文。")
public record AiWriteSafetyMetadata(
        @Schema(description = "该能力是否只读；true 表示不写 DataSpec 状态，也不写外部系统。")
        boolean readOnly,
        @Schema(description = "该能力是否会写入当前 DataSpec project 范围内的记录、标准资产或可见结果。")
        boolean writesProject,
        @Schema(description = "确认 apply 前是否必须先产生 dry-run、preview、compare 或 plan 摘要。")
        boolean requiresDryRun,
        @Schema(description = "是否存在明确的回滚、跳过、恢复、证据包或可复核操作路径。")
        boolean supportsUndo,
        @Schema(description = "写入调用是否必须携带 Idempotency-Key 或等价幂等参数。")
        boolean requiresIdempotencyKey,
        @Schema(description = "可能包含敏感内容的输入参数名或类别；只列名称，不返回 raw value。")
        List<String> sensitiveInputs,
        @Schema(description = "AI 在执行或恢复该能力前后应遵循的安全下一步。")
        List<String> nextActions
) {
}
