package com.dataspec.fieldprovenance.service;

import com.dataspec.fieldprovenance.model.FieldProvenanceConfidenceReport;

/**
 * 字段来源可信度聚合服务。
 */
public interface FieldProvenanceConfidenceService {

    /**
     * 生成指定项目的只读字段来源可信度报告。
     *
     * @param projectId 项目 ID，必须有当前调用方访问权限。
     * @return 字段来源可信度报告，不包含 raw metadata、raw evidence 或可复制凭据。
     */
    FieldProvenanceConfidenceReport report(Long projectId);
}
