package com.dataspec.reviewfinding.model;

import com.dataspec.common.safety.DryRunEvidenceSigner;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.common.validation.CodePointSize;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 跨 SQL lint、AI post-check、PR review 和 Evidence Package 的共享只读 Finding。
 *
 * @param schemaVersion 结构版本；additive 字段保持当前版本，breaking 变更必须升级
 * @param source 原始发现来源
 * @param findingKey 去重稳定键；为空时由来源、规则、对象和位置确定性生成
 * @param code 稳定规则或问题码
 * @param severity 共享问题级别
 * @param subject 项目级业务对象摘要
 * @param location 可选文件或源码位置
 * @param trigger 触发规则或判定条件
 * @param expected 期望状态或约束
 * @param observed 实际观察到的脱敏摘要，不得包含完整业务数据行
 * @param evidenceRefs 可复核证据引用；最多 20 条，每条最多 500 个 Unicode code point
 * @param confidence 置信度 0-100；未知时为空
 * @param suggestedFix 脱敏修复建议；不表示允许自动执行
 * @param autoFixSafe 是否满足当前确定性低风险自动修复语义
 * @param waiver 项目级豁免摘要
 */
@Schema(description = "版本化共享 Review Finding；bounded、secret-safe，且不包含可复用凭据或完整外部响应。")
public record ReviewFinding(
        @Min(value = 1, message = "schemaVersion 必须是正整数")
        @Schema(description = "Finding schema 版本；breaking 变更必须升级。", example = "1", minimum = "1")
        int schemaVersion,
        @Schema(description = "原始发现来源；PR 评论和 Evidence Package 不改写该值。")
        ReviewFindingSource source,
        @CodePointSize(max = 128, message = "findingKey 不能超过 128 个 Unicode code point")
        @Schema(description = "稳定去重键；为空时由确定性字段生成，最多 128 个 Unicode code point。", maxLength = 128)
        String findingKey,
        @NotBlank(message = "code 不能为空")
        @CodePointSize(min = 1, max = 128, message = "code 长度必须是 1-128 个 Unicode code point")
        @Schema(description = "稳定规则或问题码；最多 128 个 Unicode code point。", minLength = 1, maxLength = 128,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @Schema(description = "共享问题级别。")
        ReviewFindingSeverity severity,
        @Valid
        @Schema(description = "项目级业务对象摘要。")
        ReviewFindingSubject subject,
        @Valid
        @Schema(description = "可选文件或源码位置；无可靠位置时为空。")
        ReviewFindingLocation location,
        @CodePointSize(max = 1000, message = "trigger 不能超过 1000 个 Unicode code point")
        @Schema(description = "触发规则或判定条件；最多 1000 个 Unicode code point。", maxLength = 1000)
        String trigger,
        @CodePointSize(max = 1000, message = "expected 不能超过 1000 个 Unicode code point")
        @Schema(description = "期望状态或约束；最多 1000 个 Unicode code point。", maxLength = 1000)
        String expected,
        @CodePointSize(max = 1000, message = "observed 不能超过 1000 个 Unicode code point")
        @Schema(description = "实际观察到的脱敏摘要；最多 1000 个 Unicode code point，不得包含完整业务数据行。", maxLength = 1000)
        String observed,
        @Size(max = 20, message = "evidenceRefs 不能超过 20 条")
        @ArraySchema(
                maxItems = 20,
                arraySchema = @Schema(description = "可复核证据引用；最多 20 条。"),
                schema = @Schema(type = "string", description = "单条证据引用；最多 500 个 Unicode code point。", minLength = 1, maxLength = 500))
        List<@NotBlank(message = "evidenceRefs 元素不能为空")
                @CodePointSize(max = 500, message = "evidenceRef 不能超过 500 个 Unicode code point") String> evidenceRefs,
        @Min(value = 0, message = "confidence 不能小于 0")
        @Max(value = 100, message = "confidence 不能大于 100")
        @Schema(description = "置信度 0-100；未知时为空。", minimum = "0", maximum = "100")
        Integer confidence,
        @CodePointSize(max = 1000, message = "suggestedFix 不能超过 1000 个 Unicode code point")
        @Schema(description = "脱敏修复建议；最多 1000 个 Unicode code point，不表示允许自动执行。", maxLength = 1000)
        String suggestedFix,
        @Schema(description = "仅在确定性、未豁免、LOW-risk 且真正 APPLIED 时可为 true；外部 AI 声明仍须 evidence gating。")
        boolean autoFixSafe,
        @Valid
        @Schema(description = "项目级豁免摘要；未豁免时为 waived=false。")
        ReviewFindingWaiver waiver
) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_EVIDENCE_REFS = 20;

    public ReviewFinding {
        schemaVersion = schemaVersion <= 0 ? SCHEMA_VERSION : schemaVersion;
        source = source == null ? ReviewFindingSource.EXTERNAL_AI : source;
        code = sanitize(code, 128);
        severity = severity == null ? ReviewFindingSeverity.WARNING : severity;
        trigger = sanitize(trigger, 1000);
        expected = sanitize(expected, 1000);
        observed = sanitize(observed, 1000);
        evidenceRefs = sanitizeEvidenceRefs(evidenceRefs);
        confidence = confidence == null ? null : Math.max(0, Math.min(100, confidence));
        suggestedFix = sanitize(suggestedFix, 1000);
        waiver = waiver == null ? ReviewFindingWaiver.NONE : waiver;
        findingKey = sanitize(findingKey, 128);
        if (findingKey == null) {
            findingKey = generatedKey(source, code, subject, location);
        }
    }

    /** 使用当前 schema 版本创建 Finding。 */
    public ReviewFinding(
            ReviewFindingSource source,
            String findingKey,
            String code,
            ReviewFindingSeverity severity,
            ReviewFindingSubject subject,
            ReviewFindingLocation location,
            String trigger,
            String expected,
            String observed,
            List<String> evidenceRefs,
            Integer confidence,
            String suggestedFix,
            boolean autoFixSafe,
            ReviewFindingWaiver waiver
    ) {
        this(SCHEMA_VERSION, source, findingKey, code, severity, subject, location, trigger, expected, observed,
                evidenceRefs, confidence, suggestedFix, autoFixSafe, waiver);
    }

    private static List<String> sanitizeEvidenceRefs(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        for (String value : values) {
            String sanitized = sanitize(value, 500);
            if (sanitized != null) {
                refs.add(sanitized);
            }
            if (refs.size() == MAX_EVIDENCE_REFS) {
                break;
            }
        }
        return List.copyOf(refs);
    }

    private static String generatedKey(
            ReviewFindingSource source,
            String code,
            ReviewFindingSubject subject,
            ReviewFindingLocation location
    ) {
        StringBuilder canonical = new StringBuilder();
        appendCanonicalValue(canonical, "review-finding-key-v1");
        appendCanonicalValue(canonical, source.name());
        appendCanonicalValue(canonical, code);
        appendCanonicalValue(canonical, subject == null ? null : subject.projectId());
        appendCanonicalValue(canonical, subject == null ? null : subject.kind());
        appendCanonicalValue(canonical, subject == null ? null : subject.name());
        appendCanonicalValue(canonical, subject == null ? null : subject.tableName());
        appendCanonicalValue(canonical, subject == null ? null : subject.columnName());
        appendCanonicalValue(canonical, subject == null ? null : subject.stableRef());
        appendCanonicalValue(canonical, location == null ? null : location.path());
        appendCanonicalValue(canonical, location == null ? null : location.line());
        appendCanonicalValue(canonical, location == null ? null : location.column());
        appendCanonicalValue(canonical, location == null ? null : location.lineEnd());
        appendCanonicalValue(canonical, location == null ? null : location.columnEnd());
        appendCanonicalValue(canonical, location == null ? null : location.sourceStart());
        appendCanonicalValue(canonical, location == null ? null : location.sourceEnd());
        appendCanonicalValue(canonical, location == null ? null : location.locationKind());
        return source.name().toLowerCase(Locale.ROOT) + ":"
                + DryRunEvidenceSigner.sha256Hex(canonical.toString()).substring(0, 24);
    }

    /**
     * 使用 UTF-8 byte length 前缀编码字段，显式区分 null，避免可控文本跨字段拼接产生同一哈希输入。
     */
    private static void appendCanonicalValue(StringBuilder target, Object value) {
        if (value == null) {
            target.append("-1:");
            return;
        }
        String text = String.valueOf(value);
        target.append(text.getBytes(StandardCharsets.UTF_8).length).append(':').append(text);
    }

    private static String sanitize(String value, int maxLength) {
        String sanitized = SensitiveDataSanitizer.redactText(value, maxLength);
        return sanitized == null || sanitized.isBlank() ? null : sanitized;
    }
}
