package com.dataspec.aioutputcheck.model;

import com.dataspec.standardref.model.StandardReferenceType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AI 输出后置校验问题。
 *
 * @param code 稳定问题码，如 UNKNOWN_STANDARD_REFERENCE、STALE_STANDARD_REFERENCE 或 EVIDENCE_GAP。
 * @param severity 问题级别；FAIL 会阻断使用。
 * @param refType 相关标准对象类型；证据缺口可为空。
 * @param inputRef 脱敏后的输入引用。
 * @param message 脱敏诊断说明。
 * @param excerpt 有界脱敏原文片段，帮助定位问题。
 * @param replacementRef 可用替代 stableRef；没有替代时为空。
 * @param evidenceLinks 相关只读证据链接。
 * @param nextActions 建议的下一步动作。
 */
@Schema(description = "AI 输出后置校验问题；所有文本字段均必须 secret-safe。")
public record AiOutputPostCheckIssue(
        @Schema(description = "稳定问题码。")
        String code,
        @Schema(description = "问题级别。")
        AiOutputPostCheckIssueSeverity severity,
        @Schema(description = "相关标准对象类型；证据缺口可为空。")
        StandardReferenceType refType,
        @Schema(description = "脱敏后的输入引用。")
        String inputRef,
        @Schema(description = "脱敏诊断说明。")
        String message,
        @Schema(description = "有界脱敏原文片段。")
        String excerpt,
        @Schema(description = "可用替代 stableRef。")
        String replacementRef,
        @ArraySchema(schema = @Schema(description = "相关只读证据链接。"))
        List<String> evidenceLinks,
        @ArraySchema(schema = @Schema(description = "建议的下一步动作。"))
        List<String> nextActions
) {
    public AiOutputPostCheckIssue {
        evidenceLinks = evidenceLinks == null ? List.of() : List.copyOf(evidenceLinks);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
