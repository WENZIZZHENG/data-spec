package com.dataspec.reviewfinding.model;

import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.common.validation.CodePointSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Finding 指向的业务对象摘要，不包含业务数据行。
 *
 * @param projectId 当前 DataSpec 项目 ID；无法确定时为空
 * @param kind 对象类型，如 SQL_COLUMN、SQL_TABLE、STANDARD_REFERENCE 或 AI_OUTPUT
 * @param name 对象的人读名称；最多 256 个 Unicode code point
 * @param tableName SQL 表名；非 SQL finding 为空
 * @param columnName SQL 字段名；非字段 finding 为空
 * @param stableRef 标准对象 stableRef；没有稳定引用时为空
 */
@Schema(description = "Finding 的项目级业务对象摘要；所有文本均脱敏和限长，不包含业务数据行。")
public record ReviewFindingSubject(
        @Positive(message = "subject.projectId 必须是正整数")
        @Schema(description = "当前项目 ID；无法确定时为空。", minimum = "1")
        Long projectId,
        @CodePointSize(max = 64, message = "subject.kind 不能超过 64 个 Unicode code point")
        @Schema(description = "对象类型，如 SQL_COLUMN、SQL_TABLE、STANDARD_REFERENCE 或 AI_OUTPUT。", maxLength = 64)
        String kind,
        @CodePointSize(max = 256, message = "subject.name 不能超过 256 个 Unicode code point")
        @Schema(description = "对象的人读名称；最多 256 个 Unicode code point。", maxLength = 256)
        String name,
        @CodePointSize(max = 256, message = "subject.tableName 不能超过 256 个 Unicode code point")
        @Schema(description = "SQL 表名；非 SQL finding 为空。", maxLength = 256)
        String tableName,
        @CodePointSize(max = 256, message = "subject.columnName 不能超过 256 个 Unicode code point")
        @Schema(description = "SQL 字段名；非字段 finding 为空。", maxLength = 256)
        String columnName,
        @CodePointSize(max = 256, message = "subject.stableRef 不能超过 256 个 Unicode code point")
        @Schema(description = "标准对象 stableRef；没有稳定引用时为空。", maxLength = 256)
        String stableRef
) {
    private static final int KIND_MAX_LENGTH = 64;
    private static final int VALUE_MAX_LENGTH = 256;

    public ReviewFindingSubject {
        kind = sanitize(kind, KIND_MAX_LENGTH);
        name = sanitize(name, VALUE_MAX_LENGTH);
        tableName = sanitize(tableName, VALUE_MAX_LENGTH);
        columnName = sanitize(columnName, VALUE_MAX_LENGTH);
        stableRef = sanitize(stableRef, VALUE_MAX_LENGTH);
    }

    private static String sanitize(String value, int maxLength) {
        String sanitized = SensitiveDataSanitizer.redactText(value, maxLength);
        return sanitized == null || sanitized.isBlank() ? null : sanitized;
    }
}
