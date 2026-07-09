package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * metadata 采集作业的安全重试建议。
 */
@Data
@Schema(description = "数据库 metadata 采集作业的安全重试建议；只描述客户端下一步，不自动重放请求。")
public class DatabaseMetadataScanRetryPolicy {

    /** true 表示可以由用户或 AI 显式发起继续/重试。 */
    @Schema(description = "true 表示可以由用户或 AI 显式发起继续或重试；服务端不会自动重放。")
    private boolean retryable;

    /** 建议下一次请求前等待的毫秒数。 */
    @Schema(description = "建议下一次继续或重试前等待的毫秒数。")
    private int retryAfterMs;

    /** 建议的最大重试次数，避免对源库持续施压。 */
    @Schema(description = "建议的最大重试次数，避免对源库持续施压。")
    private int maxRetryAttempts;

    /** true 表示重试前建议降低 pageSize。 */
    @Schema(description = "true 表示重试前建议降低 pageSize。")
    private boolean lowerPageSizeRecommended;

    /** true 表示可优先使用 metadata cache 或刷新缓存后再继续。 */
    @Schema(description = "true 表示可优先使用 metadata cache 或刷新缓存后再继续。")
    private boolean useMetadataCacheRecommended;
}
