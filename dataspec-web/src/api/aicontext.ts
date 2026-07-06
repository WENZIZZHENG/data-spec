import request from '@/api/request'
import type { AiContextScope, AiContextScopeParams } from '@/utils/aiContextScope'

/** AI Context 预算计划的质量风险等级。 */
export type AiContextBudgetQualityRisk = 'LOW' | 'MEDIUM' | 'HIGH'

/** AI Context 预算计划请求；projectId 由页面或 CLI 明确传入，服务端只做只读估算。 */
export interface AiContextBudgetPlanRequest {
  /** 项目 ID，用于读取当前项目标准元数据。 */
  projectId: number
  /** 调用方可接受的 token 预算，单位为服务端保守估算 token。 */
  tokenBudget: number
  /** 可选任务类型，用于匹配 AI profile 默认 scope。 */
  taskType?: string
  /** 可选 AI profile ID，优先于 taskType。 */
  profileId?: string
  /** 可选裁剪范围，沿用 AI Context scoped export 语义。 */
  scope?: AiContextScope
  /** 可选检索词，用于裁剪字段、规则和示例估算。 */
  query?: string
  /** 可选字段状态过滤，例如 enabled 或 deprecated。 */
  status?: string
  /** 可选字段上限，缺省表示不主动截断。 */
  limit?: number
  /** 可选目标表提示；服务端响应只返回脱敏摘要。 */
  targetTable?: string
  /** 可选目标文件提示；服务端响应只返回脱敏摘要。 */
  targetFile?: string
}

/** 服务端回显的脱敏请求摘要和 scope 命中计数。 */
export interface AiContextBudgetRequestEcho {
  /** 项目 ID。 */
  projectId: number
  /** 请求中的 token 预算。 */
  tokenBudget: number
  /** 实际采用的任务类型。 */
  taskType?: string
  /** 实际采用的 profile ID。 */
  profileId?: string
  /** 实际裁剪范围。 */
  scope?: AiContextScope | string
  /** 实际检索词摘要，可能已脱敏或截断。 */
  query?: string
  /** 实际字段状态过滤。 */
  status?: string
  /** 实际字段上限。 */
  limit?: number
  /** 目标表提示摘要。 */
  targetTable?: string
  /** 目标文件提示摘要。 */
  targetFile?: string
  /** 项目字段总数。 */
  totalFieldCount: number
  /** 裁剪条件命中的字段数。 */
  matchedFieldCount: number
  /** 应用于 limit 后用于估算的字段数。 */
  returnedFieldCount: number
}

/** AI Context 预算估算摘要；这是确定性本地估算，不等于模型 tokenizer 精确值。 */
export interface AiContextBudgetEstimation {
  /** 请求预算，单位为保守估算 token。 */
  tokenBudget: number
  /** 当前计划选择的 artifact 估算 token 总数。 */
  selectedEstimatedTokens: number
  /** 完整候选 artifact 的估算 token 总数。 */
  totalEstimatedTokens: number
  /** 服务端使用的估算方法说明。 */
  estimationMethod: string
  /** 估算可信度摘要。 */
  confidence: string
}

/** planner 推荐的导出参数；仅供用户显式应用，前端不得静默覆盖。 */
export interface AiContextRecommendedExportParams {
  /** 推荐使用的 scoped export 范围，缺省完整导出时通常为 all。 */
  scope?: AiContextScope | string
  /** 推荐检索词，可能来自 query、targetTable 或 targetFile 且已脱敏。 */
  query?: string
  /** 推荐字段状态过滤。 */
  status?: string
  /** 推荐字段上限。 */
  limit?: number
  /** 推荐沿用的 AI profile ID。 */
  profileId?: string
  /** 推荐沿用的 AI taskType。 */
  taskType?: string
}

/** 预算计划中的单个 artifact 取舍摘要，不包含完整上下文内容。 */
export interface AiContextBudgetArtifact {
  /** artifact 路径或逻辑名称。 */
  artifact: string
  /** artifact 的保守估算 token 数。 */
  estimatedTokens: number
  /** 被选择或丢弃的原因摘要。 */
  reason?: string
  /** 该 artifact 被丢弃时的风险影响摘要。 */
  riskImpact?: string
  /** 生成估算时应用的导出参数摘要。 */
  appliedScope?: AiContextRecommendedExportParams
}

/** AI Context 预算计划响应。 */
export interface AiContextBudgetPlan {
  /** 响应类型标识。 */
  kind: string
  /** 响应 schema 版本。 */
  schemaVersion: number
  /** 项目 ID。 */
  projectId: number
  /** 脱敏后的请求摘要。 */
  request: AiContextBudgetRequestEcho
  /** token 估算摘要。 */
  estimation: AiContextBudgetEstimation
  /** 预算内建议保留的 artifact 列表。 */
  selectedArtifacts: AiContextBudgetArtifact[]
  /** 因预算不足建议舍弃的 artifact 列表。 */
  droppedArtifacts: AiContextBudgetArtifact[]
  /** 当前预算对 AI 任务质量的风险等级。 */
  qualityRisk: AiContextBudgetQualityRisk
  /** 预算不足或 scope 弱命中时的降级步骤。 */
  fallbackSteps: string[]
  /** 建议导出参数，仅供显式应用。 */
  recommendedExportParams: AiContextRecommendedExportParams
  /** 诊断信息，不包含敏感原文。 */
  diagnostics: string[]
  /** 推荐下一步动作，不包含敏感原文。 */
  recommendedNextActions: string[]
}

export function previewDatabaseRules(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, string>('/ai-context/database-rules', {
    params: scopeParams(projectId, options)
  })
}

export function previewFieldCatalog(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, string>('/ai-context/field-catalog', {
    params: scopeParams(projectId, options)
  })
}

export function previewRulesYaml(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, string>('/ai-context/rules-yaml', {
    params: snapshotParams(projectId, options)
  })
}

export function downloadAiContextPackage(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, Blob>('/ai-context/package/download', {
    params: scopeParams(projectId, options),
    responseType: 'blob'
  })
}

export function planAiContextBudget(
  projectId: number,
  data: Omit<AiContextBudgetPlanRequest, 'projectId'>
) {
  return request.post<unknown, AiContextBudgetPlan>('/ai-context/budget/plan', {
    projectId,
    ...data
  })
}

function scopeParams(projectId: number, options: AiContextScopeParams) {
  return {
    projectId,
    ...(options.scope ? { scope: options.scope } : {}),
    ...(options.query ? { query: options.query } : {}),
    ...(options.status ? { status: options.status } : {}),
    ...(options.limit ? { limit: options.limit } : {}),
    ...(options.snapshotId ? { snapshotId: options.snapshotId } : {}),
    ...(options.snapshotVersion ? { snapshotVersion: options.snapshotVersion } : {})
  }
}

function snapshotParams(projectId: number, options: AiContextScopeParams) {
  return {
    projectId,
    ...(options.snapshotId ? { snapshotId: options.snapshotId } : {}),
    ...(options.snapshotVersion ? { snapshotVersion: options.snapshotVersion } : {})
  }
}
