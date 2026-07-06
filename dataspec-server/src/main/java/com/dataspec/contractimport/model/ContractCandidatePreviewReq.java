package com.dataspec.contractimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 契约候选导入预览请求，只承载只读解析所需的契约来源和内容。
 *
 * @param projectId DataSpec 项目 ID，用于限定现有标准字段匹配范围。
 * @param sourceKind 契约来源类型，允许 openapi、json-schema、protobuf。
 * @param sourcePath 契约来源路径或人读标识；输出前会脱敏，不会被服务端读取为外部 URL。
 * @param contractContent 契约文件文本内容，服务端只在内存中解析并用于脱敏 hash。
 * @param maxCandidates 本次最多返回候选数量；为空时使用服务默认上限。
 */
@Schema(description = "契约候选导入预览请求；只读解析本地提交的契约文本，不访问外部 URL 或写入候选库。")
public record ContractCandidatePreviewReq(
        @NotNull(message = "projectId 不能为空")
        @Schema(description = "DataSpec 项目 ID，用于限定已有标准字段匹配范围。")
        Long projectId,
        @NotBlank(message = "sourceKind 不能为空")
        @Schema(description = "契约来源类型，允许 openapi、json-schema、protobuf。")
        String sourceKind,
        @NotBlank(message = "sourcePath 不能为空")
        @Schema(description = "契约来源路径或人读标识；仅作为证据展示和 hash 输入，输出前会脱敏且不会被服务端当作外部 URL 读取。")
        String sourcePath,
        @NotBlank(message = "contractContent 不能为空")
        @Size(max = 524288, message = "contractContent 不能超过 512KB")
        @Schema(description = "契约文件文本内容；服务端只在内存中解析，并用完整脱敏文本参与 contractHash 计算。")
        String contractContent,
        @Min(value = 1, message = "maxCandidates 必须大于 0")
        @Max(value = 500, message = "maxCandidates 不能超过 500")
        @Schema(description = "本次最多返回的候选数量；为空时使用服务默认上限，服务端会限制在 1 到 500。")
        Integer maxCandidates
) {
}
