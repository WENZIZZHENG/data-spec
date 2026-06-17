package com.dataspec.field;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.impl.FieldServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 标准字段服务测试
 */
class FieldServiceImplTest {

    @Test
    void create_defaultsPersonalMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("mobile_no", 1L)).thenReturn(false);
        FieldServiceImpl service = new FieldServiceImpl(repository);

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
        FieldServiceImpl service = new FieldServiceImpl(repository);

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
        FieldServiceImpl service = new FieldServiceImpl(repository);

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
}
