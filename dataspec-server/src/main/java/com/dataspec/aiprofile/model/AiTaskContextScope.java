package com.dataspec.aiprofile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Context 默认裁剪范围。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskContextScope {

    private String scope;
    private String query;
    private String status;
    private Integer limit;
}
