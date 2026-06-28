package com.dataspec.aibatch.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 同步批量 SQL lint 请求。
 */
public record AiBatchSqlLintReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        String source,
        @Valid List<AiBatchSqlLintItemReq> items
) {
}
