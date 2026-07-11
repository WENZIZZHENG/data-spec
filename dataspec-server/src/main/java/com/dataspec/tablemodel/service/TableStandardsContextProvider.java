package com.dataspec.tablemodel.service;

import com.dataspec.aicontext.model.AiContextScopeOptions;

/**
 * AI Context 导出表结构标准的只读端口。
 */
public interface TableStandardsContextProvider {

    /** AI Context 包中的表结构标准文件路径。 */
    String TABLE_STANDARDS_FILE = ".dataspec/table-standards.json";

    /** 为项目生成 AI 可读的表结构标准 JSON。 */
    String generateTableStandardsJson(Long projectId);

    /** 按 AI Context scope/query/limit 裁剪表结构标准，字段目录裁剪语义保持独立。 */
    String generateTableStandardsJson(Long projectId, AiContextScopeOptions options);

    /** 为 DATABASE_RULES.md 生成表结构标准摘要。 */
    String generateTableStandardsMarkdown(Long projectId);
}
