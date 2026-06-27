package com.dataspec.lint.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.perf.PerformanceProbe;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.SqlCheckReplay;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.dto.StandardSnapshotPayload;
import com.dataspec.standard.service.StandardSnapshotService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQL 检查记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlCheckRecordServiceImpl implements SqlCheckRecordService {

    private static final long LINT_RECORD_PAGE_WARN_MS = 500;

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
        return PerformanceProbe.measure("lint-record.page", LINT_RECORD_PAGE_WARN_MS,
                "SQL 检查记录分页变慢时优先检查 project_id/created_at 索引和分页大小",
                () -> sqlCheckRecordRepository.findByProjectId(projectId, current, size));
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

    @Override
    public SqlCheckReplay buildReplay(SqlCheckRecord record) {
        if (record == null || record.getProjectId() == null || record.getStandardSnapshotId() == null) {
            StandardSnapshotInfo unversioned = StandardSnapshotInfo.unversioned(record == null ? null : record.getProjectId());
            return new SqlCheckReplay(
                    unversioned,
                    currentSnapshotOrUnversioned(record),
                    "unversioned",
                    new SqlCheckReplay.Summary(false, 0, 0, 0, null),
                    List.of("该检查记录未绑定标准快照，只能查看原始 SQL 与当时保存的问题。"));
        }

        StandardSnapshotInfo current = currentSnapshotOrUnversioned(record);
        try {
            StandardSnapshotPayload payload = standardSnapshotService.getSnapshotPayload(record.getProjectId(), record.getStandardSnapshotId());
            boolean sameAsCurrent = Objects.equals(payload.standard().specHash(), current.specHash());
            String exportCommand = "dataspec export-context --project " + record.getProjectId()
                    + " --snapshot-id " + record.getStandardSnapshotId()
                    + " --output dataspec-ai-context-snapshot-" + record.getStandardSnapshotId() + ".zip";
            List<String> nextActions = new ArrayList<>();
            nextActions.add(exportCommand);
            nextActions.add(sameAsCurrent
                    ? "当前标准与记录快照一致，可直接复查或应用 fixedSql。"
                    : "当前标准已变化，建议先导出历史 Context 再判断是否按当前标准重跑。");
            return new SqlCheckReplay(
                    payload.standard(),
                    current,
                    sameAsCurrent ? "current" : "historical",
                    new SqlCheckReplay.Summary(
                            sameAsCurrent,
                            payload.fieldCount(),
                            payload.enumCount(),
                            payload.ruleCount(),
                            exportCommand),
                    nextActions);
        } catch (Exception e) {
            StandardSnapshotInfo recorded = new StandardSnapshotInfo(
                    record.getStandardSnapshotId(),
                    record.getProjectId(),
                    record.getStandardSnapshotVersion(),
                    null,
                    null,
                    record.getStandardSnapshotHash(),
                    null,
                    true,
                    "snapshot");
            return new SqlCheckReplay(
                    recorded,
                    current,
                    "missing_snapshot",
                    new SqlCheckReplay.Summary(false, 0, 0, 0, null),
                    List.of("记录引用的标准快照无法读取: " + e.getMessage()));
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

    private StandardSnapshotInfo currentSnapshotOrUnversioned(SqlCheckRecord record) {
        Long projectId = record == null ? null : record.getProjectId();
        if (projectId == null) {
            return StandardSnapshotInfo.unversioned(null);
        }
        try {
            return standardSnapshotService.getCurrentSnapshot(projectId);
        } catch (Exception e) {
            log.warn("检查记录当前标准快照读取失败: {}", e.getMessage());
            return StandardSnapshotInfo.unversioned(projectId);
        }
    }
}
