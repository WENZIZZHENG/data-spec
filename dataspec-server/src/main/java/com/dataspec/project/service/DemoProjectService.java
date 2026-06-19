package com.dataspec.project.service;

import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.project.entity.Project;
import com.dataspec.project.model.DemoProjectResult;
import com.dataspec.project.repository.ProjectRepository;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.security.context.ProjectAccessGuard;
import com.dataspec.standards.BuiltInStandardsImportService;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 演示项目初始化服务。
 *
 * <p>该服务只面向首次使用体验，种子数据按项目内名称/编码幂等补齐，
 * 避免用户重复点击“创建演示项目”时污染项目列表或覆盖已有配置。</p>
 */
@Service
@RequiredArgsConstructor
public class DemoProjectService {

    public static final String DEMO_PROJECT_NAME = "DataSpec 演示项目";
    private static final String DEMO_TEMPLATE_NAME = "订单表模板";
    private static final String SAMPLE_TABLE_NAME = "user_order";
    private static final String BAD_EXAMPLE_SQL = """
            CREATE TABLE UserOrder (
                id bigserial PRIMARY KEY,
                uid bigint NOT NULL,
                phone varchar(20),
                amount decimal(10,2) DEFAULT 0,
                create_time timestamp,
                update_time timestamp,
                del_flag boolean DEFAULT false
            );
            """;
    private static final String GOOD_EXAMPLE_SQL = """
            CREATE TABLE user_order (
                id bigserial PRIMARY KEY,
                user_id bigint NOT NULL,
                order_no varchar(64) NOT NULL,
                mobile_no varchar(20),
                amount_cent bigint NOT NULL DEFAULT 0,
                status integer NOT NULL DEFAULT 0,
                remark text,
                created_at timestamp with time zone NOT NULL DEFAULT now(),
                updated_at timestamp with time zone NOT NULL DEFAULT now(),
                is_deleted boolean NOT NULL DEFAULT false
            );

            COMMENT ON TABLE user_order IS '用户订单表';
            COMMENT ON COLUMN user_order.user_id IS '关联用户表';
            COMMENT ON COLUMN user_order.order_no IS '订单唯一编号';
            COMMENT ON COLUMN user_order.mobile_no IS '用户手机号';
            COMMENT ON COLUMN user_order.amount_cent IS '金额，以分为单位存储，避免浮点精度问题';
            """;

    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final BuiltInStandardsImportService standardsImportService;
    private final FieldRepository fieldRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final TemplateRepository templateRepository;

    @Transactional
    public DemoProjectResult createOrReuseDemoProject() {
        ProjectAccessGuard.requireAllProjects("创建演示项目需要全项目 API token");
        Project existing = projectRepository.findByName(DEMO_PROJECT_NAME).orElse(null);
        boolean created = existing == null;
        Project project = created ? createDemoProject() : existing;

        if (!created) {
            standardsImportService.importBuiltInStandards(project.getId());
        }
        seedRules(project.getId());
        Template template = seedTemplate(project.getId());
        return new DemoProjectResult(
                project,
                template.getId(),
                SAMPLE_TABLE_NAME,
                BAD_EXAMPLE_SQL,
                GOOD_EXAMPLE_SQL,
                created);
    }

    private Project createDemoProject() {
        Project project = new Project();
        project.setName(DEMO_PROJECT_NAME);
        project.setDescription("用于体验字段标准、DDL 生成、SQL 校验和 AI Context 导出的演示项目");
        project.setDbType("postgresql");
        return projectService.create(project, true);
    }

    private void seedRules(Long projectId) {
        for (DemoRuleSeed seed : demoRules()) {
            if (ruleConfigRepository.findByCodeAndProjectId(seed.ruleCode(), projectId).isPresent()) {
                continue;
            }
            RuleConfig rule = new RuleConfig();
            rule.setProjectId(projectId);
            rule.setRuleCode(seed.ruleCode());
            rule.setRuleName(seed.ruleName());
            rule.setSeverity(seed.severity());
            rule.setEnabled(true);
            rule.setParamsJson(seed.paramsJson());
            ruleConfigRepository.insert(rule);
        }
    }

    private Template seedTemplate(Long projectId) {
        Template template = templateRepository.findByProjectId(projectId).stream()
                .filter(item -> DEMO_TEMPLATE_NAME.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> createDemoTemplate(projectId));
        seedTemplateFields(projectId, template.getId());
        return template;
    }

    private Template createDemoTemplate(Long projectId) {
        Template template = new Template();
        template.setProjectId(projectId);
        template.setName(DEMO_TEMPLATE_NAME);
        template.setDescription("覆盖订单号、用户、金额、状态和审计字段的演示模板");
        template.setTablePrefix("");
        templateRepository.insert(template);
        return template;
    }

    private void seedTemplateFields(Long projectId, Long templateId) {
        Map<String, Field> fieldsByName = fieldRepository.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(
                        field -> field.getName().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, TemplateField> existingByName = templateRepository.findFieldsByTemplateId(templateId).stream()
                .collect(Collectors.toMap(
                        field -> field.getName().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        int sortOrder = 10;
        for (String fieldName : List.of(
                "id", "user_id", "order_no", "mobile_no", "amount_cent",
                "status", "remark", "created_at", "updated_at", "is_deleted")) {
            if (existingByName.containsKey(fieldName)) {
                sortOrder += 10;
                continue;
            }
            Field field = fieldsByName.get(fieldName);
            if (field == null) {
                sortOrder += 10;
                continue;
            }
            TemplateField templateField = new TemplateField();
            templateField.setTemplateId(templateId);
            templateField.setFieldId(field.getId());
            templateField.setName(field.getName());
            templateField.setDataType(field.getDataType());
            templateField.setNullable(field.getNullable());
            templateField.setDefaultValue(field.getDefaultValue());
            templateField.setComment(field.getComment());
            templateField.setSortOrder(sortOrder);
            templateField.setIsRequired(!Boolean.TRUE.equals(field.getNullable()));
            templateRepository.insertField(templateField);
            sortOrder += 10;
        }
    }

    private List<DemoRuleSeed> demoRules() {
        return List.of(
                new DemoRuleSeed("table_naming_snake_case", "表名 snake_case", "error", "{}"),
                new DemoRuleSeed("field_naming_snake_case", "字段 snake_case", "error", "{}"),
                new DemoRuleSeed("comment_missing", "表字段注释完整性", "error", "{}"),
                new DemoRuleSeed("required_columns", "必含审计字段", "error",
                        "{\"requiredColumns\":[\"id\",\"created_at\",\"updated_at\",\"is_deleted\"]}"),
                new DemoRuleSeed("forbidden_field_name", "禁用字段名", "error",
                        "{\"forbiddenNames\":[\"uid\",\"create_time\",\"update_time\",\"del_flag\",\"is_del\",\"tmp\",\"test\",\"flag1\",\"type1\"]}"),
                new DemoRuleSeed("recommended_field_name", "推荐字段名", "suggestion",
                        "{\"recommendations\":{\"uid\":\"user_id\",\"phone\":\"mobile_no\",\"phone_number\":\"mobile_no\",\"amount\":\"amount_cent\",\"create_time\":\"created_at\",\"update_time\":\"updated_at\",\"del_flag\":\"is_deleted\"}}"),
                new DemoRuleSeed("field_suffix_type", "字段后缀/前缀类型", "warning",
                        "{\"suffixTypes\":{\"_id\":[\"bigint\",\"integer\",\"bigserial\"],\"_at\":[\"timestamp\",\"timestamp with time zone\",\"datetime\"],\"_no\":[\"varchar\",\"char\",\"text\"],\"_count\":[\"integer\",\"bigint\"]},\"prefixTypes\":{\"is_\":[\"boolean\"]}}"),
                new DemoRuleSeed("amount_field_type", "金额字段规范", "warning", "{}")
        );
    }

    private record DemoRuleSeed(String ruleCode, String ruleName, String severity, String paramsJson) {
    }
}
