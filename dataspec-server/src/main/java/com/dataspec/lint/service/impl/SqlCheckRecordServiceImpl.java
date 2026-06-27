package com.dataspec.lint.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SQL 检查记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlCheckRecordServiceImpl implements SqlCheckRecordService {

    private final SqlCheckRecordRepository sqlCheckRecordRepository;
    private final ObjectMapper objectMapper;
    private final StandardSnapshotService standardSnapshotService;

    @Override
    public SqlCheckRecord save(Long projectId, String originalSql, LintResult result) {
        SqlCheckRecord record = new SqlCheckRecord();
        record.setProjectId(projectId);
        record.setOriginalSql(originalSql);
        record.setFixedSql(result.getFixedSql());
        record.setErrorCount(result.getErrorCount());
        record.setWarningCount(result.getWarningCount());
        record.setSuggestionCount(result.getSuggestionCount());
        record.setIssuesJson(serializeIssues(result.getIssues()));
        attachStandardSnapshot(projectId, record);
        sqlCheckRecordRepository.insert(record);
        return record;
    }

    @Override
    public IPage<SqlCheckRecord> listByProject(Long projectId, int current, int size) {
        return sqlCheckRecordRepository.findByProjectId(projectId, current, size);
    }

    @Override
    public SqlCheckRecord getById(Long id) {
        return sqlCheckRecordRepository.findById(id)
                .orElseThrow(() -> new BizException("检查记录不存在: " + id));
    }

    @Override
    public List<LintIssue> parseIssues(SqlCheckRecord record) {
        if (record == null || record.getIssuesJson() == null || record.getIssuesJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(record.getIssuesJson(),
                    new TypeReference<List<LintIssue>>() {});
        } catch (Exception e) {
            log.warn("检查记录 {} issues 反序列化失败: {}", record.getId(), e.getMessage());
            return List.of();
        }
    }

    private String serializeIssues(List<LintIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(issues);
        } catch (Exception e) {
            log.warn("检查记录 issues 序列化失败: {}", e.getMessage());
            return "[]";
        }
    }

    private void attachStandardSnapshot(Long projectId, SqlCheckRecord record) {
        if (projectId == null) {
            return;
        }
        try {
            StandardSnapshotInfo snapshot = standardSnapshotService.getCurrentSnapshot(projectId);
            if (snapshot.versioned()) {
                record.setStandardSnapshotId(snapshot.snapshotId());
                record.setStandardSnapshotVersion(snapshot.specVersion());
                record.setStandardSnapshotHash(snapshot.specHash());
            }
        } catch (Exception e) {
            log.warn("检查记录标准快照引用失败: {}", e.getMessage());
        }
    }
}
