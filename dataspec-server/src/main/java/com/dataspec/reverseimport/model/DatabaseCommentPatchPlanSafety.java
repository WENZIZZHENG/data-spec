package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * COMMENT 回写计划安全边界。
 */
@Schema(description = "COMMENT 回写计划安全边界；说明计划只读、不会写源库或项目状态，可安全用于 AI 审阅。")
@Data
public class DatabaseCommentPatchPlanSafety {

    /** true 表示计划生成只读取 schema metadata。 */
    @Schema(description = "true 表示计划生成只读取 schema metadata。")
    private Boolean readOnly = true;

    /** true 表示服务端会写源数据库；COMMENT plan 必须保持 false。 */
    @Schema(description = "true 表示服务端会写源数据库；COMMENT plan 必须保持 false。")
    private Boolean writesSourceDatabase = false;

    /** true 表示服务端会写 DataSpec 项目状态；COMMENT plan 必须保持 false。 */
    @Schema(description = "true 表示服务端会写 DataSpec 项目状态；COMMENT plan 必须保持 false。")
    private Boolean writesProject = false;

    /** true 表示输出需要人工审阅后再进入迁移流程。 */
    @Schema(description = "true 表示输出需要人工审阅后再进入迁移流程。")
    private Boolean requiresManualApply = true;

    /** true 表示响应已按敏感信息规则脱敏，可复制给 AI 辅助审阅。 */
    @Schema(description = "true 表示响应已按敏感信息规则脱敏，可复制给 AI 辅助审阅。")
    private Boolean safeForAiCopy = true;

    /** true 表示自由文本经过 password、token、JDBC URL、DSN 等脱敏处理。 */
    @Schema(description = "true 表示自由文本经过 password、token、JDBC URL、DSN 等脱敏处理。")
    private Boolean sensitiveRedaction = true;
}
