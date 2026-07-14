package com.dataspec.standardcandidate.model;

import com.dataspec.querynormalization.model.QueryTokenEvidence;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单条命名证据候选信号。
 *
 * @param signalType   候选信号类型
 * @param tokenEvidence 有界、脱敏的确定性 token evidence
 */
@Schema(description = "单条可操作命名证据信号；不包含 raw sourceText。")
public record TokenEvidenceCandidateSignal(
        @Schema(description = "信号类型。", requiredMode = Schema.RequiredMode.REQUIRED)
        TokenEvidenceCandidateSignalType signalType,
        @Schema(description = "有界、脱敏的确定性 token evidence。", requiredMode = Schema.RequiredMode.REQUIRED)
        QueryTokenEvidence tokenEvidence
) {
}
