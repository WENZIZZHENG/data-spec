import type { components } from '@/api/schema'

type Schemas = components['schemas']

export type Project = Schemas['Project']
export type CreateProjectReq = Schemas['CreateProjectReq']
export type DemoProjectResult = Schemas['DemoProjectResult']
export type Field = Schemas['Field']
export type FieldReq = Schemas['FieldReq']
export type FieldSuggestion = Schemas['FieldSuggestion']

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
export type SqlCheckRecord = Schemas['SqlCheckRecord']
export type RecordDetail = Schemas['RecordDetail']
export type AuthMe = Schemas['AuthMe']
export type DdlGenerateResult = Schemas['DdlGenerateResult']
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

export interface R<T> {
  code?: number
  message?: string
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
}

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
  tables?: TableDef[]
  fieldCandidates?: FieldCandidate[]
  missingComments?: MissingCommentIssue[]
  nonStandardFields?: NonStandardField[]
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
  tableDiffs?: ReverseImportTableDiff[]
}

export interface DatabaseConnectionReq {
  projectId?: number
  databaseType?: 'postgresql' | 'mysql'
  host?: string
  port?: number
  databaseName?: string
  schemaName?: string
  username?: string
  password?: string
  tableNames?: string[]
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

export interface DatabaseConnectionResult {
  success?: boolean
  message?: string
}

export interface DatabaseTableInfo {
  schemaName?: string
  tableName?: string
  tableType?: string
  comment?: string
}

export interface DatabaseImportResult {
  importedCount?: number
  skippedCount?: number
  importedFields?: string[]
  skippedFields?: string[]
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
  tables?: FieldCoverageTable[]
  unmanagedRankings?: UnmanagedFieldRanking[]
}
