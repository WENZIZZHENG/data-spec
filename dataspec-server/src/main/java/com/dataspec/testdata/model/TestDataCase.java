package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 字段级测试数据用例。
 *
 * @param caseId 确定性用例 ID。
 * @param fieldName 标准字段名。
 * @param caseType 用例类型：VALID、INVALID 或 BOUNDARY。
 * @param value 合成样例值；敏感字段使用安全占位或脱敏值。
 * @param expectedValidity 该值是否预期通过字段级校验。
 * @param reason 用例生成原因和业务边界说明。
 * @param sourceRefs 来源标准引用，如 field:<id> 或 enum:<id>。
 * @param requiresBusinessReview 是否需要业务人工复核。
 */
@Schema(description = "字段级测试数据用例，覆盖 valid、invalid 和 boundary 三类。")
public record TestDataCase(
        @Schema(description = "确定性用例 ID。")
        String caseId,
        @Schema(description = "标准字段名。")
        String fieldName,
        @Schema(description = "用例类型：VALID、INVALID 或 BOUNDARY。")
        String caseType,
        @Schema(description = "合成样例值；敏感字段使用安全占位或脱敏值。")
        String value,
        @Schema(description = "该值是否预期通过字段级校验。")
        boolean expectedValidity,
        @Schema(description = "用例生成原因和业务边界说明。")
        String reason,
        @Schema(description = "来源标准引用，如 field:<id> 或 enum:<id>。")
        List<String> sourceRefs,
        @Schema(description = "是否需要业务人工复核。")
        boolean requiresBusinessReview
) {
}
