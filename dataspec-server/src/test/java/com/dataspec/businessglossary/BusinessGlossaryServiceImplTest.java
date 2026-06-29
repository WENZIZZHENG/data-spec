package com.dataspec.businessglossary;

import com.dataspec.businessglossary.entity.BusinessGlossary;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictReport;
import com.dataspec.businessglossary.repository.BusinessGlossaryRepository;
import com.dataspec.businessglossary.service.impl.BusinessGlossaryServiceImpl;
import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
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

    private BusinessGlossaryServiceImpl service(BusinessGlossaryRepository repository, FieldRepository fieldRepository) {
        return new BusinessGlossaryServiceImpl(repository, fieldRepository);
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
