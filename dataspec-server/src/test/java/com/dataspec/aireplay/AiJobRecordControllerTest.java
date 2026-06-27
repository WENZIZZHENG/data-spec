package com.dataspec.aireplay;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.aireplay.controller.AiJobRecordController;
import com.dataspec.aireplay.entity.AiJobRecord;
import com.dataspec.aireplay.model.AiJobRecordListItem;
import com.dataspec.aireplay.service.AiJobRecordService;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiJobRecordControllerTest {

    @Test
    void list_returnsMetadataWithoutPayloadJson() {
        AiJobRecord record = new AiJobRecord();
        record.setId(42L);
        record.setProjectId(1L);
        record.setJobType("DDL_PREVIEW");
        record.setTitle("DDL 预览");
        record.setInputSummary("orders");
        record.setPromptVersion("ddl-preview@1");
        record.setStatus("SUCCESS");
        record.setInputPayloadJson("{\"large\":\"input\"}");
        record.setOutputPayloadJson("{\"large\":\"output\"}");
        record.setStandardSnapshotVersion("v2026.06.27");
        record.setStandardSnapshotHash("hash123");
        record.setCreatedAt(LocalDateTime.of(2026, 6, 27, 10, 30));

        Page<AiJobRecord> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(record));
        AiJobRecordService service = mock(AiJobRecordService.class);
        when(service.listByProject(1L, "DDL_PREVIEW", 1, 10)).thenReturn(page);

        AiJobRecordController controller = new AiJobRecordController(service);
        R<PageResult<AiJobRecordListItem>> response = controller.list(1L, "DDL_PREVIEW", 1, 10);

        AiJobRecordListItem item = response.getData().getRecords().get(0);
        assertThat(item.id()).isEqualTo(42L);
        assertThat(item.jobType()).isEqualTo("DDL_PREVIEW");
        assertThat(item.standardSnapshotVersion()).isEqualTo("v2026.06.27");
        assertThat(AiJobRecordListItem.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("inputPayloadJson", "outputPayloadJson");
    }
}
