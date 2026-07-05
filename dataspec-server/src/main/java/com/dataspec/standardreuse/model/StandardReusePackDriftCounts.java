package com.dataspec.standardreuse.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标准复用包漂移计数。
 */
@Schema(description = "目标项目相对标准复用包的匹配、缺失、覆盖和漂移计数。")
public record StandardReusePackDriftCounts(
        @Schema(description = "目标项目与包内容一致的资产数量。") Integer matched,
        @Schema(description = "目标项目缺失但包中存在的资产数量。") Integer missing,
        @Schema(description = "目标项目存在同自然键资产且被视为本地覆盖的数量。") Integer overridden,
        @Schema(description = "目标项目存在同自然键资产但关键内容不同的数量。") Integer drifted
) {
    public static StandardReusePackDriftCounts empty() {
        return new StandardReusePackDriftCounts(0, 0, 0, 0);
    }

    public StandardReusePackDriftCounts plusMatched(int value) {
        return new StandardReusePackDriftCounts(matched + value, missing, overridden, drifted);
    }

    public StandardReusePackDriftCounts plusMissing(int value) {
        return new StandardReusePackDriftCounts(matched, missing + value, overridden, drifted);
    }

    public StandardReusePackDriftCounts plusOverridden(int value) {
        return new StandardReusePackDriftCounts(matched, missing, overridden + value, drifted);
    }

    public StandardReusePackDriftCounts plusDrifted(int value) {
        return new StandardReusePackDriftCounts(matched, missing, overridden, drifted + value);
    }
}
