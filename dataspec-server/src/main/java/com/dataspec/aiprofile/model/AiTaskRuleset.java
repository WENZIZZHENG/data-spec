package com.dataspec.aiprofile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 任务建议采用的规则集合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskRuleset {

    private String strictness;
    private List<String> requiredRuleCodes;
    private List<String> optionalRuleCodes;
}
