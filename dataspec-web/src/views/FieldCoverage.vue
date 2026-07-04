<template>
  <div class="coverage-page">
    <div class="page-header">
      <div>
        <h2>覆盖率报告</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button plain :disabled="!hasProject" @click="handleCopyCoverageLink">
          <el-icon><Link /></el-icon>
          复制链接
        </el-button>
        <el-button type="primary" :disabled="!canGenerateReport" :loading="reportLoading" @click="handleGenerateReport">
          <el-icon><DataAnalysis /></el-icon>
          生成报告
        </el-button>
      </div>
    </div>

    <ProjectRequired
      v-if="!hasProject"
      :has-project="hasProject"
      title="请先创建并选择项目"
      @action="goProjects"
    />

    <template v-else>
      <el-tabs v-model="activeMode" class="mode-tabs">
        <el-tab-pane label="数据库直连" name="database">
          <section class="input-section database-flow">
            <div class="db-workbench">
              <div class="db-panel">
                <div class="section-header compact-header">
                  <h3>连接信息</h3>
                  <el-tag :type="connectionTagType" effect="plain">{{ connectionStatusText }}</el-tag>
                </div>
                <el-form class="db-form" label-width="92px">
                  <el-form-item label="数据库">
                    <el-select v-model="dbForm.databaseType" class="form-control" @change="handleDatabaseTypeChange">
                      <el-option label="PostgreSQL" value="postgresql" />
                      <el-option label="MySQL" value="mysql" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="主机">
                    <el-input v-model="dbForm.host" class="form-control" placeholder="localhost" />
                  </el-form-item>
                  <el-form-item label="端口">
                    <el-input-number v-model="dbForm.port" class="form-control number-input" :min="1" :max="65535" />
                  </el-form-item>
                  <el-form-item label="数据库名">
                    <el-input v-model="dbForm.databaseName" class="form-control" placeholder="dataspec_demo" />
                  </el-form-item>
                  <el-form-item label="Schema">
                    <el-input v-model="dbForm.schemaName" class="form-control" placeholder="public / database" />
                  </el-form-item>
                  <el-form-item label="用户名">
                    <el-input v-model="dbForm.username" class="form-control" autocomplete="off" />
                  </el-form-item>
                  <el-form-item label="密码">
                    <el-input v-model="dbForm.password" class="form-control" type="password" show-password autocomplete="new-password" />
                  </el-form-item>
                </el-form>
                <div class="input-toolbar">
                  <el-button :disabled="!canUseDatabaseConnection" :loading="testLoading" @click="handleTestConnection">
                    <el-icon><Connection /></el-icon>
                    测试连接
                  </el-button>
                  <el-button :disabled="!canUseDatabaseConnection" :loading="tableLoading" @click="handleLoadTables">
                    <el-icon><Refresh /></el-icon>
                    加载表
                  </el-button>
                </div>
                <div v-if="connectionSecurity || connectionHealth" class="security-diagnostic">
                  <div v-if="connectionHealth" class="security-section">
                    <div class="security-header">
                      <span>连接健康画像</span>
                      <el-tag :type="connectionStatusTagType(connectionHealth.connectionStatus)" effect="plain">
                        {{ connectionStatusLabel(connectionHealth.connectionStatus) }}
                      </el-tag>
                    </div>
                    <div class="security-summary">{{ databaseHealthSummary(connectionHealth) }}</div>
                    <div class="security-meta">
                      <span v-if="connectionHealth.failureCategory">失败分类：{{ failureCategoryLabel(connectionHealth.failureCategory) }}</span>
                      <span v-if="connectionHealth.latencyMs !== undefined">耗时：{{ connectionHealth.latencyMs }}ms</span>
                      <span v-if="connectionHealth.connectionStatus === 'FAILED'">{{ retryableLabel(connectionHealth.retryable) }}</span>
                      <span>{{ metadataReadableLabel(connectionHealth.capability?.metadataReadable) }}</span>
                    </div>
                    <div v-if="connectionHealth.capability" class="security-meta">
                      <span>Schema：{{ capabilitySupportLabel(connectionHealth.capability.schemaSupport) }}</span>
                      <span>Comment：{{ capabilitySupportLabel(connectionHealth.capability.commentSupport) }}</span>
                      <span>Index：{{ capabilitySupportLabel(connectionHealth.capability.indexSupport) }}</span>
                    </div>
                    <div v-if="connectionHealth.requiredPrivileges?.length" class="security-line">
                      所需权限：{{ connectionHealth.requiredPrivileges.join('、') }}
                    </div>
                    <div v-if="connectionHealth.warnings?.length" class="security-list">
                      <div v-for="warning in connectionHealth.warnings" :key="warning" class="security-line">
                        {{ warning }}
                      </div>
                    </div>
                    <div v-if="connectionHealth.nextActions?.length" class="security-list">
                      <div v-for="action in connectionHealth.nextActions" :key="action" class="security-line muted">
                        {{ action }}
                      </div>
                    </div>
                  </div>
                  <div v-if="connectionSecurity" class="security-section">
                    <div class="security-header">
                    <span>只读安全诊断</span>
                    <el-tag :type="securityRiskTagType(connectionSecurity.riskLevel)" effect="plain">
                      {{ securityRiskLabel(connectionSecurity.riskLevel) }}
                    </el-tag>
                    </div>
                    <div class="security-summary">{{ databaseSecuritySummary(connectionSecurity) }}</div>
                    <div class="security-meta">
                      <span>{{ readOnlyLabel(connectionSecurity.readOnly) }}</span>
                      <span>{{ writeRiskLabel(connectionSecurity.writeRisk) }}</span>
                      <span>{{ connectionSecurity.accessibleSchemaCount ?? 0 }} 个 schema</span>
                      <span>{{ connectionSecurity.accessibleTableCount ?? 0 }} 张表</span>
                    </div>
                    <div v-if="connectionSecurity.warnings?.length" class="security-list">
                      <div v-for="warning in connectionSecurity.warnings" :key="warning" class="security-line">
                        {{ warning }}
                      </div>
                    </div>
                    <div v-if="connectionSecurity.recommendedActions?.length" class="security-list">
                      <div v-for="action in connectionSecurity.recommendedActions" :key="action" class="security-line muted">
                        {{ action }}
                      </div>
                    </div>
                    <pre v-if="connectionSecurity.recommendedSql?.length" class="security-sql">{{ connectionSecurity.recommendedSql.join('\n') }}</pre>
                  </div>
                </div>
              </div>

              <div class="db-panel">
                <div class="section-header compact-header">
                  <h3>选择表</h3>
                  <el-tag type="info" effect="plain">已选 {{ selectedTableCount }} / {{ databaseTables.length }}</el-tag>
                </div>
                <div class="table-tools">
                  <el-input
                    v-model="tableSearch"
                    :prefix-icon="Search"
                    clearable
                    placeholder="搜索 schema、表名或注释"
                  />
                  <el-button :disabled="filteredDatabaseTables.length === 0" @click="selectVisibleTables">全选当前</el-button>
                  <el-button :disabled="selectedTableCount === 0" @click="clearSelectedTables">清空</el-button>
                </div>

                <el-empty v-if="databaseTables.length === 0" class="small-empty" description="暂无表，请先加载" />
                <el-empty v-else-if="filteredDatabaseTables.length === 0" class="small-empty" description="没有匹配的表" />
                <el-checkbox-group v-else v-model="dbForm.tableNames" class="table-check-list">
                  <el-checkbox
                    v-for="table in filteredDatabaseTables"
                    :key="tableKey(table)"
                    :value="table.tableName || ''"
                    :disabled="!table.tableName"
                    class="table-check-item"
                  >
                    <span class="table-title">{{ tableLabel(table) }}</span>
                    <span v-if="table.comment" class="table-comment">{{ table.comment }}</span>
                  </el-checkbox>
                </el-checkbox-group>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="覆盖率报告只读取数据库 metadata，不扫描业务数据行，不保存数据库密码，也不会修改源数据库。"
            />
          </section>
        </el-tab-pane>

        <el-tab-pane label="SQL DDL" name="sql">
          <section class="input-section">
            <el-input
              v-model="sqlText"
              type="textarea"
              :rows="12"
              spellcheck="false"
              placeholder="CREATE TABLE ..."
            />
          </section>
        </el-tab-pane>
      </el-tabs>

      <StateBlock
        v-if="reportState.errorMessage.value"
        type="error"
        title="覆盖率报告生成失败"
        :description="reportState.errorMessage.value"
        :suggested-action="reportState.suggestedAction.value"
        :docs-ref="reportState.docsRef.value"
        action-text="重试"
        @action="handleGenerateReport"
      />

      <section v-else-if="report" class="result-section">
        <div class="coverage-hero">
          <el-progress
            type="dashboard"
            :percentage="report.summary?.coverageRate ?? 0"
            :stroke-width="12"
          >
            <template #default="{ percentage }">
              <span class="coverage-rate">{{ formatCoverageRate(percentage) }}</span>
            </template>
          </el-progress>
          <div class="summary-grid">
            <div v-for="item in summaryItems" :key="item.key" class="summary-item">
              <div class="summary-label">{{ item.label }}</div>
              <div class="summary-value">{{ item.value }}</div>
            </div>
          </div>
        </div>

        <div class="evidence-actions">
          <el-button size="small" :loading="evidenceLoading" @click="handleCopyCoverageEvidence">
            复制证据 JSON
          </el-button>
          <el-button size="small" type="primary" :loading="evidenceLoading" @click="handleDownloadCoverageEvidence">
            下载证据包
          </el-button>
        </div>

        <div class="result-layout">
          <div class="main-panel">
            <div class="section-header">
              <h3>字段明细</h3>
              <div class="filter-actions">
                <el-select v-model="tableFilter" size="small" class="filter-select">
                  <el-option label="全部表" value="ALL" />
                  <el-option
                    v-for="table in report.tables ?? []"
                    :key="table.tableName"
                    :label="table.tableName"
                    :value="table.tableName || ''"
                  />
                </el-select>
                <el-radio-group v-model="statusFilter" size="small">
                  <el-radio-button
                    v-for="option in statusOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio-button>
                </el-radio-group>
              </div>
            </div>

            <StateBlock
              v-if="filteredFields.length === 0"
              type="empty"
              title="当前筛选下暂无字段"
              description="可以切换表或状态筛选，或重新生成覆盖率报告。"
              action-text="重置筛选"
              @action="resetCoverageFilters"
            />
            <el-table v-else :data="filteredFields" stripe empty-text="当前筛选下暂无字段">
              <el-table-column prop="tableName" label="表" min-width="130" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag size="small" :type="coverageStatusTagType(row.status)" effect="plain">
                    {{ coverageStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="dataType" label="类型" min-width="120" />
              <el-table-column label="标准字段" min-width="150">
                <template #default="{ row }">
                  <div>{{ row.standardFieldName || row.recommendedFieldName || '-' }}</div>
                  <div v-if="row.standardDisplayName" class="muted-text">{{ row.standardDisplayName }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="注释" min-width="180" show-overflow-tooltip />
              <el-table-column prop="reason" label="原因" min-width="240" show-overflow-tooltip />
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'UNMANAGED' || row.status === 'POSSIBLE_DUPLICATE'"
                    size="small"
                    text
                    type="primary"
                    @click="goToFieldLibrary(row.columnName)"
                  >
                    查字段库
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="side-panel">
            <div class="section-header compact-header">
              <h3>未纳管排行</h3>
              <el-button size="small" text type="primary" @click="goToReverseImport">去反向导入</el-button>
            </div>
            <el-empty v-if="!(report.unmanagedRankings?.length)" class="small-empty" description="暂无未纳管字段" />
            <div v-else class="ranking-list">
              <div v-for="item in report.unmanagedRankings" :key="item.columnName" class="ranking-item">
                <div class="ranking-title">
                  <code>{{ item.columnName }}</code>
                  <el-tag size="small" effect="plain">{{ item.count }} 次</el-tag>
                </div>
                <div class="muted-text">{{ item.tables?.join(', ') }}</div>
                <div v-if="item.recommendedFieldName" class="ranking-suggestion">
                  建议复用：{{ item.recommendedFieldName }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection, DataAnalysis, Link, Refresh, Search } from '@element-plus/icons-vue'
import { reportDatabaseCoverage, reportSqlCoverage } from '@/api/coverage'
import { downloadEvidencePackage, generateEvidencePackage } from '@/api/evidence'
import { listDatabaseTables, testDatabaseConnection } from '@/api/reverseImport'
import ProjectRequired from '@/components/ProjectRequired.vue'
import StateBlock from '@/components/StateBlock.vue'
import { useRequestState } from '@/composables/useRequestState'
import { useProjectStore } from '@/stores/project'
import {
  coverageStatusLabel,
  coverageStatusTagType,
  filterCoverageFields,
  formatCoverageRate
} from '@/utils/fieldCoverageDisplay'
import {
  filterDatabaseTables,
  mergeSelectedTableNames
} from '@/utils/reverseImportSelection'
import {
  capabilitySupportLabel,
  connectionStatusLabel,
  connectionStatusTagType,
  databaseHealthSummary,
  databaseSecuritySummary,
  failureCategoryLabel,
  metadataReadableLabel,
  readOnlyLabel,
  retryableLabel,
  securityRiskLabel,
  securityRiskTagType,
  writeRiskLabel
} from '@/utils/databaseSecurityDiagnostic'
import { copyRouteUrl, readEnumQuery, readStringQuery, replaceRouteQuery } from '@/utils/urlState'
import type {
  DatabaseConnectionHealthDiagnostic,
  DatabaseConnectionReq,
  DatabaseConnectionSecurityDiagnostic,
  DatabaseTableInfo,
  FieldCoverageReport,
  FieldCoverageStatus
} from '@/types'

type CoverageMode = 'database' | 'sql'
type ConnectionStatus = 'idle' | 'success' | 'error'
type StatusFilter = 'ALL' | FieldCoverageStatus

const projectStore = useProjectStore()
const route = useRoute()
const router = useRouter()
const activeMode = ref<CoverageMode>('database')
const sqlText = ref('')
const reportState = useRequestState<FieldCoverageReport>()
const report = reportState.data
const evidenceLoading = ref(false)
const testLoading = ref(false)
const tableLoading = ref(false)
const databaseTables = ref<DatabaseTableInfo[]>([])
const tableSearch = ref('')
const tableFilter = ref('ALL')
const statusFilter = ref<StatusFilter>('ALL')
const connectionStatus = ref<ConnectionStatus>('idle')
const connectionMessage = ref('')
const connectionSecurity = ref<DatabaseConnectionSecurityDiagnostic | null>(null)
const connectionHealth = ref<DatabaseConnectionHealthDiagnostic | null>(null)
const dbForm = reactive<DatabaseConnectionReq>({
  databaseType: 'postgresql',
  host: 'localhost',
  port: 5432,
  databaseName: '',
  schemaName: 'public',
  username: '',
  password: '',
  tableNames: []
})

const hasProject = computed(() => projectStore.currentProjectId !== null)
const reportLoading = computed(() => reportState.loading.value)
const canUseDatabaseConnection = computed(() =>
  hasProject.value
  && Boolean(dbForm.databaseType)
  && Boolean(dbForm.host?.trim())
  && Boolean(dbForm.databaseName?.trim())
  && Boolean(dbForm.username?.trim())
)
const canGenerateReport = computed(() => {
  if (!hasProject.value) {
    return false
  }
  if (activeMode.value === 'sql') {
    return Boolean(sqlText.value.trim())
  }
  return canUseDatabaseConnection.value && Boolean(dbForm.tableNames?.length)
})
const filteredDatabaseTables = computed(() => filterDatabaseTables(databaseTables.value, tableSearch.value))
const selectedTableCount = computed(() => dbForm.tableNames?.length ?? 0)
const connectionStatusText = computed(() => {
  if (connectionStatus.value === 'success') {
    return connectionMessage.value || '连接可用'
  }
  if (connectionStatus.value === 'error') {
    return connectionMessage.value || '连接失败'
  }
  return '未测试'
})
const connectionTagType = computed(() => {
  if (connectionStatus.value === 'success') {
    return 'success'
  }
  if (connectionStatus.value === 'error') {
    return 'danger'
  }
  return 'info'
})
const summaryItems = computed(() => [
  { key: 'tables', label: '表', value: report.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: report.value?.summary?.columnCount ?? 0 },
  { key: 'covered', label: '已覆盖', value: report.value?.summary?.coveredCount ?? 0 },
  { key: 'unmanaged', label: '未纳管', value: report.value?.summary?.unmanagedCount ?? 0 },
  { key: 'comments', label: '缺注释', value: report.value?.summary?.missingCommentCount ?? 0 },
  { key: 'duplicates', label: '疑似重复', value: report.value?.summary?.possibleDuplicateCount ?? 0 }
])
const statusOptions: Array<{ value: StatusFilter; label: string }> = [
  { value: 'ALL', label: '全部' },
  { value: 'STANDARD_MATCH', label: '标准命中' },
  { value: 'ALIAS_MATCH', label: '别名命中' },
  { value: 'MISSING_COMMENT', label: '缺注释' },
  { value: 'POSSIBLE_DUPLICATE', label: '疑似重复' },
  { value: 'UNMANAGED', label: '未纳管' }
]
const filteredFields = computed(() =>
  filterCoverageFields(report.value?.tables ?? [], tableFilter.value, statusFilter.value)
)

applyCoverageUrlState()

watch(
  () => projectStore.currentProjectId,
  () => {
    resetDatabaseConnectionState()
    resetReport()
  }
)

watch(activeMode, () => resetReport())

watch(
  () => [route.query.table, route.query.status],
  () => applyCoverageUrlState()
)

watch([tableFilter, statusFilter], () => {
  void syncCoverageUrlState()
})

watch(
  () => [
    dbForm.databaseType,
    dbForm.host,
    dbForm.port,
    dbForm.databaseName,
    dbForm.schemaName,
    dbForm.username,
    dbForm.password
  ],
  () => {
    resetDatabaseConnectionState()
    resetReport()
  }
)

function resetReport() {
  reportState.reset()
  applyCoverageUrlState()
}

function resetCoverageFilters() {
  tableFilter.value = 'ALL'
  statusFilter.value = 'ALL'
  void syncCoverageUrlState()
}

function resetDatabaseConnectionState() {
  databaseTables.value = []
  dbForm.tableNames = []
  tableSearch.value = ''
  connectionStatus.value = 'idle'
  connectionMessage.value = ''
  connectionSecurity.value = null
  connectionHealth.value = null
}

function databaseRequest(): DatabaseConnectionReq {
  return {
    ...dbForm,
    projectId: projectStore.currentProjectId ?? undefined,
    tableNames: [...(dbForm.tableNames ?? [])]
  }
}

async function handleGenerateReport() {
  if (!projectStore.currentProjectId || !canGenerateReport.value) {
    return
  }
  try {
    await reportState.run(() => activeMode.value === 'sql'
      ? reportSqlCoverage(projectStore.currentProjectId as number, sqlText.value)
      : reportDatabaseCoverage(databaseRequest()))
    applyCoverageUrlState()
  } catch {
    // 页面内 StateBlock 会展示可重试状态，避免只留下全局消息。
  }
}

async function syncCoverageUrlState(patch: Record<string, string | number | null> = {}) {
  await replaceRouteQuery(router, route, {
    projectId: projectStore.currentProjectId,
    table: tableFilter.value !== 'ALL' ? tableFilter.value : null,
    status: statusFilter.value !== 'ALL' ? statusFilter.value : null,
    ...patch
  })
}

function applyCoverageUrlState() {
  const table = readStringQuery(route.query, 'table')
  tableFilter.value = table || 'ALL'
  const nextStatus = readEnumQuery(route.query, 'status', statusOptions.map((item) => item.value))
  if (route.query.status && !nextStatus) {
    ElMessage.warning('链接中的覆盖率状态筛选无效，已恢复为全部')
    void syncCoverageUrlState({ status: null })
  }
  statusFilter.value = nextStatus ?? 'ALL'
}

async function handleCopyCoverageLink() {
  try {
    await syncCoverageUrlState()
    await copyRouteUrl(route, navigator.clipboard)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.error('复制失败，请手动复制浏览器地址')
  }
}

async function handleCopyCoverageEvidence() {
  const req = coverageEvidenceRequest()
  if (!req) {
    return
  }
  evidenceLoading.value = true
  try {
    const evidence = await generateEvidencePackage(req)
    await copyText(JSON.stringify(evidence, null, 2))
    ElMessage.success('已复制证据 JSON')
  } finally {
    evidenceLoading.value = false
  }
}

async function handleDownloadCoverageEvidence() {
  const req = coverageEvidenceRequest()
  if (!req) {
    return
  }
  evidenceLoading.value = true
  try {
    saveBlob(await downloadEvidencePackage(req), 'dataspec-coverage-evidence.zip')
    ElMessage.success('已下载证据包')
  } finally {
    evidenceLoading.value = false
  }
}

function coverageEvidenceRequest() {
  if (!projectStore.currentProjectId || !report.value) {
    ElMessage.warning('请先生成覆盖率报告')
    return null
  }
  return {
    projectId: projectStore.currentProjectId,
    sourceType: 'COVERAGE_REPORT',
    sourceTitle: activeMode.value === 'sql' ? 'SQL 覆盖率报告' : '数据库覆盖率报告',
    coverageReport: report.value,
    payloadSummary: {
      mode: activeMode.value,
      tableFilter: tableFilter.value,
      statusFilter: statusFilter.value
    }
  } as const
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function goProjects() {
  router.push('/projects')
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return
    }
  } catch {
    // 非安全上下文或浏览器策略拒绝时，降级到临时 textarea 复制。
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

async function handleTestConnection() {
  if (!canUseDatabaseConnection.value) {
    return
  }
  testLoading.value = true
  try {
    const result = await testDatabaseConnection(databaseRequest())
    connectionStatus.value = result.success ? 'success' : 'error'
    connectionMessage.value = result.message || (result.success ? '连接成功' : '连接失败')
    connectionHealth.value = result.health ?? null
    connectionSecurity.value = result.success ? result.security ?? null : null
    if (result.success) {
      ElMessage.success(connectionMessage.value)
    } else {
      ElMessage.error(connectionMessage.value)
    }
  } finally {
    testLoading.value = false
  }
}

async function handleLoadTables() {
  if (!canUseDatabaseConnection.value) {
    return
  }
  tableLoading.value = true
  try {
    databaseTables.value = await listDatabaseTables(databaseRequest())
    dbForm.tableNames = []
    tableSearch.value = ''
    connectionStatus.value = 'success'
    connectionMessage.value = `已加载 ${databaseTables.value.length} 张表`
    if (connectionHealth.value?.connectionStatus === 'FAILED') {
      connectionHealth.value = null
    }
    resetReport()
    ElMessage.success(connectionMessage.value)
  } finally {
    tableLoading.value = false
  }
}

function handleDatabaseTypeChange(value: string) {
  dbForm.port = value === 'mysql' ? 3306 : 5432
  dbForm.schemaName = value === 'mysql' ? '' : 'public'
}

function tableKey(table: DatabaseTableInfo) {
  return `${table.schemaName ?? ''}.${table.tableName ?? ''}`
}

function tableLabel(table: DatabaseTableInfo) {
  return table.schemaName ? `${table.schemaName}.${table.tableName}` : table.tableName
}

function selectVisibleTables() {
  dbForm.tableNames = mergeSelectedTableNames(dbForm.tableNames, filteredDatabaseTables.value)
}

function clearSelectedTables() {
  dbForm.tableNames = []
  resetReport()
}

function goToFieldLibrary(keyword?: string) {
  router.push({
    path: '/fields',
    query: {
      projectId: projectStore.currentProjectId ?? undefined,
      ...(keyword ? { keyword } : {})
    }
  })
}

function goToReverseImport() {
  router.push('/reverse-import')
}
</script>

<style scoped>
.coverage-page {
  min-height: calc(100vh - 140px);
  padding: 20px;
  background: #fff;
  border-radius: 4px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0;
  font-weight: 600;
  color: #1f2937;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.header-actions,
.input-toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.mode-tabs {
  border-top: 1px solid #ebeef5;
}

.input-section,
.result-section {
  padding-top: 16px;
}

.database-flow {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.db-workbench {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(320px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.db-panel,
.main-panel,
.side-panel {
  min-width: 0;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fcfcfd;
}

.db-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(240px, 1fr));
  gap: 2px 18px;
}

.form-control {
  width: 100%;
}

.number-input {
  display: block;
}

.table-tools {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto auto;
  gap: 10px;
  align-items: center;
}

.table-check-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 8px;
  max-height: 318px;
  margin-top: 12px;
  overflow: auto;
}

.table-check-item {
  height: auto;
  min-height: 44px;
  margin-right: 0;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.table-check-item :deep(.el-checkbox__label) {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.table-title {
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-comment {
  overflow: hidden;
  max-width: 100%;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.security-diagnostic {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  border-left: 3px solid #dcdfe6;
  background: #fff;
}

.security-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.security-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.security-summary,
.security-meta,
.security-line {
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
}

.security-meta {
  display: flex;
  gap: 8px 14px;
  flex-wrap: wrap;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.security-line.muted {
  color: #6b7280;
}

.security-sql {
  max-height: 160px;
  margin: 0;
  padding: 8px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #f8fafc;
  color: #374151;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.coverage-hero {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  margin-bottom: 16px;
}

.evidence-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.coverage-rate {
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.summary-label {
  color: #6b7280;
  font-size: 13px;
}

.summary-value {
  margin-top: 6px;
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

.result-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.compact-header {
  margin-bottom: 12px;
}

.filter-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.filter-select {
  width: 180px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ranking-item {
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.ranking-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.ranking-suggestion {
  margin-top: 6px;
  color: #1d4ed8;
  font-size: 13px;
}

.muted-text {
  color: #6b7280;
  font-size: 12px;
}

.small-empty {
  padding: 28px 0;
}

code {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

@media (max-width: 1000px) {
  .db-workbench,
  .result-layout,
  .coverage-hero {
    grid-template-columns: 1fr;
  }

  .db-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-header,
  .section-header,
  .filter-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-tools {
    grid-template-columns: 1fr;
  }
}
</style>
