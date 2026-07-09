import {
  buildCodeFieldReferenceIndex,
  sanitizeSecretText
} from './dataspec-code-refs.mjs'

const SCHEMA_VERSION = 1
const CHANGE_TYPES = {
  RENAME: 'RENAME',
  TYPE_CHANGE: 'TYPE_CHANGE',
  ENUM_CHANGE: 'ENUM_CHANGE'
}

/**
 * 构建标准字段变更对应的本地业务代码 Patch Plan。
 *
 * 该函数是 CLI、测试和后续 MCP/API 复用的本地工具 API；它只读取字段引用索引结果，
 * 不写业务仓库文件，也不把候选 diff 当作可自动应用补丁。
 */
export async function buildCodeFieldPatchPlan({
  fieldName,
  aliases = [],
  scanPaths,
  rootDir,
  outputRootDir = rootDir,
  renameTo = null,
  fromType = null,
  toType = null,
  enumChanges = []
}) {
  const change = normalizePatchChange({ fieldName, aliases, renameTo, fromType, toType, enumChanges })
  const referenceIndex = await buildCodeFieldReferenceIndex({
    fieldNames: [change.fieldName],
    aliases: change.aliases,
    scanPaths,
    rootDir,
    outputRootDir
  })
  const candidateEdits = buildCandidateEdits(referenceIndex.references, change)
  const manualSteps = buildManualSteps(referenceIndex.references, change)
  const riskLevel = summarizePlanRisk(referenceIndex.renameRisk, candidateEdits, manualSteps)
  const dryRunResult = {
    status: candidateEdits.length === 0 && manualSteps.length === 0 ? 'NO_REFERENCES' : 'READY',
    willWrite: false,
    requiresHumanReview: candidateEdits.length > 0 || manualSteps.length > 0,
    candidateEditCount: candidateEdits.length,
    manualStepCount: manualSteps.length
  }
  const verificationCommands = buildVerificationCommands(change, referenceIndex.scan.paths)
  const rollbackHint = {
    affectedFiles: uniqueSorted(candidateEdits.map((item) => item.fileRef.path)),
    hint: '本计划不会写入业务文件；如后续人工修改需要回退，请使用版本控制恢复对应业务文件。'
  }

  return sanitizePlanOutput({
    kind: 'dataspec.code-field.patch-plan',
    schemaVersion: SCHEMA_VERSION,
    change,
    scanSummary: {
      ...referenceIndex.summary,
      paths: referenceIndex.scan.paths
    },
    candidateEdits,
    manualSteps,
    riskLevel,
    dryRunResult,
    verificationCommands,
    rollbackHint,
    safety: {
      readOnly: true,
      writesProject: false,
      requiresDryRun: true,
      requiresIdempotencyKey: false,
      containsRealBusinessRows: false,
      externalNetworkUsed: false,
      externalLlmUsed: false,
      sensitiveInputs: ['businessRepositoryFiles'],
      sensitiveOutputPolicy: 'relative-paths-and-redacted-snippets-only',
      nextActions: [
        '先人工审查 candidateEdits 和 manualSteps。',
        '确认业务修改后再运行 verificationCommands。'
      ]
    },
    diagnostics: buildDiagnostics(referenceIndex, change),
    nextActions: buildNextActions(riskLevel, candidateEdits, manualSteps)
  })
}

export function formatCodeFieldPatchPlanMarkdown(plan) {
  const lines = [
    '# DataSpec Code Patch Plan',
    '',
    `Field: ${plan.change.fieldName}`,
    `Change types: ${plan.change.changeTypes.join(', ')}`,
    `Risk: ${plan.riskLevel}`,
    `Candidate edits: ${plan.candidateEdits.length}`,
    `Manual steps: ${plan.manualSteps.length}`,
    ''
  ]
  if (plan.change.renameTo) {
    lines.push(`Rename: ${plan.change.fieldName} -> ${plan.change.renameTo}`)
  }
  if (plan.change.typeChange) {
    lines.push(`Type change: ${plan.change.typeChange.fromType ?? '<unknown>'} -> ${plan.change.typeChange.toType ?? '<unknown>'}`)
  }
  if (plan.change.enumChanges.length > 0) {
    lines.push(`Enum changes: ${plan.change.enumChanges.map((item) => `${item.from}=${item.to}`).join(', ')}`)
  }
  lines.push('', '## Candidate edits')
  if (plan.candidateEdits.length === 0) {
    lines.push('- No candidate edits found in configured paths.')
  } else {
    for (const edit of plan.candidateEdits.slice(0, 30)) {
      lines.push(`- ${edit.fileRef.path}:${edit.fileRef.line}:${edit.fileRef.column} [${edit.riskLevel}] ${edit.suggestedEdit.description}`)
    }
  }
  lines.push('', '## Manual steps')
  if (plan.manualSteps.length === 0) {
    lines.push('- No manual steps beyond reviewing candidate edits.')
  } else {
    for (const step of plan.manualSteps) {
      lines.push(`- ${step.title}: ${step.description}`)
      for (const item of step.checklist) {
        lines.push(`  - ${item}`)
      }
    }
  }
  lines.push('', '## Verification commands')
  for (const item of plan.verificationCommands) {
    lines.push(`- \`${item.command}\`: ${item.reason}`)
  }
  lines.push('', '## Rollback')
  lines.push(`- ${plan.rollbackHint.hint}`)
  lines.push('', '## Safety')
  lines.push('- Read-only dry-run plan; no business files are modified.')
  lines.push('- Output uses relative paths and redacted snippets.')
  lines.push('', '## Next actions')
  for (const action of plan.nextActions) {
    lines.push(`- ${action}`)
  }
  return `${sanitizeSecretText(lines.join('\n'))}\n`
}

function normalizePatchChange({ fieldName, aliases, renameTo, fromType, toType, enumChanges }) {
  const normalizedFieldName = normalizeRequiredText(fieldName, 'code-patch plan 需要提供 --field')
  const normalizedRenameTo = normalizeText(renameTo)
  const normalizedFromType = normalizeText(fromType)
  const normalizedToType = normalizeText(toType)
  const normalizedEnumChanges = normalizeEnumChanges(enumChanges)
  const changeTypes = []
  if (normalizedRenameTo) {
    changeTypes.push(CHANGE_TYPES.RENAME)
  }
  if (normalizedFromType || normalizedToType) {
    changeTypes.push(CHANGE_TYPES.TYPE_CHANGE)
  }
  if (normalizedEnumChanges.length > 0) {
    changeTypes.push(CHANGE_TYPES.ENUM_CHANGE)
  }
  if (changeTypes.length === 0) {
    throw diagnosticError(
      'CODE_PATCH_CHANGE_REQUIRED',
      'code-patch plan 需要至少提供 --to-field、--from-type/--to-type 或 --enum-change。',
      '传入字段重命名、类型变化或枚举变化后重试。'
    )
  }
  return {
    fieldName: normalizedFieldName,
    aliases: uniqueSorted(aliases.map(normalizeText).filter(Boolean)),
    changeTypes,
    renameTo: normalizedRenameTo,
    typeChange: normalizedFromType || normalizedToType
      ? {
          fromType: normalizedFromType,
          toType: normalizedToType
        }
      : null,
    enumChanges: normalizedEnumChanges
  }
}

function normalizeEnumChanges(enumChanges) {
  return enumChanges.map(normalizeText).filter(Boolean).map((item) => {
    const separatorIndex = item.indexOf('=')
    if (separatorIndex <= 0 || separatorIndex === item.length - 1) {
      throw diagnosticError(
        'CODE_PATCH_ENUM_CHANGE_INVALID',
        'code-patch plan 的 --enum-change 需要使用 old=new 形式。',
        '例如传入 --enum-change DRAFT=PENDING。'
      )
    }
    return {
      from: item.slice(0, separatorIndex).trim(),
      to: item.slice(separatorIndex + 1).trim()
    }
  })
}

function buildCandidateEdits(references, change) {
  return references.map((reference, index) => {
    const riskLevel = riskFromReference(reference)
    return {
      id: `edit-${String(index + 1).padStart(3, '0')}`,
      changeType: primaryCandidateChangeType(change),
      fileRef: {
        path: reference.file,
        line: reference.line,
        column: reference.column,
        referenceKind: reference.referenceKind
      },
      reference: {
        fieldName: reference.fieldName,
        matchedText: reference.matchedText,
        confidence: reference.confidence,
        possibleReference: reference.possibleReference,
        snippet: sanitizeSecretText(reference.snippet)
      },
      riskLevel,
      confidence: reference.confidence,
      suggestedEdit: buildSuggestedEdit(reference, change),
      dryRunDiff: buildDryRunDiff(reference, change),
      requiresHumanReview: true,
      reason: candidateReason(reference, change, riskLevel)
    }
  })
}

function primaryCandidateChangeType(change) {
  if (change.renameTo) {
    return CHANGE_TYPES.RENAME
  }
  if (change.typeChange) {
    return CHANGE_TYPES.TYPE_CHANGE
  }
  return CHANGE_TYPES.ENUM_CHANGE
}

function buildSuggestedEdit(reference, change) {
  if (change.renameTo) {
    return {
      strategy: 'TOKEN_RENAME',
      original: reference.matchedText,
      replacement: change.renameTo,
      description: `审查并考虑把 ${reference.matchedText} 替换为 ${change.renameTo}。`
    }
  }
  if (change.typeChange) {
    return {
      strategy: 'MANUAL_TYPE_REVIEW',
      original: reference.matchedText,
      replacement: null,
      fromType: change.typeChange.fromType,
      toType: change.typeChange.toType,
      description: `审查 ${reference.matchedText} 的类型映射、迁移、序列化和测试。`
    }
  }
  return {
    strategy: 'MANUAL_ENUM_REVIEW',
    original: reference.matchedText,
    replacement: null,
    enumChanges: change.enumChanges,
    description: `审查 ${reference.matchedText} 相关枚举常量、约束、fixture 和测试。`
  }
}

function buildDryRunDiff(reference, change) {
  if (!change.renameTo) {
    return null
  }
  const original = sanitizeSecretText(reference.snippet ?? reference.matchedText)
  const replacement = sanitizeSecretText(replaceToken(original, reference.matchedText, change.renameTo))
  return [
    `--- a/${reference.file}`,
    `+++ b/${reference.file}`,
    `@@ ${reference.line},${reference.column} @@`,
    `-${original}`,
    `+${replacement}`
  ].join('\n')
}

function buildManualSteps(references, change) {
  const fileRefs = uniqueFileRefs(references)
  const steps = []
  if (change.typeChange) {
    steps.push({
      id: 'manual-type-review',
      changeType: CHANGE_TYPES.TYPE_CHANGE,
      riskLevel: fileRefs.length > 0 ? 'MEDIUM' : 'LOW',
      title: 'Review field type change',
      description: `确认 ${change.fieldName} 的类型从 ${change.typeChange.fromType ?? '<unknown>'} 调整为 ${change.typeChange.toType ?? '<unknown>'} 后，业务代码、数据库迁移和测试是否一致。`,
      fileRefs,
      checklist: [
        '检查实体、DTO、mapper、schema 和 JSON fixture 中的字段类型。',
        '检查 SQL 迁移、DDL、索引、默认值和数据兼容处理。',
        '运行业务仓库单元测试、SQL lint 或迁移 dry-run。'
      ]
    })
  }
  if (change.enumChanges.length > 0) {
    steps.push({
      id: 'manual-enum-review',
      changeType: CHANGE_TYPES.ENUM_CHANGE,
      riskLevel: fileRefs.length > 0 ? 'MEDIUM' : 'LOW',
      title: 'Review enum value change',
      description: `确认 ${change.fieldName} 的枚举映射 ${change.enumChanges.map((item) => `${item.from}=${item.to}`).join(', ')} 是否已同步到代码和测试。`,
      fileRefs,
      checklist: [
        '检查枚举常量、校验规则、SQL CHECK 约束和配置字典。',
        '检查测试数据、JSON fixture、文档样例和向后兼容处理。',
        '确认旧枚举值是否需要数据迁移或双读兼容。'
      ]
    })
  }
  return steps
}

function summarizePlanRisk(renameRisk, candidateEdits, manualSteps) {
  if (candidateEdits.some((item) => item.riskLevel === 'HIGH') || renameRisk === 'HIGH') {
    return 'HIGH'
  }
  if (candidateEdits.some((item) => item.riskLevel === 'MEDIUM') || manualSteps.some((item) => item.riskLevel === 'MEDIUM') || renameRisk === 'MEDIUM') {
    return 'MEDIUM'
  }
  return 'LOW'
}

function buildVerificationCommands(change, scanPaths) {
  const planCommand = [
    'node tools/dataspec-cli.mjs code-patch plan',
    `--field ${formatCliArgument(change.fieldName)}`,
    change.renameTo ? `--to-field ${formatCliArgument(change.renameTo)}` : null,
    change.typeChange?.fromType ? `--from-type ${formatCliArgument(change.typeChange.fromType)}` : null,
    change.typeChange?.toType ? `--to-type ${formatCliArgument(change.typeChange.toType)}` : null,
    ...change.enumChanges.map((item) => `--enum-change ${formatCliArgument(`${item.from}=${item.to}`)}`),
    ...change.aliases.map((alias) => `--alias ${formatCliArgument(alias)}`),
    ...scanPaths.map((item) => `--path ${formatCliArgument(item)}`),
    '--format json'
  ].filter(Boolean).join(' ')
  const refsCommand = [
    'node tools/dataspec-cli.mjs index-refs',
    `--field ${formatCliArgument(change.fieldName)}`,
    ...change.aliases.map((alias) => `--alias ${formatCliArgument(alias)}`),
    ...scanPaths.map((item) => `--path ${formatCliArgument(item)}`),
    '--format json'
  ].join(' ')
  return [
    {
      command: sanitizeSecretText(planCommand),
      reason: '重新生成 Patch Plan，确认候选修改和风险仍匹配当前业务仓库。'
    },
    {
      command: sanitizeSecretText(refsCommand),
      reason: '人工修改后重新扫描字段引用，确认旧字段或旧枚举相关引用是否仍存在。'
    },
    {
      command: '<run-business-tests-or-sql-checks>',
      reason: '运行业务仓库已有单元测试、迁移 dry-run、SQL lint 或 CI 验证。'
    }
  ]
}

function buildDiagnostics(referenceIndex, change) {
  const diagnostics = [...(referenceIndex.diagnostics ?? [])]
  if (referenceIndex.references.length === 0) {
    diagnostics.push({
      code: 'CODE_PATCH_NO_REFERENCES',
      severity: 'INFO',
      message: '配置扫描范围内未发现字段引用；请确认 defaultPaths 或 --path 覆盖了业务 SQL、模型、配置和测试目录。'
    })
  }
  if (change.typeChange || change.enumChanges.length > 0) {
    diagnostics.push({
      code: 'CODE_PATCH_MANUAL_REVIEW_REQUIRED',
      severity: 'WARNING',
      message: '类型变化或枚举变化无法仅靠文本替换安全完成，计划已输出人工确认步骤。'
    })
  }
  return diagnostics.map((item) => ({
    ...item,
    message: sanitizeSecretText(item.message)
  }))
}

function buildNextActions(riskLevel, candidateEdits, manualSteps) {
  if (candidateEdits.length === 0 && manualSteps.length === 0) {
    return [
      '确认扫描路径覆盖了业务 SQL、迁移、模型、配置和测试目录。',
      '如仍要调整标准字段，先运行项目测试或 SQL 检查确认没有遗漏动态引用。'
    ]
  }
  const actions = [
    '按 riskLevel 从高到低审查 candidateEdits。',
    '把人工确认点拆成业务仓库 OpenSpec 或代码任务后再修改文件。',
    '修改后运行 verificationCommands 并保留证据。'
  ]
  if (riskLevel === 'HIGH') {
    actions.unshift('发现高风险引用，禁止让 AI 自动批量改写业务文件。')
  }
  return actions
}

function candidateReason(reference, change, riskLevel) {
  if (change.renameTo) {
    return `${reference.referenceKind} ${reference.confidence} 引用命中字段 token，重命名风险为 ${riskLevel}。`
  }
  if (change.typeChange) {
    return `${reference.referenceKind} ${reference.confidence} 引用可能依赖字段类型，需要人工审查。`
  }
  return `${reference.referenceKind} ${reference.confidence} 引用可能依赖字段枚举值，需要人工审查。`
}

function riskFromReference(reference) {
  if (reference.confidence === 'HIGH') {
    return 'HIGH'
  }
  if (reference.confidence === 'MEDIUM') {
    return 'MEDIUM'
  }
  return 'LOW'
}

function uniqueFileRefs(references) {
  const seen = new Set()
  const fileRefs = []
  for (const reference of references) {
    if (seen.has(reference.file)) {
      continue
    }
    seen.add(reference.file)
    fileRefs.push({
      path: reference.file,
      referenceKind: reference.referenceKind,
      confidence: reference.confidence
    })
  }
  return fileRefs
}

function replaceToken(value, original, replacement) {
  return String(value).replace(new RegExp(escapeRegExp(original), 'g'), replacement)
}

function formatCliArgument(value) {
  const text = String(value)
  return /^[A-Za-z0-9_./:=@-]+$/.test(text)
    ? text
    : JSON.stringify(text)
}

function sanitizePlanOutput(value) {
  if (typeof value === 'string') {
    return sanitizeSecretText(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizePlanOutput(item))
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, sanitizePlanOutput(item)])
    )
  }
  return value
}

function diagnosticError(code, message, suggestedAction) {
  const error = new Error(sanitizeSecretText(message))
  error.diagnostic = {
    code,
    category: 'VALIDATION',
    severity: 'ERROR',
    retryable: true,
    suggestedAction: sanitizeSecretText(suggestedAction)
  }
  return error
}

function normalizeRequiredText(value, message) {
  const normalized = normalizeText(value)
  if (!normalized) {
    throw new Error(message)
  }
  return normalized
}

function normalizeText(value) {
  return String(value ?? '').trim()
}

function uniqueSorted(items) {
  return [...new Set(items)].sort((left, right) => left.localeCompare(right))
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
