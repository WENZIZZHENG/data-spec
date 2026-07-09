package com.dataspec.reverseimport.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * metadata 采集页的 bounded 失败摘要。
 */
@Data
@Schema(description = "数据库 metadata 采集页的 bounded 失败摘要；用于展示失败表样例和安全下一步。")
public class DatabaseMetadataScanFailureSummary {

    /** 本页失败表数量。 */
    @Schema(description = "本页失败表数量。")
    private int failedTableCount;

    /** bounded 失败表示例，默认只保留前若干项。 */
    @Schema(description = "bounded 失败表示例，默认只保留前若干项。")
    private List<DatabaseMetadataScanFailureItem> failedTables = new ArrayList<>();

    /** 本页出现过的失败类别。 */
    @Schema(description = "本页出现过的失败类别。")
    private List<String> failureCategories = new ArrayList<>();

    /** true 表示至少一个失败项可重试。 */
    @Schema(description = "true 表示至少一个失败项可重试。")
    private boolean retryable;

    /** 安全下一步动作，不包含凭据或完整连接串。 */
    @Schema(description = "安全下一步动作，不包含凭据、完整连接串或业务数据行。")
    private List<String> safeNextActions = new ArrayList<>();
}
