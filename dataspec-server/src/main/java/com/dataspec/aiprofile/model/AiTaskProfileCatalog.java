package com.dataspec.aiprofile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI profile 列表响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskProfileCatalog {

    private Long projectId;
    private String defaultProfileId;
    private String selectedProfileId;
    private List<AiTaskProfile> profiles;
    private List<AiProfileDiagnostic> diagnostics;
    private List<String> supportedTaskTypes;
}
