package com.dataspec.aicontext.service;

import com.dataspec.aicontext.model.AiContextBudgetArtifact;
import com.dataspec.aicontext.model.AiContextBudgetEstimation;
import com.dataspec.aicontext.model.AiContextBudgetPlan;
import com.dataspec.aicontext.model.AiContextBudgetPlanRequest;
import com.dataspec.aicontext.model.AiContextBudgetQualityRisk;
import com.dataspec.aicontext.model.AiContextBudgetRequestEcho;
import com.dataspec.aicontext.model.AiContextRecommendedExportParams;
import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.dataspec.aiprofile.model.AiTaskContextScope;
import com.dataspec.aiprofile.model.AiTaskProfile;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.businessglossary.model.BusinessGlossaryContextExport;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingSummaries;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * AI Context 预算 planner 服务。
 *
 * <p>该服务只读取项目标准元数据并输出 artifact 级预算计划，不生成 zip、不写缓存、不调用外部 LLM 或 tokenizer。</p>
 */
@Service
@RequiredArgsConstructor
public class AiContextBudgetPlannerService {

    private static final String KIND = "dataspec-ai-context-budget-plan";
    private static final int SCHEMA_VERSION = 1;
    private static final int GLOSSARY_CONTEXT_LIMIT = 200;
    private static final int USAGE_EXAMPLE_CONTEXT_LIMIT = 8;
    private static final int LOW_BUDGET_HINT_THRESHOLD = 2_000;
    private static final String ESTIMATION_METHOD = "deterministic-local-character-weight-v1";
    private static final Set<String> CRITICAL_ARTIFACTS = Set.of(
            ".dataspec/DATABASE_RULES.md",
            ".dataspec/field-catalog.json",
            ".dataspec/rules.yaml"
    );

    private final FieldService fieldService;
    private final RuleConfigService ruleConfigService;
    private final EnumDictService enumDictService;
    private final SqlLintService sqlLintService;
    private final AiTaskProfileService aiTaskProfileService;
    private final BusinessGlossaryService businessGlossaryService;
    private final StandardUsageExampleService standardUsageExampleService;

    /**
     * 生成只读 AI Context 预算计划。
     *
     * @param request 预算请求；projectId 和 tokenBudget 必填
     * @return artifact 级取舍、估算和风险建议
     */
    public AiContextBudgetPlan plan(AiContextBudgetPlanRequest request) {
        validateRequest(request);

        List<String> diagnostics = new ArrayList<>();
        AiContextScopeOptions options = resolveProfileScopeOptions(scopeOptions(request), diagnostics);
        ScopeResult scope = buildScope(request.projectId(), options, diagnostics);
        ArtifactStats stats = collectStats(request.projectId(), scope, diagnostics);
        AiContextBudgetQualityRisk riskSeed = initialRisk(request, options, scope, diagnostics);
        AiContextRecommendedExportParams recommendedParams = recommendedParams(options, request, riskSeed);
        List<ArtifactCandidate> candidates = artifactCandidates(scope, stats, recommendedParams);
        Selection selection = selectArtifacts(candidates, request.tokenBudget());
        AiContextBudgetQualityRisk qualityRisk = qualityRisk(riskSeed, scope, selection);
        List<String> fallbackSteps = fallbackSteps(qualityRisk, scope, request, options);
        List<String> nextActions = recommendedNextActions(qualityRisk, scope, request, options);

        AiContextBudgetRequestEcho echo = new AiContextBudgetRequestEcho(
                request.projectId(),
                request.tokenBudget(),
                safeText(options.taskType()),
                safeText(options.profileId()),
                options.scope(),
                safeText(options.query()),
                safeText(options.status()),
                options.limit(),
                safeText(request.targetTable()),
                safeText(request.targetFile()),
                scope.totalFieldCount(),
                scope.matchedFieldCount(),
                scope.returnedFieldCount()
        );
        return new AiContextBudgetPlan(
                KIND,
                SCHEMA_VERSION,
                request.projectId(),
                echo,
                new AiContextBudgetEstimation(
                        request.tokenBudget(),
                        selection.selectedEstimatedTokens(),
                        candidates.stream().mapToInt(ArtifactCandidate::estimatedTokens).sum(),
                        ESTIMATION_METHOD,
                        "conservative"
                ),
                selection.selected(),
                selection.dropped(),
                qualityRisk,
                fallbackSteps,
                recommendedParams,
                List.copyOf(diagnostics),
                nextActions
        );
    }

    private void validateRequest(AiContextBudgetPlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("预算请求不能为空");
        }
        if (request.projectId() == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        if (request.tokenBudget() == null || request.tokenBudget() <= 0) {
            throw new IllegalArgumentException("tokenBudget 必须大于 0");
        }
    }

    private AiContextScopeOptions scopeOptions(AiContextBudgetPlanRequest request) {
        String effectiveQuery = firstText(request.query(), request.targetTable(), request.targetFile());
        return new AiContextScopeOptions(
                request.scope(),
                effectiveQuery,
                request.status(),
                request.limit(),
                request.profileId(),
                request.taskType(),
                request.scope() != null && !request.scope().isBlank());
    }

    private AiContextScopeOptions resolveProfileScopeOptions(AiContextScopeOptions rawOptions, List<String> diagnostics) {
        AiContextScopeOptions options = rawOptions == null ? AiContextScopeOptions.full() : rawOptions;
        String requestedProfile = options.profileId() != null ? options.profileId() : options.taskType();
        if (requestedProfile == null) {
            return options;
        }
        if (aiTaskProfileService == null) {
            diagnostics.add("已请求 AI profile，但当前服务未启用 profile registry，已按显式 scope 参数处理。");
            return options;
        }
        Optional<AiTaskProfile> profile = aiTaskProfileService.findProfile(requestedProfile);
        if (profile == null || profile.isEmpty()) {
            diagnostics.add("未知 AI profile 或 taskType，已按显式 scope 参数处理。");
            return options;
        }
        return mergeProfileScope(options, profile.get());
    }

    private AiContextScopeOptions mergeProfileScope(AiContextScopeOptions options, AiTaskProfile profile) {
        AiTaskContextScope defaults = profile.getContextScope();
        if (defaults == null) {
            return options;
        }
        // profile 只是默认建议，显式 scope/query/status/limit 必须保持优先。
        String effectiveScope = options.scopeExplicit() ? options.scope() : firstText(defaults.getScope(), options.scope());
        String effectiveQuery = options.query() != null ? options.query() : defaults.getQuery();
        String effectiveStatus = options.status() != null ? options.status() : defaults.getStatus();
        Integer effectiveLimit = options.limit() != null ? options.limit() : defaults.getLimit();
        String profileId = options.profileId() != null ? options.profileId() : profile.getProfileId();
        String taskType = options.taskType() != null ? options.taskType() : profile.getTaskType();
        return new AiContextScopeOptions(
                effectiveScope,
                effectiveQuery,
                effectiveStatus,
                effectiveLimit,
                profileId,
                taskType,
                options.scopeExplicit());
    }

    private ScopeResult buildScope(Long projectId, AiContextScopeOptions options, List<String> diagnostics) {
        List<Field> allFields = safeList(fieldService.listByProject(projectId));
        String effectiveScope = options.scopeSupported() ? options.scope() : "all";
        if (!options.scopeSupported()) {
            diagnostics.add("未知 scope，已按完整字段文本匹配处理。");
        }
        if ("changed".equals(effectiveScope)) {
            diagnostics.add("changed 第一版基于 query 做任务相关裁剪，尚未启用快照 diff。");
        }
        if (!"all".equals(effectiveScope) && options.query() == null && options.status() == null) {
            diagnostics.add("scope 需要 query 或 status 才能确定裁剪范围。");
        }

        List<FieldMatch> matchedFields = new ArrayList<>();
        for (Field field : allFields) {
            List<String> reasons = new ArrayList<>();
            if (!matchesStatus(field, options.status())) {
                continue;
            }
            if (options.status() != null) {
                reasons.add("状态匹配");
            }
            boolean includeByText = includeByTextScope(field, effectiveScope, options.query(), reasons);
            boolean includeByStatusOnly = options.query() == null && options.status() != null;
            boolean includeFull = "all".equals(effectiveScope) && options.query() == null;
            if (includeFull || includeByText || includeByStatusOnly) {
                matchedFields.add(new FieldMatch(field, List.copyOf(reasons)));
            }
        }

        int matchedCount = matchedFields.size();
        List<FieldMatch> returnedFields = matchedFields;
        if (options.limit() != null && matchedFields.size() > options.limit()) {
            returnedFields = matchedFields.subList(0, options.limit());
            diagnostics.add("命中字段已按 limit 截断，请缩小 query 或提高 limit。");
        }
        FieldGroupSummary groupSummary = FieldGroupingSummaries.fromFields(
                projectId,
                returnedFields.stream().map(FieldMatch::field).toList());
        if (options.scoped() && groupSummary.ungroupedFieldCount() > 0) {
            diagnostics.add("返回字段中存在未分组字段，请按数据域、分类或标签补齐。");
        }
        if (options.scoped() && matchedCount == 0) {
            diagnostics.add("当前 scope/query 未命中标准字段或示例，上下文质量可能不足。");
        }
        return new ScopeResult(
                List.copyOf(returnedFields),
                effectiveScope,
                options.query(),
                options.status(),
                options.limit(),
                options.profileId(),
                options.taskType(),
                allFields.size(),
                matchedCount,
                returnedFields.size()
        );
    }

    private ArtifactStats collectStats(Long projectId, ScopeResult scope, List<String> diagnostics) {
        int enabledRuleCount = safeList(ruleConfigService.listEnabledByProject(projectId)).size();
        int availableRuleCount = safeList(sqlLintService.listAvailableRules()).size();
        int enumCount = safeList(enumDictService.listByProject(projectId)).size();
        BusinessGlossaryContextExport glossary = businessGlossaryService == null
                ? BusinessGlossaryContextExport.empty()
                : businessGlossaryService.contextExport(projectId, GLOSSARY_CONTEXT_LIMIT);
        List<StandardUsageExample> usageExamples = standardUsageExampleService == null
                ? List.of()
                : safeList(standardUsageExampleService.selectForAiContext(
                projectId,
                usageExampleFieldIds(scope),
                scope.query(),
                USAGE_EXAMPLE_CONTEXT_LIMIT + 1));
        int selectedUsageExampleCount = Math.min(usageExamples.size(), USAGE_EXAMPLE_CONTEXT_LIMIT);
        if (usageExamples.size() > USAGE_EXAMPLE_CONTEXT_LIMIT) {
            diagnostics.add("使用示例已按 AI Context 上限截断。");
        }
        return new ArtifactStats(
                enabledRuleCount,
                availableRuleCount,
                enumCount,
                glossary == null ? 0 : glossary.returnedCount(),
                selectedUsageExampleCount
        );
    }

    private AiContextBudgetQualityRisk initialRisk(AiContextBudgetPlanRequest request,
                                                   AiContextScopeOptions options,
                                                   ScopeResult scope,
                                                   List<String> diagnostics) {
        AiContextBudgetQualityRisk risk = AiContextBudgetQualityRisk.LOW;
        if (request.tokenBudget() < LOW_BUDGET_HINT_THRESHOLD
                && options.query() == null
                && options.profileId() == null
                && options.taskType() == null
                && isBlank(request.targetTable())
                && isBlank(request.targetFile())) {
            diagnostics.add("低预算且缺少 query、targetTable、targetFile 或 profile，上下文检索质量可能偏低。");
            risk = maxRisk(risk, AiContextBudgetQualityRisk.MEDIUM);
        }
        if (scope.matchedFieldCount() == 0 && isScoped(scope)) {
            risk = maxRisk(risk, AiContextBudgetQualityRisk.MEDIUM);
        }
        return risk;
    }

    private List<ArtifactCandidate> artifactCandidates(ScopeResult scope,
                                                       ArtifactStats stats,
                                                       AiContextRecommendedExportParams appliedScope) {
        int fieldCount = Math.max(scope.returnedFieldCount(), isScoped(scope) ? 0 : scope.totalFieldCount());
        int databaseRulesTokens = 450 + stats.availableRuleCount() * 80 + stats.enabledRuleCount() * 100 + fieldCount * 90;
        int fieldCatalogTokens = 600 + fieldCount * 140 + stats.enumCount() * 60
                + stats.glossaryCount() * 30 + stats.usageExampleCount() * 90;
        int rulesYamlTokens = 360 + stats.enabledRuleCount() * 80;
        return List.of(
                candidate(".dataspec/manifest.json", 180, 1, "保留上下文包版本和命令入口摘要。", "缺失后 AI 难以确认标准版本。", appliedScope),
                candidate(".dataspec/DATABASE_RULES.md", databaseRulesTokens, 2, "保留数据库命名和规则说明。", "缺失后 DDL/SQL 生成风险显著升高。", appliedScope),
                candidate(".dataspec/field-catalog.json", fieldCatalogTokens, 3, "保留字段目录摘要和裁剪命中字段。", "缺失后 AI 无法稳定复用标准字段。", appliedScope),
                candidate(".dataspec/rules.yaml", rulesYamlTokens, 4, "保留机器可读规则配置。", "缺失后自动校验和修复建议质量下降。", appliedScope),
                candidate(".dataspec/usage-examples.json", 260 + stats.usageExampleCount() * 90, 5, "保留 GOOD/BAD 示例摘要。", "缺失后 AI 难以模仿项目最佳实践。", appliedScope),
                candidate(".dataspec/schema-registry.json", 500, 6, "保留稳定 schema 契约摘要。", "缺失后工具间字段契约确认能力下降。", appliedScope),
                candidate(".dataspec/capabilities.json", 420, 7, "保留 AI/CLI/MCP 能力清单。", "缺失后 AI 难以选择正确入口。", appliedScope),
                candidate(".dataspec/README.md", 260, 8, "保留上下文包使用说明。", "缺失后人工阅读成本升高。", appliedScope),
                candidate(".dataspec/workflows.md", 260, 9, "保留常见工作流步骤。", "缺失后复杂任务拆解质量下降。", appliedScope),
                candidate(".dataspec/prompts.md", 320, 10, "保留提示词模板。", "缺失后 prompt 复用能力下降。", appliedScope),
                candidate(".dataspec/examples/good.sql", 220, 11, "保留正向 SQL 示例。", "缺失后新建表风格参考减少。", appliedScope),
                candidate(".dataspec/examples/bad.sql", 220, 12, "保留反例 SQL。", "缺失后避坑参考减少。", appliedScope),
                candidate("AGENTS.md.fragment", 240, 13, "保留 Agent 指令片段。", "缺失后自动化任务的操作边界不完整。", appliedScope)
        );
    }

    private Selection selectArtifacts(List<ArtifactCandidate> candidates, int tokenBudget) {
        int used = 0;
        List<AiContextBudgetArtifact> selected = new ArrayList<>();
        List<AiContextBudgetArtifact> dropped = new ArrayList<>();
        for (ArtifactCandidate candidate : candidates) {
            AiContextBudgetArtifact artifact = candidate.toArtifact();
            if (used + candidate.estimatedTokens() <= tokenBudget) {
                selected.add(artifact);
                used += candidate.estimatedTokens();
            } else {
                dropped.add(artifact);
            }
        }
        return new Selection(List.copyOf(selected), List.copyOf(dropped), used);
    }

    private AiContextBudgetQualityRisk qualityRisk(AiContextBudgetQualityRisk seed,
                                                   ScopeResult scope,
                                                   Selection selection) {
        AiContextBudgetQualityRisk risk = seed;
        boolean criticalDropped = selection.dropped().stream()
                .map(AiContextBudgetArtifact::artifact)
                .anyMatch(CRITICAL_ARTIFACTS::contains);
        if (criticalDropped) {
            risk = maxRisk(risk, AiContextBudgetQualityRisk.HIGH);
        } else if (!selection.dropped().isEmpty()) {
            risk = maxRisk(risk, AiContextBudgetQualityRisk.MEDIUM);
        }
        if (scope.matchedFieldCount() == 0 && isScoped(scope)) {
            risk = maxRisk(risk, AiContextBudgetQualityRisk.MEDIUM);
        }
        return risk;
    }

    private List<String> fallbackSteps(AiContextBudgetQualityRisk risk,
                                       ScopeResult scope,
                                       AiContextBudgetPlanRequest request,
                                       AiContextScopeOptions options) {
        List<String> steps = new ArrayList<>();
        if (scope.matchedFieldCount() == 0 && isScoped(scope)) {
            steps.add("放宽 query 或先使用字段搜索确认标准字段。");
            steps.add("改用完整上下文或提高 limit 后重新规划。");
        }
        if (risk == AiContextBudgetQualityRisk.HIGH) {
            steps.add("提高 tokenBudget 至至少覆盖 DATABASE_RULES、field-catalog 和 rules.yaml。");
            steps.add("收窄 query、targetTable 或 targetFile 后重新运行预算计划。");
        } else if (risk == AiContextBudgetQualityRisk.MEDIUM && options.query() == null) {
            steps.add("补充 query 或目标提示以提升低预算裁剪质量。");
        }
        return distinct(steps);
    }

    private List<String> recommendedNextActions(AiContextBudgetQualityRisk risk,
                                                ScopeResult scope,
                                                AiContextBudgetPlanRequest request,
                                                AiContextScopeOptions options) {
        List<String> actions = new ArrayList<>();
        if (risk == AiContextBudgetQualityRisk.LOW) {
            actions.add("可按推荐参数导出完整 AI Context 包。");
        } else if (risk == AiContextBudgetQualityRisk.MEDIUM) {
            actions.add("可使用 recommendedExportParams 显式填充导出参数后预览字段目录。");
            if (options.query() == null) {
                actions.add("提供 query 或目标表/文件提示后重新运行 context-budget plan。");
            }
        } else {
            actions.add("提高 tokenBudget 或收窄 query 后重新运行 context-budget plan。");
            actions.add("复杂任务请停止自动执行并等待人工确认上下文是否足够。");
        }
        if (scope.matchedFieldCount() == 0 && isScoped(scope)) {
            actions.add("先放宽 query 或使用完整上下文确认是否存在目标标准字段。");
        }
        return distinct(actions);
    }

    private AiContextRecommendedExportParams recommendedParams(AiContextScopeOptions options,
                                                               AiContextBudgetPlanRequest request,
                                                               AiContextBudgetQualityRisk seedRisk) {
        if (seedRisk == AiContextBudgetQualityRisk.LOW
                && "all".equals(options.scope())
                && options.query() == null
                && options.limit() == null
                && options.status() == null) {
            return new AiContextRecommendedExportParams("all", null, null, null,
                    safeText(options.profileId()), safeText(options.taskType()));
        }
        String scope = "all".equals(options.scope()) && request.tokenBudget() < LOW_BUDGET_HINT_THRESHOLD
                ? "field"
                : options.scope();
        Integer limit = options.limit();
        if (limit == null && request.tokenBudget() < LOW_BUDGET_HINT_THRESHOLD) {
            limit = 20;
        } else if (limit == null && !"all".equals(scope)) {
            limit = 50;
        }
        return new AiContextRecommendedExportParams(
                scope,
                safeText(options.query()),
                safeText(options.status()),
                limit,
                safeText(options.profileId()),
                safeText(options.taskType())
        );
    }

    private ArtifactCandidate candidate(String artifact,
                                        int estimatedTokens,
                                        int priority,
                                        String reason,
                                        String riskImpact,
                                        AiContextRecommendedExportParams appliedScope) {
        return new ArtifactCandidate(artifact, estimatedTokens, priority, reason, riskImpact, appliedScope);
    }

    private boolean includeByTextScope(Field field, String scope, String query, List<String> reasons) {
        if (query == null) {
            return false;
        }
        return switch (scope) {
            case "domain" -> collectDomainReasons(field, query, reasons);
            case "tag" -> collectTagReasons(field, query, reasons);
            case "table", "changed", "field", "all" -> collectGeneralTextReasons(field, query, reasons);
            default -> collectGeneralTextReasons(field, query, reasons);
        };
    }

    private boolean collectGeneralTextReasons(Field field, String query, List<String> reasons) {
        addReasonIfContains(reasons, field.getName(), query);
        addReasonIfContains(reasons, field.getDisplayName(), query);
        addReasonIfContains(reasons, field.getAliases(), query);
        addReasonIfContains(reasons, field.getComment(), query);
        addReasonIfContains(reasons, field.getCategory(), query);
        addReasonIfContains(reasons, field.getTags(), query);
        addReasonIfContains(reasons, field.getDataType(), query);
        addReasonIfContains(reasons, field.getExampleValue(), query);
        addReasonIfContains(reasons, fieldStatusForExport(field.getStatus()), query);
        return hasTextReason(reasons);
    }

    private boolean collectDomainReasons(Field field, String query, List<String> reasons) {
        addReasonIfContains(reasons, field.getCategory(), query);
        if (field.getDomainId() != null && containsIgnoreCase(String.valueOf(field.getDomainId()), query)) {
            reasons.add("数据域ID匹配");
        }
        addReasonIfContains(reasons, field.getTags(), query);
        addReasonIfContains(reasons, field.getDisplayName(), query);
        addReasonIfContains(reasons, field.getComment(), query);
        return hasTextReason(reasons);
    }

    private boolean collectTagReasons(Field field, String query, List<String> reasons) {
        addReasonIfContains(reasons, field.getTags(), query);
        addReasonIfContains(reasons, field.getAliases(), query);
        addReasonIfContains(reasons, field.getDisplayName(), query);
        addReasonIfContains(reasons, field.getCategory(), query);
        addReasonIfContains(reasons, field.getComment(), query);
        return hasTextReason(reasons);
    }

    private boolean hasTextReason(List<String> reasons) {
        return reasons.stream().anyMatch(reason -> !"状态匹配".equals(reason));
    }

    private void addReasonIfContains(List<String> reasons, String value, String query) {
        if (containsIgnoreCase(value, query)) {
            reasons.add("文本匹配");
        }
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (value == null || query == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean matchesStatus(Field field, String status) {
        return status == null || fieldStatusForExport(field.getStatus()).equalsIgnoreCase(status);
    }

    private String fieldStatusForExport(String status) {
        return status == null || status.isBlank() ? "enabled" : status;
    }

    private List<Long> usageExampleFieldIds(ScopeResult scope) {
        if (!isScoped(scope)) {
            return List.of();
        }
        return scope.fields().stream()
                .map(FieldMatch::field)
                .map(Field::getId)
                .filter(id -> id != null)
                .toList();
    }

    private boolean isScoped(ScopeResult scope) {
        return !"all".equals(scope.scope()) || scope.query() != null || scope.status() != null || scope.limit() != null;
    }

    private AiContextBudgetQualityRisk maxRisk(AiContextBudgetQualityRisk left, AiContextBudgetQualityRisk right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String safeText(String value) {
        return SensitiveDataSanitizer.redactText(firstText(value), 160);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private List<String> distinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private record FieldMatch(Field field, List<String> reasons) {
    }

    private record ScopeResult(
            List<FieldMatch> fields,
            String scope,
            String query,
            String status,
            Integer limit,
            String profileId,
            String taskType,
            int totalFieldCount,
            int matchedFieldCount,
            int returnedFieldCount
    ) {
    }

    private record ArtifactStats(
            int enabledRuleCount,
            int availableRuleCount,
            int enumCount,
            int glossaryCount,
            int usageExampleCount
    ) {
    }

    private record ArtifactCandidate(
            String artifact,
            int estimatedTokens,
            int priority,
            String reason,
            String riskImpact,
            AiContextRecommendedExportParams appliedScope
    ) {
        private AiContextBudgetArtifact toArtifact() {
            return new AiContextBudgetArtifact(artifact, estimatedTokens, reason, riskImpact, appliedScope);
        }
    }

    private record Selection(
            List<AiContextBudgetArtifact> selected,
            List<AiContextBudgetArtifact> dropped,
            int selectedEstimatedTokens
    ) {
    }
}
