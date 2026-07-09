package com.dataspec.standardmaintenanceworkflow.service;

import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlan;
import com.dataspec.standardmaintenanceworkflow.model.StandardMaintenanceWorkflowPlanReq;

/**
 * 标准维护 workflow 计划服务。
 */
public interface StandardMaintenanceWorkflowService {

    /**
     * 生成项目级标准维护 dry-run 计划。
     *
     * @param req 计划来源和筛选条件；只用于生成计划，不触发任何写入。
     * @return 脱敏、只读的标准维护 workflow 计划。
     */
    StandardMaintenanceWorkflowPlan plan(StandardMaintenanceWorkflowPlanReq req);
}
