<template>
  <div class="reverse-page">
    <div class="page-header">
      <div>
        <h2>反向导入</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :disabled="!canGeneratePreview" :loading="previewLoading" @click="handleGeneratePreview">
          <el-icon><View /></el-icon>
          生成预览
        </el-button>
        <el-button
          v-if="activeMode === 'database'"
          type="success"
          :disabled="!canImportCandidates"
          :loading="importLoading"
          @click="handleImportCandidates"
        >
          <el-icon><Check /></el-icon>
          确认导入
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <el-tabs v-model="activeMode" class="mode-tabs">
        <el-tab-pane label="SQL DDL" name="sql">
          <section class="input-section">
            <div class="input-toolbar">
              <el-upload accept=".sql" :auto-upload="false" :show-file-list="false" :on-change="handleFileChange">
                <el-button>
                  <el-icon><Upload /></el-icon>
                  读取 SQL 文件
                </el-button>
              </el-upload>
              <el-button :disabled="!sqlText" @click="clearSql">清空</el-button>
            </div>
            <el-input
              v-model="sqlText"
              type="textarea"
              :rows="12"
              spellcheck="false"
              placeholder="CREATE TABLE ..."
            />
          </section>
        </el-tab-pane>

        <el-tab-pane label="数据库直连" name="database">
          <section class="input-section database-flow">
            <el-steps :active="databaseStep" align-center finish-status="success" class="db-steps">
              <el-step title="连接信息" />
              <el-step title="选择表" />
              <el-step title="预览确认" />
              <el-step title="导入结果" />
            </el-steps>

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
              class="db-hint"
              type="info"
              :closable="false"
              show-icon
              title="连接信息只用于本次请求，DataSpec 不保存数据库密码，也不会修改源数据库。"
            />
          </section>
        </el-tab-pane>
      </el-tabs>

      <section v-if="importResult" class="result-section import-result">
        <div class="section-header">
          <h3>导入结果</h3>
          <div class="result-actions">
            <el-tag type="success">已处理</el-tag>
            <el-button size="small" type="primary" plain @click="goToFieldLibrary">查看字段库</el-button>
          </div>
        </div>
        <div class="result-grid">
          <div>
            <span class="result-number">{{ importResult.importedCount ?? 0 }}</span>
            <span>新增字段</span>
          </div>
          <div>
            <span class="result-number">{{ importResult.skippedCount ?? 0 }}</span>
            <span>跳过字段</span>
          </div>
        </div>
        <div class="import-lists">
          <div>
            <div class="list-title">新增</div>
            <el-tag
              v-for="field in importResult.importedFields ?? []"
              :key="field"
              class="field-chip"
              type="success"
              effect="plain"
            >
              {{ field }}
            </el-tag>
            <span v-if="!(importResult.importedFields?.length)" class="empty-inline">无</span>
          </div>
          <div>
            <div class="list-title">跳过</div>
            <el-tag
              v-for="field in importResult.skippedFields ?? []"
              :key="field"
              class="field-chip"
              type="warning"
              effect="plain"
            >
              {{ field }}
            </el-tag>
            <span v-if="!(importResult.skippedFields?.length)" class="empty-inline">无</span>
          </div>
        </div>
      </section>

      <section v-if="preview" class="result-section">
        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.key" class="summary-item">
            <div class="summary-label">{{ item.label }}</div>
            <div class="summary-value">{{ item.value }}</div>
          </div>
        </div>

        <el-tabs class="result-tabs">
          <el-tab-pane label="字段候选">
            <template v-if="activeMode === 'database'">
              <div class="candidate-toolbar">
                <span>已选 {{ selectedCandidateCount }} / {{ candidateTotal }}</span>
                <div>
                  <el-button size="small" :disabled="candidateTotal === 0" @click="selectAllCandidates">全选候选</el-button>
                  <el-button size="small" :disabled="selectedCandidateCount === 0" @click="clearSelectedCandidates">清空候选</el-button>
                </div>
              </div>

              <el-empty v-if="candidateGroups.length === 0" description="暂无字段候选" />
              <el-collapse v-else class="candidate-groups">
                <el-collapse-item
                  v-for="group in candidateGroups"
                  :key="group.tableName"
                  :name="group.tableName"
                >
                  <template #title>
                    <span class="group-title">{{ group.tableName }}</span>
                    <el-tag size="small" effect="plain">{{ group.candidates.length }} 个字段</el-tag>
                  </template>
                  <el-table :data="group.candidates" stripe>
                    <el-table-column label="" width="56">
                      <template #default="{ row }">
                        <el-checkbox
                          :model-value="isCandidateSelected(row)"
                          @change="handleCandidateCheck(row, $event)"
                        />
                      </template>
                    </el-table-column>
                    <el-table-column prop="columnName" label="字段" min-width="140" />
                    <el-table-column prop="dataType" label="类型" min-width="130" />
                    <el-table-column label="空值" width="90">
                      <template #default="{ row }">{{ row.nullable ? '可空' : '非空' }}</template>
                    </el-table-column>
                    <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
                  </el-table>
                </el-collapse-item>
              </el-collapse>
            </template>
            <el-table v-else :data="preview.fieldCandidates ?? []" stripe empty-text="暂无字段候选">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="dataType" label="类型" min-width="130" />
              <el-table-column label="空值" width="90">
                <template #default="{ row }">{{ row.nullable ? '可空' : '非空' }}</template>
              </el-table-column>
              <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="缺注释">
            <el-table :data="preview.missingComments ?? []" stripe empty-text="暂无缺注释项">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column label="对象" width="100">
                <template #default="{ row }">{{ row.targetType === 'table' ? '表' : '字段' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="非标准字段">
            <el-table :data="preview.nonStandardFields ?? []" stripe empty-text="暂无非标准字段">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="recommendedName" label="建议名" min-width="140" />
              <el-table-column prop="reason" label="原因" min-width="220" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="解析表">
            <el-table :data="tableRows" stripe empty-text="暂无解析结果">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="dataType" label="类型" min-width="130" />
              <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Check, Connection, Refresh, Search, Upload, View } from '@element-plus/icons-vue'
import {
  importDatabaseCandidates,
  listDatabaseTables,
  previewDatabaseReverseImport,
  previewReverseImport,
  testDatabaseConnection
} from '@/api/reverseImport'
import { useProjectStore } from '@/stores/project'
import {
  buildCandidateKey,
  filterDatabaseTables,
  groupFieldCandidatesByTable,
  mergeSelectedTableNames,
  pickSelectedCandidates
} from '@/utils/reverseImportSelection'
import type {
  DatabaseConnectionReq,
  DatabaseImportResult,
  DatabaseTableInfo,
  FieldCandidate,
  ReverseImportPreview
} from '@/types'

type ReverseImportMode = 'sql' | 'database'
type ConnectionStatus = 'idle' | 'success' | 'error'

const projectStore = useProjectStore()
const router = useRouter()
const activeMode = ref<ReverseImportMode>('sql')
const sqlText = ref('')
const preview = ref<ReverseImportPreview | null>(null)
const importResult = ref<DatabaseImportResult | null>(null)
const previewLoading = ref(false)
const testLoading = ref(false)
const tableLoading = ref(false)
const importLoading = ref(false)
const databaseTables = ref<DatabaseTableInfo[]>([])
const tableSearch = ref('')
const connectionStatus = ref<ConnectionStatus>('idle')
const connectionMessage = ref('')
const selectedCandidateKeys = ref<Set<string>>(new Set())
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
const canPreviewSql = computed(() => hasProject.value && sqlText.value.trim().length > 0)
const canUseDatabaseConnection = computed(() =>
  hasProject.value
  && Boolean(dbForm.databaseType)
  && Boolean(dbForm.host?.trim())
  && Boolean(dbForm.databaseName?.trim())
  && Boolean(dbForm.username?.trim())
)
const canPreviewDatabase = computed(() =>
  canUseDatabaseConnection.value && Boolean(dbForm.tableNames?.length)
)
const canGeneratePreview = computed(() =>
  activeMode.value === 'sql' ? canPreviewSql.value : canPreviewDatabase.value
)
const canImportCandidates = computed(() =>
  activeMode.value === 'database' && selectedCandidateCount.value > 0
)
const filteredDatabaseTables = computed(() =>
  filterDatabaseTables(databaseTables.value, tableSearch.value)
)
const selectedTableCount = computed(() => dbForm.tableNames?.length ?? 0)
const candidateTotal = computed(() => preview.value?.fieldCandidates?.length ?? 0)
const selectedCandidateCount = computed(() => selectedCandidateKeys.value.size)
const selectedFieldCandidates = computed(() =>
  pickSelectedCandidates(preview.value?.fieldCandidates ?? [], selectedCandidateKeys.value)
)
const candidateGroups = computed(() =>
  groupFieldCandidatesByTable(preview.value?.fieldCandidates ?? [])
)
const databaseStep = computed(() => {
  if (importResult.value) {
    return 3
  }
  if (preview.value) {
    return 2
  }
  if (selectedTableCount.value > 0) {
    return 1
  }
  return 0
})
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
  { key: 'tables', label: '表', value: preview.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: preview.value?.summary?.columnCount ?? 0 },
  { key: 'candidates', label: '字段候选', value: preview.value?.summary?.candidateCount ?? 0 },
  { key: 'comments', label: '缺注释', value: preview.value?.summary?.missingCommentCount ?? 0 },
  { key: 'nonStandard', label: '非标准字段', value: preview.value?.summary?.nonStandardFieldCount ?? 0 }
])
const tableRows = computed(() =>
  (preview.value?.tables ?? []).flatMap((table) =>
    (table.columns ?? []).map((column) => ({
      tableName: table.name,
      columnName: column.name,
      dataType: column.dataType,
      comment: column.comment
    }))
  )
)

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    resetResults()
    databaseTables.value = []
    dbForm.tableNames = []
    tableSearch.value = ''
    resetConnectionStatus()
  }
)

watch(activeMode, () => {
  resetResults()
})

watch(
  () => [...(dbForm.tableNames ?? [])],
  () => {
    resetResults()
  }
)

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
    resetConnectionStatus()
    databaseTables.value = []
    dbForm.tableNames = []
    tableSearch.value = ''
    resetResults()
  }
)

function resetResults() {
  preview.value = null
  importResult.value = null
  selectedCandidateKeys.value = new Set()
}

function resetConnectionStatus() {
  connectionStatus.value = 'idle'
  connectionMessage.value = ''
}

function clearSql() {
  sqlText.value = ''
  resetResults()
}

function databaseRequest(): DatabaseConnectionReq {
  return {
    ...dbForm,
    projectId: projectStore.currentProjectId ?? undefined,
    tableNames: [...(dbForm.tableNames ?? [])]
  }
}

async function handleGeneratePreview() {
  if (activeMode.value === 'sql') {
    await handleSqlPreview()
    return
  }
  await handleDatabasePreview()
}

async function handleSqlPreview() {
  if (!projectStore.currentProjectId || !sqlText.value.trim()) {
    return
  }
  previewLoading.value = true
  try {
    preview.value = await previewReverseImport(projectStore.currentProjectId, sqlText.value)
    importResult.value = null
  } finally {
    previewLoading.value = false
  }
}

async function handleDatabasePreview() {
  if (!canPreviewDatabase.value) {
    return
  }
  previewLoading.value = true
  try {
    preview.value = await previewDatabaseReverseImport(databaseRequest())
    selectAllCandidates()
    importResult.value = null
  } finally {
    previewLoading.value = false
  }
}

async function handleTestConnection() {
  if (!canUseDatabaseConnection.value) {
    return
  }
  testLoading.value = true
  try {
    const result = await testDatabaseConnection(databaseRequest())
    if (result.success) {
      connectionStatus.value = 'success'
      connectionMessage.value = result.message || '连接成功'
      ElMessage.success(result.message || '连接成功')
    } else {
      connectionStatus.value = 'error'
      connectionMessage.value = result.message || '连接失败'
      ElMessage.error(result.message || '连接失败')
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
    resetResults()
    ElMessage.success(`已加载 ${databaseTables.value.length} 张表`)
  } finally {
    tableLoading.value = false
  }
}

async function handleImportCandidates() {
  if (!projectStore.currentProjectId || selectedFieldCandidates.value.length === 0) {
    ElMessage.warning('请选择要导入的字段候选')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将导入 ${selectedFieldCandidates.value.length} 个字段候选到当前项目字段库。`,
      '确认导入数标',
      {
        type: 'warning',
        confirmButtonText: '确认导入',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  importLoading.value = true
  try {
    importResult.value = await importDatabaseCandidates(
      projectStore.currentProjectId,
      selectedFieldCandidates.value
    )
    ElMessage.success(`导入 ${importResult.value.importedCount ?? 0} 个字段，跳过 ${importResult.value.skippedCount ?? 0} 个字段`)
  } finally {
    importLoading.value = false
  }
}

function handleFileChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) {
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    sqlText.value = String(reader.result ?? '')
    resetResults()
    ElMessage.success('SQL 已读取')
  }
  reader.readAsText(file, 'utf-8')
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
}

function selectAllCandidates() {
  selectedCandidateKeys.value = new Set(
    (preview.value?.fieldCandidates ?? []).map((candidate) => buildCandidateKey(candidate))
  )
}

function clearSelectedCandidates() {
  selectedCandidateKeys.value = new Set()
}

function toggleCandidate(candidate: FieldCandidate, checked: boolean) {
  const next = new Set(selectedCandidateKeys.value)
  const key = buildCandidateKey(candidate)
  if (checked) {
    next.add(key)
  } else {
    next.delete(key)
  }
  selectedCandidateKeys.value = next
}

function isCandidateSelected(candidate: FieldCandidate) {
  return selectedCandidateKeys.value.has(buildCandidateKey(candidate))
}

function handleCandidateCheck(candidate: FieldCandidate, checked: boolean | string | number) {
  toggleCandidate(candidate, Boolean(checked))
}

function goToFieldLibrary() {
  router.push('/fields')
}
</script>

<style scoped>
.reverse-page {
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

.db-steps {
  padding: 12px 4px 4px;
}

.db-workbench {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(320px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.db-panel {
  min-width: 0;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fcfcfd;
}

.result-section {
  border-top: 1px solid #ebeef5;
  margin-top: 16px;
}

.input-toolbar {
  margin-bottom: 12px;
}

.db-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
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

.db-hint {
  margin-top: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
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
  font-size: 26px;
  font-weight: 700;
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

.result-grid {
  display: flex;
  gap: 24px;
  color: #4b5563;
}

.result-number {
  margin-right: 6px;
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.import-lists {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
}

.list-title {
  margin-bottom: 8px;
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
}

.field-chip {
  margin: 0 6px 6px 0;
}

.empty-inline {
  color: #9ca3af;
  font-size: 13px;
}

.candidate-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #4b5563;
  font-size: 13px;
}

.candidate-groups {
  border-top: 1px solid #ebeef5;
}

.group-title {
  margin-right: 8px;
  color: #1f2937;
  font-weight: 600;
}

.small-empty {
  padding: 28px 0;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .db-workbench {
    grid-template-columns: 1fr;
  }

  .db-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .table-tools,
  .import-lists {
    grid-template-columns: 1fr;
  }

  .candidate-toolbar,
  .result-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
