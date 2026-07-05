package com.dataspec.reverseimport.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据库 metadata 分页扫描请求。连接密码仍只在本次请求内使用，cursor 和 scanId 不携带凭据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DatabaseMetadataScanReq extends DatabaseConnectionReq {

    /** 前端或 AI 传回的一轮扫描标识；为空时服务端生成新的非凭据标识。 */
    private String scanId;

    /** 当前页 cursor；第一版使用已读取表数量作为短期偏移，不包含连接凭据。 */
    private String cursor;

    /** 每页表数量，服务端会限制在 1 到 100 之间，避免一次性扫描过大。 */
    @Min(value = 1, message = "分页大小不能小于 1")
    @Max(value = 100, message = "分页大小不能大于 100")
    private Integer pageSize;

    /** true 表示停止继续扫描；取消只影响扫描状态，不写源库或标准库。 */
    private Boolean cancel;
}
