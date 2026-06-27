package com.dataspec.aicontext;

import com.dataspec.aicontext.service.AiContextExportService;
import com.dataspec.aicontext.model.AiContextScopeOptions;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.FixedSqlGenerator;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        assertTrue(entries.get(".dataspec/rules.yaml").contains("naming:"));
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
        assertTrue(entries.get(".dataspec/README.md").contains(".dataspec/workflows.md"));
        assertTrue(entries.get(".dataspec/README.md").contains("dataspec lint"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/field-catalog.json"));
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
        assertTrue(manifest.path("files").toString().contains(".dataspec/workflows.md"));
        assertTrue(manifest.path("commands").path("lint").asText().contains("--project 1"));
        assertTrue(manifest.path("commands").path("workflowList").asText().contains("workflow list"));

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

    private AiContextExportService createService(AiJobRecordService aiJobRecordService) {
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID)).thenReturn(snapshotInfo("v2026.06.24", "hash123"));
        return createService(standardSnapshotService, aiJobRecordService);
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
                mock(RuleExemptionService.class));
    }

    private AiContextExportService createService(StandardSnapshotService standardSnapshotService,
                                                 AiJobRecordService aiJobRecordService,
                                                 List<Field> fields,
                                                 RuleExemptionService ruleExemptionService) {
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        SqlCheckRecordService sqlCheckRecordService = mock(SqlCheckRecordService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(ruleConfigService.listByProject(PROJECT_ID)).thenReturn(List.of());
        when(ruleConfigService.listEnabledByProject(PROJECT_ID)).thenReturn(List.of());
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
                ruleExemptionService
        );
        return new AiContextExportService(
                ruleConfigService,
                fieldService,
                enumDictService,
                standardSnapshotService,
                sqlLintService,
                objectMapper,
                aiJobRecordService,
                ruleExemptionService
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
        return field;
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
}
