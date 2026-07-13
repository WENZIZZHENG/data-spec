package com.dataspec.businessglossary;

import com.dataspec.businessglossary.entity.BusinessGlossary;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictReport;
import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.businessglossary.repository.BusinessGlossaryRepository;
import com.dataspec.businessglossary.service.impl.BusinessGlossaryServiceImpl;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.querynormalization.model.QueryTokenResolutionStatus;
import com.dataspec.querynormalization.tokenizer.NameLexicalTokenizer;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessGlossaryServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void create_normalizesDefaultsAndRejectsDuplicateActiveTerm() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        BusinessGlossaryServiceImpl service = service(repository, mock(FieldRepository.class));

        BusinessGlossary entry = glossary(null, 1L, " 会员 ", "用户, 账号", "user,member", "hy", null, 10L);
        BusinessGlossary created = service.create(entry);

        assertEquals("会员", created.getTerm());
        assertEquals("用户,账号", created.getSynonyms());
        assertEquals("enabled", created.getStatus());
        verify(repository).insert(created);

        when(repository.findAllByProjectId(1L)).thenReturn(List.of(glossary(2L, 1L, "会员", "", "", "", null, null)));
        BizException ex = assertThrows(BizException.class, () -> service.create(
                glossary(null, 1L, "会员", "", "", "", null, null)));
        assertTrue(ex.getMessage().contains("术语已存在"));
    }

    @Test
    void conflicts_reportsDuplicateTokenDisabledTermAndMissingCanonicalField() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(1L, 1L, "会员", "用户", "user", "hy", "老用户", 101L),
                glossary(2L, 1L, "客户", "用户", "customer", "kh", null, 102L),
                glossary(3L, 1L, "老用户", "legacy", "", "", null, 999L)
        ));
        when(fieldRepository.findById(101L)).thenReturn(Optional.of(field(101L, "user_id", "enabled")));
        when(fieldRepository.findById(102L)).thenReturn(Optional.of(field(102L, "customer_id", "enabled")));
        when(fieldRepository.findById(999L)).thenReturn(Optional.empty());
        BusinessGlossaryServiceImpl service = service(repository, fieldRepository);

        BusinessGlossaryConflictReport report = service.conflicts(1L);

        assertEquals(1L, report.projectId());
        assertTrue(report.conflicts().stream().anyMatch(conflict ->
                "DUPLICATE_TOKEN".equals(conflict.type()) && "用户".equals(conflict.token())));
        assertTrue(report.conflicts().stream().anyMatch(conflict ->
                "DISABLED_TERM_CONFLICT".equals(conflict.type()) && "老用户".equals(conflict.token())));
        assertTrue(report.conflicts().stream().anyMatch(conflict ->
                "MISSING_CANONICAL_FIELD".equals(conflict.type()) && conflict.message().contains("999")));
    }

    @Test
    void update_preservesProjectBoundaryAndRejectsCrossProjectCanonicalField() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        BusinessGlossary existing = glossary(1L, 1L, "会员", "用户", "user", "hy", null, 101L);
        BusinessGlossary incoming = glossary(null, 999L, "客户", "账号", "customer", "kh", null, 202L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(fieldRepository.findById(202L)).thenReturn(Optional.of(field(202L, 2L, "customer_id", "enabled")));
        BusinessGlossaryServiceImpl service = service(repository, fieldRepository);

        BizException ex = assertThrows(BizException.class, () -> service.update(1L, incoming));

        assertTrue(ex.getMessage().contains("不属于当前项目"));
        assertEquals(1L, existing.getProjectId());
        verify(repository, never()).update(any());
    }

    @Test
    void update_normalizesEditableFieldsAndKeepsExistingProject() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        BusinessGlossary existing = glossary(1L, 1L, "会员", "用户", "user", "hy", null, 101L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(existing));
        when(fieldRepository.findById(101L)).thenReturn(Optional.of(field(101L, 1L, "user_id", "enabled")));
        BusinessGlossaryServiceImpl service = service(repository, fieldRepository);

        BusinessGlossary updated = service.update(1L,
                glossary(null, 999L, " 客户 ", "账号, 账号", " customer ", "kh", "", 101L));

        assertEquals(1L, updated.getProjectId());
        assertEquals("客户", updated.getTerm());
        assertEquals("账号", updated.getSynonyms());
        assertEquals("customer", updated.getRootTerms());
        assertNull(updated.getDisabledTerms());
        verify(repository).update(existing);
    }

    @Test
    void delete_usesSoftDeleteRepositoryPathAfterProjectAccessCheck() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        BusinessGlossary existing = glossary(1L, 1L, "会员", "", "", "", null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        BusinessGlossaryServiceImpl service = service(repository, mock(FieldRepository.class));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void page_rejectsProjectOutsideTokenScope() {
        BusinessGlossaryServiceImpl service = service(mock(BusinessGlossaryRepository.class), mock(FieldRepository.class));
        DataSpecSecurityContext.set(new ApiTokenPrincipal("limited", "tester", false, Set.of(2L)));

        BizException ex = assertThrows(BizException.class, () -> service.page(1L, null, null, 1, 20));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("无权访问项目"));
    }

    @Test
    void match_prefersLongestChineseTermAtTheSamePosition() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(1L, 1L, "会员", "", "", "", null, 101L),
                glossary(2L, 1L, "手机号", "", "", "", null, 102L),
                glossary(3L, 1L, "会员手机号", "", "", "", null, 103L)));
        when(fieldRepository.findById(101L)).thenReturn(Optional.of(field(101L, "member_id", "enabled")));
        when(fieldRepository.findById(102L)).thenReturn(Optional.of(field(102L, "mobile_no", "enabled")));
        when(fieldRepository.findById(103L)).thenReturn(Optional.of(field(103L, "member_mobile_no", "enabled")));
        BusinessGlossaryServiceImpl service = service(repository, fieldRepository);

        List<GlossaryMatch> matches = service.match(1L, "会员手机号");

        assertEquals(1, matches.size());
        assertEquals("会员手机号", matches.getFirst().term());
        assertEquals(QueryTokenResolutionStatus.RESOLVED, matches.getFirst().resolutionStatus());
        verify(repository, times(1)).findAllByProjectId(1L);
    }

    @Test
    void match_requiresAbbreviationToBeACompleteLexicalToken() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(4L, 1L, "订单金额", "", "", "amt", null, 104L)));
        when(fieldRepository.findById(104L)).thenReturn(Optional.of(field(104L, "order_amount", "enabled")));
        BusinessGlossaryServiceImpl service = service(repository, fieldRepository);

        assertEquals(1, service.match(1L, "ord_amt").size());
        assertTrue(service.match(1L, "payment_amount").isEmpty());
    }

    @Test
    void match_rejectsSingleSupplementaryCodePointAsAbbreviationOrRoot() {
        String supplementaryLetter = new String(Character.toChars(0x10400));
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(13L, 1L, "补充字符", "", supplementaryLetter, supplementaryLetter, null, null)));
        BusinessGlossaryServiceImpl service = service(repository, mock(FieldRepository.class));

        assertTrue(service.match(1L, supplementaryLetter).isEmpty());
        assertTrue(service.match(1L, supplementaryLetter + "value").isEmpty());
    }

    @Test
    void match_keepsRootSubstringWithinOneLexicalToken() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(11L, 1L, "支付", "", "pay,tamo", "", null, null)));
        BusinessGlossaryServiceImpl service = service(repository, mock(FieldRepository.class));

        List<GlossaryMatch> matches = service.match(1L, "payment_amount");

        assertEquals(1, matches.size());
        assertEquals("pay", matches.getFirst().matchedToken());
        assertEquals("ROOT", matches.getFirst().matchType());
    }

    @Test
    void match_doesNotCollapseLongAbbreviationsWithTheSamePrefix() {
        String sharedPrefix = "a".repeat(64);
        String configured = sharedPrefix + "x";
        String different = sharedPrefix + "y";
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(12L, 1L, "长缩写", "", "", configured, null, null)));
        BusinessGlossaryServiceImpl service = service(repository, mock(FieldRepository.class));

        assertTrue(service.match(1L, different).isEmpty());
        assertEquals(QueryTokenResolutionStatus.RESOLVED,
                service.match(1L, configured).getFirst().resolutionStatus());
    }

    @Test
    void match_foldsSameCanonicalButKeepsDifferentCanonicalAmbiguous() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(fieldRepository.findById(105L)).thenReturn(Optional.of(field(105L, "order_amount", "enabled")));
        when(fieldRepository.findById(106L)).thenReturn(Optional.of(field(106L, "payment_amount", "enabled")));
        BusinessGlossaryServiceImpl service = service(repository, fieldRepository);

        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(5L, 1L, "订单金额", "", "", "amt", null, 105L),
                glossary(6L, 1L, "订单费用", "", "", "amt", null, 105L)));
        GlossaryMatch folded = service.match(1L, "amt").getFirst();
        assertEquals(QueryTokenResolutionStatus.RESOLVED, folded.resolutionStatus());
        assertEquals(List.of(5L, 6L), folded.glossaryIds());
        assertEquals(105L, folded.canonicalFieldId());

        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(5L, 1L, "订单金额", "", "", "amt", null, 105L),
                glossary(7L, 1L, "支付金额", "", "", "amt", null, 106L)));
        GlossaryMatch ambiguous = service.match(1L, "amt").getFirst();
        assertEquals(QueryTokenResolutionStatus.AMBIGUOUS, ambiguous.resolutionStatus());
        assertEquals(List.of(5L, 7L), ambiguous.glossaryIds());
        assertNull(ambiguous.canonicalFieldId());
        assertEquals(0, ambiguous.score());
    }

    @Test
    void match_marksDisabledTermWithoutCanonicalBinding() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(8L, 1L, "手机号", "", "", "", "老手机号", 108L)));
        BusinessGlossaryServiceImpl service = service(repository, mock(FieldRepository.class));

        GlossaryMatch disabled = service.match(1L, "老手机号").getFirst();

        assertEquals(QueryTokenResolutionStatus.DISABLED, disabled.resolutionStatus());
        assertNull(disabled.canonicalFieldId());
        assertEquals(0, disabled.score());
    }

    @Test
    void match_doesNotExposeUnreadableCanonicalFieldClaims() {
        BusinessGlossaryRepository repository = mock(BusinessGlossaryRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                glossary(9L, 1L, "订单金额", "", "", "amt", null, 109L),
                glossary(10L, 1L, "订单费用", "", "", "amt", null, 110L)));
        when(fieldRepository.findById(109L)).thenReturn(Optional.of(field(109L, 2L, "cross_project_amount", "enabled")));
        when(fieldRepository.findById(110L)).thenReturn(Optional.of(field(110L, 1L, "disabled_amount", "disabled")));
        BusinessGlossaryServiceImpl service = service(repository, fieldRepository);

        GlossaryMatch match = service.match(1L, "amt").getFirst();

        assertEquals(QueryTokenResolutionStatus.AMBIGUOUS, match.resolutionStatus());
        assertNull(match.canonicalFieldId());
        assertNull(match.canonicalFieldName());
        assertFalse(match.reason().contains("109"));
        assertFalse(match.reason().contains("110"));
    }

    private BusinessGlossaryServiceImpl service(BusinessGlossaryRepository repository, FieldRepository fieldRepository) {
        return new BusinessGlossaryServiceImpl(repository, fieldRepository, new NameLexicalTokenizer());
    }

    private BusinessGlossary glossary(Long id, Long projectId, String term, String synonyms, String rootTerms,
                                      String abbreviations, String disabledTerms, Long canonicalFieldId) {
        BusinessGlossary glossary = new BusinessGlossary();
        glossary.setId(id);
        glossary.setProjectId(projectId);
        glossary.setTerm(term);
        glossary.setSynonyms(synonyms);
        glossary.setRootTerms(rootTerms);
        glossary.setAbbreviations(abbreviations);
        glossary.setDisabledTerms(disabledTerms);
        glossary.setCanonicalFieldId(canonicalFieldId);
        glossary.setStatus("enabled");
        return glossary;
    }

    private Field field(Long id, String name, String status) {
        return field(id, 1L, name, status);
    }

    private Field field(Long id, Long projectId, String name, String status) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName(name);
        field.setStatus(status);
        return field;
    }
}
