package com.dataspec.aitaskrecommendation.service.impl;

import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationItem;
import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationReport;
import com.dataspec.aitaskrecommendation.model.AiTaskRecommendationSummary;
import com.dataspec.aitaskrecommendation.service.AiTaskRecommendationService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardhealth.model.StandardHealthAction;
import com.dataspec.standardhealth.model.StandardHealthSnapshotView;
import com.dataspec.standardhealth.model.StandardHealthTrend;
import com.dataspec.standardhealth.service.StandardHealthService;
import com.dataspec.standardqualitygate.model.QualityGateCheckResult;
import com.dataspec.standardqualitygate.model.StandardQualityGateEvaluateReq;
import com.dataspec.standardqualitygate.model.StandardQualityGateResult;
import com.dataspec.standardqualitygate.service.StandardQualityGateService;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapItem;
import com.dataspec.standardusageheatmap.model.StandardUsageHeatmapReport;
import com.dataspec.standardusageheatmap.service.StandardUsageHeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 将已有只读诊断信号编排为 AI 可执行的下一步任务队列。
 */
@Service
@RequiredArgsConstructor
public class AiTaskRecommendationServiceImpl implements AiTaskRecommendationService {

    private static final int MAX_TASK_COUNT = 8;
    private static final List<String> ACTIVE_CANDIDATE_STATUSES = List.of("PENDING", "POSTPONED");

    private final StandardHealthService standardHealthService;
    private final StandardUsageHeatmapService standardUsageHeatmapService;
    private final StandardCandidateRepository standardCandidateRepository;
    private final StandardQualityGateService standardQualityGateService;

    @Override
    @Transactional(readOnly = true)
    public AiTaskRecommendationReport report(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);

        List<AiTaskRecommendationItem> items = new ArrayList<>();
        addQualityGateTask(projectId, items);
        addHeatmapTask(projectId, items);
        addCandidateTask(projectId, items);
        addHealthTasks(projectId, items);
        addFallbackTasks(projectId, items);

        List<AiTaskRecommendationItem> sorted = items.stream()
                .sorted(Comparator
                        .comparingInt((AiTaskRecommendationItem item) -> priorityRank(item.priority()))
                        .thenComparing(AiTaskRecommendationItem::taskType)
                        .thenComparing(AiTaskRecommendationItem::title))
                .limit(MAX_TASK_COUNT)
                .toList();
        return new AiTaskRecommendationReport(projectId, summary(sorted), sorted);
    }

    private void addQualityGateTask(Long projectId, List<AiTaskRecommendationItem> items) {
        StandardQualityGateEvaluateReq req = new StandardQualityGateEvaluateReq();
        req.setProjectId(projectId);
        StandardQualityGateResult result = standardQualityGateService.evaluate(req);
        List<QualityGateCheckResult> failedChecks = result == null || result.getFailedChecks() == null
                ? List.of()
                : result.getFailedChecks();
        if (failedChecks.isEmpty()) {
            return;
        }
        QualityGateCheckResult first = failedChecks.getFirst();
        String priority = failedChecks.stream().anyMatch(this::isErrorGateCheck) ? "HIGH" : "MEDIUM";
        items.add(new AiTaskRecommendationItem(
                "FIX_QUALITY_GATE",
                priority,
                "修复质量门禁失败项",
                safeText("当前有 " + failedChecks.size() + " 个质量门禁失败项，优先处理 " + valueOrDefault(first.getLabel(), first.getCode()) + "。"),
                routeOrDefault(first.getRoute(), "/quality-gate"),
                "POST /api/quality-gate/evaluate {\"projectId\":" + projectId + "}",
                evidence("qualityGate.failedChecks=" + failedChecks.size(), "qualityGate.status=" + valueOrDefault(result.getStatus(), "UNKNOWN"), "qualityGate.firstCode=" + valueOrDefault(first.getCode(), "UNKNOWN")),
                "重新评估质量门禁，failedChecks 降为 0"));
    }

    private void addHeatmapTask(Long projectId, List<AiTaskRecommendationItem> items) {
        StandardUsageHeatmapReport report = standardUsageHeatmapService.report(projectId);
        if (report == null || report.summary() == null || report.summary().riskyFieldCount() <= 0) {
            return;
        }
        StandardUsageHeatmapItem first = report.items() == null || report.items().isEmpty() ? null : report.items().getFirst();
        List<String> evidenceRefs = new ArrayList<>();
        evidenceRefs.add("heatmap.riskyFieldCount=" + report.summary().riskyFieldCount());
        evidenceRefs.add("heatmap.averageCleanupPriority=" + report.summary().averageCleanupPriority());
        if (first != null) {
            evidenceRefs.add("heatmap.topField=" + safeText(valueOrDefault(first.name(), "UNKNOWN")));
            evidenceRefs.add("heatmap.topCleanupPriority=" + first.cleanupPriority());
        }
        items.add(new AiTaskRecommendationItem(
                "REPAIR_HOT_STANDARD",
                "HIGH",
                "治理高风险标准热区",
                safeText("当前有 " + report.summary().riskyFieldCount() + " 个高治理优先级热区字段，优先修复高使用冲突或低质量字段。"),
                "/standard-usage/heatmap",
                "GET /api/standard-usage/heatmap?projectId=" + projectId,
                List.copyOf(evidenceRefs),
                "热区报告 riskyFieldCount 降低，或最高 cleanupPriority 低于 70"));
    }

    private void addCandidateTask(Long projectId, List<AiTaskRecommendationItem> items) {
        int pendingCount = standardCandidateRepository.countByStatuses(projectId, ACTIVE_CANDIDATE_STATUSES);
        if (pendingCount <= 0) {
            return;
        }
        items.add(new AiTaskRecommendationItem(
                "REVIEW_STANDARD_CANDIDATES",
                pendingCount >= 10 ? "HIGH" : "MEDIUM",
                "处理标准候选 Inbox",
                "当前有 " + pendingCount + " 个待处理或延后候选，按置信度采纳、合并、忽略或继续延后。",
                "/standard-candidates",
                "GET /api/standard-candidates?projectId=" + projectId + "&status=PENDING",
                evidence("candidate.activeCount=" + pendingCount, "candidate.statuses=PENDING,POSTPONED"),
                "候选状态不再为 PENDING/POSTPONED，或 activeCount 明显降低"));
    }

    private void addHealthTasks(Long projectId, List<AiTaskRecommendationItem> items) {
        StandardHealthTrend trend = standardHealthService.trend(projectId, 1);
        StandardHealthSnapshotView latest = trend == null ? null : trend.getLatest();
        List<StandardHealthAction> actions = latest == null || latest.getTopActions() == null
                ? List.of()
                : latest.getTopActions();
        for (StandardHealthAction action : actions) {
            if (action == null) {
                continue;
            }
            String taskType = healthTaskType(action.title());
            if (hasTaskType(items, taskType)) {
                continue;
            }
            String targetRoute = routeOrDefault(action.targetRoute(), "/standard-health");
            items.add(new AiTaskRecommendationItem(
                    taskType,
                    normalizePriority(action.priority()),
                    safeText(valueOrDefault(action.title(), "执行标准健康建议")),
                    safeText(valueOrDefault(action.description(), "根据标准健康快照处理下一步建议。")),
                    targetRoute,
                    "OPEN " + appendProjectId(targetRoute, projectId),
                    evidence("standardHealth.action=" + safeText(valueOrDefault(action.evidence(), "topAction"))),
                    "完成该健康建议后重新生成标准健康快照或刷新趋势"));
        }
    }

    private void addFallbackTasks(Long projectId, List<AiTaskRecommendationItem> items) {
        addFallbackIfNeeded(items, new AiTaskRecommendationItem(
                "RUN_QUALITY_REPORT",
                "LOW",
                "运行字段质量报告",
                "当前高风险信号不足，先刷新字段质量报告，确认是否存在缺注释、缺别名或敏感标记缺口。",
                "/field-quality",
                "OPEN /field-quality?projectId=" + projectId,
                evidence("fallback.source=fieldQuality"),
                "字段质量报告已刷新，并确认 lowQualityCount 是否为 0"));
        addFallbackIfNeeded(items, new AiTaskRecommendationItem(
                "EXPORT_AI_CONTEXT",
                "LOW",
                "导出最小 AI Context",
                "为下一次 AI 协作准备当前项目的最小标准上下文，避免使用过期字段目录。",
                "/ai-context",
                "OPEN /ai-context?projectId=" + projectId,
                evidence("fallback.source=aiContext"),
                "AI Context 预览或导出完成，且 manifest 中 projectId 与当前项目一致"));
        addFallbackIfNeeded(items, new AiTaskRecommendationItem(
                "CREATE_HEALTH_SNAPSHOT",
                "LOW",
                "刷新标准健康快照",
                "没有足够诊断信号时，先创建健康快照作为后续推荐队列的基线。",
                "/standard-health",
                "OPEN /standard-health?projectId=" + projectId,
                evidence("fallback.source=standardHealth"),
                "标准健康快照已刷新，下一次推荐能引用 latest snapshot"));
    }

    private void addFallbackIfNeeded(List<AiTaskRecommendationItem> items, AiTaskRecommendationItem fallback) {
        if (items.size() >= 3 || hasTaskType(items, fallback.taskType())) {
            return;
        }
        items.add(fallback);
    }

    private AiTaskRecommendationSummary summary(List<AiTaskRecommendationItem> items) {
        int high = 0;
        int medium = 0;
        int low = 0;
        Set<String> sources = new LinkedHashSet<>();
        for (AiTaskRecommendationItem item : items) {
            String priority = normalizePriority(item.priority());
            if ("HIGH".equals(priority)) {
                high += 1;
            } else if ("MEDIUM".equals(priority)) {
                medium += 1;
            } else {
                low += 1;
            }
            for (String evidenceRef : item.evidenceRefs()) {
                int dot = evidenceRef == null ? -1 : evidenceRef.indexOf('.');
                if (dot > 0) {
                    sources.add(evidenceRef.substring(0, dot));
                }
            }
        }
        return new AiTaskRecommendationSummary(items.size(), high, medium, low, sources.size());
    }

    private boolean isErrorGateCheck(QualityGateCheckResult check) {
        return check != null
                && ("ERROR".equalsIgnoreCase(check.getSeverity()) || "FAILED".equalsIgnoreCase(check.getStatus()));
    }

    private boolean hasTaskType(List<AiTaskRecommendationItem> items, String taskType) {
        return items.stream().anyMatch(item -> item.taskType().equals(taskType));
    }

    private String appendProjectId(String route, Long projectId) {
        String sanitizedRoute = routeOrDefault(route, "/standard-health");
        String separator = sanitizedRoute.contains("?") ? "&" : "?";
        return sanitizedRoute + separator + "projectId=" + projectId;
    }

    private String healthTaskType(String title) {
        String normalized = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (normalized.contains("快照")) {
            return "CREATE_HEALTH_SNAPSHOT";
        }
        if (normalized.contains("覆盖率")) {
            return "RUN_COVERAGE_REPORT";
        }
        if (normalized.contains("候选")) {
            return "REVIEW_STANDARD_CANDIDATES";
        }
        return "FOLLOW_HEALTH_ACTION";
    }

    private int priorityRank(String priority) {
        return switch (normalizePriority(priority)) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "LOW";
        }
        String normalized = priority.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HIGH", "MEDIUM", "LOW" -> normalized;
            default -> "LOW";
        };
    }

    private String routeOrDefault(String route, String fallback) {
        return route == null || route.isBlank() ? fallback : safeText(route, 160);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeText(String value) {
        return safeText(value, 240);
    }

    private String safeText(String value, int maxLength) {
        String sanitized = SensitiveDataSanitizer.redactText(value, maxLength);
        if (sanitized == null) {
            return null;
        }
        // 推荐队列会被 AI 直接复制阅读，敏感标签本身也不应残留为可误用上下文。
        return sanitized.replaceAll("(?i)\\b(password|passwd|token|authorization|api[_-]?key|secret|dsn)\\b", "凭据");
    }

    private List<String> evidence(String... refs) {
        List<String> result = new ArrayList<>();
        if (refs == null) {
            return result;
        }
        for (String ref : refs) {
            if (ref != null && !ref.isBlank()) {
                result.add(safeText(ref, 160));
            }
        }
        return result;
    }
}
