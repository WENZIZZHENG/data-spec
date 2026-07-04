package com.dataspec.aicontext;

import com.dataspec.aicontext.service.AiContextExportService;
import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.common.result.PageResult;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.aiprofile.model.AiTaskContextScope;
import com.dataspec.aiprofile.model.AiTaskProfile;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.businessglossary.model.BusinessGlossaryContextExport;
import com.dataspec.businessglossary.model.BusinessGlossaryContextItem;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.capability.service.impl.AiCapabilityCatalogServiceImpl;
import com.dataspec.contract.service.impl.SchemaRegistryServiceImpl;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.fieldconflict.service.impl.FieldConflictServiceImpl;
import com.dataspec.lint.engine.FixedSqlGenerator;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.prompt.service.PromptTemplateEvaluationService;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.rulebaseline.model.RuleBaselineInfo;
import com.dataspec.rulebaseline.service.BuiltInRuleBaselines;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.dto.StandardSnapshotPayload;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.model.StandardUsageExampleSaveReq;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI Context 导出服务测试
 */
class AiContextExportServiceTest {

    private static final Long PROJECT_ID = 1L;

    @Test
    void generateAiContextPackage_containsAgentReadyContextFiles() throws Exception {
        AiContextExportService service = createService();

        byte[] zipBytes = service.generateAiContextPackage(PROJECT_ID);
        Map<String, String> entries = unzipTextEntries(zipBytes);

        assertTrue(entries.containsKey(".dataspec/DATABASE_RULES.md"));
        assertTrue(entries.containsKey(".dataspec/field-catalog.json"));
        assertTrue(entries.containsKey(".dataspec/field-catalog.schema.json"));
        assertTrue(entries.containsKey(".dataspec/schema-registry.json"));
        assertTrue(entries.containsKey(".dataspec/capabilities.json"));
        assertTrue(entries.containsKey(".dataspec/usage-examples.json"));
        assertTrue(entries.containsKey(".dataspec/manifest.json"));
        assertTrue(entries.containsKey(".dataspec/README.md"));
        assertTrue(entries.containsKey(".dataspec/rules.yaml"));
        assertTrue(entries.containsKey(".dataspec/prompts.md"));
        assertTrue(entries.containsKey(".dataspec/workflows.md"));
        assertTrue(entries.containsKey(".dataspec/examples/good.sql"));
        assertTrue(entries.containsKey(".dataspec/examples/bad.sql"));
        assertTrue(entries.containsKey("AGENTS.md.fragment"));

        assertTrue(entries.get(".dataspec/DATABASE_RULES.md").contains("table_naming_snake_case"));
        assertTrue(entries.get(".dataspec/field-catalog.json").contains("mobile_no"));
        assertTrue(entries.get(".dataspec/DATABASE_RULES.md").contains("值格式: type=mobile"));
        assertFalse(entries.get(".dataspec/DATABASE_RULES.md").contains("## 字段命名风险"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("naming:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("baseline:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("key: personal_default"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("spec_version: v2026.06.24"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("spec_hash: hash123"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("required_columns:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("suffix_types:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("prefix_types:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("forbidden_names:"));
        assertTrue(entries.get(".dataspec/prompts.md").contains("创建表"));
        assertTrue(entries.get(".dataspec/workflows.md").contains("create-table"));
        assertTrue(entries.get(".dataspec/workflows.md").contains("review-pr-sql"));
        assertTrue(entries.get(".dataspec/workflows.md").contains("reverse-import-standards"));
        assertTrue(entries.get(".dataspec/workflows.md").contains("export-min-context"));
        assertTrue(entries.get(".dataspec/README.md").contains(".dataspec/manifest.json"));
        assertTrue(entries.get(".dataspec/README.md").contains(".dataspec/schema-registry.json"));
        assertTrue(entries.get(".dataspec/README.md").contains(".dataspec/capabilities.json"));
        assertTrue(entries.get(".dataspec/README.md").contains(".dataspec/usage-examples.json"));
        assertTrue(entries.get(".dataspec/README.md").contains(".dataspec/workflows.md"));
        assertTrue(entries.get(".dataspec/README.md").contains("dataspec lint"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/field-catalog.json"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/schema-registry.json"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/capabilities.json"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/usage-examples.json"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/manifest.json"));
        assertTrue(entries.get("AGENTS.md.fragment").contains("dataspec lint <path|-> --project 1 --format json"));
        assertTrue(entries.get(".dataspec/examples/good.sql").contains("CREATE TABLE users"));
        assertTrue(entries.get(".dataspec/examples/bad.sql").contains("CREATE TABLE UserOrder"));

        var manifest = new ObjectMapper().readTree(entries.get(".dataspec/manifest.json"));
        assertEquals(1, manifest.path("schemaVersion").asInt());
        assertEquals(PROJECT_ID.longValue(), manifest.path("projectId").asLong());
        assertEquals("v2026.06.24", manifest.path("standard").path("specVersion").asText());
        assertEquals("hash123", manifest.path("standard").path("specHash").asText());
        assertFalse(manifest.path("generatedAt").asText().isBlank());
        assertTrue(manifest.path("files").isArray());
        assertTrue(manifest.path("files").toString().contains(".dataspec/schema-registry.json"));
        assertTrue(manifest.path("files").toString().contains(".dataspec/capabilities.json"));
        assertTrue(manifest.path("files").toString().contains(".dataspec/usage-examples.json"));
        assertTrue(manifest.path("files").toString().contains(".dataspec/workflows.md"));
        assertEquals(1, manifest.path("contracts").path("schemaVersion").asInt());
        assertEquals("2026.06.28", manifest.path("contracts").path("registryVersion").asText());
        assertEquals(".dataspec/schema-registry.json", manifest.path("contracts").path("file").asText());
        assertTrue(manifest.path("contracts").path("contractIds").toString().contains("field"));
        assertTrue(manifest.path("commands").path("contractList").asText().contains("contract list"));
        assertTrue(manifest.path("commands").path("capabilityList").asText().contains("capability list"));
        assertTrue(manifest.path("commands").path("lint").asText().contains("--project 1"));
        assertTrue(manifest.path("commands").path("workflowList").asText().contains("workflow list"));

        var registry = new ObjectMapper().readTree(entries.get(".dataspec/schema-registry.json"));
        assertEquals("dataspec-schema-registry", registry.path("kind").asText());
        assertEquals("2026.06.28", registry.path("registryVersion").asText());
        assertTrue(registry.path("contracts").toString().contains("lint-result"));
        assertTrue(registry.path("compatibilityPolicy").path("breakingChangePolicy").asText().contains("schemaVersion"));

        var capabilities = new ObjectMapper().readTree(entries.get(".dataspec/capabilities.json"));
        assertEquals("dataspec-ai-capability-catalog", capabilities.path("kind").asText());
        assertEquals(PROJECT_ID.longValue(), capabilities.path("projectId").asLong());
        assertTrue(capabilities.path("capabilities").toString().contains("lint-sql"));
        assertTrue(capabilities.path("capabilities").toString().contains("export-ai-context"));
        assertFalse(entries.get(".dataspec/capabilities.json").contains("Authorization"));
        assertFalse(entries.get(".dataspec/capabilities.json").contains("jdbc:postgresql://"));

        var catalog = new ObjectMapper().readTree(entries.get(".dataspec/field-catalog.json"));
        assertEquals("v2026.06.24", catalog.path("standard").path("specVersion").asText());
        assertEquals("hash123", catalog.path("standard").path("specHash").asText());
        var field = catalog.path("fields").get(0);
        assertEquals("phone", field.path("aliases").get(0).asText());
        assertEquals("mobile", field.path("aliases").get(1).asText());
        assertTrue(field.path("sensitive").asBoolean());
        assertEquals("enabled", field.path("status").asText());
        assertEquals("contact", field.path("category").asText());
        assertEquals(10L, field.path("codeSetId").asLong());
        assertEquals("13800138000", field.path("example").asText());
        assertEquals("mobile", field.path("format").path("type").asText());
        assertEquals("^1\\d{10}$", field.path("format").path("pattern").asText());
        assertEquals("string", field.path("format").path("unit").asText());
        assertEquals("not_blank", field.path("format").path("nullPolicy").asText());
        assertEquals("13800138000", field.path("format").path("validExamples").get(0).asText());
        assertEquals("12345", field.path("format").path("invalidExamples").get(0).asText());
        assertEquals("", field.path("format").path("invalidExamples").get(1).asText());
        assertTrue(entries.get(".dataspec/DATABASE_RULES.md").contains("invalidExamples=12345/\"\""));
        assertTrue(catalog.path("usageExamples").isArray());
        assertEquals(0, catalog.path("usageExampleSummary").path("totalExamples").asInt());

        var usageExamples = new ObjectMapper().readTree(entries.get(".dataspec/usage-examples.json"));
        assertEquals(PROJECT_ID.longValue(), usageExamples.path("projectId").asLong());
        assertEquals(1, usageExamples.path("schemaVersion").asInt());
        assertTrue(usageExamples.path("examples").isArray());
        assertEquals(0, usageExamples.path("summary").path("totalExamples").asInt());
        assertFalse(usageExamples.path("snapshotBound").asBoolean());

        var schema = new ObjectMapper().readTree(entries.get(".dataspec/field-catalog.schema.json"));
        assertTrue(schema.path("properties").has("projectId"));
        assertTrue(schema.path("properties").has("standard"));
        assertTrue(schema.path("properties").has("fields"));
        assertTrue(schema.path("properties").has("enums"));
        var fieldProperties = schema.path("properties").path("fields").path("items").path("properties");
        assertTrue(fieldProperties.has("aliases"));
        assertTrue(fieldProperties.has("sensitive"));
        assertTrue(fieldProperties.has("status"));
        assertTrue(fieldProperties.has("codeSetId"));
        assertTrue(fieldProperties.has("example"));
        assertTrue(fieldProperties.has("format"));
        assertTrue(fieldProperties.path("format").path("properties").has("validExamples"));
        assertTrue(fieldProperties.path("format").path("properties").has("invalidExamples"));
        assertTrue(fieldProperties.has("starterKitSources"));
    }

    @Test
    void generateAiContextPackage_exportsUsageExamplesAndAntiExamples() throws Exception {
        Field mobile = sampleField();
        mobile.setId(10L);
        StaticUsageExampleService usageExampleService = new StaticUsageExampleService(List.of(
                usageExample(1L, 10L, "FIELD", "GOOD", "使用 mobile_no 表达手机号", "mobile_no varchar(20) NOT NULL", null, "标准字段已包含手机号语义", "phone,ddl", 100),
                usageExample(2L, 10L, "FIELD", "BAD", "使用 phone_number 另造字段", null, "phone_number varchar(20)", "不要绕开 mobile_no", "phone,bad", 90),
                usageExample(3L, null, "RULE", "GOOD", "字段名必须 snake_case", "order_id", null, "命中命名规则", "rule", 80),
                usageExample(4L, null, "TEMPLATE", "BAD", "订单表模板误用", null, "缺少审计字段", "模板必须保留审计列", "template", 70)
        ));
        AiContextExportService service = createService(List.of(mobile), usageExampleService);

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID));

        var mapper = new ObjectMapper();
        var usage = mapper.readTree(entries.get(".dataspec/usage-examples.json"));
        assertEquals(PROJECT_ID.longValue(), usage.path("projectId").asLong());
        assertEquals("all", usage.path("contextScope").path("scope").asText());
        assertEquals(4, usage.path("summary").path("totalExamples").asInt());
        assertEquals(2, usage.path("summary").path("goodExamples").asInt());
        assertEquals(2, usage.path("summary").path("badExamples").asInt());
        assertEquals("GOOD", usage.path("examples").get(0).path("exampleType").asText());
        assertEquals("BAD", usage.path("examples").get(1).path("exampleType").asText());
        assertEquals("phone_number varchar(20)", usage.path("examples").get(1).path("antiPattern").asText());
        assertEquals("RULE", usage.path("examples").get(2).path("scope").asText());
        assertEquals("TEMPLATE", usage.path("examples").get(3).path("scope").asText());
        assertTrue(usageExampleService.lastFieldIds.isEmpty());
        assertFalse(entries.get(".dataspec/usage-examples.json").contains("password="));

        var catalog = mapper.readTree(entries.get(".dataspec/field-catalog.json"));
        assertEquals(4, catalog.path("usageExamples").size());
        assertEquals(4, catalog.path("usageExampleSummary").path("totalExamples").asInt());
        assertTrue(entries.get(".dataspec/README.md").contains("优先模仿 `GOOD`"));
        assertTrue(entries.get("AGENTS.md.fragment").contains("优先模仿 `GOOD`"));
    }

    @Test
    void generateAiContextPackage_scopedUsageExamplesUseMatchedFieldsAndQuery() throws Exception {
        Field mobile = sampleField();
        mobile.setId(10L);
        StaticUsageExampleService usageExampleService = new StaticUsageExampleService(List.of(
                usageExample(1L, 10L, "FIELD", "GOOD", "手机号字段", "mobile_no varchar(20)", null, "命中字段", "phone", 100)
        ));
        AiContextExportService service = createService(List.of(mobile), usageExampleService);

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(
                PROJECT_ID,
                new AiContextScopeOptions("field", "手机号", "enabled", 1)
        ));

        assertEquals(List.of(10L), usageExampleService.lastFieldIds);
        assertEquals("手机号", usageExampleService.lastQuery);
        var usage = new ObjectMapper().readTree(entries.get(".dataspec/usage-examples.json"));
        assertEquals("field", usage.path("contextScope").path("scope").asText());
        assertEquals("手机号", usage.path("contextScope").path("query").asText());
    }

    @Test
    void generateAiContextPackage_recordsUsageExampleTruncationWhenLimitReached() throws Exception {
        Field mobile = sampleField();
        mobile.setId(10L);
        List<StandardUsageExample> examples = new ArrayList<>();
        for (long i = 1; i <= 12; i++) {
            examples.add(usageExample(i, 10L, "FIELD", i % 2 == 0 ? "BAD" : "GOOD", "输入" + i, "输出" + i, "反例" + i, "原因" + i, "tag", 100 - (int) i));
        }
        AiContextExportService service = createService(List.of(mobile), new StaticUsageExampleService(examples));

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID));

        var usage = new ObjectMapper().readTree(entries.get(".dataspec/usage-examples.json"));
        assertEquals(8, usage.path("examples").size());
        assertTrue(usage.path("summary").path("truncated").asBoolean());
    }

    @Test
    void generateAiContextPackage_doesNotMarkUsageExamplesTruncatedAtExactLimit() throws Exception {
        Field mobile = sampleField();
        mobile.setId(10L);
        List<StandardUsageExample> examples = new ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            examples.add(usageExample(i, 10L, "FIELD", "GOOD", "输入" + i, "输出" + i, null, "原因" + i, "tag", 100 - (int) i));
        }
        AiContextExportService service = createService(List.of(mobile), new StaticUsageExampleService(examples));

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID));

        var usage = new ObjectMapper().readTree(entries.get(".dataspec/usage-examples.json"));
        assertEquals(8, usage.path("examples").size());
        assertFalse(usage.path("summary").path("truncated").asBoolean());
    }

    @Test
    void generateAiContextPackage_exportsFieldLifecycleReplacement() throws Exception {
        Field legacy = sampleField("old_mobile_no", "旧手机号", "contact", "pii", "phone,mobile");
        legacy.setId(1L);
        legacy.setStatus("deprecated");
        legacy.setReplacementFieldId(2L);
        legacy.setReplacementReason("历史兼容字段，改用 mobile_no");
        AiContextExportService service = createService(List.of(legacy));

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID));

        ObjectMapper mapper = new ObjectMapper();
        var catalog = mapper.readTree(entries.get(".dataspec/field-catalog.json"));
        var field = catalog.path("fields").get(0);
        assertEquals("deprecated", field.path("status").asText());
        assertEquals(2L, field.path("replacementFieldId").asLong());
        assertEquals("历史兼容字段，改用 mobile_no", field.path("replacementReason").asText());

        var schema = mapper.readTree(entries.get(".dataspec/field-catalog.schema.json"));
        var fieldProperties = schema.path("properties").path("fields").path("items").path("properties");
        assertTrue(fieldProperties.has("replacementFieldId"));
        assertTrue(fieldProperties.has("replacementReason"));
        assertTrue(fieldProperties.path("status").path("enum").toString().contains("draft"));
    }

    @Test
    void generateDatabaseRules_exportsFieldNamingRisks() {
        Field order = sampleField("order", "订单", "business", "core", null);
        Field customerType = sampleField("customer_type", "客户类型", "business", "core", "type");
        Field type = sampleField("type", "类型", "business", "core", null);
        AiContextExportService service = createService(List.of(order, customerType, type));

        String rules = service.generateDatabaseRules(PROJECT_ID);

        assertTrue(rules.contains("## 字段命名风险"));
        assertTrue(rules.contains("SQL 保留字风险: order"));
        assertTrue(rules.contains("建议替代名: order_value"));
        assertTrue(rules.contains("Alias 歧义: type"));
        assertTrue(rules.contains("不要直接使用该 alias"));
    }

    @Test
    void generateAiContextPackageAiContract_exposesStableContextFields() throws Exception {
        AiContextExportService service = createService();

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID));

        var mapper = new ObjectMapper();
        var manifest = mapper.readTree(entries.get(".dataspec/manifest.json"));
        assertEquals("dataspec-ai-context", manifest.path("kind").asText());
        assertEquals(1, manifest.path("schemaVersion").asInt());
        assertEquals(PROJECT_ID.longValue(), manifest.path("projectId").asLong());
        assertEquals("v2026.06.24", manifest.path("standard").path("specVersion").asText());
        assertEquals("hash123", manifest.path("standard").path("specHash").asText());
        assertTrue(manifest.path("files").toString().contains(".dataspec/field-catalog.json"));
        assertTrue(manifest.path("files").toString().contains(".dataspec/schema-registry.json"));
        assertTrue(manifest.path("files").toString().contains(".dataspec/capabilities.json"));
        assertTrue(manifest.path("files").toString().contains(".dataspec/workflows.md"));

        var catalog = mapper.readTree(entries.get(".dataspec/field-catalog.json"));
        assertEquals(PROJECT_ID.longValue(), catalog.path("projectId").asLong());
        assertTrue(catalog.path("fields").isArray());
        assertTrue(catalog.path("enums").isArray());
        var field = catalog.path("fields").get(0);
        assertEquals("mobile_no", field.path("name").asText());
        assertEquals("varchar(20)", field.path("dataType").asText());
        assertFalse(field.path("nullable").asBoolean());
        assertTrue(field.path("sensitive").asBoolean());
        assertEquals("enabled", field.path("status").asText());
        assertEquals("phone", field.path("aliases").get(0).asText());

        String rulesYaml = entries.get(".dataspec/rules.yaml");
        assertTrue(rulesYaml.contains("standard:"));
        assertTrue(rulesYaml.contains("baseline:"));
        assertTrue(rulesYaml.contains("naming:"));
        assertTrue(rulesYaml.contains("rules:"));

        String workflows = entries.get(".dataspec/workflows.md");
        assertTrue(workflows.contains("## create-table"));
        assertTrue(workflows.contains("## review-pr-sql"));
        assertTrue(workflows.contains("## reverse-import-standards"));
        assertTrue(workflows.contains("## export-min-context"));
    }

    @Test
    void generateAiContextPackage_usesOneSnapshotAcrossAllVersionedFiles() throws Exception {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(
                snapshotInfo("v1", "hash1"),
                snapshotInfo("v2", "hash2"),
                snapshotInfo("v3", "hash3"));
        AiContextExportService service = createService(standardSnapshotService);

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID));

        var mapper = new ObjectMapper();
        assertEquals("v1", mapper.readTree(entries.get(".dataspec/field-catalog.json")).path("standard").path("specVersion").asText());
        assertEquals("v1", mapper.readTree(entries.get(".dataspec/manifest.json")).path("standard").path("specVersion").asText());
        assertTrue(entries.get(".dataspec/rules.yaml").contains("spec_version: v1"));
    }

    @Test
    void generateAiContextFromSnapshot_usesSavedPayloadInsteadOfCurrentStandards() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        StandardSnapshotPayload payload = new StandardSnapshotPayload(
                new StandardSnapshotInfo(9L, PROJECT_ID, "v-history", "历史版本", null, "history-hash", null, true, "snapshot"),
                mapper.readTree("""
                        {
                          "projectId": 1,
                          "fields": [
                            {
                              "name": "legacy_user_id",
                              "displayName": "历史用户ID",
                              "dataType": "bigint",
                              "nullable": false,
                              "comment": "历史用户ID",
                              "aliases": "uid, user_id",
                              "status": "enabled"
                            }
                          ],
                          "enums": [
                            {
                              "code": "legacy_status",
                              "name": "历史状态",
                              "valueType": "string",
                              "values": [{"value": "Y", "label": "是"}]
                            }
                          ],
                          "rules": [
                            {
                              "ruleCode": "field_naming_snake_case",
                              "ruleName": "字段 snake_case",
                              "severity": "ERROR",
                              "enabled": true,
                              "paramsJson": "{}"
                            }
                          ]
                        }
                        """),
                1,
                1,
                1);
        when(standardSnapshotService.getSnapshotPayload(PROJECT_ID, 9L)).thenReturn(payload);
        AiContextExportService service = createService(
                standardSnapshotService,
                new NoopAiJobRecordService(),
                List.of(sampleField("current_mobile", "当前手机号", "contact", "pii", "phone")),
                mock(RuleExemptionService.class));

        String catalogJson = service.generateFieldCatalogJson(PROJECT_ID, AiContextScopeOptions.full(), 9L, null);
        String rulesYaml = service.generateRulesYaml(PROJECT_ID, 9L, null);
        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID, AiContextScopeOptions.full(), 9L, null));

        var catalog = mapper.readTree(catalogJson);
        assertEquals("snapshot", catalog.path("standard").path("source").asText());
        assertEquals("v-history", catalog.path("standard").path("specVersion").asText());
        assertEquals("legacy_user_id", catalog.path("fields").get(0).path("name").asText());
        assertEquals("user_id", catalog.path("fields").get(0).path("aliases").get(1).asText());
        assertEquals("legacy_status", catalog.path("enums").get(0).path("code").asText());
        assertFalse(catalogJson.contains("current_mobile"));
        assertTrue(rulesYaml.contains("source: snapshot"));
        assertTrue(rulesYaml.contains("field_naming_snake_case"));
        assertEquals("snapshot", mapper.readTree(entries.get(".dataspec/manifest.json")).path("standard").path("source").asText());
        assertEquals("legacy_user_id", mapper.readTree(entries.get(".dataspec/field-catalog.json")).path("fields").get(0).path("name").asText());
        assertTrue(entries.get(".dataspec/rules.yaml").contains("source: snapshot"));
    }

    @Test
    void generateRulesYamlAndDatabaseRules_includeRuleExemptions() {
        RuleExemptionService ruleExemptionService = mock(RuleExemptionService.class);
        RuleExemption exemption = new RuleExemption();
        exemption.setId(7L);
        exemption.setProjectId(PROJECT_ID);
        exemption.setRuleCode("table_naming_snake_case");
        exemption.setTableName("UserOrder");
        exemption.setReason("历史第三方表名兼容");
        exemption.setEnabled(true);
        when(ruleExemptionService.listActiveByProject(PROJECT_ID)).thenReturn(List.of(exemption));
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID))
                .thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        AiContextExportService service = createService(
                standardSnapshotService,
                new NoopAiJobRecordService(),
                List.of(sampleField()),
                ruleExemptionService);

        String rulesYaml = service.generateRulesYaml(PROJECT_ID);
        String databaseRules = service.generateDatabaseRules(PROJECT_ID);

        assertTrue(rulesYaml.contains("rule_exemptions:"));
        assertTrue(rulesYaml.contains("table_naming_snake_case"));
        assertTrue(rulesYaml.contains("历史第三方表名兼容"));
        assertTrue(rulesYaml.contains("not recommended standards"));
        assertTrue(databaseRules.contains("项目规则例外"));
        assertTrue(databaseRules.contains("不是新建表和新增字段的推荐标准"));
        assertTrue(databaseRules.contains("UserOrder"));
    }

    @Test
    void generateFieldCatalogJson_filtersFieldsAndAddsScopeMetadata() throws Exception {
        AiContextExportService service = createService(List.of(
                sampleField("mobile_no", "手机号", "contact", "pii,customer", "phone, mobile"),
                sampleField("order_amount", "订单金额", "money", "order", "amount")
        ));

        String content = service.generateFieldCatalogJson(
                PROJECT_ID,
                new AiContextScopeOptions("field", "手机", "enabled", 10)
        );

        var root = new ObjectMapper().readTree(content);
        assertEquals(1, root.path("fields").size());
        assertEquals("mobile_no", root.path("fields").get(0).path("name").asText());
        assertTrue(root.path("fields").get(0).path("matchReasons").isArray());
        assertEquals("field", root.path("contextScope").path("scope").asText());
        assertEquals("手机", root.path("contextScope").path("query").asText());
        assertEquals(2, root.path("contextScope").path("totalFieldCount").asInt());
        assertEquals(1, root.path("contextScope").path("matchedFieldCount").asInt());
        assertEquals(1, root.path("contextScope").path("returnedFieldCount").asInt());
    }

    @Test
    void generateFieldCatalogJson_exportsStarterKitSourcesFromTags() throws Exception {
        AiContextExportService service = createService(List.of(
                sampleField("user_id", "用户ID", "identifier", "user,starter:user_account@2026.06", "uid")
        ));

        String content = service.generateFieldCatalogJson(PROJECT_ID);

        var root = new ObjectMapper().readTree(content);
        var source = root.path("fields").get(0).path("starterKitSources").get(0);
        assertEquals("user_account", source.path("kitKey").asText());
        assertEquals("2026.06", source.path("kitVersion").asText());
    }

    @Test
    void generateFieldCatalogJson_exportsBusinessGlossary() throws Exception {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.contextExport(PROJECT_ID, 200)).thenReturn(new BusinessGlossaryContextExport(
                List.of(new BusinessGlossaryContextItem(
                        "会员",
                        List.of("用户", "账号"),
                        List.of("user", "member"),
                        List.of("hy"),
                        List.of("老用户"),
                        "user_id",
                        "GLOBAL",
                        null,
                        List.of("user_id")
                )),
                false,
                1,
                1
        ));
        AiContextExportService service = createService(List.of(
                sampleField("user_id", "用户ID", "user", "core", "uid")
        ), glossaryService);

        var root = new ObjectMapper().readTree(service.generateFieldCatalogJson(PROJECT_ID));

        var item = root.path("glossary").get(0);
        assertEquals("会员", item.path("term").asText());
        assertEquals("用户", item.path("synonyms").get(0).asText());
        assertEquals("user", item.path("rootTerms").get(0).asText());
        assertEquals("hy", item.path("abbreviations").get(0).asText());
        assertEquals("老用户", item.path("disabledTerms").get(0).asText());
        assertEquals("user_id", item.path("canonicalFieldName").asText());
    }

    @Test
    void generateFieldCatalogJson_warnsWhenBusinessGlossaryIsTruncated() throws Exception {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.contextExport(PROJECT_ID, 200)).thenReturn(new BusinessGlossaryContextExport(
                List.of(),
                true,
                201,
                200
        ));
        AiContextExportService service = createService(List.of(
                sampleField("user_id", "用户ID", "user", "core", "uid")
        ), glossaryService);

        var root = new ObjectMapper().readTree(service.generateFieldCatalogJson(PROJECT_ID));

        assertTrue(root.path("contextScope").path("warnings").get(0).asText().contains("业务术语表已按 200/201 条截断"));
    }

    @Test
    void generateFieldCatalogJson_includesGroupingSummaryForScopedCatalog() throws Exception {
        Field mobile = sampleField("mobile_no", "手机号", "contact", "pii,customer", "phone, mobile");
        mobile.setDomainId(10L);
        Field raw = sampleField("raw_payload", "原始报文", "", "", "");
        raw.setSensitive(false);
        AiContextExportService service = createService(List.of(mobile, raw));

        String content = service.generateFieldCatalogJson(
                PROJECT_ID,
                new AiContextScopeOptions("all", null, "enabled", null)
        );

        var root = new ObjectMapper().readTree(content);
        var groupSummary = root.path("contextScope").path("groupSummary");
        assertEquals(2, groupSummary.path("totalFieldCount").asInt());
        assertEquals(1, groupSummary.path("ungroupedFieldCount").asInt());
        assertTrue(groupSummary.path("groups").toString().contains("contact"));
        assertTrue(groupSummary.path("groups").toString().contains("ungrouped"));
        assertTrue(root.path("contextScope").path("warnings").toString().contains("未分组"));

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(
                PROJECT_ID,
                new AiContextScopeOptions("all", null, "enabled", null)
        ));
        var schema = new ObjectMapper().readTree(entries.get(".dataspec/field-catalog.schema.json"));
        assertTrue(schema.path("properties").path("contextScope").path("properties").has("groupSummary"));
    }

    @Test
    void generateFieldCatalogJson_fullCatalogKeepsScopeMetadataOptional() throws Exception {
        Field raw = sampleField("raw_payload", "原始报文", "", "", "");
        raw.setSensitive(false);
        AiContextExportService service = createService(List.of(raw));

        String content = service.generateFieldCatalogJson(PROJECT_ID, AiContextScopeOptions.full());

        var root = new ObjectMapper().readTree(content);
        assertFalse(root.has("contextScope"));
        assertEquals(1, root.path("fields").size());
    }

    @Test
    void generateAiContextPackage_scopedPackageContainsScopeSummaryAndTrimmedFields() throws Exception {
        AiContextExportService service = createService(List.of(
                sampleField("mobile_no", "手机号", "contact", "pii,customer", "phone, mobile"),
                sampleField("email", "邮箱", "contact", "pii", "mail"),
                sampleField("order", "订单", "business", "order", null),
                sampleField("order_amount", "订单金额", "money", "order", "amount")
        ));

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(
                PROJECT_ID,
                new AiContextScopeOptions("tag", "pii", "enabled", 1)
        ));

        var mapper = new ObjectMapper();
        var catalog = mapper.readTree(entries.get(".dataspec/field-catalog.json"));
        assertEquals(1, catalog.path("fields").size());
        assertEquals("tag", catalog.path("contextScope").path("scope").asText());
        assertEquals(2, catalog.path("contextScope").path("matchedFieldCount").asInt());
        assertEquals(1, catalog.path("contextScope").path("returnedFieldCount").asInt());
        assertTrue(catalog.path("contextScope").path("warnings").get(0).asText().contains("截断"));

        var manifest = mapper.readTree(entries.get(".dataspec/manifest.json"));
        assertEquals("tag", manifest.path("contextScope").path("scope").asText());
        assertTrue(entries.get(".dataspec/README.md").contains("按需包"));

        String databaseRules = entries.get(".dataspec/DATABASE_RULES.md");
        assertFalse(databaseRules.contains("SQL 保留字风险: order"));
        assertFalse(databaseRules.contains("## 字段命名风险"));
    }

    @Test
    void generateAiContextPackage_profileSuppliesScopeDefaultsAndManifestMetadata() throws Exception {
        AiTaskProfileService profileService = mock(AiTaskProfileService.class);
        when(profileService.findProfile("minimal-context")).thenReturn(Optional.of(AiTaskProfile.builder()
                .profileId("minimal-context")
                .taskType("MINIMAL_CONTEXT")
                .contextScope(AiTaskContextScope.builder()
                        .scope("field")
                        .status("enabled")
                        .limit(1)
                        .build())
                .build()));
        AiContextExportService service = createService(profileService);
        AiContextScopeOptions options = new AiContextScopeOptions(null, null, null, null, "minimal-context", null);

        String catalogJson = service.generateFieldCatalogJson(PROJECT_ID, options);
        var catalog = new ObjectMapper().readTree(catalogJson);
        assertEquals("field", catalog.path("contextScope").path("scope").asText());
        assertEquals("minimal-context", catalog.path("contextScope").path("profileId").asText());
        assertEquals("MINIMAL_CONTEXT", catalog.path("contextScope").path("taskType").asText());
        assertEquals("enabled", catalog.path("contextScope").path("status").asText());
        assertEquals(1, catalog.path("contextScope").path("limit").asInt());
        assertEquals(1, catalog.path("contextScope").path("returnedFieldCount").asInt());

        Map<String, String> entries = unzipTextEntries(service.generateAiContextPackage(PROJECT_ID, options));
        var manifest = new ObjectMapper().readTree(entries.get(".dataspec/manifest.json"));
        assertEquals("minimal-context", manifest.path("contextScope").path("profileId").asText());
        assertEquals("MINIMAL_CONTEXT", manifest.path("contextScope").path("taskType").asText());
        assertTrue(manifest.path("commands").path("exportContext").asText().contains("--profile minimal-context"));
    }

    @Test
    void generateCreateTablePrompt_containsBusinessContextAndStandards() {
        AiContextExportService service = createService();

        String prompt = service.generateCreateTablePrompt(PROJECT_ID, "订单模块");

        assertTrue(prompt.contains("订单模块"));
        assertTrue(prompt.contains("field-catalog.json"));
        assertTrue(prompt.contains("mobile_no"));
        assertTrue(prompt.contains("naming:"));
        assertTrue(prompt.contains("CREATE TABLE"));
        assertTrue(prompt.contains("promptVersion: create-table-prompt@1"));
        assertTrue(new PromptTemplateEvaluationService(new PromptTemplateRegistry())
                .evaluate(PromptTemplateRegistry.CREATE_TABLE, prompt)
                .passed());
    }

    @Test
    void generateCreateTablePrompt_recordsAiReplayJob() {
        RecordingAiJobRecordService aiJobRecordService = new RecordingAiJobRecordService();
        AiContextExportService service = createService(aiJobRecordService);

        service.generateCreateTablePrompt(PROJECT_ID, "订单模块");

        assertEquals(1, aiJobRecordService.created.size());
        AiJobRecordCreateReq req = aiJobRecordService.created.get(0);
        assertEquals(PROJECT_ID, req.projectId());
        assertEquals("CREATE_TABLE_PROMPT", req.jobType());
        assertEquals("create-table-prompt@1", req.promptVersion());
        assertEquals(6L, req.standardSnapshotId());
        assertEquals("v2026.06.24", req.standardSnapshotVersion());
        assertEquals("hash123", req.standardSnapshotHash());
        assertTrue(req.inputPayload().toString().contains("订单模块"));
        assertTrue(req.outputPayload().toString().contains("DataSpec 建表 Prompt"));
    }

    @Test
    void generateFixSqlPrompt_containsLintIssuesAndOriginalSql() {
        AiContextExportService service = createService();
        String sql = "CREATE TABLE UserOrder (user_id bigint);";

        String prompt = service.generateFixSqlPrompt(PROJECT_ID, sql);

        assertTrue(prompt.contains(sql));
        assertTrue(prompt.contains("table_naming_snake_case"));
        assertTrue(prompt.contains("UserOrder"));
        assertTrue(prompt.contains("修正后的 SQL"));
        assertTrue(prompt.contains("promptVersion: fix-sql-prompt@1"));
        assertTrue(new PromptTemplateEvaluationService(new PromptTemplateRegistry())
                .evaluate(PromptTemplateRegistry.FIX_SQL, prompt)
                .passed());
    }

    @Test
    void generateFixSqlPrompt_recordsAiReplayJob() {
        RecordingAiJobRecordService aiJobRecordService = new RecordingAiJobRecordService();
        AiContextExportService service = createService(aiJobRecordService);
        String sql = "CREATE TABLE UserOrder (user_id bigint);";

        service.generateFixSqlPrompt(PROJECT_ID, sql);

        assertTrue(aiJobRecordService.created.size() >= 2);
        AiJobRecordCreateReq req = aiJobRecordService.created.stream()
                .filter(item -> "FIX_SQL_PROMPT".equals(item.jobType()))
                .findFirst()
                .orElseThrow();
        assertEquals("FIX_SQL_PROMPT", req.jobType());
        assertEquals("fix-sql-prompt@1", req.promptVersion());
        assertTrue(req.inputPayload().toString().contains("UserOrder"));
        assertTrue(req.outputPayload().toString().contains("DataSpec SQL 修正 Prompt"));
    }

    private AiContextExportService createService() {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        return createService(standardSnapshotService, new NoopAiJobRecordService());
    }

    private AiContextExportService createService(List<Field> fields) {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        return createService(standardSnapshotService, new NoopAiJobRecordService(), fields);
    }

    private AiContextExportService createService(List<Field> fields, StandardUsageExampleService usageExampleService) {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        when(glossaryService.contextExport(PROJECT_ID, 200)).thenReturn(BusinessGlossaryContextExport.empty());
        return createService(
                standardSnapshotService,
                new NoopAiJobRecordService(),
                fields,
                mock(RuleExemptionService.class),
                null,
                glossaryService,
                usageExampleService);
    }

    private AiContextExportService createService(List<Field> fields, BusinessGlossaryService glossaryService) {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        return createService(
                standardSnapshotService,
                new NoopAiJobRecordService(),
                fields,
                mock(RuleExemptionService.class),
                null,
                glossaryService);
    }

    private AiContextExportService createService(AiJobRecordService aiJobRecordService) {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        return createService(standardSnapshotService, aiJobRecordService);
    }

    private AiContextExportService createService(AiTaskProfileService aiTaskProfileService) {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        return createService(
                standardSnapshotService,
                new NoopAiJobRecordService(),
                List.of(sampleField()),
                mock(RuleExemptionService.class),
                aiTaskProfileService);
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService) {
        return createService(standardSnapshotService, new NoopAiJobRecordService());
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService,
                                                 AiJobRecordService aiJobRecordService) {
        return createService(standardSnapshotService, aiJobRecordService, List.of(sampleField()));
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService,
                                                 AiJobRecordService aiJobRecordService,
                                                 List<Field> fields) {
        return createService(
                standardSnapshotService,
                aiJobRecordService,
                fields,
                mock(RuleExemptionService.class),
                null);
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService,
                                                 AiJobRecordService aiJobRecordService,
                                                 List<Field> fields,
                                                 RuleExemptionService ruleExemptionService) {
        return createService(standardSnapshotService, aiJobRecordService, fields, ruleExemptionService, null);
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService,
                                                 AiJobRecordService aiJobRecordService,
                                                 List<Field> fields,
                                                 RuleExemptionService ruleExemptionService,
                                                 AiTaskProfileService aiTaskProfileService) {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.contextExport(PROJECT_ID, 200)).thenReturn(BusinessGlossaryContextExport.empty());
        return createService(
                standardSnapshotService,
                aiJobRecordService,
                fields,
                ruleExemptionService,
                aiTaskProfileService,
                glossaryService,
                new NoopStandardUsageExampleService());
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService,
                                                 AiJobRecordService aiJobRecordService,
                                                 List<Field> fields,
                                                 RuleExemptionService ruleExemptionService,
                                                 AiTaskProfileService aiTaskProfileService,
                                                 BusinessGlossaryService glossaryService) {
        return createService(
                standardSnapshotService,
                aiJobRecordService,
                fields,
                ruleExemptionService,
                aiTaskProfileService,
                glossaryService,
                new NoopStandardUsageExampleService());
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService,
                                                 AiJobRecordService aiJobRecordService,
                                                 List<Field> fields,
                                                 RuleExemptionService ruleExemptionService,
                                                 AiTaskProfileService aiTaskProfileService,
                                                 BusinessGlossaryService glossaryService,
                                                 StandardUsageExampleService usageExampleService) {
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        RuleBaselineService ruleBaselineService = mock(RuleBaselineService.class);
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        SqlCheckRecordService sqlCheckRecordService = mock(SqlCheckRecordService.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        when(ruleConfigService.listByProject(PROJECT_ID)).thenReturn(List.of());
        when(ruleConfigService.listEnabledByProject(PROJECT_ID)).thenReturn(List.of());
        when(ruleBaselineService.currentBaseline(PROJECT_ID)).thenReturn(new RuleBaselineInfo(
                PROJECT_ID,
                BuiltInRuleBaselines.PERSONAL_DEFAULT,
                "个人默认规则基线",
                "1.0.0",
                "built_in",
                null,
                0));
        when(fieldService.listByProject(PROJECT_ID)).thenReturn(fields);
        when(enumDictService.listByProject(PROJECT_ID)).thenReturn(List.of());

        SqlLintService sqlLintService = new SqlLintService(
                new SqlParserService(),
                ruleConfigService,
                List.of(new TableNameSnakeCaseRule()),
                objectMapper,
                new FixedSqlGenerator(),
                sqlCheckRecordService,
                aiJobRecordService,
                ruleExemptionService,
                new PromptTemplateRegistry(),
                null
        );
        return new AiContextExportService(
                ruleConfigService,
                fieldService,
                enumDictService,
                standardSnapshotService,
                sqlLintService,
                objectMapper,
                aiJobRecordService,
                ruleExemptionService,
                ruleBaselineService,
                new PromptTemplateRegistry(),
                aiTaskProfileService,
                new SchemaRegistryServiceImpl(),
                new AiCapabilityCatalogServiceImpl(),
                glossaryService,
                new FieldConflictServiceImpl(fieldService),
                usageExampleService
        );
    }

    private StandardSnapshotInfo snapshotInfo(String version, String hash) {
        return new StandardSnapshotInfo(
                6L,
                PROJECT_ID,
                version,
                "P6-1",
                null,
                hash,
                null,
                true);
    }

    private Field sampleField() {
        return sampleField("mobile_no", "手机号", "contact", "pii,customer", "phone, mobile, tel, user_phone");
    }

    private Field sampleField(String name, String displayName, String category, String tags, String aliases) {
        Field field = new Field();
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType("varchar(20)");
        field.setNullable(false);
        field.setComment(displayName);
        field.setDefaultValue("");
        field.setAliases(aliases);
        field.setCategory(category);
        field.setTags(tags);
        field.setSensitive(true);
        field.setStatus("enabled");
        field.setCodeSetId(10L);
        field.setExampleValue("13800138000");
        field.setFormatType("mobile");
        field.setFormatPattern("^1\\d{10}$");
        field.setFormatUnit("string");
        field.setFormatPrecision("11 digits");
        field.setFormatNullPolicy("not_blank");
        field.setValidExamplesJson("[\"13800138000\"]");
        field.setInvalidExamplesJson("[\"12345\",\"\"]");
        field.setFormatNotes("中国大陆手机号");
        return field;
    }

    private StandardUsageExample usageExample(Long id,
                                              Long fieldId,
                                              String scope,
                                              String exampleType,
                                              String input,
                                              String expectedOutput,
                                              String antiPattern,
                                              String reason,
                                              String tags,
                                              Integer priority) {
        StandardUsageExample example = new StandardUsageExample();
        example.setId(id);
        example.setProjectId(PROJECT_ID);
        example.setFieldId(fieldId);
        example.setScope(scope);
        example.setExampleType(exampleType);
        example.setInput(input);
        example.setExpectedOutput(expectedOutput);
        example.setAntiPattern(antiPattern);
        example.setReason(reason);
        example.setTags(tags);
        example.setPriority(priority);
        example.setStatus("enabled");
        return example;
    }

    private Map<String, String> unzipTextEntries(byte[] zipBytes) throws Exception {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
                zip.closeEntry();
                entry = zip.getNextEntry();
            }
        }
        return entries;
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
            AiJobRecord record = new AiJobRecord();
            record.setId((long) created.size());
            return record;
        }
    }

    private static class NoopStandardUsageExampleService extends StaticUsageExampleService {
        NoopStandardUsageExampleService() {
            super(List.of());
        }
    }

    private static class StaticUsageExampleService implements StandardUsageExampleService {
        private final List<StandardUsageExample> examples;
        private List<Long> lastFieldIds = List.of();
        private String lastQuery;

        StaticUsageExampleService(List<StandardUsageExample> examples) {
            this.examples = examples;
        }

        @Override
        public PageResult<StandardUsageExample> page(Long projectId,
                                                     String scope,
                                                     String exampleType,
                                                     String status,
                                                     String query,
                                                     int current,
                                                     int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StandardUsageExample create(StandardUsageExampleSaveReq req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StandardUsageExample update(Long id, StandardUsageExampleSaveReq req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(Long projectId, Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<StandardUsageExample> selectForAiContext(Long projectId, List<Long> fieldIds, String query, int limit) {
            this.lastFieldIds = fieldIds == null ? List.of() : List.copyOf(fieldIds);
            this.lastQuery = query;
            return examples.stream().limit(limit).toList();
        }
    }
}
