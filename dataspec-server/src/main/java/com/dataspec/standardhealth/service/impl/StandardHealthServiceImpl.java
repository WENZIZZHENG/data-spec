package com.dataspec.standardhealth.service.impl;

import com.dataspec.aifeedback.model.AiFeedbackAction;
import com.dataspec.aifeedback.model.AiFeedbackReport;
import com.dataspec.aifeedback.model.AiFeedbackSummary;
import com.dataspec.aifeedback.service.AiFeedbackService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.model.FieldQualitySummary;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardhealth.entity.StandardHealthSnapshot;
import com.dataspec.standardhealth.model.StandardHealthAction;
import com.dataspec.standardhealth.model.StandardHealthCoverageInput;
import com.dataspec.standardhealth.model.StandardHealthDelta;
import com.dataspec.standardhealth.model.StandardHealthMetrics;
import com.dataspec.standardhealth.model.StandardHealthPlan;
import com.dataspec.standardhealth.model.StandardHealthSnapshotCreateReq;
import com.dataspec.standardhealth.model.StandardHealthSnapshotView;
import com.dataspec.standardhealth.model.StandardHealthTrend;
import com.dataspec.standardhealth.repository.StandardHealthSnapshotRepository;
import com.dataspec.standardhealth.service.StandardHealthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目标准健康快照服务。第一版只保存统计和元数据，不保存业务 SQL、连接串或数据行。
 */
@Service
@RequiredArgsConstructor
public class StandardHealthServiceImpl implements StandardHealthService {

    private static final List<String> PENDING_CANDIDATE_STATUSES = List.of("PENDING", "POSTPONED");
    private static final List<String> ADOPTED_CANDIDATE_STATUSES = List.of("ACCEPTED", "MERGED");
    private static final TypeReference<List<StandardHealthAction>> ACTION_LIST_TYPE = new TypeReference<>() {
    };

    private final FieldQualityService fieldQualityService;
    private final AiFeedbackService aiFeedbackService;
    private final StandardCandidateRepository standardCandidateRepository;
    private final StandardHealthSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StandardHealthSnapshotView createSnapshot(StandardHealthSnapshotCreateReq req) {
        Long projectId = req == null ? null : req.getProjectId();
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);

        FieldQualityReport qualityReport = fieldQualityService.report(projectId);
        AiFeedbackReport feedbackReport = aiFeedbackService.buildReport(projectId);
        int pendingCandidates = standardCandidateRepository.countByStatuses(projectId, PENDING_CANDIDATE_STATUSES);
        int adoptedCandidates = standardCandidateRepository.countByStatuses(projectId, ADOPTED_CANDIDATE_STATUSES);

        StandardHealthCoverageInput coverage = req.getCoverage();
        List<StandardHealthAction> actions = buildTopActions(qualityReport, feedbackReport, coverage, pendingCandidates);
        StandardHealthSnapshot snapshot = buildSnapshot(projectId, qualityReport, feedbackReport, coverage, pendingCandidates, adoptedCandidates, actions);
        snapshotRepository.insert(snapshot);
        return toView(snapshot);
    }

    @Override
    public StandardHealthTrend trend(Long projectId, Integer limit) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);

        List<StandardHealthSnapshotView> views = snapshotRepository.findRecentByProjectId(projectId, limit == null ? 30 : limit)
                .stream()
                .map(this::toView)
                .toList();
        StandardHealthTrend trend = new StandardHealthTrend();
        trend.setProjectId(projectId);
        trend.setSnapshots(views);
        if (views.isEmpty()) {
            trend.setNextActions(List.of("创建第一条标准健康快照", "运行字段质量报告和覆盖率报告后再采集趋势"));
            return trend;
        }

        StandardHealthSnapshotView latest = views.getFirst();
        trend.setLatest(latest);
        trend.setWeekDelta(delta(latest, baseline(views, latest, 7), 7));
        trend.setMonthDelta(delta(latest, baseline(views, latest, 30), 30));
        trend.setNextActions(latest.getTopActions().stream()
                .map(StandardHealthAction::title)
                .limit(3)
                .toList());
        return trend;
    }

    @Override
    public StandardHealthPlan plan(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        StandardHealthSnapshot latest = snapshotRepository.findLatestByProjectId(projectId)
                .orElseThrow(() -> new BizException("暂无标准健康快照"));
        StandardHealthSnapshotView view = toView(latest);
        StandardHealthPlan plan = new StandardHealthPlan();
        plan.setProjectId(projectId);
        plan.setSnapshotId(view.getId());
        plan.setMarkdown(view.getPlanMarkdown());
        plan.setTopActions(view.getTopActions());
        plan.setNextActions(List.of("按 HIGH 到 LOW 顺序执行 Top actions", "维护后重新创建健康快照观察趋势"));
        return plan;
    }

    private StandardHealthSnapshot buildSnapshot(
            Long projectId,
            FieldQualityReport qualityReport,
            AiFeedbackReport feedbackReport,
            StandardHealthCoverageInput coverage,
            int pendingCandidates,
            int adoptedCandidates,
            List<StandardHealthAction> actions
    ) {
        FieldQualitySummary quality = qualityReport.getSummary();
        AiFeedbackSummary feedback = feedbackReport.summary();
        StandardHealthSnapshot snapshot = new StandardHealthSnapshot();
        snapshot.setProjectId(projectId);
        snapshot.setCapturedAt(LocalDateTime.now());
        snapshot.setSource("MANUAL");
        snapshot.setAverageQualityScore(quality.getAverageScore());
        snapshot.setLowQualityFieldCount(quality.getLowQualityCount());
        snapshot.setTotalFieldCount(quality.getTotalFieldCount());
        snapshot.setCoverageStatus(coverage == null ? "not_collected" : "collected");
        snapshot.setCoverageRate(coverage == null ? null : coverage.getCoverageRate());
        snapshot.setUnmanagedFieldCount(coverage == null ? 0 : safeInt(coverage.getUnmanagedFieldCount()));
        snapshot.setMissingCommentCount(coverage == null ? 0 : safeInt(coverage.getMissingCommentCount()));
        snapshot.setPossibleDuplicateCount(coverage == null ? 0 : safeInt(coverage.getPossibleDuplicateCount()));
        snapshot.setRuleIssueCount(feedback.ruleSignalCount());
        snapshot.setRuleExemptionCount(feedback.ruleExemptionCount());
        snapshot.setAiFeedbackSignalCount(feedback.fieldSignalCount() + feedback.ruleSignalCount());
        snapshot.setPendingCandidateCount(pendingCandidates);
        snapshot.setAdoptedCandidateCount(adoptedCandidates);
        snapshot.setFixedSqlAvailableCount(feedback.fixedSqlAvailableCount());
        snapshot.setTopActionsJson(toJson(actions));
        snapshot.setPayloadJson(toJson(sanitizedPayload(snapshot, coverage, actions)));
        return snapshot;
    }

    private List<StandardHealthAction> buildTopActions(
            FieldQualityReport qualityReport,
            AiFeedbackReport feedbackReport,
            StandardHealthCoverageInput coverage,
            int pendingCandidates
    ) {
        List<StandardHealthAction> actions = new ArrayList<>();
        FieldQualitySummary quality = qualityReport.getSummary();
        if (quality.getLowQualityCount() > 0) {
            actions.add(new StandardHealthAction(
                    "补字段注释和别名",
                    "当前有 " + quality.getLowQualityCount() + " 个低质量字段，优先补注释、别名、示例和分类。",
                    "HIGH",
                    "/field-quality",
                    "quality.lowQualityCount=" + quality.getLowQualityCount()
            ));
        }
        if (coverage == null) {
            actions.add(new StandardHealthAction(
                    "采集覆盖率报告",
                    "本次快照未包含覆盖率，先从 SQL、数据库 metadata 或 dump 生成覆盖率报告。",
                    "MEDIUM",
                    "/field-coverage",
                    "coverageStatus=not_collected"
            ));
        } else if (safeInt(coverage.getUnmanagedFieldCount()) > 0) {
            actions.add(new StandardHealthAction(
                    "收敛未纳管字段",
                    "覆盖率报告发现 " + coverage.getUnmanagedFieldCount() + " 个未纳管字段，优先处理 Top 未纳管字段。",
                    "HIGH",
                    "/field-coverage",
                    "coverage.unmanagedFieldCount=" + coverage.getUnmanagedFieldCount()
            ));
        }
        for (AiFeedbackAction action : feedbackReport.nextActions()) {
            actions.add(new StandardHealthAction(
                    action.title(),
                    action.description(),
                    action.priority(),
                    action.targetRoute(),
                    "aiFeedback"
            ));
        }
        if (pendingCandidates > 0) {
            actions.add(new StandardHealthAction(
                    "处理标准候选 Inbox",
                    "当前有 " + pendingCandidates + " 个待处理或延后候选，按置信度采纳、合并或忽略。",
                    "MEDIUM",
                    "/standard-candidates",
                    "candidate.pending=" + pendingCandidates
            ));
        }
        return actions.stream()
                .sorted(Comparator
                        .comparingInt((StandardHealthAction action) -> priorityRank(action.priority()))
                        .thenComparing(StandardHealthAction::title))
                .limit(8)
                .toList();
    }

    private StandardHealthSnapshotView toView(StandardHealthSnapshot snapshot) {
        StandardHealthSnapshotView view = new StandardHealthSnapshotView();
        view.setId(snapshot.getId());
        view.setProjectId(snapshot.getProjectId());
        view.setCapturedAt(snapshot.getCapturedAt());
        view.setSource(snapshot.getSource());
        view.setMetrics(metrics(snapshot));
        view.setTopActions(parseActions(snapshot.getTopActionsJson()));
        view.setPlanMarkdown(buildPlanMarkdown(view));
        return view;
    }

    private StandardHealthMetrics metrics(StandardHealthSnapshot snapshot) {
        StandardHealthMetrics metrics = new StandardHealthMetrics();
        metrics.setAverageQualityScore(safeInt(snapshot.getAverageQualityScore()));
        metrics.setLowQualityFieldCount(safeInt(snapshot.getLowQualityFieldCount()));
        metrics.setTotalFieldCount(safeInt(snapshot.getTotalFieldCount()));
        metrics.setCoverageStatus(snapshot.getCoverageStatus());
        metrics.setCoverageRate(snapshot.getCoverageRate());
        metrics.setUnmanagedFieldCount(safeInt(snapshot.getUnmanagedFieldCount()));
        metrics.setMissingCommentCount(safeInt(snapshot.getMissingCommentCount()));
        metrics.setPossibleDuplicateCount(safeInt(snapshot.getPossibleDuplicateCount()));
        metrics.setRuleIssueCount(safeInt(snapshot.getRuleIssueCount()));
        metrics.setRuleExemptionCount(safeInt(snapshot.getRuleExemptionCount()));
        metrics.setAiFeedbackSignalCount(safeInt(snapshot.getAiFeedbackSignalCount()));
        metrics.setPendingCandidateCount(safeInt(snapshot.getPendingCandidateCount()));
        metrics.setAdoptedCandidateCount(safeInt(snapshot.getAdoptedCandidateCount()));
        metrics.setFixedSqlAvailableCount(safeInt(snapshot.getFixedSqlAvailableCount()));
        return metrics;
    }

    private StandardHealthSnapshotView baseline(List<StandardHealthSnapshotView> views, StandardHealthSnapshotView latest, int days) {
        LocalDateTime threshold = latest.getCapturedAt().minusDays(days);
        return views.stream()
                .filter(item -> !item.getCapturedAt().isAfter(threshold))
                .findFirst()
                .orElse(null);
    }

    private StandardHealthDelta delta(StandardHealthSnapshotView latest, StandardHealthSnapshotView baseline, int days) {
        if (latest == null || baseline == null) {
            return null;
        }
        StandardHealthMetrics current = latest.getMetrics();
        StandardHealthMetrics base = baseline.getMetrics();
        StandardHealthDelta delta = new StandardHealthDelta();
        delta.setBaselineSnapshotId(baseline.getId());
        delta.setDays((int) Duration.between(baseline.getCapturedAt(), latest.getCapturedAt()).toDays());
        if (delta.getDays() <= 0) {
            delta.setDays(days);
        }
        delta.setQualityAverageScoreDelta(current.getAverageQualityScore() - base.getAverageQualityScore());
        delta.setLowQualityFieldCountDelta(current.getLowQualityFieldCount() - base.getLowQualityFieldCount());
        delta.setCoverageRateDelta(doubleDelta(current.getCoverageRate(), base.getCoverageRate()));
        delta.setUnmanagedFieldCountDelta(current.getUnmanagedFieldCount() - base.getUnmanagedFieldCount());
        delta.setSummary("近 " + days + " 天质量分变化 " + signed(delta.getQualityAverageScoreDelta())
                + "，低质量字段变化 " + signed(delta.getLowQualityFieldCountDelta()));
        return delta;
    }

    private Object sanitizedPayload(
            StandardHealthSnapshot snapshot,
            StandardHealthCoverageInput coverage,
            List<StandardHealthAction> actions
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("snapshotId", snapshot.getId());
        payload.put("projectId", snapshot.getProjectId());
        payload.put("coverageStatus", snapshot.getCoverageStatus());
        payload.put("coverageTopUnmanagedFields", coverage == null ? List.of() : coverage.getTopUnmanagedFields());
        payload.put("topActions", actions);
        return SensitiveDataSanitizer.sanitizeValue(payload);
    }

    private String buildPlanMarkdown(StandardHealthSnapshotView view) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# 标准健康改进计划\n\n");
        markdown.append("- 项目 ID: ").append(view.getProjectId()).append('\n');
        markdown.append("- 快照 ID: ").append(view.getId()).append('\n');
        markdown.append("- 质量均分: ").append(view.getMetrics().getAverageQualityScore()).append('\n');
        markdown.append("- 低质量字段: ").append(view.getMetrics().getLowQualityFieldCount()).append('\n');
        markdown.append("- 覆盖率: ").append(view.getMetrics().getCoverageRate() == null ? "未采集" : view.getMetrics().getCoverageRate() + "%").append("\n\n");
        markdown.append("## Top Actions\n");
        if (view.getTopActions().isEmpty()) {
            markdown.append("- 暂无动作，维护后重新采集快照。\n");
            return markdown.toString();
        }
        for (StandardHealthAction action : view.getTopActions()) {
            markdown.append("- [").append(action.priority()).append("] ")
                    .append(action.title()).append("：")
                    .append(action.description()).append('\n');
        }
        return markdown.toString();
    }

    private List<StandardHealthAction> parseActions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, ACTION_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new BizException("标准健康快照 JSON 序列化失败: " + error.getMessage());
        }
    }

    private Double doubleDelta(Double current, Double baseline) {
        if (current == null || baseline == null) {
            return null;
        }
        return Math.round((current - baseline) * 10.0) / 10.0;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int priorityRank(String priority) {
        if ("HIGH".equalsIgnoreCase(priority)) {
            return 0;
        }
        if ("MEDIUM".equalsIgnoreCase(priority)) {
            return 1;
        }
        return 2;
    }

    private String signed(Integer value) {
        if (value == null) {
            return "N/A";
        }
        return value > 0 ? "+" + value : String.valueOf(value);
    }
}
