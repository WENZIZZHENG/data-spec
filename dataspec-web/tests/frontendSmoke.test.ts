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
    { path: '/sql-lint', view: 'SqlLint.vue', title: 'SQL 校验' },
    { path: '/generator', view: 'Generator.vue', title: '生成器' },
    { path: '/ai-export', view: 'AiExport.vue', title: 'AI 规则导出' },
    { path: '/ai-replay', view: 'AiReplay.vue', title: 'AI 回放' },
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

test('keeps global project selector backed by the project store', () => {
  const app = readSource('src/App.vue')
  const projectStore = readSource('src/stores/project.ts')

  assertContains(app, [
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

test('keeps SQL lint fixed SQL and record history flow wired', () => {
  const view = readSource('src/views/SqlLint.vue')
  const api = readSource('src/api/lint.ts')

  assertContains(view, [
    "import { getLintRecord, lintSql, listLintRecords } from '@/api/lint'",
    'projectId: projectStore.currentProjectId ?? undefined',
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
      snippets: ['执行校验', '最近检查记录', '方言诊断', '请输入 SQL 并点击执行校验', '暂无检查记录', '已复制修正 SQL']
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
      path: 'src/views/ProjectBackup.vue',
      snippets: ['导出备份 JSON', '预览恢复', '确认恢复', 'password/token/source rows', '暂无恢复记录']
    }
  ]

  for (const expectation of pageExpectations) {
    assertContains(readSource(expectation.path), expectation.snippets, expectation.path)
  }
})
