package com.dataspec.field;

import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.common.exception.BizException;
import com.dataspec.changelog.entity.StandardChangeLog;
import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldBulkUpdatePreview;
import com.dataspec.field.model.FieldBulkUpdateReq;
import com.dataspec.field.model.FieldBulkUpdateResult;
import com.dataspec.field.model.FieldChangeUndoResult;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingBatchUpdateReq;
import com.dataspec.field.model.FieldGroupingBatchUpdateResult;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.field.service.impl.FieldServiceImpl;
import com.dataspec.reverseimport.repository.FieldSourceRepository;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 标准字段服务测试
 */
class FieldServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void create_defaultsPersonalMetadata() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.existsByNameInProject("mobile_no", 1L)).thenReturn(false);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

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
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

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
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

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

    @Test
    void update_recordsBeforeAndAfterChangeLog() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = new Field();
        existing.setId(9L);
        existing.setProjectId(1L);
        existing.setName("mobile_no");
        existing.setDataType("varchar(20)");
        existing.setAliases("phone");
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("mobile_no", 1L, 9L)).thenReturn(false);
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getDataType() + "|" + field.getAliases();
        });
        FieldServiceImpl service = service(repository, changeLogService);

        Field incoming = new Field();
        incoming.setName("mobile_no");
        incoming.setDataType("varchar(30)");
        incoming.setAliases("phone,mobile");
        incoming.setNullable(true);

        service.update(9L, incoming);

        verify(changeLogService).recordChange(
                1L,
                "field",
                9L,
                "update",
                "varchar(20)|phone",
                "varchar(30)|phone,mobile");
    }

    @Test
    void groupingSummary_groupsDomainCategoryTagAndUngroupedFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile", "enabled");
        mobile.setDomainId(10L);
        mobile.setCategory("contact");
        mobile.setTags("pii, customer");
        Field email = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        email.setDomainId(10L);
        email.setCategory("contact");
        email.setTags("pii");
        Field orderNo = field("order_no", "订单号", "varchar(64)", "订单号", "order", "enabled");
        orderNo.setCategory("order");
        Field raw = field("raw_payload", "原始报文", "jsonb", "原始报文", "", "enabled");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile, email, orderNo, raw));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldGroupSummary summary = service.groupSummary(1L);

        assertEquals(4, summary.totalFieldCount());
        assertEquals(1, summary.ungroupedFieldCount());
        assertTrue(summary.groups().stream().anyMatch(group ->
                "domain".equals(group.groupType())
                        && "10".equals(group.groupKey())
                        && group.fieldCount() == 2
                        && group.sampleFields().contains("mobile_no")));
        assertTrue(summary.groups().stream().anyMatch(group ->
                "category".equals(group.groupType())
                        && "contact".equals(group.groupKey())
                        && group.fieldCount() == 2));
        assertTrue(summary.groups().stream().anyMatch(group ->
                "tag".equals(group.groupType())
                        && "pii".equals(group.groupKey())
                        && group.fieldCount() == 2));
        assertTrue(summary.groups().stream().anyMatch(group ->
                "ungrouped".equals(group.groupType())
                        && group.ungrouped()
                        && group.sampleFields().contains("raw_payload")));
    }

    @Test
    void batchUpdateGrouping_updatesExplicitFieldsAndRecordsChangeLogs() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setCategory("old");
        mobile.setTags("legacy");
        Field email = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        email.setId(2L);
        email.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        when(repository.findById(2L)).thenReturn(Optional.of(email));
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getName() + "|" + field.getDomainId() + "|" + field.getCategory() + "|" + field.getTags();
        });
        FieldServiceImpl service = service(repository, changeLogService);

        FieldGroupingBatchUpdateResult result = service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of(
                        "domainId", 10,
                        "category", "contact",
                        "tags", "pii, customer"
                )));

        assertEquals(2, result.updatedCount());
        assertEquals(10L, mobile.getDomainId());
        assertEquals("contact", mobile.getCategory());
        assertEquals("customer,pii", mobile.getTags());
        assertEquals("contact", email.getCategory());
        verify(repository).update(mobile);
        verify(repository).update(email);
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(1L), eq("update"),
                eq("mobile_no|null|old|legacy"), eq("mobile_no|10|contact|customer,pii"));
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(2L), eq("update"),
                eq("email|null|null|null"), eq("email|10|contact|customer,pii"));
    }

    @Test
    void batchUpdateGrouping_rejectsCrossProjectWithoutPartialUpdate() {
        FieldRepository repository = mock(FieldRepository.class);
        Field own = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        own.setId(1L);
        own.setProjectId(1L);
        Field foreign = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        foreign.setId(2L);
        foreign.setProjectId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(own));
        when(repository.findById(2L)).thenReturn(Optional.of(foreign));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("category", "contact"))));
        verify(repository, never()).update(any());
    }

    @Test
    void batchUpdateGrouping_canClearGroupingFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setDomainId(10L);
        mobile.setCategory("contact");
        mobile.setTags("pii");
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L),
                Map.of(
                        "domainId", "",
                        "category", "",
                        "tags", ""
                )));

        assertNull(mobile.getDomainId());
        assertNull(mobile.getCategory());
        assertNull(mobile.getTags());
    }

    @Test
    void batchUpdateGrouping_deduplicatesFieldIds() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldGroupingBatchUpdateResult result = service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L, 1L),
                Map.of("category", "contact")));

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.updatedCount());
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).update(mobile);
    }

    @Test
    void batchUpdateGrouping_rejectsInvalidFieldIdsBeforeRepositoryLookup() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.batchUpdateGrouping(new FieldGroupingBatchUpdateReq(
                1L,
                List.of(0L),
                Map.of("category", "contact"))));

        verify(repository, never()).findById(any());
        verify(repository, never()).update(any());
    }

    @Test
    void bulkUpdatePreview_reportsNormalizedChangesWithoutWriting() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setCategory("old");
        mobile.setTags("legacy");
        mobile.setSensitive(false);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        FieldServiceImpl service = service(repository, changeLogService);

        FieldBulkUpdatePreview preview = service.previewBulkUpdate(new FieldBulkUpdateReq(
                1L,
                List.of(1L),
                Map.of(
                        "status", "deprecated",
                        "category", "contact",
                        "tags", "pii, customer",
                        "sensitive", true
                )));

        assertEquals(1, preview.requestedCount());
        assertEquals(1, preview.changedCount());
        assertEquals(0, preview.unchangedCount());
        assertEquals(4, preview.items().getFirst().changes().size());
        assertTrue(preview.items().getFirst().changes().stream()
                .anyMatch(change -> "tags".equals(change.attribute())
                        && "legacy".equals(change.beforeValue())
                        && "customer,pii".equals(change.afterValue())));
        verify(repository, never()).update(any());
        verify(changeLogService, never()).recordChange(anyLong(), anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void bulkUpdatePreview_deduplicatesFieldIdsInCounts() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldBulkUpdatePreview preview = service.previewBulkUpdate(new FieldBulkUpdateReq(
                1L,
                List.of(1L, 1L),
                Map.of("status", "deprecated")));

        assertEquals(1, preview.requestedCount());
        assertEquals(1, preview.items().size());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void bulkUpdateFields_updatesChangedFieldsAndSkipsNoopLogs() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setProjectId(1L);
        mobile.setCategory("old");
        Field email = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        email.setId(2L);
        email.setProjectId(1L);
        email.setCategory("contact");
        when(repository.findById(1L)).thenReturn(Optional.of(mobile));
        when(repository.findById(2L)).thenReturn(Optional.of(email));
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation -> {
            Field field = invocation.getArgument(0);
            return field.getName() + "|" + field.getCategory();
        });
        FieldServiceImpl service = service(repository, changeLogService);

        FieldBulkUpdateResult result = service.bulkUpdateFields(new FieldBulkUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("category", "contact")));

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.unchangedCount());
        assertEquals("contact", mobile.getCategory());
        verify(repository).update(mobile);
        verify(repository, never()).update(email);
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(1L), eq("update"),
                eq("mobile_no|old"), eq("mobile_no|contact"));
    }

    @Test
    void bulkUpdatePreview_rejectsUnsupportedKeysBeforeRepositoryLookup() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.previewBulkUpdate(new FieldBulkUpdateReq(
                1L,
                List.of(1L),
                Map.of("name", "bad_name"))));

        verify(repository, never()).findById(any());
        verify(repository, never()).update(any());
    }

    @Test
    void bulkUpdateFields_rejectsCrossProjectWithoutPartialUpdate() {
        FieldRepository repository = mock(FieldRepository.class);
        Field own = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        own.setId(1L);
        own.setProjectId(1L);
        Field foreign = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        foreign.setId(2L);
        foreign.setProjectId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(own));
        when(repository.findById(2L)).thenReturn(Optional.of(foreign));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.bulkUpdateFields(new FieldBulkUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("status", "deprecated"))));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_restoresBeforeSnapshotAndRecordsUndoLog() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "deprecated");
        existing.setId(1L);
        existing.setProjectId(1L);
        existing.setCategory("new");
        Field before = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        before.setId(1L);
        before.setProjectId(1L);
        before.setCategory("old");
        StandardChangeLog log = fieldLog(50L, 1L, 1L, "update", objectMapper.writeValueAsString(before));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("mobile_no", 1L, 1L)).thenReturn(false);
        when(changeLogService.getById(50L)).thenReturn(log);
        when(changeLogService.snapshot(any(Field.class))).thenAnswer(invocation ->
                objectMapper.writeValueAsString(invocation.getArgument(0)));
        FieldServiceImpl service = service(repository, changeLogService);

        FieldChangeUndoResult result = service.undoFieldChange(1L, 50L);

        assertEquals(1L, result.fieldId());
        assertEquals(50L, result.logId());
        assertEquals("enabled", existing.getStatus());
        assertEquals("old", existing.getCategory());
        verify(repository).update(existing);
        verify(changeLogService).recordChange(eq(1L), eq("field"), eq(1L), eq("undo"), anyString(), anyString());
    }

    @Test
    void undoFieldChange_rejectsNameConflictWithoutUpdating() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        before.setId(1L);
        before.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameInProjectExcludeId("email", 1L, 1L)).thenReturn(true);
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 1L, 1L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
        verify(changeLogService, never()).recordChange(anyLong(), anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void undoFieldChange_rejectsMismatchedLogTarget() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        before.setId(2L);
        before.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 1L, 2L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_rejectsCrossProjectLogWithoutUpdating() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        before.setId(1L);
        before.setProjectId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 2L, 1L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_rejectsMissingBeforeJsonWithoutUpdating() {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L)).thenReturn(fieldLog(50L, 1L, 1L, "update", null));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void undoFieldChange_rejectsMismatchedSnapshotWithoutUpdating() throws Exception {
        FieldRepository repository = mock(FieldRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        Field existing = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        existing.setId(1L);
        existing.setProjectId(1L);
        Field before = field("email", "邮箱", "varchar(128)", "邮箱", "mail", "enabled");
        before.setId(2L);
        before.setProjectId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(changeLogService.getById(50L))
                .thenReturn(fieldLog(50L, 1L, 1L, "update", objectMapper.writeValueAsString(before)));
        FieldServiceImpl service = service(repository, changeLogService);

        assertThrows(BizException.class, () -> service.undoFieldChange(1L, 50L));

        verify(repository, never()).update(any());
    }

    @Test
    void suggest_matchesAliasAndChineseDisplayName() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "enabled"),
                field("amount_cent", "金额（分）", "bigint", "支付金额，以分存储", "amount,pay_amount", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户手机号", 5);

        assertFalse(suggestions.isEmpty());
        FieldSuggestion first = suggestions.getFirst();
        assertTrue(first.existing());
        assertEquals("mobile_no", first.recommendedName());
        assertEquals("mobile_no", first.field().getName());
        assertTrue(first.score() > 0);
        assertTrue(first.matchReason().contains("显示名")
                || first.matchReason().contains("注释")
                || first.matchReason().contains("语义词"));
    }

    @Test
    void search_matchesKeywordAliasesAndReturnsAiReadableFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "联系电话", "varchar(20)", "用户联系号码", "phone,mobile,tel", "enabled");
        mobile.setId(1L);
        mobile.setCategory("contact");
        mobile.setTags("pii,customer");
        mobile.setSensitive(true);
        Field amount = field("amount_cent", "金额（分）", "bigint", "支付金额，以分存储", "amount,pay_amount", "enabled");
        amount.setId(2L);
        amount.setCategory("money");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount, mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "用户sjh", null, null, null, null, null, 10));

        assertEquals(1L, result.projectId());
        assertEquals("用户sjh", result.query());
        assertEquals(1, result.items().size());
        assertEquals("mobile_no", result.items().getFirst().field().getName());
        assertTrue(result.items().getFirst().score() > 0);
        assertFalse(result.items().getFirst().matchReasons().isEmpty());
        assertTrue(result.items().getFirst().recommendedUse().contains("敏感字段"));
        assertFalse(result.items().getFirst().nextActions().isEmpty());
        assertEquals(1, result.summary().matchedCount());

        JsonNode evidence = objectMapper.valueToTree(result.items().getFirst()).path("evidence");
        assertTrue(evidence.isArray());
        assertFalse(evidence.isEmpty());
        assertEquals("FIELD", evidence.get(0).path("sourceType").asText());
        assertEquals(1L, evidence.get(0).path("sourceId").asLong());
        assertTrue(evidence.get(0).path("matchReason").asText().contains("语义词")
                || evidence.get(0).path("matchReason").asText().contains("显示名")
                || evidence.get(0).path("matchReason").asText().contains("注释"));
        assertTrue(evidence.get(0).path("confidence").asInt() > 0);
        assertFalse(evidence.get(0).path("docsRef").asText().isBlank());
    }

    @Test
    void suggest_usesBusinessGlossaryCanonicalFieldAndReason() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "联系人号码", "", "enabled");
        mobile.setId(10L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile));
        when(glossaryService.match(1L, "会员手机号")).thenReturn(List.of(
                new GlossaryMatch(3L, "手机号", "手机号", "TERM", 120, 10L, "mobile_no", Set.of("mobile_no"), false,
                        "术语表：手机号 -> mobile_no")
        ));
        FieldServiceImpl service = service(repository, mock(FieldSourceRepository.class), mock(StandardChangeLogService.class), glossaryService);

        List<FieldSuggestion> suggestions = service.suggest(1L, "会员手机号", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("术语表"));
    }

    @Test
    void search_usesBusinessGlossaryExpandedTerms() {
        FieldRepository repository = mock(FieldRepository.class);
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        Field amount = field("amount_cent", "支付金额", "bigint", "金额以分存储", "", "enabled");
        amount.setId(20L);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(amount));
        when(glossaryService.match(1L, "订单费用")).thenReturn(List.of(
                new GlossaryMatch(4L, "费用", "费用", "SYNONYM", 116, 20L, "amount_cent", Set.of("amount_cent"), false,
                        "术语表：费用 -> amount_cent")
        ));
        FieldServiceImpl service = service(repository, mock(FieldSourceRepository.class), mock(StandardChangeLogService.class), glossaryService);

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "订单费用", null, null, null, null, null, 10));

        assertEquals("amount_cent", result.items().getFirst().field().getName());
        assertTrue(result.items().getFirst().matchReasons().stream().anyMatch(reason -> reason.contains("术语表")));
    }

    @Test
    void search_filtersByCategoryTagStatusSensitiveAndSourceBatch() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldSourceRepository sourceRepository = mock(FieldSourceRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled");
        mobile.setId(1L);
        mobile.setCategory("contact");
        mobile.setTags("pii");
        mobile.setSensitive(true);
        Field amount = field("amount_cent", "金额（分）", "bigint", "支付金额", "amount", "deprecated");
        amount.setId(3L);
        amount.setCategory("money");
        amount.setTags("finance");
        amount.setSensitive(false);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile, amount));
        when(sourceRepository.findFieldIdsByProjectAndBatch(1L, 77L)).thenReturn(List.of(3L));
        FieldServiceImpl service = service(repository, sourceRepository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, null, "money", "finance", "deprecated", false, 77L, 10));

        assertEquals(1, result.items().size());
        assertEquals("amount_cent", result.items().getFirst().field().getName());
        assertTrue(result.items().getFirst().matchReasons().contains("导入批次过滤命中: 77"));
        assertEquals(77L, result.summary().appliedFilters().get("sourceBatchId"));
        assertEquals(1, result.summary().matchedCount());
    }

    @Test
    void search_requiresQueryOrFilter() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        BizException ex = assertThrows(BizException.class, () -> service.search(new FieldSearchReq(
                1L, "!!!", null, null, null, null, null, 10)));

        assertTrue(ex.getMessage().contains("字段检索需要"));
        verify(repository, never()).findAllByProjectId(anyLong());
    }

    @Test
    void search_returnsNextActionWhenNoFieldMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSearchResult result = service.search(new FieldSearchReq(
                1L, "发票抬头", null, null, null, null, null, 10));

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.summary().matchedCount());
        assertFalse(result.summary().hints().isEmpty());
        assertTrue(result.nextActions().getFirst().contains("标准候选"));
    }

    @Test
    void suggestAiContract_exposesStableRecommendationFields() {
        FieldRepository repository = mock(FieldRepository.class);
        Field mobile = field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "enabled");
        mobile.setId(15L);
        mobile.setSensitive(true);
        mobile.setCategory("contact");
        mobile.setCodeSetId(10L);
        mobile.setExampleValue("13800138000");
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(mobile));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "用户手机号", 5).getFirst();

        JsonNode root = new ObjectMapper().valueToTree(suggestion);
        assertEquals("mobile_no", root.path("recommendedName").asText());
        assertTrue(root.path("existing").asBoolean());
        assertTrue(root.path("score").asInt() > 0);
        assertFalse(root.path("matchReason").asText().isBlank());
        JsonNode fieldNode = root.path("field");
        assertEquals("mobile_no", fieldNode.path("name").asText());
        assertEquals("手机号", fieldNode.path("displayName").asText());
        assertEquals("varchar(20)", fieldNode.path("dataType").asText());
        assertEquals("phone,mobile,tel", fieldNode.path("aliases").asText());
        assertTrue(fieldNode.path("sensitive").asBoolean());
        assertEquals("enabled", fieldNode.path("status").asText());
        assertEquals("contact", fieldNode.path("category").asText());
        assertEquals(10L, fieldNode.path("codeSetId").asLong());
        assertEquals("13800138000", fieldNode.path("exampleValue").asText());

        JsonNode evidence = root.path("evidence");
        assertTrue(evidence.isArray());
        assertFalse(evidence.isEmpty());
        assertEquals("FIELD", evidence.get(0).path("sourceType").asText());
        assertEquals(15L, evidence.get(0).path("sourceId").asLong());
        assertFalse(evidence.get(0).path("matchReason").asText().isBlank());
        assertTrue(evidence.get(0).path("confidence").asInt() > 0);
        assertFalse(evidence.get(0).path("docsRef").asText().isBlank());
    }

    @Test
    void suggest_matchesSemanticSynonymAndPinyinAbbreviation() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "联系电话", "varchar(20)", "用户联系号码", "", "enabled"),
                field("user_name", "用户姓名", "varchar(64)", "用户姓名", "", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户sjh", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("语义词"));
    }

    @Test
    void suggest_penalizesGenericOnlyMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("user_profile", "用户资料", "jsonb", "用户扩展信息", "", "enabled"),
                field("mobile_no", "联系电话", "varchar(20)", "联系号码", "", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "用户手机号", 5);

        assertEquals("mobile_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().score() > suggestions.get(1).score());
    }

    @Test
    void suggest_marksSensitiveFieldInReason() {
        FieldRepository repository = mock(FieldRepository.class);
        Field idCard = field("id_card_no", "身份证号", "varchar(32)", "证件号码", "", "enabled");
        idCard.setSensitive(true);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(idCard));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "sfzh", 5);

        assertEquals("id_card_no", suggestions.getFirst().recommendedName());
        assertTrue(suggestions.getFirst().matchReason().contains("敏感"));
    }

    @Test
    void suggest_generatesCanonicalFallbackNamesForKnownSemanticGroups() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertEquals("user_id", service.suggest(1L, "用户编号 uid", 5).getFirst().recommendedName());
        assertEquals("mobile_no", service.suggest(1L, "客户手机", 5).getFirst().recommendedName());
        assertEquals("amount_cent", service.suggest(1L, "付款金额", 5).getFirst().recommendedName());
    }

    @Test
    void suggest_doesNotMatchShortEnglishKeywordInsideUnrelatedToken() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("updated_at", "更新时间", "timestamp", "更新时间", "", "enabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        FieldSuggestion suggestion = service.suggest(1L, "status", 5).getFirst();

        assertFalse(suggestion.existing());
        assertEquals("status", suggestion.recommendedName());
    }

    @Test
    void suggest_filtersDisabledFields() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of(
                field("mobile_no", "手机号", "varchar(20)", "用户手机号", "phone,mobile,tel", "disabled")
        ));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "手机号", 5);

        assertFalse(suggestions.isEmpty());
        assertFalse(suggestions.getFirst().existing());
        assertNull(suggestions.getFirst().field());
    }

    @Test
    void suggest_returnsFallbackSnakeCaseNameWhenNoExistingFieldMatches() {
        FieldRepository repository = mock(FieldRepository.class);
        when(repository.findAllByProjectId(1L)).thenReturn(List.of());
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        List<FieldSuggestion> suggestions = service.suggest(1L, "客户生日", 5);

        assertEquals(1, suggestions.size());
        FieldSuggestion fallback = suggestions.getFirst();
        assertFalse(fallback.existing());
        assertEquals("customer_birthday", fallback.recommendedName());
        assertEquals(0, fallback.score());
        assertNull(fallback.field());
    }

    @Test
    void suggest_rejectsDescriptionWithoutSearchableContent() {
        FieldRepository repository = mock(FieldRepository.class);
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        assertThrows(BizException.class, () -> service.suggest(1L, "!!!", 5));
        verify(repository, never()).findAllByProjectId(anyLong());
    }

    @Test
    void getById_rejectsUnauthorizedProject() {
        FieldRepository repository = mock(FieldRepository.class);
        Field field = new Field();
        field.setId(9L);
        field.setProjectId(1L);
        field.setName("mobile_no");
        when(repository.findById(9L)).thenReturn(Optional.of(field));
        DataSpecSecurityContext.set(new ApiTokenPrincipal("limited", "bob", false, Set.of(2L)));
        FieldServiceImpl service = service(repository, mock(StandardChangeLogService.class));

        BizException ex = assertThrows(BizException.class, () -> service.getById(9L));

        assertEquals(403, ex.getCode());
    }

    private Field field(String name, String displayName, String dataType, String comment,
                        String aliases, String status) {
        Field field = new Field();
        field.setProjectId(1L);
        field.setName(name);
        field.setDisplayName(displayName);
        field.setDataType(dataType);
        field.setComment(comment);
        field.setAliases(aliases);
        field.setStatus(status);
        return field;
    }

    private StandardChangeLog fieldLog(Long id, Long projectId, Long fieldId, String action, String beforeJson) {
        StandardChangeLog log = new StandardChangeLog();
        log.setId(id);
        log.setProjectId(projectId);
        log.setTargetType(StandardChangeLogService.TARGET_FIELD);
        log.setTargetId(fieldId);
        log.setAction(action);
        log.setBeforeJson(beforeJson);
        return log;
    }

    private FieldServiceImpl service(FieldRepository repository, StandardChangeLogService changeLogService) {
        return service(repository, mock(FieldSourceRepository.class), changeLogService);
    }

    private FieldServiceImpl service(FieldRepository repository, FieldSourceRepository sourceRepository,
                                     StandardChangeLogService changeLogService) {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(anyLong(), anyString())).thenReturn(List.of());
        return service(repository, sourceRepository, changeLogService, glossaryService);
    }

    private FieldServiceImpl service(FieldRepository repository, FieldSourceRepository sourceRepository,
                                     StandardChangeLogService changeLogService,
                                     BusinessGlossaryService glossaryService) {
        return new FieldServiceImpl(repository, sourceRepository, changeLogService, objectMapper, glossaryService);
    }
}
