package com.dataspec.aibatch.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 批量 SQL lint 的单个输入项，可来自前端粘贴或 CLI 扫描文件。
 */
public record AiBatchSqlLintItemReq(
        String itemName,
        String filePath,
        @NotBlank(message = "SQL 不能为空") String sql
) {
}
