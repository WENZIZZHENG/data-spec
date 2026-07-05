package com.dataspec.standardreuse;

import com.dataspec.standardreuse.controller.StandardReusePackController;
import com.dataspec.standardreuse.model.StandardReusePackApplyReq;
import com.dataspec.standardreuse.model.StandardReusePackAssetCounts;
import com.dataspec.standardreuse.model.StandardReusePackCreateReq;
import com.dataspec.standardreuse.model.StandardReusePackDetail;
import com.dataspec.standardreuse.model.StandardReusePackInfo;
import com.dataspec.standardreuse.service.StandardReusePackService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardReusePackControllerTest {

    @Test
    void createPack_delegatesToService() {
        StandardReusePackService service = mock(StandardReusePackService.class);
        StandardReusePackCreateReq req = new StandardReusePackCreateReq(1L, "shared_core", "通用标准", "2026.07", null);
        StandardReusePackDetail detail = new StandardReusePackDetail(info(), "{}");
        when(service.createPack(req)).thenReturn(detail);
        StandardReusePackController controller = new StandardReusePackController(service);

        var response = controller.createPack(req);

        assertEquals(200, response.getCode());
        assertEquals("shared_core", response.getData().info().packKey());
        verify(service).createPack(req);
    }

    @Test
    void previewAndApply_delegateToService() {
        StandardReusePackService service = mock(StandardReusePackService.class);
        StandardReusePackApplyReq req = new StandardReusePackApplyReq(10L, 2L, false);
        StandardReusePackController controller = new StandardReusePackController(service);

        controller.previewApply(req);
        controller.applyPack(req);

        verify(service).previewApply(req);
        verify(service).applyPack(req);
    }

    @Test
    void listPacks_returnsProjectScopedRows() {
        StandardReusePackService service = mock(StandardReusePackService.class);
        when(service.listPacks(1L)).thenReturn(List.of(info()));
        StandardReusePackController controller = new StandardReusePackController(service);

        var response = controller.listPacks(1L);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        verify(service).listPacks(1L);
    }

    private StandardReusePackInfo info() {
        return new StandardReusePackInfo(
                10L,
                1L,
                "源项目",
                "shared_core",
                "通用标准",
                "2026.07",
                null,
                "hash",
                StandardReusePackAssetCounts.empty(),
                LocalDateTime.parse("2026-07-05T10:00:00"));
    }
}
