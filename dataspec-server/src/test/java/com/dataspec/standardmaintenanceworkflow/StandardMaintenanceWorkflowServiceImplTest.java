package com.dataspec.standardmaintenanceworkflow;

import com.dataspec.fieldquality.model.FieldQualityIssue;
import com.dataspec.fieldquality.model.FieldQualityItem;
import com.dataspec.fieldquality.model.FieldQualityReport;
import com.dataspec.fieldquality.model.FieldQualitySeverity;
import com.dataspec.fieldquality.service.FieldQualityService;
import com.dataspec.standardcandidate.repository.StandardCandidateRepository;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlanReq;
import com.dataspec.standardmaintenanceworkflow.service.impl.StandardMaintenanceWorkflowServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardMaintenanceWorkflowServiceImplTest {

    @Test
    void plan_buildsCandidateReviewWorkflowWithoutMutatingCandidates() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        when(candidateRepository.countByStatuses(1L, List.of("PENDING", "POSTPONED"))).thenReturn(4);
        StandardMaintenanceWorkflowServiceImpl service = new StandardMaintenanceWorkflowServiceImpl(
                candidateRepository,
                fieldQualityService);
        StandardMaintenanceWorkflowPlanReq req = new StandardMaintenanceWorkflowPlanReq();
        req.setProjectId(1L);
        req.setSourceType("STANDARD_CANDIDATE");
        req.setSourceIds(List.of(10L, 11L));

        var plan = service.plan(req);

        assertThat(plan.projectId()).isEqualTo(1L);
        assertThat(plan.inboxAction().actionType()).isEqualTo("REVIEW_CANDIDATES");
        assertThat(plan.inboxAction().confirmationRequired()).isTrue();
        assertThat(plan.recipeBinding().recipeId()).isEqualTo("standard-maintenance");
        assertThat(plan.recipeBinding().sourceParameters()).containsEntry("sourceType", "STANDARD_CANDIDATE");
        assertThat(plan.dryRunSteps()).extracting("phase")
                .contains("precheck", "review", "execute", "verify", "archive");
        assertThat(plan.dryRunSteps()).anySatisfy(step -> {
            assertThat(step.requiresConfirmation()).isTrue();
            assertThat(step.recommendedAction()).contains("/api/standard-candidates");
        });
        assertThat(plan.executionState().status()).isEqualTo("DRY_RUN");
        assertThat(plan.evidenceLinks()).anySatisfy(link -> {
            assertThat(link.sourceCapability()).isEqualTo("standard-candidate-inbox");
            assertThat(link.count()).isEqualTo(4);
        });
        assertThat(plan.undoHint()).contains("未执行写入");
        verify(candidateRepository).countByStatuses(1L, List.of("PENDING", "POSTPONED"));
    }

    @Test
    void plan_buildsQualityRepairWorkflowAndRedactsSensitiveText() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        FieldQualityReport report = new FieldQualityReport();
        report.getSummary().setLowQualityCount(2);
        FieldQualityItem item = new FieldQualityItem();
        item.setFieldId(20L);
        item.setName("secret_token");
        item.setDisplayName("访问凭据");
        item.setScore(45);
        item.getIssues().add(new FieldQualityIssue(
                "comment_missing",
                FieldQualitySeverity.ERROR,
                "缺说明 jdbc:mysql://localhost/db?password=secret",
                "补充注释 Authorization=Bearer raw",
                20));
        report.getFields().add(item);
        FieldQualityItem outOfSelection = new FieldQualityItem();
        outOfSelection.setFieldId(21L);
        outOfSelection.setName("order_amount");
        outOfSelection.setDisplayName("订单金额");
        outOfSelection.setScore(52);
        outOfSelection.getIssues().add(new FieldQualityIssue(
                "comment_missing",
                FieldQualitySeverity.WARNING,
                "缺少业务口径",
                "补充金额口径",
                10));
        report.getFields().add(outOfSelection);
        when(fieldQualityService.report(1L)).thenReturn(report);
        StandardMaintenanceWorkflowServiceImpl service = new StandardMaintenanceWorkflowServiceImpl(
                candidateRepository,
                fieldQualityService);
        StandardMaintenanceWorkflowPlanReq req = new StandardMaintenanceWorkflowPlanReq();
        req.setProjectId(1L);
        req.setSourceType("FIELD_QUALITY");
        req.setSourceIds(List.of(20L));
        req.setIssueCodes(List.of("comment_missing"));

        var plan = service.plan(req);

        assertThat(plan.inboxAction().actionType()).isEqualTo("REPAIR_FIELD_QUALITY");
        assertThat(plan.evidenceLinks()).anySatisfy(link -> {
            assertThat(link.sourceCapability()).isEqualTo("field-quality-scoring");
            assertThat(link.count()).isEqualTo(1);
        });
        assertThat(plan.dryRunSteps()).anySatisfy(step ->
                assertThat(step.description()).contains("comment_missing"));
        assertThat(plan.dryRunSteps()).anySatisfy(step ->
                assertThat(step.description()).contains("ERROR", "补充注释"));
        assertThat(plan.toString()).doesNotContain(
                "jdbc:mysql",
                "password",
                "secret",
                "Authorization",
                "Bearer raw");
        verify(fieldQualityService).report(1L);
    }

    @Test
    void plan_keepsPartialCoverageBoundaryVisible() {
        StandardCandidateRepository candidateRepository = mock(StandardCandidateRepository.class);
        FieldQualityService fieldQualityService = mock(FieldQualityService.class);
        StandardMaintenanceWorkflowServiceImpl service = new StandardMaintenanceWorkflowServiceImpl(
                candidateRepository,
                fieldQualityService);
        StandardMaintenanceWorkflowPlanReq req = new StandardMaintenanceWorkflowPlanReq();
        req.setProjectId(1L);
        req.setSourceType("FIELD_COVERAGE");
        req.setCoverageStatuses(List.of("UNMANAGED", "POSSIBLE_DUPLICATE"));
        req.setSourceStatus("PARTIAL");
        req.setFailedTableCount(2);
        req.setSkippedTableCount(3);
        req.setItemCount(7);

        var plan = service.plan(req);

        assertThat(plan.inboxAction().actionType()).isEqualTo("REPAIR_FIELD_COVERAGE");
        assertThat(plan.executionState().status()).isEqualTo("DRY_RUN");
        assertThat(plan.executionState().blockedReason()).contains("PARTIAL");
        assertThat(plan.evidenceLinks()).anySatisfy(link -> {
            assertThat(link.sourceCapability()).isEqualTo("field-coverage-report");
            assertThat(link.summary()).contains("PARTIAL");
            assertThat(link.summary()).contains("failedTableCount=2", "skippedTableCount=3");
            assertThat(link.count()).isEqualTo(7);
        });
        assertThat(plan.recipeBinding().sourceParameters())
                .containsEntry("failedTableCount", 2)
                .containsEntry("skippedTableCount", 3);
        assertThat(plan.nextActions()).anySatisfy(action ->
                assertThat(action.message()).contains("未扫描或失败字段不能视为已处理"));
    }
}
