package com.dataspec.fieldquality.service;

import com.dataspec.fieldquality.model.FieldQualityReport;

/**
 * 标准字段质量评分服务。
 */
public interface FieldQualityService {

    FieldQualityReport report(Long projectId);
}
