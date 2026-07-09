package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单张表 metadata 读取失败的脱敏摘要。
 */
@Data
@Schema(description = "单张表 metadata 读取失败的脱敏摘要；只记录 schema/table、类别和安全错误摘要。")
public class DatabaseMetadataScanFailureItem {

    /** 失败表所在 schema；MySQL 场景可能为空。 */
    @Schema(description = "失败表所在 schema；MySQL 场景可能为空。")
    private String schemaName;

    /** 失败表名；来自 metadata，不包含业务数据行。 */
    @Schema(description = "失败表名；来自 metadata，不包含业务数据行。")
    private String tableName;

    /** 失败类别，如 PERMISSION_DENIED、TIMEOUT、CONNECTION 或 UNKNOWN。 */
    @Schema(description = "失败类别，如 PERMISSION_DENIED、TIMEOUT、CONNECTION 或 UNKNOWN。")
    private String category;

    /** true 表示可在降低 pageSize 或等待后重试。 */
    @Schema(description = "true 表示可在降低 pageSize 或等待后重试。")
    private boolean retryable;

    /** 脱敏错误摘要；不得包含 password、token、Authorization、JDBC URL 或 DSN。 */
    @Schema(description = "脱敏错误摘要；不得包含 password、token、Authorization、JDBC URL 或 DSN。")
    private String message;
}
