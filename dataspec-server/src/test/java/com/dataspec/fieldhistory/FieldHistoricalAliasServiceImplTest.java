package com.dataspec.fieldhistory;

import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.repository.StandardChangeLogRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.fieldhistory.model.FieldHistoricalAlias;
import com.dataspec.fieldhistory.service.impl.FieldHistoricalAliasServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldHistoricalAliasServiceImplTest {

    @Test
    void loadDerivesAuditableHistoryAndIgnoresCurrentDuplicateOrMalformedValues() {
        StandardChangeLogRepository repository = mock(StandardChangeLogRepository.class);
        when(repository.findFieldHistoryByProjectId(1L)).thenReturn(List.of(
                log(100L, 10L,
                        "{\"name\":\"legacy_phone\",\"displayName\":\"旧手机号\",\"aliases\":\"old_mobile, legacy_phone\"}",
                        "{\"name\":\"mobile_no\",\"displayName\":\"手机号\",\"aliases\":\"mobile_phone\"}"),
                log(101L, 10L,
                        "{\"name\":\"LEGACY_PHONE\",\"aliases\":\"old_mobile\"}",
                        "{broken-json"),
                log(102L, 99L,
                        "{\"name\":\"deleted_field\"}",
                        null)));
        Field current = field(10L, "mobile_no", "手机号", "mobile_phone");

        FieldHistoricalAliasServiceImpl service = new FieldHistoricalAliasServiceImpl(repository, new ObjectMapper());

        Map<Long, List<FieldHistoricalAlias>> result = service.load(1L, List.of(current));

        assertThat(result).containsOnlyKeys(10L);
        assertThat(result.get(10L))
                .extracting(FieldHistoricalAlias::value)
                .containsExactly("legacy_phone", "旧手机号", "old_mobile");
        assertThat(result.get(10L))
                .extracting(FieldHistoricalAlias::evidenceRef)
                .containsOnly("dataspec://change-logs/100");
        verify(repository).findFieldHistoryByProjectId(1L);
    }

    private StandardChangeLog log(Long id, Long targetId, String beforeJson, String afterJson) {
        StandardChangeLog log = new StandardChangeLog();
        log.setId(id);
        log.setProjectId(1L);
        log.setTargetType("field");
        log.setTargetId(targetId);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        return log;
    }

    private Field field(Long id, String name, String displayName, String aliases) {
        Field field = new Field();
        field.setId(id);
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setAliases(aliases);
        return field;
    }
}
