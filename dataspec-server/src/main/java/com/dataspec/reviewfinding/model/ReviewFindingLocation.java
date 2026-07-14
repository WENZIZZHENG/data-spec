package com.dataspec.reviewfinding.model;

import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.common.validation.CodePointSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * Finding 在业务仓库文件或 SQL 文本中的位置。
 *
 * @param path 业务仓库相对路径；后端仅收到 SQL 文本时为空
 * @param line 1-based 起始行
 * @param column 1-based 起始列
 * @param lineEnd 1-based 结束行
 * @param columnEnd 1-based 结束列，不含结束位置
 * @param sourceStart 0-based 起始字符偏移
 * @param sourceEnd 0-based 结束字符偏移，不含结束位置
 * @param locationKind 定位类型，如 table、column 或 comment_column
 */
@Schema(description = "Finding 的可选文件与源码范围；无可靠位置时字段为空，不伪造行号。")
public record ReviewFindingLocation(
        @CodePointSize(max = 512, message = "location.path 不能超过 512 个 Unicode code point")
        @Schema(description = "业务仓库相对路径；最多 512 个 Unicode code point，后端仅收到 SQL 文本时为空。", maxLength = 512)
        String path,
        @Min(value = 1, message = "location.line 必须大于等于 1")
        @Schema(description = "1-based 起始行；未知时为空。", minimum = "1")
        Integer line,
        @Min(value = 1, message = "location.column 必须大于等于 1")
        @Schema(description = "1-based 起始列；未知时为空。", minimum = "1")
        Integer column,
        @Min(value = 1, message = "location.lineEnd 必须大于等于 1")
        @Schema(description = "1-based 结束行；未知时为空。", minimum = "1")
        Integer lineEnd,
        @Min(value = 1, message = "location.columnEnd 必须大于等于 1")
        @Schema(description = "1-based 结束列，不含结束位置；未知时为空。", minimum = "1")
        Integer columnEnd,
        @Min(value = 0, message = "location.sourceStart 必须大于等于 0")
        @Schema(description = "0-based 起始字符偏移；未知时为空。", minimum = "0")
        Integer sourceStart,
        @Min(value = 0, message = "location.sourceEnd 必须大于等于 0")
        @Schema(description = "0-based 结束字符偏移，不含结束位置；未知时为空。", minimum = "0")
        Integer sourceEnd,
        @CodePointSize(max = 64, message = "location.locationKind 不能超过 64 个 Unicode code point")
        @Schema(description = "定位类型，如 table、column 或 comment_column。", maxLength = 64)
        String locationKind
) {
    public ReviewFindingLocation {
        path = sanitize(path, 512);
        line = positiveOrNull(line);
        column = positiveOrNull(column);
        lineEnd = positiveOrNull(lineEnd);
        columnEnd = positiveOrNull(columnEnd);
        sourceStart = nonNegativeOrNull(sourceStart);
        sourceEnd = nonNegativeOrNull(sourceEnd);
        locationKind = sanitize(locationKind, 64);
    }

    private static Integer positiveOrNull(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    private static Integer nonNegativeOrNull(Integer value) {
        return value != null && value >= 0 ? value : null;
    }

    private static String sanitize(String value, int maxLength) {
        String sanitized = SensitiveDataSanitizer.redactText(value, maxLength);
        return sanitized == null || sanitized.isBlank() ? null : sanitized;
    }
}
