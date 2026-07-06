package com.dataspec.syntheticexample.model;

import java.util.List;

/**
 * 合成样例生成的安全边界，供 CLI、AI 和测试判断该能力不会写入项目或泄漏真实数据。
 *
 * @param readOnly 是否只读执行。
 * @param writesProject 是否会写入 DataSpec 项目数据。
 * @param containsRealBusinessRows 是否包含真实业务数据行。
 * @param externalLlmUsed 是否调用外部 LLM。
 * @param sensitiveInputs 调用方应避免复制到日志或文档的敏感输入类别。
 */
public record SyntheticExampleSafety(
        boolean readOnly,
        boolean writesProject,
        boolean containsRealBusinessRows,
        boolean externalLlmUsed,
        List<String> sensitiveInputs
) {
}
