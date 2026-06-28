package com.dataspec.standardcandidate.model;

import jakarta.validation.constraints.NotNull;

/**
 * 合并标准候选请求。
 */
public record StandardCandidateMergeReq(
        @NotNull Long targetFieldId,
        String reason
) {
}
