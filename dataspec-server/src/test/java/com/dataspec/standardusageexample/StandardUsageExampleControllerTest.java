package com.dataspec.standardusageexample;

import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.standardusageexample.controller.StandardUsageExampleController;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.model.StandardUsageExampleSaveReq;
import com.dataspec.standardusageexample.service.StandardUsageExampleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardUsageExampleControllerTest {

    @Test
    void pageCreateUpdateAndDelete_delegateToService() {
        StandardUsageExampleService service = mock(StandardUsageExampleService.class);
        StandardUsageExample example = new StandardUsageExample();
        example.setId(10L);
        example.setProjectId(1L);
        example.setScope("FIELD");
        PageResult<StandardUsageExample> page = new PageResult<>();
        page.setRecords(List.of(example));
        page.setTotal(1);
        StandardUsageExampleSaveReq req = new StandardUsageExampleSaveReq();
        req.setProjectId(1L);
        when(service.page(1L, "FIELD", "GOOD", "enabled", "user", 1, 10)).thenReturn(page);
        when(service.create(req)).thenReturn(example);
        when(service.update(10L, req)).thenReturn(example);
        StandardUsageExampleController controller = new StandardUsageExampleController(service);

        R<PageResult<StandardUsageExample>> pageResponse = controller.page(1L, "FIELD", "GOOD", "enabled", "user", 1, 10);
        R<StandardUsageExample> createResponse = controller.create(req);
        R<StandardUsageExample> updateResponse = controller.update(10L, req);
        R<Void> deleteResponse = controller.delete(1L, 10L);

        assertThat(pageResponse.getData().getTotal()).isEqualTo(1);
        assertThat(createResponse.getData().getId()).isEqualTo(10L);
        assertThat(updateResponse.getData().getProjectId()).isEqualTo(1L);
        assertThat(deleteResponse.getCode()).isEqualTo(200);
        verify(service).delete(1L, 10L);
    }
}
