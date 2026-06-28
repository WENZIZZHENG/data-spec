package com.dataspec.activity;

import com.dataspec.activity.controller.ProjectActivityController;
import com.dataspec.activity.model.ProjectActivityTimeline;
import com.dataspec.activity.service.ProjectActivityService;
import com.dataspec.common.result.R;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectActivityControllerTest {

    @Test
    void list_returnsServiceTimeline() {
        ProjectActivityTimeline timeline = new ProjectActivityTimeline(1L, List.of(), List.of(), LocalDateTime.now());
        ProjectActivityService service = mock(ProjectActivityService.class);
        when(service.listActivities(1L, "SQL_CHECK", 10)).thenReturn(timeline);

        R<ProjectActivityTimeline> response = new ProjectActivityController(service).list(1L, "SQL_CHECK", 10);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isSameAs(timeline);
    }
}
