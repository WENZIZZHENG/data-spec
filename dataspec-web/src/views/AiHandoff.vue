<template>
  <div class="ai-handoff-page">
    <div class="page-header">
      <div>
        <h2>AI 交接证据</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-tooltip content="刷新交接记录">
          <el-button aria-label="刷新交接记录" :disabled="!hasProject" :loading="loadState.loading.value" @click="loadPage">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <ProjectRequired
      v-if="!hasProject"
      :has-project="hasProject"
      title="请先创建并选择项目"
      description="AI 交接证据按项目聚合任务、作业、SQL 检查和批量任务，只展示脱敏后的可交接摘要。"
      @action="goProjects"
    />

    <template v-else>
      <StateBlock
        v-if="loadState.errorMessage.value"
        type="error"
        title="交接证据加载失败"
        :description="loadState.errorMessage.value"
        :suggested-action="loadState.suggestedAction.value"
        :next-actions="loadState.nextActions.value"
        :docs-ref="loadState.docsRef.value"
        action-text="重试"
        @action="loadPage"
      />

      <template v-else>
        <section class="metric-grid" v-loading="loadState.loading.value">
          <div class="metric-item">
            <span>任务交接记录</span>
            <strong>{{ taskRuns.length }}</strong>
          </div>
          <div class="metric-item">
            <span>关联证据源</span>
            <strong>{{ sourceOptions.length }}</strong>
          </div>
          <div class="metric-item danger">
            <span>失败或未验证项</span>
            <strong>{{ riskSources.length }}</strong>
          </div>
          <div class="metric-item">
            <span>当前证据包</span>
            <strong>{{ activeEvidence?.packageId || '-' }}</strong>
          </div>
        </section>

        <el-alert
          v-if="loadWarnings.length > 0"
          class="load-warning"
          type="warning"
          :closable="false"
          show-icon
          :title="`部分来源加载失败：${loadWarnings.join('、')}`"
        />

        <section class="handoff-section">
          <div class="section-heading">
            <div>
              <h3>失败或未验证项</h3>
              <p>优先处理非成功状态，避免接手时误判已完成。</p>
            </div>
          </div>
          <StateBlock
            v-if="!loadState.loading.value && riskSources.length === 0"
            type="empty"
            title="暂无失败或未验证项"
            description="最近记录均为成功状态，仍可在下方生成证据包留档。"
          />
          <el-table v-else :data="riskSources" size="small" border empty-text="暂无失败或未验证项">
            <el-table-column label="来源" width="130">
              <template #default="{ row }">{{ evidenceSourceTypeLabel(row.sourceType) }}</template>
            </el-table-column>
            <el-table-column prop="sourceTitle" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="handoffStatusTagType(row.status)" effect="plain">
                  {{ handoffStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="摘要" min-width="260" show-overflow-tooltip />
            <el-table-column prop="nextAction" label="下一步" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" :loading="evidenceLoadingKey === sourceKey(row)" @click="handleGenerateEvidence(row)">
                  生成证据
                </el-button>
                <el-button size="small" text type="primary" @click="handleDownloadEvidence(row)">
                  下载证据包
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="handoff-section">
          <div class="section-heading">
            <div>
              <h3>任务交接记录</h3>
              <p>来自 AI task run 的恢复状态、失败步骤和交接建议。</p>
            </div>
          </div>
          <StateBlock
            v-if="!loadState.loading.value && taskRuns.length === 0"
            type="empty"
            title="暂无任务交接记录"
            description="AI task run 落库后，这里会显示可继续处理的任务链路。"
          />
          <el-table v-else :data="taskRuns" size="small" border empty-text="暂无任务交接记录">
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ formatHandoffTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="taskType" label="类型" min-width="130" show-overflow-tooltip />
            <el-table-column prop="failedStep" label="失败步骤" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="handoffStatusTagType(row.status)" effect="plain">
                  {{ handoffStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nextAction" label="下一步" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openTaskRunDetail(row)">详情</el-button>
                <el-button size="small" text type="primary" :loading="evidenceLoadingKey === taskRunSourceKey(row)" @click="handleGenerateEvidence(taskRunSource(row))">
                  生成证据
                </el-button>
                <el-button size="small" text type="primary" @click="handleDownloadEvidence(taskRunSource(row))">
                  下载证据包
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="handoff-section">
          <div class="section-heading">
            <div>
              <h3>关联证据源</h3>
              <p>汇总 AI 作业、SQL 检查和 AI 批量任务，便于补齐 lint / DDL / Context 产物。</p>
            </div>
          </div>
          <el-table :data="sourceOptions" size="small" border empty-text="暂无关联证据源">
            <el-table-column label="来源" width="130">
              <template #default="{ row }">{{ evidenceSourceTypeLabel(row.sourceType) }}</template>
            </el-table-column>
            <el-table-column prop="sourceTitle" label="标题" min-width="190" show-overflow-tooltip />
            <el-table-column prop="taskType" label="类型" width="140" show-overflow-tooltip />
            <el-table-column label="标准版本" min-width="150">
              <template #default="{ row }">{{ sourceStandardText(row) }}</template>
            </el-table-column>
            <el-table-column label="验证结果" width="130">
              <template #default="{ row }">
                <el-tag size="small" :type="handoffStatusTagType(row.status)" effect="plain">
                  {{ handoffStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="摘要" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" :loading="evidenceLoadingKey === sourceKey(row)" @click="handleGenerateEvidence(row)">
                  生成证据
                </el-button>
                <el-button size="small" text type="primary" @click="handleDownloadEvidence(row)">
                  下载证据包
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="handoff-section evidence-preview">
          <div class="section-heading">
            <div>
              <h3>证据包预览</h3>
              <p>生成后可复制脱敏 JSON，或下载后端打包结果交给下一位接手者。</p>
            </div>
            <div class="section-actions">
              <el-button size="small" :disabled="!activeEvidence" @click="copyEvidenceJson">复制证据 JSON</el-button>
              <el-button size="small" type="primary" :disabled="!selectedSource" @click="handleDownloadEvidence(selectedSource)">
                下载证据包
              </el-button>
            </div>
          </div>

          <StateBlock
            v-if="!activeEvidence"
            type="empty"
            title="尚未生成证据包"
            description="从上方任意来源生成证据包后，这里会展示输入输出摘要、验证结果、标准版本、相关 commit 和 nextActions。"
          />
          <template v-else>
            <el-descriptions :column="3" border class="evidence-meta">
              <el-descriptions-item label="来源">{{ evidenceSourceTypeLabel(activeEvidence.source?.sourceType) }}</el-descriptions-item>
              <el-descriptions-item label="来源 ID">{{ activeEvidence.source?.sourceId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag size="small" :type="handoffStatusTagType(activeEvidence.source?.status)" effect="plain">
                  {{ handoffStatusLabel(activeEvidence.source?.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="标准版本">{{ activeEvidence.standardSnapshot?.specVersion || '-' }}</el-descriptions-item>
              <el-descriptions-item label="相关 commit">{{ evidenceCommitText }}</el-descriptions-item>
              <el-descriptions-item label="生成时间">{{ formatHandoffTime(activeEvidence.generatedAt) }}</el-descriptions-item>
            </el-descriptions>

            <div class="preview-grid">
              <div class="preview-panel">
                <h4>输入 / 输出摘要</h4>
                <pre>{{ summaryJson(activeEvidence.inputsSummary) }}</pre>
                <pre>{{ summaryJson(activeEvidence.outputsSummary) }}</pre>
              </div>
              <div class="preview-panel">
                <h4>验证结果</h4>
                <pre>{{ summaryJson(activeEvidence.validationSummary) }}</pre>
              </div>
              <div class="preview-panel">
                <h4>lint / DDL / Context 产物</h4>
                <el-table :data="activeEvidence.artifacts ?? []" size="small" border empty-text="暂无产物">
                  <el-table-column prop="artifactType" label="类型" width="120" show-overflow-tooltip />
                  <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="format" label="格式" width="90" />
                </el-table>
              </div>
              <div class="preview-panel">
                <h4>nextActions / commands</h4>
                <ul class="compact-list">
                  <li v-for="item in activeEvidence.nextActions ?? []" :key="item">{{ sanitizeHandoffText(item) }}</li>
                </ul>
                <ul class="compact-list command-list">
                  <li v-for="item in activeEvidence.suggestedCommands ?? []" :key="item"><code>{{ sanitizeHandoffText(item) }}</code></li>
                </ul>
              </div>
            </div>
          </template>
        </section>
      </template>
    </template>

    <el-dialog v-model="detailVisible" title="任务交接详情" width="900px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="activeTaskRunDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="Task Run ID">{{ activeTaskRunDetail.id || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="handoffStatusTagType(activeTaskRunDetail.status)" effect="plain">
                {{ handoffStatusLabel(activeTaskRunDetail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="失败步骤">{{ activeTaskRunDetail.failedStep || '-' }}</el-descriptions-item>
            <el-descriptions-item label="可重试">{{ activeTaskRunDetail.retryable ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="下一步">{{ sanitizeHandoffText(activeTaskRunDetail.nextAction || '-') }}</el-descriptions-item>
            <el-descriptions-item label="恢复命令">{{ sanitizeHandoffText(activeTaskRunDetail.resumeCommand || '-') }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-section">
            <h4>步骤状态</h4>
            <el-table :data="activeTaskRunDetail.stepStatus ?? []" size="small" border empty-text="暂无步骤状态">
              <el-table-column label="步骤" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ safeHandoffText(row.step) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag size="small" :type="handoffStatusTagType(row.status)" effect="plain">
                    {{ handoffStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="消息" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">{{ safeHandoffText(row.message) }}</template>
              </el-table-column>
              <el-table-column label="产物引用" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ safeHandoffText(row.artifactRef) }}</template>
              </el-table-column>
            </el-table>
          </div>

          <div class="detail-section">
            <h4>部分产物</h4>
            <el-table :data="activeTaskRunDetail.partialArtifacts ?? []" size="small" border empty-text="暂无部分产物">
              <el-table-column label="类型" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ safeHandoffText(row.type) }}</template>
              </el-table-column>
              <el-table-column label="名称" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ safeHandoffText(row.name) }}</template>
              </el-table-column>
              <el-table-column label="引用" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ safeHandoffText(row.ref) }}</template>
              </el-table-column>
              <el-table-column label="摘要" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ safeHandoffText(row.summary) }}</template>
              </el-table-column>
            </el-table>
          </div>

          <div class="detail-section">
            <h4>脱敏元数据</h4>
            <pre class="metadata-json">{{ summaryJson(activeTaskRunDetail.metadata) }}</pre>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { listAiTaskRuns, getAiTaskRunDetail } from '@/api/aiTaskRun'
import { listAiJobs } from '@/api/aiJob'
import { listLintRecords } from '@/api/lint'
import { listAiBatches } from '@/api/aiBatch'
import { downloadEvidencePackage, generateEvidencePackage } from '@/api/evidence'
import ProjectRequired from '@/components/ProjectRequired.vue'
import StateBlock from '@/components/StateBlock.vue'
import { useRequestState } from '@/composables/useRequestState'
import { useProjectStore } from '@/stores/project'
import {
  buildEvidenceRequest,
  buildHandoffEvidenceJson,
  evidenceSourceTypeLabel,
  formatHandoffTime,
  handoffStatusLabel,
  handoffStatusTagType,
  sanitizeHandoffText
} from '@/utils/handoffEvidenceDisplay'
import type {
  AiBatchRunListItem,
  AiEvidencePackage,
  AiJobRecordListItem,
  AiTaskRunDetail,
  AiTaskRunListItem,
  PageResult,
  SqlCheckRecord
} from '@/types'
import type { HandoffEvidenceSource } from '@/utils/handoffEvidenceDisplay'

interface HandoffEvidenceSourceOption extends HandoffEvidenceSource {
  standardVersion?: string
}

const router = useRouter()
const projectStore = useProjectStore()
const loadState = useRequestState<{ loadedAt: string }>()
const taskRuns = ref<AiTaskRunListItem[]>([])
const aiJobs = ref<AiJobRecordListItem[]>([])
const lintRecords = ref<SqlCheckRecord[]>([])
const aiBatches = ref<AiBatchRunListItem[]>([])
const loadWarnings = ref<string[]>([])
const selectedSource = ref<HandoffEvidenceSource | null>(null)
const activeEvidence = ref<AiEvidencePackage | null>(null)
const evidenceLoadingKey = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const activeTaskRunDetail = ref<AiTaskRunDetail | null>(null)

const hasProject = computed(() => projectStore.currentProjectId !== null)
const sourceOptions = computed<HandoffEvidenceSourceOption[]>(() => [
  ...taskRuns.value.map(taskRunSource),
  ...aiJobs.value.map(aiJobSource),
  ...lintRecords.value.map(sqlCheckSource),
  ...aiBatches.value.map(aiBatchSource)
])
const riskSources = computed(() =>
  sourceOptions.value.filter((source) => handoffStatusTagType(source.status) !== 'success')
)
const evidenceCommitText = computed(() =>
  extractSummaryString(activeEvidence.value?.outputsSummary, ['commit', 'commitSha', 'commitHash', 'gitCommit'])
    || extractSummaryString(activeEvidence.value?.inputsSummary, ['commit', 'commitSha', 'commitHash', 'gitCommit'])
    || '-'
)

onMounted(() => {
  void loadPage()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    resetPage()
    void loadPage()
  }
)

async function loadPage() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    resetPage()
    return
  }
  resetPage()
  await loadState.run(async () => {
    const results = await Promise.allSettled([
      listAiTaskRuns({ projectId, current: 1, size: 10 }),
      listAiJobs(projectId, 1, 10),
      listLintRecords(projectId, 1, 10),
      listAiBatches(projectId, 1, 10)
    ])
    if (projectStore.currentProjectId !== projectId) {
      return { loadedAt: new Date().toISOString() }
    }
    loadWarnings.value = []
    taskRuns.value = recordsFromSettled(results[0], 'AI task run')
    aiJobs.value = recordsFromSettled(results[1], 'AI 作业')
    lintRecords.value = recordsFromSettled(results[2], 'SQL 检查')
    aiBatches.value = recordsFromSettled(results[3], 'AI 批量任务')
    if (loadWarnings.value.length === results.length) {
      throw new Error('交接证据来源全部加载失败')
    }
    return { loadedAt: new Date().toISOString() }
  })
}

function resetPage() {
  loadState.reset()
  taskRuns.value = []
  aiJobs.value = []
  lintRecords.value = []
  aiBatches.value = []
  loadWarnings.value = []
  activeEvidence.value = null
  selectedSource.value = null
  activeTaskRunDetail.value = null
  detailVisible.value = false
}

function recordsFromSettled<T>(result: PromiseSettledResult<PageResult<T>>, label: string): T[] {
  if (result.status === 'fulfilled') {
    return result.value.records ?? []
  }
  loadWarnings.value.push(label)
  return []
}

async function handleGenerateEvidence(source: HandoffEvidenceSource) {
  const request = buildEvidenceRequest(projectStore.currentProjectId, source)
  if (!request) {
    ElMessage.warning('缺少可生成证据包的来源 ID')
    return
  }
  evidenceLoadingKey.value = sourceKey(source)
  activeEvidence.value = null
  selectedSource.value = null
  try {
    const evidence = await generateEvidencePackage(request)
    selectedSource.value = source
    activeEvidence.value = evidence
    ElMessage.success('已生成证据包')
  } finally {
    evidenceLoadingKey.value = ''
  }
}

async function handleDownloadEvidence(source: HandoffEvidenceSource | null) {
  const request = buildEvidenceRequest(projectStore.currentProjectId, source)
  if (!request) {
    ElMessage.warning('请先选择可下载的证据来源')
    return
  }
  const filename = `dataspec-handoff-evidence-${request.sourceType}-${request.sourceId}.zip`
  saveBlob(await downloadEvidencePackage(request), filename)
  ElMessage.success('已下载证据包')
}

async function openTaskRunDetail(row: AiTaskRunListItem) {
  const projectId = projectStore.currentProjectId
  if (!projectId || !row.id) {
    return
  }
  detailVisible.value = true
  detailLoading.value = true
  activeTaskRunDetail.value = null
  try {
    activeTaskRunDetail.value = await getAiTaskRunDetail(row.id, projectId)
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function copyEvidenceJson() {
  if (!activeEvidence.value) {
    return
  }
  await copyText(buildHandoffEvidenceJson(activeEvidence.value))
}

function taskRunSource(row: AiTaskRunListItem): HandoffEvidenceSourceOption {
  return {
    sourceType: 'AI_TASK_RUN',
    sourceId: row.id,
    sourceTitle: `AI task run #${row.id ?? '-'}`,
    status: row.status,
    createdAt: row.createdAt,
    taskType: row.taskType,
    nextAction: sanitizeHandoffText(row.nextAction || ''),
    description: sanitizeHandoffText([row.failedStep ? `失败步骤 ${row.failedStep}` : '', row.retryable ? '可重试' : ''].filter(Boolean).join(' · '))
  }
}

function aiJobSource(row: AiJobRecordListItem): HandoffEvidenceSourceOption {
  return {
    sourceType: 'AI_JOB',
    sourceId: row.id,
    sourceTitle: sanitizeHandoffText(row.title || `AI 作业 #${row.id ?? '-'}`),
    status: row.status,
    createdAt: row.createdAt,
    taskType: row.jobType,
    description: sanitizeHandoffText(row.inputSummary || ''),
    standardVersion: row.standardSnapshotVersion
  }
}

function sqlCheckSource(row: SqlCheckRecord): HandoffEvidenceSourceOption {
  return {
    sourceType: 'SQL_CHECK',
    sourceId: row.id,
    sourceTitle: `SQL 检查 #${row.id ?? '-'}`,
    status: sqlCheckStatus(row),
    createdAt: row.createdAt,
    taskType: 'SQL_LINT',
    description: `错误 ${row.errorCount ?? 0} / 警告 ${row.warningCount ?? 0} / 建议 ${row.suggestionCount ?? 0}`,
    standardVersion: row.standardSnapshotVersion
  }
}

function aiBatchSource(row: AiBatchRunListItem): HandoffEvidenceSourceOption {
  return {
    sourceType: 'AI_BATCH_RUN',
    sourceId: row.id,
    sourceTitle: `AI 批量任务 #${row.id ?? '-'}`,
    status: row.status,
    createdAt: row.createdAt,
    taskType: row.batchType,
    description: `项 ${row.summary?.totalItems ?? 0} / 错 ${row.summary?.errorCount ?? 0} / 警 ${row.summary?.warningCount ?? 0}`
  }
}

function sqlCheckStatus(row: SqlCheckRecord) {
  if ((row.errorCount ?? 0) > 0) {
    return 'FAILED'
  }
  if ((row.warningCount ?? 0) > 0 || (row.suggestionCount ?? 0) > 0) {
    return 'PARTIAL_FAILED'
  }
  return 'SUCCESS'
}

function sourceStandardText(source: HandoffEvidenceSourceOption) {
  return source.standardVersion || '-'
}

function sourceKey(source: HandoffEvidenceSource) {
  return `${source.sourceType}:${source.sourceId ?? 'none'}`
}

function taskRunSourceKey(row: AiTaskRunListItem) {
  return sourceKey(taskRunSource(row))
}

function summaryJson(value: unknown) {
  if (!value) {
    return '-'
  }
  return buildHandoffEvidenceJson(value)
}

function safeHandoffText(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return sanitizeHandoffText(String(value))
}

function extractSummaryString(value: unknown, keys: string[]) {
  if (!value || typeof value !== 'object') {
    return ''
  }
  const record = value as Record<string, unknown>
  for (const key of keys) {
    const candidate = record[key]
    if (typeof candidate === 'string' && candidate.trim()) {
      return sanitizeHandoffText(candidate.trim())
    }
  }
  return ''
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      ElMessage.success('已复制')
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
  ElMessage.success('已复制')
}

function goProjects() {
  router.push('/projects')
}
</script>

<style scoped>
.ai-handoff-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  color: #1f2937;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #64748b;
}

.header-actions,
.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid #d8dee8;
  border-radius: 6px;
  background: #fff;
}

.metric-item span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.metric-item strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 24px;
}

.metric-item.danger strong {
  color: #b91c1c;
}

.load-warning {
  border-radius: 6px;
}

.handoff-section {
  padding: 16px;
  border: 1px solid #d8dee8;
  border-radius: 6px;
  background: #fff;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-heading h3 {
  margin: 0;
  font-size: 17px;
  color: #1f2937;
}

.section-heading p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.evidence-meta {
  margin-bottom: 14px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.preview-panel {
  min-width: 0;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.preview-panel h4,
.detail-section h4 {
  margin: 0 0 10px;
  color: #374151;
  font-size: 14px;
}

pre,
.metadata-json {
  max-height: 260px;
  margin: 0 0 10px;
  padding: 10px;
  overflow: auto;
  border-radius: 4px;
  background: #0f172a;
  color: #e5edf7;
  font-size: 12px;
  line-height: 1.5;
}

.compact-list {
  margin: 0 0 10px;
  padding-left: 18px;
  color: #374151;
  line-height: 1.6;
}

.command-list code {
  word-break: break-all;
}

.detail-body {
  min-height: 240px;
}

.detail-section {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .metric-grid,
  .preview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-header,
  .section-heading {
    flex-direction: column;
    align-items: stretch;
  }

  .metric-grid,
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
