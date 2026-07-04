package com.dataspec.aibatch;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aibatch.controller.AiBatchController;
import com.dataspec.aibatch.entity.AiBatchRun;
import com.dataspec.aibatch.model.AiBatchDeliveryPackage;
import com.dataspec.aibatch.model.AiBatchFixedSqlSummary;
import com.dataspec.aibatch.model.AiBatchIssueSummary;
import com.dataspec.aibatch.model.AiBatchRunListItem;
import com.dataspec.aibatch.model.AiBatchSummary;
import com.dataspec.aibatch.service.AiBatchService;
import com.dataspec.aitaskrun.model.AiTaskResumeInfo;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiBatchControllerTest {

    @Test
    void list_returnsSummaryWithoutPayloadJson() {
        AiBatchRun run = new AiBatchRun();
        run.setId(42L);
        run.setProjectId(1L);
        run.setBatchType("SQL_LINT");
        run.setSource("frontend");
        run.setStatus("SUCCESS");
        run.setSummaryJson("{\"totalItems\":2,\"errorCount\":1,\"warningCount\":0,\"suggestionCount\":0,\"fixedSqlCount\":1}");
        run.setPayloadJson("{\"large\":\"payload\"}");
        run.setOperatorName("local");
        run.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 30));

        Page<AiBatchRun> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(run));
        AiBatchService service = mock(AiBatchService.class);
        when(service.listByProject(1L, 1, 10)).thenReturn(page);

        AiBatchController controller = new AiBatchController(service);
        R<PageResult<AiBatchRunListItem>> response = controller.list(1L, 1, 10);

        AiBatchRunListItem item = response.getData().getRecords().get(0);
        assertThat(item.id()).isEqualTo(42L);
        assertThat(item.summary().totalItems()).isEqualTo(2);
        assertThat(item.summary().errorCount()).isEqualTo(1);
        assertThat(AiBatchRunListItem.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("payloadJson");
    }

    @Test
    void download_returnsJsonAttachment() {
        AiBatchDeliveryPackage pkg = new AiBatchDeliveryPackage(
                "ai-batch-delivery@1",
                "server-42",
                1L,
                "SQL_LINT",
                "frontend",
                "SUCCESS",
                new AiBatchSummary(1, 1, 0, 0, 0, 0, 0),
                List.of(),
                new AiBatchIssueSummary(0, 0, 0, List.of()),
                new AiBatchFixedSqlSummary(0, 0),
                List.of(),
                List.of(),
                List.of("无需处理"),
                LocalDateTime.of(2026, 6, 28, 10, 30),
                new AiTaskResumeInfo(77L, "SUCCEEDED", false, null, null, "无需处理")
        );
        AiBatchService service = mock(AiBatchService.class);
        when(service.getPackage(42L)).thenReturn(pkg);

        ResponseEntity<String> response = new AiBatchController(service).download(42L);

        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("dataspec-ai-batch-42.json");
        assertThat(response.getBody()).contains("\"batchId\":\"server-42\"");
    }
}
