import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

function readSource(relativePath: string) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

function assertContains(source: string, snippets: string[], context: string) {
  for (const snippet of snippets) {
    assert.ok(source.includes(snippet), `${context} should include ${snippet}`)
  }
}

test('keeps critical frontend routes and navigation entries wired', () => {
  const router = readSource('src/router/index.ts')
  const app = readSource('src/App.vue')

  const criticalRoutes = [
    { path: '/fields', view: 'FieldLibrary.vue', title: '标准字段库' },
    { path: '/standard-candidates', view: 'StandardCandidate.vue', title: '标准候选' },
    { path: '/sql-lint', view: 'SqlLint.vue', title: 'SQL 校验' },
    { path: '/generator', view: 'Generator.vue', title: '生成器' },
    { path: '/ai-export', view: 'AiExport.vue', title: 'AI 规则导出' },
    { path: '/ai-profiles', view: 'AiProfile.vue', title: 'AI 任务模式' },
    { path: '/ai-replay', view: 'AiReplay.vue', title: 'AI 回放' },
    { path: '/ai-batches', view: 'AiBatch.vue', title: 'AI 批量任务' },
    { path: '/ai-feedback', view: 'AiFeedback.vue', title: 'AI 反馈' },
    { path: '/project-backup', view: 'ProjectBackup.vue', title: '项目备份' },
    { path: '/reverse-import', view: 'ReverseImport.vue', title: '反向导入' },
    { path: '/field-coverage', view: 'FieldCoverage.vue', title: '覆盖率报告' }
  ]

  for (const route of criticalRoutes) {
    assertContains(router, [
      `path: '${route.path}'`,
      `@/views/${route.view}`,
      `title: '${route.title}'`
    ], `router ${route.path}`)
    assertContains(app, [`index="${route.path}"`, route.title], `app navigation ${route.path}`)
  }
})

test('keeps AI batch delivery package page wired', () => {
  const view = readSource('src/views/AiBatch.vue')
  const api = readSource('src/api/aiBatch.ts')
  const types = readSource('src/types/index.ts')

  assertContains(view, [
    "import { downloadAiBatchPackage, getAiBatchDetail, listAiBatches } from '@/api/aiBatch'",
    'projectStore.currentProjectId',
    'async function loadBatches()',
    'await listAiBatches(projectId, current.value, size.value)',
    'async function openDetail(id?: number)',
    'activeDetail.value = await getAiBatchDetail(id)',
    'async function handleDownload(id?: number)',
    'downloadAiBatchPackage(id)',
    'AI 批量任务',
    '暂无 AI 批量任务'
  ], 'AiBatch.vue')

  assertContains(api, [
    "request.get<unknown, PageResult<AiBatchRunListItem>>('/ai-batches'",
    'export function getAiBatchDetail(id: number)',
    'export function downloadAiBatchPackage(id: number)'
  ], 'ai batch api')

  assertContains(types, [
    'export interface AiBatchDeliveryPackage',
    'export interface AiBatchRunListItem',
    'export interface AiBatchRunDetail'
  ], 'ai batch types')
})

test('keeps AI feedback improvement loop page wired', () => {
  const view = readSource('src/views/AiFeedback.vue')
  const api = readSource('src/api/aiFeedback.ts')
  const types = readSource('src/types/index.ts')
  const schema = readSource('src/api/schema.ts')

  assertContains(view, [
    "import { getAiFeedbackReport } from '@/api/aiFeedback'",
    'projectStore.currentProjectId',
    'async function loadReport()',
    'report.value = await getAiFeedbackReport(projectId)',
    'function goTarget(route?: string | null)',
    'buildAiFeedbackRoute(route)',
    'AI 反馈',
    '高频字段信号',
    '规则问题排行',
    'fixedSql 机会',
    '标准化信号',
    '请先创建并选择项目'
  ], 'AiFeedback.vue')

  assertContains(api, [
    "request.get<unknown, AiFeedbackReport>('/ai-feedback/report'",
    'export function getAiFeedbackReport(projectId: number)'
  ], 'ai feedback api')

  assertContains(types, [
    'export interface AiFeedbackReport',
    'export interface AiFeedbackSignal',
    'export interface AiFeedbackAction'
  ], 'ai feedback types')

  assertContains(schema, [
    '"/api/ai-feedback/report"',
    'AiFeedbackReport',
    'RAiFeedbackReport'
  ], 'ai feedback schema')
})

test('keeps AI profile review and selection flow wired', () => {
  const view = readSource('src/views/AiProfile.vue')
  const api = readSource('src/api/aiProfile.ts')
  const types = readSource('src/types/index.ts')
  const schema = readSource('src/api/schema.ts')
  const selection = readSource('src/utils/aiProfileSelection.ts')

  assertContains(view, [
    "import { listAiProfiles } from '@/api/aiProfile'",
    'projectStore.currentProjectId',
    'readSelectedAiProfile(projectId)',
    'resolveSelectedAiProfile',
    'saveSelectedAiProfile(projectId, selectedProfileId.value)',
    'selectedProfile?.fixedSqlPolicy?.mode',
    'selectedProfile.recommendedCommands',
    'copyFirstCommand',
    'AI 任务模式',
    '当前选择只作为 AI/CLI/MCP 默认建议',
    '可用模式',
    '推荐命令'
  ], 'AiProfile.vue')

  assertContains(api, [
    "request.get<unknown, AiTaskProfileCatalog>('/ai-profiles'",
    'export function getAiProfile(profileOrTaskType: string',
    '`/ai-profiles/${encodeURIComponent(profileOrTaskType)}`'
  ], 'AI profile api')

  assertContains(types, [
    "export type AiTaskContextScope = Schemas['AiTaskContextScope']",
    "export type AiTaskRuleset = Schemas['AiTaskRuleset']",
    "export type AiTaskOutputFormat = Schemas['AiTaskOutputFormat']",
    "export type AiProfileDiagnostic = Schemas['AiProfileDiagnostic']",
    "export type AiTaskProfile = Schemas['AiTaskProfile']",
    "export type AiTaskProfileCatalog = Schemas['AiTaskProfileCatalog']",
    "export type AiTaskProfileDetail = Schemas['AiTaskProfileDetail']"
  ], 'AI profile types')

  assertContains(schema, [
    '"/api/ai-profiles"',
    '"/api/ai-profiles/{profileOrTaskType}"',
    'listAiTaskProfiles',
    'getAiTaskProfile',
    'AiTaskProfileCatalog',
    'RAiTaskProfileDetail'
  ], 'AI profile schema')

  assertContains(selection, [
    'aiProfileStorageKey',
    'readSelectedAiProfile',
    'saveSelectedAiProfile',
    'resolveSelectedAiProfile',
    'dataspec.aiProfile'
  ], 'AI profile selection utility')
})

test('keeps schema registry contract api and types wired', () => {
  const api = readSource('src/api/contract.ts')
  const types = readSource('src/types/index.ts')
  const schema = readSource('src/api/schema.ts')

  assertContains(api, [
    "request.get<unknown, SchemaRegistryCatalog>('/contracts')",
    'export function getContract(contractId: string)',
    '`/contracts/${encodeURIComponent(contractId)}`'
  ], 'schema registry api')

  assertContains(types, [
    "export type DeprecatedContractField = Schemas['DeprecatedContractField']",
    "export type SchemaCompatibilityPolicy = Schemas['SchemaCompatibilityPolicy']",
    "export type SchemaContractSummary = Schemas['SchemaContractSummary']",
    "export type SchemaContract = Schemas['SchemaContract']",
    "export type SchemaRegistryCatalog = Schemas['SchemaRegistryCatalog']"
  ], 'schema registry types')

  assertContains(schema, [
    '"/api/contracts"',
    '"/api/contracts/{contractId}"',
    'listContracts',
    'getContract',
    'SchemaRegistryCatalog',
    'RSchemaContract'
  ], 'schema registry schema')
})

test('keeps AI evidence package actions wired on high frequency result pages', () => {
  const api = readSource('src/api/evidence.ts')
  const types = readSource('src/types/index.ts')
  const schema = readSource('src/api/schema.ts')
  const sqlLint = readSource('src/views/SqlLint.vue')
  const coverage = readSource('src/views/FieldCoverage.vue')
  const aiBatch = readSource('src/views/AiBatch.vue')

  assertContains(api, [
    "request.post<unknown, AiEvidencePackage>('/evidence-packages', data)",
    "request.post<unknown, Blob>('/evidence-packages/download', data"
  ], 'evidence api')

  assertContains(types, [
    "export type EvidenceSourceType = Schemas['EvidenceSourceType']",
    "export type AiEvidencePackage = Schemas['AiEvidencePackage']",
    "export type AiEvidencePackageReq = Schemas['AiEvidencePackageReq']"
  ], 'evidence types')

  assertContains(schema, [
    '"/api/evidence-packages"',
    '"/api/evidence-packages/download"',
    'generateEvidencePackage',
    'downloadEvidencePackage',
    'AiEvidencePackage',
    'AiEvidencePackageReq',
    'RAiEvidencePackage'
  ], 'evidence schema')

  assertContains(sqlLint, [
    "import { downloadEvidencePackage, generateEvidencePackage } from '@/api/evidence'",
    'handleCopyRecordEvidence',
    'handleDownloadRecordEvidence',
    "sourceType: 'SQL_CHECK'",
    '复制证据 JSON',
    '下载证据包'
  ], 'SqlLint evidence actions')

  assertContains(coverage, [
    "import { downloadEvidencePackage, generateEvidencePackage } from '@/api/evidence'",
    'handleCopyCoverageEvidence',
    'handleDownloadCoverageEvidence',
    "sourceType: 'COVERAGE_REPORT'",
    'coverageReport: report.value',
    '复制证据 JSON',
    '下载证据包'
  ], 'FieldCoverage evidence actions')

  assertContains(aiBatch, [
    "import { downloadEvidencePackage, generateEvidencePackage } from '@/api/evidence'",
    'handleCopyEvidence',
    'handleDownloadEvidence',
    "sourceType: 'AI_BATCH_RUN'",
    '复制证据 JSON',
    '下载证据包'
  ], 'AiBatch evidence actions')
})

test('keeps standard candidate inbox workbench wired', () => {
  const view = readSource('src/views/StandardCandidate.vue')
  const api = readSource('src/api/standardCandidate.ts')
  const types = readSource('src/types/index.ts')
  const schema = readSource('src/api/schema.ts')

  assertContains(view, [
    "import { listFields } from '@/api/field'",
    "import {",
    "listStandardCandidates",
    "createStandardCandidate",
    "acceptStandardCandidate",
    "mergeStandardCandidate",
    "ignoreStandardCandidate",
    "postponeStandardCandidate",
    "projectStore.currentProjectId",
    "createVisible.value = false",
    "decisionVisible.value = false",
    "mergeVisible.value = false",
    "await createStandardCandidate({ ...createForm, projectId })",
    "async function loadCandidates()",
    "async function submitCreate()",
    "async function submitDecision()",
    "async function submitMerge()",
    "标准候选",
    "新建候选",
    "暂无标准候选",
    "请先创建并选择项目"
  ], 'StandardCandidate.vue')

  assertContains(api, [
    "request.get<unknown, PageResult<StandardCandidate>>('/standard-candidates'",
    "export function createStandardCandidate",
    "export function acceptStandardCandidate",
    "export function mergeStandardCandidate",
    "export function ignoreStandardCandidate",
    "export function postponeStandardCandidate"
  ], 'standard candidate api')

  assertContains(types, [
    'export interface StandardCandidate',
    'export interface StandardCandidateCreateReq',
    'export interface StandardCandidateMergeReq'
  ], 'standard candidate types')

  assertContains(schema, [
    '"/api/standard-candidates"',
    '"/api/standard-candidates/{id}/accept"',
    '"/api/standard-candidates/{id}/merge"',
    '"/api/standard-candidates/{id}/ignore"',
    '"/api/standard-candidates/{id}/postpone"',
    'StandardCandidate',
    'RPageResultStandardCandidate',
    'RStandardCandidate'
  ], 'standard candidate schema')
})

test('keeps global project selector backed by the project store', () => {
  const app = readSource('src/App.vue')
  const projectStore = readSource('src/stores/project.ts')

  assertContains(app, [
    'el-breadcrumb',
    'routeTitle',
    'route.meta.title',
    'useProjectStore',
    'projectStore.loadProjects()',
    'v-model="projectStore.currentProjectId"',
    'projectStore.projects',
    'handleProjectChange',
    'projectStore.setCurrentProjectById(val)',
    'projectStore.clearCurrentProject()'
  ], 'App.vue project selector')

  assertContains(projectStore, [
    'const projects = ref<Project[]>([])',
    'const currentProjectId = ref<number | null>(null)',
    "const currentProjectName = ref('')",
    'const currentProject = computed',
    'async function loadProjects()',
    'projects.value = await listProjects()',
    'function setCurrentProjectById(projectId: number | null)',
    'function clearCurrentProject()'
  ], 'project store')
})

test('keeps dashboard task entrypoints, recent tasks, and breadcrumbs wired', () => {
  const dashboard = readSource('src/views/Dashboard.vue')
  const app = readSource('src/App.vue')

  assertContains(dashboard, [
    'const RECENT_TASKS_KEY',
    'dataspec.dashboard.recentTasks.v1',
    'const taskEntries',
    '导入现有库',
    '检查 SQL',
    '生成覆盖率',
    '补标准字段',
    '生成 DDL',
    '导出给 AI',
    '管理 Token',
    '任务入口',
    '最近任务',
    'function openTask',
    'function recordRecentTask',
    'function loadRecentTasks',
    'function clearRecentTasks',
    'const cleaned = parsed.filter(isRecentTask).slice(0, 20)',
    'cleaned.length !== parsed.length',
    'localStorage.removeItem(RECENT_TASKS_KEY)',
    'localStorage.getItem(RECENT_TASKS_KEY)',
    'localStorage.setItem(RECENT_TASKS_KEY'
  ], 'Dashboard.vue task entrypoints')

  assertContains(app, [
    '<el-breadcrumb',
    ":to=\"{ path: '/dashboard' }\"",
    'route.path !== \'/dashboard\'',
    'const routeTitle = computed'
  ], 'App.vue breadcrumbs')
})

test('keeps project activity timeline wired on dashboard', () => {
  const dashboard = readSource('src/views/Dashboard.vue')
  const api = readSource('src/api/activity.ts')
  const types = readSource('src/types/index.ts')

  assertContains(dashboard, [
    "import { listProjectActivities } from '@/api/activity'",
    'activityTimeline',
    'activityActionType',
    'async function loadActivities()',
    'await listProjectActivities(',
    'activityItems',
    'activityActionOptions',
    'goActivity(activity.detailRoute)',
    '最近活动',
    '暂无项目活动'
  ], 'Dashboard.vue activity timeline')

  assertContains(api, [
    'export function listProjectActivities',
    "`/projects/${projectId}/activities`",
    'ProjectActivityTimeline'
  ], 'project activity api')

  assertContains(types, [
    'export type ProjectActivityAction',
    'export type ProjectActivityItem',
    'export type ProjectActivityTimeline'
  ], 'project activity types')
})

test('keeps SQL lint fixed SQL and record history flow wired', () => {
  const view = readSource('src/views/SqlLint.vue')
  const api = readSource('src/api/lint.ts')

  assertContains(view, [
    "import { listAiProfiles } from '@/api/aiProfile'",
    "import { getLintRecord, lintSql, listLintRecords } from '@/api/lint'",
    'projectId: projectStore.currentProjectId ?? undefined',
    'request.profileId = selectedProfileId.value',
    'request.fixPolicy = currentFixPolicy.value',
    'profileFixPolicyActive',
    'fixPolicyMode',
    'fixMaxRiskLevel',
    'includeFixExplanations',
    'AI 模式',
    'profile 策略',
    '手动策略',
    '修复模式',
    '最高风险',
    '修复策略',
    'fixChanges',
    'fixSummary',
    'fixStatusLabel',
    'fixChangeLabel',
    'lintResult.fixedSql',
    'handleCopySql',
    'copyToClipboard(fixedSql',
    'handleCopyReplayCommand',
    'activeRecord.replay',
    'lintDialectDiagnostics',
    'dialectSummary(lintDialectDiagnostics)',
    'async function loadRecords()',
    'await listLintRecords(projectId, recordCurrent.value, recordSize.value)',
    'async function handleViewRecord(id?: number)',
    'activeRecord.value = await getLintRecord(id)'
  ], 'SqlLint.vue')

  assertContains(api, [
    "request.post<unknown, LintResult>('/lint', data)",
    "request.get<unknown, PageResult<SqlCheckRecord>>('/lint/records'",
    'export function getLintRecord(id: number)',
    'RecordDetail'
  ], 'lint api')

  const schema = readSource('src/api/schema.ts')
  assertContains(schema, [
    'FixPolicy',
    'FixChange',
    'FixPlanSummary',
    'fixPolicy?: components["schemas"]["FixPolicy"]',
    'fixChanges?: components["schemas"]["FixChange"][]',
    'fixRiskLevel?: "LOW" | "MEDIUM" | "HIGH"'
  ], 'lint schema fixedSql policy')
})

test('keeps database reverse import and comparison flow wired', () => {
  const view = readSource('src/views/ReverseImport.vue')
  const api = readSource('src/api/reverseImport.ts')

  assertContains(view, [
    'previewReverseImport',
    'previewDatabaseReverseImport',
    'compareDatabaseReverseImport',
    'importDatabaseCandidates',
    'testDatabaseConnection',
    'listDatabaseTables',
    'loadReverseImportMemory',
    'saveReverseImportMemory',
    'projectStore.currentProjectId',
    'async function handleLoadTables()',
    'async function handleGenerateCompare()',
    'async function handleImportCandidates()',
    'previewDialectDiagnostics',
    'dialectSummary(previewDialectDiagnostics)',
    'connectionSecurity',
    'securityRiskLabel(connectionSecurity.riskLevel)',
    'connectionSecurity.recommendedSql',
    '只读安全诊断',
    'goToFieldLibrary'
  ], 'ReverseImport.vue')

  assertContains(api, [
    "request.post<unknown, ReverseImportPreview>('/reverse-import/preview'",
    "request.post<unknown, DatabaseConnectionResult>('/reverse-import/database/test'",
    "request.post<unknown, DatabaseTableInfo[]>('/reverse-import/database/tables'",
    "request.post<unknown, ReverseImportPreview>('/reverse-import/database/preview'",
    "request.post<unknown, ReverseImportCompareResult>('/reverse-import/database/compare'",
    "request.post<unknown, DatabaseImportResult>('/reverse-import/database/import'"
  ], 'reverse import api')
})

test('keeps field library filtering, grouping, bulk maintenance, and undo flow wired', () => {
  const view = readSource('src/views/FieldLibrary.vue')
  const api = readSource('src/api/field.ts')

  assertContains(view, [
    'const fieldKeyword = ref',
    'route.query.keyword',
    'searchFields(searchRequest)',
    'fieldSearchSummary',
    'fieldSearchItems',
    'fieldSearchNextActions',
    '命中原因',
    '下一步建议',
    'listFields(projectId)',
    'getFieldGroupSummary(projectId)',
    'previewFieldBulkUpdate(payload)',
    'bulkUpdateFields(payload)',
    'listChangeLogs(projectId',
    'undoFieldChange(fieldId, log.id)',
    'getFieldImpactReport(field.id, projectStore.currentProjectId)',
    'openBulkDialog',
    'handleUndoChange'
  ], 'FieldLibrary.vue')

  assertContains(api, [
    "request.get<unknown, Field[]>('/fields/all'",
    "request.get<unknown, FieldSearchResult>('/fields/search'",
    "request.get<unknown, FieldGroupSummary>('/fields/groups'",
    "request.post<unknown, FieldBulkUpdatePreview>('/fields/bulk-update/preview'",
    "request.post<unknown, FieldBulkUpdateResult>('/fields/bulk-update'",
    "request.post<unknown, FieldChangeUndoResult>(`/fields/${id}/undo`"
  ], 'field api')
})

test('keeps DDL generation and AI Context export flows project-scoped', () => {
  const generator = readSource('src/views/Generator.vue')
  const generatorApi = readSource('src/api/generator.ts')
  const aiExport = readSource('src/views/AiExport.vue')
  const aiContextApi = readSource('src/api/aicontext.ts')

  assertContains(generator, [
    'projectStore.currentProjectId',
    'listTemplates(projectId)',
    'listTemplateFields(templateId)',
    'previewDdl(projectId, templateId, normalizedTableName)',
    'ddlDialectDiagnostics',
    'dialectSummary(ddlDialectDiagnostics)',
    'handleCopyDdl',
    'previewDataDictionaryHtml(projectId)',
    'downloadDataDictionaryHtml(projectId)',
    'route.query.templateId',
    'route.query.tableName'
  ], 'Generator.vue')

  assertContains(generatorApi, [
    "request.get<unknown, DdlGenerateResult>('/generator/ddl/preview'",
    'previewDataDictionaryHtml',
    'downloadDataDictionaryHtml',
    'previewDataDictionaryErd',
    'downloadDataDictionaryErd'
  ], 'generator api')

  assertContains(aiExport, [
    'projectStore.currentProjectId',
    'normalizeAiContextScopeParams(scopeForm)',
    'previewDatabaseRules(projectId, scopeParams)',
    'previewFieldCatalog(projectId, scopeParams)',
    'previewRulesYaml(projectId, scopeParams)',
    'downloadAiContextPackage(projectId, scopeParams)',
    'listStandardSnapshots(projectId)',
    'projectStore.createDemoProjectAndSelect()'
  ], 'AiExport.vue')

  assertContains(aiContextApi, [
    "request.get<unknown, string>('/ai-context/database-rules'",
    "request.get<unknown, string>('/ai-context/field-catalog'",
    "request.get<unknown, string>('/ai-context/rules-yaml'",
    "request.get<unknown, Blob>('/ai-context/package/download'"
  ], 'AI Context api')
})

test('keeps rule baseline suite workflow wired on rule config page', () => {
  const ruleConfig = readSource('src/views/RuleConfig.vue')
  const ruleBaselineApi = readSource('src/api/ruleBaseline.ts')

  assertContains(ruleConfig, [
    'getCurrentRuleBaseline(projectId)',
    'listRuleBaselineTemplates()',
    'applyRuleBaseline({',
    'exportRuleBaseline(projectId)',
    'importRuleBaseline({',
    'overwriteBaseline',
    'baselineResultSummary',
    '请先选择项目'
  ], 'RuleConfig.vue')

  assertContains(ruleBaselineApi, [
    "request.get<unknown, RuleBaselineTemplate[]>('/rule-baselines/templates')",
    "request.get<unknown, RuleBaselineInfo>('/rule-baselines/current'",
    "request.post<unknown, RuleBaselineApplyResult>('/rule-baselines/apply'",
    "request.get<unknown, RuleBaselinePackage>('/rule-baselines/export'",
    "request.post<unknown, RuleBaselineApplyResult>('/rule-baselines/import'"
  ], 'rule baseline api')
})

test('keeps project backup export and restore workflow wired', () => {
  const view = readSource('src/views/ProjectBackup.vue')
  const api = readSource('src/api/projectBackup.ts')
  const types = readSource('src/types/index.ts')
  const schema = readSource('src/api/schema.ts')

  assertContains(view, [
    '导出备份 JSON',
    '粘贴备份 JSON',
    '预览恢复',
    '确认恢复',
    '恢复到新项目',
    '恢复到当前项目',
    'password/token/source rows',
    '未选择项目，导出已禁用；仍可在下方恢复到新项目。',
    '最近恢复记录',
    'exportProjectBackup(projectId)',
    'previewProjectBackupRestore({',
    'applyProjectBackupRestore({',
    'listProjectRestoreRecords(projectId)'
  ], 'ProjectBackup.vue')

  assertContains(api, [
    "request.get<unknown, ProjectBackupPackage>('/project-backups/export'",
    "request.post<unknown, ProjectRestorePlan>('/project-backups/restore/preview'",
    "request.post<unknown, ProjectRestoreResult>('/project-backups/restore/apply'",
    "request.get<unknown, ProjectRestoreRecord[]>('/project-backups/restore/records'"
  ], 'project backup api')

  assertContains(types, [
    "export type ProjectBackupPackage = Schemas['ProjectBackupPackage']",
    "export type ProjectRestoreReq = Schemas['ProjectRestoreReq']",
    "export type ProjectRestorePlan = Schemas['ProjectRestorePlan']",
    "export type ProjectRestoreRecord = Schemas['ProjectRestoreRecord']",
    "export type ProjectRestoreResult = Schemas['ProjectRestoreResult']"
  ], 'project backup types')

  assertContains(schema, [
    '"/api/project-backups/export"',
    '"/api/project-backups/restore/preview"',
    '"/api/project-backups/restore/apply"',
    '"/api/project-backups/restore/records"',
    'ProjectBackupPackage',
    'ProjectRestorePlan',
    'ProjectRestoreResult',
    'RListProjectRestoreRecord'
  ], 'project backup schema')
})

test('keeps coverage and AI replay supporting flows wired', () => {
  const coverage = readSource('src/views/FieldCoverage.vue')
  const coverageApi = readSource('src/api/coverage.ts')
  const replay = readSource('src/views/AiReplay.vue')
  const replayApi = readSource('src/api/aiJob.ts')

  assertContains(coverage, [
    'projectStore.currentProjectId',
    'reportSqlCoverage(projectStore.currentProjectId, sqlText.value)',
    'reportDatabaseCoverage(databaseRequest())',
    'testDatabaseConnection(databaseRequest())',
    'listDatabaseTables(databaseRequest())',
    'connectionSecurity',
    'securityRiskLabel(connectionSecurity.riskLevel)',
    'connectionSecurity.recommendedSql',
    '只读安全诊断',
    'handleGenerateReport'
  ], 'FieldCoverage.vue')
  assertContains(coverageApi, [
    "request.post<unknown, FieldCoverageReport>('/coverage/sql'",
    "request.post<unknown, FieldCoverageReport>('/coverage/database'"
  ], 'coverage api')

  assertContains(replay, [
    'projectStore.currentProjectId',
    'listAiJobs(projectStore.currentProjectId',
    'getAiJobDetail(id)',
    'buildReplayJson',
    'copyText(activeDetail.replayCommand || \'\')',
    'copyText(replayJson)'
  ], 'AiReplay.vue')
  assertContains(replayApi, [
    "request.get<unknown, PageResult<AiJobRecordListItem>>('/ai-jobs'",
    'export function getAiJobDetail(id: number)'
  ], 'AI replay api')
})

test('keeps critical action labels and empty states visible', () => {
  const pageExpectations = [
    {
      path: 'src/views/SqlLint.vue',
      snippets: ['执行校验', '最近检查记录', '方言诊断', 'AI 模式', '修复模式', '修复策略', '最高风险', '请输入 SQL 并点击执行校验', '暂无检查记录', '已复制修正 SQL']
    },
    {
      path: 'src/views/ReverseImport.vue',
      snippets: ['生成预览', '生成差异', '确认导入', '方言诊断', '只读安全诊断', '请先创建并选择项目', '暂无表，请先加载', '当前筛选下暂无差异']
    },
    {
      path: 'src/views/FieldLibrary.vue',
      snippets: ['搜索字段名、显示名、别名、分类或注释', '批量维护', '暂无标准字段', '暂无字段变更', '回退字段变更']
    },
    {
      path: 'src/views/Generator.vue',
      snippets: ['生成 DDL', '方言诊断', '暂无模板字段', '当前项目暂无表模板', '预览 HTML', '下载 SQL', 'DDL 已生成']
    },
    {
      path: 'src/views/AiExport.vue',
      snippets: ['AI Context', '下载 Zip', '创建演示项目', '暂无预览', '请先选择项目']
    },
    {
      path: 'src/views/FieldCoverage.vue',
      snippets: ['生成报告', '只读安全诊断', '请先创建并选择项目', '暂无表，请先加载', '当前筛选下暂无字段', '暂无未纳管字段']
    },
    {
      path: 'src/views/AiReplay.vue',
      snippets: ['刷新记录', '暂无 AI 回放记录', '复制命令', '复制 JSON', '已复制']
    },
    {
      path: 'src/views/AiFeedback.vue',
      snippets: ['刷新反馈', '下一步动作', '高频字段信号', '规则问题排行', 'fixedSql 机会', '标准化信号', '请先创建并选择项目']
    },
    {
      path: 'src/views/AiProfile.vue',
      snippets: ['AI 任务模式', '当前模式', '可用模式', '模式详情', '推荐命令', '请选择项目后查看 AI 任务模式']
    },
    {
      path: 'src/views/StandardCandidate.vue',
      snippets: ['新建候选', '刷新', '暂无标准候选', '采纳', '合并', '延后', '忽略', '请先创建并选择项目']
    },
    {
      path: 'src/views/ProjectBackup.vue',
      snippets: ['导出备份 JSON', '预览恢复', '确认恢复', 'password/token/source rows', '暂无恢复记录']
    }
  ]

  for (const expectation of pageExpectations) {
    assertContains(readSource(expectation.path), expectation.snippets, expectation.path)
  }
})
