package com.dataspec.activity.service.impl;

import com.dataspec.activity.model.ProjectActivityAction;
import com.dataspec.activity.model.ProjectActivityItem;
import com.dataspec.activity.model.ProjectActivityTimeline;
import com.dataspec.activity.service.ProjectActivityService;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.common.exception.BizException;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.repository.SqlCheckRecordRepository;
import com.dataspec.reverseimport.entity.ReverseImportBatch;
import com.dataspec.reverseimport.repository.ReverseImportBatchRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.repository.ApiTokenRepository;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 项目活动时间线服务实现。
 * <p>
 * 第一版只做只读聚合，不新增审计表；所有 metadata 都必须保持安全摘要。
 */
@Service
@RequiredArgsConstructor
public class ProjectActivityServiceImpl implements ProjectActivityService {

    public static final String FIELD_CHANGE = "FIELD_CHANGE";
    public static final String STANDARD_SNAPSHOT = "STANDARD_SNAPSHOT";
    public static final String REVERSE_IMPORT = "REVERSE_IMPORT";
    public static final String SQL_CHECK = "SQL_CHECK";
    public static final String AI_JOB = "AI_JOB";
    public static final String TOKEN_USAGE = "TOKEN_USAGE";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int SOURCE_FETCH_LIMIT = 100;
    private static final String ALL_PROJECTS = "*";

    private static final List<ProjectActivityAction> ACTIONS = List.of(
            new ProjectActivityAction(FIELD_CHANGE, "标准变更"),
            new ProjectActivityAction(STANDARD_SNAPSHOT, "标准快照"),
            new ProjectActivityAction(REVERSE_IMPORT, "反向导入"),
            new ProjectActivityAction(SQL_CHECK, "SQL 检查"),
            new ProjectActivityAction(AI_JOB, "AI 任务"),
            new ProjectActivityAction(TOKEN_USAGE, "Token 使用")
    );

    private final StandardChangeLogRepository standardChangeLogRepository;
    private final StandardSnapshotRepository standardSnapshotRepository;
    private final ReverseImportBatchRepository reverseImportBatchRepository;
    private final SqlCheckRecordRepository sqlCheckRecordRepository;
    private final AiJobRecordRepository aiJobRecordRepository;
    private final ApiTokenRepository apiTokenRepository;

    @Override
    public ProjectActivityTimeline listActivities(Long projectId, String actionType, Integer limit) {
        if (projectId == null) {
            throw new BizException(400, "项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);

        String normalizedActionType = normalizeActionType(actionType);
        int safeLimit = safeLimit(limit);
        List<ProjectActivityItem> activities = new ArrayList<>();
        addFieldChanges(projectId, activities);
        addSnapshots(projectId, activities);
        addReverseImports(projectId, activities);
        addSqlChecks(projectId, activities);
        addAiJobs(projectId, activities);
        addTokenUsage(projectId, activities);

        return new ProjectActivityTimeline(
                projectId,
                ACTIONS,
                activities.stream()
                        .filter(item -> normalizedActionType == null || normalizedActionType.equals(item.actionType()))
                        .sorted(this::compareByOccurredAtDesc)
                        .limit(safeLimit)
                        .toList(),
                LocalDateTime.now());
    }

    private void addFieldChanges(Long projectId, List<ProjectActivityItem> activities) {
        for (StandardChangeLog log : standardChangeLogRepository.findByProjectId(projectId, SOURCE_FETCH_LIMIT)) {
            activities.add(new ProjectActivityItem(
                    "field-change:" + log.getId(),
                    FIELD_CHANGE,
                    "标准变更：" + displayText(log.getTargetType(), "未知类型") + " " + displayText(log.getAction(), "update"),
                    "目标 ID " + log.getTargetId(),
                    log.getChangedAt(),
                    displayText(log.getOperatorName(), "未知操作者"),
                    "标准库",
                    "delete".equalsIgnoreCase(log.getAction()) ? "WARNING" : "INFO",
                    "/fields?changeLogId=" + log.getId(),
                    safeMap(
                            "logId", log.getId(),
                            "targetType", log.getTargetType(),
                            "targetId", log.getTargetId(),
                            "action", log.getAction()
                    )));
        }
    }

    private void addSnapshots(Long projectId, List<ProjectActivityItem> activities) {
        for (StandardSnapshot snapshot : standardSnapshotRepository.findRecentByProjectId(projectId, SOURCE_FETCH_LIMIT)) {
            activities.add(new ProjectActivityItem(
                    "standard-snapshot:" + snapshot.getId(),
                    STANDARD_SNAPSHOT,
                    "创建标准快照：" + displayText(snapshot.getVersion(), "未命名版本"),
                    displayText(snapshot.getName(), snapshot.getDescription(), "标准快照已保存"),
                    snapshot.getCreatedAt(),
                    "DataSpec",
                    "标准快照",
                    "INFO",
                    "/standard-snapshots?snapshotId=" + snapshot.getId(),
                    safeMap(
                            "snapshotId", snapshot.getId(),
                            "version", snapshot.getVersion(),
                            "snapshotHash", snapshot.getSnapshotHash()
                    )));
        }
    }

    private void addReverseImports(Long projectId, List<ProjectActivityItem> activities) {
        for (ReverseImportBatch batch : reverseImportBatchRepository.findRecentByProjectId(projectId, SOURCE_FETCH_LIMIT)) {
            int importedCount = count(batch.getImportedCount());
            int skippedCount = count(batch.getSkippedCount());
            activities.add(new ProjectActivityItem(
                    "reverse-import:" + batch.getId(),
                    REVERSE_IMPORT,
                    "数据库反向导入：" + displayText(batch.getDatabaseName(), batch.getSchemaName(), "未知数据源"),
                    "导入 " + importedCount + " 个字段，跳过 " + skippedCount + " 个字段",
                    batch.getCreatedAt(),
                    displayText(batch.getOperatorName(), "未知操作者"),
                    displayText(batch.getSourceType(), "反向导入"),
                    skippedCount > 0 ? "WARNING" : "INFO",
                    "/reverse-import?batchId=" + batch.getId(),
                    safeMap(
                            "batchId", batch.getId(),
                            "sourceType", batch.getSourceType(),
                            "databaseType", batch.getDatabaseType(),
                            "databaseName", batch.getDatabaseName(),
                            "schemaName", batch.getSchemaName(),
                            "importedCount", importedCount,
                            "skippedCount", skippedCount
                    )));
        }
    }

    private void addSqlChecks(Long projectId, List<ProjectActivityItem> activities) {
        for (SqlCheckRecord record : sqlCheckRecordRepository.findRecentByProjectId(projectId, SOURCE_FETCH_LIMIT)) {
            int errorCount = count(record.getErrorCount());
            int warningCount = count(record.getWarningCount());
            int suggestionCount = count(record.getSuggestionCount());
            int issueCount = errorCount + warningCount + suggestionCount;
            activities.add(new ProjectActivityItem(
                    "sql-check:" + record.getId(),
                    SQL_CHECK,
                    "SQL 检查完成",
                    "发现 " + issueCount + " 个问题（错误 " + errorCount + "，警告 " + warningCount + "，建议 " + suggestionCount + "）",
                    record.getCreatedAt(),
                    "DataSpec",
                    "SQL 校验",
                    sqlSeverity(errorCount, warningCount),
                    "/sql-lint?recordId=" + record.getId(),
                    safeMap(
                            "recordId", record.getId(),
                            "errorCount", errorCount,
                            "warningCount", warningCount,
                            "suggestionCount", suggestionCount,
                            "issueCount", issueCount,
                            "standardSnapshotId", record.getStandardSnapshotId(),
                            "standardSnapshotVersion", record.getStandardSnapshotVersion()
                    )));
        }
    }

    private void addAiJobs(Long projectId, List<ProjectActivityItem> activities) {
        for (AiJobRecord record : aiJobRecordRepository.findRecentByProjectId(projectId, SOURCE_FETCH_LIMIT)) {
            activities.add(new ProjectActivityItem(
                    "ai-job:" + record.getId(),
                    AI_JOB,
                    "AI 任务：" + displayText(record.getTitle(), record.getJobType(), "未命名任务"),
                    displayText(record.getInputSummary(), "AI 任务已记录，可回放输入输出摘要"),
                    record.getCreatedAt(),
                    "AI/用户",
                    displayText(record.getJobType(), "AI"),
                    aiSeverity(record.getStatus()),
                    "/ai-replay?id=" + record.getId(),
                    safeMap(
                            "recordId", record.getId(),
                            "jobType", record.getJobType(),
                            "status", record.getStatus(),
                            "promptVersion", record.getPromptVersion(),
                            "sqlCheckRecordId", record.getSqlCheckRecordId(),
                            "standardSnapshotId", record.getStandardSnapshotId(),
                            "standardSnapshotVersion", record.getStandardSnapshotVersion()
                    )));
        }
    }

    private void addTokenUsage(Long projectId, List<ProjectActivityItem> activities) {
        // token 名称、操作者和授权范围属于管理元数据；项目级 token 用户只能看到业务活动，不暴露 token 使用摘要。
        if (!DataSpecSecurityContext.get().allProjects()) {
            return;
        }
        for (ApiToken token : apiTokenRepository.findAllActiveRows()) {
            if (token.getLastUsedAt() == null || !tokenCanAccessProject(token, projectId)) {
                continue;
            }
            boolean allProjects = isAllProjects(token.getProjectIds());
            activities.add(new ProjectActivityItem(
                    "token-usage:" + token.getId(),
                    TOKEN_USAGE,
                    "API Token 使用：" + displayText(token.getName(), "未命名 token"),
                    "最近一次认证成功",
                    token.getLastUsedAt(),
                    displayText(token.getOperatorName(), "未知操作者"),
                    "API Token",
                    "INFO",
                    "/tokens?tokenId=" + token.getId(),
                    safeMap(
                            "tokenId", token.getId(),
                            "tokenName", token.getName(),
                            "operatorName", token.getOperatorName(),
                            "allProjects", allProjects,
                            "scope", allProjects ? "ALL_PROJECTS" : "SCOPED"
                    )));
        }
    }

    private boolean tokenCanAccessProject(ApiToken token, Long projectId) {
        String text = token.getProjectIds();
        if (isAllProjects(text)) {
            return true;
        }
        for (String part : text.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            try {
                if (Long.parseLong(trimmed) == projectId) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean isAllProjects(String projectIds) {
        return projectIds == null || projectIds.isBlank() || ALL_PROJECTS.equals(projectIds.trim());
    }

    private String normalizeActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return null;
        }
        String normalized = actionType.trim().toUpperCase(Locale.ROOT);
        boolean known = ACTIONS.stream().anyMatch(action -> action.actionType().equals(normalized));
        if (!known) {
            throw new BizException(400, "不支持的活动类型: " + actionType);
        }
        return normalized;
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private int compareByOccurredAtDesc(ProjectActivityItem left, ProjectActivityItem right) {
        if (left.occurredAt() == null && right.occurredAt() == null) {
            return right.id().compareTo(left.id());
        }
        if (left.occurredAt() == null) {
            return 1;
        }
        if (right.occurredAt() == null) {
            return -1;
        }
        int timeCompare = right.occurredAt().compareTo(left.occurredAt());
        return timeCompare != 0 ? timeCompare : right.id().compareTo(left.id());
    }

    private String sqlSeverity(int errorCount, int warningCount) {
        if (errorCount > 0) {
            return "ERROR";
        }
        return warningCount > 0 ? "WARNING" : "INFO";
    }

    private String aiSeverity(String status) {
        if (status == null || status.isBlank() || "SUCCESS".equalsIgnoreCase(status)) {
            return "INFO";
        }
        return "FAILED".equalsIgnoreCase(status) ? "ERROR" : "WARNING";
    }

    private int count(Integer value) {
        return value != null ? value : 0;
    }

    private String displayText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String displayText(String first, String second, String fallback) {
        String firstText = displayText(first, "");
        if (!firstText.isBlank()) {
            return firstText;
        }
        return displayText(second, fallback);
    }

    private Map<String, Object> safeMap(Object... keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            Object key = keyValues[index];
            Object value = keyValues[index + 1];
            if (key != null && value != null) {
                result.put(String.valueOf(key), value);
            }
        }
        return result;
    }
}
