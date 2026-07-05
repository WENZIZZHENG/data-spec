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
