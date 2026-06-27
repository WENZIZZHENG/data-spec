package com.dataspec.rulebaseline.service;

import com.dataspec.rulebaseline.model.RuleBaselineRule;
import com.dataspec.rulebaseline.model.RuleBaselineTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 内置规则基线库。
 *
 * <p>第一版直接用代码维护，便于测试和 OpenAPI 暴露；后续如需要用户自定义模板，
 * 可在不改变规则应用契约的前提下迁移到数据库。</p>
 */
public final class BuiltInRuleBaselines {

    public static final String PERSONAL_DEFAULT = "personal_default";
    public static final String STRICT = "strict";
    public static final String LEGACY_COMPATIBLE = "legacy_compatible";
    public static final int SCHEMA_VERSION = 1;

    private BuiltInRuleBaselines() {
    }

    public static List<RuleBaselineTemplate> list() {
        return List.of(personalDefault(), strict(), legacyCompatible());
    }

    public static Optional<RuleBaselineTemplate> find(String key) {
        return list().stream()
                .filter(template -> template.key().equals(key))
                .findFirst();
    }

    private static RuleBaselineTemplate personalDefault() {
        List<RuleBaselineRule> rules = List.of(
                rule("table_naming_snake_case", "表名 snake_case", "ERROR", "{}"),
                rule("field_naming_snake_case", "字段 snake_case", "ERROR", "{}"),
                rule("comment_missing", "表字段注释完整性", "ERROR", "{}"),
                rule("required_columns", "必含审计字段", "ERROR",
                        "{\"requiredColumns\":[\"id\",\"created_at\",\"updated_at\",\"is_deleted\"]}"),
                rule("forbidden_field_name", "禁用字段名", "ERROR",
                        "{\"forbiddenNames\":[\"uid\",\"create_time\",\"update_time\",\"del_flag\",\"is_del\",\"tmp\",\"test\",\"flag1\",\"type1\"]}"),
                rule("recommended_field_name", "推荐字段名", "SUGGESTION",
                        "{\"recommendations\":{\"uid\":\"user_id\",\"phone\":\"mobile_no\",\"phone_number\":\"mobile_no\",\"amount\":\"amount_cent\",\"create_time\":\"created_at\",\"update_time\":\"updated_at\",\"del_flag\":\"is_deleted\"}}"),
                rule("field_suffix_type", "字段后缀/前缀类型", "WARNING",
                        "{\"suffixTypes\":{\"_id\":[\"bigint\",\"integer\",\"bigserial\"],\"_at\":[\"timestamp\",\"timestamp with time zone\",\"datetime\"],\"_no\":[\"varchar\",\"char\",\"text\"],\"_count\":[\"integer\",\"bigint\"]},\"prefixTypes\":{\"is_\":[\"boolean\"]}}"),
                rule("amount_field_type", "金额字段规范", "WARNING", "{}")
        );
        return template(PERSONAL_DEFAULT, "个人默认规则基线", "1.0.0",
                "适合个人/小团队新项目的默认命名、注释、审计字段和常见字段规则。", rules);
    }

    private static RuleBaselineTemplate strict() {
        List<RuleBaselineRule> rules = List.of(
                rule("table_naming_snake_case", "表名 snake_case", "ERROR", "{}"),
                rule("field_naming_snake_case", "字段 snake_case", "ERROR", "{}"),
                rule("comment_missing", "表字段注释完整性", "ERROR", "{}"),
                rule("required_columns", "严格必含审计字段", "ERROR",
                        "{\"requiredColumns\":[\"id\",\"created_at\",\"updated_at\",\"created_by\",\"updated_by\",\"is_deleted\"]}"),
                rule("forbidden_field_name", "严格禁用字段名", "ERROR",
                        "{\"forbiddenNames\":[\"uid\",\"userid\",\"create_time\",\"update_time\",\"del_flag\",\"is_del\",\"tmp\",\"test\",\"flag1\",\"type1\",\"status1\"]}"),
                rule("recommended_field_name", "严格推荐字段名", "ERROR",
                        "{\"recommendations\":{\"uid\":\"user_id\",\"userid\":\"user_id\",\"phone\":\"mobile_no\",\"phone_number\":\"mobile_no\",\"amount\":\"amount_cent\",\"price\":\"price_cent\",\"create_time\":\"created_at\",\"update_time\":\"updated_at\",\"del_flag\":\"is_deleted\"}}"),
                rule("field_suffix_type", "严格字段后缀/前缀类型", "ERROR",
                        "{\"suffixTypes\":{\"_id\":[\"bigint\",\"integer\",\"bigserial\"],\"_at\":[\"timestamp\",\"timestamp with time zone\",\"datetime\"],\"_no\":[\"varchar\",\"char\"],\"_count\":[\"integer\",\"bigint\"],\"_cent\":[\"bigint\",\"integer\"]},\"prefixTypes\":{\"is_\":[\"boolean\"],\"has_\":[\"boolean\"]}}"),
                rule("amount_field_type", "金额字段规范", "ERROR", "{}")
        );
        return template(STRICT, "严格规则基线", "1.0.0",
                "适合新建核心业务库，尽量把命名、注释、金额和审计字段问题前置为错误。", rules);
    }

    private static RuleBaselineTemplate legacyCompatible() {
        List<RuleBaselineRule> rules = List.of(
                rule("table_naming_snake_case", "表名 snake_case", "WARNING", "{}"),
                rule("field_naming_snake_case", "字段 snake_case", "WARNING", "{}"),
                rule("comment_missing", "表字段注释完整性", "SUGGESTION", "{}"),
                rule("required_columns", "兼容库必含审计字段", "WARNING",
                        "{\"requiredColumns\":[\"id\",\"created_at\",\"updated_at\"]}"),
                rule("forbidden_field_name", "兼容库禁用字段名", "WARNING",
                        "{\"forbiddenNames\":[\"tmp\",\"test\",\"flag1\",\"type1\"]}"),
                rule("recommended_field_name", "兼容库推荐字段名", "SUGGESTION",
                        "{\"recommendations\":{\"uid\":\"user_id\",\"create_time\":\"created_at\",\"update_time\":\"updated_at\",\"del_flag\":\"is_deleted\"}}"),
                rule("field_suffix_type", "兼容库字段后缀/前缀类型", "SUGGESTION",
                        "{\"suffixTypes\":{\"_id\":[\"bigint\",\"integer\",\"bigserial\",\"varchar\"],\"_at\":[\"timestamp\",\"timestamp with time zone\",\"datetime\",\"varchar\"],\"_no\":[\"varchar\",\"char\",\"text\"],\"_count\":[\"integer\",\"bigint\"]},\"prefixTypes\":{\"is_\":[\"boolean\",\"integer\",\"tinyint\"]}}"),
                rule("amount_field_type", "金额字段规范", "SUGGESTION", "{}")
        );
        return template(LEGACY_COMPATIBLE, "兼容历史库规则基线", "1.0.0",
                "适合先盘点和逐步治理历史库，降低命名与注释规则的阻断强度。", rules);
    }

    private static RuleBaselineTemplate template(
            String key,
            String name,
            String version,
            String description,
            List<RuleBaselineRule> rules
    ) {
        return new RuleBaselineTemplate(key, name, version, description, rules.size(), rules);
    }

    private static RuleBaselineRule rule(String code, String name, String severity, String paramsJson) {
        return new RuleBaselineRule(code, name, severity, true, paramsJson);
    }
}
