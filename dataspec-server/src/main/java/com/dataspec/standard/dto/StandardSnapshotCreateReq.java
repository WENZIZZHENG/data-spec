package com.dataspec.standard.dto;

/**
 * 标准快照创建请求。
 *
 * @param version 用户定义版本号
 * @param name 快照名称
 * @param description 快照说明
 */
public record StandardSnapshotCreateReq(
        String version,
        String name,
        String description
) {
}
