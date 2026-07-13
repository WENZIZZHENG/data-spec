package com.dataspec.querynormalization.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 查询命名解析证据；所有文本在返回前均已脱敏和限长。
 *
 * @param token              经脱敏的输入 token
 * @param normalizedToken    经脱敏的小写规范 token
 * @param tokenKind          词法边界类型
 * @param resolutionStatus   当前项目 glossary 解析状态
 * @param canonicalTerm      唯一解析出的 canonical 术语；歧义、禁用或未解析时为空
 * @param canonicalFieldId   唯一解析出的当前项目 canonical 字段 ID；没有绑定时为空
 * @param canonicalFieldName canonical 字段名；没有绑定时为空
 * @param glossaryIds        支撑该状态的当前项目 glossary ID；最多返回固定数量
 * @param reason             经脱敏和限长的确定性原因
 */
@Schema(description = "查询 token 的确定性解析证据；文本已脱敏，数组和原因均有固定上限。")
public record QueryTokenEvidence(
        @Schema(description = "经脱敏和限长的输入 token。")
        String token,
        @Schema(description = "使用 Locale.ROOT 小写后的脱敏规范 token。")
        String normalizedToken,
        @Schema(description = "词法边界类型。")
        QueryTokenKind tokenKind,
        @Schema(description = "当前项目 glossary 解析状态。")
        QueryTokenResolutionStatus resolutionStatus,
        @Schema(description = "唯一 canonical 术语；AMBIGUOUS、DISABLED、UNRESOLVED 时为空", nullable = true)
        String canonicalTerm,
        @Schema(description = "唯一 canonical 字段 ID；未绑定或非 RESOLVED 时为空", nullable = true)
        Long canonicalFieldId,
        @Schema(description = "唯一 canonical 字段名；未绑定或非 RESOLVED 时为空", nullable = true)
        String canonicalFieldName,
        @ArraySchema(
                arraySchema = @Schema(description = "支撑该解析状态的当前项目 glossary 条目 ID；最多返回 8 个。"),
                schema = @Schema(description = "当前项目业务术语表条目 ID。", format = "int64"))
        List<Long> glossaryIds,
        @Schema(description = "经脱敏和限长的确定性解析原因。")
        String reason
) {
}
