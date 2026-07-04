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
