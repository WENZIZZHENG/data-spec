package com.dataspec.aitaskrun;

import com.dataspec.aitaskrun.controller.AiTaskRunController;
import com.dataspec.aitaskrun.model.AiTaskRunListItem;
import com.dataspec.aitaskrun.service.AiTaskRunService;
import com.dataspec.common.result.PageResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiTaskRunControllerTest {

    @Test
    void listAndRecentFailures_returnTaskRunSummaries() {
        AiTaskRunService service = mock(AiTaskRunService.class);
        AiTaskRunListItem item = new AiTaskRunListItem(
                7L,
                1L,
                "SQL_LINT",
                "AI_BATCH",
                42L,
                "PARTIAL_FAILED",
                "hash-1",
                true,
                "lint-items",
                "node tools/dataspec-cli.mjs task show 7 --project 1 --format json",
                "修正失败 SQL 后重试",
                "local",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                null,
                null,
                LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        PageResult<AiTaskRunListItem> page = new PageResult<>();
        page.setRecords(List.of(item));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);
        page.setPages(1);
        when(service.list(1L, "PARTIAL_FAILED", "SQL_LINT", 1, 10)).thenReturn(page);
        when(service.recentFailures(1L, 5)).thenReturn(List.of(item));
        AiTaskRunController controller = new AiTaskRunController(service);

        assertThat(controller.list(1L, "PARTIAL_FAILED", "SQL_LINT", 1, 10).getData().getRecords())
                .extracting(AiTaskRunListItem::id)
                .containsExactly(7L);
        assertThat(controller.recentFailures(1L, 5).getData())
                .extracting(AiTaskRunListItem::failedStep)
                .containsExactly("lint-items");
    }
}
