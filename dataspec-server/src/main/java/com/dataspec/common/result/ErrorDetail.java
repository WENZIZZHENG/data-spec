package com.dataspec.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 面向 AI/CLI/MCP 的错误诊断字段。
 *
 * <p>顶层 {@link R#getCode()} 和 {@link R#getMessage()} 保持兼容；该对象只在失败响应中提供机器可读
 * 下一步建议，便于 agent 判断是补 token、换项目、修 SQL 还是检查数据库连接。</p>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorDetail(
        @Schema(description = "稳定机器可读错误码；用于 CLI/MCP/AI 判断错误类别，不包含敏感明文。")
        String code,
        @Schema(description = "错误大类，例如 AUTH、SAFETY、VALIDATION、DATABASE、SQL 或 SERVER。")
        String category,
        @Schema(description = "true 表示修正输入、补 token、重试或稍后重试后通常可以恢复。")
        boolean retryable,
        @Schema(description = "面向用户和 AI 的下一步建议；生成前已按项目脱敏规则处理。")
        String suggestedAction,
        @Schema(description = "关联文档锚点；为空表示暂无稳定文档入口。")
        String docsRef,
        @Schema(description = "缺失的安全参数或必填输入名，例如 Idempotency-Key、dryRunToken。")
        List<String> missing,
        @Schema(description = "触发错误的高风险写入 operation 标识；用于复用幂等 key 或重新生成 dry-run 证据。")
        String operation,
        @Schema(description = "触发错误的 capability id；为空表示无法从错误中稳定识别能力。")
        String capabilityId,
        @Schema(description = "与错误直接相关的安全约束摘要；只包含布尔/短文本约束，不包含 raw secret。")
        Map<String, Object> safety,
        @Schema(description = "AI 或用户修复该错误的可执行下一步列表；不得包含 token、密码、JDBC URL 或 DSN。")
        List<String> nextActions
) {
    public ErrorDetail(String code, String category, boolean retryable, String suggestedAction, String docsRef) {
        this(code, category, retryable, suggestedAction, docsRef, List.of(), null, null, Map.of(), List.of());
    }
}
