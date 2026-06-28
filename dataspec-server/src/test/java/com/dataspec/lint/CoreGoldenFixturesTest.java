package com.dataspec.lint;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.FixedSqlGenerator;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.LintRule;
import com.dataspec.lint.model.SqlCheckReplay;
import com.dataspec.lint.model.TableDef;
import com.dataspec.lint.rules.AmountFieldRule;
import com.dataspec.lint.rules.CommentMissingRule;
import com.dataspec.lint.rules.FieldNamingSnakeCaseRule;
import com.dataspec.lint.rules.FieldSuffixTypeRule;
import com.dataspec.lint.rules.ForbiddenFieldNameRule;
import com.dataspec.lint.rules.RecommendedFieldNameRule;
import com.dataspec.lint.rules.RequiredColumnsRule;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import com.dataspec.reverseimport.model.FieldCandidate;
import com.dataspec.reverseimport.model.ReverseImportPreview;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 核心 SQL fixture/golden 回归测试。
 */
class CoreGoldenFixturesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postgresqlGoodFixture_hasNoNamingRequiredOrCommentIssues() {
        LintResult result = lintService(commonRules()).lint(readResource("fixtures/sql/postgresql-good.sql"), null);

        assertThat(result.getTables()).hasSize(1);
        assertThat(result.getTables().getFirst().getName()).isEqualTo("user_order");
        assertThat(result.getDialectDiagnostics())
                .extracting("code")
                .contains("POSTGRESQL_DIALECT_INFERRED", "POSTGRESQL_COMMENT_ON_SUPPORTED");
        assertThat(result.getIssues())
                .extracting(LintIssue::getRuleCode)
                .doesNotContain(
                        "table_naming_snake_case",
                        "field_naming_snake_case",
                        "comment_missing",
                        "required_columns",
                        "field_suffix_type",
                        "amount_field_type");
    }

    @Test
    void mysqlBadFixture_reportsStableKeyIssues() {
        LintResult result = lintService(commonRules()).lint(readResource("fixtures/sql/mysql-bad.sql"), null);

        assertThat(result.getTables()).hasSize(1);
        assertThat(result.getDialectDiagnostics())
                .extracting("code")
                .contains(
                        "MYSQL_DIALECT_INFERRED",
                        "MYSQL_UNSIGNED_TYPE_PARTIAL",
                        "MYSQL_INDEX_TABLE_OPTION_PARTIAL",
                        "MYSQL_INLINE_COMMENT_PARTIAL",
                        "MYSQL_FIXED_SQL_REVIEW_REQUIRED");
        assertThat(result.getIssues())
                .extracting(LintIssue::getRuleCode)
                .contains(
                        "table_naming_snake_case",
                        "field_naming_snake_case",
                        "comment_missing",
                        "recommended_field_name",
                        "required_columns");
    }

    @Test
    void fixedSql_matchesGoldenFile() {
        LintResult result = lintService(List.of(
                new TableNameSnakeCaseRule(),
                new RecommendedFieldNameRule(),
                new RequiredColumnsRule()
        )).lint(readResource("fixtures/sql/fixed-sql-input.sql"), null);

        assertThat(normalizeSql(result.getFixedSql()))
                .isEqualTo(normalizeSql(readResource("fixtures/golden/fixed-sql-user-order.sql")));
    }

    @Test
    void reverseImportMetadataFixture_reportsStablePreviewSummary() throws Exception {
        List<FieldCandidate> candidates = objectMapper.readValue(
                readResource("fixtures/reverseimport/database-metadata.json"),
                new TypeReference<>() {});
        TableDef table = TableDef.builder()
                .name("user_order")
                .comment("用户订单表")
                .columns(candidates.stream().map(this::columnFromCandidate).toList())
                .build();
        FieldService fieldService = mock(FieldService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(
                standardField("id", null),
                standardField("mobile_no", "phone,mobile")
        ));
        ReverseImportServiceImpl service = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));

        ReverseImportPreview preview = service.previewTables(1L, List.of(table));

        assertThat(preview.getSummary().getTableCount()).isEqualTo(1);
        assertThat(preview.getSummary().getColumnCount()).isEqualTo(4);
        assertThat(preview.getSummary().getCandidateCount()).isEqualTo(2);
        assertThat(preview.getSummary().getMissingCommentCount()).isEqualTo(1);
        assertThat(preview.getSummary().getNonStandardFieldCount()).isEqualTo(2);
        assertThat(preview.getFieldCandidates())
                .extracting(FieldCandidate::getColumnName)
                .containsExactly("user_name", "legacy_code");
        assertThat(preview.getMissingComments())
                .extracting("columnName")
                .containsExactly("legacy_code");
        assertThat(preview.getNonStandardFields())
                .extracting("columnName")
                .containsExactly("user_name", "legacy_code");
    }

    private SqlLintService lintService(List<LintRule> rules) {
        return new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                rules,
                objectMapper,
                new FixedSqlGenerator(),
                new NoopCheckRecordService(),
                new NoopAiJobRecordService(),
                mock(RuleExemptionService.class),
                new PromptTemplateRegistry());
    }

    private List<LintRule> commonRules() {
        return List.of(
                new TableNameSnakeCaseRule(),
                new FieldNamingSnakeCaseRule(),
                new CommentMissingRule(),
                new RequiredColumnsRule(),
                new RecommendedFieldNameRule(),
                new ForbiddenFieldNameRule(),
                new AmountFieldRule(),
                new FieldSuffixTypeRule()
        );
    }

    private ColumnDef columnFromCandidate(FieldCandidate candidate) {
        return ColumnDef.builder()
                .name(candidate.getColumnName())
                .dataType(candidate.getDataType())
                .nullable(Boolean.TRUE.equals(candidate.getNullable()))
                .defaultValue(candidate.getDefaultValue())
                .comment(candidate.getComment())
                .build();
    }

    private Field standardField(String name, String aliases) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setAliases(aliases);
        return field;
    }

    private String readResource(String path) {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "测试资源不存在: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取测试资源失败: " + path, e);
        }
    }

    private String normalizeSql(String sql) {
        return sql == null ? null : sql.replace("\r\n", "\n").trim();
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

    private static class NoopCheckRecordService implements SqlCheckRecordService {
        @Override
        public SqlCheckRecord save(Long projectId, String originalSql, LintResult result) {
            return new SqlCheckRecord();
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
}
