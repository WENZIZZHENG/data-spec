package com.dataspec.reverseimport.model;

import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.entity.ReverseImportBatch;

/**
 * 字段来源详情，包含字段级来源和对应批次摘要。
 */
public record FieldSourceDetail(FieldSource source, ReverseImportBatch batch) {
}
