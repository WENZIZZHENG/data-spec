package com.dataspec.standardusageexample;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.result.PageResult;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.model.StandardUsageExampleSaveReq;
import com.dataspec.standardusageexample.repository.StandardUsageExampleRepository;
import com.dataspec.standardusageexample.service.impl.StandardUsageExampleServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardUsageExampleServiceImplTest {

    @Test
    void page_returnsProjectExamplesWithNormalizedFilters() {
        StandardUsageExampleRepository repository = mock(StandardUsageExampleRepository.class);
        Page<StandardUsageExample> page = new Page<>(2, 5, 1);
        page.setRecords(List.of(example(10L, 1L, "FIELD", "GOOD", "enabled", 90)));
        when(repository.page(1L, "FIELD", "GOOD", "enabled", "phone", 2, 5)).thenReturn(page);
        StandardUsageExampleServiceImpl service = new StandardUsageExampleServiceImpl(repository);

        PageResult<StandardUsageExample> result = service.page(1L, "field", "good", "enabled", "phone", 2, 5);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).extracting(StandardUsageExample::getScope).containsExactly("FIELD");
        verify(repository).page(1L, "FIELD", "GOOD", "enabled", "phone", 2, 5);
    }

    @Test
    void create_persistsNormalizedGoodExample() {
        StandardUsageExampleRepository repository = mock(StandardUsageExampleRepository.class);
        ArgumentCaptor<StandardUsageExample> saved = ArgumentCaptor.forClass(StandardUsageExample.class);
        when(repository.insert(saved.capture())).thenReturn(1);
        StandardUsageExampleServiceImpl service = new StandardUsageExampleServiceImpl(repository);

        StandardUsageExample created = service.create(saveReq(
                1L,
                10L,
                "field",
                "good",
                "enabled",
                "使用 user_id 作为用户外键",
                "user_id bigint NOT NULL",
                null,
                "避免 uid",
                "user,ddl",
                120));

        assertThat(created.getScope()).isEqualTo("FIELD");
        assertThat(created.getExampleType()).isEqualTo("GOOD");
        assertThat(created.getStatus()).isEqualTo("enabled");
        assertThat(created.getPriority()).isEqualTo(100);
        assertThat(saved.getValue().getProjectId()).isEqualTo(1L);
        assertThat(saved.getValue().getTags()).isEqualTo("user,ddl");
    }

    @Test
    void create_rejectsUnsafeSecretContent() {
        StandardUsageExampleRepository repository = mock(StandardUsageExampleRepository.class);
        StandardUsageExampleServiceImpl service = new StandardUsageExampleServiceImpl(repository);
        StandardUsageExampleSaveReq req = saveReq(
                1L,
                null,
                "general",
                "bad",
                "enabled",
                "jdbc:postgresql://localhost/db?password=secret",
                null,
                "Bearer abc.def",
                "不要复制真实连接串",
                "unsafe",
                50);

        BizException error = assertThrows(BizException.class, () -> service.create(req));

        assertThat(error.getMessage()).doesNotContain("secret", "abc.def", "jdbc:postgresql");
        verify(repository, never()).insert(any(StandardUsageExample.class));
    }

    @Test
    void update_preservesIdentityAndCreationTime() {
        StandardUsageExampleRepository repository = mock(StandardUsageExampleRepository.class);
        StandardUsageExample existing = example(10L, 1L, "FIELD", "GOOD", "enabled", 50);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 4, 10, 0);
        existing.setCreatedAt(createdAt);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        StandardUsageExampleServiceImpl service = new StandardUsageExampleServiceImpl(repository);

        StandardUsageExampleSaveReq req = saveReq(
                1L,
                20L,
                "rule",
                "bad",
                "disabled",
                "create_time timestamp",
                "created_at timestamp",
                "create_time",
                "使用统一审计字段",
                "audit",
                80);
        req.setRuleCode("field_naming_snake_case");

        StandardUsageExample updated = service.update(10L, req);

        assertThat(updated.getId()).isEqualTo(10L);
        assertThat(updated.getProjectId()).isEqualTo(1L);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getScope()).isEqualTo("RULE");
        assertThat(updated.getExampleType()).isEqualTo("BAD");
        verify(repository).update(existing);
    }

    @Test
    void delete_softDeletesProjectExample() {
        StandardUsageExampleRepository repository = mock(StandardUsageExampleRepository.class);
        when(repository.findById(10L)).thenReturn(Optional.of(example(10L, 1L, "FIELD", "GOOD", "enabled", 50)));
        StandardUsageExampleServiceImpl service = new StandardUsageExampleServiceImpl(repository);

        service.delete(1L, 10L);

        verify(repository).deleteById(10L);
    }

    @Test
    void selectForAiContext_returnsEnabledExamplesByPriority() {
        StandardUsageExampleRepository repository = mock(StandardUsageExampleRepository.class);
        List<StandardUsageExample> examples = List.of(
                example(1L, 1L, "FIELD", "GOOD", "enabled", 100),
                example(2L, 1L, "RULE", "BAD", "enabled", 90));
        when(repository.findForAiContext(1L, List.of(10L), "用户", 3)).thenReturn(examples);
        StandardUsageExampleServiceImpl service = new StandardUsageExampleServiceImpl(repository);

        List<StandardUsageExample> result = service.selectForAiContext(1L, List.of(10L), "用户", 3);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StandardUsageExample::getStatus).containsOnly("enabled");
        verify(repository).findForAiContext(1L, List.of(10L), "用户", 3);
    }

    private StandardUsageExampleSaveReq saveReq(Long projectId,
                                                Long fieldId,
                                                String scope,
                                                String exampleType,
                                                String status,
                                                String input,
                                                String expectedOutput,
                                                String antiPattern,
                                                String reason,
                                                String tags,
                                                Integer priority) {
        StandardUsageExampleSaveReq req = new StandardUsageExampleSaveReq();
        req.setProjectId(projectId);
        req.setFieldId(fieldId);
        req.setScope(scope);
        req.setExampleType(exampleType);
        req.setStatus(status);
        req.setInput(input);
        req.setExpectedOutput(expectedOutput);
        req.setAntiPattern(antiPattern);
        req.setReason(reason);
        req.setTags(tags);
        req.setPriority(priority);
        return req;
    }

    private StandardUsageExample example(Long id, Long projectId, String scope, String exampleType, String status, Integer priority) {
        StandardUsageExample example = new StandardUsageExample();
        example.setId(id);
        example.setProjectId(projectId);
        example.setFieldId(10L);
        example.setScope(scope);
        example.setExampleType(exampleType);
        example.setStatus(status);
        example.setInput("input");
        example.setReason("reason");
        example.setPriority(priority);
        return example;
    }
}
