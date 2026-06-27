package com.dataspec.field;

import com.dataspec.common.exception.BizException;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.impl.FieldServiceImpl;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 标准字段服务测试
 */
class FieldServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void create_defaultsPersonalMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("mobile_no", 1L)).thenReturn(false);
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("mobile_no");
        field.setDataType("varchar(20)");

        Field created = service.create(field);

        assertTrue(created.getNullable());
        assertFalse(created.getSensitive());
        assertEquals("enabled", created.getStatus());
        verify(repository).insert(created);
    }

    @Test
    void create_rejectsInvalidStatus() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        Field field = new Field();
        field.setProjectId(1L);
        field.setName("mobile_no");
        field.setDataType("varchar(20)");
        field.setStatus("archived");

        assertThrows(BizException.class, () -> service.create(field));
        verify(repository, never()).insert(any());
    }

    @Test
    void update_copiesPersonalMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("mobile_no");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("mobile_no", 1L, 9L)).thenReturn(false);
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        Field incoming = new Field();
        incoming.setName("mobile_no");
        incoming.setDisplayName("手机号");
        incoming.setDataType("varchar(20)");
        incoming.setNullable(false);
        incoming.setAliases("phone,mobile");
        incoming.setCategory("contact");
        incoming.setCodeSetId(10L);
        incoming.setSensitive(true);
        incoming.setStatus("deprecated");
        incoming.setExampleValue("13800138000");

        Field updated = service.update(9L, incoming);

        assertEquals("phone,mobile", updated.getAliases());
        assertEquals("contact", updated.getCategory());
        assertEquals(10L, updated.getCodeSetId());
        assertTrue(updated.getSensitive());
        assertEquals("deprecated", updated.getStatus());
        assertEquals("13800138000", updated.getExampleValue());
        verify(repository).update(updated);
    }

    @Test
    void update_recordsBeforeAndAfterChangeLog() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("mobile_no");
        existing.setDataType("varchar(20)");
        existing.setAliases("phone");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("mobile_no", 1L, 9L)).thenReturn(false);
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getDataType() + "|" + field.getAliases();
        });
        FieldServiceImpl service = new FieldServiceImpl(repository, changeLogService);

        Field incoming = new Field();
        incoming.setName("mobile_no");
        incoming.setDataType("varchar(30)");
        incoming.setAliases("phone,mobile");
        incoming.setNullable(true);

        service.update(9L, incoming);

        verify(changeLogService).recordChange(
                1L,
                "field",
                9L,
                "update",
                "varchar(20)|phone",
                "varchar(30)|phone,mobile");
    }

    @Test
    void suggest_matchesAliasAndChineseDisplayName() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "enabled"),
                field("amount_cent", "金额（分）", "bigint", "支付金额，以分存储", "amount,pay_amount", "enabled")
        ));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户手机号", 5);

        assertFalse(suggestions.isEmpty());
        FieldSuggestion first = suggestions.getFirst();
        assertTrue(first.existing());
        assertEquals("mobile_no", first.recommendedName());
        assertEquals("mobile_no", first.field().getName());
        assertTrue(first.score() > 0);
        assertTrue(first.matchReason().contains("显示名")
                || first.matchReason().contains("注释")
                || first.matchReason().contains("语义词"));
    }

    @Test
    void suggestAiContract_exposesStableRecommendationFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "enabled");
        mobile.setSensitive(true);
        mobile.setCategory("contact");
        mobile.setCodeSetId(10L);
        mobile.setExampleValue("13800138000");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "用户手机号", 5).getFirst();

        JsonNode root = new ObjectMapper().valueToTree(suggestion);
        assertEquals("mobile_no", root.path("recommendedName").asText());
        assertTrue(root.path("existing").asBoolean());
        assertTrue(root.path("score").asInt() > 0);
        assertFalse(root.path("matchReason").asText().isBlank());
        JsonNode fieldNode = root.path("field");
        assertEquals("mobile_no", fieldNode.path("name").asText());
        assertEquals("手机号", fieldNode.path("displayName").asText());
        assertEquals("varchar(20)", fieldNode.path("dataType").asText());
        assertEquals("phone,mobile,tel", fieldNode.path("aliases").asText());
        assertTrue(fieldNode.path("sensitive").asBoolean());
        assertEquals("enabled", fieldNode.path("status").asText());
        assertEquals("contact", fieldNode.path("category").asText());
        assertEquals(10L, fieldNode.path("codeSetId").asLong());
        assertEquals("13800138000", fieldNode.path("exampleValue").asText());
    }

    @Test
    void suggest_matchesSemanticSynonymAndPinyinAbbreviation() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "联系电话", "varchar(20)", "用户联系号码", "", "enabled"),
                field("user_name", "用户姓名", "varchar(64)", "用户姓名", "", "enabled")
        ));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户sjh", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("语义词"));
    }

    @Test
    void suggest_penalizesGenericOnlyMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("user_profile", "用户资料", "jsonb", "用户扩展信息", "", "enabled"),
                field("mobile_no", "联系电话", "varchar(20)", "联系号码", "", "enabled")
        ));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户手机号", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().score() > suggestions.get(1).score());
    }

    @Test
    void suggest_marksSensitiveFieldInReason() {
        FieldRepository repository = mock(FieldRepository.class);
        Field idCard = field("id_card_no", "身份证号", "varchar(32)", "证件号码", "", "enabled");
        idCard.setSensitive(true);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(idCard));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "sfzh", 5);

        assertEquals("id_card_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("敏感"));
    }

    @Test
    void suggest_generatesCanonicalFallbackNamesForKnownSemanticGroups() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        assertEquals("user_id", service.suggest(1L, "用户编号 uid", 5).getFirst().recommendedName());
        assertEquals("mobile_no", service.suggest(1L, "客户手机", 5).getFirst().recommendedName());
        assertEquals("amount_cent", service.suggest(1L, "付款金额", 5).getFirst().recommendedName());
    }

    @Test
    void suggest_doesNotMatchShortEnglishKeywordInsideUnrelatedToken() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("updated_at", "更新时间", "timestamp", "更新时间", "", "enabled")
        ));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "status", 5).getFirst();

        assertFalse(suggestion.existing());
        assertEquals("status", suggestion.recommendedName());
    }

    @Test
    void suggest_filtersDisabledFields() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "disabled")
        ));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "手机号", 5);

        assertFalse(suggestions.isEmpty());
        assertFalse(suggestions.getFirst().existing());
        assertNull(suggestions.getFirst().field());
    }

    @Test
    void suggest_returnsFallbackSnakeCaseNameWhenNoExistingFieldMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "客户生日", 5);

        assertEquals(1, suggestions.size());
        FieldSuggestion fallback = suggestions.getFirst();
        assertFalse(fallback.existing());
        assertEquals("customer_birthday", fallback.recommendedName());
        assertEquals(0, fallback.score());
        assertNull(fallback.field());
    }

    @Test
    void suggest_rejectsDescriptionWithoutSearchableContent() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.suggest(1L, "!!!", 5));
        verify(repository, never()).findAllByProjectId(anyLong());
    }

    @Test
    void getById_rejectsUnauthorizedProject() {
        FieldRepository repository = mock(FieldRepository.class);
        Field field = new Field();
        field.setId(9L);
        field.setProjectId(1L);
        field.setName("mobile_no");
        when(repository.findById(9L)).thenReturn(Optional.of(field));
        DataSpecSecurityContext.set(new ApiTokenPrincipal("limited", "bob", false, Set.of(2L)));
        FieldServiceImpl service = new FieldServiceImpl(repository, mock(StandardChangeLogService.class));

        BizException ex = assertThrows(BizException.class, () -> service.getById(9L));

        assertEquals(403, ex.getCode());
    }

    private Field field(String name, String displayName, String dataType, String comment,
                        String aliases, String status) {
        Field field = new Field();
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
