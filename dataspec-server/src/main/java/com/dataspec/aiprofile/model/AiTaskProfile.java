package com.dataspec.aiprofile.model;

import com.dataspec.lint.model.FixPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 项目级 AI 使用画像。第一版为内置建议，不代表权限边界。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskProfile {

    private String profileId;
    private String taskType;
    private String displayName;
    private String description;
    private AiTaskContextScope contextScope;
    private AiTaskRuleset ruleset;
    private FixPolicy fixedSqlPolicy;
    private AiTaskOutputFormat outputFormat;
    private Integer maxContextFields;
    private List<String> recommendedCommands;
    private List<String> nextActions;
    private Boolean defaultProfile;
}
