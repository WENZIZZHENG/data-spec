package com.dataspec.aiprofile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个 AI profile 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskProfileDetail {

    private Long projectId;
    private String requestedProfile;
    private AiTaskProfile profile;
    private List<AiProfileDiagnostic> diagnostics;
    private List<String> supportedProfileIds;
    private List<String> supportedTaskTypes;
}
