package com.dataspec.reverseimport.model;

import com.dataspec.dialect.model.DialectDiagnostic;
import com.dataspec.lint.model.TableDef;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 反向导入预览结果。
 */
@Data
public class ReverseImportPreview {

    private ReverseImportSummary summary = new ReverseImportSummary();
    private List<TableDef> tables = new ArrayList<>();
    private List<FieldCandidate> fieldCandidates = new ArrayList<>();
    private List<ReverseImportDecision> mappingDecisions = new ArrayList<>();
    private List<MissingCommentIssue> missingComments = new ArrayList<>();
    private List<NonStandardField> nonStandardFields = new ArrayList<>();
    private List<DialectDiagnostic> dialectDiagnostics = new ArrayList<>();
}
