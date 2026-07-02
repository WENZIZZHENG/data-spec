<template>
  <div class="standard-health-page">
    <div class="page-header">
      <div>
        <h2>标准健康</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!hasProject" :loading="loading" @click="loadTrend">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" :loading="createLoading" @click="handleCreateSnapshot">
          <el-icon><DataAnalysis /></el-icon>
          创建快照
        </el-button>
        <el-button :disabled="!latest" :loading="planLoading" @click="handleCopyPlan">
          <el-icon><DocumentCopy /></el-icon>
          复制计划
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
      <StateBlock
        v-if="errorMessage"
        type="error"
        title="标准健康加载失败"
        :description="errorMessage"
        suggested-action="检查当前项目、后端服务和 API Token 后重试。"
        action-text="重试"
        @action="loadTrend"
      />

      <template v-else>
        <section class="summary-section">
          <div class="score-panel">
            <span class="metric-label">质量均分</span>
            <strong>{{ metrics.averageQualityScore ?? 0 }}</strong>
            <span class="muted-text">快照 {{ latest?.id ? `#${latest.id}` : '未创建' }}</span>
          </div>
          <div class="summary-grid">
            <div v-for="item in summaryItems" :key="item.key" class="summary-item">
              <span class="metric-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>

        <section class="delta-section">
          <div v-for="item in deltaItems" :key="item.key" class="delta-item">
            <span class="metric-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <span class="muted-text">{{ item.summary }}</span>
          </div>
        </section>

        <div class="health-layout">
          <section class="main-panel">
            <div class="section-header">
              <h3>最近快照</h3>
              <el-tag effect="plain" type="info">{{ snapshots.length }} 条</el-tag>
            </div>
            <el-table
              v-loading="loading"
              :data="snapshots"
              stripe
              empty-text="暂无标准健康快照"
            >
              <el-table-column label="采集时间" min-width="170">
                <template #default="{ row }">
                  {{ formatTime(row.capturedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="质量均分" width="110">
                <template #default="{ row }">
                  {{ row.metrics?.averageQualityScore ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column label="低质量" width="100">
                <template #default="{ row }">
                  {{ row.metrics?.lowQualityFieldCount ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column label="覆盖率" width="110">
                <template #default="{ row }">
                  {{ formatPercent(row.metrics?.coverageRate) }}
                </template>
              </el-table-column>
              <el-table-column label="未纳管" width="100">
                <template #default="{ row }">
                  {{ row.metrics?.unmanagedFieldCount ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column label="候选" width="100">
                <template #default="{ row }">
                  {{ row.metrics?.pendingCandidateCount ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column label="动作" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ actionSummary(row.topActions) }}
                </template>
              </el-table-column>
            </el-table>
          </section>

          <aside class="side-panel">
            <div class="section-header">
              <h3>Top actions</h3>
              <el-tag v-if="latest?.metrics?.coverageStatus === 'not_collected'" type="warning" effect="plain">
                未采集覆盖率
              </el-tag>
            </div>
            <el-empty v-if="topActions.length === 0" class="small-empty" description="暂无改进动作" />
            <div v-else class="action-list">
              <div v-for="action in topActions" :key="`${action.priority}-${action.title}`" class="action-item">
                <div class="action-title">
                  <el-tag size="small" :type="priorityTagType(action.priority)" effect="plain">
                    {{ action.priority || 'LOW' }}
                  </el-tag>
                  <strong>{{ action.title }}</strong>
                </div>
                <p>{{ action.description }}</p>
                <div class="action-footer">
                  <span class="muted-text">{{ action.evidence || 'health.snapshot' }}</span>
                  <el-button v-if="action.targetRoute" size="small" text type="primary" @click="goTarget(action.targetRoute)">
                    打开
                  </el-button>
                </div>
              </div>
            </div>
          </aside>
        </div>

        <el-collapse v-model="activePanels" class="health-collapse">
          <el-collapse-item title="覆盖率摘要（可选，创建快照时写入）" name="coverage">
            <el-form class="coverage-form" label-width="108px">
              <el-form-item label="覆盖率">
                <el-input-number
                  v-model="coverageForm.coverageRate"
                  class="number-control"
                  :min="0"
                  :max="100"
                  :precision="1"
                  placeholder="例如 86.5"
                />
              </el-form-item>
              <el-form-item label="未纳管字段">
                <el-input-number v-model="coverageForm.unmanagedFieldCount" class="number-control" :min="0" />
              </el-form-item>
              <el-form-item label="缺注释字段">
                <el-input-number v-model="coverageForm.missingCommentCount" class="number-control" :min="0" />
              </el-form-item>
              <el-form-item label="疑似重复">
                <el-input-number v-model="coverageForm.possibleDuplicateCount" class="number-control" :min="0" />
              </el-form-item>
              <el-form-item label="Top 未纳管">
                <el-input
                  v-model="coverageForm.topUnmanagedFields"
                  type="textarea"
                  :rows="2"
                  placeholder="user_id, mobile_no；用逗号、中文逗号或换行分隔"
                />
              </el-form-item>
            </el-form>
          </el-collapse-item>
          <el-collapse-item title="AI 可复制计划" name="plan">
            <pre class="plan-preview">{{ planMarkdown }}</pre>
          </el-collapse-item>
        </el-collapse>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, DocumentCopy, Refresh } from '@element-plus/icons-vue'
import { createStandardHealthSnapshot, getStandardHealthPlan, getStandardHealthTrend } from '@/api/standardHealth'
import ProjectRequired from '@/components/ProjectRequired.vue'
import StateBlock from '@/components/StateBlock.vue'
import { useProjectStore } from '@/stores/project'
import type {
  StandardHealthAction,
  StandardHealthCoverageInput,
  StandardHealthDelta,
  StandardHealthMetrics,
  StandardHealthPlan,
  StandardHealthTrend
} from '@/types'

interface CoverageFormState {
  coverageRate?: number
  unmanagedFieldCount?: number
  missingCommentCount?: number
  possibleDuplicateCount?: number
  topUnmanagedFields: string
}

const projectStore = useProjectStore()
const router = useRouter()
const loading = ref(false)
const createLoading = ref(false)
const planLoading = ref(false)
const errorMessage = ref('')
const trend = ref<StandardHealthTrend>({})
const plan = ref<StandardHealthPlan | null>(null)
const activePanels = ref<string[]>(['coverage', 'plan'])
const coverageForm = reactive<CoverageFormState>({
  coverageRate: undefined,
  unmanagedFieldCount: undefined,
  missingCommentCount: undefined,
  possibleDuplicateCount: undefined,
  topUnmanagedFields: ''
})

const hasProject = computed(() => projectStore.currentProjectId !== null)
const latest = computed(() => trend.value.latest ?? null)
const metrics = computed<StandardHealthMetrics>(() => latest.value?.metrics ?? {})
const snapshots = computed(() => trend.value.snapshots ?? [])
const topActions = computed(() => latest.value?.topActions ?? [])
const planMarkdown = computed(() =>
  latest.value?.planMarkdown || plan.value?.markdown || '暂无计划。创建第一条快照后，这里会出现可复制给 AI 的维护计划。'
)
const summaryItems = computed(() => [
  { key: 'fields', label: '字段数', value: metrics.value.totalFieldCount ?? 0 },
  { key: 'lowQuality', label: '低质量', value: metrics.value.lowQualityFieldCount ?? 0 },
  { key: 'coverage', label: '覆盖率', value: formatPercent(metrics.value.coverageRate) },
  { key: 'unmanaged', label: '未纳管', value: metrics.value.unmanagedFieldCount ?? 0 },
  { key: 'rules', label: '规则信号', value: metrics.value.ruleIssueCount ?? 0 },
  { key: 'feedback', label: 'AI 信号', value: metrics.value.aiFeedbackSignalCount ?? 0 },
  { key: 'candidates', label: '待处理候选', value: metrics.value.pendingCandidateCount ?? 0 },
  { key: 'fixedSql', label: 'fixedSql', value: metrics.value.fixedSqlAvailableCount ?? 0 }
])
const deltaItems = computed(() => [
  {
    key: 'week',
    label: '本周变化',
    value: deltaValue(trend.value.weekDelta),
    summary: trend.value.weekDelta?.summary || '暂无 7 天前基线'
  },
  {
    key: 'month',
    label: '本月变化',
    value: deltaValue(trend.value.monthDelta),
    summary: trend.value.monthDelta?.summary || '暂无 30 天前基线'
  }
])

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    plan.value = null
    void loadTrend()
  },
  { immediate: true }
)

async function loadTrend() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    trend.value = {}
    errorMessage.value = ''
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    trend.value = await getStandardHealthTrend(projectId)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '标准健康趋势加载失败'
  } finally {
    loading.value = false
  }
}

async function handleCreateSnapshot() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  createLoading.value = true
  try {
    await createStandardHealthSnapshot({
      projectId,
      coverage: buildCoverageInput()
    })
    plan.value = null
    ElMessage.success('已创建标准健康快照')
    await loadTrend()
  } finally {
    createLoading.value = false
  }
}

async function handleCopyPlan() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  planLoading.value = true
  try {
    if (!latest.value?.planMarkdown) {
      plan.value = await getStandardHealthPlan(projectId)
    }
    await copyText(planMarkdown.value)
    ElMessage.success('已复制标准健康改进计划')
  } finally {
    planLoading.value = false
  }
}

function buildCoverageInput(): StandardHealthCoverageInput | undefined {
  const topUnmanagedFields = splitTopUnmanagedFields(coverageForm.topUnmanagedFields)
  const hasCoverage = hasNumber(coverageForm.coverageRate)
    || hasNumber(coverageForm.unmanagedFieldCount)
    || hasNumber(coverageForm.missingCommentCount)
    || hasNumber(coverageForm.possibleDuplicateCount)
    || topUnmanagedFields.length > 0
  if (!hasCoverage) {
    return undefined
  }
  return {
    coverageRate: coverageForm.coverageRate,
    unmanagedFieldCount: coverageForm.unmanagedFieldCount,
    missingCommentCount: coverageForm.missingCommentCount,
    possibleDuplicateCount: coverageForm.possibleDuplicateCount,
    topUnmanagedFields
  }
}

function splitTopUnmanagedFields(value: string) {
  return value
    .split(/[\n,，]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 20)
}

function hasNumber(value?: number) {
  return typeof value === 'number' && Number.isFinite(value)
}

function formatPercent(value?: number | null) {
  if (value === null || value === undefined) {
    return '未采集'
  }
  return `${value}%`
}

function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

function deltaValue(delta?: StandardHealthDelta | null) {
  if (!delta) {
    return 'N/A'
  }
  const quality = signed(delta.qualityAverageScoreDelta)
  const lowQuality = signed(delta.lowQualityFieldCountDelta)
  return `质量 ${quality} / 低质量 ${lowQuality}`
}

function signed(value?: number | null) {
  if (value === null || value === undefined) {
    return 'N/A'
  }
  return value > 0 ? `+${value}` : String(value)
}

function actionSummary(actions?: StandardHealthAction[]) {
  if (!actions?.length) {
    return '-'
  }
  return actions.map((action) => action.title).filter(Boolean).slice(0, 3).join('；')
}

function priorityTagType(priority?: string) {
  if (priority === 'HIGH') {
    return 'danger'
  }
  if (priority === 'MEDIUM') {
    return 'warning'
  }
  return 'info'
}

function goTarget(targetRoute: string) {
  const [path, search = ''] = targetRoute.split('?')
  if (!path.startsWith('/')) {
    return
  }
  const query: Record<string, string | number | undefined> = {}
  new URLSearchParams(search).forEach((value, key) => {
    query[key] = value
  })
  void router.push({
    path,
    query: {
      ...query,
      projectId: projectStore.currentProjectId ?? undefined
    }
  })
}

function goProjects() {
  void router.push('/projects')
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return
    }
  } catch {
    // 部分本地非安全上下文会拒绝 Clipboard API，下面使用 textarea 兜底。
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
</script>

<style scoped>
.standard-health-page {
  min-height: calc(100vh - 140px);
  padding: 20px;
  border-radius: 4px;
  background: #fff;
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
  color: #1f2937;
  font-weight: 600;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-section {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}

.score-panel,
.summary-item,
.delta-item,
.main-panel,
.side-panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fcfcfd;
}

.score-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  min-height: 108px;
  padding: 14px;
}

.score-panel strong {
  color: #111827;
  font-size: 34px;
  line-height: 1;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
}

.summary-item,
.delta-item {
  min-height: 72px;
  padding: 12px 14px;
}

.summary-item strong,
.delta-item strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 22px;
  line-height: 1.1;
}

.delta-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.health-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
  margin-bottom: 16px;
}

.main-panel,
.side-panel {
  min-width: 0;
  padding: 14px;
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
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-item {
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.action-item p {
  margin: 8px 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.55;
}

.action-title,
.action-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.action-title {
  justify-content: flex-start;
}

.health-collapse {
  border-top: 1px solid #ebeef5;
}

.coverage-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 0 16px;
}

.coverage-form :deep(.el-form-item:last-child) {
  grid-column: 1 / -1;
}

.number-control {
  width: 100%;
}

.plan-preview {
  max-height: 260px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  color: #374151;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.metric-label,
.muted-text {
  color: #6b7280;
  font-size: 12px;
}

.small-empty {
  padding: 32px 0;
}

@media (max-width: 1000px) {
  .summary-section,
  .health-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .page-header,
  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .delta-section,
  .coverage-form {
    grid-template-columns: 1fr;
  }
}
</style>
