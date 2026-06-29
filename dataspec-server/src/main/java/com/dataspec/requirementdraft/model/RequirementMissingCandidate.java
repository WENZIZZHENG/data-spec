package com.dataspec.requirementdraft.model;

import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;

/**
 * 需求草案中发现但字段库尚未稳定命中的候选字段。
 */
public record RequirementMissingCandidate(
        String candidateName,
        String displayName,
        String dataType,
        String comment,
        String evidence,
        int confidence,
        StandardCandidateCreateReq inboxPayload
) {
}
