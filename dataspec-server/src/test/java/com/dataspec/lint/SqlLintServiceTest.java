package com.dataspec.lint;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.lint.engine.FixedSqlGenerator;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.SqlCheckReplay;
import com.dataspec.lint.rules.RecommendedFieldNameRule;
import com.dataspec.lint.rules.RequiredColumnsRule;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQL 校验服务单元测试（不依赖 Spring 容器）
 */
class SqlLintServiceTest {

    @Test
    void lintWithProjectWithoutRuleConfig_fallsBackToBuiltInRules() {
        RecordingCheckRecordService recordService = new RecordingCheckRecordService();
        SqlLintService service = newService(recordService);

        LintResult result = service.lint("""
                CREATE TABLE UserOrder (
                    id bigserial PRIMARY KEY
                );
                """, 1L);

        assertTrue(result.getIssues().stream()
                        .anyMatch(issue -> "table_naming_snake_case".equals(issue.getRuleCode())),
                "新项目无规则配置时应回退执行内置规则");
        // 应同步生成修正 SQL 并落库
        assertNotNull(result.getFixedSql(), "应生成修正 SQL");
        assertTrue(result.getFixedSql().contains("user_order"), "修正 SQL 应含重命名后的表名");
        assertNotNull(result.getFixedSqlDiff(), "应生成修正 SQL diff");
        assertTrue(result.getFixedSqlDiff().contains("-CREATE TABLE UserOrder"));
        assertTrue(result.getFixedSqlDiff().contains("+CREATE TABLE user_order"));
        assertEquals(1, recordService.saved.size(), "应保存 1 条检查记录");
    }

    @Test
    void lintPersistsRecordAndFillsFixedSqlForBadExample() {
        RecordingCheckRecordService recordService = new RecordingCheckRecordService();
        // 注册完整规则集,让验收场景(create_time→created_at + 补必备列)真实生效
        SqlLintService service = new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                List.of(
                        new TableNameSnakeCaseRule(),
                        new RecommendedFieldNameRule(),
                        new RequiredColumnsRule()
                ),
                new ObjectMapper(),
                new FixedSqlGenerator(),
                recordService,
                new NoopAiJobRecordService(),
                new NoopRuleExemptionService()
        );

        // 验收示例:含 create_time 且缺 created_at 等必备列
        LintResult result = service.lint("""
                CREATE TABLE t_user (
                    id bigserial PRIMARY KEY,
                    create_time timestamp NOT NULL,
                    phone varchar(20)
                );
                """, null);

        assertNotNull(result.getFixedSql());
        // create_time 经 RecommendedFieldNameRule 建议 → created_at
        assertTrue(result.getFixedSql().contains("created_at"));
        // 必备列缺失应补齐(updated_at/is_deleted 在 create_time 改名后仍缺)
        assertTrue(result.getFixedSql().contains("updated_at"));
        assertTrue(result.getFixedSql().contains("is_deleted"));
        assertEquals(1, recordService.saved.size());
        SqlCheckRecord record = recordService.saved.get(0);
        assertNotNull(record.getIssuesJson());
        assertTrue(record.getIssuesJson().startsWith("["));
    }

    @Test
    void lintFillsSourceLocationForTableAndColumnIssues() {
        RecordingCheckRecordService recordService = new RecordingCheckRecordService();
        SqlLintService service = new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                List.of(
                        new TableNameSnakeCaseRule(),
                        new com.dataspec.lint.rules.FieldNamingSnakeCaseRule()
                ),
                new ObjectMapper(),
                new FixedSqlGenerator(),
                recordService,
                new NoopAiJobRecordService(),
                new NoopRuleExemptionService()
        );

        LintResult result = service.lint("""
                CREATE TABLE UserOrder (
                    userId bigint NOT NULL
                );
                """, null);

        LintIssue tableIssue = result.getIssues().stream()
                .filter(issue -> "table_naming_snake_case".equals(issue.getRuleCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, tableIssue.getLine());
        assertEquals(14, tableIssue.getColumn());
        assertEquals(1, tableIssue.getLineEnd());
        assertEquals(23, tableIssue.getColumnEnd());
        assertEquals("table", tableIssue.getLocationKind());
        assertNotNull(tableIssue.getSourceStart());
        assertTrue(tableIssue.getSourceEnd() > tableIssue.getSourceStart());

        LintIssue columnIssue = result.getIssues().stream()
                .filter(issue -> "field_naming_snake_case".equals(issue.getRuleCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, columnIssue.getLine());
        assertEquals(5, columnIssue.getColumn());
        assertEquals(2, columnIssue.getLineEnd());
        assertEquals(11, columnIssue.getColumnEnd());
        assertEquals("column", columnIssue.getLocationKind());
        assertNotNull(columnIssue.getSourceStart());
        assertTrue(columnIssue.getSourceEnd() > columnIssue.getSourceStart());
        assertTrue(recordService.saved.get(0).getIssuesJson().contains("\"line\":1"));
        assertTrue(recordService.saved.get(0).getIssuesJson().contains("\"locationKind\":\"table\""));
    }

    @Test
    void lintAiContract_exposesStableIssueAndFixedSqlFields() throws Exception {
        RecordingCheckRecordService recordService = new RecordingCheckRecordService();
        SqlLintService service = newService(recordService);

        LintResult result = service.lint("""
                CREATE TABLE UserOrder (
                    id bigserial PRIMARY KEY
                );
                """, 1L);

        JsonNode root = new ObjectMapper().valueToTree(result);
        assertEquals(1, root.path("errorCount").asInt());
        assertEquals(0, root.path("warningCount").asInt());
        assertEquals(0, root.path("suggestionCount").asInt());
        assertEquals(0, root.path("suppressedCount").asInt());
        assertTrue(root.path("fixedSql").asText().contains("CREATE TABLE user_order"));
        assertTrue(root.path("fixedSqlDiff").asText().contains("-CREATE TABLE UserOrder"));
        assertTrue(root.path("dialectDiagnostics").isArray());
        assertEquals("POSTGRESQL_DIALECT_INFERRED", root.path("dialectDiagnostics").get(0).path("code").asText());

        JsonNode issue = root.path("issues").get(0);
        assertEquals("ERROR", issue.path("severity").asText());
        assertEquals("table_naming_snake_case", issue.path("ruleCode").asText());
        assertEquals("UserOrder", issue.path("tableName").asText());
        assertEquals("user_order", issue.path("replacement").asText());
        assertEquals("UserOrder", issue.path("before").asText());
        assertEquals("user_order", issue.path("after").asText());
        assertEquals(90, issue.path("confidence").asInt());
        assertEquals(1, issue.path("line").asInt());
        assertEquals(14, issue.path("column").asInt());
        assertEquals("table", issue.path("locationKind").asText());

        JsonNode persistedIssue = new ObjectMapper()
                .readTree(recordService.saved.get(0).getIssuesJson())
                .get(0);
        assertEquals("table_naming_snake_case", persistedIssue.path("ruleCode").asText());
        assertEquals("table", persistedIssue.path("locationKind").asText());
    }

    @Test
    void lintKeepsUnresolvedIssueWithoutLocation() {
        RecordingCheckRecordService recordService = new RecordingCheckRecordService();
        SqlLintService service = new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                List.of(new com.dataspec.lint.model.LintRule() {
                    @Override
                    public String getCode() {
                        return "synthetic_unresolved";
                    }

                    @Override
                    public String getName() {
                        return "合成不可定位问题";
                    }

                    @Override
                    public List<LintIssue> check(com.dataspec.lint.model.RuleContext context) {
                        return List.of(LintIssue.builder()
                                .severity(com.dataspec.lint.model.Severity.WARNING)
                                .ruleCode(getCode())
                                .ruleName(getName())
                                .message("不可定位的问题")
                                .build());
                    }
                }),
                new ObjectMapper(),
                new FixedSqlGenerator(),
                recordService,
                new NoopAiJobRecordService(),
                new NoopRuleExemptionService()
        );

        LintResult result = service.lint("CREATE TABLE users (id bigint);", null);

        LintIssue issue = result.getIssues().get(0);
        assertNull(issue.getLine());
        assertNull(issue.getColumn());
        assertNull(issue.getSourceStart());
        assertNull(issue.getSourceEnd());
    }

    private SqlLintService newService(SqlCheckRecordService recordService) {
        return newService(recordService, new NoopAiJobRecordService());
    }

    private SqlLintService newService(SqlCheckRecordService recordService, AiJobRecordService aiJobRecordService) {
        return new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                List.of(new TableNameSnakeCaseRule()),
                new ObjectMapper(),
                new FixedSqlGenerator(),
                recordService,
                aiJobRecordService,
                new NoopRuleExemptionService()
        );
    }

    @Test
    void lintRecordsAiReplayJobForProject() {
        RecordingCheckRecordService recordService = new RecordingCheckRecordService();
        RecordingAiJobRecordService aiJobRecordService = new RecordingAiJobRecordService();
        SqlLintService service = newService(recordService, aiJobRecordService);

        LintResult result = service.lint("""
                CREATE TABLE UserOrder (
                    id bigserial PRIMARY KEY
                );
                """, 1L);

        assertNotNull(result.getFixedSql());
        assertEquals(1, aiJobRecordService.created.size());
        AiJobRecordCreateReq req = aiJobRecordService.created.get(0);
        assertEquals("SQL_LINT_FIX", req.jobType());
        assertEquals("sql-lint-fix@1", req.promptVersion());
        assertEquals(7L, req.sqlCheckRecordId());
        assertTrue(req.inputPayload().toString().contains("UserOrder"));
        assertTrue(req.outputPayload().toString().contains("fixedSql"));
        assertTrue(req.outputPayload().toString().contains("dialectDiagnostics"));
    }

    @Test
    void lintAppliesRuleExemptionAndExcludesSuppressedIssueFromCounts() {
        RecordingCheckRecordService recordService = new RecordingCheckRecordService();
        RuleExemption exemption = new RuleExemption();
        exemption.setId(11L);
        exemption.setProjectId(1L);
        exemption.setRuleCode("table_naming_snake_case");
        exemption.setTableName("UserOrder");
        exemption.setReason("历史三方表名兼容");
        exemption.setEnabled(true);
        SqlLintService service = new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                List.of(new TableNameSnakeCaseRule()),
                new ObjectMapper(),
                new FixedSqlGenerator(),
                recordService,
                new NoopAiJobRecordService(),
                new StaticRuleExemptionService(List.of(exemption))
        );

        LintResult result = service.lint("""
                CREATE TABLE UserOrder (
                    id bigserial PRIMARY KEY
                );
                """, 1L);

        LintIssue issue = result.getIssues().stream()
                .filter(item -> "table_naming_snake_case".equals(item.getRuleCode()))
                .findFirst()
                .orElseThrow();
        assertTrue(issue.getSuppressed());
        assertEquals(11L, issue.getSuppressionId());
        assertEquals("历史三方表名兼容", issue.getSuppressionReason());
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getSuppressedCount());
        assertNull(result.getFixedSql(), "已豁免的问题不应继续驱动 fixedSql 重命名");
        assertTrue(recordService.saved.get(0).getIssuesJson().contains("\"suppressed\":true"));
    }

    private static class EmptyRuleConfigService implements RuleConfigService {
        @Override
        public List<RuleConfig> listByProject(Long projectId) {
            return List.of();
        }

        @Override
        public List<RuleConfig> listEnabledByProject(Long projectId) {
            return List.of();
        }

        @Override
        public RuleConfig getById(Long id) {
            return null;
        }

        @Override
        public RuleConfig create(RuleConfig ruleConfig) {
            return ruleConfig;
        }

        @Override
        public RuleConfig update(Long id, RuleConfig ruleConfig) {
            return ruleConfig;
        }

        @Override
        public void delete(Long id) {
        }

        @Override
        public void toggle(Long id, boolean enabled) {
        }
    }

    /** 记录 save 调用的 stub,便于断言落库行为 */
    private static class RecordingCheckRecordService implements SqlCheckRecordService {
        final List<SqlCheckRecord> saved = new ArrayList<>();

        @Override
        public SqlCheckRecord save(Long projectId, String originalSql, LintResult result) {
            SqlCheckRecord record = new SqlCheckRecord();
            record.setProjectId(projectId);
            record.setId(7L);
            record.setOriginalSql(originalSql);
            record.setFixedSql(result.getFixedSql());
            record.setErrorCount(result.getErrorCount());
            record.setWarningCount(result.getWarningCount());
            record.setSuggestionCount(result.getSuggestionCount());
            // 与产品实现一致,序列化 issues 以便断言
            try {
                record.setIssuesJson(result.getIssues() == null ? "[]" : new ObjectMapper().writeValueAsString(result.getIssues()));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            saved.add(record);
            return record;
        }

        @Override
        public IPage<SqlCheckRecord> listByProject(Long projectId, int current, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SqlCheckRecord getById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<LintIssue> parseIssues(SqlCheckRecord record) {
            return List.of();
        }

        @Override
        public SqlCheckReplay buildReplay(SqlCheckRecord record) {
            throw new UnsupportedOperationException();
        }
    }

    private static class NoopAiJobRecordService implements AiJobRecordService {
        @Override
        public AiJobRecord create(AiJobRecordCreateReq req) {
            return new AiJobRecord();
        }

        @Override
        public IPage<AiJobRecord> listByProject(Long projectId, String jobType, int current, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiJobRecordDetail getDetail(Long id) {
            throw new UnsupportedOperationException();
        }
    }

    private static class RecordingAiJobRecordService extends NoopAiJobRecordService {
        final List<AiJobRecordCreateReq> created = new ArrayList<>();

        @Override
        public AiJobRecord create(AiJobRecordCreateReq req) {
            created.add(req);
            return new AiJobRecord();
        }
    }

    private static class NoopRuleExemptionService implements RuleExemptionService {
        @Override
        public List<RuleExemption> listByProject(Long projectId) {
            return List.of();
        }

        @Override
        public List<RuleExemption> listActiveByProject(Long projectId) {
            return List.of();
        }

        @Override
        public RuleExemption getById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RuleExemption create(RuleExemption exemption) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void disable(Long id) {
        }

        @Override
        public void delete(Long id) {
        }

        @Override
        public void applySuppressions(Long projectId, List<LintIssue> issues) {
        }
    }

    private static class StaticRuleExemptionService extends NoopRuleExemptionService {
        private final List<RuleExemption> exemptions;

        private StaticRuleExemptionService(List<RuleExemption> exemptions) {
            this.exemptions = exemptions;
        }

        @Override
        public List<RuleExemption> listActiveByProject(Long projectId) {
            return exemptions;
        }

        @Override
        public void applySuppressions(Long projectId, List<LintIssue> issues) {
            for (LintIssue issue : issues) {
                exemptions.stream()
                        .filter(exemption -> exemption.getRuleCode().equals(issue.getRuleCode()))
                        .filter(exemption -> exemption.getTableName() == null
                                || exemption.getTableName().equals(issue.getTableName()))
                        .filter(exemption -> exemption.getColumnName() == null
                                || exemption.getColumnName().equals(issue.getColumnName()))
                        .findFirst()
                        .ifPresent(exemption -> {
                            issue.setSuppressed(true);
                            issue.setSuppressionId(exemption.getId());
                            issue.setSuppressionReason(exemption.getReason());
                        });
            }
        }
    }
}
