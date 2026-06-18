package com.dataspec.reverseimport.service;

import com.dataspec.reverseimport.model.ReverseImportPreview;

/**
 * SQL 反向导入预览服务。
 */
public interface ReverseImportService {

    ReverseImportPreview preview(Long projectId, String sql);
}
