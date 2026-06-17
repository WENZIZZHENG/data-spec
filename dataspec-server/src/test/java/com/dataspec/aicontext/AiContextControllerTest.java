package com.dataspec.aicontext;

import com.dataspec.aicontext.controller.AiContextController;
import com.dataspec.aicontext.service.AiContextExportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI Context 导出接口测试
 */
class AiContextControllerTest {

    @Test
    void downloadAiContextPackage_returnsZipAttachment() {
        AiContextExportService service = mock(AiContextExportService.class);
        when(service.generateAiContextPackage(1L)).thenReturn(new byte[] { 1, 2, 3 });
        AiContextController controller = new AiContextController(service);

        var response = controller.downloadAiContextPackage(1L);

        assertEquals(MediaType.parseMediaType("application/zip"), response.getHeaders().getContentType());
        assertEquals("attachment; filename=dataspec-ai-context.zip",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(new byte[] { 1, 2, 3 }, response.getBody());
    }

    @Test
    void generateCreateTablePrompt_returnsText() {
        AiContextExportService service = mock(AiContextExportService.class);
        when(service.generateCreateTablePrompt(1L, "订单模块")).thenReturn("prompt text");
        AiContextController controller = new AiContextController(service);

        var response = controller.generateCreateTablePrompt(
                new AiContextController.CreateTablePromptReq(1L, "订单模块"));

        assertEquals("prompt text", response.getData());
    }

    @Test
    void generateFixSqlPrompt_returnsText() {
        AiContextExportService service = mock(AiContextExportService.class);
        when(service.generateFixSqlPrompt(1L, "select 1")).thenReturn("fix prompt");
        AiContextController controller = new AiContextController(service);

        var response = controller.generateFixSqlPrompt(
                new AiContextController.FixSqlPromptReq(1L, "select 1"));

        assertEquals("fix prompt", response.getData());
    }
}
