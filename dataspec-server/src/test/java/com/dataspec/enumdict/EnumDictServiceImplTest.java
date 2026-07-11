package com.dataspec.enumdict;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.enumdict.service.impl.EnumDictServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 枚举字典服务变更记录测试。
 */
class EnumDictServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void create_recordsChangeLog() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        when(repository.existsDictByCodeInProject("order_status", 1L)).thenReturn(false);
        when(changeLogService.snapshot(any(EnumDict.class))).thenReturn("after-json");
        doAnswer(invocation -> {
            EnumDict dict = invocation.getArgument(0);
            dict.setId(21L);
            return 1;
        }).when(repository).insertDict(any(EnumDict.class));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService, objectMapper);

        EnumDict dict = new EnumDict();
        dict.setProjectId(1L);
        dict.setCode("order_status");
        dict.setName("订单状态");

        service.create(dict);

        verify(changeLogService).recordChange(
                1L,
                "enum_dict",
                21L,
                "create",
                null,
                "after-json");
    }

    @Test
    void createValue_preservesLifecycleMetadata() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        when(repository.findDictById(7L)).thenReturn(Optional.of(enumDict(7L, 1L)));
        doAnswer(invocation -> {
            EnumValue value = invocation.getArgument(0);
            value.setId(31L);
            return 1;
        }).when(repository).insertValue(any(EnumValue.class));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService, objectMapper);

        EnumValue value = enumValue(7L, "OLD_PAID", "旧支付成功");
        value.setStatus(" DEPRECATED ");
        value.setAliasesJson("[\"paid_success\", \"legacy_paid\"]");
        value.setReplacementValue(" PAID ");
        value.setValidFrom(LocalDate.of(2020, 1, 1));
        value.setValidTo(LocalDate.of(2025, 12, 31));
        value.setSourceEvidence(" 旧系统枚举映射 ");
        value.setMappingHints(" OLD_PAID 映射到 PAID ");
        value.setAiUsageNotes(" 新 SQL 不要继续使用 OLD_PAID ");

        EnumValue created = service.createValue(value);

        assertThat(created.getId()).isEqualTo(31L);
        assertThat(created.getStatus()).isEqualTo("deprecated");
        assertThat(created.getAliasesJson()).isEqualTo("[\"paid_success\",\"legacy_paid\"]");
        assertThat(created.getReplacementValue()).isEqualTo("PAID");
        assertThat(created.getValidFrom()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(created.getValidTo()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(created.getSourceEvidence()).isEqualTo("旧系统枚举映射");
        assertThat(created.getMappingHints()).isEqualTo("OLD_PAID 映射到 PAID");
        assertThat(created.getAiUsageNotes()).isEqualTo("新 SQL 不要继续使用 OLD_PAID");
        verify(repository).insertValue(created);
    }

    @Test
    void updateValue_copiesLifecycleMetadataWhenProvided() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        EnumValue existing = enumValue(7L, "OLD", "旧值");
        existing.setId(30L);
        when(repository.findValueById(30L)).thenReturn(Optional.of(existing));
        when(repository.findDictById(7L)).thenReturn(Optional.of(enumDict(7L, 1L)));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService, objectMapper);

        EnumValue incoming = enumValue(7L, "OLD", "旧值");
        incoming.setStatus(" DISABLED ");
        incoming.setAliasesJson("[\"legacy\"]");
        incoming.setReplacementValue("NEW");
        incoming.setValidFrom(LocalDate.of(2021, 1, 1));
        incoming.setValidTo(LocalDate.of(2022, 1, 1));
        incoming.setSourceEvidence("停用决策");
        incoming.setMappingHints("旧值映射 NEW");
        incoming.setAiUsageNotes("不要生成旧值");

        EnumValue updated = service.updateValue(30L, incoming);

        assertThat(updated.getStatus()).isEqualTo("disabled");
        assertThat(updated.getAliasesJson()).isEqualTo("[\"legacy\"]");
        assertThat(updated.getReplacementValue()).isEqualTo("NEW");
        assertThat(updated.getValidFrom()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(updated.getValidTo()).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(updated.getSourceEvidence()).isEqualTo("停用决策");
        assertThat(updated.getMappingHints()).isEqualTo("旧值映射 NEW");
        assertThat(updated.getAiUsageNotes()).isEqualTo("不要生成旧值");
        verify(repository).updateValue(updated);
    }

    @Test
    void updateValue_preservesLifecycleMetadataWhenOrdinaryUpdateOmitsIt() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        EnumValue existing = enumValue(7L, "OLD", "旧值");
        existing.setId(30L);
        existing.setStatus("deprecated");
        existing.setAliasesJson("[\"legacy\"]");
        existing.setReplacementValue("NEW");
        existing.setValidFrom(LocalDate.of(2021, 1, 1));
        existing.setValidTo(LocalDate.of(2022, 1, 1));
        existing.setSourceEvidence("停用决策");
        existing.setMappingHints("旧值映射 NEW");
        existing.setAiUsageNotes("不要生成旧值");
        when(repository.findValueById(30L)).thenReturn(Optional.of(existing));
        when(repository.findDictById(7L)).thenReturn(Optional.of(enumDict(7L, 1L)));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService, objectMapper);

        EnumValue incoming = enumValue(7L, "OLD", "旧值改名");

        EnumValue updated = service.updateValue(30L, incoming);

        assertThat(updated.getLabel()).isEqualTo("旧值改名");
        assertThat(updated.getStatus()).isEqualTo("deprecated");
        assertThat(updated.getAliasesJson()).isEqualTo("[\"legacy\"]");
        assertThat(updated.getReplacementValue()).isEqualTo("NEW");
        assertThat(updated.getValidFrom()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(updated.getValidTo()).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(updated.getSourceEvidence()).isEqualTo("停用决策");
        assertThat(updated.getMappingHints()).isEqualTo("旧值映射 NEW");
        assertThat(updated.getAiUsageNotes()).isEqualTo("不要生成旧值");
        verify(repository).updateValue(updated);
    }

    @Test
    void createValue_rejectsInvalidLifecycleStatus() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        when(repository.findDictById(7L)).thenReturn(Optional.of(enumDict(7L, 1L)));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService, objectMapper);
        EnumValue value = enumValue(7L, "UNKNOWN", "未知");
        value.setStatus("archived");

        assertThatThrownBy(() -> service.createValue(value))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无效枚举值状态");

        verify(repository, never()).insertValue(any());
    }

    @Test
    void createValue_rejectsSensitiveLifecycleStatusWithoutEchoingSecret() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        when(repository.findDictById(7L)).thenReturn(Optional.of(enumDict(7L, 1L)));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService, objectMapper);
        EnumValue value = enumValue(7L, "UNKNOWN", "未知");
        value.setStatus("Authorization: Bearer raw-enum-secret");

        assertThatThrownBy(() -> service.createValue(value))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("status 包含敏感连接或凭据信息")
                .satisfies(error -> {
                    assertThat(error.getMessage()).doesNotContain("raw-enum-secret");
                    assertThat(error.getMessage()).doesNotContain("Authorization");
                });

        verify(repository, never()).insertValue(any());
    }

    @Test
    void createValue_acceptsDraftLifecycleStatusBoundary() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        when(repository.findDictById(7L)).thenReturn(Optional.of(enumDict(7L, 1L)));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService, objectMapper);
        EnumValue value = enumValue(7L, "PENDING", "待确认");
        value.setStatus("DRAFT");

        EnumValue created = service.createValue(value);

        assertThat(created.getStatus()).isEqualTo("draft");
        verify(repository).insertValue(created);
    }

    private EnumDict enumDict(Long id, Long projectId) {
        EnumDict dict = new EnumDict();
        dict.setId(id);
        dict.setProjectId(projectId);
        dict.setCode("order_status");
        dict.setName("订单状态");
        return dict;
    }

    private EnumValue enumValue(Long enumId, String value, String label) {
        EnumValue enumValue = new EnumValue();
        enumValue.setEnumId(enumId);
        enumValue.setValue(value);
        enumValue.setLabel(label);
        enumValue.setSortOrder(1);
        return enumValue;
    }
}
