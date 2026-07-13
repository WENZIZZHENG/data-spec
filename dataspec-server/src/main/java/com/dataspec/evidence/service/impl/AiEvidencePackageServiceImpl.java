package com.dataspec.evidence.service.impl;

import com.dataspec.aibatch.model.AiBatchDeliveryPackage;
import com.dataspec.aibatch.model.AiBatchRunDetail;
import com.dataspec.aibatch.service.AiBatchService;
import com.dataspec.aitaskrun.model.AiTaskRunDetail;
import com.dataspec.aitaskrun.service.AiTaskRunService;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.common.exception.BizException;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.model.FieldCoverageTable;
import com.dataspec.coverage.model.UnmanagedFieldRanking;
import com.dataspec.evidence.model.AiEvidenceArtifact;
import com.dataspec.evidence.model.AiEvidenceDiagnostic;
import com.dataspec.evidence.model.AiEvidencePackage;
import com.dataspec.evidence.model.AiEvidencePackageReq;
import com.dataspec.evidence.model.AiEvidenceSource;
import com.dataspec.evidence.model.AiEvidenceStandardSnapshot;
import com.dataspec.evidence.model.EvidenceSourceType;
import com.dataspec.evidence.service.AiEvidencePackageService;
import com.dataspec.evidenceclaim.service.EvidenceClaimResolver;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.SqlCheckReplay;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class AiEvidencePackageServiceImpl implements AiEvidencePackageService {

    public static final String KIND = "dataspec-ai-evidence-package";
    public static final int SCHEMA_VERSION = 1;

    private static final int TEXT_PREVIEW_LIMIT = 600;
    private static final int LIST_PREVIEW_LIMIT = 12;

    private final SqlCheckRecordService sqlCheckRecordService;
    private final AiJobRecordService aiJobRecordService;
    private final AiBatchService aiBatchService;
    private final AiTaskRunService aiTaskRunService;
    private final EvidenceClaimResolver evidenceClaimResolver;
    private final ObjectMapper objectMapper;

    @Override
    public AiEvidencePackage generate(AiEvidencePackageReq req) {
        if (req == null || req.sourceType() == null) {
            throw new BizException("sourceType 不能为空，支持: AI_JOB, SQL_CHECK, COVERAGE_REPORT, AI_BATCH_RUN, AI_TASK_RUN");
        }
        return switch (req.sourceType()) {
            case SQL_CHECK -> fromSqlCheck(req);
            case AI_JOB -> fromAiJob(req);
            case AI_BATCH_RUN -> fromAiBatch(req);
            case AI_TASK_RUN -> fromAiTaskRun(req);
            case COVERAGE_REPORT -> fromCoverage(req);
        };
    }

    @Override
    public byte[] generateZip(AiEvidencePackageReq req) {
        AiEvidencePackage evidencePackage = generate(req);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            addText(zip, "evidence.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidencePackage));
            addText(zip, "summary.md", summaryMarkdown(evidencePackage));
            addText(zip, "README.md", readmeMarkdown(evidencePackage));
            zip.finish();
            return output.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成 AI evidence zip 失败: " + e.getMessage());
        }
    }

    private AiEvidencePackage fromSqlCheck(AiEvidencePackageReq req) {
        Long sourceId = requireSourceId(req);
        SqlCheckRecord record = sqlCheckRecordService.getById(sourceId);
        List<LintIssue> issues = sqlCheckRecordService.parseIssues(record);
        SqlCheckReplay replay = sqlCheckRecordService.buildReplay(record);
        Long projectId = firstNonNull(req.projectId(), record.getProjectId());

        Map<String, Object> inputs = orderedMap(
                "sqlPreview", preview(record.getOriginalSql()),
                "recordId", record.getId()
        );
        Map<String, Object> outputs = orderedMap(
                "fixedSqlAvailable", hasText(record.getFixedSql()),
                "fixedSqlPreview", preview(record.getFixedSql()),
                "issueSamples", issueSamples(issues)
        );
        Map<String, Object> validation = orderedMap(
                "errorCount", zero(record.getErrorCount()),
                "warningCount", zero(record.getWarningCount()),
                "suggestionCount", zero(record.getSuggestionCount()),
                "replayStatus", replay != null ? replay.status() : null
        );
        List<AiEvidenceArtifact> artifacts = List.of(
                new AiEvidenceArtifact("sql-check-record", "SQL 检查记录", "json", orderedMap("id", record.getId())),
                new AiEvidenceArtifact("replay", "标准快照回放", "json", sanitizeToMap(replay))
        );
        return build(projectId,
                source(EvidenceSourceType.SQL_CHECK, sourceId, "SQL 检查记录 #" + sourceId, statusFromCounts(record), true),
                snapshot(record.getStandardSnapshotId(), record.getStandardSnapshotVersion(), record.getStandardSnapshotHash()),
                inputs,
                outputs,
                validation,
                req.postCheckSummary(),
                artifacts,
                List.of("查看 fixedSqlDiff 和 issueSamples 后再决定是否应用修复。", "如标准已变化，先重新运行 lint 或导出最新 AI Context。"),
                List.of("dataspec lint <path|-> --project " + projectId + " --format json"),
                List.of()
        );
    }

    private AiEvidencePackage fromAiJob(AiEvidencePackageReq req) {
        Long sourceId = requireSourceId(req);
        AiJobRecordDetail detail = aiJobRecordService.getDetail(sourceId);
        AiJobRecord record = detail.record();
        Long projectId = firstNonNull(req.projectId(), record.getProjectId());
        Map<String, Object> inputs = orderedMap(
                "inputSummary", preview(record.getInputSummary()),
                "promptVersion", record.getPromptVersion(),
                "payloadSummary", summarizeObject(detail.inputPayload())
        );
        Map<String, Object> outputs = orderedMap(
                "jobType", record.getJobType(),
                "status", record.getStatus(),
                "payloadSummary", summarizeObject(detail.outputPayload())
        );
        Map<String, Object> validation = orderedMap(
                "status", record.getStatus(),
                "sqlCheckRecordId", record.getSqlCheckRecordId()
        );
        List<AiEvidenceArtifact> artifacts = new ArrayList<>();
        artifacts.add(new AiEvidenceArtifact("ai-job-record", "AI 作业记录", "json", orderedMap("id", record.getId(), "jobType", record.getJobType())));
        artifacts.add(new AiEvidenceArtifact("replay-payload", "回放 payload 摘要", "json", sanitizeToMap(detail.replayPayload())));
        return build(projectId,
                source(EvidenceSourceType.AI_JOB, sourceId, firstText(record.getTitle(), "AI 作业 #" + sourceId), record.getStatus(), true),
                snapshot(record.getStandardSnapshotId(), record.getStandardSnapshotVersion(), record.getStandardSnapshotHash()),
                inputs,
                outputs,
                validation,
                req.postCheckSummary(),
                artifacts,
                List.of("需要复现时使用 replayCommand，并确认当前标准快照是否一致。"),
                List.of(firstText(detail.replayCommand(), "dataspec replay ai-job --id " + sourceId)),
                List.of()
        );
    }

    private AiEvidencePackage fromAiBatch(AiEvidencePackageReq req) {
        Long sourceId = requireSourceId(req);
        AiBatchRunDetail detail = aiBatchService.getDetail(sourceId);
        AiBatchDeliveryPackage deliveryPackage = detail.deliveryPackage();
        Long projectId = firstNonNull(req.projectId(), detail.run().getProjectId(), deliveryPackage != null ? deliveryPackage.projectId() : null);
        Map<String, Object> inputs = orderedMap(
                "batchType", detail.run().getBatchType(),
                "source", preview(detail.run().getSource())
        );
        Map<String, Object> outputs = orderedMap(
                "summary", deliveryPackage != null ? sanitizeToMap(deliveryPackage.summary()) : Map.of(),
                "issueSummary", deliveryPackage != null ? sanitizeToMap(deliveryPackage.issueSummary()) : Map.of(),
                "fixedSqlSummary", deliveryPackage != null ? sanitizeToMap(deliveryPackage.fixedSqlSummary()) : Map.of()
        );
        Map<String, Object> validation = orderedMap(
                "status", detail.run().getStatus(),
                "failedItems", deliveryPackage != null && deliveryPackage.summary() != null ? deliveryPackage.summary().failedItems() : 0
        );
        List<AiEvidenceArtifact> artifacts = List.of(
                new AiEvidenceArtifact("ai-batch-run", "AI 批量任务运行", "json", orderedMap("id", detail.run().getId(), "status", detail.run().getStatus())),
                new AiEvidenceArtifact("ai-batch-delivery", "AI 批量交付包摘要", "json", outputs)
        );
        return build(projectId,
                source(EvidenceSourceType.AI_BATCH_RUN, sourceId, "AI 批量任务 #" + sourceId, detail.run().getStatus(), true),
                AiEvidenceStandardSnapshot.unversioned(),
                inputs,
                outputs,
                validation,
                req.postCheckSummary(),
                artifacts,
                deliveryPackage != null && deliveryPackage.nextActions() != null ? sanitizeStringList(deliveryPackage.nextActions()) : List.of("查看失败项并按需重试。"),
                List.of("dataspec evidence export --source-type AI_BATCH_RUN --source-id " + sourceId + " --format json"),
                List.of()
        );
    }

    private AiEvidencePackage fromAiTaskRun(AiEvidencePackageReq req) {
        Long sourceId = requireSourceId(req);
        if (req.projectId() == null) {
            throw new BizException("AI_TASK_RUN 需要 projectId");
        }
        AiTaskRunDetail detail = aiTaskRunService.detail(req.projectId(), sourceId);
        Map<String, Object> inputs = orderedMap(
                "taskType", detail.taskType(),
                "sourceType", detail.sourceType(),
                "sourceId", detail.sourceId(),
                "inputHash", detail.inputHash()
        );
        Map<String, Object> outputs = orderedMap(
                "status", detail.status(),
                "retryable", detail.retryable(),
                "failedStep", detail.failedStep(),
                "resumeCommand", detail.resumeCommand(),
                "nextAction", detail.nextAction()
        );
        Map<String, Object> validation = orderedMap(
                "status", detail.status(),
                "retryable", detail.retryable(),
                "failedStep", detail.failedStep()
        );
        List<AiEvidenceArtifact> artifacts = List.of(
                new AiEvidenceArtifact("ai-task-run", "AI 任务运行状态", "json", sanitizeToMap(detail)),
                new AiEvidenceArtifact("ai-task-partial-artifacts", "AI 任务已完成产物摘要", "json", orderedMap("items", detail.partialArtifacts()))
        );
        List<String> suggestedCommands = hasText(detail.resumeCommand())
                ? List.of(detail.resumeCommand())
                : List.of("dataspec task show " + sourceId + " --project " + req.projectId() + " --format json");
        return build(req.projectId(),
                source(EvidenceSourceType.AI_TASK_RUN, sourceId, "AI 任务运行 #" + sourceId, detail.status(), true),
                AiEvidenceStandardSnapshot.unversioned(),
                inputs,
                outputs,
                validation,
                req.postCheckSummary(),
                artifacts,
                List.of(firstText(detail.nextAction(), "查看任务状态后决定是否重试。")),
                suggestedCommands,
                List.of()
        );
    }

    private AiEvidencePackage fromCoverage(AiEvidencePackageReq req) {
        FieldCoverageReport report = req.coverageReport();
        if (report == null) {
            throw new BizException("coverageReport 不能为空，COVERAGE_REPORT 需要传入当前覆盖率报告摘要");
        }
        Long projectId = req.projectId();
        if (projectId == null) {
            throw new BizException("projectId 不能为空，COVERAGE_REPORT 需要项目 ID");
        }
        Map<String, Object> inputs = orderedMap(
                "sourceTitle", firstText(req.sourceTitle(), "字段覆盖率报告"),
                "payloadBased", true,
                "payloadSummary", sanitizeToMap(req.payloadSummary())
        );
        Map<String, Object> outputs = orderedMap(
                "summary", sanitizeToMap(report.getSummary()),
                "tableSamples", tableSamples(report.getTables()),
                "unmanagedRankingSamples", rankingSamples(report.getUnmanagedRankings())
        );
        Map<String, Object> validation = orderedMap(
                "coverageRate", report.getSummary() != null ? report.getSummary().getCoverageRate() : 0,
                "unmanagedCount", report.getSummary() != null ? report.getSummary().getUnmanagedCount() : 0
        );
        return build(projectId,
                source(EvidenceSourceType.COVERAGE_REPORT, null, firstText(req.sourceTitle(), "字段覆盖率报告"), "READY", false),
                req.standardSnapshot() != null ? req.standardSnapshot() : AiEvidenceStandardSnapshot.unversioned(),
                inputs,
                outputs,
                validation,
                req.postCheckSummary(),
                List.of(new AiEvidenceArtifact("coverage-report", "字段覆盖率报告摘要", "json", outputs)),
                List.of("优先处理 unmanagedRankingSamples 中出现频次最高的字段。", "需要写入标准字段前先进入候选或反向导入确认流程。"),
                List.of("dataspec coverage report --project " + projectId + " --format json"),
                List.of(new AiEvidenceDiagnostic("INFO", "PAYLOAD_SOURCE", "覆盖率报告为即时 payload source，未持久化到 DataSpec。"))
        );
    }

    private AiEvidencePackage build(Long projectId,
                                    AiEvidenceSource source,
                                    AiEvidenceStandardSnapshot snapshot,
                                    Map<String, Object> inputs,
                                    Map<String, Object> outputs,
                                    Map<String, Object> validation,
                                    Map<String, Object> postCheckSummary,
                                    List<AiEvidenceArtifact> artifacts,
                                    List<String> nextActions,
                                    List<String> suggestedCommands,
                                    List<AiEvidenceDiagnostic> diagnostics) {
        String packageId = "evidence-" + source.sourceType().name().toLowerCase() + "-" + UUID.randomUUID();
        return new AiEvidencePackage(
                KIND,
                SCHEMA_VERSION,
                packageId,
                projectId,
                Instant.now(),
                source,
                snapshot != null ? snapshot : AiEvidenceStandardSnapshot.unversioned(),
                sanitizeToMap(inputs),
                sanitizeToMap(outputs),
                sanitizeToMap(validation),
                sanitizePostCheckSummary(postCheckSummary, projectId),
                artifacts == null ? List.of() : artifacts.stream()
                        .map(item -> new AiEvidenceArtifact(item.artifactType(), sanitizeText(item.title()), item.format(), sanitizeToMap(item.summary())))
                        .toList(),
                sanitizeStringList(nextActions),
                sanitizeStringList(suggestedCommands),
                diagnostics == null ? List.of() : diagnostics.stream()
                        .map(item -> new AiEvidenceDiagnostic(item.level(), item.code(), sanitizeText(item.message())))
                        .toList()
        );
    }

    private AiEvidenceSource source(
            EvidenceSourceType sourceType,
            Long sourceId,
            String sourceTitle,
            String status,
            boolean persisted
    ) {
        String evidenceRef = persisted ? evidenceClaimResolver.canonicalRef(sourceType, sourceId) : null;
        return new AiEvidenceSource(sourceType, sourceId, sourceTitle, status, persisted, evidenceRef);
    }

    private Long requireSourceId(AiEvidencePackageReq req) {
        if (req.sourceId() == null) {
            throw new BizException(req.sourceType() + " 需要 sourceId");
        }
        return req.sourceId();
    }

    private AiEvidenceStandardSnapshot snapshot(Long id, String version, String hash) {
        if (id == null && !hasText(version) && !hasText(hash)) {
            return AiEvidenceStandardSnapshot.unversioned();
        }
        return new AiEvidenceStandardSnapshot(id, firstText(version, "unversioned"), hash, id != null || hasText(version) || hasText(hash));
    }

    private String statusFromCounts(SqlCheckRecord record) {
        return zero(record.getErrorCount()) > 0 ? "ERROR" : "READY";
    }

    private List<Map<String, Object>> issueSamples(List<LintIssue> issues) {
        if (issues == null) {
            return List.of();
        }
        return issues.stream().limit(LIST_PREVIEW_LIMIT).map(issue -> orderedMap(
                "severity", issue.getSeverity() != null ? issue.getSeverity().name() : null,
                "ruleCode", issue.getRuleCode(),
                "message", preview(issue.getMessage()),
                "tableName", issue.getTableName(),
                "columnName", issue.getColumnName(),
                "suppressed", issue.getSuppressed()
        )).toList();
    }

    private List<Map<String, Object>> tableSamples(List<FieldCoverageTable> tables) {
        if (tables == null) {
            return List.of();
        }
        return tables.stream().limit(LIST_PREVIEW_LIMIT).map(table -> orderedMap(
                "tableName", table.getTableName(),
                "columnCount", table.getColumnCount(),
                "coveredCount", table.getCoveredCount(),
                "unmanagedCount", table.getUnmanagedCount(),
                "coverageRate", table.getCoverageRate()
        )).toList();
    }

    private List<Map<String, Object>> rankingSamples(List<UnmanagedFieldRanking> rankings) {
        if (rankings == null) {
            return List.of();
        }
        return rankings.stream().limit(LIST_PREVIEW_LIMIT).map(item -> orderedMap(
                "columnName", item.getColumnName(),
                "count", item.getCount(),
                "recommendedFieldName", item.getRecommendedFieldName(),
                "reason", preview(item.getReason())
        )).toList();
    }

    private Map<String, Object> summarizeObject(Object value) {
        Map<String, Object> map = sanitizeToMap(value);
        return orderedMap(
                "type", value == null ? "null" : value.getClass().getSimpleName(),
                "keys", new ArrayList<>(map.keySet()).stream().limit(LIST_PREVIEW_LIMIT).toList(),
                "preview", map
        );
    }

    private Map<String, Object> sanitizePostCheckSummary(Map<String, Object> value, Long projectId) {
        if (value == null || value.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        copyPostCheckValue(result, value, "status");
        copyPostCheckValue(result, value, "safeToUse");
        copyPostCheckValue(result, value, "issueCounts");
        copyPostCheckValue(result, value, "blockingRefs");
        copyPostCheckValue(result, value, "replacementRefs");
        copyPostCheckValue(result, value, "evidenceLinks");
        copyPostCheckValue(result, value, "nextActions");
        Object command = value.get("suggestedCheckCommand");
        result.put("suggestedCheckCommand", command instanceof String text && hasText(text)
                ? preview(text)
                : "dataspec ai-output check --project " + projectId + " --type <contentType> --file <path> --format json");
        return result;
    }

    private void copyPostCheckValue(Map<String, Object> target, Map<String, Object> source, String key) {
        if (source.containsKey(key)) {
            target.put(key, sanitizeValue(source.get(key), 0));
        }
    }

    private Map<String, Object> sanitizeToMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        Object plain = value instanceof Map<?, ?> ? value : objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
        Object sanitized = sanitizeValue(plain, 0);
        if (sanitized instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return orderedMap("value", sanitized);
    }

    private Object sanitizeValue(Object value, int depth) {
        if (value == null) {
            return null;
        }
        if (depth > 5) {
            return "[TRUNCATED_DEPTH]";
        }
        if (value instanceof String text) {
            return preview(text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count++ >= LIST_PREVIEW_LIMIT) {
                    result.put("_truncated", true);
                    break;
                }
                String key = String.valueOf(entry.getKey());
                result.put(key, SensitiveDataSanitizer.isSensitiveKey(key)
                        ? SensitiveDataSanitizer.REDACTION
                        : sanitizeValue(entry.getValue(), depth + 1));
            }
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            int count = 0;
            for (Object item : iterable) {
                if (count++ >= LIST_PREVIEW_LIMIT) {
                    result.add("[TRUNCATED_LIST]");
                    break;
                }
                result.add(sanitizeValue(item, depth + 1));
            }
            return result;
        }
        return value;
    }

    private String preview(String text) {
        String sanitized = sanitizeText(text);
        if (sanitized == null || sanitized.length() <= TEXT_PREVIEW_LIMIT) {
            return sanitized;
        }
        return sanitized.substring(0, TEXT_PREVIEW_LIMIT) + "...[TRUNCATED]";
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        return SensitiveDataSanitizer.redactText(text);
    }

    private List<String> sanitizeStringList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().limit(LIST_PREVIEW_LIMIT).map(this::preview).toList();
    }

    private String summaryMarkdown(AiEvidencePackage pkg) {
        return """
                # DataSpec AI Evidence Package

                - Package ID: `%s`
                - Source: `%s`
                - Status: `%s`
                - Project ID: `%s`
                - Standard: `%s`

                ## Validation

                ```json
                %s
                ```

                ## Next Actions

                %s
                """.formatted(
                pkg.packageId(),
                pkg.source().sourceType(),
                pkg.source().status(),
                pkg.projectId(),
                pkg.standardSnapshot().specVersion(),
                toJson(pkg.validationSummary()),
                markdownList(pkg.nextActions())
        );
    }

    private String readmeMarkdown(AiEvidencePackage pkg) {
        return """
                # DataSpec Evidence Package

                This zip is generated locally by DataSpec for AI handoff and human review.
                It is a read-only evidence artifact, not an approval record, audit log, or permission boundary.

                Files:

                - `evidence.json`: machine-readable package.
                - `summary.md`: human-readable summary.
                - `README.md`: this file.

                Sensitive tokens, passwords, Authorization headers, and complete JDBC URLs are redacted.

                Package ID: `%s`
                """.formatted(pkg.packageId());
    }

    private String markdownList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- 无";
        }
        return items.stream().map(item -> "- " + item).reduce((a, b) -> a + "\n" + b).orElse("- 无");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void addText(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return sanitizeText(value);
            }
        }
        return "";
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    private Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
