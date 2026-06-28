package com.dataspec.standardcandidate;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.result.PageResult;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.FieldService;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.standardcandidate.model.StandardCandidateDecisionReq;
import com.dataspec.standardcandidate.model.StandardCandidateMergeReq;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardcandidate.service.impl.StandardCandidateServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardCandidateServiceImplTest {

    @Test
    void page_returnsFilteredProjectCandidates() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        Page<StandardCandidate> page = new Page<>(2, 5, 1);
        page.setRecords(List.of(candidate(10L, 1L, "user_id", "PENDING")));
        when(candidateRepository.page(1L, "PENDING", "COVERAGE", "user", 2, 5)).thenReturn(page);
        StandardCandidateServiceImpl service = service(candidateRepository, mock(FieldRepository.class), mock(FieldService.class));

        PageResult<StandardCandidate> result = service.page(1L, "pending", "coverage", "user", 2, 5);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        verify(candidateRepository).page(1L, "PENDING", "COVERAGE", "user", 2, 5);
    }

    @Test
    void create_insertsSanitizedCandidateAndRejectsDuplicateField() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        StandardCandidateServiceImpl service = service(candidateRepository, fieldRepository, mock(FieldService.class));
        when(fieldRepository.existsByNameInProject("mobile", 1L)).thenReturn(false);
        when(candidateRepository.existsActiveByNameInProject(1L, "mobile")).thenReturn(false);

        StandardCandidate created = service.create(new StandardCandidateCreateReq(
                1L,
                "mobile",
                "手机号",
                "varchar",
                "{\"password\":\"secret\"}",
                "coverage",
                "jdbc:postgresql://localhost/db",
                "{\"token\":\"abc\",\"apiToken\":\"xyz\",\"note\":\"Bearer abc\"}",
                120));

        assertThat(created.getCandidateName()).isEqualTo("mobile");
        assertThat(created.getSourceType()).isEqualTo("COVERAGE");
        assertThat(created.getConfidence()).isEqualTo(100);
        assertThat(created.toString()).doesNotContain(
                "secret",
                "jdbc:postgresql://localhost/db",
                "\"token\":\"abc\"",
                "\"apiToken\":\"xyz\"",
                "Bearer abc");
        verify(candidateRepository).insert(any(StandardCandidate.class));

        when(fieldRepository.existsByNameInProject("mobile", 1L)).thenReturn(true);
        assertThrows(BizException.class, () -> service.create(new StandardCandidateCreateReq(
                1L, "mobile", null, "varchar", null, "manual", null, null, null)));
    }

    @Test
    void create_rejectsValuesLongerThanCandidateSchema() {
        StandardCandidateServiceImpl service = service(
                mock(StandardCandidateRepository.class),
                mock(FieldRepository.class),
                mock(FieldService.class));

        BizException error = assertThrows(BizException.class, () -> service.create(new StandardCandidateCreateReq(
                1L,
                "long_type",
                null,
                "x".repeat(51),
                null,
                "manual",
                null,
                null,
                null)));

        assertThat(error.getMessage()).contains("候选字段类型长度不能超过50");
    }

    @Test
    void accept_createsFieldAndMarksCandidateAccepted() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldService fieldService = mock(FieldService.class);
        StandardCandidate candidate = candidate(10L, 1L, "user_id", "POSTPONED");
        candidate.setDisplayName("用户ID");
        candidate.setDataType("bigint");
        candidate.setComment("用户标识");
        when(candidateRepository.findById(10L)).thenReturn(Optional.of(candidate));
        when(fieldRepository.existsByNameInProject("user_id", 1L)).thenReturn(false);
        Field createdField = field(99L, 1L, "user_id");
        when(fieldService.create(any(Field.class))).thenReturn(createdField);
        StandardCandidateServiceImpl service = service(candidateRepository, fieldRepository, fieldService);

        StandardCandidate accepted = service.accept(10L, new StandardCandidateDecisionReq("确认转正"));

        assertThat(accepted.getStatus()).isEqualTo("ACCEPTED");
        assertThat(accepted.getTargetFieldId()).isEqualTo(99L);
        assertThat(accepted.getDecisionReason()).isEqualTo("确认转正");
        verify(fieldService).create(any(Field.class));
        verify(candidateRepository).update(candidate);
    }

    @Test
    void merge_recordsTargetFieldWithoutMutatingIt() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldService fieldService = mock(FieldService.class);
        StandardCandidate candidate = candidate(10L, 1L, "uid", "PENDING");
        when(candidateRepository.findById(10L)).thenReturn(Optional.of(candidate));
        when(fieldService.getById(20L)).thenReturn(field(20L, 1L, "user_id"));
        StandardCandidateServiceImpl service = service(candidateRepository, fieldRepository, fieldService);

        StandardCandidate merged = service.merge(10L, new StandardCandidateMergeReq(20L, "已有 user_id"));

        assertThat(merged.getStatus()).isEqualTo("MERGED");
        assertThat(merged.getTargetFieldId()).isEqualTo(20L);
        verify(fieldRepository, never()).update(any(Field.class));
        verify(candidateRepository).update(candidate);
    }

    @Test
    void merge_rejectsTargetFieldFromOtherProject() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldService fieldService = mock(FieldService.class);
        when(candidateRepository.findById(10L)).thenReturn(Optional.of(candidate(10L, 1L, "uid", "PENDING")));
        when(fieldService.getById(20L)).thenReturn(field(20L, 2L, "user_id"));
        StandardCandidateServiceImpl service = service(candidateRepository, mock(FieldRepository.class), fieldService);

        assertThrows(BizException.class, () -> service.merge(10L, new StandardCandidateMergeReq(20L, "跨项目")));
    }

    @Test
    void ignoreAndPostpone_recordDecisionReason() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        when(candidateRepository.findById(10L)).thenReturn(Optional.of(candidate(10L, 1L, "uid", "PENDING")));
        when(candidateRepository.findById(11L)).thenReturn(Optional.of(candidate(11L, 1L, "phone", "PENDING")));
        StandardCandidateServiceImpl service = service(candidateRepository, mock(FieldRepository.class), mock(FieldService.class));

        StandardCandidate ignored = service.ignore(10L, new StandardCandidateDecisionReq("历史字段"));
        StandardCandidate postponed = service.postpone(11L, new StandardCandidateDecisionReq("稍后处理"));

        assertThat(ignored.getStatus()).isEqualTo("IGNORED");
        assertThat(postponed.getStatus()).isEqualTo("POSTPONED");
        assertThat(ignored.getDecisionReason()).isEqualTo("历史字段");
        assertThat(postponed.getDecisionReason()).isEqualTo("稍后处理");
    }

    private StandardCandidateServiceImpl service(
            StandardCandidateRepository candidateRepository,
            FieldRepository fieldRepository,
            FieldService fieldService
    ) {
        return new StandardCandidateServiceImpl(candidateRepository, fieldRepository, fieldService);
    }

    private StandardCandidate candidate(Long id, Long projectId, String name, String status) {
        StandardCandidate candidate = new StandardCandidate();
        candidate.setId(id);
        candidate.setProjectId(projectId);
        candidate.setCandidateName(name);
        candidate.setDataType("varchar");
        candidate.setSourceType("MANUAL");
        candidate.setStatus(status);
        candidate.setConfidence(50);
        return candidate;
    }

    private Field field(Long id, Long projectId, String name) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(projectId);
        field.setName(name);
        field.setDataType("bigint");
        return field;
    }
}
