package com.dataspec.tablemodel.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务对象标准响应，向 API、前端、CLI/MCP 和 AI Context 暴露结构化字段。
 *
 * @param id 业务对象标准 ID
 * @param projectId 所属项目 ID
 * @param objectKey 项目内唯一业务对象键
 * @param entityName 人可读业务实体名称
 * @param tablePattern 推荐表名模式或前缀提示
 * @param templateId 可选关联表模板 ID
 * @param requiredFields 必选字段名或稳定引用数组
 * @param optionalFields 可选字段名或稳定引用数组
 * @param relations 业务对象关系提示
 * @param foreignKeyHints 外键提示
 * @param auditFields 审计字段提示
 * @param commonPitfalls 常见反模式
 * @param aiUsageNotes AI 使用说明
 * @param contextExport 是否默认导出到 AI Context
 * @param status 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record BusinessObjectStandardResp(
        Long id,
        Long projectId,
        String objectKey,
        String entityName,
        String tablePattern,
        Long templateId,
        List<String> requiredFields,
        List<String> optionalFields,
        List<TableRelationHint> relations,
        List<TableForeignKeyStandard> foreignKeyHints,
        TableAuditPolicy auditFields,
        List<String> commonPitfalls,
        String aiUsageNotes,
        Boolean contextExport,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
