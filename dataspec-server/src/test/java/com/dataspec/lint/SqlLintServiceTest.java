package com.dataspec.lint;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.lint.engine.FixedSqlGenerator;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.rules.RecommendedFieldNameRule;
import com.dataspec.lint.rules.RequiredColumnsRule;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                recordService
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

    private SqlLintService newService(SqlCheckRecordService recordService) {
        return new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                List.of(new TableNameSnakeCaseRule()),
                new ObjectMapper(),
                new FixedSqlGenerator(),
                recordService
        );
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
            record.setOriginalSql(originalSql);
            record.setFixedSql(result.getFixedSql());
            record.setErrorCount(result.getErrorCount());
            record.setWarningCount(result.getWarningCount());
            record.setSuggestionCount(result.getSuggestionCount());
            // 与产品实现一致,序列化 issues 以便断言
            record.setIssuesJson(result.getIssues() == null ? "[]" : "[\"stub\"]");
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
    }
}
