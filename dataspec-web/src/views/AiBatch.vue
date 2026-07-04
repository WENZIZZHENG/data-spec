<template>
  <div class="ai-batch-page">
    <div class="page-header">
      <div>
        <h2>AI 批量任务</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-tooltip content="刷新任务">
          <el-button aria-label="刷新任务" :disabled="!hasProject" :loading="listLoading || taskRunLoading" @click="refreshPage">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <ProjectRequired
      v-if="!hasProject"
      :has-project="hasProject"
      title="请先创建并选择项目"
      @action="goProjects"
    />

    <template v-else>
      <section v-if="recoverableTaskRuns.length > 0" class="recoverable-panel" v-loading="taskRunLoading">
        <div class="panel-heading">
          <div>
            <h3>最近可恢复任务</h3>
            <p>失败或部分失败的 AI task run，可复制恢复命令继续处理。</p>
          </div>
          <el-button size="small" text type="primary" @click="loadRecoverableTasks">刷新</el-button>
        </div>
        <el-table :data="recoverableTaskRuns" size="small" border empty-text="暂无可恢复任务">
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatAiBatchTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ row.taskType || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="taskRunTagType(row.status)" effect="plain">
                {{ taskRunStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="failedStep" label="失败步骤" min-width="140" show-overflow-tooltip />
          <el-table-column label="可重试" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.retryable ? 'success' : 'info'" effect="plain">
                {{ row.retryable ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="nextAction" label="下一步" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="copyResumeCommand(row.resumeCommand)">
                复制命令
              </el-button>
              <el-button size="small" text type="primary" @click="copyText(JSON.stringify(row, null, 2))">
                复制 JSON
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <StateBlock
        v-if="listState.errorMessage.value"
        type="error"
        title="AI 批量任务加载失败"
        :description="listState.errorMessage.value"
        :suggested-action="listState.suggestedAction.value"
        :docs-ref="listState.docsRef.value"
        action-text="重试"
        @action="refreshPage"
      />
      <StateBlock
        v-else-if="!listLoading && records.length === 0"
        type="empty"
        title="暂无 AI 批量任务"
        description="通过 CLI lint-files 或后续 AI 批量流程生成任务后，这里会显示交付包和证据包入口。"
        action-text="刷新"
        @action="loadBatches"
      />
      <el-table v-else :data="records" stripe empty-text="暂无 AI 批量任务" v-loading="listLoading">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatAiBatchTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.batchType || 'SQL_LINT' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="120" show-overflow-tooltip />
        <el-table-column label="任务摘要" min-width="260">
          <template #default="{ row }">
            <div class="summary-tags">
              <el-tag size="small" effect="plain">项 {{ row.summary?.totalItems ?? 0 }}</el-tag>
              <el-tag size="small" type="danger" effect="plain">错 {{ row.summary?.errorCount ?? 0 }}</el-tag>
              <el-tag size="small" type="warning" effect="plain">警 {{ row.summary?.warningCount ?? 0 }}</el-tag>
              <el-tag size="small" type="info" effect="plain">修正 {{ row.summary?.fixedSqlCount ?? 0 }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作者" min-width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="aiBatchStatusTagType(row.status)" effect="plain">
              {{ aiBatchStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button size="small" text type="primary" @click="handleDownload(row.id)">下载</el-button>
            <el-button
              size="small"
              text
              type="primary"
              :loading="evidenceActionId === row.id"
              @click="handleDownloadEvidence(row.id)"
            >
              证据包
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="records.length > 0" class="pagination-bar">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadBatches"
          @size-change="handleSizeChange"
        />
      </div>
    </template>

    <el-dialog v-model="detailVisible" title="AI 批量任务详情" width="960px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="activeDetail">
          <el-descriptions :column="2" border class="detail-meta">
            <el-descriptions-item label="批次 ID">{{ activePackage?.batchId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="aiBatchStatusTagType(activePackage?.status)" effect="plain">
                {{ aiBatchStatusLabel(activePackage?.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="来源">{{ activePackage?.source || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatAiBatchTime(activePackage?.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="交付包版本">{{ activePackage?.packageVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="操作者">{{ activeDetail.run?.operatorName || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="summary-panel">
            <el-tag type="danger" effect="plain">错误 {{ activeSummary?.errorCount ?? 0 }}</el-tag>
            <el-tag type="warning" effect="plain">警告 {{ activeSummary?.warningCount ?? 0 }}</el-tag>
            <el-tag type="info" effect="plain">建议 {{ activeSummary?.suggestionCount ?? 0 }}</el-tag>
            <el-tag effect="plain">fixedSql {{ activeSummary?.fixedSqlCount ?? 0 }}</el-tag>
          </div>

          <div v-if="activeTaskRun || activeTaskRunDetail" class="task-run-panel">
            <div class="panel-heading">
              <div>
                <h3>AI task run</h3>
                <p>{{ linkedNextAction || '该批量任务已绑定运行状态，可用于失败恢复和交接。' }}</p>
              </div>
              <el-tag size="small" :type="taskRunTagType(linkedTaskStatus)" effect="plain">
                {{ taskRunStatusLabel(linkedTaskStatus) }}
              </el-tag>
            </div>
            <div class="task-run-grid">
              <div>
                <span>Task Run ID</span>
                <strong>{{ linkedTaskRunId || '-' }}</strong>
              </div>
              <div>
                <span>失败步骤</span>
                <strong>{{ linkedFailedStep || '-' }}</strong>
              </div>
              <div>
                <span>可重试</span>
                <strong>{{ linkedRetryable ? '是' : '否' }}</strong>
              </div>
              <div>
                <span>来源</span>
                <strong>{{ activeTaskRunDetail?.sourceType || 'AI_BATCH' }}</strong>
              </div>
            </div>
            <div v-if="linkedResumeCommand" class="resume-command">
              <code>{{ linkedResumeCommand }}</code>
              <el-button size="small" type="primary" @click="copyResumeCommand(linkedResumeCommand)">
                复制恢复命令
              </el-button>
            </div>
            <el-table
              v-if="activeTaskRunDetail?.partialArtifacts?.length"
              :data="activeTaskRunDetail.partialArtifacts"
              size="small"
              border
              class="artifact-table"
            >
              <el-table-column prop="type" label="Artifact" min-width="140" show-overflow-tooltip />
              <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="ref" label="引用" min-width="160" show-overflow-tooltip />
              <el-table-column label="摘要" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ formatArtifactSummary(row.summary) }}</template>
              </el-table-column>
            </el-table>
          </div>

          <div class="detail-actions">
            <el-button size="small" @click="copyText(packageJson)">复制 JSON</el-button>
            <el-button size="small" type="primary" @click="handleDownload(activeDetail.run?.id)">下载 JSON</el-button>
            <el-button
              size="small"
              :loading="evidenceActionId === activeDetail.run?.id"
              @click="handleCopyEvidence(activeDetail.run?.id)"
            >
              复制证据 JSON
            </el-button>
            <el-button
              size="small"
              type="primary"
              :loading="evidenceActionId === activeDetail.run?.id"
              @click="handleDownloadEvidence(activeDetail.run?.id)"
            >
              下载证据包
            </el-button>
          </div>

          <el-tabs class="detail-tabs">
            <el-tab-pane label="分项结果">
              <el-table :data="activePackage?.items ?? []" size="small" border empty-text="暂无分项结果">
                <el-table-column label="文件/项" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.filePath || row.itemName || '-' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="plain">
                      {{ row.status || '-' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="错误" width="80">
                  <template #default="{ row }">{{ row.errorCount ?? 0 }}</template>
                </el-table-column>
                <el-table-column label="警告" width="80">
                  <template #default="{ row }">{{ row.warningCount ?? 0 }}</template>
                </el-table-column>
                <el-table-column label="建议" width="80">
                  <template #default="{ row }">{{ row.suggestionCount ?? 0 }}</template>
                </el-table-column>
                <el-table-column label="fixedSql" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.fixedSqlAvailable ? 'success' : 'info'" effect="plain">
                      {{ row.fixedSqlAvailable ? '可用' : '无' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="errorMessage" label="失败原因" min-width="180" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="问题">
              <div class="issue-list">
                <div v-for="item in issueItems" :key="item.key" class="issue-item">
                  <el-tag size="small" :type="issueTagType(item.issue.severity)" effect="plain">
                    {{ item.issue.severity || 'UNKNOWN' }}
                  </el-tag>
                  <span class="issue-path">{{ item.path }}</span>
                  <span class="issue-rule">{{ item.issue.ruleCode }}</span>
                  <span>{{ item.issue.message }}</span>
                </div>
                <el-empty v-if="issueItems.length === 0" description="暂无问题" />
              </div>
            </el-tab-pane>
            <el-tab-pane label="fixedSql">
              <div class="fixed-sql-list">
                <div v-for="item in fixedSqlItems" :key="item.filePath || item.itemName" class="fixed-sql-item">
                  <div class="fixed-sql-title">{{ item.filePath || item.itemName }}</div>
                  <pre class="code-block">{{ item.fixedSql }}</pre>
                </div>
                <el-empty v-if="fixedSqlItems.length === 0" description="暂无 fixedSql" />
              </div>
            </el-tab-pane>
            <el-tab-pane label="交付包">
              <pre class="json-code">{{ packageJson }}</pre>
            </el-tab-pane>
            <el-tab-pane label="下一步">
              <el-timeline>
                <el-timeline-item v-for="action in activePackage?.nextActions ?? []" :key="action">
                  {{ action }}
                </el-timeline-item>
              </el-timeline>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { downloadAiBatchPackage, getAiBatchDetail, listAiBatches } from '@/api/aiBatch'
import { getAiTaskRunDetail, listRecentAiTaskFailures } from '@/api/aiTaskRun'
import { downloadEvidencePackage, generateEvidencePackage } from '@/api/evidence'
import ProjectRequired from '@/components/ProjectRequired.vue'
import StateBlock from '@/components/StateBlock.vue'
import { useRequestState } from '@/composables/useRequestState'
import { useProjectStore } from '@/stores/project'
import {
  aiBatchStatusLabel,
  aiBatchStatusTagType,
  buildAiBatchJson,
  formatAiBatchTime
} from '@/utils/aiBatchDisplay'
import type {
  AiBatchItemResult,
  AiBatchRunDetail,
  AiBatchRunListItem,
  AiTaskRunDetail,
  AiTaskRunListItem,
  LintIssue,
  PageResult
} from '@/types'

const projectStore = useProjectStore()
const router = useRouter()
const records = ref<AiBatchRunListItem[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const listState = useRequestState<PageResult<AiBatchRunListItem>>()
const detailLoading = ref(false)
const detailVisible = ref(false)
const activeDetail = ref<AiBatchRunDetail | null>(null)
const activeTaskRunDetail = ref<AiTaskRunDetail | null>(null)
const recoverableTaskRuns = ref<AiTaskRunListItem[]>([])
const taskRunLoading = ref(false)
const evidenceActionId = ref<number | null>(null)

const hasProject = computed(() => projectStore.currentProjectId !== null)
const listLoading = computed(() => listState.loading.value)
const activePackage = computed(() => activeDetail.value?.deliveryPackage)
const activeTaskRun = computed(() => activePackage.value?.taskRun)
const activeSummary = computed(() => activePackage.value?.summary)
const packageJson = computed(() => buildAiBatchJson(activePackage.value))
const linkedTaskRunId = computed(() => activeTaskRunDetail.value?.id ?? activeTaskRun.value?.taskRunId)
const linkedTaskStatus = computed(() => activeTaskRunDetail.value?.status ?? activeTaskRun.value?.status)
const linkedRetryable = computed(() => activeTaskRunDetail.value?.retryable ?? activeTaskRun.value?.retryable ?? false)
const linkedFailedStep = computed(() => activeTaskRunDetail.value?.failedStep ?? activeTaskRun.value?.failedStep)
const linkedResumeCommand = computed(() => activeTaskRunDetail.value?.resumeCommand ?? activeTaskRun.value?.resumeCommand)
const linkedNextAction = computed(() => activeTaskRunDetail.value?.nextAction ?? activeTaskRun.value?.nextAction)
const issueItems = computed(() => {
  const items: Array<{ key: string; path: string; issue: LintIssue }> = []
  for (const item of activePackage.value?.items ?? []) {
    for (const issue of item.issues ?? []) {
      items.push({
        key: `${item.filePath || item.itemName}-${issue.ruleCode}-${items.length}`,
        path: item.filePath || item.itemName || '-',
        issue
      })
    }
  }
  return items
})
const fixedSqlItems = computed<AiBatchItemResult[]>(() =>
  (activePackage.value?.items ?? []).filter((item) => item.fixedSqlAvailable && item.fixedSql)
)

onMounted(() => {
  void refreshPage()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    current.value = 1
    activeDetail.value = null
    activeTaskRunDetail.value = null
    detailVisible.value = false
    void refreshPage()
  }
)

async function refreshPage() {
  await Promise.all([loadBatches(), loadRecoverableTasks()])
}

async function loadBatches() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    records.value = []
    total.value = 0
    listState.reset()
    return
  }
  try {
    await listState.run(async () => {
      const page = await listAiBatches(projectId, current.value, size.value)
      records.value = page.records ?? []
      total.value = page.total ?? 0
      return page
    })
  } catch {
    // 统一错误块负责展示建议与重试入口，避免列表刷新散落未处理异常。
  }
}

async function loadRecoverableTasks() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    recoverableTaskRuns.value = []
    return
  }
  taskRunLoading.value = true
  try {
    recoverableTaskRuns.value = await listRecentAiTaskFailures(projectId, 5)
  } catch {
    recoverableTaskRuns.value = []
  } finally {
    taskRunLoading.value = false
  }
}

async function openDetail(id?: number) {
  if (!id) {
    return
  }
  activeDetail.value = null
  activeTaskRunDetail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    activeDetail.value = await getAiBatchDetail(id)
    await loadLinkedTaskRunDetail()
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function loadLinkedTaskRunDetail() {
  const projectId = projectStore.currentProjectId
  const taskRunId = activeDetail.value?.deliveryPackage?.taskRun?.taskRunId
  if (!projectId || !taskRunId) {
    return
  }
  try {
    activeTaskRunDetail.value = await getAiTaskRunDetail(taskRunId, projectId)
  } catch {
    // 交付包本身仍可查看；task run 详情失败时保留 delivery package 内的恢复摘要。
  }
}

async function handleDownload(id?: number) {
  if (!id) {
    return
  }
  saveBlob(await downloadAiBatchPackage(id), `dataspec-ai-batch-${id}.json`)
}

async function handleCopyEvidence(id?: number) {
  const req = evidenceRequest(id)
  if (!req) {
    return
  }
  evidenceActionId.value = id ?? null
  try {
    const evidence = await generateEvidencePackage(req)
    await copyText(JSON.stringify(evidence, null, 2))
  } finally {
    evidenceActionId.value = null
  }
}

async function handleDownloadEvidence(id?: number) {
  const req = evidenceRequest(id)
  if (!req) {
    return
  }
  evidenceActionId.value = id ?? null
  try {
    saveBlob(await downloadEvidencePackage(req), `dataspec-ai-batch-evidence-${id}.zip`)
    ElMessage.success('已下载证据包')
  } finally {
    evidenceActionId.value = null
  }
}

function evidenceRequest(id?: number) {
  if (!id) {
    return null
  }
  return {
    projectId: projectStore.currentProjectId ?? undefined,
    sourceType: 'AI_BATCH_RUN',
    sourceId: id,
    sourceTitle: `AI 批量任务 #${id}`
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

function handleSizeChange() {
  current.value = 1
  void loadBatches()
}

function goProjects() {
  router.push('/projects')
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

async function copyResumeCommand(command?: string | null) {
  if (!command) {
    ElMessage.warning('暂无恢复命令')
    return
  }
  await copyText(command)
}

function issueTagType(severity?: string) {
  if (severity === 'ERROR') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

function taskRunStatusLabel(status?: string | null) {
  if (status === 'SUCCEEDED') {
    return '成功'
  }
  if (status === 'PARTIAL_FAILED') {
    return '部分失败'
  }
  if (status === 'FAILED') {
    return '失败'
  }
  if (status === 'RUNNING') {
    return '运行中'
  }
  return status || '-'
}

function taskRunTagType(status?: string | null) {
  if (status === 'SUCCEEDED') {
    return 'success'
  }
  if (status === 'PARTIAL_FAILED') {
    return 'warning'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

function formatArtifactSummary(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value)
}
</script>

<style scoped>
.ai-batch-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
}

.header-actions,
.summary-tags,
.summary-panel,
.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-body {
  min-height: 260px;
}

.detail-meta,
.summary-panel,
.recoverable-panel,
.task-run-panel,
.detail-actions,
.detail-tabs {
  margin-top: 14px;
}

.recoverable-panel,
.task-run-panel {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-heading h3 {
  margin: 0;
  font-size: 15px;
}

.panel-heading p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.task-run-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.task-run-grid div {
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.task-run-grid span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.task-run-grid strong {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.resume-command {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.resume-command code {
  padding: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: 6px;
  background: #0f172a;
  color: #e5e7eb;
  font-size: 12px;
}

.artifact-table {
  margin-top: 10px;
}

.issue-list,
.fixed-sql-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-item {
  display: grid;
  grid-template-columns: 90px minmax(120px, 1fr) minmax(120px, 180px) minmax(180px, 2fr);
  gap: 8px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.issue-path,
.issue-rule {
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fixed-sql-title {
  margin-bottom: 6px;
  color: #606266;
  font-weight: 600;
}

.json-code,
.code-block {
  margin: 0;
  padding: 12px;
  max-height: 420px;
  overflow: auto;
  background: #0f172a;
  color: #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }

  .issue-item {
    grid-template-columns: 1fr;
  }
}
</style>
