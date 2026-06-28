package com.dataspec.evidence;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.model.AiBatchDeliveryPackage;
import com.dataspec.aibatch.model.AiBatchFixedSqlSummary;
import com.dataspec.aibatch.model.AiBatchIssueSummary;
import com.dataspec.aibatch.model.AiBatchRunDetail;
import com.dataspec.aibatch.model.AiBatchSqlLintReq;
import com.dataspec.aibatch.model.AiBatchSummary;
import com.dataspec.aibatch.service.AiBatchService;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.coverage.model.FieldCoverageReport;
import com.dataspec.coverage.model.FieldCoverageSummary;
import com.dataspec.coverage.model.FieldCoverageTable;
import com.dataspec.coverage.model.UnmanagedFieldRanking;
import com.dataspec.evidence.model.AiEvidencePackage;
import com.dataspec.evidence.model.AiEvidencePackageReq;
import com.dataspec.evidence.model.EvidenceSourceType;
import com.dataspec.evidence.service.impl.AiEvidencePackageServiceImpl;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.Severity;
import com.dataspec.lint.model.SqlCheckReplay;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiEvidencePackageServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AiEvidencePackageServiceImpl service = new AiEvidencePackageServiceImpl(
            new StubSqlCheckRecordService(),
            new StubAiJobRecordService(),
            new StubAiBatchService(),
            objectMapper
    );

    @Test
    void generatesSqlCheckEvidenceAndRedactsSensitiveValues() throws Exception {
        AiEvidencePackage pkg = service.generate(new AiEvidencePackageReq(
                null,
                EvidenceSourceType.SQL_CHECK,
                11L,
                null,
                null,
                null,
                null
        ));

        JsonNode json = objectMapper.valueToTree(pkg);
        assertEquals("dataspec-ai-evidence-package", pkg.kind());
        assertEquals(1, pkg.schemaVersion());
        assertEquals(7L, pkg.projectId());
        assertEquals("SQL_CHECK", pkg.source().sourceType().name());
        assertEquals("v1", pkg.standardSnapshot().specVersion());
        assertTrue(json.path("validationSummary").path("errorCount").asInt() > 0);
        assertTrue(json.toString().contains("[REDACTED]"));
        assertFalse(json.toString().contains("secret123"));
        assertFalse(json.toString().contains("ds_token_raw"));
        assertFalse(json.toString().contains("jdbc:postgresql://localhost:5432/app"));
    }

    @Test
    void generatesEvidenceForAiJobCoverageAndBatchSources() {
        AiEvidencePackage aiJob = service.generate(new AiEvidencePackageReq(null, EvidenceSourceType.AI_JOB, 21L, null, null, null, null));
        assertEquals(EvidenceSourceType.AI_JOB, aiJob.source().sourceType());
        assertEquals("SQL_FIX", aiJob.outputsSummary().get("jobType"));

        AiEvidencePackage batch = service.generate(new AiEvidencePackageReq(null, EvidenceSourceType.AI_BATCH_RUN, 31L, null, null, null, null));
        assertEquals(EvidenceSourceType.AI_BATCH_RUN, batch.source().sourceType());
        assertEquals("DONE", batch.validationSummary().get("status"));

        AiEvidencePackage coverage = service.generate(new AiEvidencePackageReq(
                7L,
                EvidenceSourceType.COVERAGE_REPORT,
                null,
                "覆盖率报告 password=secret123",
                coverageReport(),
                null,
                Map.of("mode", "database", "connection", "jdbc:mysql://localhost:3306/app?password=secret123")
        ));
        assertEquals(EvidenceSourceType.COVERAGE_REPORT, coverage.source().sourceType());
        assertEquals("database", ((Map<?, ?>) coverage.inputsSummary().get("payloadSummary")).get("mode"));
        assertFalse(objectMapper.valueToTree(coverage).toString().contains("secret123"));
        assertFalse(objectMapper.valueToTree(coverage).toString().contains("jdbc:mysql://localhost:3306/app"));
        assertTrue(coverage.diagnostics().stream().anyMatch(item -> "PAYLOAD_SOURCE".equals(item.code())));
    }

    @Test
    void zipContainsStableFilesAndRedactedEvidenceJson() throws Exception {
        byte[] zip = service.generateZip(new AiEvidencePackageReq(null, EvidenceSourceType.SQL_CHECK, 11L, null, null, null, null));
        Map<String, String> entries = unzip(zip);

        assertTrue(entries.containsKey("evidence.json"));
        assertTrue(entries.containsKey("summary.md"));
        assertTrue(entries.containsKey("README.md"));
        assertTrue(entries.get("evidence.json").contains("dataspec-ai-evidence-package"));
        assertTrue(entries.get("summary.md").contains("DataSpec AI Evidence Package"));
        assertFalse(String.join("\n", entries.values()).contains("secret123"));
        assertFalse(String.join("\n", entries.values()).contains("ds_token_raw"));
    }

    private FieldCoverageReport coverageReport() {
        FieldCoverageReport report = new FieldCoverageReport();
        FieldCoverageSummary summary = new FieldCoverageSummary();
        summary.setTableCount(1);
        summary.setColumnCount(2);
        summary.setCoveredCount(1);
        summary.setUnmanagedCount(1);
        summary.setCoverageRate(50.0);
        report.setSummary(summary);

        FieldCoverageTable table = new FieldCoverageTable();
        table.setTableName("orders");
        table.setColumnCount(2);
        table.setCoveredCount(1);
        table.setUnmanagedCount(1);
        table.setCoverageRate(50.0);
        report.setTables(List.of(table));

        UnmanagedFieldRanking ranking = new UnmanagedFieldRanking();
        ranking.setColumnName("legacy_phone");
        ranking.setCount(1);
        ranking.setRecommendedFieldName("mobile_no");
        ranking.setReason("token=ds_token_raw");
        report.setUnmanagedRankings(List.of(ranking));
        return report;
    }

    private Map<String, String> unzip(byte[] zip) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            var entry = input.getNextEntry();
            while (entry != null) {
                entries.put(entry.getName(), new String(input.readAllBytes()));
                entry = input.getNextEntry();
            }
        }
        return entries;
    }

    private static class StubSqlCheckRecordService implements SqlCheckRecordService {
        @Override
        public SqlCheckRecord save(Long projectId, String originalSql, LintResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IPage<SqlCheckRecord> listByProject(Long projectId, int current, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SqlCheckRecord getById(Long id) {
            SqlCheckRecord record = new SqlCheckRecord();
            record.setId(id);
            record.setProjectId(7L);
            record.setOriginalSql("select * from users where password=secret123 and token=ds_token_raw");
            record.setFixedSql("select id from users");
            record.setErrorCount(1);
            record.setWarningCount(0);
            record.setSuggestionCount(1);
            record.setStandardSnapshotId(3L);
            record.setStandardSnapshotVersion("v1");
            record.setStandardSnapshotHash("hash1");
            return record;
        }

        @Override
        public List<LintIssue> parseIssues(SqlCheckRecord record) {
            return List.of(LintIssue.builder()
                    .severity(Severity.ERROR)
                    .ruleCode("sensitive_literal")
                    .message("jdbc:postgresql://localhost:5432/app Authorization: Bearer abc")
                    .tableName("users")
                    .columnName("password")
                    .build());
        }

        @Override
        public SqlCheckReplay buildReplay(SqlCheckRecord record) {
            return new SqlCheckReplay(
                    new StandardSnapshotInfo(3L, 7L, "v1", "快照", null, "hash1", LocalDateTime.now(), true),
                    StandardSnapshotInfo.unversioned(7L),
                    "STALE",
                    new SqlCheckReplay.Summary(false, 1, 0, 2, "dataspec export-context --dataspec-token=ds_token_raw"),
                    List.of("重新运行 lint password=secret123")
            );
        }
    }

    private static class StubAiJobRecordService implements AiJobRecordService {
        @Override
        public AiJobRecord create(AiJobRecordCreateReq req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IPage<AiJobRecord> listByProject(Long projectId, String jobType, int current, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiJobRecordDetail getDetail(Long id) {
            AiJobRecord record = new AiJobRecord();
            record.setId(id);
            record.setProjectId(7L);
            record.setJobType("SQL_FIX");
            record.setTitle("修复 SQL");
            record.setInputSummary("password=secret123");
            record.setPromptVersion("p1");
            record.setStatus("DONE");
            record.setStandardSnapshotVersion("v1");
            return new AiJobRecordDetail(
                    record,
                    Map.of("token", "ds_token_raw"),
                    Map.of("fixedSqlAvailable", true),
                    Map.of("jobId", id, "password", "secret123"),
                    "dataspec ai replay --id " + id
            );
        }
    }

    private static class StubAiBatchService implements AiBatchService {
        @Override
        public AiBatchDeliveryPackage createSqlLintBatch(AiBatchSqlLintReq req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IPage<AiBatchRun> listByProject(Long projectId, int current, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiBatchRunDetail getDetail(Long id) {
            AiBatchRun run = new AiBatchRun();
            run.setId(id);
            run.setProjectId(7L);
            run.setBatchType("SQL_LINT");
            run.setSource("local password=secret123");
            run.setStatus("DONE");
            return new AiBatchRunDetail(run, getPackage(id));
        }

        @Override
        public AiBatchDeliveryPackage getPackage(Long id) {
            return new AiBatchDeliveryPackage(
                    "1",
                    "batch-" + id,
                    7L,
                    "SQL_LINT",
                    "local",
                    "DONE",
                    new AiBatchSummary(2, 2, 0, 1, 0, 0, 1),
                    List.of(),
                    new AiBatchIssueSummary(1, 0, 0, List.of()),
                    new AiBatchFixedSqlSummary(1, 1),
                    List.of(),
                    List.of(),
                    List.of("下载证据包 token=ds_token_raw"),
                    LocalDateTime.now()
            );
        }
    }
}
