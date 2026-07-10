package com.dataspec.aioutputcheck.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 产物内容类型。
 */
@Schema(description = "AI 输出后置校验的内容类型；决定字段、stableRef、规则和证据引用的确定性提取方式。")
public enum AiOutputContentType {
    /** SQL 查询或片段，使用保守 identifier 提取，不写入 SQL 检查记录。 */
    SQL,
    /** DDL，优先复用只读 SQL parser 提取 CREATE TABLE 字段。 */
    DDL,
    /** Markdown 文档，提取显式 stableRef、反引号字段和 evidence ref。 */
    MARKDOWN,
    /** JSON 文本，提取显式 stableRef，不信任 raw business payload。 */
    JSON,
    /** 普通文本，第一版只提取显式 stableRef；面向 CLI/MCP/API 的稳定外部协议值。 */
    TEXT,
    /** 普通文本的历史兼容值；新客户端应发送 TEXT。 */
    @Deprecated
    PLAIN_TEXT
}
