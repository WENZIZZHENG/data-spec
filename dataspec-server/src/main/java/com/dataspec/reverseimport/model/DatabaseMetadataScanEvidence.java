package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 可复制给 AI 的 metadata 采集作业证据。
 */
@Data
@Schema(description = "可复制给 AI 的 metadata 采集作业证据；只描述 schema-only 边界、进度和安全标志。")
public class DatabaseMetadataScanEvidence {

    /** 采集作业标识；不携带连接凭据，不承诺跨重启持久化。 */
    @Schema(description = "采集作业标识；不携带连接凭据，不承诺跨重启持久化。")
    private String scanJobId;

    /** 作业状态：RUNNING、PARTIAL、COMPLETED、CANCELLED 或 FAILED。 */
    @Schema(description = "作业状态：RUNNING、PARTIAL、COMPLETED、CANCELLED 或 FAILED。")
    private String status;

    /** 已处理表数量。 */
    @Schema(description = "已处理表数量。")
    private int processedTableCount;

    /** 失败表数量。 */
    @Schema(description = "失败表数量。")
    private int failedTableCount;

    /** schema 扫描范围摘要。 */
    @Schema(description = "schema 扫描范围摘要；不得包含连接串。")
    private String schemaScope;

    /** 表范围摘要，只保留表名，不包含业务数据行。 */
    @Schema(description = "表范围摘要，只保留表名，不包含业务数据行。")
    private List<String> tableScope = new ArrayList<>();

    /** metadata cache fingerprint；为空表示未命中缓存证据。 */
    @Schema(description = "metadata cache fingerprint；为空表示未命中缓存证据。")
    private String metadataFingerprint;

    /** true 表示只读取 schema metadata，不读取业务数据行。 */
    @Schema(description = "true 表示只读取 schema metadata，不读取业务数据行。")
    private boolean schemaOnly;

    /** true 表示本作业不会写入源数据库。 */
    @Schema(description = "true 表示本作业不会写入源数据库。")
    private boolean noSourceWrites;

    /** true 表示本作业不会写入 DataSpec 标准字段库。 */
    @Schema(description = "true 表示本作业不会写入 DataSpec 标准字段库。")
    private boolean noStandardWrites;

    /** true 表示 evidence 已脱敏，可复制给 AI。 */
    @Schema(description = "true 表示 evidence 已脱敏，可复制给 AI。")
    private boolean safeForAiCopy;

    /** 基于当前作业状态生成的下一步验证或恢复动作。 */
    @Schema(description = "基于当前作业状态生成的下一步验证或恢复动作；不得包含凭据。")
    private List<String> nextActions = new ArrayList<>();
}
