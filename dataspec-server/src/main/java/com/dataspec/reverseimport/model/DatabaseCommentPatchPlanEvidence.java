package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * COMMENT 回写计划生成证据。
 */
@Schema(description = "COMMENT 回写计划生成证据；只记录 schema-only 范围、标准引用和安全摘要。")
@Data
public class DatabaseCommentPatchPlanEvidence {

    /** schema/database 范围摘要，不包含 JDBC URL。 */
    @Schema(description = "schema/database 范围摘要，不包含 JDBC URL。")
    private String schemaScope;

    /** 本次选择的表范围。 */
    @Schema(description = "本次选择的表范围；只包含表名或 schema.table 名。")
    private List<String> tableScope = new ArrayList<>();

    /** schema-only metadata fingerprint；不包含凭据或业务数据行。 */
    @Schema(description = "schema-only metadata fingerprint；不包含凭据或业务数据行。")
    private String metadataFingerprint;

    /** 参与计划判断的标准引用。 */
    @Schema(description = "参与计划判断的标准引用，如 template:<key> 或 field:<name>。")
    private List<String> standardReferences = new ArrayList<>();

    /** 脱敏请求摘要，便于 AI/评审复核。 */
    @Schema(description = "脱敏请求摘要，便于 AI/评审复核；不得包含 password、token、完整 JDBC URL 或 DSN。")
    private String normalizedInputSummary;

    /** 安全标记，如 readOnly、noSourceWrites、schemaOnly。 */
    @Schema(description = "安全标记，如 readOnly、noSourceWrites、schemaOnly。")
    private List<String> safetyFlags = new ArrayList<>();
}
