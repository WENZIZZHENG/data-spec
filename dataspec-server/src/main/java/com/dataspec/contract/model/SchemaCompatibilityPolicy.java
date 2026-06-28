package com.dataspec.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 输出契约的兼容策略。它描述结构兼容性，不代表权限、审批或写入安全策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaCompatibilityPolicy {

    private String level;
    private String compatibleSince;
    private String additiveFieldPolicy;
    private String breakingChangePolicy;
    private String deprecationPolicy;
    private String compatibilityWindow;
}
