package com.dataspec.template.model;

import com.dataspec.tablemodel.model.TableStructureStandard;

import java.time.LocalDateTime;

/**
 * 表模板 API 响应，保留既有模板字段并 additive 暴露表结构标准。
 *
 * @param id 模板 ID
 * @param projectId 所属项目 ID
 * @param name 模板名称
 * @param description 模板说明
 * @param tablePrefix 表名前缀
 * @param structure 表级结构标准，包含主键、唯一键、索引、外键和策略 guidance
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record TemplateResp(
        Long id,
        Long projectId,
        String name,
        String description,
        String tablePrefix,
        TableStructureStandard structure,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
