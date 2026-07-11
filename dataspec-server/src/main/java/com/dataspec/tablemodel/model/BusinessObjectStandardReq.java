package com.dataspec.tablemodel.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 业务对象标准写入请求；数组和对象字段由服务端序列化为受控 JSON 保存。
 *
 * @param projectId 所属项目 ID，创建时必填，更新时用于校验项目归属
 * @param objectKey 项目内唯一业务对象键，供 CLI/MCP/AI Context 稳定引用
 * @param entityName 人可读业务实体名称，例如订单、用户、支付记录
 * @param tablePattern 推荐表名模式或前缀提示，不会自动创建表
 * @param templateId 可选关联表模板 ID，必须属于同一项目
 * @param requiredFields 必选字段名或稳定引用数组，不得包含业务数据行
 * @param optionalFields 可选字段名或稳定引用数组，不得包含业务数据行
 * @param relations 业务对象关系结构数组，只作为只读关系提示
 * @param foreignKeyHints 外键提示结构数组，只用于 DDL preview 和 AI guidance
 * @param auditFields 审计字段提示
 * @param commonPitfalls 常见误用或反模式说明
 * @param aiUsageNotes AI 使用说明，服务端会拒绝明显 secret
 * @param contextExport 是否默认导出到 AI Context
 * @param status 状态，缺省为 ENABLED
 */
public record BusinessObjectStandardReq(
        @NotNull(message = "项目ID不能为空") Long projectId,
        @NotBlank(message = "业务对象键不能为空") String objectKey,
        @NotBlank(message = "业务对象名称不能为空") String entityName,
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
        String status
) {
}
