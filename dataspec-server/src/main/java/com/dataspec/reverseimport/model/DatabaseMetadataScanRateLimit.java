package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据库 metadata 采集作业的限速输入与实际执行边界。
 */
@Data
@Schema(description = "数据库 metadata 采集作业限速边界；只影响 schema-only 扫描页大小和前端等待建议，不包含连接凭据。")
public class DatabaseMetadataScanRateLimit {

    /** 请求方希望单页最多读取的表数量；服务端会继续套用全局上限。 */
    @Schema(description = "请求方希望单页最多读取的表数量；为空时只使用 pageSize 和服务端全局上限。")
    private Integer maxTablesPerPage;

    /** 建议客户端两次继续扫描之间等待的毫秒数；第一版不在服务端线程 sleep。 */
    @Schema(description = "建议客户端两次继续扫描之间等待的毫秒数；服务端不会在请求线程内等待。")
    private Integer minDelayMs;

    /** 原始请求 pageSize，用于 evidence 说明是否被服务端降限。 */
    @Schema(description = "原始请求 pageSize；由服务端回填，用于说明是否触发降限。")
    private Integer requestedPageSize;

    /** 请求方传入的 maxTablesPerPage；由服务端回填，便于前端展示实际限速来源。 */
    @Schema(description = "请求方传入的 maxTablesPerPage；由服务端回填，便于前端展示限速来源。")
    private Integer requestedMaxTablesPerPage;

    /** 服务端全局允许的最大单页表数量。 */
    @Schema(description = "服务端全局允许的最大单页表数量。")
    private Integer maxPageSize;

    /** 本次请求实际采用的 pageSize。 */
    @Schema(description = "本次请求实际采用的 pageSize，已经应用请求限速和服务端上限。")
    private Integer effectivePageSize;
}
