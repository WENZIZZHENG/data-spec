package com.dataspec.perf;

import com.dataspec.aicontext.service.AiContextExportService;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.businessglossary.model.BusinessGlossaryContextExport;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.capability.service.impl.AiCapabilityCatalogServiceImpl;
import com.dataspec.common.perf.PerformanceProbe;
import com.dataspec.contract.service.impl.SchemaRegistryServiceImpl;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.FieldService;
import com.dataspec.field.service.impl.FieldServiceImpl;
import com.dataspec.fieldconflict.service.impl.FieldConflictServiceImpl;
import com.dataspec.lint.engine.FixedSqlGenerator;
import com.dataspec.lint.engine.SqlLintService;
import com.dataspec.lint.engine.SqlParserService;
import com.dataspec.lint.model.ColumnDef;
import com.dataspec.lint.model.TableDef;
import com.dataspec.lint.rules.TableNameSnakeCaseRule;
import com.dataspec.lint.service.SqlCheckRecordService;
import com.dataspec.prompt.service.PromptTemplateRegistry;
import com.dataspec.reverseimport.model.ReverseImportCompareResult;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import com.dataspec.reverseimport.service.impl.ReverseImportServiceImpl;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.rulebaseline.model.RuleBaselineInfo;
import com.dataspec.rulebaseline.service.RuleBaselineService;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.service.StandardSnapshotService;
import com.dataspec.standardreuse.service.StandardReusePackService;
import com.dataspec.standardquery.service.impl.StandardQueryServiceImpl;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 合成大字段库性能基线。
 *
 * <p>这不是精确 benchmark，只用于在 `mvn test` 中持续给出可重复的本地耗时参考，并拦截明显退化。</p>
 */
class PerformanceBaselineTest {

    private static final Long PROJECT_ID = 1L;
    private static final int FIELD_COUNT = 5_000;
    private static final int TABLE_COUNT = 100;
    private static final int COLUMNS_PER_TABLE = 20;
    private static final long BROAD_REGRESSION_LIMIT_MS = 10_000;

    @Test
    void performanceProbeMarksSlowSamples() {
        assertFalse(PerformanceProbe.sample("fast", 10, 100, "hint").slow());
        assertFalse(PerformanceProbe.sample("edge", 100, 100, "hint").slow());
        assertTrue(PerformanceProbe.sample("slow", 101, 100, "hint").slow());
    }

    @Test
    void largeFieldLibraryBaseline_reportsRepeatableMetrics() {
        List<Field> fields = syntheticFields(FIELD_COUNT);
        FieldServiceImpl fieldService = fieldService(fields);
        AiContextExportService aiContextExportService = aiContextExportService(fieldService);
        ReverseImportServiceImpl reverseImportService = new ReverseImportServiceImpl(
                new SqlParserService(),
                fieldService,
                mock(ReverseImportSourceService.class));
        List<TableDef> tables = syntheticTables(fields, TABLE_COUNT, COLUMNS_PER_TABLE);

        FieldGroupSummary groupSummary = measure("field.groupSummary", FIELD_COUNT,
                () -> fieldService.groupSummary(PROJECT_ID));
        assertEquals(FIELD_COUNT, groupSummary.totalFieldCount());
        assertFalse(groupSummary.groups().isEmpty());

        List<FieldSuggestion> suggestions = measure("field.suggest", FIELD_COUNT,
                () -> fieldService.suggest(PROJECT_ID, "用户手机号", 5));
        assertFalse(suggestions.isEmpty());
        assertEquals("mobile_no", suggestions.getFirst().recommendedName());

        String fieldCatalogJson = measure("ai-context.fieldCatalog", FIELD_COUNT,
                () -> aiContextExportService.generateFieldCatalogJson(PROJECT_ID));
        assertTrue(fieldCatalogJson.contains("\"fields\""));
        assertTrue(fieldCatalogJson.contains("mobile_no"));

        ReverseImportCompareResult compareResult = measure("reverse-import.compareTables",
                TABLE_COUNT * COLUMNS_PER_TABLE,
                () -> reverseImportService.compareTables(PROJECT_ID, tables));
        assertEquals(TABLE_COUNT, compareResult.getSummary().getTableCount());
        assertEquals(TABLE_COUNT * COLUMNS_PER_TABLE, compareResult.getSummary().getColumnCount());
        assertTrue(compareResult.getSummary().getMatchedCount() > 0);
        assertTrue(compareResult.getSummary().getNewCount() > 0);
    }

    private <T> T measure(String operation, int itemCount, Supplier<T> supplier) {
        long startNanos = System.nanoTime();
        T result = supplier.get();
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        System.out.printf(
                "[dataspec-perf-baseline] operation=%s itemCount=%d durationMs=%d%n",
                operation,
                itemCount,
                durationMs);
        assertTrue(durationMs < BROAD_REGRESSION_LIMIT_MS,
                () -> operation + " 耗时 " + durationMs + "ms，超过宽松退化阈值");
        return result;
    }

    private FieldServiceImpl fieldService(List<Field> fields) {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(repository.findAllByProjectId(PROJECT_ID)).thenReturn(fields);
        when(glossaryService.match(PROJECT_ID, "用户手机号")).thenReturn(List.of());
        com.dataspec.fieldsemantic.service.FieldSemanticRuleService semanticRuleService =
                mock(com.dataspec.fieldsemantic.service.FieldSemanticRuleService.class);
        when(semanticRuleService.list(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class)))
                .thenReturn(List.of());
        com.dataspec.metric.service.MetricDefinitionService metricDefinitionService =
                mock(com.dataspec.metric.service.MetricDefinitionService.class);
        when(metricDefinitionService.list(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenReturn(List.of());
        return new FieldServiceImpl(
                repository,
                mock(FieldSourceRepository.class),
                mock(StandardChangeLogService.class),
                mock(com.dataspec.fieldhistory.service.FieldHistoricalAliasService.class),
                new ObjectMapper(),
                new com.dataspec.querynormalization.service.impl.QueryNormalizationServiceImpl(
                        new com.dataspec.querynormalization.tokenizer.NameLexicalTokenizer(),
                        glossaryService),
                semanticRuleService,
                metricDefinitionService);
    }

    private AiContextExportService aiContextExportService(FieldService fieldService) {
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        StandardSnapshotService standardSnapshotService = mock(StandardSnapshotService.class);
        SqlCheckRecordService sqlCheckRecordService = mock(SqlCheckRecordService.class);
        AiJobRecordService aiJobRecordService = mock(AiJobRecordService.class);
        RuleExemptionService ruleExemptionService = mock(RuleExemptionService.class);
        RuleBaselineService ruleBaselineService = mock(RuleBaselineService.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        StandardUsageExampleService usageExampleService = mock(StandardUsageExampleService.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        when(ruleConfigService.listByProject(PROJECT_ID)).thenReturn(List.of());
        when(ruleConfigService.listEnabledByProject(PROJECT_ID)).thenReturn(List.of());
        when(enumDictService.listByProject(PROJECT_ID)).thenReturn(List.of());
        when(standardSnapshotService.getCurrentSnapshot(PROJECT_ID))
                .thenReturn(StandardSnapshotInfo.unversioned(PROJECT_ID));
        when(ruleExemptionService.listActiveByProject(PROJECT_ID)).thenReturn(List.of());
        when(ruleBaselineService.currentBaseline(PROJECT_ID))
                .thenReturn(new RuleBaselineInfo(PROJECT_ID, "custom", "自定义规则", "unversioned", "inferred", null, 0));
        when(glossaryService.contextExport(PROJECT_ID, 200)).thenReturn(BusinessGlossaryContextExport.empty());
        when(usageExampleService.selectForAiContext(
                org.mockito.ArgumentMatchers.eq(PROJECT_ID),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        com.dataspec.fieldknowledge.service.FieldKnowledgeCardService fieldKnowledgeCardService =
                mock(com.dataspec.fieldknowledge.service.FieldKnowledgeCardService.class);
        when(fieldKnowledgeCardService.get(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> new com.dataspec.fieldknowledge.model.FieldKnowledgeCardResp(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        "field:" + invocation.getArgument(0) + ":" + invocation.getArgument(1),
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        null));
        com.dataspec.fieldsemantic.service.FieldSemanticRuleService fieldSemanticRuleService =
                mock(com.dataspec.fieldsemantic.service.FieldSemanticRuleService.class);
        when(fieldSemanticRuleService.list(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class)))
                .thenReturn(List.of());
        com.dataspec.metric.service.MetricDefinitionService metricDefinitionService =
                mock(com.dataspec.metric.service.MetricDefinitionService.class);
        when(metricDefinitionService.list(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenReturn(List.of());

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
                null,
                new SchemaRegistryServiceImpl(),
                new AiCapabilityCatalogServiceImpl(),
                glossaryService,
                new FieldConflictServiceImpl(fieldService),
                usageExampleService,
                mock(StandardReusePackService.class),
                new StandardQueryServiceImpl(fieldService),
                null,
                fieldKnowledgeCardService,
                fieldSemanticRuleService,
                metricDefinitionService
        );
    }

    private List<Field> syntheticFields(int count) {
        List<Field> fields = new ArrayList<>(count);
        fields.add(field(1, "mobile_no", "手机号", "contact", "pii,customer", "phone,mobile,tel"));
        for (int i = 2; i <= count; i++) {
            fields.add(field(
                    i,
                    "field_" + i,
                    "字段 " + i,
                    "domain_" + (i % 25),
                    "tag_" + (i % 20) + ",tag_" + (i % 7),
                    "legacy_" + i + ",alias_" + i));
        }
        return fields;
    }

    private Field field(int id, String name, String displayName, String category, String tags, String aliases) {
        Field field = new Field();
        field.setId((long) id);
        field.setProjectId(PROJECT_ID);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(id % 5 == 0 ? "bigint" : "varchar");
        field.setLength(id % 5 == 0 ? null : 64);
        field.setNullable(true);
        field.setComment(displayName + " 注释");
        field.setCategory(category);
        field.setTags(tags);
        field.setAliases(aliases);
        field.setSensitive(id == 1 || id % 97 == 0);
        field.setStatus(id % 89 == 0 ? "deprecated" : "enabled");
        field.setExampleValue("example_" + id);
        return field;
    }

    private List<TableDef> syntheticTables(List<Field> fields, int tableCount, int columnsPerTable) {
        List<TableDef> tables = new ArrayList<>(tableCount);
        for (int tableIndex = 0; tableIndex < tableCount; tableIndex++) {
            List<ColumnDef> columns = new ArrayList<>(columnsPerTable);
            for (int columnIndex = 0; columnIndex < columnsPerTable; columnIndex++) {
                int globalIndex = tableIndex * columnsPerTable + columnIndex;
                boolean standardColumn = columnIndex % 2 == 0;
                String columnName = standardColumn
                        ? fields.get((globalIndex % (fields.size() - 1)) + 1).getName()
                        : "raw_column_" + globalIndex;
                columns.add(ColumnDef.builder()
                        .name(columnName)
                        .dataType(columnIndex % 3 == 0 ? "bigint" : "varchar")
                        .nullable(true)
                        .comment("列 " + globalIndex)
                        .build());
            }
            tables.add(TableDef.builder()
                    .name("table_" + tableIndex)
                    .comment("表 " + tableIndex)
                    .columns(columns)
                    .build());
        }
        return tables;
    }
}
