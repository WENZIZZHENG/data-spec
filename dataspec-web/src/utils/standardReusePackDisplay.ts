import type {
  StandardReusePackApplyReq,
  StandardReusePackCreateReq,
  StandardReusePackPlan,
  StandardReusePackPlanCounts
} from '@/types'

/** 构造标准复用包创建请求，前端先 trim 用户输入，避免落库空白版本。 */
export function buildStandardReusePackCreatePayload(
  projectId: number,
  packKey: string,
  packName: string,
  basePackVersion: string,
  description?: string
): StandardReusePackCreateReq {
  return {
    projectId,
    packKey: packKey.trim(),
    packName: packName.trim(),
    basePackVersion: basePackVersion.trim(),
    description: description?.trim() || null
  }
}

/** 构造标准复用包应用请求，第一版固定不执行破坏性覆盖。 */
export function buildStandardReusePackApplyPayload(
  packId: number,
  targetProjectId: number,
  overwrite = false
): StandardReusePackApplyReq {
  return {
    packId,
    targetProjectId,
    overwrite
  }
}

export function reusePackActionTagType(action?: string) {
  if (action === 'CREATE' || action === 'MATCHED') {
    return 'success'
  }
  if (action === 'DRIFTED' || action === 'OVERRIDDEN' || action === 'MISSING') {
    return 'warning'
  }
  if (action === 'BLOCKED') {
    return 'danger'
  }
  return 'info'
}

export function summarizeReusePackCounts(plan?: StandardReusePackPlan | null) {
  const counts: StandardReusePackPlanCounts = plan?.counts ?? {}
  return [
    { key: 'created', label: '创建', value: counts.created ?? 0 },
    { key: 'skipped', label: '跳过', value: counts.skipped ?? 0 },
    { key: 'overridden', label: '覆盖项', value: counts.overridden ?? 0 },
    { key: 'drifted', label: '漂移项', value: counts.drifted ?? 0 },
    { key: 'blocked', label: '阻塞', value: counts.blocked ?? 0 },
    { key: 'warnings', label: '警告', value: counts.warnings ?? 0 }
  ]
}

/** AI 写入安全元数据，用于在确认应用前展示 dry-run、幂等和敏感输入约束。 */
export interface AiWriteSafetyMetadata {
  /** true 表示能力只读，不会写入项目资产或检查记录。 */
  readOnly?: boolean
  /** true 表示能力会写入当前项目资产、记录或自动化任务结果。 */
  writesProject?: boolean
  /** true 表示确认写入前必须先完成 preview/compare/plan dry-run。 */
  requiresDryRun?: boolean
  /** true 表示写入后存在撤销、恢复或可审计补偿路径。 */
  supportsUndo?: boolean
  /** true 表示确认写入时必须携带 Idempotency-Key 或等价幂等参数。 */
  requiresIdempotencyKey?: boolean
  /** 调用方需要脱敏或避免落日志的输入字段名。 */
  sensitiveInputs?: string[]
  /** AI 或用户在确认写入前应执行的下一步动作。 */
  nextActions?: string[]
}

/** 构造写入安全摘要所需的最小输入。 */
export interface AiWriteSafetySummaryInput {
  /** 能力目录返回的安全元数据；为空时按未声明安全约束处理。 */
  safety?: AiWriteSafetyMetadata | null
  /** dry-run 计划的数量摘要，用于提示本次应用会产生的用户可见变化。 */
  counts?: StandardReusePackPlanCounts | null
}

export function buildAiWriteSafetySummary(input: AiWriteSafetySummaryInput) {
  const safety = input.safety ?? {}
  const counts = input.counts ?? {}
  const countItems = [
    { key: 'created', label: '创建', value: counts.created ?? 0 },
    { key: 'skipped', label: '跳过', value: counts.skipped ?? 0 },
    { key: 'drifted', label: '漂移项', value: counts.drifted ?? 0 },
    { key: 'blocked', label: '阻塞', value: counts.blocked ?? 0 },
    { key: 'warnings', label: '警告', value: counts.warnings ?? 0 }
  ]
  const requiresDryRun = Boolean(safety.requiresDryRun)
  const requiresIdempotencyKey = Boolean(safety.requiresIdempotencyKey)
  return {
    title: '写入安全 dry-run 摘要',
    requiresDryRun,
    requiresIdempotencyKey,
    requiresReview: Boolean(safety.writesProject || requiresDryRun || requiresIdempotencyKey || countItems.some((item) => item.value > 0)),
    riskText: requiresDryRun ? '需要 dry-run 预览后再确认应用' : '当前写入未声明强制 dry-run',
    idempotencyText: requiresIdempotencyKey ? '确认应用需携带 Idempotency-Key' : '当前写入未强制要求 Idempotency-Key',
    counts: countItems,
    nextActions: safety.nextActions ?? [],
    sensitiveInputs: safety.sensitiveInputs ?? []
  }
}

export function hasBlockingReusePackItems(plan?: StandardReusePackPlan | null) {
  return (plan?.items ?? []).some((item) => item.action === 'BLOCKED')
}

export function shortReusePackHash(hash?: string | null) {
  return hash ? hash.slice(0, 10) : '-'
}

export function formatReusePackCountText(counts?: {
  domains?: number
  fields?: number
  enums?: number
  enumValues?: number
  rules?: number
  templates?: number
  templateFields?: number
}) {
  if (!counts) {
    return '-'
  }
  return `字段 ${counts.fields ?? 0} / 枚举 ${counts.enums ?? 0} / 规则 ${counts.rules ?? 0} / 模板 ${counts.templates ?? 0}`
}
