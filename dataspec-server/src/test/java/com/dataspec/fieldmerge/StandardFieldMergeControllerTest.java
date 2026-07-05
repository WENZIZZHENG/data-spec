package com.dataspec.fieldmerge;

import com.dataspec.fieldmerge.controller.StandardFieldMergeController;
import com.dataspec.fieldmerge.model.StandardFieldMergeApplyReq;
import com.dataspec.fieldmerge.model.StandardFieldMergeFieldSummary;
import com.dataspec.fieldmerge.model.StandardFieldMergePreview;
import com.dataspec.fieldmerge.model.StandardFieldMergePreviewReq;
import com.dataspec.fieldmerge.model.StandardFieldMergeResult;
import com.dataspec.fieldmerge.service.StandardFieldMergeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标准字段合并接口测试。
 */
class StandardFieldMergeControllerTest {

    @Test
    void preview_forwardsRequestToMergeService() {
        StandardFieldMergeService service = mock(StandardFieldMergeService.class);
        StandardFieldMergePreviewReq req = new StandardFieldMergePreviewReq(1L, 10L, 20L);
        StandardFieldMergePreview preview = preview();
        when(service.preview(req)).thenReturn(preview);
        StandardFieldMergeController controller = new StandardFieldMergeController(service);

        var response = controller.preview(req);

        assertEquals("standard_field_merge_preview", response.getData().kind());
        verify(service).preview(req);
    }

    @Test
    void apply_forwardsRequestToMergeService() {
        StandardFieldMergeService service = mock(StandardFieldMergeService.class);
        StandardFieldMergeApplyReq req = new StandardFieldMergeApplyReq(1L, 10L, 20L, "统一使用 mobile_no");
        StandardFieldMergeResult result = new StandardFieldMergeResult(
                "standard_field_merge_result",
                1,
                1L,
                true,
                preview(),
                List.of(),
                List.of("查看字段变更日志确认合并结果"));
        when(service.apply(req)).thenReturn(result);
        StandardFieldMergeController controller = new StandardFieldMergeController(service);

        var response = controller.apply(req);

        assertEquals(true, response.getData().applied());
        verify(service).apply(req);
    }

    private StandardFieldMergePreview preview() {
        StandardFieldMergeFieldSummary target = new StandardFieldMergeFieldSummary(
                10L,
                "mobile_no",
                "手机号",
                "varchar(20)",
                true,
                null,
                false,
                "enabled",
                null,
                null,
                List.of("phone"),
                List.of("contact"),
                null,
                List.of(),
                List.of());
        StandardFieldMergeFieldSummary source = new StandardFieldMergeFieldSummary(
                20L,
                "user_mobile",
                "用户手机",
                "varchar(20)",
                true,
                null,
                false,
                "enabled",
                null,
                null,
                List.of("mobile"),
                List.of("pii"),
                null,
                List.of(),
                List.of("customer.user_mobile"));
        return new StandardFieldMergePreview(
                "standard_field_merge_preview",
                1,
                1L,
                10L,
                target,
                source,
                target,
                source,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("填写合并原因后确认应用"));
    }
}
