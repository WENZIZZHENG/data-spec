package com.dataspec.fieldimpact.service;

import com.dataspec.fieldimpact.model.FieldImpactReport;

/**
 * 标准字段影响分析服务。
 */
public interface FieldImpactService {

    FieldImpactReport report(Long projectId, Long fieldId);
}
