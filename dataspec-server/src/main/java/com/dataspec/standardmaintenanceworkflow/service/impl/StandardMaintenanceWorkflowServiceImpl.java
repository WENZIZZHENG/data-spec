package com.dataspec.standardmaintenanceworkflow.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.fieldquality.model.FieldQualityIssue;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowEvidenceLink;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowExecutionState;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowInboxAction;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowNextAction;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlan;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlanReq;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowRecipeBinding;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowStep;
import com.dataspec.standardmaintenanceworkflow.service.StandardMaintenanceWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 基于现有诊断信号生成标准维护 workflow dry-run 计划。
 */
@Service
@RequiredArgsConstructor
public class StandardMaintenanceWorkflowServiceImpl implements StandardMaintenanceWorkflowService {

    private static final String RECIPE_ID = "standard-maintenance";
    private static final int RECIPE_VERSION = 1;
    private static final List<String> ACTIVE_CANDIDATE_STATUSES = List.of("PENDING", "POSTPONED");

    private final StandardCandidateRepository standardCandidateRepository;
    private final FieldQualityService fieldQualityService;

    @Override
    @Transactional(readOnly = true)
    public StandardMaintenanceWorkflowPlan plan(StandardMaintenanceWorkflowPlanReq req) {
        if (req == null || req.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(req.getProjectId());

        String sourceType = normalizeSourceType(req.getSourceType());
        return switch (sourceType) {
            case "STANDARD_CANDIDATE" -> candidatePlan(req);
            case "FIELD_QUALITY" -> qualityPlan(req);
            case "FIELD_COVERAGE" -> coveragePlan(req);
            case "AI_TASK_FAILURE" -> genericPlan(req, "RESUME_AI_TASK_FAILURE", "恢复 AI 失败任务", "ai-task-runs");
            default -> genericPlan(req, "PREPARE_STANDARD_MAINTENANCE", "准备标准维护计划", "frontend-task-entrypoints");
        };
    }

    private StandardMaintenanceWorkflowPlan candidatePlan(StandardMaintenanceWorkflowPlanReq req) {
        int requested = safeSize(req.getSourceIds());
        int activeCount = standardCandidateRepository.countByStatuses(req.getProjectId(), ACTIVE_CANDIDATE_STATUSES);
        int targetCount = requested > 0 ? requested : activeCount;
        String targetRoute = routeOrDefault(req.getSourceRoute(), "/standard-candidates?projectId=" + req.getProjectId() + "&status=PENDING");
        StandardMaintenanceWorkflowInboxAction action = new StandardMaintenanceWorkflowInboxAction(
                "REVIEW_CANDIDATES",
                "STANDARD_CANDIDATE",
                targetCount,
                "处理标准候选 Inbox",
                safeText("对 " + targetCount + " 个候选执行采纳、合并、忽略或延后前的 dry-run 计划。"),
                true);
        List<StandardMaintenanceWorkflowEvidenceLink> evidence = List.of(evidence(
                "standard-candidate-inbox",
                "待处理标准候选",
                targetRoute,
                "PENDING/POSTPONED activeCount=" + activeCount,
                activeCount));
        return buildPlan(req, action, evidence, List.of(),
                "候选状态仍保持不变；未执行写入，若误生成计划只需关闭或重新选择候选。",
                "DRY_RUN",
                null);
    }

    private StandardMaintenanceWorkflowPlan qualityPlan(StandardMaintenanceWorkflowPlanReq req) {
        FieldQualityReport report = fieldQualityService.report(req.getProjectId());
        List<FieldQualityItem> fields = report == null || report.getFields() == null
                ? List.of()
                : report.getFields();
        Set<Long> requestedIds = normalizeIds(req.getSourceIds());
        Set<String> requestedCodes = normalizeSet(req.getIssueCodes());
        int matchedCount = 0;
        Set<String> issueCodes = new LinkedHashSet<>();
        Set<String> issueSummaries = new LinkedHashSet<>();
        for (FieldQualityItem field : fields) {
            if (!requestedIds.isEmpty() && (field.getFieldId() == null || !requestedIds.contains(field.getFieldId()))) {
                continue;
            }
            List<FieldQualityIssue> issues = field.getIssues() == null ? List.of() : field.getIssues();
            boolean matched = false;
            for (FieldQualityIssue issue : issues) {
                String code = safeText(issue.getCode(), 80);
                if (code != null && (requestedCodes.isEmpty() || requestedCodes.contains(code))) {
                    issueCodes.add(code);
                    issueSummaries.add(qualityIssueSummary(issue, code));
                    matched = true;
                }
            }
            if (matched) {
                matchedCount += 1;
            }
        }
        if (matchedCount == 0 && requestedIds.isEmpty() && requestedCodes.isEmpty() && report != null && report.getSummary() != null) {
            matchedCount = report.getSummary().getLowQualityCount();
        }
        String issueSummary = issueSummaries.isEmpty()
                ? (issueCodes.isEmpty() ? safeText(String.valueOf(req.getIssueCodes())) : String.join(",", issueCodes))
                : String.join("；", issueSummaries);
        StandardMaintenanceWorkflowInboxAction action = new StandardMaintenanceWorkflowInboxAction(
                "REPAIR_FIELD_QUALITY",
                "FIELD_QUALITY",
                matchedCount,
                "修复字段质量问题",
                safeText("按质量问题 " + (issueCodes.isEmpty() ? "当前筛选" : String.join(",", issueCodes)) + " 生成字段 metadata 修复计划。"),
                true);
        List<StandardMaintenanceWorkflowEvidenceLink> evidence = List.of(evidence(
                "field-quality-scoring",
                "低质量字段",
                "/field-quality?projectId=" + req.getProjectId(),
                "sourceIds=" + (requestedIds.isEmpty() ? "ALL" : requestedIds.size()) + ", issueSummary=" + issueSummary,
                matchedCount));
        List<StandardMaintenanceWorkflowStep> extraSteps = issueSummaries.isEmpty() && issueCodes.isEmpty()
                ? List.of()
                : List.of(step("review-quality-issues", "review", "复核质量问题代码",
                        "本次计划聚焦 " + issueSummary + "，先确认是否需要补注释、别名、示例、敏感标记或码表。",
                        "OPEN /field-quality?projectId=" + req.getProjectId(),
                        false,
                        "记录质量报告筛选条件和字段列表。"));
        return buildPlan(req, action, evidence, extraSteps,
                "字段内容尚未修改；如果计划不合适，重新筛选质量问题后再生成。",
                "DRY_RUN",
                null);
    }

    private StandardMaintenanceWorkflowPlan coveragePlan(StandardMaintenanceWorkflowPlanReq req) {
        int targetCount = req.getItemCount() == null ? safeSize(req.getSourceIds()) : Math.max(0, req.getItemCount());
        String sourceStatus = valueOrDefault(req.getSourceStatus(), "COMPLETE").toUpperCase(Locale.ROOT);
        String statuses = req.getCoverageStatuses() == null || req.getCoverageStatuses().isEmpty()
                ? "UNMANAGED,POSSIBLE_DUPLICATE"
                : String.join(",", req.getCoverageStatuses());
        int failedTableCount = safeCount(req.getFailedTableCount());
        int skippedTableCount = safeCount(req.getSkippedTableCount());
        StandardMaintenanceWorkflowInboxAction action = new StandardMaintenanceWorkflowInboxAction(
                "REPAIR_FIELD_COVERAGE",
                "FIELD_COVERAGE",
                targetCount,
                "处理字段覆盖率缺口",
                safeText("把覆盖率状态 " + statuses + " 转成候选复核和覆盖率验证计划。"),
                true);
        List<StandardMaintenanceWorkflowEvidenceLink> evidence = List.of(evidence(
                "field-coverage-report",
                "覆盖率缺口",
                routeOrDefault(req.getSourceRoute(), "/field-coverage?projectId=" + req.getProjectId()),
                "sourceStatus=" + sourceStatus + ", coverageStatuses=" + statuses
                        + ", failedTableCount=" + failedTableCount
                        + ", skippedTableCount=" + skippedTableCount,
                targetCount));
        List<StandardMaintenanceWorkflowNextAction> extraActions = List.of(new StandardMaintenanceWorkflowNextAction(
                "KEEP_PARTIAL_BOUNDARY",
                "warning",
                "未扫描或失败字段不能视为已处理；failedTableCount=" + failedTableCount
                        + "，skippedTableCount=" + skippedTableCount + "。先补齐覆盖率输入或在交付中记录 partial 边界。",
                "OPEN /field-coverage?projectId=" + req.getProjectId(),
                true));
        String blockedReason = "COMPLETE".equals(sourceStatus) ? null : "来源覆盖率报告为 " + sourceStatus + "，计划只能覆盖成功统计的字段。";
        return buildPlan(req, action, evidence, List.of(),
                "覆盖率报告本身只读；未执行候选创建或字段修改。",
                "DRY_RUN",
                blockedReason,
                extraActions);
    }

    private StandardMaintenanceWorkflowPlan genericPlan(StandardMaintenanceWorkflowPlanReq req, String actionType, String title, String capability) {
        int targetCount = req.getItemCount() == null ? Math.max(1, safeSize(req.getSourceIds())) : Math.max(0, req.getItemCount());
        StandardMaintenanceWorkflowInboxAction action = new StandardMaintenanceWorkflowInboxAction(
                actionType,
                normalizeSourceType(req.getSourceType()),
                targetCount,
                title,
                safeText(valueOrDefault(req.getNote(), "根据当前 DataSpec 诊断信号生成维护 workflow dry-run 计划。")),
                true);
        List<StandardMaintenanceWorkflowEvidenceLink> evidence = List.of(evidence(
                capability,
                title,
                routeOrDefault(req.getSourceRoute(), "/dashboard?projectId=" + req.getProjectId()),
                "sourceType=" + normalizeSourceType(req.getSourceType()),
                targetCount));
        return buildPlan(req, action, evidence, List.of(),
                "计划未持久化且未执行写入；补充来源后可重新生成。",
                targetCount <= 0 ? "BLOCKED" : "DRY_RUN",
                targetCount <= 0 ? "缺少可处理的维护来源。" : null);
    }

    private StandardMaintenanceWorkflowPlan buildPlan(
            StandardMaintenanceWorkflowPlanReq req,
            StandardMaintenanceWorkflowInboxAction action,
            List<StandardMaintenanceWorkflowEvidenceLink> evidenceLinks,
            List<StandardMaintenanceWorkflowStep> extraSteps,
            String undoHint,
            String status,
            String blockedReason) {
        return buildPlan(req, action, evidenceLinks, extraSteps, undoHint, status, blockedReason, List.of());
    }

    private StandardMaintenanceWorkflowPlan buildPlan(
            StandardMaintenanceWorkflowPlanReq req,
            StandardMaintenanceWorkflowInboxAction action,
            List<StandardMaintenanceWorkflowEvidenceLink> evidenceLinks,
            List<StandardMaintenanceWorkflowStep> extraSteps,
            String undoHint,
            String status,
            String blockedReason,
            List<StandardMaintenanceWorkflowNextAction> extraNextActions) {
        List<StandardMaintenanceWorkflowStep> steps = new ArrayList<>();
        steps.add(step("precheck-1", "precheck", "确认项目和维护来源",
                "确认 DataSpec 服务、projectId 和当前维护来源可访问。",
                "node tools/dataspec-cli.mjs doctor --project " + req.getProjectId() + " --format json",
                false,
                "doctor 关键检查通过或 warn 已记录。"));
        steps.addAll(extraSteps);
        steps.add(step("review-1", "review", "复核维护证据",
                action.description(),
                routeOrDefault(req.getSourceRoute(), defaultRoute(action.sourceType(), req.getProjectId())),
                false,
                "记录待处理项数量、筛选条件和证据来源。"));
        steps.add(step("execute-1", "execute", "人工确认后执行维护动作",
                "执行步骤必须显式调用既有候选决策、字段编辑或页面操作；plan API 不会自动写入。",
                recommendedWriteAction(action.sourceType(), req.getProjectId()),
                true,
                "记录具体 API 响应、字段 ID 或候选状态变化。"));
        steps.add(step("verify-1", "verify", "验证维护结果",
                "完成显式维护动作后重新运行对应报告，确认待处理项下降且没有 partial 边界被忽略。",
                verificationAction(action.sourceType(), req.getProjectId()),
                false,
                "保存验证命令、报告摘要和剩余风险。"));
        steps.add(step("archive-1", "archive", "归档证据和后续动作",
                "把维护结果、验证证据、未处理项和后续动作写入任务说明或 OpenSpec evidence。",
                "记录 evidenceLinks、completionCheck 和未覆盖风险",
                false,
                "形成可复制的维护交付摘要。"));

        StandardMaintenanceWorkflowRecipeBinding recipeBinding = new StandardMaintenanceWorkflowRecipeBinding(
                RECIPE_ID,
                RECIPE_VERSION,
                sourceParameters(req, action),
                "node tools/dataspec-cli.mjs task-card create --workflow standard-maintenance --project " + req.getProjectId());
        List<StandardMaintenanceWorkflowNextAction> nextActions = new ArrayList<>();
        nextActions.add(new StandardMaintenanceWorkflowNextAction(
                "RUN_PRECHECK",
                "info",
                "先运行 precheck，再逐步执行需要确认的维护动作。",
                "node tools/dataspec-cli.mjs doctor --project " + req.getProjectId() + " --format json",
                true));
        nextActions.addAll(extraNextActions);
        nextActions.add(new StandardMaintenanceWorkflowNextAction(
                "REVIEW_CONFIRMATION_REQUIRED",
                "warning",
                "execute 阶段需要人工确认；不要让 AI 自动采纳、合并、忽略或编辑字段。",
                recommendedWriteAction(action.sourceType(), req.getProjectId()),
                false));
        return new StandardMaintenanceWorkflowPlan(
                req.getProjectId(),
                "workflow-standard-maintenance-" + req.getProjectId() + "-" + action.actionType().toLowerCase(Locale.ROOT),
                action,
                recipeBinding,
                sanitizeSteps(steps),
                new StandardMaintenanceWorkflowExecutionState(status, "precheck-1", true, safeText(blockedReason)),
                safeText(undoHint),
                evidenceLinks,
                sanitizeActions(nextActions));
    }

    private Map<String, Object> sourceParameters(StandardMaintenanceWorkflowPlanReq req, StandardMaintenanceWorkflowInboxAction action) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("projectId", req.getProjectId());
        params.put("sourceType", action.sourceType());
        if (req.getSourceIds() != null && !req.getSourceIds().isEmpty()) {
            params.put("sourceIds", List.copyOf(req.getSourceIds()));
        }
        if (req.getIssueCodes() != null && !req.getIssueCodes().isEmpty()) {
            params.put("issueCodes", sanitizeList(req.getIssueCodes()));
        }
        if (req.getCoverageStatuses() != null && !req.getCoverageStatuses().isEmpty()) {
            params.put("coverageStatuses", sanitizeList(req.getCoverageStatuses()));
        }
        if (req.getSourceStatus() != null && !req.getSourceStatus().isBlank()) {
            params.put("sourceStatus", safeText(req.getSourceStatus(), 80));
        }
        if (req.getFailedTableCount() != null) {
            params.put("failedTableCount", safeCount(req.getFailedTableCount()));
        }
        if (req.getSkippedTableCount() != null) {
            params.put("skippedTableCount", safeCount(req.getSkippedTableCount()));
        }
        return params;
    }

    private StandardMaintenanceWorkflowStep step(
            String stepId,
            String phase,
            String title,
            String description,
            String recommendedAction,
            boolean requiresConfirmation,
            String expectedEvidence) {
        return new StandardMaintenanceWorkflowStep(
                stepId,
                phase,
                safeText(title),
                safeText(description),
                safeText(recommendedAction),
                requiresConfirmation,
                safeText(expectedEvidence),
                "PENDING");
    }

    private StandardMaintenanceWorkflowEvidenceLink evidence(String sourceCapability, String label, String targetRoute, String summary, int count) {
        return new StandardMaintenanceWorkflowEvidenceLink(
                safeText(sourceCapability, 80),
                safeText(label, 80),
                safeText(targetRoute, 180),
                safeText(summary, 180),
                Math.max(0, count));
    }

    private List<StandardMaintenanceWorkflowStep> sanitizeSteps(List<StandardMaintenanceWorkflowStep> steps) {
        return steps.stream()
                .map(step -> new StandardMaintenanceWorkflowStep(
                        step.stepId(),
                        step.phase(),
                        step.title(),
                        step.description(),
                        step.recommendedAction(),
                        step.requiresConfirmation(),
                        step.expectedEvidence(),
                        step.status()))
                .toList();
    }

    private List<StandardMaintenanceWorkflowNextAction> sanitizeActions(List<StandardMaintenanceWorkflowNextAction> actions) {
        return actions.stream()
                .map(action -> new StandardMaintenanceWorkflowNextAction(
                        safeText(action.code(), 80),
                        safeText(action.severity(), 20),
                        safeText(action.message()),
                        safeText(action.command()),
                        action.retryable()))
                .toList();
    }

    private String recommendedWriteAction(String sourceType, Long projectId) {
        return switch (sourceType) {
            case "STANDARD_CANDIDATE" -> "POST /api/standard-candidates/{id}/accept|merge|ignore|postpone";
            case "FIELD_QUALITY" -> "OPEN /fields?projectId=" + projectId + " 并显式编辑字段 metadata";
            case "FIELD_COVERAGE" -> "OPEN /reverse-import?projectId=" + projectId + " 或 /standard-candidates?projectId=" + projectId;
            default -> "按计划打开对应 DataSpec 页面并人工确认写入动作";
        };
    }

    private String verificationAction(String sourceType, Long projectId) {
        return switch (sourceType) {
            case "STANDARD_CANDIDATE" -> "GET /api/standard-candidates?projectId=" + projectId + "&status=PENDING";
            case "FIELD_QUALITY" -> "GET /api/fields/quality?projectId=" + projectId;
            case "FIELD_COVERAGE" -> "重新生成 /api/coverage/report 或刷新字段覆盖率页面";
            default -> "重新运行关联报告并记录 completionCheck";
        };
    }

    private String defaultRoute(String sourceType, Long projectId) {
        return switch (sourceType) {
            case "STANDARD_CANDIDATE" -> "/standard-candidates?projectId=" + projectId;
            case "FIELD_QUALITY" -> "/field-quality?projectId=" + projectId;
            case "FIELD_COVERAGE" -> "/field-coverage?projectId=" + projectId;
            default -> "/dashboard?projectId=" + projectId;
        };
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> safeText(value, 80))
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private Set<String> normalizeSet(List<String> values) {
        return new LinkedHashSet<>(sanitizeList(values));
    }

    private Set<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null && value > 0) {
                ids.add(value);
            }
        }
        return ids;
    }

    private String qualityIssueSummary(FieldQualityIssue issue, String fallbackCode) {
        String code = safeText(valueOrDefault(issue.getCode(), fallbackCode), 80);
        String severity = issue.getSeverity() == null ? "UNKNOWN" : issue.getSeverity().name();
        String suggestedAction = safeText(issue.getSuggestedAction(), 80);
        if (suggestedAction == null || suggestedAction.isBlank()) {
            return code + "/" + severity;
        }
        return code + "/" + severity + ":" + suggestedAction;
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "STANDARD_MAINTENANCE";
        }
        return safeText(sourceType.trim().toUpperCase(Locale.ROOT), 80);
    }

    private String routeOrDefault(String route, String fallback) {
        return route == null || route.isBlank() ? fallback : safeText(route, 180);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeText(String value) {
        return safeText(value, 240);
    }

    private String safeText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = SensitiveDataSanitizer.redactText(value, maxLength);
        return sanitized
                .replaceAll("(?i)\\b(password|passwd|token|authorization|api[_-]?key|secret|dsn)\\b", "凭据")
                .replace("Bearer raw", "Bearer [REDACTED]");
    }
}
