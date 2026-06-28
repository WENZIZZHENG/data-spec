package com.dataspec.standardcandidate.service;

import com.dataspec.common.result.PageResult;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.standardcandidate.model.StandardCandidateDecisionReq;
import com.dataspec.standardcandidate.model.StandardCandidateMergeReq;

public interface StandardCandidateService {

    PageResult<StandardCandidate> page(Long projectId, String status, String sourceType, String keyword, int current, int size);

    StandardCandidate create(StandardCandidateCreateReq req);

    StandardCandidate accept(Long id, StandardCandidateDecisionReq req);

    StandardCandidate merge(Long id, StandardCandidateMergeReq req);

    StandardCandidate ignore(Long id, StandardCandidateDecisionReq req);

    StandardCandidate postpone(Long id, StandardCandidateDecisionReq req);
}
