package com.dataspec.standardref.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准引用解析置信度。
 */
@Schema(description = "标准引用解析置信度；stableRef 和精确名称通常为 HIGH，派生别名或文本提示可降为 MEDIUM/LOW。")
public enum StandardReferenceConfidence {
    /** stableRef、当前标准名或唯一精确别名命中。 */
    HIGH,
    /** 派生历史名或兼容摘要命中，需要调用方结合 evidence 复核。 */
    MEDIUM,
    /** 低置信自然语言候选，只能作为提示，不能自动采纳。 */
    LOW
}
