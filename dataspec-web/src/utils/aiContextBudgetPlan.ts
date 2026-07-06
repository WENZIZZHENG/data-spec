export type BudgetQualityRisk = 'LOW' | 'MEDIUM' | 'HIGH' | string

interface BudgetEstimationLike {
  selectedEstimatedTokens?: number
  tokenBudget?: number
}

interface BudgetArtifactLike {
  artifact?: string
}

interface BudgetPlanLike {
  estimation?: BudgetEstimationLike
  selectedArtifacts?: BudgetArtifactLike[]
  droppedArtifacts?: BudgetArtifactLike[]
  recommendedNextActions?: string[]
}

export function budgetRiskLabel(risk?: BudgetQualityRisk): string {
  if (risk === 'LOW') {
    return '低风险'
  }
  if (risk === 'MEDIUM') {
    return '中风险'
  }
  if (risk === 'HIGH') {
    return '高风险'
  }
  return risk || '未知风险'
}

export function budgetRiskTagType(risk?: BudgetQualityRisk): 'success' | 'warning' | 'danger' | 'info' {
  if (risk === 'LOW') {
    return 'success'
  }
  if (risk === 'MEDIUM') {
    return 'warning'
  }
  if (risk === 'HIGH') {
    return 'danger'
  }
  return 'info'
}

export function formatEstimatedTokens(estimation?: BudgetEstimationLike): string {
  return `${formatNumber(estimation?.selectedEstimatedTokens)} / ${formatNumber(estimation?.tokenBudget)} tokens`
}

export function buildBudgetPlanSummary(plan?: BudgetPlanLike | null): string {
  if (!plan) {
    return '暂无预算计划'
  }
  const selectedCount = plan.selectedArtifacts?.length ?? 0
  const droppedCount = plan.droppedArtifacts?.length ?? 0
  const nextAction = plan.recommendedNextActions?.[0]
  return [
    formatEstimatedTokens(plan.estimation),
    `已选 ${selectedCount} 项`,
    `舍弃 ${droppedCount} 项`,
    ...(nextAction ? [nextAction] : [])
  ].join(' · ')
}

function formatNumber(value?: number): string {
  return typeof value === 'number' && Number.isFinite(value)
    ? Math.round(value).toLocaleString('en-US')
    : '0'
}
