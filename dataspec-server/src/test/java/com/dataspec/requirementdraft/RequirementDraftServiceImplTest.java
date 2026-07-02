package com.dataspec.requirementdraft;

import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSearchItem;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSearchSummary;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.requirementdraft.model.RequirementDraftReq;
import com.dataspec.requirementdraft.model.RequirementDraftResult;
import com.dataspec.requirementdraft.service.impl.RequirementDraftServiceImpl;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequirementDraftServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void draft_buildsMatchedFieldsMissingCandidatesAmbiguityTemplateAndPrompt() {
        FieldService fieldService = mock(FieldService.class);
        TemplateService templateService = mock(TemplateService.class);
        RequirementDraftServiceImpl service = new RequirementDraftServiceImpl(fieldService, templateService);

        Field userId = field(1L, "user_id", "用户ID", "bigint", "用户主键", "用户,会员", "user");
        Field amount = field(2L, "pay_amount", "支付金额", "numeric", "支付金额", "金额,费用", "money");
        Field totalAmount = field(3L, "order_amount", "订单金额", "numeric", "订单金额", "金额", "money");
        when(fieldService.suggest(eq(1L), any(String.class), anyInt())).thenReturn(List.of(
                new FieldSuggestion(userId, 94, "字段名/别名命中", "user_id", true),
                new FieldSuggestion(amount, 90, "术语表命中: 支付金额", "pay_amount", true),
                new FieldSuggestion(totalAmount, 72, "泛化词命中: 金额", "order_amount", true)
        ));
        when(fieldService.search(any(FieldSearchReq.class))).thenReturn(new FieldSearchResult(
                1L,
                "会员支付流水表，记录会员、支付金额、支付状态、第三方流水号",
                new FieldSearchSummary(3, 3, 3, false, Map.of("query", "会员支付流水表"), List.of()),
                List.of(
                        new FieldSearchItem(amount, 88, List.of("金额命中"), "支付场景可复用", List.of()),
                        new FieldSearchItem(totalAmount, 84, List.of("金额命中"), "订单场景可复用", List.of())
                ),
                List.of()
        ));

        Template paymentTemplate = template(10L, "支付流水表模板", "支付场景表模板", "pay_");
        Template memberTemplate = template(11L, "会员基础表模板", "会员资料", "member_");
        when(templateService.listByProject(1L)).thenReturn(List.of(memberTemplate, paymentTemplate));
        when(templateService.listFields(10L)).thenReturn(List.of(
                templateField("user_id"),
                templateField("pay_amount"),
                templateField("pay_status")
        ));
        when(templateService.listFields(11L)).thenReturn(List.of(templateField("user_id"), templateField("mobile_no")));

        RequirementDraftResult result = service.draft(new RequirementDraftReq(
                1L,
                "会员支付流水表，记录会员、支付金额、支付状态、第三方流水号",
                "pay_trade",
                "payment",
                10
        ));

        assertEquals(1L, result.projectId());
        assertEquals("pay_trade", result.targetTableName());
        assertTrue(result.matchedFields().stream().anyMatch(item -> item.field().getName().equals("user_id")));
        assertTrue(result.missingCandidates().stream().anyMatch(item ->
                "pay_status".equals(item.candidateName())
                        && item.inboxPayload().sourceType().equals("REQUIREMENT_DRAFT")));
        assertTrue(result.missingCandidates().stream().anyMatch(item -> "third_party_trade_no".equals(item.candidateName())));
        assertTrue(result.ambiguousTerms().stream().anyMatch(item ->
                "金额".equals(item.term()) && item.candidates().size() >= 2));
        assertNotNull(result.recommendedTemplate());
        assertEquals(10L, result.recommendedTemplate().id());
        assertTrue(result.copyablePrompt().contains("pay_trade"));
        assertTrue(result.copyablePrompt().contains("matchedFields"));
        assertTrue(result.nextActions().stream().anyMatch(action -> action.contains("DDL")));
        verify(fieldService, never()).create(any());

        JsonNode root = objectMapper.valueToTree(result);
        JsonNode matchedEvidence = root.path("matchedFields").path(0).path("evidence").path(0);
        assertEquals("FIELD", matchedEvidence.path("sourceType").asText());
        assertTrue(matchedEvidence.path("sourceId").asLong() > 0);
        assertFalse(matchedEvidence.path("matchReason").asText().isBlank());
        assertTrue(matchedEvidence.path("confidence").asInt() > 0);
        assertFalse(matchedEvidence.path("docsRef").asText().isBlank());

        JsonNode missingEvidence = root.path("missingCandidates").path(0).path("evidenceTrace").path(0);
        assertEquals("REQUIREMENT_DRAFT", missingEvidence.path("sourceType").asText());
        assertEquals("missing_candidate_pattern", missingEvidence.path("ruleCode").asText());
        assertFalse(missingEvidence.path("matchReason").asText().isBlank());
        assertTrue(missingEvidence.path("confidence").asInt() > 0);

        JsonNode templateEvidence = root.path("recommendedTemplate").path("evidence").path(0);
        assertEquals("TEMPLATE", templateEvidence.path("sourceType").asText());
        assertEquals(10L, templateEvidence.path("sourceId").asLong());
        assertTrue(templateEvidence.path("confidence").asInt() > 0);
    }

    @Test
    void draft_rejectsBlankDescriptionOrTableName() {
        RequirementDraftServiceImpl service = new RequirementDraftServiceImpl(mock(FieldService.class), mock(TemplateService.class));

        assertThrows(RuntimeException.class, () -> service.draft(new RequirementDraftReq(1L, " ", "pay_trade", null, null)));
        assertThrows(RuntimeException.class, () -> service.draft(new RequirementDraftReq(1L, "支付流水", " ", null, null)));
    }

    private Field field(Long id, String name, String displayName, String dataType, String comment,
                        String aliases, String category) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setComment(comment);
        field.setAliases(aliases);
        field.setCategory(category);
        field.setStatus("enabled");
        return field;
    }

    private Template template(Long id, String name, String description, String tablePrefix) {
        Template template = new Template();
        template.setId(id);
        template.setProjectId(1L);
        template.setName(name);
        template.setDescription(description);
        template.setTablePrefix(tablePrefix);
        return template;
    }

    private TemplateField templateField(String name) {
        TemplateField field = new TemplateField();
        field.setName(name);
        return field;
    }
}
