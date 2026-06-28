package com.dataspec.aifeedback;

import com.dataspec.aifeedback.controller.AiFeedbackController;
import com.dataspec.aifeedback.model.AiFeedbackReport;
import com.dataspec.aifeedback.model.AiFeedbackSampleSize;
import com.dataspec.aifeedback.model.AiFeedbackSummary;
import com.dataspec.aifeedback.service.AiFeedbackService;
import com.dataspec.common.result.R;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiFeedbackControllerTest {

    @Test
    void report_returnsServiceReport() {
        AiFeedbackReport report = new AiFeedbackReport(
                1L,
                new AiFeedbackSummary(1, 2, 0, 0, 1, 1, 1, true, "缺少推荐历史"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new AiFeedbackSampleSize(1, 2, 0, 0, 3),
                LocalDateTime.of(2026, 6, 28, 10, 30)
        );
        AiFeedbackService service = mock(AiFeedbackService.class);
        when(service.buildReport(1L)).thenReturn(report);

        R<AiFeedbackReport> response = new AiFeedbackController(service).report(1L);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().summary().sqlCheckCount()).isEqualTo(2);
        assertThat(response.getData().sampleSize().fields()).isEqualTo(3);
    }
}
