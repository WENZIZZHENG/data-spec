package com.dataspec.aicontext;

import com.dataspec.aicontext.service.AiContextExportService;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.engine.FixedSqlGenerator;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.rule.service.RuleConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        assertTrue(entries.containsKey(".dataspec/examples/good.sql"));
        assertTrue(entries.containsKey(".dataspec/examples/bad.sql"));
        assertTrue(entries.containsKey("AGENTS.md.fragment"));

        assertTrue(entries.get(".dataspec/DATABASE_RULES.md").contains("table_naming_snake_case"));
        assertTrue(entries.get(".dataspec/field-catalog.json").contains("mobile_no"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("naming:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("required_columns:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("suffix_types:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("prefix_types:"));
        assertTrue(entries.get(".dataspec/rules.yaml").contains("forbidden_names:"));
        assertTrue(entries.get(".dataspec/prompts.md").contains("创建表"));
        assertTrue(entries.get(".dataspec/README.md").contains(".dataspec/manifest.json"));
        assertTrue(entries.get(".dataspec/README.md").contains("dataspec lint"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/field-catalog.json"));
        assertTrue(entries.get("AGENTS.md.fragment").contains(".dataspec/manifest.json"));
        assertTrue(entries.get("AGENTS.md.fragment").contains("dataspec lint <path|-> --project 1 --format json"));
        assertTrue(entries.get(".dataspec/examples/good.sql").contains("CREATE TABLE users"));
        assertTrue(entries.get(".dataspec/examples/bad.sql").contains("CREATE TABLE UserOrder"));

        var manifest = new ObjectMapper().readTree(entries.get(".dataspec/manifest.json"));
        assertEquals(1, manifest.path("schemaVersion").asInt());
        assertEquals(PROJECT_ID.longValue(), manifest.path("projectId").asLong());
        assertFalse(manifest.path("generatedAt").asText().isBlank());
        assertTrue(manifest.path("files").isArray());
        assertTrue(manifest.path("commands").path("lint").asText().contains("--project 1"));

        var catalog = new ObjectMapper().readTree(entries.get(".dataspec/field-catalog.json"));
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
    void generateFixSqlPrompt_containsLintIssuesAndOriginalSql() {
        AiContextExportService service = createService();
        String sql = "CREATE TABLE UserOrder (user_id bigint);";

        String prompt = service.generateFixSqlPrompt(PROJECT_ID, sql);

        assertTrue(prompt.contains(sql));
        assertTrue(prompt.contains("table_naming_snake_case"));
        assertTrue(prompt.contains("UserOrder"));
        assertTrue(prompt.contains("修正后的 SQL"));
    }

    private AiContextExportService createService() {
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        SqlCheckRecordService sqlCheckRecordService = mock(SqlCheckRecordService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(ruleConfigService.listByProject(PROJECT_ID)).thenReturn(List.of());
        when(ruleConfigService.listEnabledByProject(PROJECT_ID)).thenReturn(List.of());
        when(fieldService.listByProject(PROJECT_ID)).thenReturn(List.of(sampleField()));
        when(enumDictService.listByProject(PROJECT_ID)).thenReturn(List.of());

        SqlLintService sqlLintService = new SqlLintService(
                new SqlParserService(),
                ruleConfigService,
                List.of(new TableNameSnakeCaseRule()),
                objectMapper,
                new FixedSqlGenerator(),
                sqlCheckRecordService
        );
        return new AiContextExportService(
                ruleConfigService,
                fieldService,
                enumDictService,
                sqlLintService,
                objectMapper
        );
    }

    private Field sampleField() {
        Field field = new Field();
        field.setName("mobile_no");
        field.setDisplayName("手机号");
        field.setDataType("varchar(20)");
        field.setNullable(false);
        field.setComment("用户手机号");
        field.setDefaultValue("");
        field.setAliases("phone, mobile, tel, user_phone");
        field.setCategory("contact");
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
}
