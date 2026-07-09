package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * metadata 采集对源库压力的解释。
 */
@Data
@Schema(description = "数据库 metadata 采集作业的源库压力提示；解释 pageSize 限制、重试等待和安全下一步。")
public class DatabaseMetadataScanSourcePressureHint {

    /** INFO/WARNING/DANGER；第一版只用于前端提示，不阻塞只读扫描。 */
    @Schema(description = "压力等级：INFO、WARNING 或 DANGER；第一版只用于提示，不自动阻塞只读扫描。")
    private String level;

    /** 可读说明，必须脱敏，不包含 JDBC URL、DSN、token 或 password。 */
    @Schema(description = "可读说明；必须脱敏，不包含 JDBC URL、DSN、token、Authorization 或 password。")
    private String message;

    /** true 表示请求 pageSize 被服务端上限或 rateLimit 降低。 */
    @Schema(description = "true 表示请求 pageSize 被服务端上限或 rateLimit 降低。")
    private boolean boundedByServerLimit;

    /** 建议下一次继续扫描采用的 pageSize。 */
    @Schema(description = "建议下一次继续扫描采用的 pageSize。")
    private Integer suggestedPageSize;

    /** 对用户和 AI 安全的下一步动作。 */
    @Schema(description = "对用户和 AI 安全的下一步动作；不得包含连接凭据或业务数据行。")
    private List<String> safeNextActions = new ArrayList<>();
}
