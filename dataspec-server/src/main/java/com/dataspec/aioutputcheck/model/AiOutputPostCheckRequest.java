package com.dataspec.aioutputcheck.model;

import com.dataspec.common.validation.CodePointSize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.dataspec.reviewfinding.model.ReviewFinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 输出后置校验请求。
 *
 * @param projectId 当前项目 ID；所有标准引用按该项目解析。
 * @param contentType AI 产物类型；决定提取策略。
 * @param content 待校验 AI 输出文本；只读输入，不会被保存或改写。
 * @param snapshotRef 可选标准快照 stableRef 或版本；用于声明生成时依赖的标准版本。
 * @param findings 可选外部结构化 findings；最多 100 条，均按当前项目验证 evidence refs。
 */
@Schema(description = "AI 输出后置校验请求；校验过程只读，不修改标准、AI job、业务文件或数据库。")
public record AiOutputPostCheckRequest(
        @NotNull(message = "projectId 不能为空")
        @Schema(description = "当前项目 ID；引用解析严格限制在该项目内。", example = "1")
        Long projectId,

        @NotNull(message = "contentType 不能为空")
        @Schema(description = "AI 输出内容类型。")
        AiOutputContentType contentType,

        @NotBlank(message = "content 不能为空")
        @CodePointSize(max = 20000, message = "content 不能超过 20000 个 Unicode code point")
        @Schema(description = "待校验 AI 输出文本；结果只返回有界脱敏 excerpt，不保存 raw content。")
        String content,

        @Schema(description = "可选标准快照引用，如 snapshot:<projectId>:<snapshotId|version>；用于识别旧快照或快照漂移。")
        String snapshotRef,

        @Valid
        @Size(max = 100, message = "findings 不能超过 100 条")
        @ArraySchema(
                maxItems = 100,
                arraySchema = @Schema(description = "外部 AI 提交的结构化 findings；服务端会重写 source、projectId 和 findingKey，并验证 evidenceRefs。"),
                schema = @Schema(implementation = ReviewFinding.class))
        List<@NotNull(message = "findings 元素不能为空") @Valid ReviewFinding> findings
) {
    public AiOutputPostCheckRequest {
        findings = findings == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(findings));
    }

    /** 兼容未提交结构化 findings 的既有调用方。 */
    public AiOutputPostCheckRequest(
            Long projectId,
            AiOutputContentType contentType,
            String content,
            String snapshotRef
    ) {
        this(projectId, contentType, content, snapshotRef, List.of());
    }
}
