package com.dataspec.reverseimport.service;

import com.dataspec.field.entity.Field;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.FieldSourceDetail;

import java.util.List;

/**
 * 数据库反向导入来源追踪服务。
 */
public interface ReverseImportSourceService {

    String SOURCE_TYPE_DATABASE = "database";

    ReverseImportBatch createDatabaseBatch(DatabaseImportReq req, int importedCount, int skippedCount);

    void recordFieldSource(ReverseImportBatch batch, Field field, FieldCandidate candidate);

    void recordMappingDecisions(ReverseImportBatch batch, List<ReverseImportDecision> decisions);

    List<ReverseImportDecision> listDecisions(Long projectId, Long batchId, Integer limit);

    List<FieldSourceDetail> listByFieldId(Long fieldId);
}
