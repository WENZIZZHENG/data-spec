package com.dataspec.common.result;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将现有业务 code/message 收敛为稳定错误码。
 *
 * <p>第一版故意采用保守分类，避免为了机器可读错误码而批量改造所有业务异常。后续新增明确异常类型时，
 * 可以继续在这里补充更精确的映射。</p>
 */
public final class ErrorCatalog {

    private static final Pattern OPERATION_PATTERN = Pattern.compile("operation=([A-Za-z0-9:_-]+)");

    private ErrorCatalog() {
    }

    public static ErrorDetail from(int statusCode, String message) {
        String text = message == null ? "" : message;
        String lower = text.toLowerCase(Locale.ROOT);

        if (statusCode == 401 || lower.contains("authorization") || lower.contains("token")) {
            return new ErrorDetail(
                    "AUTH_TOKEN_MISSING_OR_INVALID",
                    "AUTH",
                    true,
                    "提供有效的 API Token；CLI/MCP 可设置 DATASPEC_TOKEN 或 --dataspec-token，前端可在系统设置创建 token。",
                    "README.md#安全基线"
            );
        }
        if (statusCode == 403) {
            return new ErrorDetail(
                    "PROJECT_ACCESS_DENIED",
                    "AUTH",
                    false,
                    "切换到 token 授权的项目，或使用具备该项目权限的 API Token 后重试。",
                    "README.md#安全基线"
            );
        }
        if (text.contains("Idempotency-Key") && (text.contains("缺少") || lower.contains("required"))) {
            String operation = extractOperation(text);
            return new ErrorDetail(
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "SAFETY",
                    true,
                    "为该高风险写入生成并传入 Idempotency-Key；AI 自动重试必须复用同一 projectId、operation 和 key。",
                    "README.md#ai-写入安全协议",
                    List.of("Idempotency-Key"),
                    operation,
                    null,
                    Map.of(
                            "readOnly", false,
                            "writesProject", true,
                            "requiresIdempotencyKey", true
                    ),
                    List.of("重新运行命令并传入 --idempotency-key <key> 或 Idempotency-Key header。")
            );
        }
        if (lower.contains("dry-run evidence") || lower.contains("dry run evidence")) {
            String operation = extractOperation(text);
            return new ErrorDetail(
                    "DRY_RUN_REQUIRED",
                    "SAFETY",
                    true,
                    "先执行对应 preview/compare/plan dry-run，确认结果后将返回的 dryRunToken 原样带回 apply/import。",
                    "README.md#ai-写入安全协议",
                    List.of("dryRunToken"),
                    operation,
                    null,
                    Map.of(
                            "readOnly", false,
                            "writesProject", true,
                            "requiresDryRun", true
                    ),
                    List.of("重新运行 preview/compare/plan，并在确认写入请求中传入 dryRunToken。")
            );
        }
        if (statusCode == 409 || text.contains("写入操作正在进行") || lower.contains("idempotency")) {
            return new ErrorDetail(
                    "WRITE_OPERATION_IN_PROGRESS",
                    "CONFLICT",
                    true,
                    "同一项目的高风险写入正在执行；保留相同 Idempotency-Key 稍后重试，或等待当前任务完成后再提交。",
                    "README.md#验证"
            );
        }
        if (containsProjectId(text, lower)) {
            return new ErrorDetail(
                    "PROJECT_ID_INVALID",
                    "VALIDATION",
                    true,
                    "提供有效 projectId；不确定时先运行 dataspec doctor --format json 查看当前项目状态。",
                    "README.md#验证"
            );
        }
        if (statusCode == 404 || text.contains("不存在") || lower.contains("not found")) {
            return new ErrorDetail(
                    "RESOURCE_NOT_FOUND",
                    "NOT_FOUND",
                    false,
                    "检查请求中的 ID、projectId 或资源名称是否存在；必要时刷新项目列表或运行 dataspec doctor。",
                    "README.md#验证"
            );
        }
        if (containsDatabaseFailure(text, lower)) {
            return new ErrorDetail(
                    "DATABASE_CONNECTION_FAILED",
                    "DATABASE",
                    true,
                    "检查数据库类型、host、port、database、schema、只读账号和网络连通性；不要在日志中输出密码。",
                    "README.md#sql-校验记录与反向导入"
            );
        }
        if (containsSqlFailure(text, lower)) {
            return new ErrorDetail(
                    "SQL_INPUT_INVALID",
                    "SQL",
                    true,
                    "检查 SQL/DDL 是否完整且方言受支持；可先在 SQL 校验页或 CLI lint 中复现问题。",
                    "README.md#sql-校验记录与反向导入"
            );
        }
        if (statusCode >= 500) {
            return new ErrorDetail(
                    "INTERNAL_ERROR",
                    "SERVER",
                    true,
                    "查看 DataSpec 后端日志；如果持续失败，请保留请求路径、输入摘要和错误时间用于排查。",
                    "README.md#验证"
            );
        }
        return new ErrorDetail(
                "VALIDATION_FAILED",
                "VALIDATION",
                true,
                "检查请求参数并按错误消息修正；AI agent 可先运行 doctor 或查看对应页面必填项。",
                "README.md#验证"
        );
    }

    private static String extractOperation(String text) {
        Matcher matcher = OPERATION_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean containsProjectId(String text, String lower) {
        return lower.contains("projectid")
                || text.contains("项目ID")
                || text.contains("项目 ID")
                || text.contains("当前项目")
                || text.contains("无权访问项目");
    }

    private static boolean containsDatabaseFailure(String text, String lower) {
        return text.contains("数据库")
                || text.contains("连接")
                || lower.contains("jdbc")
                || lower.contains("schema");
    }

    private static boolean containsSqlFailure(String text, String lower) {
        return lower.contains("sql")
                || text.contains("DDL")
                || text.contains("解析")
                || text.contains("表结构");
    }
}
