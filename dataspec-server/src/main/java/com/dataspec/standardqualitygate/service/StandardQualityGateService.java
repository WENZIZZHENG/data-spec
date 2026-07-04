package com.dataspec.standardqualitygate.service;

import com.dataspec.standardqualitygate.model.StandardQualityGateConfig;
import com.dataspec.standardqualitygate.model.StandardQualityGateEvaluateReq;
import com.dataspec.standardqualitygate.model.StandardQualityGateResult;
import com.dataspec.standardqualitygate.model.StandardQualityGateSaveReq;

public interface StandardQualityGateService {

    StandardQualityGateConfig getConfig(Long projectId);

    StandardQualityGateConfig saveConfig(StandardQualityGateSaveReq req);

    StandardQualityGateResult evaluate(StandardQualityGateEvaluateReq req);
}
