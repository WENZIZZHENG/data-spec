package com.dataspec.standardchange.model;

/**
 * 保存后的回退辅助提示。
 */
public record StandardChangeRollbackHint(
        String type,
        String action,
        String description,
        String targetPath
) {
}
