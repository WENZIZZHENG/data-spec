package com.dataspec.requirementdraft.model;

import com.dataspec.explaintrace.model.ExplainTrace;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;

import java.util.List;

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
        StandardCandidateCreateReq inboxPayload,
        List<ExplainTrace> evidenceTrace
) {
    public RequirementMissingCandidate(String candidateName, String displayName, String dataType, String comment,
                                       String evidence, int confidence, StandardCandidateCreateReq inboxPayload) {
        this(candidateName, displayName, dataType, comment, evidence, confidence, inboxPayload, List.of());
    }
}
