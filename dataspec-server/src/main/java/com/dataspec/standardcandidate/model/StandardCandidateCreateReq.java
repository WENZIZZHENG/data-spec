package com.dataspec.standardcandidate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 新建标准候选请求。
 */
public record StandardCandidateCreateReq(
        @NotNull Long projectId,
        @NotBlank String candidateName,
        String displayName,
        @NotBlank String dataType,
        String comment,
        @NotBlank String sourceType,
        String sourceRef,
        String evidenceJson,
        Integer confidence
) {
}
