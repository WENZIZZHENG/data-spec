package com.dataspec.standardmaintenanceworkflow;

import com.dataspec.standardmaintenanceworkflow.controller.StandardMaintenanceWorkflowController;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowExecutionState;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowInboxAction;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlan;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowRecipeBinding;
import com.dataspec.standardmaintenanceworkflow.service.StandardMaintenanceWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StandardMaintenanceWorkflowControllerTest {

    @Test
    void plan_returnsWrappedDryRunPlan() throws Exception {
        StandardMaintenanceWorkflowService service = mock(StandardMaintenanceWorkflowService.class);
        StandardMaintenanceWorkflowPlan plan = new StandardMaintenanceWorkflowPlan(
                1L,
                "workflow-standard-maintenance-1",
                new StandardMaintenanceWorkflowInboxAction(
                        "REVIEW_CANDIDATES",
                        "STANDARD_CANDIDATE",
                        2,
                        "处理标准候选",
                        "选择候选并人工确认采纳、合并或忽略。",
                        true),
                new StandardMaintenanceWorkflowRecipeBinding(
                        "standard-maintenance",
                        1,
                        Map.of("sourceType", "STANDARD_CANDIDATE"),
                        "node tools/dataspec-cli.mjs task-card create --workflow standard-maintenance --project 1"),
                List.of(),
                new StandardMaintenanceWorkflowExecutionState(
                        "DRY_RUN",
                        "precheck-1",
                        true,
                        null),
                "当前只是 dry-run，未执行写入。",
                List.of(),
                List.of());
        when(service.plan(argThat(req -> req.getProjectId().equals(1L)
                && "STANDARD_CANDIDATE".equals(req.getSourceType())))).thenReturn(plan);
        MockMvc mockMvc = standaloneSetup(new StandardMaintenanceWorkflowController(service)).build();

        mockMvc.perform(post("/api/standard-maintenance/workflows/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"sourceType\":\"STANDARD_CANDIDATE\",\"sourceIds\":[10,11]}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.inboxAction.actionType").value("REVIEW_CANDIDATES"))
                .andExpect(jsonPath("$.data.recipeBinding.recipeId").value("standard-maintenance"))
                .andExpect(jsonPath("$.data.executionState.status").value("DRY_RUN"))
                .andExpect(jsonPath("$.data.undoHint").value("当前只是 dry-run，未执行写入。"));

        verify(service).plan(argThat(req -> req.getProjectId().equals(1L)
                && "STANDARD_CANDIDATE".equals(req.getSourceType())));
    }
}
