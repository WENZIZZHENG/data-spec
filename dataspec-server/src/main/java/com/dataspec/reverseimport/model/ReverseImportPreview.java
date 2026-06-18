package com.dataspec.reverseimport.model;

import com.dataspec.lint.model.TableDef;
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
    private List<MissingCommentIssue> missingComments = new ArrayList<>();
    private List<NonStandardField> nonStandardFields = new ArrayList<>();
}
