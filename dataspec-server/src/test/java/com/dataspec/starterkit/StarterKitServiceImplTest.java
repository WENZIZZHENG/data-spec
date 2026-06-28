package com.dataspec.starterkit;

import com.dataspec.common.exception.BizException;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.starterkit.entity.StarterKitInstallation;
import com.dataspec.starterkit.model.StarterKitApplyResult;
import com.dataspec.starterkit.repository.StarterKitInstallationRepository;
import com.dataspec.starterkit.service.BuiltInDomainStarterKits;
import com.dataspec.starterkit.service.impl.StarterKitServiceImpl;
import com.dataspec.template.entity.Template;
import com.dataspec.template.entity.TemplateField;
import com.dataspec.template.repository.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarterKitServiceImplTest {

    @Test
    void builtInKits_areSmallComposableAndVersioned() {
        assertTrue(BuiltInDomainStarterKits.list().size() >= 5);
        assertTrue(BuiltInDomainStarterKits.find("user_account").isPresent());
        assertTrue(BuiltInDomainStarterKits.find("order_trade").isPresent());
        for (var kit : BuiltInDomainStarterKits.list()) {
            assertEquals(BuiltInDomainStarterKits.VERSION, kit.version());
            assertTrue(kit.fieldCount() > 0);
            assertTrue(kit.templateCount() > 0);
        }
    }

    @Test
    void applyKit_createsMissingAssetsAndMarksFieldSource() {
        DomainRepository domainRepository = mock(DomainRepository.class);
        EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        TemplateRepository templateRepository = mock(TemplateRepository.class);
        StarterKitInstallationRepository installationRepository = mock(StarterKitInstallationRepository.class);
        StarterKitServiceImpl service = new StarterKitServiceImpl(
                domainRepository,
                enumDictRepository,
                fieldRepository,
                templateRepository,
                installationRepository,
                new ObjectMapper());

        when(domainRepository.findByProjectId(1L)).thenReturn(List.of());
        when(enumDictRepository.findDictsByProjectId(1L)).thenReturn(List.of());
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(List.of());
        when(templateRepository.findByNameInProject(anyString(), eq(1L))).thenReturn(Optional.empty());
        when(enumDictRepository.existsValueByEnumIdAndValue(anyLong(), anyString())).thenReturn(false);
        assignDomainIds(domainRepository);
        assignEnumIds(enumDictRepository);
        assignFieldIds(fieldRepository);
        assignTemplateIds(templateRepository);

        StarterKitApplyResult result = service.applyKit(1L, "user_account", BuiltInDomainStarterKits.VERSION);

        assertEquals(1, result.created().domains());
        assertEquals(1, result.created().enums());
        assertEquals(3, result.created().enumValues());
        assertEquals(7, result.created().fields());
        assertEquals(1, result.created().templates());
        assertTrue(result.createdFields().contains("mobile_phone"));
        assertTrue(result.skippedFields().isEmpty());

        ArgumentCaptor<Field> fieldCaptor = ArgumentCaptor.forClass(Field.class);
        verify(fieldRepository, times(7)).insert(fieldCaptor.capture());
        Field mobile = fieldCaptor.getAllValues().stream()
                .filter(field -> "mobile_phone".equals(field.getName()))
                .findFirst()
                .orElseThrow();
        assertTrue(mobile.getTags().contains("starter:user_account@2026.06"));
        assertTrue(Boolean.TRUE.equals(mobile.getSensitive()));

        ArgumentCaptor<StarterKitInstallation> installationCaptor =
                ArgumentCaptor.forClass(StarterKitInstallation.class);
        verify(installationRepository).insert(installationCaptor.capture());
        assertEquals("user_account", installationCaptor.getValue().getKitKey());
        assertTrue(installationCaptor.getValue().getCreatedCountsJson().contains("\"fields\":7"));
    }

    @Test
    void applyKit_repeatedApplySkipsExistingAssetsWithoutOverwriting() {
        DomainRepository domainRepository = mock(DomainRepository.class);
        EnumDictRepository enumDictRepository = mock(EnumDictRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        TemplateRepository templateRepository = mock(TemplateRepository.class);
        StarterKitInstallationRepository installationRepository = mock(StarterKitInstallationRepository.class);
        StarterKitServiceImpl service = new StarterKitServiceImpl(
                domainRepository,
                enumDictRepository,
                fieldRepository,
                templateRepository,
                installationRepository,
                new ObjectMapper());

        Domain domain = new Domain();
        domain.setId(10L);
        domain.setCode("user");
        EnumDict dict = new EnumDict();
        dict.setId(20L);
        dict.setCode("account_status");
        Template template = new Template();
        template.setId(30L);
        template.setName("用户账号表模板");

        when(domainRepository.findByProjectId(1L)).thenReturn(List.of(domain));
        when(enumDictRepository.findDictsByProjectId(1L)).thenReturn(List.of(dict));
        when(fieldRepository.findAllByProjectId(1L)).thenReturn(userAccountFields());
        when(templateRepository.findByNameInProject(eq("用户账号表模板"), eq(1L))).thenReturn(Optional.of(template));
        when(enumDictRepository.existsValueByEnumIdAndValue(eq(20L), anyString())).thenReturn(true);

        StarterKitApplyResult result = service.applyKit(1L, "user_account", null);

        assertEquals(0, result.created().fields());
        assertEquals(7, result.skipped().fields());
        assertEquals(1, result.skipped().templates());
        assertTrue(result.skippedTemplates().contains("用户账号表模板"));
        verify(fieldRepository, never()).insert(any(Field.class));
        verify(templateRepository, never()).insert(any(Template.class));
        verify(templateRepository, never()).insertField(any(TemplateField.class));
        verify(enumDictRepository, never()).insertValue(any(EnumValue.class));
        verify(installationRepository).insert(any(StarterKitInstallation.class));
    }

    @Test
    void applyKit_rejectsUnknownKitBeforeWriting() {
        StarterKitServiceImpl service = new StarterKitServiceImpl(
                mock(DomainRepository.class),
                mock(EnumDictRepository.class),
                mock(FieldRepository.class),
                mock(TemplateRepository.class),
                mock(StarterKitInstallationRepository.class),
                new ObjectMapper());

        assertThrows(BizException.class, () -> service.applyKit(1L, "missing", null));
    }

    private void assignDomainIds(DomainRepository repository) {
        AtomicLong ids = new AtomicLong(10);
        doAnswer(invocation -> {
            Domain domain = invocation.getArgument(0);
            domain.setId(ids.getAndIncrement());
            return 1;
        }).when(repository).insert(any(Domain.class));
    }

    private void assignEnumIds(EnumDictRepository repository) {
        AtomicLong dictIds = new AtomicLong(20);
        AtomicLong valueIds = new AtomicLong(200);
        doAnswer(invocation -> {
            EnumDict dict = invocation.getArgument(0);
            dict.setId(dictIds.getAndIncrement());
            return 1;
        }).when(repository).insertDict(any(EnumDict.class));
        doAnswer(invocation -> {
            EnumValue value = invocation.getArgument(0);
            value.setId(valueIds.getAndIncrement());
            return 1;
        }).when(repository).insertValue(any(EnumValue.class));
    }

    private void assignFieldIds(FieldRepository repository) {
        AtomicLong ids = new AtomicLong(100);
        doAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            field.setId(ids.getAndIncrement());
            return 1;
        }).when(repository).insert(any(Field.class));
    }

    private void assignTemplateIds(TemplateRepository repository) {
        AtomicLong templateIds = new AtomicLong(300);
        AtomicLong fieldIds = new AtomicLong(400);
        doAnswer(invocation -> {
            Template template = invocation.getArgument(0);
            template.setId(templateIds.getAndIncrement());
            return 1;
        }).when(repository).insert(any(Template.class));
        doAnswer(invocation -> {
            TemplateField field = invocation.getArgument(0);
            field.setId(fieldIds.getAndIncrement());
            return 1;
        }).when(repository).insertField(any(TemplateField.class));
    }

    private List<Field> userAccountFields() {
        return BuiltInDomainStarterKits.find("user_account").orElseThrow().fields().stream()
                .map(seed -> {
                    Field field = new Field();
                    field.setName(seed.name());
                    field.setId((long) seed.name().hashCode());
                    return field;
                })
                .toList();
    }
}
