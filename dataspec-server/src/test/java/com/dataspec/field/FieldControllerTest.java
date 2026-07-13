package com.dataspec.field;

import com.dataspec.field.controller.FieldController;
import com.dataspec.field.entity.Field;
import com.dataspec.field.model.FieldBulkUpdatePreview;
import com.dataspec.field.model.FieldBulkUpdateReq;
import com.dataspec.field.model.FieldBulkUpdateResult;
import com.dataspec.field.model.FieldChangeUndoResult;
import com.dataspec.field.model.FieldGroupItem;
import com.dataspec.field.model.FieldGroupSummary;
import com.dataspec.field.model.FieldGroupingBatchUpdateReq;
import com.dataspec.field.model.FieldGroupingBatchUpdateResult;
import com.dataspec.field.model.FieldSearchReq;
import com.dataspec.field.model.FieldSearchPage;
import com.dataspec.field.model.FieldSearchResult;
import com.dataspec.field.model.FieldSearchSummary;
import com.dataspec.field.model.FieldSuggestion;
import com.dataspec.field.service.FieldService;
import com.dataspec.reverseimport.entity.FieldSource;
import com.dataspec.reverseimport.model.FieldSourceDetail;
import com.dataspec.reverseimport.service.ReverseImportSourceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标准字段接口测试
 */
class FieldControllerTest {

    @Test
    void suggest_returnsServiceSuggestions() {
        FieldService service = mock(FieldService.class);
        Field field = new Field();
        field.setName("mobile_no");
        when(service.suggest(1L, "用户手机号", 3))
                .thenReturn(List.of(new FieldSuggestion(field, 92, "显示名匹配", "mobile_no", true)));
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.suggest(1L, "用户手机号", 3);

        assertEquals(1, response.getData().size());
        assertEquals("mobile_no", response.getData().getFirst().recommendedName());
    }

    @Test
    void search_forwardsFiltersToService() {
        FieldService service = mock(FieldService.class);
        FieldSearchReq req = new FieldSearchReq(
                1L, "手机号", "contact", "pii", "enabled", true, 8L, 10,
                3, 25, 9L, false, true);
        FieldSearchResult result = new FieldSearchResult(
                1L,
                "手机号",
                new FieldSearchSummary(1, 1, 0, false, Map.of("category", "contact"), List.of()),
                List.of(),
                List.of("继续收窄查询"),
                new FieldSearchPage(3, 25, 51, 3, true, false));
        when(service.search(req)).thenReturn(result);
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.search(
                1L, "手机号", "contact", "pii", "enabled", true, 8L, 10,
                3, 25, 9L, false, true);

        assertEquals("手机号", response.getData().query());
        assertEquals(3, response.getData().page().current());
        verify(service).search(req);
    }

    @Test
    void listSources_checksFieldAccessBeforeReturningSourceDetails() {
        FieldService service = mock(FieldService.class);
        ReverseImportSourceService sourceService = mock(ReverseImportSourceService.class);
        Field field = new Field();
        field.setId(99L);
        field.setProjectId(1L);
        FieldSource source = new FieldSource();
        source.setFieldId(99L);
        when(service.getById(99L)).thenReturn(field);
        when(sourceService.listByFieldId(99L)).thenReturn(List.of(new FieldSourceDetail(source, null)));
        FieldController controller = new FieldController(service, sourceService);

        var response = controller.listSources(99L);

        assertEquals(1, response.getData().size());
        verify(service).getById(99L);
        verify(sourceService).listByFieldId(99L);
    }

    @Test
    void groupSummary_returnsServiceSummary() {
        FieldService service = mock(FieldService.class);
        FieldGroupSummary summary = new FieldGroupSummary(
                1L,
                2,
                1,
                List.of(new FieldGroupItem("category", "contact", "contact", 2, List.of("mobile_no"), false)));
        when(service.groupSummary(1L)).thenReturn(summary);
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.groupSummary(1L);

        assertEquals(2, response.getData().totalFieldCount());
        assertEquals("contact", response.getData().groups().getFirst().groupKey());
    }

    @Test
    void batchUpdateGrouping_forwardsRequestToService() {
        FieldService service = mock(FieldService.class);
        FieldGroupingBatchUpdateReq req = new FieldGroupingBatchUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("category", "contact"));
        when(service.batchUpdateGrouping(req))
                .thenReturn(new FieldGroupingBatchUpdateResult(1L, 2, 2));
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.batchUpdateGrouping(req);

        assertEquals(2, response.getData().updatedCount());
        verify(service).batchUpdateGrouping(req);
    }

    @Test
    void previewBulkUpdate_forwardsRequestToService() {
        FieldService service = mock(FieldService.class);
        FieldBulkUpdateReq req = new FieldBulkUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("status", "deprecated"));
        when(service.previewBulkUpdate(req))
                .thenReturn(new FieldBulkUpdatePreview(1L, 2, 1, 1, List.of()));
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.previewBulkUpdate(req);

        assertEquals(1, response.getData().changedCount());
        verify(service).previewBulkUpdate(req);
    }

    @Test
    void bulkUpdateFields_forwardsRequestToService() {
        FieldService service = mock(FieldService.class);
        FieldBulkUpdateReq req = new FieldBulkUpdateReq(
                1L,
                List.of(1L, 2L),
                Map.of("category", "contact"));
        when(service.bulkUpdateFields(req))
                .thenReturn(new FieldBulkUpdateResult(1L, 2, 1, 1));
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.bulkUpdateFields(req);

        assertEquals(1, response.getData().updatedCount());
        verify(service).bulkUpdateFields(req);
    }

    @Test
    void undoFieldChange_forwardsRequestToService() {
        FieldService service = mock(FieldService.class);
        when(service.undoFieldChange(9L, 50L))
                .thenReturn(new FieldChangeUndoResult(1L, 9L, 50L));
        FieldController controller = new FieldController(service, mock(ReverseImportSourceService.class));

        var response = controller.undoFieldChange(9L, 50L);

        assertEquals(50L, response.getData().logId());
        verify(service).undoFieldChange(9L, 50L);
    }
}
