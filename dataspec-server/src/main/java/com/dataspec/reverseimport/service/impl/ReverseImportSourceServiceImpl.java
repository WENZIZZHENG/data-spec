package com.dataspec.reverseimport.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.field.entity.Field;
import com.dataspec.reverseimport.entity.ReverseImportDecision;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.model.DatabaseImportReq;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.FieldSourceDetail;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.reverseimport.repository.ReverseImportDecisionRepository;
import com.dataspec.reverseimport.repository.ReverseImportBatchRepository;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库反向导入来源追踪服务实现。
 */
@Service
@RequiredArgsConstructor
public class ReverseImportSourceServiceImpl implements ReverseImportSourceService {

    private final ReverseImportBatchRepository batchRepository;
    private final FieldSourceRepository fieldSourceRepository;
    private final ReverseImportDecisionRepository decisionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ReverseImportBatch createDatabaseBatch(DatabaseImportReq req, int importedCount, int skippedCount) {
        ReverseImportBatch batch = new ReverseImportBatch();
        batch.setProjectId(req.getProjectId());
        batch.setSourceType(SOURCE_TYPE_DATABASE);
        batch.setDatabaseType(req.getDatabaseType());
        batch.setDatabaseName(req.getDatabaseName());
        batch.setSchemaName(req.getSchemaName());
        batch.setTableNamesJson(writeJson(req.getTableNames()));
        batch.setImportedCount(importedCount);
        batch.setSkippedCount(skippedCount);
        batch.setOperatorName(DataSpecSecurityContext.currentOperator());
        batch.setCreatedAt(LocalDateTime.now());
        batchRepository.insert(batch);
        return batch;
    }

    @Override
    public void recordFieldSource(ReverseImportBatch batch, Field field, FieldCandidate candidate) {
        FieldSource source = new FieldSource();
        source.setProjectId(field.getProjectId());
        source.setFieldId(field.getId());
        source.setBatchId(batch.getId());
        source.setSourceType(SOURCE_TYPE_DATABASE);
        source.setSchemaName(batch.getSchemaName());
        source.setTableName(candidate.getTableName());
        source.setColumnName(candidate.getColumnName());
        source.setDataType(candidate.getDataType());
        source.setNullable(candidate.getNullable());
        source.setDefaultValue(candidate.getDefaultValue());
        source.setComment(candidate.getComment());
        source.setMetadataJson(writeJson(fieldSourceMetadata(candidate)));
        source.setCreatedAt(LocalDateTime.now());
        fieldSourceRepository.insert(source);
    }

    @Override
    public void recordMappingDecisions(ReverseImportBatch batch, List<ReverseImportDecision> decisions) {
        if (batch == null || decisions == null || decisions.isEmpty()) {
            return;
        }
        for (ReverseImportDecision decision : decisions) {
            if (decision == null || decision.getColumnName() == null || decision.getColumnName().isBlank()) {
                continue;
            }
            // 决策记录必须绑定真实批次，避免前端或旧客户端漏传上下文时形成游离历史。
            decision.setProjectId(batch.getProjectId());
            decision.setBatchId(batch.getId());
            decision.setSourceType(SOURCE_TYPE_DATABASE);
            if (decision.getSchemaName() == null || decision.getSchemaName().isBlank()) {
                decision.setSchemaName(batch.getSchemaName());
            }
            decision.setCreatedAt(LocalDateTime.now());
            decisionRepository.insert(decision);
        }
    }

    @Override
    public List<ReverseImportDecision> listDecisions(Long projectId, Long batchId, Integer limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        if (batchId != null) {
            return decisionRepository.findByBatchId(batchId).stream()
                    .filter(decision -> projectId.equals(decision.getProjectId()))
                    .toList();
        }
        return decisionRepository.findRecentByProjectId(projectId, limit == null ? 50 : limit);
    }

    @Override
    public List<FieldSourceDetail> listByFieldId(Long fieldId) {
        return fieldSourceRepository.findByFieldId(fieldId).stream()
                .map(source -> new FieldSourceDetail(
                        source,
                        batchRepository.findById(source.getBatchId()).orElse(null)))
                .toList();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("反向导入来源序列化失败: " + e.getMessage());
        }
    }

    private Map<String, Object> fieldSourceMetadata(FieldCandidate candidate) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putMetadata(metadata, "tableName", candidate.getTableName());
        putMetadata(metadata, "columnName", candidate.getColumnName());
        putMetadata(metadata, "dataType", candidate.getDataType());
        putMetadata(metadata, "nullable", candidate.getNullable());
        putMetadata(metadata, "defaultValue", candidate.getDefaultValue());
        putMetadata(metadata, "comment", candidate.getComment());
        return metadata;
    }

    private void putMetadata(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, SensitiveDataSanitizer.sanitizeValue(value));
        }
    }
}
