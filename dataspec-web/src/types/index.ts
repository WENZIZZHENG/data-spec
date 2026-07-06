import type { components } from '@/api/schema'

type Schemas = components['schemas']

export type ErrorDetail = Schemas['ErrorDetail']
export type Project = Schemas['Project']
export type CreateProjectReq = Schemas['CreateProjectReq']
export type DemoProjectResult = Schemas['DemoProjectResult']
export type Field = Schemas['Field']
export type FieldReq = Schemas['FieldReq']
export type FieldSuggestion = Schemas['FieldSuggestion']
export interface FieldSearchReq {
  projectId?: number
  query?: string
  category?: string
  tag?: string
  status?: string
  sensitive?: boolean
  sourceBatchId?: number
  limit?: number
}
export type FieldSearchItem = Schemas['FieldSearchItem']
export type FieldSearchSummary = Schemas['FieldSearchSummary']
export type FieldSearchResult = Schemas['FieldSearchResult']
export type Domain = Schemas['Domain']
export type FieldGroupItem = Schemas['FieldGroupItem']
export type FieldGroupSummary = Schemas['FieldGroupSummary']
export type FieldGroupingBatchUpdateReq = Omit<Schemas['FieldGroupingBatchUpdateReq'], 'updates'> & {
  updates: {
    domainId?: number | string | null
    category?: string | null
    tags?: string | null
  }
}
export type FieldGroupingBatchUpdateResult = Schemas['FieldGroupingBatchUpdateResult']

export type FieldBulkUpdateKey =
  | 'status'
  | 'category'
  | 'tags'
  | 'sensitive'
  | 'codeSetId'
  | 'aliases'

export interface FieldBulkUpdateChange {
  attribute?: FieldBulkUpdateKey | string
  beforeValue?: unknown
  afterValue?: unknown
}

export interface FieldBulkUpdateItem {
  fieldId?: number
  fieldName?: string
  changed?: boolean
  changes?: FieldBulkUpdateChange[]
}

export interface FieldBulkUpdatePreview {
  projectId?: number
  requestedCount?: number
  changedCount?: number
  unchangedCount?: number
  items?: FieldBulkUpdateItem[]
}

export interface FieldBulkUpdateReq {
  projectId?: number
  fieldIds?: number[]
  updates?: Partial<Record<FieldBulkUpdateKey, string | number | boolean | null>>
}

export interface FieldBulkUpdateResult {
  projectId?: number
  requestedCount?: number
  updatedCount?: number
  unchangedCount?: number
}

export interface FieldChangeUndoResult {
  projectId?: number
  fieldId?: number
  logId?: number
}

/** 标准字段合并预览请求，只在同一项目内比较一个保留字段和一个来源字段。 */
export interface StandardFieldMergePreviewReq {
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** 保留字段 ID。 */
  targetFieldId?: number
  /** 来源字段 ID。 */
  sourceFieldId?: number
}

/** 标准字段合并确认请求，reason 会写入来源字段替代说明。 */
export interface StandardFieldMergeApplyReq extends StandardFieldMergePreviewReq {
  /** 用户确认合并的业务原因，前端会先阻止空值提交。 */
  reason?: string
}

/** 标准字段合并向导中的字段选项。 */
export interface StandardFieldMergeOption {
  /** 字段 ID。 */
  fieldId?: number
  /** 标准字段名。 */
  name?: string
  /** 显示名。 */
  displayName?: string
  /** 数据类型。 */
  dataType?: string
  /** 生命周期状态。 */
  status?: string
}

/** 标准字段合并响应中的字段摘要，不包含源库业务行值。 */
export interface StandardFieldMergeFieldSummary {
  /** 字段 ID。 */
  id?: number
  /** 标准字段名。 */
  name?: string
  /** 显示名。 */
  displayName?: string
  /** 数据类型。 */
  dataType?: string
  /** 是否允许空值。 */
  nullable?: boolean
  /** 代码集 ID。 */
  codeSetId?: number | null
  /** 是否敏感字段。 */
  sensitive?: boolean
  /** 生命周期状态。 */
  status?: string
  /** 替代字段 ID。 */
  replacementFieldId?: number | null
  /** 替代说明。 */
  replacementReason?: string | null
  /** 合并后的别名列表。 */
  aliases?: string[]
  /** 合并后的标签列表。 */
  tags?: string[]
  /** 脱敏后的示例值。 */
  exampleValue?: string | null
  /** 格式约束摘要。 */
  formatNotes?: string[]
  /** 来源摘要，只包含表列名称。 */
  sourceSummaries?: string[]
}

/** 标准字段合并预览中的字段级变化。 */
export interface StandardFieldMergeChange {
  /** 受影响属性名。 */
  attribute?: string
  /** 变更前值。 */
  beforeValue?: unknown
  /** 变更后值或建议值。 */
  afterValue?: unknown
  /** 迁移模式，如 SAFE_MERGE、MANUAL_REVIEW。 */
  migrationMode?: string
  /** 变化说明。 */
  description?: string
}

/** 标准字段合并风险。 */
export interface StandardFieldMergeRisk {
  /** 风险级别。 */
  severity?: 'ERROR' | 'WARNING' | 'INFO' | string
  /** 稳定风险码。 */
  code?: string
  /** 风险说明。 */
  message?: string
  /** true 表示阻断 apply。 */
  blocking?: boolean
  /** 人工处理建议。 */
  manualAction?: string
}

/** 标准字段合并影响摘要。 */
export interface StandardFieldMergeImpact {
  /** 影响类型。 */
  impactType?: string
  /** 影响对象 ID。 */
  sourceId?: number | null
  /** 影响标题。 */
  title?: string
  /** 影响数量。 */
  count?: number
  /** 影响说明。 */
  description?: string
  /** 结构化补充信息，不包含凭据或源库行值。 */
  metadata?: Record<string, unknown>
}

/** 标准字段合并回退提示。 */
export interface StandardFieldMergeRollbackHint {
  /** 回退提示类型。 */
  type?: string
  /** 建议动作。 */
  action?: string
  /** 回退说明。 */
  description?: string
  /** API 或页面路径。 */
  targetPath?: string
}

/** 标准字段合并预览响应。 */
export interface StandardFieldMergePreview {
  /** 响应类型标识。 */
  kind?: string
  /** 响应 schema 版本。 */
  schemaVersion?: number
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** 推荐保留字段 ID。 */
  recommendedTargetFieldId?: number
  /** 保留字段当前摘要。 */
  target?: StandardFieldMergeFieldSummary
  /** 来源字段当前摘要。 */
  source?: StandardFieldMergeFieldSummary
  /** 应用后的保留字段摘要。 */
  targetAfter?: StandardFieldMergeFieldSummary
  /** 应用后的来源字段摘要。 */
  sourceAfter?: StandardFieldMergeFieldSummary
  /** 字段级变化。 */
  changes?: StandardFieldMergeChange[]
  /** 风险列表。 */
  risks?: StandardFieldMergeRisk[]
  /** 影响对象。 */
  impactItems?: StandardFieldMergeImpact[]
  /** 回退提示。 */
  rollbackHints?: StandardFieldMergeRollbackHint[]
  /** 下一步建议。 */
  nextActions?: string[]
}

/** 标准字段合并确认结果。 */
export interface StandardFieldMergeResult {
  /** 响应类型标识。 */
  kind?: string
  /** 响应 schema 版本。 */
  schemaVersion?: number
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** true 表示服务端已写入字段库。 */
  applied?: boolean
  /** 本次应用采用的预览。 */
  preview?: StandardFieldMergePreview
  /** 回退提示。 */
  rollbackHints?: StandardFieldMergeRollbackHint[]
  /** 下一步建议。 */
  nextActions?: string[]
}

export interface BusinessGlossary {
  id?: number
  projectId?: number
  term?: string
  synonyms?: string | null
  rootTerms?: string | null
  abbreviations?: string | null
  disabledTerms?: string | null
  canonicalFieldId?: number | null
  scopeType?: string | null
  scopeValue?: string | null
  exampleFields?: string | null
  description?: string | null
  status?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface BusinessGlossaryReq {
  projectId?: number
  term?: string
  synonyms?: string | null
  rootTerms?: string | null
  abbreviations?: string | null
  disabledTerms?: string | null
  canonicalFieldId?: number | null
  scopeType?: string | null
  scopeValue?: string | null
  exampleFields?: string | null
  description?: string | null
  status?: string | null
}

export interface BusinessGlossaryConflictEntry {
  id?: number
  term?: string
  canonicalFieldId?: number
  canonicalFieldName?: string
}

export interface BusinessGlossaryConflictGroup {
  type?: string
  severity?: 'ERROR' | 'WARNING'
  token?: string
  message?: string
  entries?: BusinessGlossaryConflictEntry[]
  nextAction?: string
}

export interface BusinessGlossaryConflictSummary {
  conflictCount?: number
  errorCount?: number
  warningCount?: number
}

export interface BusinessGlossaryConflictReport {
  projectId?: number
  summary?: BusinessGlossaryConflictSummary
  conflicts?: BusinessGlossaryConflictGroup[]
}

export type StandardChangeLog = Schemas['StandardChangeLog']
export type BackupProject = Schemas['BackupProject']
export type ProjectBackupAssets = Schemas['ProjectBackupAssets']
export type ProjectBackupCounts = Schemas['ProjectBackupCounts']
export type ProjectBackupSanitization = Schemas['ProjectBackupSanitization']
export type ProjectBackupPackage = Schemas['ProjectBackupPackage']
export type ProjectRestoreReq = Schemas['ProjectRestoreReq']
export type ProjectRestoreCounts = Schemas['ProjectRestoreCounts']
export type ProjectRestoreItem = Schemas['ProjectRestoreItem']
export type ProjectRestorePlan = Schemas['ProjectRestorePlan']
export type ProjectRestoreRecord = Schemas['ProjectRestoreRecord']
export type ProjectRestoreResult = Schemas['ProjectRestoreResult']

/** 标准复用包字段来源，用于 AI Context 和前端展示字段来自哪个共享包版本。 */
export interface StandardPackSource {
  /** 复用包 key。 */
  packKey?: string
  /** 复用包版本。 */
  basePackVersion?: string
}

/** 标准复用包资产数量摘要。 */
export interface StandardReusePackAssetCounts {
  /** 数据域数量。 */
  domains?: number
  /** 标准字段数量。 */
  fields?: number
  /** 枚举字典数量。 */
  enums?: number
  /** 枚举值数量。 */
  enumValues?: number
  /** 规则配置数量。 */
  rules?: number
  /** 表模板数量。 */
  templates?: number
  /** 模板字段数量。 */
  templateFields?: number
}

/** 创建标准复用包请求，只从当前项目导出标准资产，不包含凭据或源库行值。 */
export interface StandardReusePackCreateReq {
  /** 源项目 ID。 */
  projectId?: number
  /** 项目内稳定包 key，如 shared_core。 */
  packKey?: string
  /** 用户可读包名称。 */
  packName?: string
  /** 用户定义共享包版本。 */
  basePackVersion?: string
  /** 包说明。 */
  description?: string | null
}

/** 标准复用包应用请求，第一版仅创建缺失资产，不覆盖本地资产。 */
export interface StandardReusePackApplyReq {
  /** 标准复用包 ID。 */
  packId?: number
  /** 目标项目 ID。 */
  targetProjectId?: number
  /** 覆盖开关，第一版仅用于报告覆盖项，不执行破坏性覆盖。 */
  overwrite?: boolean
}

/** 标准复用包列表摘要。 */
export interface StandardReusePackInfo {
  /** 标准复用包 ID。 */
  packId?: number
  /** 源项目 ID。 */
  projectId?: number
  /** 源项目名称快照。 */
  sourceProjectName?: string
  /** 复用包 key。 */
  packKey?: string
  /** 复用包名称。 */
  packName?: string
  /** 复用包版本。 */
  basePackVersion?: string
  /** 包说明。 */
  description?: string | null
  /** 复用包内容 hash。 */
  packageHash?: string
  /** 资产数量摘要。 */
  assetCounts?: StandardReusePackAssetCounts
  /** 创建时间。 */
  createdAt?: string
}

/** 标准复用包详情。 */
export interface StandardReusePackDetail {
  /** 复用包摘要。 */
  info?: StandardReusePackInfo
  /** 确定性 payload JSON。 */
  payloadJson?: string
}

/** 标准复用包应用计划计数。 */
export interface StandardReusePackPlanCounts {
  /** 将创建的顶层资产数量。 */
  created?: number
  /** 将跳过的顶层资产数量。 */
  skipped?: number
  /** 本地覆盖项数量。 */
  overridden?: number
  /** 漂移项数量。 */
  drifted?: number
  /** 阻塞项数量。 */
  blocked?: number
  /** 警告数量。 */
  warnings?: number
}

/** 标准复用包计划或漂移明细。 */
export interface StandardReusePackPlanItem {
  /** 资产类型，如 field、enum_dict、rule、template。 */
  assetType?: string
  /** 项目内自然键。 */
  key?: string
  /** 动作，如 CREATE、SKIP、DRIFTED、BLOCKED。 */
  action?: string
  /** 动作原因。 */
  reason?: string
}

/** 标准复用包漂移计数。 */
export interface StandardReusePackDriftCounts {
  /** 内容一致数量。 */
  matched?: number
  /** 目标项目缺失数量。 */
  missing?: number
  /** 本地覆盖数量。 */
  overridden?: number
  /** 内容漂移数量。 */
  drifted?: number
}

/** 目标项目相对某个标准复用包的漂移报告。 */
export interface StandardReusePackDriftReport {
  /** 漂移报告 schema 版本。 */
  schemaVersion?: number
  /** 标准复用包 ID。 */
  packId?: number
  /** 目标项目 ID。 */
  targetProjectId?: number
  /** 复用包 key。 */
  packKey?: string
  /** 复用包版本。 */
  basePackVersion?: string
  /** 漂移计数。 */
  counts?: StandardReusePackDriftCounts
  /** 漂移明细。 */
  items?: StandardReusePackPlanItem[]
}

/** 标准复用包应用预览计划。 */
export interface StandardReusePackPlan {
  /** 响应类型标识。 */
  kind?: string
  /** 响应 schema 版本。 */
  schemaVersion?: number
  /** 标准复用包 ID。 */
  packId?: number
  /** 目标项目 ID。 */
  targetProjectId?: number
  /** 复用包 key。 */
  packKey?: string
  /** 复用包版本。 */
  basePackVersion?: string
  /** 是否可确认应用。 */
  canApply?: boolean
  /** 动作计数。 */
  counts?: StandardReusePackPlanCounts
  /** 计划明细。 */
  items?: StandardReusePackPlanItem[]
  /** 非阻断警告。 */
  warnings?: string[]
  /** 漂移报告。 */
  driftReport?: StandardReusePackDriftReport
}

/** 标准复用包应用记录。 */
export interface StandardReusePackApplicationInfo {
  /** 应用记录 ID。 */
  applicationId?: number
  /** 目标项目 ID。 */
  projectId?: number
  /** 复用包 ID。 */
  packId?: number
  /** 复用包 key 快照。 */
  packKey?: string
  /** 复用包名称快照。 */
  packName?: string
  /** 复用包版本快照。 */
  basePackVersion?: string
  /** 复用包 hash 快照。 */
  packageHash?: string
  /** 源项目 ID 快照。 */
  sourceProjectId?: number
  /** 源项目名称快照。 */
  sourceProjectName?: string
  /** 创建资产计数。 */
  createdCounts?: StandardReusePackAssetCounts
  /** 跳过资产计数。 */
  skippedCounts?: StandardReusePackAssetCounts
  /** 漂移计数。 */
  driftCounts?: StandardReusePackDriftCounts
  /** 应用时间。 */
  appliedAt?: string
}

/** 标准复用包确认应用结果。 */
export interface StandardReusePackApplyResult {
  /** 实际应用计划。 */
  plan?: StandardReusePackPlan
  /** 落库后的应用摘要。 */
  application?: StandardReusePackApplicationInfo
}

export type FieldQualitySeverity = 'ERROR' | 'WARNING' | 'SUGGESTION'
export type FieldQualityLevel = 'GOOD' | 'WARNING' | 'POOR'

export interface FieldQualityIssue {
  code?: string
  severity?: FieldQualitySeverity
  message?: string
  suggestedAction?: string
  scorePenalty?: number
}

export interface FieldQualityItem {
  fieldId?: number
  name?: string
  displayName?: string
  dataType?: string
  status?: string
  sensitive?: boolean
  codeSetId?: number
  score?: number
  level?: FieldQualityLevel
  issues?: FieldQualityIssue[]
  suggestions?: string[]
}

export interface FieldQualitySummary {
  totalFieldCount?: number
  averageScore?: number
  goodCount?: number
  warningCount?: number
  poorCount?: number
  lowQualityCount?: number
  errorIssueCount?: number
  warningIssueCount?: number
  suggestionIssueCount?: number
}

export interface FieldQualityReport {
  summary?: FieldQualitySummary
  fields?: FieldQualityItem[]
}

export type FieldConflictSeverity = 'ERROR' | 'WARNING' | 'INFO'
export type FieldConflictType =
  | 'NAME_DUPLICATE'
  | 'ALIAS_CONFLICT'
  | 'DISPLAY_NAME_DUPLICATE'
  | 'SEMANTIC_DUPLICATE'
  | 'RESERVED_WORD'
  | 'DANGEROUS_SQL_NAME'
  | 'CASE_COLLISION'
  | 'AMBIGUOUS_ALIAS'

export interface FieldConflictField {
  fieldId?: number
  name?: string
  displayName?: string
  dataType?: string
  codeSetId?: number
  sensitive?: boolean
  status?: string
  aliases?: string[]
}

export interface FieldConflictGroup {
  groupKey?: string
  conflictType?: FieldConflictType
  severity?: FieldConflictSeverity
  title?: string
  description?: string
  fields?: FieldConflictField[]
  evidence?: string[]
  suggestedAction?: string
}

export interface FieldConflictSummary {
  totalFieldCount?: number
  conflictGroupCount?: number
  affectedFieldCount?: number
  errorCount?: number
  warningCount?: number
  infoCount?: number
  aliasConflictCount?: number
  semanticDuplicateCount?: number
  attributeMismatchCount?: number
}

export interface FieldConflictReport {
  projectId?: number
  summary?: FieldConflictSummary
  groups?: FieldConflictGroup[]
}

export type FieldImpactSeverity = 'HIGH' | 'WARNING' | 'INFO'
export type FieldImpactType = 'TEMPLATE' | 'IMPORT_SOURCE' | 'SQL_CHECK' | 'STANDARD_SNAPSHOT' | 'CODE_SET'

export interface FieldEditWarning {
  attribute?: string
  severity?: FieldImpactSeverity
  message?: string
}

export interface FieldImpactItem {
  impactType?: FieldImpactType
  severity?: FieldImpactSeverity
  sourceId?: number
  sourceName?: string
  count?: number
  possibleReference?: boolean
  description?: string
  metadata?: Record<string, unknown>
}

export interface FieldImpactSummary {
  totalImpactCount?: number
  templateImpactCount?: number
  importSourceImpactCount?: number
  sqlCheckImpactCount?: number
  snapshotImpactCount?: number
  codeSetImpactCount?: number
  warningCount?: number
}

export interface FieldImpactReport {
  projectId?: number
  fieldId?: number
  fieldName?: string
  displayName?: string
  summary?: FieldImpactSummary
  impacts?: FieldImpactItem[]
  editWarnings?: FieldEditWarning[]
}

export type RuleConfig = Schemas['RuleConfig']
export type RuleConfigReq = Schemas['RuleConfigReq']

export type StandardChangeRiskLevel = 'INFO' | 'WARNING' | 'HIGH'
export type StandardChangeTargetType = 'field' | 'rule' | string

export interface StandardChangePreviewChange {
  attribute?: string
  beforeValue?: unknown
  afterValue?: unknown
  riskLevel?: StandardChangeRiskLevel
  description?: string
}

export interface StandardChangePreviewImpact {
  impactType?: string
  severity?: StandardChangeRiskLevel | string
  sourceId?: number
  title?: string
  count?: number
  description?: string
  metadata?: Record<string, unknown>
}

export interface StandardChangeRollbackHint {
  type?: string
  action?: string
  description?: string
  targetPath?: string
}

export interface StandardChangePreview {
  projectId?: number
  targetType?: StandardChangeTargetType
  targetId?: number
  targetName?: string
  operation?: string
  riskLevel?: StandardChangeRiskLevel
  requiresConfirmation?: boolean
  summary?: string
  changes?: StandardChangePreviewChange[]
  impacts?: StandardChangePreviewImpact[]
  validationCommands?: string[]
  rollbackHints?: StandardChangeRollbackHint[]
  currentSnapshot?: StandardSnapshotInfo
}

export type FieldChangePreviewReq = FieldReq

export interface RuleChangePreviewReq {
  projectId?: number
  ruleName?: string
  severity?: string
  enabled?: boolean
  paramsJson?: string
}

export type RuleBaselineRule = Schemas['RuleBaselineRule']
export type RuleBaselineTemplate = Schemas['RuleBaselineTemplate']
export type RuleBaselineInfo = Schemas['RuleBaselineInfo']
export type RuleBaselinePackage = Schemas['RuleBaselinePackage']
export type RuleBaselineApplyReq = Schemas['RuleBaselineApplyReq']
export type RuleBaselineImportReq = Schemas['RuleBaselineImportReq']
export type RuleBaselineApplyResult = Schemas['RuleBaselineApplyResult']
export type StarterKitDomain = Schemas['StarterKitDomain']
export type StarterKitEnumValue = Schemas['StarterKitEnumValue']
export type StarterKitEnumDefinition = Schemas['StarterKitEnumDefinition']
export type StarterKitFieldDefinition = Schemas['StarterKitFieldDefinition']
export type StarterKitTemplateField = Schemas['StarterKitTemplateField']
export type StarterKitTemplateDefinition = Schemas['StarterKitTemplateDefinition']
export type StarterKitDefinition = Schemas['StarterKitDefinition']
export type StarterKitApplyCounts = Schemas['StarterKitApplyCounts']
export type StarterKitApplyReq = Schemas['StarterKitApplyReq']
export type StarterKitApplyResult = Schemas['StarterKitApplyResult']
export type StarterKitInstallationInfo = Schemas['StarterKitInstallationInfo']

export interface RuleExemption {
  id?: number
  projectId?: number
  ruleCode?: string
  tableName?: string | null
  columnName?: string | null
  reason?: string
  enabled?: boolean
  expiresAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface RuleExemptionReq {
  projectId?: number
  ruleCode?: string
  tableName?: string | null
  columnName?: string | null
  reason?: string
  expiresAt?: string | null
}

export type LintRequest = Schemas['LintRequest']
export type LintResult = Schemas['LintResult']
export type LintIssue = Schemas['LintIssue']
export type SqlLintDebugResult = Schemas['SqlLintDebugResult']
export type SqlRuleDebugTrace = Schemas['SqlRuleDebugTrace']
export type SqlRuleMatchTrace = Schemas['SqlRuleMatchTrace']
export type SqlRuleSourceRange = Schemas['SqlRuleSourceRange']
export type FixPolicy = Schemas['FixPolicy']
export type FixChange = Schemas['FixChange']
export type FixPlanSummary = Schemas['FixPlanSummary']
export type SqlCheckRecord = Schemas['SqlCheckRecord']
export type RecordDetail = Schemas['RecordDetail']
export type SqlCheckReplay = Schemas['SqlCheckReplay']
export type AiTaskContextScope = Schemas['AiTaskContextScope']
export type AiTaskRuleset = Schemas['AiTaskRuleset']
export type AiTaskOutputFormat = Schemas['AiTaskOutputFormat']
export type AiProfileDiagnostic = Schemas['AiProfileDiagnostic']
export type AiTaskProfile = Schemas['AiTaskProfile']
export type AiTaskProfileCatalog = Schemas['AiTaskProfileCatalog']
export type AiTaskProfileDetail = Schemas['AiTaskProfileDetail']
export type DeprecatedContractField = Schemas['DeprecatedContractField']
export type SchemaCompatibilityPolicy = Schemas['SchemaCompatibilityPolicy']
export type SchemaContractSummary = Schemas['SchemaContractSummary']
export type SchemaContract = Schemas['SchemaContract']
export type SchemaRegistryCatalog = Schemas['SchemaRegistryCatalog']
export type AiCapabilityCatalog = Schemas['AiCapabilityCatalog']
export type AiCapabilityDiagnostic = Schemas['AiCapabilityDiagnostic']
export type AiCapabilityEntry = Schemas['AiCapabilityEntry']
export type AiCapabilityExample = Schemas['AiCapabilityExample']
export type EvidenceSourceType = NonNullable<Schemas['AiEvidencePackageReq']['sourceType']>
export type AiEvidenceSource = Schemas['AiEvidenceSource']
export type AiEvidenceStandardSnapshot = Schemas['AiEvidenceStandardSnapshot']
export type AiEvidenceArtifact = Schemas['AiEvidenceArtifact']
export type AiEvidenceDiagnostic = Schemas['AiEvidenceDiagnostic']
export type AiEvidencePackage = Schemas['AiEvidencePackage']
export type AiEvidencePackageReq = Omit<Schemas['AiEvidencePackageReq'], 'payloadSummary'> & {
  payloadSummary?: Record<string, unknown>
}
export type AuthMe = Schemas['AuthMe']
export type DdlGenerateResult = Schemas['DdlGenerateResult']
export type DialectDiagnostic = Schemas['DialectDiagnostic']
export type Template = Schemas['Template']
export type TemplateField = Schemas['TemplateField']
export type TableDef = Schemas['TableDef']
export type ColumnDef = Schemas['ColumnDef']

export interface ApiTokenInfo {
  id?: number
  name?: string
  operatorName?: string
  allProjects?: boolean
  projectIds?: number[]
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
  disabledAt?: string
  lastUsedAt?: string
}

export interface ApiTokenCreateReq {
  name?: string
  operatorName?: string
  allProjects?: boolean
  projectIds?: number[]
}

export interface ApiTokenCreateResp {
  plainToken?: string
  token?: ApiTokenInfo
}

export interface StandardSnapshotInfo {
  snapshotId?: number
  projectId?: number
  specVersion?: string
  name?: string
  description?: string
  specHash?: string
  createdAt?: string
  versioned?: boolean
  source?: 'current' | 'snapshot' | 'unversioned' | string
}

export interface StandardSnapshotCreateReq {
  version?: string
  name?: string
  description?: string
}

export interface PageResult<T> {
  records?: T[]
  total?: number
  current?: number
  size?: number
  pages?: number
}

export type AiTaskResumeInfo = Schemas['AiTaskResumeInfo']
export type AiTaskRunListItem = Schemas['AiTaskRunListItem']
export type AiTaskRunDetail = Schemas['AiTaskRunDetail']
export type AiTaskStepStatus = Schemas['AiTaskStepStatus']
export type AiTaskPartialArtifact = Schemas['AiTaskPartialArtifact']

export interface AiJobRecord {
  id?: number
  projectId?: number
  jobType?: string
  title?: string
  inputSummary?: string
  promptVersion?: string
  status?: string
  inputPayloadJson?: string
  outputPayloadJson?: string
  sqlCheckRecordId?: number
  standardSnapshotId?: number
  standardSnapshotVersion?: string
  standardSnapshotHash?: string
  createdAt?: string
  updatedAt?: string
}

export type AiJobRecordListItem = Omit<AiJobRecord, 'inputPayloadJson' | 'outputPayloadJson'>

export interface AiJobRecordDetail {
  record?: AiJobRecord
  inputPayload?: unknown
  outputPayload?: unknown
  replayPayload?: unknown
  replayCommand?: string
}

export interface AiBatchSummary {
  totalItems?: number
  successItems?: number
  failedItems?: number
  errorCount?: number
  warningCount?: number
  suggestionCount?: number
  fixedSqlCount?: number
}

export interface AiBatchIssueRuleSummary {
  ruleCode?: string
  ruleName?: string
  count?: number
}

export interface AiBatchIssueSummary {
  errorCount?: number
  warningCount?: number
  suggestionCount?: number
  byRule?: AiBatchIssueRuleSummary[]
}

export interface AiBatchFixedSqlSummary {
  availableCount?: number
  changedCount?: number
}

export interface AiBatchEvidence {
  kind?: string
  name?: string
  value?: string
}

export interface AiBatchItemResult {
  itemName?: string
  filePath?: string
  status?: string
  errorCount?: number
  warningCount?: number
  suggestionCount?: number
  suppressedCount?: number
  fixedSqlAvailable?: boolean
  fixedSql?: string | null
  fixedSqlDiff?: string | null
  issues?: LintIssue[]
  dialectDiagnostics?: DialectDiagnostic[]
  sqlCheckRecordId?: number | null
  errorMessage?: string | null
}

export interface AiBatchDeliveryPackage {
  packageVersion?: string
  batchId?: string
  projectId?: number
  batchType?: string
  source?: string
  status?: string
  summary?: AiBatchSummary
  items?: AiBatchItemResult[]
  issueSummary?: AiBatchIssueSummary
  fixedSqlSummary?: AiBatchFixedSqlSummary
  unmanagedHints?: string[]
  evidence?: AiBatchEvidence[]
  taskRun?: AiTaskResumeInfo
  nextActions?: string[]
  createdAt?: string
}

export interface AiBatchRun {
  id?: number
  projectId?: number
  batchType?: string
  source?: string
  status?: string
  summaryJson?: string
  payloadJson?: string
  operatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface AiBatchRunListItem {
  id?: number
  projectId?: number
  batchType?: string
  source?: string
  status?: string
  summary?: AiBatchSummary
  operatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface AiBatchRunDetail {
  run?: AiBatchRun
  deliveryPackage?: AiBatchDeliveryPackage
}

export interface AiFeedbackEvidence {
  sourceKind?: string
  sourceId?: number | null
  description?: string
}

export interface AiFeedbackSignal {
  signalType?: string
  title?: string
  count?: number
  severity?: string
  evidence?: AiFeedbackEvidence[]
  suggestedAction?: string
  targetRoute?: string
}

export interface AiFeedbackAction {
  title?: string
  description?: string
  priority?: string
  targetRoute?: string
}

export interface AiFeedbackSummary {
  aiJobCount?: number
  sqlCheckCount?: number
  ruleExemptionCount?: number
  fieldSourceCount?: number
  fieldSignalCount?: number
  ruleSignalCount?: number
  fixedSqlAvailableCount?: number
  insufficientSuggestionHistory?: boolean
  recommendationHistoryNote?: string
}

export interface AiFeedbackSampleSize {
  aiJobRecords?: number
  sqlCheckRecords?: number
  ruleExemptions?: number
  fieldSources?: number
  fields?: number
}

export interface AiFeedbackReport {
  projectId?: number
  summary?: AiFeedbackSummary
  fieldSignals?: AiFeedbackSignal[]
  ruleSignals?: AiFeedbackSignal[]
  fixedSqlSignals?: AiFeedbackSignal[]
  unmanagedSignals?: AiFeedbackSignal[]
  nextActions?: AiFeedbackAction[]
  sampleSize?: AiFeedbackSampleSize
  generatedAt?: string
}

export type StandardHealthCoverageInput = Schemas['StandardHealthCoverageInput']
export type StandardHealthSnapshotCreateReq = Schemas['StandardHealthSnapshotCreateReq']
export type StandardHealthMetrics = Schemas['StandardHealthMetrics']
export type StandardHealthAction = Schemas['StandardHealthAction']
export type StandardHealthSnapshotView = Schemas['StandardHealthSnapshotView']
export type StandardHealthDelta = Schemas['StandardHealthDelta']
export type StandardHealthTrend = Schemas['StandardHealthTrend']
export type StandardHealthPlan = Schemas['StandardHealthPlan']
export type StandardQualityGateConfig = Schemas['StandardQualityGateConfig']
export type StandardQualityGateSaveReq = Schemas['StandardQualityGateSaveReq']
export type StandardQualityGateEvaluateReq = Schemas['StandardQualityGateEvaluateReq']
export type StandardQualityGateResult = Schemas['StandardQualityGateResult']
export type QualityGateCheckResult = Schemas['QualityGateCheckResult']
export type QualityGateSummary = Schemas['QualityGateSummary']
export type QualityGateLintSummary = Schemas['QualityGateLintSummary']
export type StandardUsageExample = Schemas['StandardUsageExample']
export type StandardUsageExampleSaveReq = Schemas['StandardUsageExampleSaveReq']

export interface StandardCandidate {
  id?: number
  projectId?: number
  candidateName?: string
  displayName?: string
  dataType?: string
  comment?: string
  sourceType?: string
  sourceRef?: string
  evidenceJson?: string
  confidence?: number
  status?: string
  targetFieldId?: number
  decisionReason?: string
  decidedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface StandardCandidateCreateReq {
  projectId: number
  candidateName: string
  displayName?: string
  dataType: string
  comment?: string
  sourceType: string
  sourceRef?: string
  evidenceJson?: string
  confidence?: number
}

export interface StandardCandidateDecisionReq {
  reason?: string
}

export interface StandardCandidateMergeReq {
  targetFieldId: number
  reason?: string
}

export interface RequirementDraftReq {
  projectId: number
  description: string
  targetTableName: string
  groupHint?: string
  limit?: number
}

export interface ExplainTrace {
  sourceType?: string
  sourceId?: number
  snapshotVersion?: string | null
  matchReason?: string
  confidence?: number
  ruleCode?: string | null
  docsRef?: string
}

export interface RequirementMatchedField {
  field?: Field
  score?: number
  matchReasons?: string[]
  recommended?: boolean
  evidence?: ExplainTrace[]
}

export interface RequirementMissingCandidate {
  candidateName?: string
  displayName?: string
  dataType?: string
  comment?: string
  evidence?: string
  confidence?: number
  inboxPayload?: StandardCandidateCreateReq
  evidenceTrace?: ExplainTrace[]
}

export interface RequirementAmbiguousCandidate {
  field?: Field
  score?: number
  matchReasons?: string[]
  evidence?: ExplainTrace[]
}

export interface RequirementAmbiguousTerm {
  term?: string
  reason?: string
  candidates?: RequirementAmbiguousCandidate[]
}

export interface RequirementRecommendedTemplate {
  id?: number
  name?: string
  description?: string
  tablePrefix?: string
  score?: number
  matchReasons?: string[]
  evidence?: ExplainTrace[]
}

export interface RequirementDraftResult {
  projectId?: number
  description?: string
  targetTableName?: string
  groupHint?: string
  matchedFields?: RequirementMatchedField[]
  missingCandidates?: RequirementMissingCandidate[]
  ambiguousTerms?: RequirementAmbiguousTerm[]
  recommendedTemplate?: RequirementRecommendedTemplate | null
  nextActions?: string[]
  copyablePrompt?: string
}

export interface R<T> {
  code?: number
  message?: string
  error?: ErrorDetail
  data?: T
}

export interface ExcelSheetSummary {
  total?: number
  createCount?: number
  updateCount?: number
  conflictCount?: number
}

export interface ExcelImportError {
  sheet?: string
  rowNumber?: number
  field?: string
  message?: string
}

export interface ExcelImportDiff {
  field?: string
  beforeValue?: string
  afterValue?: string
}

export interface ExcelImportPreviewItem {
  sheet?: string
  rowNumber?: number
  key?: string
  action?: 'CREATE' | 'UPDATE' | 'CONFLICT' | string
  status?: 'READY' | 'BLOCKED' | string
  reason?: string
  diffs?: ExcelImportDiff[]
}

export interface ExcelImportPreview {
  valid?: boolean
  fields?: ExcelSheetSummary
  enumDicts?: ExcelSheetSummary
  enumValues?: ExcelSheetSummary
  errors?: ExcelImportError[]
  items?: ExcelImportPreviewItem[]
}

export interface ExcelImportResult {
  success?: boolean
  importedFields?: number
  importedEnumDicts?: number
  importedEnumValues?: number
  errors?: ExcelImportError[]
}

export interface RecentSqlCheck {
  id?: number
  createdAt?: string
  errorCount?: number
  warningCount?: number
  suggestionCount?: number
  issueCount?: number
}

export interface IssueTrendPoint {
  recordId?: number
  createdAt?: string
  issueCount?: number
  errorCount?: number
  warningCount?: number
  suggestionCount?: number
}

export interface DashboardSummary {
  fieldCount?: number
  enumDictCount?: number
  ruleCount?: number
  forbiddenTermCount?: number
  recentCheckCount?: number
  fieldHitRate?: number | null
  recentChecks?: RecentSqlCheck[]
  trend?: IssueTrendPoint[]
}

export type ProjectActivitySeverity = 'INFO' | 'WARNING' | 'ERROR' | string
export type ProjectActivityAction = Schemas['ProjectActivityAction']
export type ProjectActivityItem = Omit<Schemas['ProjectActivityItem'], 'metadata'> & {
  metadata?: Record<string, unknown>
}
export type ProjectActivityTimeline = Schemas['ProjectActivityTimeline']

export interface ReverseImportSummary {
  tableCount?: number
  columnCount?: number
  candidateCount?: number
  missingCommentCount?: number
  nonStandardFieldCount?: number
}

export interface FieldCandidate {
  tableName?: string
  columnName?: string
  dataType?: string
  nullable?: boolean
  defaultValue?: string
  comment?: string
  decisionType?: string
  matchedFieldId?: number
  matchedFieldName?: string
  matchReason?: string
  confidence?: number
  ignoreReason?: string
  confirmReason?: string
  /** 字段候选所属预览批次的 dry-run evidence，确认导入或忽略时必须与请求 dryRunToken 一致。 */
  dryRunToken?: string
}

export type ReverseImportDecision = Schemas['ReverseImportDecision']

export interface MissingCommentIssue {
  tableName?: string
  columnName?: string
  targetType?: string
}

export interface NonStandardField {
  tableName?: string
  columnName?: string
  dataType?: string
  recommendedName?: string
  reason?: string
}

export interface ReverseImportPreview {
  summary?: ReverseImportSummary
  /** 本次预览生成的 dry-run evidence，确认导入字段候选时必须随请求带回。 */
  dryRunToken?: string
  /** 数据库直连预览关联的 metadata cache 证据；SQL 预览或旧响应可能为空。 */
  metadataCache?: DatabaseMetadataCacheInfo
  tables?: TableDef[]
  fieldCandidates?: FieldCandidate[]
  mappingDecisions?: ReverseImportDecision[]
  missingComments?: MissingCommentIssue[]
  nonStandardFields?: NonStandardField[]
  dialectDiagnostics?: DialectDiagnostic[]
}

export type ReverseImportFieldStatus =
  | 'MATCHED'
  | 'CHANGED'
  | 'NEW'
  | 'MISSING_COMMENT'
  | 'NON_STANDARD'

export interface ReverseImportCompareSummary {
  tableCount?: number
  columnCount?: number
  matchedCount?: number
  changedCount?: number
  newCount?: number
  missingCommentCount?: number
  nonStandardCount?: number
}

export interface ReverseImportFieldChange {
  property?: string
  currentValue?: string
  standardValue?: string
}

export interface ReverseImportFieldDiff {
  tableName?: string
  columnName?: string
  dataType?: string
  nullable?: boolean
  defaultValue?: string
  comment?: string
  standardFieldName?: string
  standardDisplayName?: string
  status?: ReverseImportFieldStatus
  reason?: string
  nonStandard?: boolean
  changes?: ReverseImportFieldChange[]
}

export interface ReverseImportTableDiff {
  tableName?: string
  comment?: string
  fieldDiffs?: ReverseImportFieldDiff[]
}

export interface ReverseImportCompareResult {
  summary?: ReverseImportCompareSummary
  /** 数据库直连差异结果关联的 metadata cache 证据，用于判断结构是否需要刷新。 */
  metadataCache?: DatabaseMetadataCacheInfo
  tableDiffs?: ReverseImportTableDiff[]
}

export type DatabaseMetadataCacheMode = 'AUTO' | 'REFRESH' | 'BYPASS' | (string & {})

export interface DatabaseConnectionReq {
  projectId?: number
  /** 连接预设 ID；存在时服务端以预设作为 metadata cache 来源边界。 */
  presetId?: number
  databaseType?: 'postgresql' | 'mysql'
  host?: string
  port?: number
  databaseName?: string
  schemaName?: string
  username?: string
  password?: string
  tableNames?: string[]
  /** metadata cache 策略：AUTO 默认复用新鲜缓存，REFRESH 强制刷新，BYPASS 绕过缓存。 */
  metadataCacheMode?: DatabaseMetadataCacheMode
}

export interface DatabaseConnectionPreset {
  id?: number
  projectId?: number
  name?: string
  databaseType?: 'postgresql' | 'mysql'
  host?: string
  port?: number
  databaseName?: string
  schemaName?: string | null
  tableNames?: string[]
  createdAt?: string
  updatedAt?: string
}

export interface DatabaseConnectionPresetReq {
  projectId?: number
  name?: string
  databaseType?: 'postgresql' | 'mysql'
  host?: string
  port?: number
  databaseName?: string
  schemaName?: string | null
  tableNames?: string[]
}

export type DatabaseConnectionResult = Schemas['DatabaseConnectionResult']
export type DatabaseConnectionSecurityDiagnostic = Schemas['DatabaseConnectionSecurityDiagnostic']
export type DatabaseConnectionHealthDiagnostic = Schemas['DatabaseConnectionHealthDiagnostic']
export type DatabaseDialectCapability = Schemas['DatabaseDialectCapability']

export interface DatabaseTableInfo {
  schemaName?: string
  tableName?: string
  tableType?: string
  comment?: string
}

/** 数据库 metadata 分页扫描请求；连接密码仅用于本次请求，不进入 cursor 或 scanId。 */
export interface DatabaseMetadataScanReq extends DatabaseConnectionReq {
  /** 一轮扫描标识；为空时由服务端生成，不代表持久后台任务。 */
  scanId?: string
  /** 短期分页游标，第一版使用已读取表数量偏移。 */
  cursor?: string
  /** 每页表数量，服务端限制在 1 到 100。 */
  pageSize?: number
  /** true 表示停止继续扫描，不写源库或标准库。 */
  cancel?: boolean
}

/** 数据库 metadata 分页扫描进度。 */
export interface DatabaseMetadataScanProgress {
  /** 已处理表数量。 */
  processedTableCount?: number
  /** 剩余表数量估算。 */
  remainingTableEstimate?: number
  /** 本次请求采用的分页大小。 */
  pageSize?: number
  /** 是否还有下一批。 */
  hasMore?: boolean
}

/** 当前扫描页的轻量汇总。 */
export interface DatabaseMetadataScanSummary {
  /** 当前页返回的表数量。 */
  pageTableCount?: number
  /** 用户已选择的表数量。 */
  selectedTableCount?: number
  /** 当前连接可见表数量估算。 */
  estimatedTableCount?: number
}

/** 数据库 metadata 分页扫描响应；只包含表级 metadata、进度和脱敏恢复信息。 */
export interface DatabaseMetadataScanResult {
  /** 响应类型标识。 */
  kind?: string
  /** 响应 schema 版本。 */
  schemaVersion?: number
  /** 当前 DataSpec 项目 ID。 */
  projectId?: number
  /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
  databaseType?: string
  /** 数据库名，服务端已做敏感信息清洗。 */
  databaseName?: string
  /** schema 名；MySQL 场景可能为空。 */
  schemaName?: string
  /** 一轮扫描标识；不承诺持久化生命周期。 */
  scanId?: string
  /** 当前连接可见表数量估算。 */
  estimatedTableCount?: number
  /** 下一批 cursor；为空表示无后续批次。 */
  cursor?: string | null
  /** 当前页表级 metadata，不包含列 metadata 或业务数据行。 */
  tables?: DatabaseTableInfo[]
  /** 当前扫描页和下一批状态。 */
  progress?: DatabaseMetadataScanProgress
  /** 当前扫描页轻量汇总。 */
  partialSummary?: DatabaseMetadataScanSummary
  /** 面向 AI 的脱敏恢复提示。 */
  resumeCommand?: string
  /** true 表示用户已请求取消，不应继续下一批。 */
  cancelled?: boolean
  /** 当前扫描页关联的 metadata cache 证据。 */
  metadataCache?: DatabaseMetadataCacheInfo
  /** 扫描后的建议动作，不代表自动写入。 */
  nextActions?: string[]
}

export interface DatabaseMetadataColumnChange {
  /** 发生变化的字段名。 */
  columnName?: string
  /** 字段属性变化列表；currentValue 为旧缓存值，standardValue 为刷新后的新值。 */
  changes?: ReverseImportFieldChange[]
}

export interface DatabaseMetadataTableChange {
  /** 表所在 schema；MySQL 场景可能为空。 */
  schemaName?: string
  /** 发生变化的表名。 */
  tableName?: string
  /** 表变化类型：ADDED/REMOVED/CHANGED/UNCHANGED。 */
  changeType?: string
  /** 旧缓存表结构 fingerprint；新增表可能为空。 */
  oldFingerprint?: string
  /** 刷新后表结构 fingerprint；删除表可能为空。 */
  newFingerprint?: string
  /** 新增字段名列表，仅来自 schema metadata。 */
  addedColumns?: string[]
  /** 删除字段名列表，仅来自 schema metadata。 */
  removedColumns?: string[]
  /** 字段属性变化列表。 */
  changedColumns?: DatabaseMetadataColumnChange[]
}

export interface DatabaseMetadataChangeSummary {
  /** true 表示本次刷新相对旧缓存存在结构变化。 */
  changed?: boolean
  /** 新增表数量。 */
  addedTableCount?: number
  /** 删除表数量。 */
  removedTableCount?: number
  /** 存在字段或索引属性变化的表数量。 */
  changedTableCount?: number
  /** 新增字段数量。 */
  addedColumnCount?: number
  /** 删除字段数量。 */
  removedColumnCount?: number
  /** 字段属性变化数量。 */
  changedColumnCount?: number
  /** 有界表级变化示例。 */
  tables?: DatabaseMetadataTableChange[]
}

export interface DatabaseMetadataCacheInfo {
  /** 聚合结构 fingerprint，供 AI 判断是否重跑反向导入或覆盖率。 */
  metadataFingerprint?: string
  /** true 表示本次结果完全来自新鲜缓存。 */
  cacheHit?: boolean
  /** true 表示曾发现缓存过期或缺失，需要重新读取源库。 */
  stale?: boolean
  /** 实际使用的刷新策略：AUTO/REFRESH/BYPASS。 */
  refreshMode?: string
  /** 本次结构快照最近一次读取源库 metadata 的时间。 */
  lastSeenAt?: string
  /** 当前缓存过期时间；为空表示本次未写入或未读取缓存。 */
  expiresAt?: string
  /** 源数据库产品和版本的脱敏摘要。 */
  sourceDatabaseVersion?: string
  /** 刷新时产生的结构变化摘要。 */
  changeSummary?: DatabaseMetadataChangeSummary
  /** 面向用户和 AI 的安全下一步提示，不包含凭据。 */
  nextActions?: string[]
}

export interface DatabaseSchemaIndex {
  schemaName?: string
  tableName?: string
  indexName?: string
  columnName?: string
  nonUnique?: boolean
  ordinalPosition?: number
}

export interface DatabaseMetadataBrowserSummary {
  tableCount?: number
  columnCount?: number
  indexCount?: number
  candidateCount?: number
  missingCommentCount?: number
  changedCount?: number
  unmanagedCount?: number
}

export interface DatabaseMetadataBrowserColumn {
  schemaName?: string
  tableName?: string
  columnName?: string
  dataType?: string
  nullable?: boolean
  defaultValue?: string
  comment?: string
  standardFieldName?: string
  standardDisplayName?: string
  matchStatus?: string
  matchReason?: string
  candidateKey?: string
  importCandidate?: boolean
  selectedByDefault?: boolean
  missingComment?: boolean
  typeChanged?: boolean
  unmanaged?: boolean
  indexNames?: string[]
  changes?: ReverseImportFieldChange[]
}

export interface DatabaseMetadataBrowserTable {
  schemaName?: string
  tableName?: string
  tableType?: string
  comment?: string
  columnCount?: number
  indexCount?: number
  candidateCount?: number
  missingCommentCount?: number
  changedCount?: number
  unmanagedCount?: number
  indexes?: DatabaseSchemaIndex[]
  columns?: DatabaseMetadataBrowserColumn[]
  warnings?: string[]
}

export interface DatabaseMetadataBrowser {
  kind?: string
  schemaVersion?: number
  projectId?: number
  databaseType?: string
  databaseName?: string
  schemaName?: string
  selectedTableNames?: string[]
  summary?: DatabaseMetadataBrowserSummary
  tables?: DatabaseMetadataBrowserTable[]
  aiReadableSummary?: string
  /** 元数据浏览结果关联的 cache 证据。 */
  metadataCache?: DatabaseMetadataCacheInfo
  nextActions?: string[]
  preview?: ReverseImportPreview
  compare?: ReverseImportCompareResult
  coverage?: FieldCoverageReport
}

export interface DatabaseImportResult {
  batchId?: number
  importedCount?: number
  skippedCount?: number
  importedFields?: string[]
  skippedFields?: string[]
  mappingDecisions?: ReverseImportDecision[]
}

export interface DatabaseImportSourceContext {
  databaseType?: string
  databaseName?: string
  schemaName?: string
  tableNames?: string[]
}

export interface ReverseImportBatch {
  id?: number
  projectId?: number
  sourceType?: string
  databaseType?: string
  databaseName?: string
  schemaName?: string
  tableNamesJson?: string
  importedCount?: number
  skippedCount?: number
  operatorName?: string
  createdAt?: string
}

export interface FieldSource {
  id?: number
  projectId?: number
  fieldId?: number
  batchId?: number
  sourceType?: string
  schemaName?: string
  tableName?: string
  columnName?: string
  dataType?: string
  nullable?: boolean
  defaultValue?: string
  comment?: string
  metadataJson?: string
  createdAt?: string
}

export interface FieldSourceDetail {
  source?: FieldSource
  batch?: ReverseImportBatch | null
}

export type FieldCoverageStatus =
  | 'STANDARD_MATCH'
  | 'ALIAS_MATCH'
  | 'MISSING_COMMENT'
  | 'POSSIBLE_DUPLICATE'
  | 'UNMANAGED'

export interface FieldCoverageSummary {
  tableCount?: number
  columnCount?: number
  coveredCount?: number
  unmanagedCount?: number
  missingCommentCount?: number
  possibleDuplicateCount?: number
  coverageRate?: number
}

export interface FieldCoverageItem {
  tableName?: string
  columnName?: string
  dataType?: string
  comment?: string
  status?: FieldCoverageStatus
  covered?: boolean
  standardFieldName?: string
  standardDisplayName?: string
  matchType?: string
  recommendedFieldName?: string
  reason?: string
}

export interface FieldCoverageTable {
  tableName?: string
  comment?: string
  columnCount?: number
  coveredCount?: number
  unmanagedCount?: number
  missingCommentCount?: number
  possibleDuplicateCount?: number
  coverageRate?: number
  fields?: FieldCoverageItem[]
}

export interface UnmanagedFieldRanking {
  columnName?: string
  count?: number
  tables?: string[]
  recommendedFieldName?: string
  reason?: string
}

export interface FieldCoverageReport {
  summary?: FieldCoverageSummary
  /** 覆盖率报告关联的 metadata cache 证据，用于判断报告所基于的结构版本。 */
  metadataCache?: DatabaseMetadataCacheInfo
  tables?: FieldCoverageTable[]
  unmanagedRankings?: UnmanagedFieldRanking[]
}
