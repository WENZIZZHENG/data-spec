package com.dataspec.generator.model;

import com.dataspec.lint.model.LintResult;
import com.dataspec.standard.dto.StandardSnapshotInfo;

/**
 * DDL 生成结果，包含生成文本与同一次生成后的 lint 自检结果。
 */
public record DdlGenerateResult(
        String ddl,
        LintResult lintResult,
        StandardSnapshotInfo standardSnapshot
) {
}
