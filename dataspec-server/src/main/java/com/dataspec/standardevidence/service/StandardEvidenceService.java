package com.dataspec.standardevidence.service;

import com.dataspec.standardevidence.model.StandardEvidenceReport;

/**
 * 标准对象跨来源证据只读聚合服务。
 */
public interface StandardEvidenceService {

    /**
     * 查询单个标准对象的跨来源证据视图。
     *
     * @param projectId 项目 ID，必须有当前调用方访问权限。
     * @param subjectType 目标对象类型，第一版仅支持 FIELD。
     * @param subjectId 目标对象 ID，FIELD 时为标准字段 ID。
     * @return 只读证据视图，不包含 raw SQL、AI payload、候选 raw evidence、raw source metadata 或凭据。
     */
    StandardEvidenceReport report(Long projectId, String subjectType, Long subjectId);
}
