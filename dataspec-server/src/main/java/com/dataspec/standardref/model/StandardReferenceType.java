package com.dataspec.standardref.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准对象引用类型。
 *
 * <p>该枚举用于 API、CLI、MCP 和 AI Context 之间稳定表达待解析对象范围，第一版只覆盖
 * DataSpec 已有且可项目内确定解析的字段、枚举、规则和标准快照。</p>
 */
@Schema(description = "标准对象引用类型；用于限定 stableRef、字段名、别名或版本号应解析到哪类项目内标准对象。")
public enum StandardReferenceType {
    /** 标准字段，stableRef 格式为 field:<projectId>:<fieldId>。 */
    FIELD,
    /** 枚举代码集，stableRef 格式为 enum:<projectId>:<codeSetId>。 */
    ENUM,
    /** 项目规则，stableRef 格式为 rule:<projectId>:<ruleCode>。 */
    RULE,
    /** 标准快照，stableRef 格式为 snapshot:<projectId>:<snapshotId|version>。 */
    SNAPSHOT
}
