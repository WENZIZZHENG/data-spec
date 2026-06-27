package com.dataspec.standard;

import com.dataspec.common.exception.BizException;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.standard.dto.StandardSnapshotCreateReq;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import com.dataspec.standard.entity.StandardSnapshot;
import com.dataspec.standard.repository.StandardSnapshotRepository;
import com.dataspec.standard.service.impl.StandardSnapshotServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StandardSnapshotServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void createSnapshot_storesDeterministicPayloadAndHash() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        StandardSnapshotRepository repository = mock(StandardSnapshotRepository.class);
        FieldService fieldService = mock(FieldService.class);
        EnumDictService enumDictService = mock(EnumDictService.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        StandardSnapshotServiceImpl service = new StandardSnapshotServiceImpl(
                repository,
                fieldService,
                enumDictService,
                ruleConfigService,
                new ObjectMapper());
        when(fieldService.listByProject(1L)).thenReturn(List.of(field(2L, "updated_at"), field(1L, "created_at")));
        EnumDict enumDict = enumDict(7L, "order_status");
        when(enumDictService.listByProject(1L)).thenReturn(List.of(enumDict));
        when(enumDictService.listValues(7L)).thenReturn(List.of(enumValue(2L, "PAID", 20), enumValue(1L, "PENDING", 10)));
        when(ruleConfigService.listByProject(1L)).thenReturn(List.of(rule(9L, "required_columns"), rule(8L, "field_naming_snake_case")));

        StandardSnapshotInfo info = service.createSnapshot(1L, new StandardSnapshotCreateReq(
                "v2026.06.24",
                "P6-1 第一版",
                "AI Context 可复现基线"));

        ArgumentCaptor<StandardSnapshot> captor = ArgumentCaptor.forClass(StandardSnapshot.class);
        verify(repository).save(captor.capture());
        StandardSnapshot saved = captor.getValue();
        assertEquals(1L, saved.getProjectId());
        assertEquals("v2026.06.24", saved.getVersion());
        assertEquals("P6-1 第一版", saved.getName());
        assertEquals(64, saved.getSnapshotHash().length());
        assertTrue(saved.getPayloadJson().indexOf("created_at") < saved.getPayloadJson().indexOf("updated_at"));
        assertTrue(saved.getPayloadJson().indexOf("PENDING") < saved.getPayloadJson().indexOf("PAID"));
        assertTrue(saved.getPayloadJson().indexOf("field_naming_snake_case") < saved.getPayloadJson().indexOf("required_columns"));
        assertEquals(saved.getSnapshotHash(), info.specHash());
        assertEquals("v2026.06.24", info.specVersion());
        assertTrue(info.versioned());
    }

    @Test
    void latestSnapshot_returnsUnversionedWhenMissing() {
        StandardSnapshotRepository repository = mock(StandardSnapshotRepository.class);
        StandardSnapshotServiceImpl service = new StandardSnapshotServiceImpl(
                repository,
                mock(FieldService.class),
                mock(EnumDictService.class),
                mock(RuleConfigService.class),
                new ObjectMapper());
        when(repository.findLatestByProjectId(1L)).thenReturn(Optional.empty());

        StandardSnapshotInfo info = service.getCurrentSnapshot(1L);

        assertFalse(info.versioned());
        assertEquals("unversioned", info.specVersion());
        assertNull(info.specHash());
    }

    @Test
    void createSnapshot_requiresProjectAccess() {
        DataSpecSecurityContext.set(new ApiTokenPrincipal("scoped", "alice", false, Set.of(1L)));
        StandardSnapshotRepository repository = mock(StandardSnapshotRepository.class);
        StandardSnapshotServiceImpl service = new StandardSnapshotServiceImpl(
                repository,
                mock(FieldService.class),
                mock(EnumDictService.class),
                mock(RuleConfigService.class),
                new ObjectMapper());

        BizException ex = assertThrows(BizException.class, () -> service.createSnapshot(2L, new StandardSnapshotCreateReq("v1", null, null)));

        assertEquals(403, ex.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void createSnapshot_rejectsDuplicateVersionBeforeBuildingPayload() {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        StandardSnapshotRepository repository = mock(StandardSnapshotRepository.class);
        FieldService fieldService = mock(FieldService.class);
        StandardSnapshotServiceImpl service = new StandardSnapshotServiceImpl(
                repository,
                fieldService,
                mock(EnumDictService.class),
                mock(RuleConfigService.class),
                new ObjectMapper());
        when(repository.existsByProjectIdAndVersion(1L, "v1")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> service.createSnapshot(1L, new StandardSnapshotCreateReq(" v1 ", null, null)));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("标准版本号已存在"));
        verify(fieldService, never()).listByProject(anyLong());
        verify(repository, never()).save(any());
    }

    private Field field(Long id, String name) {
        Field field = new Field();
        field.setId(id);
        field.setName(name);
        field.setDisplayName(name);
        field.setDataType("timestamp with time zone");
        field.setNullable(false);
        field.setComment(name);
        field.setStatus("enabled");
        return field;
    }

    private EnumDict enumDict(Long id, String code) {
        EnumDict enumDict = new EnumDict();
        enumDict.setId(id);
        enumDict.setCode(code);
        enumDict.setName(code);
        enumDict.setValueType("string");
        return enumDict;
    }

    private EnumValue enumValue(Long id, String value, Integer sortOrder) {
        EnumValue enumValue = new EnumValue();
        enumValue.setId(id);
        enumValue.setValue(value);
        enumValue.setLabel(value);
        enumValue.setSortOrder(sortOrder);
        return enumValue;
    }

    private RuleConfig rule(Long id, String code) {
        RuleConfig rule = new RuleConfig();
        rule.setId(id);
        rule.setRuleCode(code);
        rule.setRuleName(code);
        rule.setSeverity("ERROR");
        rule.setEnabled(true);
        rule.setParamsJson("{}");
        return rule;
    }
}
