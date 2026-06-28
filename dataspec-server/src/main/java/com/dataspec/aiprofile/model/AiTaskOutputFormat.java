package com.dataspec.aiprofile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 任务推荐输出格式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskOutputFormat {

    private String format;
    private String schemaRef;
    private Boolean includeEvidence;
    private Boolean includeNextActions;
}
