package com.dataspec.standardquery;

import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.standardquery.exception.StandardQueryValidationException;
import com.dataspec.field.service.FieldService;
import com.dataspec.field.service.impl.FieldServiceImpl;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.standardquery.model.StandardQueryFilter;
import com.dataspec.standardquery.model.StandardQueryRequest;
import com.dataspec.standardquery.model.StandardQueryResult;
import com.dataspec.standardquery.service.StandardQueryService;
import com.dataspec.standardquery.service.impl.StandardQueryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Standard Query DSL 服务测试。
 */
class StandardQueryServiceImplTest {

    @Test
    void search_validFieldDslReturnsNormalizedSummaryAndFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field amount = field(10L, "amount_cent", "订单金额", "bigint", "支付金额", "amount", "enabled");
        amount.setCategory("money");
        amount.setTags("finance,order");
        amount.setSensitive(false);
        amount.setExampleValue("1999");
        amount.setUpdatedAt(LocalDateTime.parse("2026-07-01T10:00:00"));
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        StandardQueryService service = service(repository);

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                "订单金额",
                List.of(
                        new StandardQueryFilter("category", "eq", "money"),
                        new StandardQueryFilter("tag", "contains", "finance"),
                        new StandardQueryFilter("sensitive", "eq", false),
                        new StandardQueryFilter("hasExample", "eq", true),
                        new StandardQueryFilter("updatedSince", "gte", "2026-06-01T00:00:00")),
                null,
                5,
                true,
                false));

        assertEquals(1L, result.projectId());
        assertEquals("FIELD", result.normalizedQuery().target());
        assertEquals("订单金额", result.normalizedQuery().text());
        assertEquals(5, result.normalizedQuery().limit());
        assertEquals(5, result.appliedFilters().size());
        assertTrue(result.ignoredFilters().isEmpty());
        assertEquals(1, result.querySummary().resultCount());
        assertEquals(1, result.querySummary().returnedCount());
        assertFalse(result.querySummary().truncated());
        assertEquals("amount_cent", result.fields().getFirst().field().getName());
        assertEquals("field:1:10", result.fields().getFirst().stableRef());
    }

    @Test
    void fromLegacyFieldSearchMapsParametersToDslExplanation() {
        StandardQueryService service = service(mock(FieldRepository.class));

        StandardQueryResult result = service.searchFieldsFromLegacyParameters(
                1L, "手机号", "contact", "pii", "enabled", true, 77L, 10);

        assertEquals("FIELD", result.normalizedQuery().target());
        assertEquals("手机号", result.normalizedQuery().text());
        assertEquals(10, result.normalizedQuery().limit());
        assertTrue(result.appliedFilters().stream().anyMatch(filter ->
                "category".equals(filter.field()) && "contact".equals(filter.redactedValue())));
        assertTrue(result.appliedFilters().stream().anyMatch(filter ->
                "sourceBatchId".equals(filter.field()) && "77".equals(filter.redactedValue())));
    }

    @Test
    void search_nonStrictUnsupportedFilterIsIgnoredAndHinted() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field(10L, "mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled")));
        StandardQueryService service = service(repository);

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                "手机号",
                List.of(new StandardQueryFilter("owner", "eq", "platform-team")),
                null,
                10,
                true,
                false));

        assertEquals(1, result.ignoredFilters().size());
        assertEquals("owner", result.ignoredFilters().getFirst().field());
        assertTrue(result.nextQueryHints().stream().anyMatch(hint -> hint.contains("支持字段")));
    }

    @Test
    void search_strictUnsupportedFilterFailsBeforeExecution() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardQueryService service = service(repository);

        StandardQueryValidationException ex = assertThrows(StandardQueryValidationException.class, () -> service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                "手机号",
                List.of(new StandardQueryFilter("owner", "eq", "platform-team")),
                null,
                10,
                true,
                true)));

        assertTrue(ex.getMessage().contains("不支持"));
        assertEquals("STANDARD_QUERY_DSL_INVALID", ex.getValidationError().code());
        verify(repository, never()).findAllByProjectId(anyLong());
    }

    @Test
    void search_rejectsOversizedLimitAndFilterCount() {
        StandardQueryService service = service(mock(FieldRepository.class));
        StandardQueryRequest tooLargeLimit = new StandardQueryRequest(
                1L, "FIELD", "手机号", List.of(), null, 51, true, false);

        StandardQueryValidationException limitEx = assertThrows(StandardQueryValidationException.class, () -> service.search(tooLargeLimit));
        assertTrue(limitEx.getMessage().contains("limit"));
        assertTrue(limitEx.getValidationError().bounds().contains("limit"));

        List<StandardQueryFilter> tooManyFilters = java.util.stream.IntStream.range(0, 21)
                .mapToObj(index -> new StandardQueryFilter("tag", "contains", "tag" + index))
                .toList();
        StandardQueryValidationException filterEx = assertThrows(StandardQueryValidationException.class, () -> service.search(new StandardQueryRequest(
                1L, "FIELD", "手机号", tooManyFilters, null, 10, true, false)));
        assertTrue(filterEx.getMessage().contains("过滤条件"));
    }

    @Test
    void search_refFilterRequiresCurrentProjectStableRef() {
        FieldRepository repository = mock(FieldRepository.class);
        Field sameLocalId = field(10L, "amount_cent", "订单金额", "bigint", "支付金额", "amount", "enabled");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(sameLocalId));
        StandardQueryService service = service(repository);

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                null,
                List.of(new StandardQueryFilter("stableRef", "eq", "field:2:10")),
                null,
                10,
                true,
                false));

        assertTrue(result.fields().isEmpty());
        assertEquals(1, result.ignoredFilters().size());
        assertTrue(result.ignoredFilters().getFirst().reason().contains("projectId"));
    }

    @Test
    void search_canonicalRefMatchesFieldsThatPointToReplacement() {
        FieldRepository repository = mock(FieldRepository.class);
        Field replacement = field(12L, "mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        Field legacy = field(11L, "old_mobile_no", "旧手机号", "varchar(20)", "历史手机号", "legacy_phone", "deprecated");
        legacy.setReplacementFieldId(12L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(replacement, legacy));
        when(repository.findById(12L)).thenReturn(java.util.Optional.of(replacement));
        StandardQueryService service = service(repository);

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                null,
                List.of(new StandardQueryFilter("canonicalRef", "eq", "field:1:12")),
                null,
                10,
                true,
                false));

        assertTrue(result.fields().stream()
                .anyMatch(item -> "old_mobile_no".equals(item.field().getName())));
        assertTrue(result.fields().stream()
                .anyMatch(item -> "mobile_no".equals(item.field().getName())));
    }

    @Test
    void search_nonStrictUnsupportedTargetRecordsIgnoredDiagnostic() {
        StandardQueryService service = service(mock(FieldRepository.class));

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "ENUM",
                null,
                List.of(),
                null,
                10,
                true,
                false));

        assertEquals("FIELD", result.normalizedQuery().target());
        assertEquals("target", result.ignoredFilters().getFirst().field());
    }

    @Test
    void search_redactsSecretLikeTextInSummaryIgnoredFiltersAndErrors() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        StandardQueryService service = service(repository);

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                "token=raw-secret-123",
                List.of(new StandardQueryFilter("owner", "eq", "Authorization: Bearer raw.jwt")),
                null,
                10,
                true,
                false));

        assertEquals("token=[REDACTED]", result.querySummary().text());
        assertFalse(result.querySummary().text().contains("raw-secret-123"));
        assertFalse(result.ignoredFilters().getFirst().redactedValue().contains("raw.jwt"));

        StandardQueryValidationException strictEx = assertThrows(StandardQueryValidationException.class, () -> service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                "token=raw-secret-123",
                List.of(new StandardQueryFilter("owner", "eq", "Authorization: Bearer raw.jwt")),
                null,
                10,
                true,
                true)));
        assertFalse(strictEx.getMessage().contains("raw.jwt"));
    }

    @Test
    void search_exposesTheSameFieldTokenNormalizationWhenExplainIsEnabled() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field(20L, "http_status_2_code", "HTTP 状态码", "int", "HTTP 状态码", "", "enabled")));
        StandardQueryService service = service(repository);

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                "HTTPStatus2Code",
                List.of(),
                null,
                10,
                true,
                false));

        assertEquals(List.of("http", "status", "2", "code"), result.normalizedQuery().queryTokens().stream()
                .map(token -> token.normalizedToken())
                .toList());
        assertEquals("http_status_2_code", result.fields().getFirst().field().getName());
    }

    @Test
    void search_explainFalseStillUsesSharedTokenizerButOmitsTokenEvidence() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field(21L, "http_status_2_code", "HTTP 状态码", "int", "HTTP 状态码", "", "enabled")));
        StandardQueryService service = service(repository);

        StandardQueryResult result = service.search(new StandardQueryRequest(
                1L,
                "FIELD",
                "HTTPStatus2Code",
                List.of(),
                null,
                10,
                false,
                false));

        assertEquals("http_status_2_code", result.fields().getFirst().field().getName());
        assertTrue(result.normalizedQuery().queryTokens().isEmpty());
    }

    private StandardQueryService service(FieldRepository repository) {
        com.dataspec.fieldsemantic.service.FieldSemanticRuleService semanticRuleService =
                mock(com.dataspec.fieldsemantic.service.FieldSemanticRuleService.class);
        when(semanticRuleService.list(
                anyLong(),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class)))
                .thenReturn(List.of());
        com.dataspec.metric.service.MetricDefinitionService metricDefinitionService =
                mock(com.dataspec.metric.service.MetricDefinitionService.class);
        when(metricDefinitionService.list(
                anyLong(),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenReturn(List.of());
        FieldService fieldService = new FieldServiceImpl(
                repository,
                mock(com.dataspec.common.service.ProjectFieldNameReservationGuard.class),
                mock(FieldSourceRepository.class),
                mock(com.dataspec.changelog.service.StandardChangeLogService.class),
                mock(com.dataspec.fieldhistory.service.FieldHistoricalAliasService.class),
                new ObjectMapper(),
                new com.dataspec.querynormalization.service.impl.QueryNormalizationServiceImpl(
                        new com.dataspec.querynormalization.tokenizer.NameLexicalTokenizer(),
                        mock(com.dataspec.businessglossary.service.BusinessGlossaryService.class)),
                semanticRuleService,
                metricDefinitionService);
        return new StandardQueryServiceImpl(fieldService);
    }

    private Field field(Long id, String name, String displayName, String dataType, String comment,
                        String aliases, String status) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setComment(comment);
        field.setAliases(aliases);
        field.setStatus(status);
        return field;
    }
}
