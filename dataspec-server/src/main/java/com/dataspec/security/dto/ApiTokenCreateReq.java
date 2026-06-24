package com.dataspec.security.dto;

import java.util.List;

/**
 * API token 创建请求。
 *
 * @param name token 名称
 * @param operatorName 变更日志中记录的操作者
 * @param allProjects 是否授权全部项目
 * @param projectIds 授权项目列表；allProjects=false 时必填
 */
public record ApiTokenCreateReq(
        String name,
        String operatorName,
        Boolean allProjects,
        List<Long> projectIds
) {
}
