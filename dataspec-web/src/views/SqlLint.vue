<template>
  <div class="sql-lint-page">
    <div class="page-header">
      <h2>SQL 校验</h2>
      <el-button type="primary" :loading="linting" @click="handleLint">
        <el-icon><CaretRight /></el-icon>
        执行校验
      </el-button>
    </div>

    <div class="lint-content">
      <div class="editor-panel">
        <div class="panel-title">SQL 编辑器</div>
        <div ref="editorContainer" class="editor-container"></div>
      </div>

      <div class="result-panel">
        <div class="panel-title">校验结果</div>
        <div class="result-content">
          <template v-if="lintResult">
            <div class="summary-row">
              <el-tag :type="issueTotal === 0 ? 'success' : 'danger'">
                共 {{ issueTotal }} 个问题
              </el-tag>
              <el-tag type="danger">错误 {{ lintResult.errorCount ?? 0 }}</el-tag>
              <el-tag type="warning">警告 {{ lintResult.warningCount ?? 0 }}</el-tag>
              <el-tag type="info">建议 {{ lintResult.suggestionCount ?? 0 }}</el-tag>
            </div>

            <div v-if="lintResult.fixedSql" class="fixed-sql-panel">
              <div class="fixed-sql-header">
                <span>修正 SQL</span>
                <el-button size="small" text type="primary" @click="handleCopySql">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
              <pre class="sql-code">{{ lintResult.fixedSql }}</pre>
            </div>

            <el-table :data="lintResult.issues ?? []" stripe style="width: 100%">
              <el-table-column prop="severity" label="级别" width="110">
                <template #default="{ row }">
                  <el-tag :type="severityType(row.severity)" size="small">
                    {{ severityLabel(row.severity) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="tableName" label="表" width="130" />
              <el-table-column prop="columnName" label="字段" width="130" />
              <el-table-column label="位置" width="100">
                <template #default="{ row }">
                  <el-button
                    v-if="row.line"
                    size="small"
                    text
                    type="primary"
                    @click="handleGoToIssue(row)"
                  >
                    {{ locationLabel(row) }}
                  </el-button>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="ruleCode" label="规则" width="180" />
              <el-table-column prop="message" label="描述" min-width="260" />
              <el-table-column label="建议" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  <span>{{ fixSuggestion(row) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else description="请输入 SQL 并点击执行校验" />
        </div>
      </div>
    </div>

    <div class="history-panel">
      <el-collapse v-model="historyActiveNames">
        <el-collapse-item name="records">
          <template #title>
            <div class="history-title">
              <span>最近检查记录</span>
              <el-tag size="small" type="info">{{ recordTotal }} 条</el-tag>
            </div>
          </template>

          <el-empty v-if="!projectStore.currentProjectId" description="请选择项目后查看记录" />
          <template v-else>
            <el-table
              v-loading="recordLoading"
              :data="records"
              stripe
              class="record-table"
              empty-text="暂无检查记录"
            >
              <el-table-column label="检查时间" min-width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column prop="errorCount" label="错误" width="90" />
              <el-table-column prop="warningCount" label="警告" width="90" />
              <el-table-column prop="suggestionCount" label="建议" width="90" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    text
                    type="primary"
                    :loading="recordDetailLoading && loadingRecordId === row.id"
                    @click="handleViewRecord(row.id)"
                  >
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-row">
              <el-pagination
                v-if="recordTotal > recordSize"
                background
                layout="prev, pager, next"
                :current-page="recordCurrent"
                :page-size="recordSize"
                :total="recordTotal"
                @current-change="handleRecordPageChange"
              />
            </div>
          </template>
        </el-collapse-item>
      </el-collapse>
    </div>

    <el-dialog v-model="recordDialogVisible" title="检查记录详情" width="860px">
      <div v-if="activeRecord?.record" class="record-detail">
        <div class="summary-row">
          <el-tag type="danger">错误 {{ activeRecord.record.errorCount ?? 0 }}</el-tag>
          <el-tag type="warning">警告 {{ activeRecord.record.warningCount ?? 0 }}</el-tag>
          <el-tag type="info">建议 {{ activeRecord.record.suggestionCount ?? 0 }}</el-tag>
          <span class="record-time">{{ formatDate(activeRecord.record.createdAt) }}</span>
        </div>

        <div class="detail-section">
          <div class="detail-title">原始 SQL</div>
          <pre class="sql-code compact">{{ activeRecord.record.originalSql }}</pre>
        </div>

        <div v-if="activeRecord.record.fixedSql" class="detail-section">
          <div class="detail-title">修正 SQL</div>
          <pre class="sql-code compact">{{ activeRecord.record.fixedSql }}</pre>
        </div>

        <div class="detail-section">
          <div class="detail-title">问题列表</div>
          <el-table :data="activeRecord.issues ?? []" stripe style="width: 100%">
            <el-table-column prop="severity" label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)" size="small">
                  {{ severityLabel(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tableName" label="表" width="120" />
            <el-table-column prop="columnName" label="字段" width="120" />
            <el-table-column label="位置" width="90">
              <template #default="{ row }">
                {{ locationLabel(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="ruleCode" label="规则" width="170" />
            <el-table-column prop="message" label="描述" min-width="240" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as monaco from 'monaco-editor'
import { getLintRecord, lintSql, listLintRecords } from '@/api/lint'
import { useProjectStore } from '@/stores/project'
import type { LintIssue, LintResult, RecordDetail, SqlCheckRecord } from '@/types'

const editorContainer = ref<HTMLElement>()
const lintResult = ref<LintResult | null>(null)
const linting = ref(false)
const records = ref<SqlCheckRecord[]>([])
const recordTotal = ref(0)
const recordCurrent = ref(1)
const recordSize = ref(10)
const recordLoading = ref(false)
const recordDetailLoading = ref(false)
const loadingRecordId = ref<number | null>(null)
const activeRecord = ref<RecordDetail | null>(null)
const recordDialogVisible = ref(false)
const historyActiveNames = ref<string[]>([])
const projectStore = useProjectStore()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const issueTotal = computed(() => {
  if (!lintResult.value) {
    return 0
  }
  return (
    (lintResult.value.errorCount ?? 0) +
    (lintResult.value.warningCount ?? 0) +
    (lintResult.value.suggestionCount ?? 0)
  )
})

onMounted(() => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: `CREATE TABLE users (
    id bigserial PRIMARY KEY,
    username varchar(50) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false
);`,
      language: 'sql',
      theme: 'vs-dark',
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 14,
      lineNumbers: 'on',
      scrollBeyondLastLine: false
    })
  }
  if (projectStore.currentProjectId) {
    void loadRecords()
  } else if (!projectStore.loading && projectStore.projects.length === 0) {
    void projectStore.loadProjects().then(() => loadRecords())
  }
})

onBeforeUnmount(() => {
  editor?.dispose()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    recordCurrent.value = 1
    void loadRecords()
  }
)

async function handleLint() {
  const sql = editor?.getValue() || ''
  if (!sql.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }

  linting.value = true
  try {
    lintResult.value = await lintSql({
      sql,
      projectId: projectStore.currentProjectId ?? undefined
    })
    recordCurrent.value = 1
    await loadRecords()
  } finally {
    linting.value = false
  }
}

async function loadRecords() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    records.value = []
    recordTotal.value = 0
    return
  }

  recordLoading.value = true
  try {
    const page = await listLintRecords(projectId, recordCurrent.value, recordSize.value)
    records.value = page.records ?? []
    recordTotal.value = page.total ?? 0
    recordCurrent.value = page.current ?? recordCurrent.value
    recordSize.value = page.size ?? recordSize.value
  } finally {
    recordLoading.value = false
  }
}

async function handleCopySql() {
  const fixedSql = lintResult.value?.fixedSql
  if (!fixedSql) {
    return
  }
  try {
    await navigator.clipboard.writeText(fixedSql)
    ElMessage.success('已复制修正 SQL')
  } catch {
    ElMessage.error('复制失败，请手动选择修正 SQL')
  }
}

async function handleViewRecord(id?: number) {
  if (!id) {
    return
  }
  recordDetailLoading.value = true
  loadingRecordId.value = id
  try {
    activeRecord.value = await getLintRecord(id)
    recordDialogVisible.value = true
  } finally {
    recordDetailLoading.value = false
    loadingRecordId.value = null
  }
}

function handleRecordPageChange(page: number) {
  recordCurrent.value = page
  void loadRecords()
}

function handleGoToIssue(issue: LintIssue) {
  if (!editor || !issue.line) {
    return
  }
  const position = {
    lineNumber: issue.line,
    column: issue.column ?? 1
  }
  editor.setPosition(position)
  editor.revealPositionInCenter(position)
  editor.focus()
}

function severityType(severity: LintIssue['severity']) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = {
    ERROR: 'danger',
    WARNING: 'warning',
    SUGGESTION: 'info'
  }
  return severity ? map[severity] : 'info'
}

function severityLabel(severity: LintIssue['severity']) {
  const map: Record<string, string> = {
    ERROR: '错误',
    WARNING: '警告',
    SUGGESTION: '建议'
  }
  return severity ? map[severity] : '-'
}

function fixSuggestion(issue: LintIssue) {
  if (issue.suggestion) {
    return issue.suggestion
  }
  if (issue.replacement) {
    return `建议替换为 ${issue.replacement}`
  }
  return '-'
}

function locationLabel(issue: LintIssue) {
  if (!issue.line) {
    return '-'
  }
  return `${issue.line}:${issue.column ?? 1}`
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.sql-lint-page {
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-header h2 {
  margin: 0;
}

.lint-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  min-height: 560px;
}

.editor-panel,
.result-panel {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.panel-title {
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.editor-container {
  flex: 1;
  min-height: 520px;
}

.result-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.summary-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.fixed-sql-panel,
.history-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.fixed-sql-panel {
  margin-bottom: 14px;
}

.fixed-sql-header,
.history-title,
.pagination-row {
  display: flex;
  align-items: center;
}

.fixed-sql-header {
  justify-content: space-between;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.history-title {
  gap: 8px;
  width: 100%;
  font-weight: 600;
}

.sql-code {
  margin: 0;
  padding: 12px;
  max-height: 280px;
  overflow: auto;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #1f2d3d;
  background: #f7f8fa;
  white-space: pre-wrap;
  word-break: break-word;
}

.sql-code.compact {
  max-height: 220px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.record-table {
  width: 100%;
}

.pagination-row {
  justify-content: flex-end;
  padding-top: 12px;
}

.record-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.record-time {
  margin-left: auto;
  color: #606266;
  font-size: 13px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

@media (max-width: 1100px) {
  .lint-content {
    grid-template-columns: 1fr;
  }
}
</style>
