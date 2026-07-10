package com.dataspec.standardref.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 单条标准引用解析结果。
 *
 * @param inputRef 脱敏后的原始输入引用；不得包含 token、password、Authorization、JDBC URL 或 DSN。
 * @param refType 本次解析使用的标准对象类型。
 * @param resolutionStatus 解析状态；不确定状态下 canonicalRef 为空，调用方不得猜测。
 * @param stableRef 命中的对象 stableRef；UNKNOWN/AMBIGUOUS/CROSS_PROJECT 时可为空。
 * @param canonicalRef 当前推荐 canonical stableRef；废弃字段可指向 replacementRef。
 * @param objectId 当前项目对象 ID；规则等无数字对象或跨项目引用时可为空。
 * @param currentName 当前标准对象名称、编码、规则码或快照版本；输出前已脱敏。
 * @param matchedAlias 触发命中的别名或历史名；非别名命中时可为空，输出前已脱敏。
 * @param lifecycleStatus 字段生命周期或对象可用状态；无生命周期概念时可为空。
 * @param replacementRef 废弃或停用对象的替代 stableRef；无替代时可为空。
 * @param confidence 解析置信度；用于 AI 判断是否可自动采纳。
 * @param evidenceLinks 只读证据链接或对象定位符；不得包含业务密文。
 * @param warnings 面向用户和 AI 的脱敏诊断，说明过期、歧义、未知或跨项目原因。
 */
@Schema(description = "单条标准引用解析结果；所有可复制文本均经过 secret-safe 脱敏。")
public record StandardReferenceResolutionResult(
        @Schema(description = "脱敏后的原始输入引用。")
        String inputRef,
        @Schema(description = "引用类型。")
        StandardReferenceType refType,
        @Schema(description = "解析状态。")
        StandardReferenceResolutionStatus resolutionStatus,
        @Schema(description = "命中的 project-scoped stableRef；未命中或不可暴露时为空。")
        String stableRef,
        @Schema(description = "当前推荐 canonical stableRef；歧义、未知或跨项目时为空。")
        String canonicalRef,
        @Schema(description = "当前项目对象 ID；规则或不可暴露对象可为空。")
        Long objectId,
        @Schema(description = "当前标准对象名称、编码、规则码或快照版本。")
        String currentName,
        @Schema(description = "命中的别名、历史名或版本别名。")
        String matchedAlias,
        @Schema(description = "字段生命周期或对象可用状态。")
        String lifecycleStatus,
        @Schema(description = "替代对象 stableRef；仅废弃、停用或被替代对象需要。")
        String replacementRef,
        @Schema(description = "解析置信度。")
        StandardReferenceConfidence confidence,
        @ArraySchema(schema = @Schema(description = "只读证据链接或对象定位符。"))
        List<String> evidenceLinks,
        @ArraySchema(schema = @Schema(description = "脱敏诊断或下一步提示。"))
        List<String> warnings
) {
    public StandardReferenceResolutionResult {
        evidenceLinks = evidenceLinks == null ? List.of() : List.copyOf(evidenceLinks);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
