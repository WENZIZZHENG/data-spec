package com.dataspec.standardqualitygate.model;

import com.dataspec.standardhealth.model.StandardHealthCoverageInput;
import lombok.Data;

/**
 * 质量门禁评估请求。coverage/lint 摘要可选，缺失时不会伪造通过。
 */
@Data
public class StandardQualityGateEvaluateReq {

    private Long projectId;
    private StandardHealthCoverageInput coverage;
    private QualityGateLintSummary lintSummary;
}
