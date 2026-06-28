<template>
  <div class="ai-replay-page">
    <div class="page-header">
      <div>
        <h2>AI 回放</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-select v-model="jobTypeFilter" class="type-filter" clearable placeholder="全部类型" @change="handleFilterChange">
          <el-option v-for="item in jobTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-tooltip content="刷新记录">
          <el-button aria-label="刷新记录" :disabled="!hasProject" :loading="loading" @click="loadJobs">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <el-table :data="records" stripe empty-text="暂无 AI 回放记录" v-loading="loading">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatAiJobTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ aiJobTypeLabel(row.jobType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="inputSummary" label="输入摘要" min-width="240" show-overflow-tooltip />
        <el-table-column label="标准快照" min-width="170">
          <template #default="{ row }">
            <span>{{ snapshotText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="promptVersion" label="模板版本" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="aiJobStatusTagType(row.status)" effect="plain">
              {{ row.status || 'UNKNOWN' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </template>

    <el-dialog v-model="detailVisible" title="AI 回放详情" width="860px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="activeDetail">
          <el-descriptions :column="2" border class="detail-meta">
            <el-descriptions-item label="类型">{{ aiJobTypeLabel(activeRecord?.jobType) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="aiJobStatusTagType(activeRecord?.status)" effect="plain">
                {{ activeRecord?.status || 'UNKNOWN' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="模板版本">{{ activeRecord?.promptVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="标准快照">{{ snapshotText(activeRecord) }}</el-descriptions-item>
            <el-descriptions-item label="关联检查记录">{{ activeRecord?.sqlCheckRecordId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatAiJobTime(activeRecord?.createdAt) }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="lintSummary" class="lint-summary">
            <span class="lint-summary-label">Lint</span>
            <el-tag size="small" type="danger" effect="plain">错误 {{ lintSummary.errorCount }}</el-tag>
            <el-tag size="small" type="warning" effect="plain">警告 {{ lintSummary.warningCount }}</el-tag>
            <el-tag size="small" type="info" effect="plain">建议 {{ lintSummary.suggestionCount }}</el-tag>
          </div>

          <div class="detail-actions">
            <el-button size="small" @click="copyReplayLink">复制链接</el-button>
            <el-button size="small" @click="copyText(activeDetail.replayCommand || '')">复制命令</el-button>
            <el-button size="small" type="primary" @click="copyText(replayJson)">复制 JSON</el-button>
          </div>

          <el-tabs class="detail-tabs">
            <el-tab-pane label="输入">
              <pre class="json-code">{{ inputJson }}</pre>
            </el-tab-pane>
            <el-tab-pane label="输出">
              <pre class="json-code">{{ outputJson }}</pre>
            </el-tab-pane>
            <el-tab-pane label="回放">
              <pre class="json-code">{{ replayJson }}</pre>
            </el-tab-pane>
            <el-tab-pane label="命令">
              <pre class="json-code">{{ activeDetail.replayCommand }}</pre>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getAiJobDetail, listAiJobs } from '@/api/aiJob'
import { useProjectStore } from '@/stores/project'
import {
  aiJobStatusTagType,
  aiJobTypeLabel,
  buildReplayJson,
  formatAiJobTime
} from '@/utils/aiReplayDisplay'
import { copyRouteUrl, readEnumQuery, readPositiveIntQuery, replaceRouteQuery } from '@/utils/urlState'
import type { AiJobRecord, AiJobRecordDetail, AiJobRecordListItem } from '@/types'

interface LintSummary {
  errorCount: number
  warningCount: number
  suggestionCount: number
}

const projectStore = useProjectStore()
const route = useRoute()
const router = useRouter()
const records = ref<AiJobRecordListItem[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const activeDetail = ref<AiJobRecordDetail | null>(null)
const jobTypeFilter = ref('')
const jobTypeOptions = [
  { label: '建表 Prompt', value: 'CREATE_TABLE_PROMPT' },
  { label: '修 SQL Prompt', value: 'FIX_SQL_PROMPT' },
  { label: 'SQL 检查修正', value: 'SQL_LINT_FIX' },
  { label: 'DDL 预览', value: 'DDL_PREVIEW' }
]

const hasProject = computed(() => projectStore.currentProjectId !== null)
const activeRecord = computed(() => activeDetail.value?.record)
const inputJson = computed(() => buildReplayJson(activeDetail.value?.inputPayload))
const outputJson = computed(() => buildReplayJson(activeDetail.value?.outputPayload))
const replayJson = computed(() => buildReplayJson(activeDetail.value?.replayPayload))
const lintSummary = computed(
  () => extractLintSummary(activeDetail.value?.outputPayload) ?? extractLintSummary(activeDetail.value?.inputPayload)
)

onMounted(() => {
  applyReplayUrlState()
  loadJobs()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    applyReplayUrlState()
    activeDetail.value = null
    detailVisible.value = false
    loadJobs()
  }
)

watch(
  () => [route.query.jobType, route.query.page, route.query.size],
  () => {
    applyReplayUrlState()
    loadJobs()
  }
)

watch(
  () => route.query.aiJobId,
  () => {
    void openDetailFromRoute()
  }
)

watch(detailVisible, (visible) => {
  if (!visible && route.query.aiJobId) {
    activeDetail.value = null
    void syncReplayUrlState({ aiJobId: null })
  }
})

async function loadJobs() {
  if (!projectStore.currentProjectId) {
    records.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const page = await listAiJobs(projectStore.currentProjectId, current.value, size.value, jobTypeFilter.value)
    records.value = page.records ?? []
    total.value = page.total ?? 0
    await openDetailFromRoute()
  } finally {
    loading.value = false
  }
}

async function openDetail(id?: number) {
  if (!id) {
    return
  }
  activeDetail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    activeDetail.value = await getAiJobDetail(id)
    await syncReplayUrlState({ aiJobId: id })
  } catch {
    ElMessage.warning('链接中的 AI 回放记录不存在或不可访问')
    detailVisible.value = false
    await syncReplayUrlState({ aiJobId: null })
  } finally {
    detailLoading.value = false
  }
}

function handleFilterChange() {
  current.value = 1
  void syncReplayUrlState()
}

function handleSizeChange() {
  current.value = 1
  void syncReplayUrlState()
}

function handlePageChange() {
  void syncReplayUrlState()
}

async function openDetailFromRoute() {
  const aiJobId = readPositiveIntQuery(route.query, 'aiJobId')
  if (!aiJobId) {
    if (detailVisible.value) {
      detailVisible.value = false
      activeDetail.value = null
    }
    return
  }
  if (!projectStore.currentProjectId || detailLoading.value) {
    return
  }
  if (detailVisible.value && activeRecord.value?.id === aiJobId) {
    return
  }
  await openDetail(aiJobId)
}

function applyReplayUrlState() {
  current.value = readPositiveIntQuery(route.query, 'page') ?? 1
  size.value = readPositiveIntQuery(route.query, 'size') ?? 10
  const jobType = readEnumQuery(route.query, 'jobType', jobTypeOptions.map((item) => item.value))
  if (route.query.jobType && !jobType) {
    ElMessage.warning('链接中的 AI 回放类型筛选无效，已恢复为全部')
    void syncReplayUrlState({ jobType: null })
  }
  jobTypeFilter.value = jobType ?? ''
}

async function syncReplayUrlState(patch: Record<string, string | number | null> = {}) {
  await replaceRouteQuery(router, route, {
    projectId: projectStore.currentProjectId,
    jobType: jobTypeFilter.value || null,
    page: current.value > 1 ? current.value : null,
    size: size.value !== 10 ? size.value : null,
    ...patch
  })
}

async function copyReplayLink() {
  const aiJobId = activeRecord.value?.id
  if (!aiJobId) {
    return
  }
  try {
    await syncReplayUrlState({ aiJobId })
    await copyRouteUrl(route, navigator.clipboard)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.error('复制失败，请手动复制浏览器地址')
  }
}

function snapshotText(record?: AiJobRecord | AiJobRecordListItem) {
  if (!record) {
    return '-'
  }
  if (record.standardSnapshotVersion) {
    const hash = record.standardSnapshotHash ? ` (${record.standardSnapshotHash.slice(0, 8)})` : ''
    return `${record.standardSnapshotVersion}${hash}`
  }
  return 'unversioned'
}

function extractLintSummary(payload?: unknown): LintSummary | null {
  if (!isRecord(payload)) {
    return null
  }
  const source = isRecord(payload.lintSummary) ? payload.lintSummary : payload
  const hasLintCount =
    'errorCount' in source ||
    'warningCount' in source ||
    'suggestionCount' in source
  if (!hasLintCount) {
    return null
  }
  return {
    errorCount: toNumber(source.errorCount),
    warningCount: toNumber(source.warningCount),
    suggestionCount: toNumber(source.suggestionCount)
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function toNumber(value: unknown) {
  return typeof value === 'number' ? value : 0
}

async function copyText(text: string) {
  if (!text) {
    return
  }
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}
</script>

<style scoped>
.ai-replay-page {
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

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.type-filter {
  width: 180px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-meta {
  margin-bottom: 12px;
}

.detail-body {
  min-height: 220px;
}

.lint-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.lint-summary-label {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin: 12px 0;
}

.detail-tabs {
  margin-top: 8px;
}

.json-code {
  max-height: 360px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #0f172a;
  color: #e5e7eb;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions,
  .type-filter {
    width: 100%;
  }
}
</style>
