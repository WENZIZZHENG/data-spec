import type {
  StandardFieldMergeApplyReq,
  StandardFieldMergePreview,
  StandardFieldMergePreviewReq,
  StandardFieldMergeResult,
  StandardFieldMergeRisk
} from '@/types'

/** 构造标准字段合并预览请求，保持字段 ID 与项目 ID 的显式边界。 */
export function buildFieldMergePreviewPayload(
  projectId: number,
  targetFieldId: number,
  sourceFieldId: number
): StandardFieldMergePreviewReq {
  return {
    projectId,
    targetFieldId,
    sourceFieldId
  }
}

/** 构造标准字段合并确认请求，reason 在前端先 trim，避免提交空白原因。 */
export function buildFieldMergeApplyPayload(
  projectId: number,
  targetFieldId: number,
  sourceFieldId: number,
  reason: string
): StandardFieldMergeApplyReq {
  return {
    projectId,
    targetFieldId,
    sourceFieldId,
    reason: reason.trim()
  }
}

export function fieldMergeRiskTagType(severity?: string) {
  if (severity === 'ERROR') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

export function blockingMergeRisks(preview?: StandardFieldMergePreview | null): StandardFieldMergeRisk[] {
  return (preview?.risks ?? []).filter((risk) => risk.blocking)
}

export function hasBlockingMergeRisk(preview?: StandardFieldMergePreview | null) {
  return blockingMergeRisks(preview).length > 0
}

export function formatMergeValue(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '空'
  }
  if (Array.isArray(value)) {
    return value.length > 0 ? value.join(', ') : '空'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

export function mergeFieldOptionLabel(name?: string, displayName?: string, status?: string) {
  const title = name || displayName || '-'
  const suffix = displayName && displayName !== name ? `（${displayName}）` : ''
  return status ? `${title}${suffix} · ${status}` : `${title}${suffix}`
}

/** 合并提交拦截结果，用于页面区分“未提交原因”和真实 API 返回。 */
export interface FieldMergeSubmitResult {
  submitted: boolean
  error?: string
  result?: StandardFieldMergeResult
}

/** 当前页面选择的合并字段，用于防止旧 preview 在选择变化后被误提交。 */
export interface FieldMergeSubmitSelection {
  projectId?: number | null
  targetFieldId?: number | null
  sourceFieldId?: number | null
}

/** 提交合并前统一拦截空原因、阻断风险和缺失字段，确保页面不会误调用 apply API。 */
export async function submitFieldMergeApply(
  preview: StandardFieldMergePreview | null | undefined,
  reason: string,
  applyFn: (payload: StandardFieldMergeApplyReq) => Promise<StandardFieldMergeResult>,
  selection?: FieldMergeSubmitSelection
): Promise<FieldMergeSubmitResult> {
  const normalizedReason = reason.trim()
  if (!normalizedReason) {
    return { submitted: false, error: '请输入合并原因' }
  }
  if (hasBlockingMergeRisk(preview)) {
    return { submitted: false, error: '存在阻断风险，无法应用合并' }
  }
  const projectId = preview?.projectId
  const targetFieldId = preview?.target?.id
  const sourceFieldId = preview?.source?.id
  if (!projectId || !targetFieldId || !sourceFieldId) {
    return { submitted: false, error: '合并预览缺少字段信息' }
  }
  if (selection && (
    selection.projectId !== projectId
    || selection.targetFieldId !== targetFieldId
    || selection.sourceFieldId !== sourceFieldId
  )) {
    return { submitted: false, error: '字段选择已变化，请重新生成合并预览' }
  }
  const result = await applyFn(buildFieldMergeApplyPayload(projectId, targetFieldId, sourceFieldId, normalizedReason))
  return { submitted: true, result }
}
