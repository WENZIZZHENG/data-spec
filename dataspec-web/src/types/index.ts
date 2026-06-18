import type { components } from '@/api/schema'

type Schemas = components['schemas']

export type Project = Schemas['Project']
export type CreateProjectReq = Schemas['CreateProjectReq']
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
export type DdlGenerateResult = Schemas['DdlGenerateResult']
export type Template = Schemas['Template']
export type TemplateField = Schemas['TemplateField']
export type TableDef = Schemas['TableDef']
export type ColumnDef = Schemas['ColumnDef']

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

export interface ExcelImportPreview {
  valid?: boolean
  fields?: ExcelSheetSummary
  enumDicts?: ExcelSheetSummary
  enumValues?: ExcelSheetSummary
  errors?: ExcelImportError[]
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
