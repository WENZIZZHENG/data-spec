package com.dataspec.aiprofile.service.impl;

import com.dataspec.aiprofile.model.AiProfileDiagnostic;
import com.dataspec.aiprofile.model.AiTaskContextScope;
import com.dataspec.aiprofile.model.AiTaskOutputFormat;
import com.dataspec.aiprofile.model.AiTaskProfile;
import com.dataspec.aiprofile.model.AiTaskProfileCatalog;
import com.dataspec.aiprofile.model.AiTaskProfileDetail;
import com.dataspec.aiprofile.model.AiTaskRuleset;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.model.FixMode;
import com.dataspec.lint.model.FixPolicy;
import com.dataspec.lint.model.FixRiskLevel;
import com.dataspec.rule.service.RuleConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 内置 AI 任务画像注册表。
 * <p>
 * 第一版只提供建议和诊断，不落库、不授予额外写权限。
 */
@Service
@RequiredArgsConstructor
public class AiTaskProfileServiceImpl implements AiTaskProfileService {

    public static final String DEFAULT_PROFILE_ID = "create-table";

    private final FieldService fieldService;
    private final RuleConfigService ruleConfigService;
    private final Map<String, AiTaskProfile> profiles = builtIns();

    @Override
    public AiTaskProfileCatalog listProfiles(Long projectId, String selectedProfile) {
        String selected = normalize(selectedProfile);
        AiTaskProfile selectedResolved = selected == null ? null : findProfile(selected).orElse(null);
        String selectedProfileId = selectedResolved == null ? DEFAULT_PROFILE_ID : selectedResolved.getProfileId();
        return AiTaskProfileCatalog.builder()
                .projectId(projectId)
                .defaultProfileId(DEFAULT_PROFILE_ID)
                .selectedProfileId(selectedProfileId)
                .profiles(profiles.values().stream()
                        .map(profile -> withDefaultFlag(profile, profile.getProfileId().equals(selectedProfileId)))
                        .toList())
                .diagnostics(diagnostics(projectId, selectedResolved == null ? profiles.get(DEFAULT_PROFILE_ID) : selectedResolved, selected))
                .supportedTaskTypes(supportedTaskTypes())
                .build();
    }

    @Override
    public AiTaskProfileDetail getProfile(Long projectId, String profileOrTaskType) {
        Optional<AiTaskProfile> profile = findProfile(profileOrTaskType);
        return AiTaskProfileDetail.builder()
                .projectId(projectId)
                .requestedProfile(profileOrTaskType)
                .profile(profile.orElse(null))
                .diagnostics(profile
                        .map(value -> diagnostics(projectId, value, normalize(profileOrTaskType)))
                        .orElseGet(() -> unknownDiagnostics(profileOrTaskType)))
                .supportedProfileIds(new ArrayList<>(profiles.keySet()))
                .supportedTaskTypes(supportedTaskTypes())
                .build();
    }

    @Override
    public Optional<AiTaskProfile> findProfile(String profileOrTaskType) {
        String normalized = normalize(profileOrTaskType);
        if (normalized == null) {
            return Optional.empty();
        }
        AiTaskProfile byId = profiles.get(normalized);
        if (byId != null) {
            return Optional.of(byId);
        }
        return profiles.values().stream()
                .filter(profile -> normalize(profile.getTaskType()).equals(normalized))
                .findFirst();
    }

    @Override
    public FixPolicy resolveFixedSqlPolicy(String profileOrTaskType) {
        return findProfile(profileOrTaskType)
                .map(AiTaskProfile::getFixedSqlPolicy)
                .orElse(null);
    }

    private AiTaskProfile withDefaultFlag(AiTaskProfile profile, boolean selected) {
        return AiTaskProfile.builder()
                .profileId(profile.getProfileId())
                .taskType(profile.getTaskType())
                .displayName(profile.getDisplayName())
                .description(profile.getDescription())
                .contextScope(profile.getContextScope())
                .ruleset(profile.getRuleset())
                .fixedSqlPolicy(profile.getFixedSqlPolicy())
                .outputFormat(profile.getOutputFormat())
                .maxContextFields(profile.getMaxContextFields())
                .recommendedCommands(profile.getRecommendedCommands())
                .nextActions(profile.getNextActions())
                .defaultProfile(selected)
                .build();
    }

    private List<AiProfileDiagnostic> diagnostics(Long projectId, AiTaskProfile profile, String requestedProfile) {
        List<AiProfileDiagnostic> diagnostics = new ArrayList<>();
        if (requestedProfile != null && findProfile(requestedProfile).isEmpty()) {
            diagnostics.addAll(unknownDiagnostics(requestedProfile));
        }
        if (projectId == null) {
            diagnostics.add(warn("MISSING_PROJECT", "未提供 projectId，profile 只能作为本地默认建议。",
                    "选择项目后重新读取 AI profile。"));
            return diagnostics;
        }

        int fieldCount = safeFieldCount(projectId);
        int enabledRuleCount = safeEnabledRuleCount(projectId);
        if (fieldCount == 0 && profile.getMaxContextFields() != null && profile.getMaxContextFields() > 0) {
            diagnostics.add(warn("NO_STANDARD_FIELDS", "当前项目暂无标准字段，AI Context 只能提供规则与空字段目录。",
                    "先导入字段、创建演示项目，或使用 reverse-import profile 补标准。"));
        }
        if (enabledRuleCount == 0) {
            diagnostics.add(warn("NO_ENABLED_RULES", "当前项目未配置启用规则，将回退或依赖内置规则。",
                    "在规则配置页应用基线套件或确认内置规则是否足够。"));
        }
        if (diagnostics.isEmpty()) {
            diagnostics.add(pass("PROFILE_READY", "AI profile 可用于当前项目。",
                    firstCommand(profile)));
        }
        return diagnostics;
    }

    private List<AiProfileDiagnostic> unknownDiagnostics(String requestedProfile) {
        return List.of(AiProfileDiagnostic.builder()
                .code("UNKNOWN_AI_PROFILE")
                .status("fail")
                .message("未知 AI profile 或 taskType: " + requestedProfile)
                .nextAction("使用支持的 profile: " + String.join(", ", profiles.keySet()))
                .build());
    }

    private int safeFieldCount(Long projectId) {
        try {
            return fieldService.listByProject(projectId).size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int safeEnabledRuleCount(Long projectId) {
        try {
            return ruleConfigService.listEnabledByProject(projectId).size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private AiProfileDiagnostic pass(String code, String message, String nextAction) {
        return diagnostic(code, "pass", message, nextAction);
    }

    private AiProfileDiagnostic warn(String code, String message, String nextAction) {
        return diagnostic(code, "warn", message, nextAction);
    }

    private AiProfileDiagnostic diagnostic(String code, String status, String message, String nextAction) {
        return AiProfileDiagnostic.builder()
                .code(code)
                .status(status)
                .message(message)
                .nextAction(nextAction)
                .build();
    }

    private String firstCommand(AiTaskProfile profile) {
        return profile.getRecommendedCommands() == null || profile.getRecommendedCommands().isEmpty()
                ? null
                : profile.getRecommendedCommands().get(0);
    }

    private List<String> supportedTaskTypes() {
        return profiles.values().stream().map(AiTaskProfile::getTaskType).toList();
    }

    private static Map<String, AiTaskProfile> builtIns() {
        Map<String, AiTaskProfile> map = new LinkedHashMap<>();
        add(map, profile("create-table", "CREATE_TABLE", "建表生成",
                "面向新表设计，优先读取标准字段和数据库规则，输出可审阅 DDL。",
                scope("field", null, "enabled", 80),
                ruleset("strict", List.of("table_naming_snake_case", "field_naming_snake_case", "required_columns")),
                FixPolicy.builder().mode(FixMode.GENERATE).maxRiskLevel(FixRiskLevel.MEDIUM).includeExplanations(true).build(),
                output("markdown+sql", "dataspec://contracts/create-table", true),
                80,
                List.of("dataspec export-context --profile create-table --cache", "dataspec generate-ddl --project <id> --template <id>"),
                List.of("先读取字段目录和 rules.yaml，再生成 DDL；输出前运行 SQL lint。")));
        add(map, profile("sql-fix", "SQL_FIX", "SQL 修复",
                "面向修复已有建表 SQL，默认 dry-run 并限制到低风险自动修复。",
                scope("field", null, "enabled", 60),
                ruleset("safe", List.of("table_naming_snake_case", "field_naming_snake_case")),
                FixPolicy.builder().mode(FixMode.DRY_RUN).maxRiskLevel(FixRiskLevel.LOW).includeExplanations(true).build(),
                output("json+diff", "dataspec://contracts/lint-result", true),
                60,
                List.of("dataspec lint <file.sql> --profile sql-fix --format json"),
                List.of("只把 fixedSql 当预览；应用前人工确认 diff、风险和方言诊断。")));
        add(map, profile("reverse-import", "REVERSE_IMPORT", "反向导入补标准",
                "面向从已有数据库提取候选标准，强调只读连接、候选入箱和人工采纳。",
                scope("domain", "unmanaged", "enabled", 120),
                ruleset("advisory", List.of("comment_missing", "field_naming_snake_case")),
                FixPolicy.builder().mode(FixMode.DISABLED).maxRiskLevel(FixRiskLevel.LOW).includeExplanations(true).build(),
                output("json+checklist", "dataspec://contracts/reverse-import", true),
                120,
                List.of("dataspec workflow show reverse-import-standards --format json"),
                List.of("使用只读账号，先 preview/compare，再把候选进入 Inbox。")));
        add(map, profile("pr-review", "PR_REVIEW", "PR SQL Review",
                "面向 Pull Request 中的 SQL 文件评审，输出稳定诊断和可评论摘要。",
                scope("field", null, "enabled", 80),
                ruleset("ci", List.of("table_naming_snake_case", "field_naming_snake_case", "required_columns")),
                FixPolicy.builder().mode(FixMode.DRY_RUN).maxRiskLevel(FixRiskLevel.LOW).includeExplanations(true).build(),
                output("json+markdown", "dataspec://contracts/review-pr", true),
                80,
                List.of("dataspec review-pr sql --profile pr-review --format json"),
                List.of("不要直接改 PR 文件；把 fixedSql/diff 作为评论证据。")));
        add(map, profile("minimal-context", "MINIMAL_CONTEXT", "最小上下文",
                "面向上下文预算紧张的 AI 会话，只导出少量命中字段和规则摘要。",
                scope("field", null, "enabled", 20),
                ruleset("minimal", List.of("table_naming_snake_case", "field_naming_snake_case")),
                FixPolicy.builder().mode(FixMode.DRY_RUN).maxRiskLevel(FixRiskLevel.LOW).includeExplanations(false).build(),
                output("json", "dataspec://contracts/minimal-context", false),
                20,
                List.of("dataspec export-context --profile minimal-context --cache"),
                List.of("优先按任务 query 获取 scoped context，不要一次性读取完整包。")));
        return map;
    }

    private static void add(Map<String, AiTaskProfile> map, AiTaskProfile profile) {
        map.put(profile.getProfileId(), profile);
    }

    private static AiTaskProfile profile(String profileId, String taskType, String displayName, String description,
                                         AiTaskContextScope scope, AiTaskRuleset ruleset, FixPolicy policy,
                                         AiTaskOutputFormat output, Integer maxContextFields,
                                         List<String> commands, List<String> nextActions) {
        return AiTaskProfile.builder()
                .profileId(profileId)
                .taskType(taskType)
                .displayName(displayName)
                .description(description)
                .contextScope(scope)
                .ruleset(ruleset)
                .fixedSqlPolicy(policy)
                .outputFormat(output)
                .maxContextFields(maxContextFields)
                .recommendedCommands(commands)
                .nextActions(nextActions)
                .defaultProfile(DEFAULT_PROFILE_ID.equals(profileId))
                .build();
    }

    private static AiTaskContextScope scope(String scope, String query, String status, Integer limit) {
        return AiTaskContextScope.builder()
                .scope(scope)
                .query(query)
                .status(status)
                .limit(limit)
                .build();
    }

    private static AiTaskRuleset ruleset(String strictness, List<String> requiredRuleCodes) {
        return AiTaskRuleset.builder()
                .strictness(strictness)
                .requiredRuleCodes(requiredRuleCodes)
                .optionalRuleCodes(List.of())
                .build();
    }

    private static AiTaskOutputFormat output(String format, String schemaRef, boolean evidence) {
        return AiTaskOutputFormat.builder()
                .format(format)
                .schemaRef(schemaRef)
                .includeEvidence(evidence)
                .includeNextActions(true)
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
