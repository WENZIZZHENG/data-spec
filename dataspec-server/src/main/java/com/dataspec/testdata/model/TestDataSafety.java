package com.dataspec.testdata.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 测试数据包安全边界。
 *
 * @param readOnly 是否只读。
 * @param writesProject 是否写入 DataSpec 项目记录。
 * @param writesBusinessRepo 是否写入业务仓库文件。
 * @param containsRealBusinessRows 是否包含真实业务数据行。
 * @param externalNetworkUsed 是否访问外部网络。
 * @param externalLlmUsed 是否调用外部 LLM。
 * @param sensitiveInputCategories 可能参与脱敏的敏感输入类别。
 */
@Schema(description = "测试数据包安全边界声明。")
public record TestDataSafety(
        @Schema(description = "是否只读。")
        boolean readOnly,
        @Schema(description = "是否写入 DataSpec 项目记录。")
        boolean writesProject,
        @Schema(description = "是否写入业务仓库文件。")
        boolean writesBusinessRepo,
        @Schema(description = "是否包含真实业务数据行。")
        boolean containsRealBusinessRows,
        @Schema(description = "是否访问外部网络。")
        boolean externalNetworkUsed,
        @Schema(description = "是否调用外部 LLM。")
        boolean externalLlmUsed,
        @Schema(description = "可能参与脱敏的敏感输入类别。")
        List<String> sensitiveInputCategories
) {
}
