package com.dataspec.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 契约字段废弃说明，用于告诉 AI 旧字段还能读多久以及推荐替代字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeprecatedContractField {

    private String fieldPath;
    private String deprecatedSince;
    private String removalAfter;
    private String replacement;
    private String reason;
}
