package com.dataspec.fieldhistory.model;

/**
 * 可追溯到字段变更日志的历史名称。
 *
 * @param fieldId    当前字段 ID；历史名只会关联仍存在于当前项目的字段
 * @param value      历史名称、显示名或别名原值；对外返回前仍需经过统一脱敏
 * @param changeLogId 提供该历史值的变更日志 ID
 */
public record FieldHistoricalAlias(
        Long fieldId,
        String value,
        Long changeLogId
) {

    /**
     * 返回不包含快照原文的只读证据定位符。
     */
    public String evidenceRef() {
        return "dataspec://change-logs/" + changeLogId;
    }
}
