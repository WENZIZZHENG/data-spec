<template>
  <div class="ai-export-page" :data-testid="stableTestIds.aiContext.page">
    <div class="page-header">
      <div>
        <h2>AI Context</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!hasProject" :loading="previewLoading" @click="loadPreviews">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button
          type="primary"
          :disabled="!hasProject"
          :loading="downloadLoading"
          @click="handleDownloadPackage"
        >
          <el-icon><Download /></el-icon>
          下载 Zip
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" :loading="demoLoading" @click="handleCreateDemoProject">
        创建演示项目
      </el-button>
    </el-empty>

    <template v-else>
      <div class="scope-toolbar" :data-testid="stableTestIds.aiContext.scopeToolbar">
        <el-segmented v-model="scopeForm.scope" :options="scopeOptions" />
        <el-input
          v-model="scopeForm.query"
          class="scope-query"
          clearable
          placeholder="关键词"
          @keyup.enter="loadPreviews"
        />
        <el-select v-model="scopeForm.status" class="scope-status" clearable placeholder="状态">
          <el-option label="enabled" value="enabled" />
          <el-option label="disabled" value="disabled" />
          <el-option label="deprecated" value="deprecated" />
        </el-select>
        <el-input-number
          v-model="scopeForm.limit"
          class="scope-limit"
          :min="1"
          :max="50"
          :step="10"
          controls-position="right"
          placeholder="上限"
        />
        <el-select
          v-model="scopeForm.snapshotId"
          class="snapshot-select"
          :loading="snapshotLoading"
          placeholder="标准版本"
        >
          <el-option label="当前标准" :value="0" />
          <el-option
            v-for="snapshot in snapshotOptions"
            :key="snapshot.snapshotId"
            :label="snapshotOptionLabel(snapshot)"
            :value="snapshot.snapshotId"
          />
        </el-select>
        <el-button @click="handleResetScope">重置</el-button>
      </div>

      <div class="post-check-guidance">
        <span class="budget-section-label">Post-check</span>
        <code>{{ postCheckCommand }}</code>
      </div>

      <div v-if="aiContextStandardQuerySummary" class="standard-query-guidance">
        <span class="budget-section-label">Standard Query</span>
        <code>{{ aiContextStandardQuerySummary }}</code>
      </div>

      <div class="semantic-artifacts">
        <span class="budget-section-label">语义证据</span>
        <div class="semantic-artifact-list">
          <el-tag
            v-for="artifact in semanticArtifacts"
            :key="artifact"
            effect="plain"
            type="info"
          >
            {{ artifact }}
          </el-tag>
        </div>
        <span class="semantic-artifact-note">metadata guidance，不作为可执行 SQL 或真实计算结果。</span>
      </div>

      <div class="budget-preview">
        <div class="budget-controls">
          <div class="budget-heading">
            <span class="budget-title">预算预览</span>
            <span class="budget-summary-line">{{ buildBudgetPlanSummary(budgetPlan) }}</span>
          </div>
          <div class="budget-actions">
            <el-input-number
              v-model="budgetForm.tokenBudget"
              class="budget-token-input"
              :min="1"
              :max="200000"
              :step="1000"
              controls-position="right"
              placeholder="Token 预算"
            />
            <el-button type="primary" :loading="budgetLoading" @click="handlePlanBudget">
              <el-icon><DataAnalysis /></el-icon>
              计划
            </el-button>
          </div>
        </div>

        <div v-if="budgetPlan" class="budget-result">
          <div class="budget-metrics">
            <div class="budget-metric">
              <span>估算 tokens</span>
              <strong>{{ formatEstimatedTokens(budgetPlan.estimation) }}</strong>
            </div>
            <div class="budget-metric">
              <span>质量风险</span>
              <el-tag :type="budgetRiskTagType(budgetPlan.qualityRisk)">
                {{ budgetRiskLabel(budgetPlan.qualityRisk) }}
              </el-tag>
            </div>
            <div class="budget-metric">
              <span>字段命中</span>
              <strong>
                {{ budgetPlan.request.returnedFieldCount }} / {{ budgetPlan.request.matchedFieldCount }}
              </strong>
            </div>
            <el-button
              size="small"
              :disabled="!hasRecommendedExportParams"
              @click="handleApplyRecommendedParams"
            >
              一键填充
            </el-button>
          </div>

          <div v-if="recommendedParamItems.length" class="budget-recommended">
            <span class="budget-section-label">推荐导出参数</span>
            <el-tag
              v-for="item in recommendedParamItems"
              :key="item.key"
              effect="plain"
              type="info"
            >
              {{ item.label }}={{ item.value }}
            </el-tag>
          </div>

          <div class="budget-artifacts">
            <div class="budget-artifact-list">
              <div class="budget-section-label">Selected artifacts</div>
              <ul>
                <li
                  v-for="(artifact, index) in budgetPlan.selectedArtifacts"
                  :key="`selected-${artifact.artifact}-${index}`"
                >
                  <span>{{ artifact.artifact }}</span>
                  <strong>{{ artifact.estimatedTokens.toLocaleString('en-US') }}</strong>
                </li>
              </ul>
            </div>
            <div class="budget-artifact-list dropped">
              <div class="budget-section-label">Dropped artifacts</div>
              <ul v-if="budgetPlan.droppedArtifacts.length">
                <li
                  v-for="(artifact, index) in budgetPlan.droppedArtifacts"
                  :key="`dropped-${artifact.artifact}-${index}`"
                >
                  <span>{{ artifact.artifact }}</span>
                  <strong>{{ artifact.estimatedTokens.toLocaleString('en-US') }}</strong>
                </li>
              </ul>
              <p v-else class="budget-empty">无舍弃项</p>
            </div>
          </div>

          <div
            v-if="budgetPlan.diagnostics.length || budgetPlan.fallbackSteps.length || budgetPlan.recommendedNextActions.length"
            class="budget-notes"
          >
            <el-tag
              v-for="item in budgetPlan.diagnostics"
              :key="`diagnostic-${item}`"
              type="warning"
              effect="plain"
            >
              {{ item }}
            </el-tag>
            <el-tag
              v-for="item in budgetPlan.fallbackSteps"
              :key="`fallback-${item}`"
              type="info"
              effect="plain"
            >
              {{ item }}
            </el-tag>
            <el-tag
              v-for="item in budgetPlan.recommendedNextActions"
              :key="`next-${item}`"
              type="success"
              effect="plain"
            >
              {{ item }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-tabs
        v-model="activeTab"
        class="preview-tabs"
        :data-testid="stableTestIds.aiContext.previewTabs"
      >
        <el-tab-pane name="databaseRules">
          <template #label>
            <span :data-testid="stableTestIds.aiContext.databaseRulesTab">DATABASE_RULES.md</span>
          </template>
          <pre class="preview-code">{{ databaseRules || '暂无预览' }}</pre>
        </el-tab-pane>
        <el-tab-pane name="fieldCatalog">
          <template #label>
            <span :data-testid="stableTestIds.aiContext.fieldCatalogTab">field-catalog.json</span>
          </template>
          <pre class="preview-code">{{ fieldCatalog || '暂无预览' }}</pre>
        </el-tab-pane>
        <el-tab-pane name="rulesYaml">
          <template #label>
            <span :data-testid="stableTestIds.aiContext.rulesYamlTab">rules.yaml</span>
          </template>
          <pre class="preview-code">{{ rulesYaml || '暂无预览' }}</pre>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Download, Refresh } from '@element-plus/icons-vue'
import {
  downloadAiContextPackage,
  planAiContextBudget,
  previewDatabaseRules,
  previewFieldCatalog,
  previewRulesYaml,
  type AiContextBudgetPlan,
  type AiContextRecommendedExportParams
} from '@/api/aicontext'
import { listStandardSnapshots } from '@/api/standardSnapshot'
import { useProjectStore } from '@/stores/project'
import type { StandardSnapshotInfo } from '@/types'
import {
  aiContextScopeFilename,
  normalizeAiContextScopeParams,
  type AiContextScope
} from '@/utils/aiContextScope'
import {
  budgetRiskLabel,
  budgetRiskTagType,
  buildBudgetPlanSummary,
  formatEstimatedTokens
} from '@/utils/aiContextBudgetPlan'
import {
  buildAiOutputPostCheckCommand,
  buildSnapshotRef
} from '@/utils/aiOutputPostCheckDisplay'
import {
  buildFieldStandardQueryFromAiContextScope
} from '@/utils/standardQuerySummary'
import { stableTestIds } from '@/utils/stableTestIds'

const projectStore = useProjectStore()
const activeTab = ref<'databaseRules' | 'fieldCatalog' | 'rulesYaml'>('databaseRules')
const databaseRules = ref('')
const fieldCatalog = ref('')
const rulesYaml = ref('')
const previewLoading = ref(false)
const downloadLoading = ref(false)
const demoLoading = ref(false)
const snapshotLoading = ref(false)
const budgetLoading = ref(false)
const snapshots = ref<StandardSnapshotInfo[]>([])
const budgetPlan = shallowRef<AiContextBudgetPlan | null>(null)
const scopeForm = reactive<{
  scope: AiContextScope
  query: string
  status: string
  limit: number | null
  snapshotId: number | null
}>({
  scope: 'all',
  query: '',
  status: '',
  limit: null,
  snapshotId: 0
})
const budgetForm = reactive<{
  tokenBudget: number | null
}>({
  tokenBudget: 8000
})

const scopeOptions: Array<{ label: string; value: AiContextScope }> = [
  { label: '全部', value: 'all' },
  { label: '字段', value: 'field' },
  { label: '数据域', value: 'domain' },
  { label: '标签', value: 'tag' },
  { label: '表', value: 'table' },
  { label: '业务对象', value: 'business-object' },
  { label: '表模板', value: 'table-template' },
  { label: '变更', value: 'changed' }
]

const semanticArtifacts = [
  '.dataspec/field-knowledge-cards.json',
  '.dataspec/field-semantics.json',
  '.dataspec/metrics.json'
]

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const currentScopeParams = computed(() => normalizeAiContextScopeParams(scopeForm))
const recommendedParamItems = computed(() => {
  const params = budgetPlan.value?.recommendedExportParams
  if (!params) {
    return []
  }
  return ([
    { key: 'scope', label: 'scope', value: params.scope },
    { key: 'query', label: 'query', value: params.query },
    { key: 'status', label: 'status', value: params.status },
    { key: 'limit', label: 'limit', value: params.limit },
    { key: 'profileId', label: 'profile', value: params.profileId },
    { key: 'taskType', label: 'task', value: params.taskType }
  ] as Array<{ key: string; label: string; value?: string | number }>).filter((item) =>
    item.value !== undefined && item.value !== null && `${item.value}`.trim() !== ''
  )
})
const hasRecommendedExportParams = computed(() => recommendedParamItems.value.length > 0)
const snapshotOptions = computed(() =>
  snapshots.value.filter((snapshot): snapshot is StandardSnapshotInfo & { snapshotId: number } =>
    typeof snapshot.snapshotId === 'number'
  )
)
const postCheckCommand = computed(() =>
  buildAiOutputPostCheckCommand({
    projectId: projectStore.currentProjectId,
    contentType: 'SQL',
    snapshotRef: buildSnapshotRef(
      projectStore.currentProjectId,
      scopeForm.snapshotId && scopeForm.snapshotId > 0 ? scopeForm.snapshotId : undefined
    )
  })
)
const currentStandardQuery = computed(() => {
  const projectId = projectStore.currentProjectId
  return projectId ? buildFieldStandardQueryFromAiContextScope(projectId, currentScopeParams.value) : null
})
const aiContextStandardQuerySummary = computed(() => {
  const query = currentStandardQuery.value
  if (!query) {
    return ''
  }
  const parts = [`target: ${query.target ?? 'FIELD'}`]
  if (query.text) {
    parts.push('text: 已设置')
  }
  if (query.filters?.length) {
    parts.push(`filters: ${query.filters.length}`)
  }
  if (query.limit) {
    parts.push(`limit: ${query.limit}`)
  }
  return parts.join('；')
})

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  await loadSnapshots()
  await loadPreviews()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    budgetPlan.value = null
    scopeForm.snapshotId = 0
    void loadSnapshots()
    void loadPreviews()
  }
)

watch(
  () => scopeForm.snapshotId,
  () => {
    budgetPlan.value = null
    void loadPreviews()
  }
)

watch(
  () => [scopeForm.scope, scopeForm.query, scopeForm.status, scopeForm.limit, budgetForm.tokenBudget],
  () => {
    budgetPlan.value = null
  }
)

async function loadPreviews() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    databaseRules.value = ''
    fieldCatalog.value = ''
    rulesYaml.value = ''
    budgetPlan.value = null
    return
  }
  previewLoading.value = true
  try {
    const scopeParams = currentScopeParams.value
    const [rules, fields, yaml] = await Promise.all([
      previewDatabaseRules(projectId, scopeParams),
      previewFieldCatalog(projectId, scopeParams),
      previewRulesYaml(projectId, scopeParams)
    ])
    databaseRules.value = rules
    fieldCatalog.value = fields
    rulesYaml.value = yaml
  } finally {
    previewLoading.value = false
  }
}

async function loadSnapshots() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    snapshots.value = []
    return
  }
  snapshotLoading.value = true
  try {
    snapshots.value = await listStandardSnapshots(projectId)
  } finally {
    snapshotLoading.value = false
  }
}

async function handleDownloadPackage() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  downloadLoading.value = true
  try {
    const scopeParams = currentScopeParams.value
    saveBlob(await downloadAiContextPackage(projectId, scopeParams), aiContextScopeFilename(scopeParams))
  } finally {
    downloadLoading.value = false
  }
}

async function handlePlanBudget() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  if (!budgetForm.tokenBudget || budgetForm.tokenBudget <= 0) {
    ElMessage.warning('请输入大于 0 的 token 预算')
    return
  }
  budgetLoading.value = true
  try {
    const budgetRequest = {
      ...currentScopeParams.value,
      tokenBudget: Math.floor(budgetForm.tokenBudget)
    }
    budgetPlan.value = await planAiContextBudget(projectId, budgetRequest)
  } finally {
    budgetLoading.value = false
  }
}

function handleApplyRecommendedParams() {
  const params = budgetPlan.value?.recommendedExportParams
  if (!params) {
    return
  }
  // Planner 推荐只作为显式用户动作应用，不能静默覆盖已有导出参数。
  scopeForm.scope = normalizeRecommendedScope(params.scope)
  scopeForm.query = params.query || ''
  scopeForm.status = params.status || ''
  scopeForm.limit = typeof params.limit === 'number' && Number.isFinite(params.limit) && params.limit > 0
    ? Math.floor(params.limit)
    : null
  ElMessage.success('已填充推荐导出参数')
  void loadPreviews()
}

async function handleCreateDemoProject() {
  demoLoading.value = true
  try {
    const result = await projectStore.createDemoProjectAndSelect()
    ElMessage.success(result.created ? '演示项目已创建' : '已切换到演示项目')
    await loadPreviews()
  } finally {
    demoLoading.value = false
  }
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function handleResetScope() {
  scopeForm.scope = 'all'
  scopeForm.query = ''
  scopeForm.status = ''
  scopeForm.limit = null
  scopeForm.snapshotId = 0
  budgetPlan.value = null
  void loadPreviews()
}

function snapshotOptionLabel(snapshot: StandardSnapshotInfo) {
  const version = snapshot.specVersion || 'unversioned'
  const name = snapshot.name ? ` ${snapshot.name}` : ''
  const hash = snapshot.specHash ? ` / ${snapshot.specHash.slice(0, 8)}` : ''
  return `${version}${name}${hash}`
}

function normalizeRecommendedScope(scope?: AiContextRecommendedExportParams['scope']): AiContextScope {
  return isAiContextScope(scope) ? scope : 'all'
}

function isAiContextScope(scope?: AiContextRecommendedExportParams['scope']): scope is AiContextScope {
  return scope === 'all' ||
    scope === 'field' ||
    scope === 'domain' ||
    scope === 'tag' ||
    scope === 'table' ||
    scope === 'business-object' ||
    scope === 'table-template' ||
    scope === 'changed'
}
</script>

<style scoped>
.ai-export-page {
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
  color: #1f2937;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preview-tabs {
  margin-top: 8px;
}

.scope-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f9fafb;
}

.scope-query {
  width: min(260px, 100%);
}

.scope-status {
  width: 150px;
}

.scope-limit {
  width: 132px;
}

.snapshot-select {
  width: 230px;
}

.post-check-guidance {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f8fafc;
}

.standard-query-guidance {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f9fafb;
}

.semantic-artifacts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #ffffff;
}

.semantic-artifact-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.semantic-artifact-note {
  color: #6b7280;
  font-size: 12px;
}

.post-check-guidance code,
.standard-query-guidance code {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #1f2937;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.budget-preview {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #ffffff;
}

.budget-controls,
.budget-actions,
.budget-metrics,
.budget-recommended,
.budget-notes {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.budget-controls {
  justify-content: space-between;
}

.budget-heading {
  min-width: min(100%, 360px);
}

.budget-title,
.budget-section-label {
  color: #374151;
  font-weight: 600;
}

.budget-summary-line {
  display: block;
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.budget-token-input {
  width: 160px;
}

.budget-result {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
}

.budget-metrics {
  align-items: stretch;
}

.budget-metric {
  min-width: 128px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f9fafb;
}

.budget-metric span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.budget-metric strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 14px;
}

.budget-recommended,
.budget-notes {
  margin-top: 12px;
}

.budget-artifacts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.budget-artifact-list {
  min-width: 0;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fcfcfd;
}

.budget-artifact-list.dropped {
  background: #fffdf7;
}

.budget-artifact-list ul {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
}

.budget-artifact-list li {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 6px 0;
  border-top: 1px solid #eef2f7;
  color: #374151;
  font-size: 13px;
}

.budget-artifact-list li span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.budget-artifact-list li strong {
  flex: 0 0 auto;
  color: #111827;
}

.budget-empty {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.preview-code {
  min-height: 520px;
  max-height: calc(100vh - 270px);
  overflow: auto;
  margin: 0;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #1f2933;
  color: #f8fafc;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
  }

  .scope-toolbar {
    align-items: stretch;
  }

  .scope-query,
  .scope-status,
  .scope-limit,
  .snapshot-select,
  .budget-token-input {
    width: 100%;
  }

  .post-check-guidance {
    align-items: flex-start;
    flex-direction: column;
  }

  .standard-query-guidance {
    align-items: flex-start;
    flex-direction: column;
  }

  .semantic-artifacts {
    align-items: flex-start;
    flex-direction: column;
  }

  .budget-controls,
  .budget-actions {
    align-items: stretch;
  }

  .budget-artifacts {
    grid-template-columns: 1fr;
  }
}
</style>
