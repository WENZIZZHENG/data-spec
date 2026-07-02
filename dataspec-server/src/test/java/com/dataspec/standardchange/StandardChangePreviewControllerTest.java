package com.dataspec.standardchange;

import com.dataspec.standardchange.controller.StandardChangePreviewController;
import com.dataspec.standardchange.model.FieldChangePreviewReq;
import com.dataspec.standardchange.model.StandardChangePreview;
import com.dataspec.standardchange.service.StandardChangePreviewService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardChangePreviewControllerTest {

    @Test
    void previewFieldUpdate_returnsServiceResult() {
        StandardChangePreviewService service = mock(StandardChangePreviewService.class);
        StandardChangePreview preview = new StandardChangePreview(
                1L,
                "field",
                10L,
                "user_id",
                "FIELD_UPDATE",
                "INFO",
                false,
                "没有检测到有效字段变更",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null);
        FieldChangePreviewReq req = new FieldChangePreviewReq(
                1L, "user_id", null, "bigint", null, null, null, true,
                null, null, null, null, null, null, null, false, "enabled", null, null, null);
        when(service.previewFieldUpdate(10L, req)).thenReturn(preview);
        StandardChangePreviewController controller = new StandardChangePreviewController(service);

        var response = controller.previewFieldUpdate(10L, req);

        assertSame(preview, response.getData());
        verify(service).previewFieldUpdate(10L, req);
    }
}
