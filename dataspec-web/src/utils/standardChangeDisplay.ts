import type {
  StandardChangePreview,
  StandardChangePreviewChange,
  StandardChangeRiskLevel
} from '@/types'

const ATTRIBUTE_LABELS: Record<string, string> = {
  name: '字段名',
  displayName: '显示名',
  dataType: '数据类型',
  length: '长度',
  precisionVal: '精度',
  scaleVal: '小数位',
  nullable: '允许空值',
  defaultValue: '默认值',
  comment: '注释',
  domainId: '数据域',
  tags: '标签',
  aliases: '别名',
  category: '分类',
  codeSetId: '代码集',
  sensitive: '敏感标记',
  status: '状态',
  exampleValue: '示例值',
  ruleName: '规则名称',
  severity: '规则级别',
  enabled: '启用状态',
  paramsJson: '规则参数'
}

export function standardChangeRiskTagType(risk?: StandardChangeRiskLevel | string) {
  if (risk === 'HIGH') {
    return 'danger'
  }
  if (risk === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

export function standardChangeRiskText(risk?: StandardChangeRiskLevel | string) {
  if (risk === 'HIGH') {
    return '高风险'
  }
  if (risk === 'WARNING') {
    return '需确认'
  }
  return '提示'
}

export function standardChangeAttributeLabel(attribute?: string) {
  return ATTRIBUTE_LABELS[attribute ?? ''] ?? attribute ?? '-'
}

export function standardChangeChangedAttributes(changes?: StandardChangePreviewChange[]) {
  return (changes ?? [])
    .map((change) => standardChangeAttributeLabel(change.attribute))
    .filter(Boolean)
}

export function standardChangeConfirmMessage(preview?: StandardChangePreview | null) {
  if (!preview) {
    return '无法获取变更预览，是否继续保存？'
  }
  const changed = standardChangeChangedAttributes(preview.changes)
  const impactCount = preview.impacts?.length ?? 0
  const commands = preview.validationCommands?.slice(0, 2).join('；')
  const parts = [
    preview.summary || `将修改 ${changed.length} 个属性`,
    changed.length ? `变更：${changed.join('、')}` : '',
    `影响项：${impactCount}`,
    preview.currentSnapshot?.versioned ? `当前快照：${preview.currentSnapshot.specVersion}` : '',
    commands ? `建议验证：${commands}` : ''
  ].filter(Boolean)
  return parts.join('\n')
}

export function shouldShowStandardChangeConfirm(preview?: StandardChangePreview | null) {
  return Boolean(preview?.requiresConfirmation && (preview.changes?.length ?? 0) > 0)
}
