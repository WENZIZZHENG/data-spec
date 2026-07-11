package com.dataspec.standardquery.service;

import com.dataspec.standardquery.model.StandardQueryRequest;
import com.dataspec.standardquery.model.StandardQueryResult;

/**
 * Standard Query DSL 只读查询服务。
 */
public interface StandardQueryService {

    /**
     * 执行项目内 Standard Query DSL 查询。
     *
     * @param request DSL 请求，查询只读且必须限制在 projectId 内。
     * @return 脱敏、可解释、有界的查询结果。
     */
    StandardQueryResult search(StandardQueryRequest request);

    /**
     * 将字段搜索 legacy 参数映射成 DSL 后执行，用于保持旧 API/CLI 兼容。
     *
     * @return 与 DSL 查询同形的可解释结果。
     */
    StandardQueryResult searchFieldsFromLegacyParameters(
            Long projectId,
            String query,
            String category,
            String tag,
            String status,
            Boolean sensitive,
            Long sourceBatchId,
            Integer limit);
}
