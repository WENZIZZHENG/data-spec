package com.dataspec.standardmaintenanceworkflow.controller;

import com.dataspec.common.result.R;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlan;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlanReq;
import com.dataspec.standardmaintenanceworkflow.service.StandardMaintenanceWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准维护 workflow dry-run API。
 */
@RestController
@RequestMapping("/api/standard-maintenance/workflows")
@RequiredArgsConstructor
public class StandardMaintenanceWorkflowController {

    private final StandardMaintenanceWorkflowService standardMaintenanceWorkflowService;

    @PostMapping("/plan")
    public R<StandardMaintenanceWorkflowPlan> plan(@Valid @RequestBody StandardMaintenanceWorkflowPlanReq req) {
        return R.ok(standardMaintenanceWorkflowService.plan(req));
    }
}
