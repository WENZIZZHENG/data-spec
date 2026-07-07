package com.dataspec.bootstrap.service.impl;

import com.dataspec.bootstrap.model.AiSessionBootstrap;
import com.dataspec.bootstrap.model.AiSessionBootstrapCapability;
import com.dataspec.bootstrap.model.AiSessionBootstrapCheck;
import com.dataspec.bootstrap.model.AiSessionBootstrapNextAction;
import com.dataspec.bootstrap.model.AiSessionBootstrapSnapshot;
import com.dataspec.bootstrap.service.AiSessionBootstrapService;
import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityEntry;
import com.dataspec.capability.service.AiCapabilityCatalogService;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 会话启动包只做只读聚合，不替代 doctor，也不会自动执行任何能力。
 */
@Service
@RequiredArgsConstructor
public class AiSessionBootstrapServiceImpl implements AiSessionBootstrapService {

    public static final String KIND = "dataspec-ai-session-bootstrap";
    public static final int SCHEMA_VERSION = 1;

    private static final List<String> BOOTSTRAP_CAPABILITY_IDS = List.of(
            "session-bootstrap",
            "doctor",
            "capability-catalog",
            "export-ai-context",
            "lint-sql",
            "reverse-import",
            "generate-ddl",
            "standard-evidence"
    );

    private final AiCapabilityCatalogService capabilityCatalogService;
    private final StandardSnapshotService standardSnapshotService;

    @Override
    public AiSessionBootstrap getBootstrap(Long projectId, String server, boolean tokenPresent) {
        String safeServer = normalizeServer(server);
        String authMode = tokenPresent ? "TOKEN_PRESENT" : "TOKEN_MISSING";
        AiCapabilityCatalog catalog = capabilityCatalogService.getCatalog(projectId);
        List<AiSessionBootstrapCapability> capabilities = summarizeCapabilities(catalog.capabilities());
        List<AiSessionBootstrapCheck> checks = new ArrayList<>();
        List<AiSessionBootstrapNextAction> nextActions = new ArrayList<>();

        if (projectId == null) {
            checks.add(check("project", "fail", "未选择 projectId，项目级能力暂不可执行", "提供 --project <id> 或写入 .dataspec/config.json"));
            nextActions.add(action(
                    "SELECT_PROJECT",
                    "error",
                    "先选择 DataSpec 项目，再执行 lint、导出 Context、反向导入或生成 DDL。",
                    "dataspec doctor --format json",
                    "README.md#cli",
                    true
            ));
            return build(
                    "BLOCKED",
                    null,
                    safeServer,
                    authMode,
                    "unselected",
                    new AiSessionBootstrapSnapshot(null, null, "unselected", null, false, "unselected"),
                    capabilities,
                    recommendedCommands(null),
                    knownRisks(capabilities, projectId),
                    docsRefs(catalog, true),
                    checks,
                    nextActions
            );
        }

        checks.add(check("project", "pass", "已选择 projectId: " + projectId, null));
        checks.add(tokenPresent
                ? check("auth", "pass", "请求包含 API token，启动包不会输出 token 明文", null)
                : check("auth", "warn", "未检测到 API token；安全模式关闭时可继续，否则后续请求会失败", "配置 DATASPEC_TOKEN 或 --dataspec-token"));

        StandardSnapshotInfo snapshot = standardSnapshotService.getCurrentSnapshot(projectId);
        AiSessionBootstrapSnapshot standardSnapshot = new AiSessionBootstrapSnapshot(
                snapshot.snapshotId(),
                snapshot.projectId(),
                snapshot.specVersion(),
                snapshot.specHash(),
                snapshot.versioned(),
                snapshot.source()
        );
        if (snapshot.versioned()) {
            checks.add(check("standard", "pass", "当前标准快照可用: " + snapshot.specVersion(), null));
        } else {
            checks.add(check("standard", "warn", "当前项目还没有版本化标准快照", "创建或刷新标准快照后再生成可复现 AI Context"));
            nextActions.add(action(
                    "REFRESH_STANDARD_SNAPSHOT",
                    "warn",
                    "建议创建标准快照，让 AI Context 和后续证据包带上可复现 specVersion。",
                    null,
                    "README.md#标准版本快照",
                    true
            ));
        }
        checks.add(check("capabilities", "pass", "已读取 AI 能力清单: " + capabilities.size() + " 项", null));

        String status = checks.stream().anyMatch(item -> "fail".equals(item.status()))
                ? "BLOCKED"
                : checks.stream().anyMatch(item -> "warn".equals(item.status())) ? "DEGRADED" : "READY";
        if ("READY".equals(status)) {
            nextActions.add(action(
                    "RUN_DOCTOR",
                    "info",
                    "从 doctor 开始确认本地配置和远端契约状态。",
                    "dataspec doctor --project " + projectId + " --format json",
                    "README.md#cli",
                    true
            ));
        }

        return build(
                status,
                projectId,
                safeServer,
                authMode,
                snapshot.specVersion(),
                standardSnapshot,
                capabilities,
                recommendedCommands(projectId),
                knownRisks(capabilities, projectId),
                docsRefs(catalog, false),
                checks,
                nextActions
        );
    }

    private AiSessionBootstrap build(
            String status,
            Long projectId,
            String server,
            String authMode,
            String specVersion,
            AiSessionBootstrapSnapshot standardSnapshot,
            List<AiSessionBootstrapCapability> capabilities,
            List<String> recommendedCommands,
            List<String> knownRisks,
            List<String> docsRefs,
            List<AiSessionBootstrapCheck> checks,
            List<AiSessionBootstrapNextAction> nextActions
    ) {
        return new AiSessionBootstrap(
                KIND,
                SCHEMA_VERSION,
                LocalDateTime.now(),
                status,
                projectId,
                server,
                authMode,
                specVersion,
                standardSnapshot,
                capabilities,
                recommendedCommands,
                knownRisks,
                docsRefs,
                checks,
                nextActions
        );
    }

    private List<AiSessionBootstrapCapability> summarizeCapabilities(List<AiCapabilityEntry> entries) {
        return entries.stream()
                .filter(entry -> BOOTSTRAP_CAPABILITY_IDS.contains(entry.id()))
                .sorted(Comparator.comparingInt(entry -> BOOTSTRAP_CAPABILITY_IDS.indexOf(entry.id())))
                .map(entry -> new AiSessionBootstrapCapability(
                        entry.id(),
                        entry.title(),
                        entry.status(),
                        entry.writeRisk(),
                        entry.requiresProject(),
                        entry.apiEndpoints(),
                        entry.cliCommands(),
                        entry.mcpResources(),
                        entry.mcpTools(),
                        entry.nextActions()
                ))
                .toList();
    }

    private List<String> recommendedCommands(Long projectId) {
        if (projectId == null) {
            return List.of(
                    "dataspec doctor --format json",
                    "dataspec capability list --format json",
                    "dataspec bootstrap --project <id> --format json"
            );
        }
        return List.of(
                "dataspec doctor --project " + projectId + " --format json",
                "dataspec export-context --project " + projectId + " --cache",
                "dataspec lint <sql-file> --project " + projectId + " --format json",
                "dataspec workflow show reverse-import-standards --format json",
                "dataspec generate-ddl --project " + projectId + " --template <id> --table <snake_case_name> --format json"
        );
    }

    private List<String> knownRisks(List<AiSessionBootstrapCapability> capabilities, Long projectId) {
        List<String> risks = new ArrayList<>();
        if (projectId == null) {
            risks.add("缺少 projectId 时，项目级能力只能读取全局说明，不能可靠执行。");
        }
        capabilities.stream()
                .filter(capability -> !"READ_ONLY".equals(capability.writeRisk()))
                .forEach(capability -> risks.add(capability.id() + " 的 writeRisk=" + capability.writeRisk() + "，执行前需阅读 preflight/nextActions 并避免自动写入。"));
        risks.add("启动包不会执行 lint、导出 Context、反向导入或生成 DDL；它只告诉 AI 下一步该调用什么。");
        risks.add("不要把 API token、数据库密码或 JDBC URL 写入提示词、日志或启动包。");
        return risks;
    }

    private List<String> docsRefs(AiCapabilityCatalog catalog, boolean missingProject) {
        Set<String> refs = new LinkedHashSet<>();
        refs.add("README.md#ai-会话启动包");
        refs.add("README.md#cli");
        catalog.capabilities().stream()
                .map(AiCapabilityEntry::docsRef)
                .filter(ref -> ref != null && !ref.isBlank())
                .forEach(refs::add);
        if (missingProject) {
            refs.add("README.md#项目配置");
        }
        return refs.stream().toList();
    }

    private AiSessionBootstrapCheck check(String name, String status, String message, String nextAction) {
        return new AiSessionBootstrapCheck(name, status, message, nextAction);
    }

    private AiSessionBootstrapNextAction action(
            String code,
            String severity,
            String message,
            String command,
            String docsRef,
            boolean retryable
    ) {
        return new AiSessionBootstrapNextAction(code, severity, message, command, docsRef, retryable);
    }

    private String normalizeServer(String server) {
        if (server == null || server.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(server.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return SensitiveDataSanitizer.redactText(server.trim().replaceAll("/+$", ""));
            }
            int port = uri.getPort();
            String path = uri.getPath();
            String base = scheme + "://" + host + (port > -1 ? ":" + port : "") + (path == null ? "" : path);
            return SensitiveDataSanitizer.redactText(base.replaceAll("/+$", ""));
        } catch (IllegalArgumentException e) {
            return SensitiveDataSanitizer.redactText(server.trim().replaceAll("/+$", ""));
        }
    }
}
