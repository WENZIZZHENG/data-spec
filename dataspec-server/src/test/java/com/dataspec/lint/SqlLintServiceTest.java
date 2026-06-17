package com.dataspec.lint;

import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQL 校验服务单元测试（不依赖 Spring 容器）
 */
class SqlLintServiceTest {

    @Test
    void lintWithProjectWithoutRuleConfig_fallsBackToBuiltInRules() {
        SqlLintService service = new SqlLintService(
                new SqlParserService(),
                new EmptyRuleConfigService(),
                List.of(new TableNameSnakeCaseRule()),
                new ObjectMapper()
        );

        LintResult result = service.lint("""
                CREATE TABLE UserOrder (
                    id bigserial PRIMARY KEY
                );
                """, 1L);

        assertTrue(result.getIssues().stream()
                        .anyMatch(issue -> "table_naming_snake_case".equals(issue.getRuleCode())),
                "新项目无规则配置时应回退执行内置规则");
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
}
