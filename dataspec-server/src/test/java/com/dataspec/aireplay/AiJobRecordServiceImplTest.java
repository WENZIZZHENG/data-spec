package com.dataspec.aireplay;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordCreateReq;
import com.dataspec.aireplay.model.AiJobRecordDetail;
import com.dataspec.aireplay.repository.AiJobRecordRepository;
import com.dataspec.aireplay.service.impl.AiJobRecordServiceImpl;
import com.dataspec.common.exception.BizException;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiJobRecordServiceImplTest {

    @Test
    void create_rejectsMissingProjectIdOrJobType() {
        AiJobRecordServiceImpl service = new AiJobRecordServiceImpl(mock(AiJobRecordRepository.class), new ObjectMapper());

        assertThrows(BizException.class, () -> service.create(new AiJobRecordCreateReq(
                null,
                "CREATE_TABLE_PROMPT",
                "建表 Prompt",
                "订单模块",
                "create-table-prompt@1",
                "SUCCESS",
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null
        )));

        assertThrows(BizException.class, () -> service.create(new AiJobRecordCreateReq(
                1L,
                "",
                "建表 Prompt",
                "订单模块",
                "create-table-prompt@1",
                "SUCCESS",
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null
        )));
    }

    @Test
    void create_serializesPayloadsAndStoresSnapshotMetadata() {
        AiJobRecordRepository repository = mock(AiJobRecordRepository.class);
        ArgumentCaptor<AiJobRecord> captor = ArgumentCaptor.forClass(AiJobRecord.class);
        when(repository.insert(captor.capture())).thenAnswer(invocation -> {
            AiJobRecord record = invocation.getArgument(0);
            record.setId(42L);
            return 1;
        });
        AiJobRecordServiceImpl service = new AiJobRecordServiceImpl(repository, new ObjectMapper());

        AiJobRecord record = service.create(new AiJobRecordCreateReq(
                1L,
                "CREATE_TABLE_PROMPT",
                "建表 Prompt",
                "订单模块",
                "create-table-prompt@1",
                "SUCCESS",
                Map.of("businessDescription", "订单模块"),
                Map.of("prompt", "prompt text"),
                9L,
                "v2026.06.27",
                "hash123",
                null
        ));

        assertThat(record.getId()).isEqualTo(42L);
        AiJobRecord saved = captor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(1L);
        assertThat(saved.getJobType()).isEqualTo("CREATE_TABLE_PROMPT");
        assertThat(saved.getPromptVersion()).isEqualTo("create-table-prompt@1");
        assertThat(saved.getStandardSnapshotId()).isEqualTo(9L);
        assertThat(saved.getInputPayloadJson()).contains("businessDescription");
        assertThat(saved.getOutputPayloadJson()).contains("prompt text");
    }

    @Test
    void create_reusesRecordForSameStableFingerprint() {
        AiJobRecordRepository repository = mock(AiJobRecordRepository.class);
        when(repository.insert(any(AiJobRecord.class))).thenAnswer(invocation -> {
            AiJobRecord record = invocation.getArgument(0);
            record.setId(42L);
            return 1;
        });
        AiJobRecordServiceImpl service = new AiJobRecordServiceImpl(repository, new ObjectMapper());
        AiJobRecordCreateReq req = new AiJobRecordCreateReq(
                1L,
                "SQL_LINT_FIX",
                "SQL 检查与修正",
                "CREATE TABLE users(id bigint);",
                "sql-lint-fix@1",
                "SUCCESS",
                Map.of("sql", "CREATE TABLE users(id bigint);"),
                Map.of("fixedSql", "CREATE TABLE users(id bigint);"),
                9L,
                "v1",
                "hash",
                88L
        );

        AiJobRecord first = service.create(req);
        AiJobRecord second = service.create(req);

        assertThat(second).isSameAs(first);
        verify(repository, times(1)).insert(any(AiJobRecord.class));
    }

    @Test
    void getDetail_parsesPayloadAndBuildsReplayPayload() {
        AiJobRecord record = new AiJobRecord();
        record.setId(42L);
        record.setProjectId(1L);
        record.setJobType("SQL_LINT_FIX");
        record.setTitle("SQL 检查与修正");
        record.setPromptVersion("sql-lint-fix@1");
        record.setStatus("SUCCESS");
        record.setInputPayloadJson("{\"sql\":\"CREATE TABLE UserOrder(id bigint);\"}");
        record.setOutputPayloadJson("{\"fixedSql\":\"CREATE TABLE user_order(id bigint);\"}");
        record.setStandardSnapshotId(9L);
        record.setStandardSnapshotVersion("v2026.06.27");
        record.setStandardSnapshotHash("hash123");
        record.setSqlCheckRecordId(88L);
        AiJobRecordRepository repository = mock(AiJobRecordRepository.class);
        when(repository.findById(42L)).thenReturn(Optional.of(record));
        AiJobRecordServiceImpl service = new AiJobRecordServiceImpl(repository, new ObjectMapper());

        AiJobRecordDetail detail = service.getDetail(42L);

        assertThat(detail.record().getId()).isEqualTo(42L);
        assertThat(detail.inputPayload()).isInstanceOf(Map.class);
        assertThat(detail.outputPayload()).isInstanceOf(Map.class);
        assertThat(detail.replayCommand()).contains("/api/ai-jobs/42");
        assertThat(detail.replayPayload())
                .containsEntry("kind", "dataspec-ai-replay")
                .containsEntry("jobType", "SQL_LINT_FIX")
                .containsEntry("sqlCheckRecordId", 88L);
        @SuppressWarnings("unchecked")
        Map<String, Object> standard = (Map<String, Object>) detail.replayPayload().get("standard");
        assertThat(standard)
                .containsEntry("snapshotId", 9L)
                .containsEntry("specVersion", "v2026.06.27")
                .containsEntry("specHash", "hash123");
    }

    @Test
    void getDetail_rejectsProjectWithoutTokenAccess() {
        AiJobRecord record = new AiJobRecord();
        record.setId(42L);
        record.setProjectId(1L);
        record.setJobType("SQL_LINT_FIX");
        record.setInputPayloadJson("{}");
        record.setOutputPayloadJson("{}");
        AiJobRecordRepository repository = mock(AiJobRecordRepository.class);
        when(repository.findById(42L)).thenReturn(Optional.of(record));
        AiJobRecordServiceImpl service = new AiJobRecordServiceImpl(repository, new ObjectMapper());

        DataSpecSecurityContext.set(new ApiTokenPrincipal("limited", "tester", false, Set.of(2L)));
        try {
            BizException error = assertThrows(BizException.class, () -> service.getDetail(42L));
            assertThat(error.getCode()).isEqualTo(403);
        } finally {
            DataSpecSecurityContext.clear();
        }
    }

    @Test
    void listByProject_delegatesToRepositoryWithTypeFilter() {
        AiJobRecordRepository repository = mock(AiJobRecordRepository.class);
        IPage<AiJobRecord> page = new Page<>(1, 10);
        when(repository.findByProjectId(1L, "DDL_PREVIEW", 1, 10)).thenReturn(page);
        AiJobRecordServiceImpl service = new AiJobRecordServiceImpl(repository, new ObjectMapper());

        IPage<AiJobRecord> result = service.listByProject(1L, "DDL_PREVIEW", 1, 10);

        assertThat(result).isSameAs(page);
    }
}
