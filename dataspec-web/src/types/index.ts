import type { components } from '@/api/schema'

type Schemas = components['schemas']

export type ErrorDetail = Schemas['ErrorDetail']
export type Project = Schemas['Project']
export type CreateProjectReq = Schemas['CreateProjectReq']
export type DemoProjectResult = Schemas['DemoProjectResult']
export type Field = Schemas['Field']
export type FieldReq = Schemas['FieldReq']
export type FieldSuggestion = Schemas['FieldSuggestion']

/** 后端统一生成的查询 token evidence；所有文本已脱敏且长度有界。 */
export type QueryTokenEvidence = Schemas['QueryTokenEvidence']

/** 确定性命名 token 的词法类型；仅描述边界，不代表业务分类。 */
export type QueryTokenKind = NonNullable<QueryTokenEvidence['tokenKind']>

/** 当前项目 glossary 对查询 token 的保守解析状态。 */
export type QueryTokenResolutionStatus = NonNullable<QueryTokenEvidence['resolutionStatus']>
export type FieldKnowledgeCardListResp = Schemas['FieldKnowledgeCardListResp']
export type FieldKnowledgeCardResp = Schemas['FieldKnowledgeCardResp']
export type FieldSemanticRuleReq = Schemas['FieldSemanticRuleReq']
export type FieldSemanticRuleResp = Schemas['FieldSemanticRuleResp']
export type MetricDefinitionReq = Schemas['MetricDefinitionReq']
export type MetricDefinitionResp = Schemas['MetricDefinitionResp']
export type EnumDict = Schemas['EnumDict']
export type EnumDictReq = Schemas['EnumDictReq']
export type EnumValue = Schemas['EnumValue']
export type EnumValueReq = Schemas['EnumValueReq']

/** Standard Query DSL v1 支持的查询目标；当前前端只暴露 FIELD 只读查询。 */
export type StandardQueryTarget = 'FIELD'

/** Standard Query DSL v1 支持的字段过滤名，均为服务端 allowlist 字段。 */
export type StandardQueryFilterField =
  | 'category'
  | 'tag'
  | 'status'
  | 'sensitive'
  | 'sourceBatchId'
  | 'stableRef'
  | 'canonicalRef'
  | 'hasExample'
  | 'updatedSince'

/** Standard Query DSL v1 支持的过滤操作符；未传时服务端按字段默认语义处理。 */
export type StandardQueryFilterOp = 'eq' | 'contains' | 'gte'

/** Standard Query DSL 的单个过滤条件；value 会被服务端视为敏感输入并只输出脱敏摘要。 */
export interface StandardQueryFilter {
  /** allowlist 字段名；前端 v1 只生成 FIELD 目标支持的字段。 */
  field?: StandardQueryFilterField | string
  /** allowlist 操作符；为空时由服务端按字段默认语义解析。 */
  op?: StandardQueryFilterOp | string
  /** 过滤值；不得在前端日志或 UI 中原样作为 secret 展示。 */
  value?: string | number | boolean | string[] | number[] | boolean[] | null
}

/** Standard Query DSL 只读请求；v1 target 固定为 FIELD，不触发标准、业务文件或数据库写入。 */
export interface StandardQueryRequest {
  /** 当前 DataSpec 项目 ID；服务端会按项目隔离查询。 */
  projectId?: number
  /** 标准对象目标；第一版仅支持 FIELD。 */
  target?: StandardQueryTarget
  /** 自然语言或字段名检索文本；服务端返回摘要时会脱敏。 */
  text?: string
  /** allowlist 过滤条件；不支持项在非 strict 模式会进入 ignoredFilters。 */
  filters?: StandardQueryFilter[]
  /** 预留排序字段；v1 服务端保持字段搜索既有排序。 */
  sort?: string[]
  /** 返回上限；v1 允许 1 到 50，和字段搜索实际执行上限一致。 */
  limit?: number
  /** 是否请求解释信息；v1 服务端始终返回可解释摘要。 */
  explain?: boolean
  /** 严格模式；true 时不支持的 target/filter/op/value 会在执行前失败。 */
  strict?: boolean
}

/** Standard Query DSL 已应用过滤摘要；所有值已由服务端脱敏，可用于页面 summary。 */
export interface StandardQueryAppliedFilter {
  /** 生效的 allowlist 字段名。 */
  field?: string
  /** 生效操作符。 */
  op?: string
  /** 脱敏后的过滤值。 */
  redactedValue?: string
  /** 面向用户和 AI 的过滤语义说明。 */
  description?: string
}

/** Standard Query DSL 被忽略过滤摘要；用于解释非 strict 模式的降级。 */
export interface StandardQueryIgnoredFilter {
  /** 原始过滤字段名。 */
  field?: string
  /** 原始操作符。 */
  op?: string
  /** 脱敏后的原始值。 */
  redactedValue?: string
  /** 脱敏后的忽略原因。 */
  reason?: string
}

/** Standard Query DSL 归一化结果；不包含 raw secret，可作为只读查询摘要复用。 */
export interface StandardQueryNormalized {
  /** 归一化目标类型；v1 为 FIELD。 */
  target?: StandardQueryTarget | string
  /** 归一化并脱敏后的检索文本。 */
  text?: string
  /** 已应用的过滤条件摘要。 */
  filters?: StandardQueryAppliedFilter[]
  /** 已接受的排序字段；v1 通常为空。 */
  sort?: string[]
  /** 生效返回上限。 */
  limit?: number
  /** 是否返回解释信息。 */
  explain?: boolean
  /** 是否严格校验。 */
  strict?: boolean
  /** FIELD text 的确定性 token evidence；explain=false 时可为空。 */
  queryTokens?: QueryTokenEvidence[]
}

/** Standard Query DSL 执行摘要；服务端保证文本、建议和过滤值为 secret-safe summary。 */
export interface StandardQuerySummary {
  /** 查询目标类型。 */
  target?: StandardQueryTarget | string
  /** 脱敏后的检索文本。 */
  text?: string
  /** 过滤和检索后命中的总数。 */
  resultCount?: number
  /** 本次返回条数。 */
  returnedCount?: number
  /** 是否因 limit 截断。 */
  truncated?: boolean
  /** 下一步收窄、修正或改写查询的建议；不得包含 raw secret。 */
  nextQueryHints?: string[]
}

/** Standard Query DSL 只读查询结果；target=FIELD 时 fields 返回字段标准命中项。 */
export interface StandardQueryResult {
  /** 当前项目 ID。 */
  projectId?: number
  /** 已归一化且脱敏的查询表达。 */
  normalizedQuery?: StandardQueryNormalized
  /** 查询执行摘要。 */
  querySummary?: StandardQuerySummary
  /** 已应用过滤条件。 */
  appliedFilters?: StandardQueryAppliedFilter[]
  /** 被忽略过滤条件。 */
  ignoredFilters?: StandardQueryIgnoredFilter[]
  /** 命中总数。 */
  resultCount?: number
  /** 返回条数。 */
  returnedCount?: number
  /** 是否截断。 */
  truncated?: boolean
  /** 下一步查询建议。 */
  nextQueryHints?: string[]
  /** 字段标准命中项；v1 target=FIELD 时返回。 */
  fields?: FieldSearchItem[]
}

export interface FieldSearchReq {
  /** 字段所属项目 ID。 */
  projectId?: number
  /** 字段名、显示名、别名或业务语义关键词。 */
  query?: string
  /** 字段分类精确过滤条件。 */
  category?: string
  /** 字段标签精确过滤条件。 */
  tag?: string
  /** 字段生命周期状态过滤条件。 */
  status?: string
  /** 是否敏感字段的精确过滤条件。 */
  sensitive?: boolean
  /** 反向导入来源批次 ID。 */
  sourceBatchId?: number
  /** legacy 首批返回上限；未提供 current/size 时生效。 */
  limit?: number
  /** 服务端页码，从 1 开始；与 size 任一存在即启用分页模式。 */
  current?: number
  /** 服务端页大小，范围 1-100。 */
  size?: number
  /** 字段数据域 ID 精确过滤条件。 */
  domainId?: number
  /** true 仅返回未归组字段，false 仅返回已归组字段。 */
  ungrouped?: boolean
  /** true 表示包含全部生命周期状态；省略时保留 legacy 的 enabled 默认过滤。 */
  includeAllStatuses?: boolean
}
export type FieldSearchItem = Schemas['FieldSearchItem']
/** 字段搜索服务端分页元数据；legacy limit-only 调用可为空。 */
export type FieldSearchPage = Schemas['FieldSearchPage']
export type FieldSearchSummary = Schemas['FieldSearchSummary'] & {
  /** 字段搜索映射到 Standard Query DSL 后的脱敏查询摘要；additive 字段，不改变旧字段语义。 */
  querySummary?: StandardQuerySummary
  /** 字段搜索对应的 DSL 已应用过滤条件；值已脱敏。 */
  dslAppliedFilters?: StandardQueryAppliedFilter[]
  /** 字段搜索对应的 DSL 忽略过滤条件；legacy 搜索通常为空。 */
  dslIgnoredFilters?: StandardQueryIgnoredFilter[]
  /** 字段搜索对应的 DSL 下一步查询建议；等价于 querySummary.nextQueryHints。 */
  nextQueryHints?: string[]
  /** 字段搜索使用的确定性 token evidence；文本已脱敏且数量有界。 */
  queryTokens?: QueryTokenEvidence[]
}
export type FieldSearchResult = Omit<Schemas['FieldSearchResult'], 'summary' | 'page'> & {
  /** 字段搜索摘要；在生成 Schema 更新前兼容 additive queryTokens。 */
  summary?: FieldSearchSummary
  /** 服务端分页元数据；legacy limit-only 调用可为空。 */
  page?: FieldSearchPage | null
}
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

/** 标准维护 workflow dry-run 计划来源请求。 */
export interface StandardMaintenanceWorkflowPlanReq {
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** 维护来源类型，例如 STANDARD_CANDIDATE、FIELD_QUALITY 或 FIELD_COVERAGE。 */
  sourceType?: 'STANDARD_CANDIDATE' | 'FIELD_QUALITY' | 'FIELD_COVERAGE' | 'AI_TASK_FAILURE' | string
  /** 来源对象 ID 列表，例如候选 ID、字段 ID 或任务运行 ID。 */
  sourceIds?: number[]
  /** 字段质量问题代码筛选。 */
  issueCodes?: string[]
  /** 覆盖率状态筛选。 */
  coverageStatuses?: string[]
  /** 来源报告完整性状态。 */
  sourceStatus?: 'COMPLETE' | 'PARTIAL' | 'CANCELLED' | 'FAILED' | string
  /** 覆盖率来源中未纳入统计的失败表数量。 */
  failedTableCount?: number
  /** 覆盖率来源中跳过或未扫描表数量。 */
  skippedTableCount?: number
  /** 页面已知待处理项数量。 */
  itemCount?: number
  /** 可打开的来源页面 route。 */
  sourceRoute?: string
  /** 非敏感补充说明。 */
  note?: string
}

/** 标准维护 workflow 的建议动作摘要。 */
export interface StandardMaintenanceWorkflowInboxAction {
  /** 稳定动作类型。 */
  actionType?: string
  /** 触发动作的来源类型。 */
  sourceType?: string
  /** 本次计划覆盖的待处理项数量。 */
  targetCount?: number
  /** 人类可读标题。 */
  title?: string
  /** 脱敏动作说明。 */
  description?: string
  /** 是否需要人工确认。 */
  confirmationRequired?: boolean
}

/** 标准维护 workflow 与 AI recipe/task-card 的绑定。 */
export interface StandardMaintenanceWorkflowRecipeBinding {
  /** workflow recipe id。 */
  recipeId?: string
  /** recipe 契约版本。 */
  recipeVersion?: number
  /** 脱敏来源参数。 */
  sourceParameters?: Record<string, unknown>
  /** 可复制 task-card 命令模板。 */
  taskCardCommand?: string
}

/** 标准维护 workflow dry-run 步骤。 */
export interface StandardMaintenanceWorkflowStep {
  /** 步骤稳定 ID。 */
  stepId?: string
  /** 阶段：precheck、review、execute、verify 或 archive。 */
  phase?: string
  /** 步骤标题。 */
  title?: string
  /** 脱敏步骤说明。 */
  description?: string
  /** 推荐页面、API 或命令模板。 */
  recommendedAction?: string
  /** 是否需要人工确认。 */
  requiresConfirmation?: boolean
  /** 完成后应记录的证据。 */
  expectedEvidence?: string
  /** dry-run 步骤状态。 */
  status?: string
}

/** 标准维护 workflow 执行状态摘要。 */
export interface StandardMaintenanceWorkflowExecutionState {
  /** 当前状态。 */
  status?: string
  /** 当前建议步骤 ID。 */
  currentStepId?: string
  /** 是否可重试。 */
  retryable?: boolean
  /** 阻塞或 partial 来源说明。 */
  blockedReason?: string | null
}

/** 标准维护 workflow 证据链接或安全摘要。 */
export interface StandardMaintenanceWorkflowEvidenceLink {
  /** 来源能力。 */
  sourceCapability?: string
  /** 证据名称。 */
  label?: string
  /** 可打开的页面或 API 模板。 */
  targetRoute?: string
  /** 脱敏证据摘要。 */
  summary?: string
  /** 证据代表的待处理项数量。 */
  count?: number
}

/** 标准维护 workflow 下一步提示。 */
export interface StandardMaintenanceWorkflowNextAction {
  /** 稳定动作代码。 */
  code?: string
  /** 提示级别。 */
  severity?: string
  /** 脱敏说明。 */
  message?: string
  /** 可选命令或 API 模板。 */
  command?: string | null
  /** 是否可重试。 */
  retryable?: boolean
}

/** 标准维护 workflow dry-run 计划响应。 */
export interface StandardMaintenanceWorkflowPlan {
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** 本次 dry-run 计划 ID。 */
  workflowId?: string
  /** 来源 Inbox 或诊断信号动作。 */
  inboxAction?: StandardMaintenanceWorkflowInboxAction
  /** AI workflow recipe 绑定。 */
  recipeBinding?: StandardMaintenanceWorkflowRecipeBinding
  /** dry-run 步骤。 */
  dryRunSteps?: StandardMaintenanceWorkflowStep[]
  /** 当前状态和恢复位置。 */
  executionState?: StandardMaintenanceWorkflowExecutionState
  /** 未执行或中止时的安全回退说明。 */
  undoHint?: string
  /** 脱敏证据链接。 */
  evidenceLinks?: StandardMaintenanceWorkflowEvidenceLink[]
  /** 当前可执行下一步。 */
  nextActions?: StandardMaintenanceWorkflowNextAction[]
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
export type FieldImpactType = 'TEMPLATE' | 'IMPORT_SOURCE' | 'SQL_CHECK' | 'STANDARD_SNAPSHOT' | 'CODE_SET' | 'CODE_REFERENCE'

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
  codeReferenceImpactCount?: number
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
export type StandardTestDataPackageReq = Schemas['StandardTestDataPackageReq']
export type StandardTestDataPackage = Schemas['StandardTestDataPackage']
export type TestDataCase = Schemas['TestDataCase']
export type TestDataCoverageReport = Schemas['TestDataCoverageReport']
export type TestDataDiagnostic = Schemas['TestDataDiagnostic']
export type TestDataMockPayload = Schemas['TestDataMockPayload']
export type TestDataSafety = Schemas['TestDataSafety']
export type TestDataSeedProfile = Schemas['TestDataSeedProfile']
export type TestDataSourceSummary = Schemas['TestDataSourceSummary']
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
export type AiEvidencePackageReq = Omit<Schemas['AiEvidencePackageReq'], 'payloadSummary' | 'postCheckSummary'> & {
  payloadSummary?: Record<string, unknown>
  postCheckSummary?: Record<string, unknown>
}
export type AuthMe = Schemas['AuthMe']
export type DialectDiagnostic = Schemas['DialectDiagnostic']
export type TablePrimaryKeyStandard = Schemas['TablePrimaryKeyStandard']
export type TableUniqueKeyStandard = Schemas['TableUniqueKeyStandard']
export type TableIndexStandard = Schemas['TableIndexStandard']
export type TableForeignKeyStandard = Schemas['TableForeignKeyStandard']

/** CHECK 或校验提示的前端归一化展示类型；后端第一版保存为字符串数组。 */
export interface TableCheckHintStandard {
  /** 提示名称或短标题。 */
  name?: string | null
  /** 涉及的列名数组。 */
  columns?: string[]
  /** 非 raw SQL 的校验说明。 */
  hint?: string | null
  /** true 表示仅作为 guidance，不生成 CHECK 约束。 */
  advisoryOnly?: boolean | null
}

export type TableAuditPolicy = Schemas['TableAuditPolicy']
export type TableSoftDeletePolicy = Schemas['TableSoftDeletePolicy']

/** 通用表级策略说明；用于 Schema Registry/AI Context 描述审计、软删除等策略边界。 */
export interface TablePolicyStandard {
  /** 策略类型，如 AUDIT、SOFT_DELETE、CHECK_HINT。 */
  policyType?: string | null
  /** 策略适用字段名数组。 */
  fields?: string[]
  /** 策略是否只作为 guidance，不自动生成或应用数据库变更。 */
  advisoryOnly?: boolean | null
  /** 非敏感说明。 */
  notes?: string | null
}

export type TableRelationHint = Schemas['TableRelationHint']
export type TableStructureStandard = Schemas['TableStructureStandard']
export type BusinessObjectStandard = Schemas['BusinessObjectStandardResp']
export type BusinessObjectStandardReq = Schemas['BusinessObjectStandardReq']
export type BusinessObjectRelationNode = Schemas['TableRelationSummaryNode']
export type BusinessObjectRelationEdge = Schemas['TableRelationSummaryEdge']
export type BusinessObjectRelationStats = Schemas['TableRelationSummaryStats']
export type BusinessObjectRelationSummary = Schemas['TableRelationSummary']
export type DdlStructureSummary = Schemas['DdlStructureSummary']
export type DdlGenerateResult = Schemas['DdlGenerateResult']
export type TemplateResp = Schemas['TemplateResp']
export type TemplateSaveReq = Schemas['TemplateReq']

/** 兼容旧引用的模板类型；新增代码可优先使用 TemplateResp 表达详情。 */
export type Template = TemplateResp

/** 兼容表模板保存请求的旧命名。 */
export type TemplateReq = TemplateSaveReq
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

/** schema plan 字段级动作；动作只表示 dry-run 预览，不代表前端会执行迁移。 */
export type DatabaseSchemaChangeAction =
  | 'ALTER_COMMENT'
  | 'ALTER_COLUMN'
  | 'DROP_CANDIDATE'
  | (string & {})

/** schema plan 风险等级；BLOCKED 表示存在不得自动执行的阻塞项。 */
export type DatabaseSchemaRiskLevel =
  | 'SAFE'
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'BLOCKED'
  | (string & {})

/** schema change plan 的单个字段级变更项，值均来自 schema metadata 或 DataSpec 标准摘要。 */
export interface DatabaseSchemaChangeItem {
  /** 来源数据库表名，不包含连接串或业务数据行。 */
  tableName?: string
  /** 来源数据库字段名。 */
  columnName?: string
  /** 命中的 DataSpec 标准字段名；未命中标准时为空。 */
  standardFieldName?: string
  /** 预览动作，如注释修正、结构调整或删除候选。 */
  action?: DatabaseSchemaChangeAction
  /** 发生变化的属性，如 dataType、nullable、defaultValue、comment 或 column。 */
  property?: string
  /** 当前数据库 metadata 值；应为脱敏后的结构值。 */
  currentValue?: string
  /** 目标 DataSpec 标准值；应为脱敏后的结构值。 */
  targetValue?: string
  /** 字段级风险等级。 */
  riskLevel?: DatabaseSchemaRiskLevel
  /** 本项 dry-run SQL 草案；DROP_CANDIDATE 只能是注释化提示，不应是可执行 DROP。 */
  migrationSql?: string
  /** 本项回滚或撤销提示。 */
  rollbackHint?: string
  /** 执行前必须人工确认的检查点。 */
  manualChecks?: string[]
  /** 阻止自动执行的原因；非空时客户端不得提示自动应用。 */
  blockedReasons?: string[]
  /** 面向用户和 AI 的本项说明。 */
  reason?: string
}

/** schema change plan 聚合统计，用于前端风险面板和 AI 摘要。 */
export interface DatabaseSchemaChangeSummary {
  /** 本次计划覆盖的表数量。 */
  tableCount?: number
  /** 本次计划读取的字段数量。 */
  columnCount?: number
  /** 字段级变更项数量。 */
  changeCount?: number
  /** 低风险变更项数量。 */
  lowRiskCount?: number
  /** 中风险变更项数量。 */
  mediumRiskCount?: number
  /** 高风险变更项数量。 */
  highRiskCount?: number
  /** 带阻塞原因、不得自动执行的变更项数量。 */
  blockedCount?: number
}

/** 数据库 schema 变更计划响应；只用于预览和审计，不执行迁移、不保存连接凭据。 */
export interface DatabaseSchemaChangePlan {
  /** 响应类型标识，供 CLI/AI 判断 JSON 语义。 */
  kind?: string
  /** 响应 schema 版本；新增可选字段时保持兼容递增。 */
  schemaVersion?: number
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
  databaseType?: string
  /** 数据库名；不得包含完整 JDBC URL 或 DSN。 */
  databaseName?: string
  /** schema 名；MySQL 场景可能为空。 */
  schemaName?: string
  /** 当前源库 schema-only metadata hash，不包含密码、连接串或业务数据行。 */
  currentSchemaHash?: string
  /** 目标 DataSpec 标准摘要 hash，不包含连接凭据。 */
  targetSpecHash?: string
  /** 整体风险等级。 */
  riskLevel?: DatabaseSchemaRiskLevel
  /** 计划聚合统计。 */
  summary?: DatabaseSchemaChangeSummary
  /** 字段级变更项列表。 */
  changeSet?: DatabaseSchemaChangeItem[]
  /** 合并后的 dry-run SQL 草案，不得直接视为已审批迁移脚本。 */
  migrationSql?: string
  /** 整体回滚提示。 */
  rollbackHint?: string
  /** 全局人工检查点。 */
  manualChecks?: string[]
  /** 全局阻塞原因；非空时客户端不得提示自动执行迁移。 */
  blockedReasons?: string[]
  /** 面向用户和 AI 的后续动作建议。 */
  nextActions?: string[]
  /** metadata cache 证据；只描述 schema-only 缓存状态，不包含凭据。 */
  metadataCache?: DatabaseMetadataCacheInfo
}

export type DatabaseCommentPlanStatus = 'NO_OP' | 'MISSING' | 'CHANGED' | 'UNSUPPORTED' | (string & {})
export type DatabaseCommentObjectType = 'TABLE' | 'COLUMN' | (string & {})
export type DatabaseCommentRiskLevel = 'SAFE' | 'LOW' | 'MEDIUM' | 'HIGH' | (string & {})

/** COMMENT 回写计划聚合统计；只描述差异范围，不代表 SQL 已执行。 */
export interface DatabaseCommentPatchPlanSummary {
  /** 本次计划覆盖的表数量。 */
  tableCount?: number
  /** 本次计划读取的字段数量。 */
  columnCount?: number
  /** 计划项总数，包含 no-op 和 unsupported。 */
  itemCount?: number
  /** 可生成 dry-run SQL 的变更数量。 */
  executableChangeCount?: number
  /** 已一致、无需生成 SQL 的项数量。 */
  noOpCount?: number
  /** 当前注释为空但存在目标注释的项数量。 */
  missingCount?: number
  /** 当前注释与目标注释不同的项数量。 */
  changedCount?: number
  /** 因方言或证据不足不能安全生成 SQL 的项数量。 */
  unsupportedCount?: number
  /** 带阻塞原因、不得自动执行的项数量。 */
  blockedCount?: number
}

/** COMMENT SQL 方言支持摘要，前端据此展示 unsupported 和人工处理边界。 */
export interface DatabaseCommentDialectSupport {
  /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
  databaseType?: string
  /** true 表示可安全生成表 COMMENT dry-run SQL。 */
  tableCommentSqlSupported?: boolean
  /** true 表示可安全生成列 COMMENT dry-run SQL。 */
  columnCommentSqlSupported?: boolean
  /** 不支持或需人工处理的原因；文本应已脱敏。 */
  unsupportedReasons?: string[]
  /** 方言相关补充说明。 */
  notes?: string[]
}

/** COMMENT patch plan 证据摘要；只包含 schema-only 范围和标准引用。 */
export interface DatabaseCommentPatchPlanEvidence {
  /** schema/database 范围摘要，不包含 JDBC URL。 */
  schemaScope?: string
  /** 本次选择的表范围。 */
  tableScope?: string[]
  /** schema-only metadata fingerprint。 */
  metadataFingerprint?: string
  /** 参与计划判断的标准引用，如 template:<key> 或 field:<name>。 */
  standardReferences?: string[]
  /** 脱敏请求摘要，便于复制给 AI 或评审。 */
  normalizedInputSummary?: string
  /** 安全标记，如 readOnly、schemaOnly、noSourceWrites。 */
  safetyFlags?: string[]
}

/** COMMENT patch plan 安全边界，说明该响应仅用于 dry-run 审阅。 */
export interface DatabaseCommentPatchPlanSafety {
  /** true 表示计划生成只读取 schema metadata。 */
  readOnly?: boolean
  /** true 表示服务端会写源数据库；COMMENT plan 应保持 false。 */
  writesSourceDatabase?: boolean
  /** true 表示服务端会写 DataSpec 项目状态；COMMENT plan 应保持 false。 */
  writesProject?: boolean
  /** true 表示需要人工审阅后再进入迁移流程。 */
  requiresManualApply?: boolean
  /** true 表示响应可复制给 AI 辅助审阅。 */
  safeForAiCopy?: boolean
  /** true 表示自由文本已经经过敏感信息脱敏。 */
  sensitiveRedaction?: boolean
}

/** COMMENT 回写计划的单个表或字段注释差异项。 */
export interface DatabaseCommentPatchPlanItem {
  /** 对象类型：TABLE 或 COLUMN。 */
  objectType?: DatabaseCommentObjectType
  /** 来源 schema 名；MySQL 场景可能为空。 */
  schemaName?: string
  /** 来源表名；来自 schema metadata，不包含连接串或业务数据行。 */
  tableName?: string
  /** 来源字段名；TABLE 项为空。 */
  columnName?: string
  /** 命中的 DataSpec 标准字段名；表项或未命中时为空。 */
  standardFieldName?: string
  /** 差异状态：NO_OP、MISSING、CHANGED 或 UNSUPPORTED。 */
  status?: DatabaseCommentPlanStatus
  /** 当前数据库 COMMENT；应为脱敏文本。 */
  currentComment?: string
  /** 目标 DataSpec COMMENT；应为脱敏文本。 */
  targetComment?: string
  /** 面向用户和 AI 的注释差异说明。 */
  commentDiff?: string
  /** 单项 dry-run SQL；unsupported/no-op 项为空。 */
  dryRunSql?: string
  /** 当前项的方言支持摘要。 */
  dialectSupport?: string
  /** 单项风险：LOW、MEDIUM 或 HIGH。 */
  riskLevel?: DatabaseCommentRiskLevel
  /** 本项回滚提示。 */
  rollbackHint?: string
  /** 证据引用，如 template:<tablePrefix> 或 field:<fieldName>。 */
  evidenceRefs?: string[]
  /** 需要人工处理的检查点。 */
  manualChecks?: string[]
  /** 阻止生成可执行 SQL 的原因。 */
  blockedReasons?: string[]
}

/** 数据库 COMMENT 回写计划响应；只用于预览、复制和审阅，不执行源库写入。 */
export interface DatabaseCommentPatchPlan {
  /** 响应类型标识，供 CLI、前端和 AI 判断 JSON 语义。 */
  kind?: string
  /** 响应 schema 版本。 */
  schemaVersion?: number
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** 数据库类型，如 POSTGRESQL 或 MYSQL。 */
  databaseType?: string
  /** 数据库名；不得包含完整 JDBC URL 或 DSN。 */
  databaseName?: string
  /** schema 名；MySQL 场景可能为空。 */
  schemaName?: string
  /** schema-only metadata fingerprint；不包含凭据或业务数据行。 */
  metadataFingerprint?: string
  /** 当前 COMMENT 计划内容 hash。 */
  planHash?: string
  /** 计划聚合统计。 */
  summary?: DatabaseCommentPatchPlanSummary
  /** 表/字段 COMMENT 差异项。 */
  items?: DatabaseCommentPatchPlanItem[]
  /** 合并后的 dry-run SQL 草案。 */
  dryRunSql?: string
  /** 当前方言对表/列 COMMENT SQL 的支持情况。 */
  dialectSupport?: DatabaseCommentDialectSupport
  /** 整体风险等级。 */
  riskLevel?: DatabaseCommentRiskLevel
  /** 整体回滚提示。 */
  rollbackHint?: string
  /** 计划生成证据。 */
  evidence?: DatabaseCommentPatchPlanEvidence
  /** 只读安全边界。 */
  safety?: DatabaseCommentPatchPlanSafety
  /** 面向用户和 AI 的后续动作建议。 */
  nextActions?: string[]
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
  /** 新版采集作业标识；兼容 scanId，不包含凭据。 */
  scanJobId?: string
  /** 短期分页游标，第一版使用已读取表数量偏移。 */
  cursor?: string
  /** 新版恢复 cursor；兼容 cursor，不包含凭据。 */
  resumeCursor?: string
  /** 每页表数量，服务端限制在 1 到 100。 */
  pageSize?: number
  /** true 表示停止继续扫描，不写源库或标准库。 */
  cancel?: boolean
  /** 新版取消令牌；只用于显式取消动作，不包含连接密码或 DSN。 */
  cancelToken?: string
  /** 请求方限速偏好；服务端仍会应用全局上限。 */
  rateLimit?: DatabaseMetadataScanRateLimit
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

/** 采集作业限速边界。 */
export interface DatabaseMetadataScanRateLimit {
  /** 请求方希望单页最多读取的表数量。 */
  maxTablesPerPage?: number
  /** 建议客户端两次继续扫描之间等待的毫秒数。 */
  minDelayMs?: number
  /** 原始请求 pageSize，由服务端回填。 */
  requestedPageSize?: number
  /** 请求方传入的 maxTablesPerPage，由服务端回填。 */
  requestedMaxTablesPerPage?: number
  /** 服务端全局允许的最大单页表数量。 */
  maxPageSize?: number
  /** 本次请求实际采用的 pageSize。 */
  effectivePageSize?: number
}

/** 源库压力提示；文本必须脱敏。 */
export interface DatabaseMetadataScanSourcePressureHint {
  /** 压力等级：INFO/WARNING/DANGER。 */
  level?: string
  /** 可读提示，不包含 JDBC URL、DSN、token 或 password。 */
  message?: string
  /** true 表示请求 pageSize 被降限。 */
  boundedByServerLimit?: boolean
  /** 建议下一次继续扫描采用的 pageSize。 */
  suggestedPageSize?: number
  /** 安全下一步动作。 */
  safeNextActions?: string[]
}

/** 采集作业重试策略。 */
export interface DatabaseMetadataScanRetryPolicy {
  /** true 表示可由用户或 AI 显式继续/重试。 */
  retryable?: boolean
  /** 建议重试等待毫秒数。 */
  retryAfterMs?: number
  /** 建议最大重试次数。 */
  maxRetryAttempts?: number
  /** true 表示建议降低 pageSize。 */
  lowerPageSizeRecommended?: boolean
  /** true 表示建议优先使用 metadata cache。 */
  useMetadataCacheRecommended?: boolean
}

/** schema-only dump 的列结构。 */
export interface DatabaseSchemaColumn {
  /** 字段名。 */
  columnName?: string
  /** 数据库字段类型。 */
  dataType?: string
  /** true 表示字段可空。 */
  nullable?: boolean
  /** 默认值。 */
  defaultValue?: string
  /** 字段注释。 */
  comment?: string
  /** 字段序号。 */
  ordinalPosition?: number
}

/** schema-only dump 的表结构。 */
export interface DatabaseSchemaTable {
  /** 表所在 schema；MySQL 场景可能为空。 */
  schemaName?: string
  /** 表名。 */
  tableName?: string
  /** 表类型。 */
  tableType?: string
  /** 表注释。 */
  comment?: string
  /** 列 metadata；不包含业务数据行。 */
  columns?: DatabaseSchemaColumn[]
  /** 索引 metadata。 */
  indexes?: DatabaseSchemaIndex[]
  /** 表级警告。 */
  warnings?: string[]
}

/** 当前页可复用的成功/失败部分结果。 */
export interface DatabaseMetadataScanPartialResult {
  /** 成功读取 schema metadata 的表结构。 */
  successfulTables?: DatabaseSchemaTable[]
  /** 成功读取 metadata 的表名。 */
  successfulTableNames?: string[]
  /** 失败表名；不得静默导入。 */
  failedTableNames?: string[]
  /** 跳过表名。 */
  skippedTableNames?: string[]
  /** true 表示成功表足以生成预览。 */
  completeForPreview?: boolean
  /** true 表示成功表足以生成覆盖率。 */
  completeForCoverage?: boolean
  /** true 表示整个扫描范围已完成且无失败。 */
  complete?: boolean
}

/** 单表失败脱敏摘要。 */
export interface DatabaseMetadataScanFailureItem {
  /** 失败表所在 schema。 */
  schemaName?: string
  /** 失败表名。 */
  tableName?: string
  /** 失败类别。 */
  category?: string
  /** true 表示可重试。 */
  retryable?: boolean
  /** 脱敏错误摘要。 */
  message?: string
}

/** 当前页 bounded 失败摘要。 */
export interface DatabaseMetadataScanFailureSummary {
  /** 本页失败表数量。 */
  failedTableCount?: number
  /** bounded 失败表示例。 */
  failedTables?: DatabaseMetadataScanFailureItem[]
  /** 本页失败类别。 */
  failureCategories?: string[]
  /** true 表示至少一个失败项可重试。 */
  retryable?: boolean
  /** 安全下一步动作。 */
  safeNextActions?: string[]
}

/** 可复制给 AI 的 scan evidence。 */
export interface DatabaseMetadataScanEvidence {
  /** 采集作业标识。 */
  scanJobId?: string
  /** 作业状态。 */
  status?: string
  /** 已处理表数量。 */
  processedTableCount?: number
  /** 失败表数量。 */
  failedTableCount?: number
  /** schema 范围摘要。 */
  schemaScope?: string
  /** 表范围摘要。 */
  tableScope?: string[]
  /** metadata cache fingerprint。 */
  metadataFingerprint?: string
  /** true 表示只读取 schema metadata。 */
  schemaOnly?: boolean
  /** true 表示不会写源库。 */
  noSourceWrites?: boolean
  /** true 表示不会写标准字段库。 */
  noStandardWrites?: boolean
  /** true 表示 evidence 可复制给 AI。 */
  safeForAiCopy?: boolean
  /** 安全下一步动作。 */
  nextActions?: string[]
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
  /** 新版采集作业 ID；与 scanId 兼容。 */
  scanJobId?: string
  /** 作业状态：RUNNING/PARTIAL/COMPLETED/CANCELLED/FAILED。 */
  status?: string
  /** 当前连接可见表数量估算。 */
  estimatedTableCount?: number
  /** 下一批 cursor；为空表示无后续批次。 */
  cursor?: string | null
  /** 新版恢复 cursor；为空表示无后续批次。 */
  resumeCursor?: string | null
  /** 新版取消令牌；仅用于显式取消动作。 */
  cancelToken?: string
  /** 本次请求实际采用的 pageSize。 */
  pageSize?: number
  /** 当前页表级 metadata，不包含列 metadata 或业务数据行。 */
  tables?: DatabaseTableInfo[]
  /** 本次扫描应用后的限速边界。 */
  rateLimit?: DatabaseMetadataScanRateLimit
  /** 源库压力提示和安全下一步。 */
  sourcePressureHint?: DatabaseMetadataScanSourcePressureHint
  /** 重试/继续扫描建议。 */
  retryPolicy?: DatabaseMetadataScanRetryPolicy
  /** 当前页 schema-only 部分结果。 */
  partialResult?: DatabaseMetadataScanPartialResult
  /** 当前页失败摘要。 */
  failureSummary?: DatabaseMetadataScanFailureSummary
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
  /** 可复制给 AI 的只读证据摘要。 */
  evidence?: DatabaseMetadataScanEvidence
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
  /** 输入完整性状态；PARTIAL/CANCELLED/FAILED 表示报告只覆盖成功采集到的表。 */
  inputStatus?: 'COMPLETE' | 'PARTIAL' | 'CANCELLED' | 'FAILED' | string
  /** 未纳入覆盖率计算的失败表数量。 */
  failedTableCount?: number
  /** 未纳入覆盖率计算的跳过或未扫描表数量。 */
  skippedTableCount?: number
  /** partial/cancelled/failed 输入的安全下一步。 */
  nextActions?: string[]
  /** 覆盖率报告关联的 metadata cache 证据，用于判断报告所基于的结构版本。 */
  metadataCache?: DatabaseMetadataCacheInfo
  tables?: FieldCoverageTable[]
  unmanagedRankings?: UnmanagedFieldRanking[]
}

export interface ScanPartialCoverageReq {
  /** DataSpec 项目 ID。 */
  projectId: number
  /** metadata scan job 返回的 schema-only partialResult；覆盖率只统计 successfulTables。 */
  partialResult: DatabaseMetadataScanPartialResult
  /** metadata scan job 返回的失败摘要。 */
  failureSummary?: DatabaseMetadataScanFailureSummary
  /** metadata scan job 状态，如 PARTIAL/CANCELLED/FAILED/COMPLETED。 */
  scanStatus?: string
}
