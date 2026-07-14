package com.dataspec.reviewfinding.model;

import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.common.validation.CodePointSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Finding 的项目级豁免摘要。
 *
 * @param waived 是否已被确定性规则豁免
 * @param waiverId DataSpec 规则豁免 ID；未豁免时为空
 * @param reason 脱敏后的豁免原因；最多 500 个 Unicode code point
 */
@Schema(description = "Finding 的规则豁免摘要；未豁免时 waiverId 和 reason 固定为空。")
public record ReviewFindingWaiver(
        @Schema(description = "是否已被确定性规则豁免；默认 false。")
        boolean waived,
        @Positive(message = "waiver.waiverId 必须是正整数")
        @Schema(description = "DataSpec 规则豁免 ID；未豁免或无持久化记录时为空。", minimum = "1")
        Long waiverId,
        @CodePointSize(max = 500, message = "waiver.reason 不能超过 500 个 Unicode code point")
        @Schema(description = "脱敏后的豁免原因；最多 500 个 Unicode code point。", maxLength = 500)
        String reason
) {
    public static final ReviewFindingWaiver NONE = new ReviewFindingWaiver(false, null, null);

    public ReviewFindingWaiver {
        if (!waived) {
            waiverId = null;
            reason = null;
        } else {
            reason = SensitiveDataSanitizer.redactText(reason, 500);
            if (reason != null && reason.isBlank()) {
                reason = null;
            }
        }
    }
}
