<template>
  <div class="sql-lint-page">
    <div class="page-header">
      <h2>SQL 校验</h2>
      <el-button type="primary" :loading="linting" @click="handleLint">
        <el-icon><CaretRight /></el-icon>
        执行校验
      </el-button>
    </div>
    <div class="fix-policy-toolbar">
      <div class="policy-control">
        <span class="policy-label">修复模式</span>
        <el-radio-group v-model="fixPolicyMode" size="small">
          <el-radio-button label="GENERATE">生成</el-radio-button>
          <el-radio-button label="DRY_RUN">dry-run</el-radio-button>
          <el-radio-button label="DISABLED">关闭</el-radio-button>
        </el-radio-group>
      </div>
      <div class="policy-control">
        <span class="policy-label">最高风险</span>
        <el-select v-model="fixMaxRiskLevel" size="small" class="risk-select">
          <el-option label="低" value="LOW" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="高" value="HIGH" />
        </el-select>
      </div>
      <el-switch v-model="includeFixExplanations" size="small" active-text="解释" inactive-text="简略" />
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

            <div v-if="lintDialectDiagnostics.length" class="dialect-panel">
              <div class="dialect-header">
                <span>方言诊断</span>
                <el-tag size="small" :type="diagnosticSummaryTagType(lintDialectDiagnostics)">
                  {{ dialectSummary(lintDialectDiagnostics) }}
                </el-tag>
              </div>
              <div class="diagnostic-list">
                <div
                  v-for="diagnostic in lintDialectDiagnostics"
                  :key="diagnostic.code || `${diagnostic.dialect}-${diagnostic.capability}`"
                  class="diagnostic-item"
                >
                  <el-tag size="small" :type="diagnosticTagType(diagnostic.level)">
                    {{ diagnosticLevelLabel(diagnostic.level) }}
                  </el-tag>
                  <div class="diagnostic-copy">
                    <span>{{ diagnostic.message }}</span>
                    <small v-if="diagnostic.nextAction">{{ diagnostic.nextAction }}</small>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="hasFixPlan" class="fix-plan-panel">
              <div class="fix-plan-header">
                <span>修复策略</span>
                <div class="fix-plan-tags">
                  <el-tag size="small" type="info">{{ fixModeLabel(lintResult.fixPolicy?.mode) }}</el-tag>
                  <el-tag size="small" :type="riskTagType(lintResult.fixPolicy?.maxRiskLevel)">
                    {{ riskLabel(lintResult.fixPolicy?.maxRiskLevel) }}
                  </el-tag>
                  <el-tag v-if="lintResult.fixDryRun" size="small" type="warning">dry-run</el-tag>
                </div>
              </div>
              <div class="fix-summary-row">
                <el-tag size="small" type="success">应用 {{ lintResult.fixSummary?.appliedCount ?? 0 }}</el-tag>
                <el-tag size="small" type="warning">预览 {{ lintResult.fixSummary?.plannedCount ?? 0 }}</el-tag>
                <el-tag size="small" type="info">跳过 {{ lintResult.fixSummary?.skippedCount ?? 0 }}</el-tag>
              </div>
              <ul v-if="lintResult.fixNextActions?.length" class="fix-actions">
                <li v-for="(action, index) in lintResult.fixNextActions" :key="index">{{ action }}</li>
              </ul>
              <el-table
                v-if="fixChanges.length"
                :data="fixChanges"
                size="small"
                class="fix-change-table"
                empty-text="暂无修复变更"
              >
                <el-table-column label="状态" width="86">
                  <template #default="{ row }">
                    <el-tag size="small" :type="fixStatusType(row.status)">
                      {{ fixStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="风险" width="76">
                  <template #default="{ row }">
                    <el-tag size="small" :type="riskTagType(row.riskLevel)">
                      {{ riskLabel(row.riskLevel) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="ruleCode" label="规则" min-width="150" />
                <el-table-column label="变更" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ fixChangeLabel(row) }}
                  </template>
                </el-table-column>
                <el-table-column prop="explain" label="解释" min-width="240" show-overflow-tooltip />
              </el-table>
            </div>

            <div v-if="lintResult.fixedSql" class="fixed-sql-panel">
              <div class="fixed-sql-header">
                <span>修正 SQL</span>
                <el-button size="small" text type="primary" @click="handleCopySql">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
              <div v-if="fixedSqlDiffLines.length" class="sql-diff">
                <div
                  v-for="(line, index) in fixedSqlDiffLines"
                  :key="`current-${index}`"
                  :class="['diff-line', `diff-line-${line.type}`]"
                >
                  {{ line.text || ' ' }}
                </div>
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
              <el-table-column label="修复" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.fixStatus" size="small" :type="fixStatusType(row.fixStatus)">
                    {{ fixStatusLabel(row.fixStatus) }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
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

        <div v-if="activeRecord.replay" class="detail-section replay-section">
          <div class="detail-title replay-title">
            <span>标准回放</span>
            <el-tag size="small" :type="replayStatusType(activeRecord.replay.status)">
              {{ replayStatusLabel(activeRecord.replay.status) }}
            </el-tag>
          </div>
          <div class="replay-grid">
            <div class="replay-meta">
              <span class="replay-label">记录标准</span>
              <span>{{ standardLabel(activeRecord.replay.recordedStandard) }}</span>
            </div>
            <div class="replay-meta">
              <span class="replay-label">当前标准</span>
              <span>{{ standardLabel(activeRecord.replay.currentStandard) }}</span>
            </div>
            <div class="replay-meta">
              <span class="replay-label">计数</span>
              <span>
                字段 {{ activeRecord.replay.summary?.fieldCount ?? 0 }} /
                枚举 {{ activeRecord.replay.summary?.enumCount ?? 0 }} /
                规则 {{ activeRecord.replay.summary?.ruleCount ?? 0 }}
              </span>
            </div>
          </div>

          <div v-if="activeRecord.replay.summary?.exportCommand" class="replay-command">
            <div class="fixed-sql-header compact-header">
              <span>历史 Context 导出命令</span>
              <el-button size="small" text type="primary" @click="handleCopyReplayCommand">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
            <pre class="sql-code compact">{{ activeRecord.replay.summary.exportCommand }}</pre>
          </div>

          <ul v-if="activeRecord.replay.nextActions?.length" class="replay-actions">
            <li v-for="(action, index) in activeRecord.replay.nextActions" :key="index">
              {{ action }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <div class="detail-title">原始 SQL</div>
          <pre class="sql-code compact">{{ activeRecord.record.originalSql }}</pre>
        </div>

        <div v-if="activeRecord.record.fixedSql" class="detail-section">
          <div class="detail-title">修正 SQL</div>
          <div v-if="recordDiffLines.length" class="sql-diff compact">
            <div
              v-for="(line, index) in recordDiffLines"
              :key="`record-${index}`"
              :class="['diff-line', `diff-line-${line.type}`]"
            >
              {{ line.text || ' ' }}
            </div>
          </div>
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
            <el-table-column label="修复" width="120">
              <template #default="{ row }">
                <div v-if="row.fixStatus" class="issue-fix-cell">
                  <el-tag size="small" :type="fixStatusType(row.fixStatus)">
                    {{ fixStatusLabel(row.fixStatus) }}
                  </el-tag>
                  <span v-if="row.fixRiskLevel">{{ riskLabel(row.fixRiskLevel) }}</span>
                </div>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="描述" min-width="240" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as monaco from 'monaco-editor'
import { getLintRecord, lintSql, listLintRecords } from '@/api/lint'
import { useProjectStore } from '@/stores/project'
import {
  diagnosticLevelLabel,
  diagnosticSummaryTagType,
  diagnosticTagType,
  dialectSummary
} from '@/utils/dialectDiagnostics'
import type {
  FixChange,
  FixPolicy,
  LintIssue,
  LintResult,
  RecordDetail,
  SqlCheckRecord,
  StandardSnapshotInfo
} from '@/types'

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
const fixPolicyMode = ref<NonNullable<FixPolicy['mode']>>('GENERATE')
const fixMaxRiskLevel = ref<NonNullable<FixPolicy['maxRiskLevel']>>('MEDIUM')
const includeFixExplanations = ref(true)
const projectStore = useProjectStore()
const route = useRoute()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const DEFAULT_SQL = `CREATE TABLE users (
    id bigserial PRIMARY KEY,
    username varchar(50) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false
);`
const DEMO_LINT_SQL = `CREATE TABLE UserOrder (
    id bigserial PRIMARY KEY,
    uid bigint NOT NULL,
    phone varchar(20),
    amount decimal(10,2) DEFAULT 0,
    create_time timestamp,
    update_time timestamp,
    del_flag boolean DEFAULT false
);`

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
const fixedSqlDiffLines = computed(() => parseDiff(lintResult.value?.fixedSqlDiff))
const lintDialectDiagnostics = computed(() => lintResult.value?.dialectDiagnostics ?? [])
const currentFixPolicy = computed<FixPolicy>(() => ({
  mode: fixPolicyMode.value,
  maxRiskLevel: fixMaxRiskLevel.value,
  includeExplanations: includeFixExplanations.value
}))
const fixChanges = computed<FixChange[]>(() => lintResult.value?.fixChanges ?? [])
const hasFixPlan = computed(() => Boolean(
  lintResult.value?.fixSummary ||
  lintResult.value?.fixPolicy ||
  fixChanges.value.length ||
  lintResult.value?.fixNextActions?.length
))
const recordDiffLines = computed(() => {
  const record = activeRecord.value?.record
  return parseDiff(buildSqlDiff(record?.originalSql, record?.fixedSql))
})

onMounted(() => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: initialSql(),
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

watch(
  () => route.query.demo,
  () => {
    if (route.query.demo === 'lint' && editor) {
      editor.setValue(DEMO_LINT_SQL)
    }
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
      projectId: projectStore.currentProjectId ?? undefined,
      fixPolicy: currentFixPolicy.value
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
  await copyToClipboard(fixedSql, '已复制修正 SQL', '复制失败，请手动选择修正 SQL')
}

async function handleCopyReplayCommand() {
  const command = activeRecord.value?.replay?.summary?.exportCommand
  if (!command) {
    return
  }
  await copyToClipboard(command, '已复制回放命令', '复制失败，请手动选择回放命令')
}

async function copyToClipboard(text: string, successMessage: string, errorMessage: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch {
    ElMessage.error(errorMessage)
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

function fixModeLabel(mode?: FixPolicy['mode']) {
  const map: Record<string, string> = {
    GENERATE: '生成',
    DRY_RUN: 'dry-run',
    DISABLED: '关闭'
  }
  return mode ? map[mode] ?? mode : '生成'
}

function riskLabel(risk?: FixPolicy['maxRiskLevel'] | FixChange['riskLevel']) {
  const map: Record<string, string> = {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险'
  }
  return risk ? map[risk] ?? risk : '中风险'
}

function riskTagType(risk?: FixPolicy['maxRiskLevel'] | FixChange['riskLevel']) {
  const map: Record<string, 'success' | 'warning' | 'danger'> = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger'
  }
  return risk ? map[risk] ?? 'warning' : 'warning'
}

function fixStatusLabel(status?: FixChange['status'] | LintIssue['fixStatus']) {
  const map: Record<string, string> = {
    APPLIED: '已应用',
    PLANNED: '预览',
    SKIPPED: '跳过'
  }
  return status ? map[status] ?? status : '-'
}

function fixStatusType(status?: FixChange['status'] | LintIssue['fixStatus']) {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    APPLIED: 'success',
    PLANNED: 'warning',
    SKIPPED: 'info'
  }
  return status ? map[status] ?? 'info' : 'info'
}

function fixChangeLabel(change: FixChange) {
  const target = [change.tableName, change.columnName].filter(Boolean).join('.')
  const before = change.before ?? '-'
  const after = change.after ?? '-'
  return `${target || change.changeType || '变更'}：${before} -> ${after}`
}

function locationLabel(issue: LintIssue) {
  if (!issue.line) {
    return '-'
  }
  const start = `${issue.line}:${issue.column ?? 1}`
  if (!issue.lineEnd || !issue.columnEnd) {
    return start
  }
  return `${start}-${issue.lineEnd}:${issue.columnEnd}`
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

function replayStatusLabel(status?: string) {
  const map: Record<string, string> = {
    current: '当前一致',
    historical: '历史标准',
    unversioned: '未版本化',
    missing_snapshot: '快照缺失'
  }
  return status ? map[status] ?? status : '-'
}

function replayStatusType(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    current: 'success',
    historical: 'warning',
    unversioned: 'info',
    missing_snapshot: 'danger'
  }
  return status ? map[status] ?? 'info' : 'info'
}

function standardLabel(standard?: StandardSnapshotInfo) {
  if (!standard) {
    return '-'
  }
  const version = standard.specVersion || 'unversioned'
  const hash = standard.specHash ? ` / ${shortHash(standard.specHash)}` : ''
  const source = standard.source ? ` / ${standard.source}` : ''
  return `${version}${hash}${source}`
}

function shortHash(hash: string) {
  return hash.length > 12 ? `${hash.slice(0, 12)}...` : hash
}

function initialSql() {
  return route.query.demo === 'lint' ? DEMO_LINT_SQL : DEFAULT_SQL
}

type DiffLineType = 'header' | 'add' | 'remove' | 'context'

function parseDiff(diff?: string | null) {
  if (!diff) {
    return []
  }
  return diff.split('\n').map((text) => ({
    text,
    type: diffLineType(text)
  }))
}

function diffLineType(line: string): DiffLineType {
  if (line.startsWith('+++') || line.startsWith('---') || line.startsWith('@@')) {
    return 'header'
  }
  if (line.startsWith('+')) {
    return 'add'
  }
  if (line.startsWith('-')) {
    return 'remove'
  }
  return 'context'
}

function buildSqlDiff(originalSql?: string, fixedSql?: string) {
  if (!originalSql || !fixedSql || originalSql === fixedSql) {
    return null
  }
  const originalLines = originalSql.split(/\r?\n/)
  const fixedLines = fixedSql.split(/\r?\n/)
  if (originalLines.join('\n') === fixedLines.join('\n')) {
    return null
  }
  return [
    '--- original.sql',
    '+++ fixed.sql',
    '@@',
    ...buildDiffLines(originalLines, fixedLines)
  ].join('\n')
}

function buildDiffLines(originalLines: string[], fixedLines: string[]) {
  const lcs = Array.from({ length: originalLines.length + 1 }, () =>
    Array.from({ length: fixedLines.length + 1 }, () => 0)
  )
  for (let i = originalLines.length - 1; i >= 0; i--) {
    for (let j = fixedLines.length - 1; j >= 0; j--) {
      lcs[i][j] = originalLines[i] === fixedLines[j]
        ? lcs[i + 1][j + 1] + 1
        : Math.max(lcs[i + 1][j], lcs[i][j + 1])
    }
  }

  const lines: string[] = []
  let i = 0
  let j = 0
  while (i < originalLines.length && j < fixedLines.length) {
    if (originalLines[i] === fixedLines[j]) {
      lines.push(` ${originalLines[i]}`)
      i++
      j++
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      lines.push(`-${originalLines[i]}`)
      i++
    } else {
      lines.push(`+${fixedLines[j]}`)
      j++
    }
  }
  while (i < originalLines.length) {
    lines.push(`-${originalLines[i]}`)
    i++
  }
  while (j < fixedLines.length) {
    lines.push(`+${fixedLines[j]}`)
    j++
  }
  return lines
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

.fix-policy-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.policy-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.policy-label {
  color: #606266;
  font-size: 13px;
}

.risk-select {
  width: 92px;
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
.history-panel,
.fix-plan-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.fixed-sql-panel,
.fix-plan-panel {
  margin-bottom: 14px;
}

.fixed-sql-header,
.history-title,
.pagination-row,
.fix-plan-header,
.fix-summary-row {
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

.fix-plan-panel {
  padding: 10px 12px;
}

.fix-plan-header {
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.fix-plan-tags,
.fix-summary-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.fix-summary-row {
  margin-bottom: 8px;
}

.fix-actions {
  margin: 0 0 8px;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.fix-change-table {
  width: 100%;
}

.issue-fix-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
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

.sql-diff {
  max-height: 260px;
  overflow: auto;
  border-bottom: 1px solid #e4e7ed;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
  line-height: 1.6;
  background: #f8fafc;
}

.sql-diff.compact {
  max-height: 220px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.diff-line {
  min-height: 19px;
  padding: 0 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.diff-line-header {
  color: #64748b;
  background: #f1f5f9;
}

.diff-line-add {
  color: #14532d;
  background: #dcfce7;
}

.diff-line-remove {
  color: #7f1d1d;
  background: #fee2e2;
}

.diff-line-context {
  color: #334155;
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

.dialect-panel {
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
}

.dialect-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.diagnostic-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.diagnostic-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.diagnostic-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.diagnostic-copy small {
  color: #909399;
}

.replay-section {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #f9fafb;
}

.replay-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.replay-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.replay-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  font-size: 13px;
  color: #303133;
}

.replay-label {
  color: #909399;
  font-size: 12px;
}

.replay-command {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.compact-header {
  padding: 8px 10px;
}

.replay-actions {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .lint-content {
    grid-template-columns: 1fr;
  }

  .replay-grid {
    grid-template-columns: 1fr;
  }
}
</style>
