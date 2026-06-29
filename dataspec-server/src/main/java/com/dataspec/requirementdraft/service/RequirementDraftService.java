package com.dataspec.requirementdraft.service;

import com.dataspec.requirementdraft.model.RequirementDraftReq;
import com.dataspec.requirementdraft.model.RequirementDraftResult;

/**
 * 自然语言需求草案服务。
 */
public interface RequirementDraftService {

    RequirementDraftResult draft(RequirementDraftReq req);
}
