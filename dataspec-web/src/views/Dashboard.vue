<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div>
        <h2>工作台</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <el-button :disabled="!hasProject" :loading="dashboardLoading" @click="loadDashboard">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <ProjectRequired
      v-if="!hasProject"
      :has-project="hasProject"
      action-text="创建演示项目"
      secondary-action-text="去项目列表"
      :loading="demoLoading"
      @action="handleCreateDemoProject"
      @secondary="goProjects"
    />

    <template v-else>
      <StateBlock
        v-if="dashboardErrorMessage"
        type="error"
        title="工作台加载失败"
        :description="dashboardErrorMessage"
        :suggested-action="dashboardSuggestedAction"
        :docs-ref="dashboardDocsRef"
        action-text="重试"
        @action="loadDashboard"
      />

      <div v-else v-loading="dashboardLoading" class="dashboard-content">
        <section class="metric-grid">
          <div v-for="metric in metrics" :key="metric.key" class="metric-item">
            <div class="metric-label">{{ metric.label }}</div>
            <div class="metric-value">{{ metric.value }}</div>
          </div>
        </section>

        <section class="task-panel">
          <div class="section-header">
            <h3>任务入口</h3>
            <el-button text type="primary" :loading="demoLoading" @click="handleCreateDemoProject">
              演示项目
            </el-button>
          </div>
          <div class="task-grid">
            <button
              v-for="task in taskEntries"
              :key="task.key"
              type="button"
              class="task-card"
              @click="openTask(task)"
            >
              <el-icon class="task-icon">
                <component :is="task.icon" />
              </el-icon>
              <span class="task-title">{{ task.title }}</span>
              <span class="task-meta">{{ task.meta }}</span>
            </button>
          </div>
          <div class="demo-task-row">
            <el-button type="primary" :loading="demoLoading" @click="openDemoDdl">
              生成演示 DDL
            </el-button>
            <el-button :loading="demoLoading" @click="openDemoSqlLint">
              校验示例 SQL
            </el-button>
          </div>
        </section>

        <section v-if="recentTaskItems.length" class="panel">
          <div class="section-header">
            <h3>最近任务</h3>
            <el-button text type="primary" @click="clearRecentTasks">清空</el-button>
          </div>
          <div class="recent-task-row">
            <el-button
              v-for="task in recentTaskItems"
              :key="`${task.projectId}-${task.key}`"
              @click="openRecentTask(task)"
            >
              <el-icon><ArrowRight /></el-icon>
              {{ task.title }}
            </el-button>
          </div>
        </section>

        <section class="panel">
          <div class="section-header">
            <h3>问题趋势</h3>
            <el-tag type="info">最近 {{ summary?.trend?.length ?? 0 }} 次</el-tag>
          </div>
          <div v-if="trendItems.length" class="trend-list">
            <div v-for="item in trendItems" :key="item.key" class="trend-row">
              <span class="trend-time">{{ item.time }}</span>
              <div class="trend-track">
                <div class="trend-bar" :style="{ width: item.width }" />
              </div>
              <span class="trend-count">{{ item.issueCount }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无检查记录" />
        </section>

        <section class="panel">
          <div class="section-header">
            <h3>最近检查</h3>
            <el-button text type="primary" @click="$router.push('/sql-lint')">SQL 校验</el-button>
          </div>
          <el-table :data="summary?.recentChecks ?? []" stripe empty-text="暂无检查记录">
            <el-table-column label="检查时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="errorCount" label="错误" width="90" />
            <el-table-column prop="warningCount" label="警告" width="90" />
            <el-table-column prop="suggestionCount" label="建议" width="90" />
            <el-table-column prop="issueCount" label="问题数" width="100" />
          </el-table>
        </section>

        <section class="panel">
          <div class="section-header">
            <h3>最近活动</h3>
            <div class="activity-actions">
              <el-select
                v-model="activityActionType"
                clearable
                placeholder="全部活动"
                size="small"
                class="activity-filter"
              >
                <el-option
                  v-for="action in activityActionOptions"
                  :key="action.actionType"
                  :label="action.label || action.actionType"
                  :value="action.actionType"
                />
              </el-select>
              <el-button text type="primary" :loading="activityState.loading.value" @click="loadActivities">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>

          <div v-loading="activityState.loading.value" class="activity-body">
            <el-timeline v-if="activityItems.length" class="activity-timeline">
              <el-timeline-item
                v-for="activity in activityItems"
                :key="activity.id"
                :timestamp="formatDate(activity.occurredAt)"
                :type="activityTimelineType(activity.severity)"
                placement="top"
              >
                <div class="activity-item">
                  <div class="activity-title-row">
                    <span class="activity-title">{{ activity.title || '--' }}</span>
                    <el-tag :type="severityTagType(activity.severity)" size="small" effect="plain">
                      {{ severityLabel(activity.severity) }}
                    </el-tag>
                  </div>
                  <p class="activity-description">{{ activity.description || '--' }}</p>
                  <div class="activity-meta">
                    <span>{{ activity.source || '未知来源' }}</span>
                    <span>{{ activity.actor || '未知操作者' }}</span>
                  </div>
                  <el-button
                    v-if="activity.detailRoute"
                    text
                    type="primary"
                    size="small"
                    class="activity-link"
                    @click="goActivity(activity.detailRoute)"
                  >
                    <el-icon><ArrowRight /></el-icon>
                    查看
                  </el-button>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无项目活动" />
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, type Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Cpu,
  DataAnalysis,
  Edit,
  Key,
  List,
  MagicStick,
  Refresh,
  Search
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listProjectActivities } from '@/api/activity'
import { getDashboardSummary } from '@/api/dashboard'
import ProjectRequired from '@/components/ProjectRequired.vue'
import StateBlock from '@/components/StateBlock.vue'
import { useRequestState } from '@/composables/useRequestState'
import { useProjectStore } from '@/stores/project'
import type { DashboardSummary, IssueTrendPoint, ProjectActivityTimeline } from '@/types'

interface DashboardTask {
  key: string
  title: string
  meta: string
  route: string
  icon: Component
}

interface RecentTask {
  key: string
  title: string
  route: string
  projectId: number
  usedAt: string
}

const RECENT_TASKS_KEY = 'dataspec.dashboard.recentTasks.v1'

const projectStore = useProjectStore()
const router = useRouter()
const summaryState = useRequestState<DashboardSummary>()
const activityState = useRequestState<ProjectActivityTimeline>()
const summary = summaryState.data
const activityTimeline = activityState.data
const demoLoading = ref(false)
const activityActionType = ref<string>('')
const recentTasks = ref<RecentTask[]>([])

const taskEntries: DashboardTask[] = [
  { key: 'reverse-import', title: '导入现有库', meta: '反向导入', route: '/reverse-import', icon: Search },
  { key: 'sql-lint', title: '检查 SQL', meta: 'fixedSql', route: '/sql-lint', icon: Edit },
  { key: 'field-coverage', title: '生成覆盖率', meta: '未纳管字段', route: '/field-coverage', icon: DataAnalysis },
  { key: 'fields', title: '补标准字段', meta: '字段库', route: '/fields', icon: List },
  { key: 'generator', title: '生成 DDL', meta: '表模板', route: '/generator', icon: MagicStick },
  { key: 'ai-export', title: '导出给 AI', meta: 'Context', route: '/ai-export', icon: Cpu },
  { key: 'tokens', title: '管理 Token', meta: 'CLI/MCP', route: '/tokens', icon: Key }
]

const hasProject = computed(() => projectStore.currentProjectId !== null)
const dashboardLoading = computed(() => summaryState.loading.value || activityState.loading.value)
const dashboardErrorMessage = computed(() => summaryState.errorMessage.value || activityState.errorMessage.value)
const dashboardSuggestedAction = computed(() => summaryState.suggestedAction.value || activityState.suggestedAction.value)
const dashboardDocsRef = computed(() => summaryState.docsRef.value || activityState.docsRef.value)
const metrics = computed(() => [
  { key: 'fields', label: '标准字段', value: summary.value?.fieldCount ?? 0 },
  { key: 'enums', label: '代码集', value: summary.value?.enumDictCount ?? 0 },
  { key: 'rules', label: '命名规则', value: summary.value?.ruleCount ?? 0 },
  { key: 'forbidden', label: '禁用词', value: summary.value?.forbiddenTermCount ?? 0 },
  { key: 'checks', label: 'SQL 检查', value: summary.value?.recentCheckCount ?? 0 },
  { key: 'hitRate', label: '字段命中率', value: hitRateText(summary.value?.fieldHitRate) }
])
const maxTrendIssueCount = computed(() =>
  Math.max(1, ...(summary.value?.trend ?? []).map((item) => item.issueCount ?? 0))
)
const trendItems = computed(() =>
  (summary.value?.trend ?? []).map((item, index) => ({
    key: `${item.recordId ?? index}-${item.createdAt ?? ''}`,
    time: formatDate(item.createdAt),
    issueCount: item.issueCount ?? 0,
    width: `${Math.max(6, ((item.issueCount ?? 0) / maxTrendIssueCount.value) * 100)}%`
  }))
)
const activityActionOptions = computed(() => activityTimeline.value?.availableActionTypes ?? [])
const activityItems = computed(() => activityTimeline.value?.activities ?? [])
const recentTaskItems = computed(() => {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return []
  }
  return recentTasks.value
    .filter((task) => task.projectId === projectId)
    .sort((left, right) => right.usedAt.localeCompare(left.usedAt))
    .slice(0, 4)
})

onMounted(async () => {
  loadRecentTasks()
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  await loadDashboard()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadDashboard()
  }
)

watch(
  () => activityActionType.value,
  () => {
    void loadActivities()
  }
)

async function loadDashboard() {
  await Promise.allSettled([loadSummary(), loadActivities()])
}

async function loadSummary() {
  if (!projectStore.currentProjectId) {
    summaryState.reset()
    return
  }
  try {
    await summaryState.run(() => getDashboardSummary(projectStore.currentProjectId as number))
  } catch {
    // 页面级 StateBlock 会展示后端返回的建议与重试入口。
  }
}

async function loadActivities() {
  if (!projectStore.currentProjectId) {
    activityState.reset()
    return
  }
  try {
    await activityState.run(() => listProjectActivities(
      projectStore.currentProjectId as number,
      activityActionType.value || undefined,
      20
    ))
  } catch {
    // 活动列表失败不阻塞工作台其余区域，错误状态留给 StateBlock 呈现。
  }
}

async function handleCreateDemoProject() {
  const result = await ensureDemoProject()
  ElMessage.success(result.created ? '演示项目已创建' : '已切换到演示项目')
  await loadDashboard()
}

function openTask(task: DashboardTask) {
  recordRecentTask(task)
  router.push(task.route)
}

function openRecentTask(task: RecentTask) {
  router.push(task.route)
}

function goProjects() {
  router.push('/projects')
}

async function openDemoSqlLint() {
  await ensureDemoProject()
  await router.push({ path: '/sql-lint', query: { demo: 'lint' } })
}

async function openDemoDdl() {
  const result = await ensureDemoProject()
  await router.push({
    path: '/generator',
    query: {
      templateId: result.templateId,
      tableName: result.sampleTableName
    }
  })
}

async function ensureDemoProject() {
  demoLoading.value = true
  try {
    return await projectStore.createDemoProjectAndSelect()
  } finally {
    demoLoading.value = false
  }
}

function hitRateText(value?: number | null) {
  return value === null || value === undefined ? '--' : `${value}%`
}

function formatDate(value?: string) {
  if (!value) {
    return '--'
  }
  return value.replace('T', ' ').slice(0, 16)
}

function goActivity(route?: string) {
  if (!route) {
    return
  }
  router.push(route)
}

function loadRecentTasks() {
  try {
    const raw = localStorage.getItem(RECENT_TASKS_KEY)
    if (!raw) {
      recentTasks.value = []
      return
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      throw new Error('recent tasks must be an array')
    }
    const cleaned = parsed.filter(isRecentTask).slice(0, 20)
    recentTasks.value = cleaned
    if (cleaned.length !== parsed.length) {
      localStorage.setItem(RECENT_TASKS_KEY, JSON.stringify(cleaned))
    }
  } catch {
    localStorage.removeItem(RECENT_TASKS_KEY)
    recentTasks.value = []
  }
}

function recordRecentTask(task: DashboardTask) {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  const next: RecentTask = {
    key: task.key,
    title: task.title,
    route: task.route,
    projectId,
    usedAt: new Date().toISOString()
  }
  const deduped = recentTasks.value.filter(
    (item) => !(item.projectId === projectId && item.key === task.key)
  )
  recentTasks.value = [next, ...deduped].slice(0, 20)
  localStorage.setItem(RECENT_TASKS_KEY, JSON.stringify(recentTasks.value))
}

function clearRecentTasks() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  recentTasks.value = recentTasks.value.filter((task) => task.projectId !== projectId)
  localStorage.setItem(RECENT_TASKS_KEY, JSON.stringify(recentTasks.value))
}

function isRecentTask(value: unknown): value is RecentTask {
  if (!value || typeof value !== 'object') {
    return false
  }
  const task = value as Partial<RecentTask>
  return (
    typeof task.key === 'string' &&
    typeof task.title === 'string' &&
    typeof task.route === 'string' &&
    typeof task.projectId === 'number' &&
    typeof task.usedAt === 'string'
  )
}

function severityLabel(value?: string) {
  if (value === 'ERROR') {
    return '错误'
  }
  if (value === 'WARNING') {
    return '警告'
  }
  return '信息'
}

function severityTagType(value?: string) {
  if (value === 'ERROR') {
    return 'danger'
  }
  if (value === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

function activityTimelineType(value?: string) {
  if (value === 'ERROR') {
    return 'danger'
  }
  if (value === 'WARNING') {
    return 'warning'
  }
  return 'primary'
}
</script>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 140px);
  padding: 20px;
  background: #fff;
  border-radius: 4px;
}

.page-header,
.section-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.page-header {
  margin-bottom: 18px;
}

.page-header h2,
.section-header h3 {
  margin: 0;
  font-weight: 600;
  color: #1f2937;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.metric-label {
  color: #6b7280;
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  color: #111827;
  font-size: 28px;
  font-weight: 700;
}

.panel {
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.task-panel {
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.empty-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.task-card {
  min-height: 86px;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #1f2937;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  text-align: left;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.task-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgb(64 158 255 / 12%);
  transform: translateY(-1px);
}

.task-icon {
  color: #409eff;
  font-size: 20px;
}

.task-title {
  font-size: 14px;
  font-weight: 600;
}

.task-meta {
  color: #6b7280;
  font-size: 12px;
}

.demo-task-row,
.recent-task-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.activity-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
  align-items: center;
}

.activity-filter {
  width: 160px;
}

.activity-body {
  min-height: 120px;
  margin-top: 14px;
}

.activity-timeline {
  padding-left: 4px;
}

.activity-item {
  position: relative;
  padding-right: 72px;
}

.activity-title-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.activity-title {
  color: #111827;
  font-weight: 600;
}

.activity-description {
  margin: 6px 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.5;
}

.activity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #6b7280;
  font-size: 12px;
}

.activity-link {
  position: absolute;
  top: -2px;
  right: 0;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.trend-row {
  display: grid;
  grid-template-columns: 140px 1fr 48px;
  gap: 10px;
  align-items: center;
}

.trend-time,
.trend-count {
  color: #4b5563;
  font-size: 13px;
}

.trend-count {
  text-align: right;
}

.trend-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.trend-bar {
  height: 100%;
  border-radius: inherit;
  background: #409eff;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .task-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-header,
  .section-header {
    flex-direction: column;
  }

  .metric-grid,
  .task-grid,
  .trend-row {
    grid-template-columns: 1fr;
  }

  .trend-count {
    text-align: left;
  }

  .activity-actions {
    justify-content: flex-start;
  }

  .activity-filter {
    width: 100%;
  }

  .activity-item {
    padding-right: 0;
  }

  .activity-link {
    position: static;
    margin-top: 8px;
  }
}
</style>
