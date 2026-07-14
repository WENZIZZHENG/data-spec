package com.dataspec.standards;

import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.common.service.ProjectFieldNameReservationGuard;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuiltInStandardsImportServiceTest {

    @Test
    void importBuiltInStandards_loadsYamlAndSkipsExistingItems() {
        DomainRepository domainRepository = mock(DomainRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        when(domainRepository.existsByCodeInProject("system", 1L)).thenReturn(true);
        when(fieldRepository.existsByNameInProject("id", 1L)).thenReturn(true);
        BuiltInStandardsImportService service = new BuiltInStandardsImportService(
                new ObjectMapper(new YAMLFactory()),
                domainRepository,
                fieldRepository,
                mock(ProjectFieldNameReservationGuard.class));

        service.importBuiltInStandards(1L);

        ArgumentCaptor<Domain> domainCaptor = ArgumentCaptor.forClass(Domain.class);
        verify(domainRepository, times(4)).insert(domainCaptor.capture());
        List<String> insertedDomainCodes = domainCaptor.getAllValues().stream()
                .map(Domain::getCode)
                .toList();
        assertFalse(insertedDomainCodes.contains("system"));
        assertTrue(insertedDomainCodes.contains("user"));
        assertTrue(insertedDomainCodes.contains("order"));

        ArgumentCaptor<Field> fieldCaptor = ArgumentCaptor.forClass(Field.class);
        verify(fieldRepository, times(9)).insert(fieldCaptor.capture());
        List<Field> insertedFields = fieldCaptor.getAllValues();
        assertTrue(insertedFields.stream().noneMatch(field -> "id".equals(field.getName())));

        Field mobileNo = insertedFields.stream()
                .filter(field -> "mobile_no".equals(field.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(1L, mobileNo.getProjectId());
        assertEquals("手机号", mobileNo.getDisplayName());
        assertEquals("varchar(20)", mobileNo.getDataType());
        assertEquals("phone,mobile,tel,user_phone", mobileNo.getAliases());
        assertEquals("contact", mobileNo.getCategory());
        assertTrue(mobileNo.getSensitive());
        assertEquals("enabled", mobileNo.getStatus());
        assertEquals("13800138000", mobileNo.getExampleValue());

        Field amountCent = insertedFields.stream()
                .filter(field -> "amount_cent".equals(field.getName()))
                .findFirst()
                .orElseThrow();
        assertFalse(amountCent.getNullable());
        assertEquals("0", amountCent.getDefaultValue());
    }

    @Test
    void importBuiltInStandards_refreshesFieldsAfterNameReservation() {
        DomainRepository domainRepository = mock(DomainRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        Field concurrentlyCreated = new Field();
        concurrentlyCreated.setProjectId(1L);
        concurrentlyCreated.setName("mobile_no");
        when(fieldRepository.findByNamesInProject(anyCollection(), eq(1L)))
                .thenReturn(List.of(concurrentlyCreated));
        BuiltInStandardsImportService service = new BuiltInStandardsImportService(
                new ObjectMapper(new YAMLFactory()),
                domainRepository,
                fieldRepository,
                mock(ProjectFieldNameReservationGuard.class));

        service.importBuiltInStandards(1L);

        ArgumentCaptor<Field> fieldCaptor = ArgumentCaptor.forClass(Field.class);
        verify(fieldRepository, times(9)).insert(fieldCaptor.capture());
        assertTrue(fieldCaptor.getAllValues().stream()
                .noneMatch(field -> "mobile_no".equals(field.getName())));
    }
}
