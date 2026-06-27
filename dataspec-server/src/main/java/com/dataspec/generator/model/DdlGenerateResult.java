package com.dataspec.generator.model;

import com.dataspec.dialect.model.DialectDiagnostic;
import com.dataspec.lint.model.LintResult;
import com.dataspec.standard.dto.StandardSnapshotInfo;

import java.util.List;

/**
 * DDL 生成结果，包含生成文本与同一次生成后的 lint 自检结果。
 */
public record DdlGenerateResult(
        String ddl,
        LintResult lintResult,
        StandardSnapshotInfo standardSnapshot,
        List<DialectDiagnostic> dialectDiagnostics
) {
}
