package com.dataspec.standardchange.model;

/**
 * 规则配置变更预览请求。
 */
public record RuleChangePreviewReq(
        Long projectId,
        String ruleName,
        String severity,
        Boolean enabled,
        String paramsJson
) {
}
