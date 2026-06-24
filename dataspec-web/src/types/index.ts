import type { components } from '@/api/schema'

type Schemas = components['schemas']

export type Project = Schemas['Project']
export type CreateProjectReq = Schemas['CreateProjectReq']
export type DemoProjectResult = Schemas['DemoProjectResult']
export type Field = Schemas['Field']
export type FieldReq = Schemas['FieldReq']
export type FieldSuggestion = Schemas['FieldSuggestion']
export type RuleConfig = Schemas['RuleConfig']
export type RuleConfigReq = Schemas['RuleConfigReq']
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

export interface PageResult<T> {
  records?: T[]
  total?: number
  current?: number
  size?: number
  pages?: number
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
