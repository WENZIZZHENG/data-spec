package com.dataspec.syntheticexample.model;

import java.util.List;

/**
 * 合成样例的数据来源摘要，说明生成器使用了多少项目标准素材以及是否启用内置场景兜底。
 *
 * @param standardFieldCount 项目标准字段总数。
 * @param templateCount 项目表模板总数。
 * @param codeSetReferenceCount 字段中引用代码集/枚举的数量。
 * @param fallbackUsed 是否使用内置合成场景字段补齐。
 * @param selectedFieldNames 本次场景最终参与生成的字段名。
 */
public record SyntheticExampleSourceSummary(
        int standardFieldCount,
        int templateCount,
        int codeSetReferenceCount,
        boolean fallbackUsed,
        List<String> selectedFieldNames
) {
}
