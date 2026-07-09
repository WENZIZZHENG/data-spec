import { typedPost } from '@/api/typedClient'
import type { StandardMaintenanceWorkflowPlan, StandardMaintenanceWorkflowPlanReq } from '@/types'

const postWorkflowPlan = typedPost as unknown as (
  path: string,
  data: StandardMaintenanceWorkflowPlanReq
) => Promise<StandardMaintenanceWorkflowPlan>

/**
 * 生成标准维护 workflow dry-run 计划。
 *
 * 该调用只读取 DataSpec 维护信号并返回步骤计划，不执行候选采纳、字段编辑或覆盖率刷新。
 */
export function generateStandardMaintenanceWorkflowPlan(data: StandardMaintenanceWorkflowPlanReq) {
  return postWorkflowPlan('/api/standard-maintenance/workflows/plan', data)
}
