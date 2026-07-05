<template>
  <div class="standard-reuse-pack-page">
    <div class="page-header">
      <div>
        <h2>标准复用包</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" :loading="createLoading" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          创建复用包
        </el-button>
      </div>
    </div>

    <ProjectRequired
      v-if="!hasProject"
      :has-project="hasProject"
      title="请先创建并选择项目"
      @action="$router.push('/projects')"
    />

    <template v-else>
      <section class="tool-section create-section">
        <div class="section-header">
          <h3>创建复用包</h3>
          <el-tag effect="plain">当前项目</el-tag>
        </div>
        <el-form class="create-form" label-position="top">
          <el-row :gutter="16">
            <el-col :span="6">
              <el-form-item label="Pack Key">
                <el-input v-model="form.packKey" placeholder="shared_core" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="名称">
                <el-input v-model="form.packName" placeholder="通用交易标准" />
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item label="版本">
                <el-input v-model="form.basePackVersion" placeholder="2026.07" />
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item label="说明">
                <el-input v-model="form.description" placeholder="用户、订单、支付共享字段" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </section>

      <section class="tool-section">
        <div class="section-header">
          <h3>复用包列表</h3>
          <el-tag effect="plain">{{ packs.length }} 个</el-tag>
        </div>
        <el-table
          v-loading="packsLoading"
          :data="packs"
          stripe
          empty-text="暂无复用包"
          @current-change="handleSelectPack"
        >
          <el-table-column prop="packName" label="名称" min-width="170" show-overflow-tooltip />
          <el-table-column prop="packKey" label="Key" min-width="140" />
          <el-table-column prop="basePackVersion" label="版本" width="110" />
          <el-table-column label="资产" min-width="230">
            <template #default="{ row }">{{ formatReusePackCountText(row.assetCounts) }}</template>
          </el-table-column>
          <el-table-column label="Hash" width="120">
            <template #default="{ row }">{{ shortReusePackHash(row.packageHash) }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="178" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleSelectPack(row)">选择</el-button>
              <el-button text type="primary" @click="handlePreview(row)">
                <el-icon><View /></el-icon>
                预览应用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="tool-section">
        <div class="section-header">
          <h3>应用与漂移</h3>
          <el-tag :type="selectedPack ? 'success' : 'info'" effect="plain">
            {{ selectedPack?.packKey || '未选择复用包' }}
          </el-tag>
        </div>

        <div class="apply-toolbar">
          <el-select v-model="targetProjectId" placeholder="选择目标项目" class="target-select">
            <el-option
              v-for="project in projectStore.projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
          <el-button :disabled="!selectedPack || !targetProjectId" :loading="previewLoading" @click="handlePreview()">
            <el-icon><View /></el-icon>
            预览应用
          </el-button>
          <el-button
            type="primary"
            :disabled="!canApply"
            :loading="applyLoading"
            @click="handleApply"
          >
            <el-icon><Check /></el-icon>
            确认应用
          </el-button>
          <el-button :disabled="!selectedPack || !targetProjectId" :loading="driftLoading" @click="handleDrift">
            漂移报告
          </el-button>
        </div>

        <div v-if="plan" class="summary-grid">
          <div v-for="item in countItems" :key="item.key" class="summary-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div v-if="safetySummary" class="safety-summary">
          <div class="safety-summary__header">
            <strong>{{ safetySummary.title }}</strong>
            <el-tag v-if="safetySummary.requiresDryRun" type="warning" effect="plain" size="small">requiresDryRun</el-tag>
          </div>
          <p>{{ safetySummary.riskText }}</p>
          <p>{{ safetySummary.idempotencyText }}</p>
          <div class="safety-summary__counts">
            <span v-for="item in safetySummary.counts" :key="item.key">{{ item.label }} {{ item.value }}</span>
          </div>
          <ul v-if="safetySummary.nextActions.length" class="safety-summary__actions">
            <li v-for="action in safetySummary.nextActions" :key="action">{{ action }}</li>
          </ul>
        </div>

        <el-alert
          v-for="warning in plan?.warnings || []"
          :key="warning"
          class="warning-alert"
          type="warning"
          show-icon
          :closable="false"
          :title="warning"
        />

        <el-table
          v-if="plan"
          :data="plan.items || []"
          stripe
          class="plan-table"
          empty-text="暂无应用计划"
        >
          <el-table-column prop="assetType" label="资产类型" width="130" />
          <el-table-column prop="key" label="自然键" min-width="180" show-overflow-tooltip />
          <el-table-column label="动作" width="120">
            <template #default="{ row }">
              <el-tag :type="reusePackActionTagType(row.action)" size="small" effect="plain">
                {{ actionLabel(row.action) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="260" show-overflow-tooltip />
        </el-table>

        <el-table
          v-if="driftReport"
          :data="driftReport.items || []"
          stripe
          class="plan-table"
          empty-text="暂无漂移明细"
        >
          <el-table-column prop="assetType" label="漂移资产" width="130" />
          <el-table-column prop="key" label="自然键" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="reusePackActionTagType(row.action)" size="small" effect="plain">
                {{ actionLabel(row.action) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="说明" min-width="260" show-overflow-tooltip />
        </el-table>
      </section>

      <section class="tool-section">
        <div class="section-header">
          <h3>最近应用记录</h3>
          <el-tag effect="plain">{{ applications.length }} 条</el-tag>
        </div>
        <el-table
          v-loading="applicationsLoading"
          :data="applications"
          stripe
          empty-text="暂无复用包应用记录"
        >
          <el-table-column prop="appliedAt" label="应用时间" width="178" />
          <el-table-column prop="packName" label="复用包" min-width="170" show-overflow-tooltip />
          <el-table-column prop="basePackVersion" label="版本" width="110" />
          <el-table-column label="创建" min-width="160">
            <template #default="{ row }">{{ formatReusePackCountText(row.createdCounts) }}</template>
          </el-table-column>
          <el-table-column label="漂移" width="170">
            <template #default="{ row }">
              匹配 {{ row.driftCounts?.matched ?? 0 }} / 漂移 {{ row.driftCounts?.drifted ?? 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="sourceProjectName" label="来源项目" min-width="140" show-overflow-tooltip />
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Plus, Refresh, View } from '@element-plus/icons-vue'
import {
  applyStandardReusePack,
  createStandardReusePack,
  getStandardReusePackDrift,
  listStandardReusePackApplications,
  listStandardReusePacks,
  previewStandardReusePackApply
} from '@/api/standardReusePack'
import ProjectRequired from '@/components/ProjectRequired.vue'
import { useProjectStore } from '@/stores/project'
import {
  buildAiWriteSafetySummary,
  buildStandardReusePackApplyPayload,
  buildStandardReusePackCreatePayload,
  formatReusePackCountText,
  hasBlockingReusePackItems,
  reusePackActionTagType,
  shortReusePackHash,
  summarizeReusePackCounts
} from '@/utils/standardReusePackDisplay'
import type {
  StandardPackSource,
  StandardReusePackApplicationInfo,
  StandardReusePackDriftReport,
  StandardReusePackInfo,
  StandardReusePackPlan
} from '@/types'

const projectStore = useProjectStore()
const packs = ref<StandardReusePackInfo[]>([])
const applications = ref<StandardReusePackApplicationInfo[]>([])
const selectedPack = ref<StandardReusePackInfo | null>(null)
const targetProjectId = ref<number | null>(null)
const plan = ref<StandardReusePackPlan | null>(null)
const driftReport = ref<StandardReusePackDriftReport | null>(null)
const loading = ref(false)
const packsLoading = ref(false)
const applicationsLoading = ref(false)
const createLoading = ref(false)
const previewLoading = ref(false)
const applyLoading = ref(false)
const driftLoading = ref(false)

const form = reactive({
  packKey: 'shared_core',
  packName: '通用标准包',
  basePackVersion: new Date().toISOString().slice(0, 7),
  description: ''
})

const standardPackSources = ref<StandardPackSource[]>([])
const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const canApply = computed(() => Boolean(plan.value?.canApply && selectedPack.value && targetProjectId.value && !hasBlockingReusePackItems(plan.value)))
const countItems = computed(() => summarizeReusePackCounts(plan.value))
const safetySummary = computed(() => plan.value
  ? buildAiWriteSafetySummary({
    safety: {
      readOnly: false,
      writesProject: true,
      requiresDryRun: false,
      supportsUndo: true,
      requiresIdempotencyKey: false,
      sensitiveInputs: [],
      nextActions: ['先运行预览应用并确认阻塞项为 0', '确认应用后导出 evidence package 或查看应用摘要']
    },
    counts: plan.value.counts
  })
  : null)

onMounted(async () => {
  if (projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  targetProjectId.value = projectStore.currentProjectId
  await loadData()
})

watch(
  () => projectStore.currentProjectId,
  async (projectId) => {
    targetProjectId.value = projectId
    selectedPack.value = null
    plan.value = null
    driftReport.value = null
    await loadData()
  }
)

async function loadData() {
  if (!projectStore.currentProjectId) {
    packs.value = []
    applications.value = []
    return
  }
  loading.value = true
  try {
    await Promise.all([loadPacks(), loadApplications()])
  } finally {
    loading.value = false
  }
}

async function loadPacks() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    packs.value = []
    return
  }
  packsLoading.value = true
  try {
    packs.value = await listStandardReusePacks(projectId)
    if (!selectedPack.value && packs.value.length > 0) {
      selectedPack.value = packs.value[0]
    }
  } finally {
    packsLoading.value = false
  }
}

async function loadApplications() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    applications.value = []
    return
  }
  applicationsLoading.value = true
  try {
    applications.value = await listStandardReusePackApplications(projectId)
  } finally {
    applicationsLoading.value = false
  }
}

async function handleCreate() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  const payload = buildStandardReusePackCreatePayload(
    projectId,
    form.packKey,
    form.packName,
    form.basePackVersion,
    form.description
  )
  if (!payload.packKey || !payload.packName || !payload.basePackVersion) {
    ElMessage.warning('请填写 Pack Key、名称和版本')
    return
  }
  createLoading.value = true
  try {
    const detail = await createStandardReusePack(payload)
    ElMessage.success('标准复用包已创建')
    await loadPacks()
    selectedPack.value = detail.info ?? null
  } finally {
    createLoading.value = false
  }
}

function handleSelectPack(row?: StandardReusePackInfo | null) {
  if (!row) {
    return
  }
  selectedPack.value = row
  plan.value = null
  driftReport.value = null
  standardPackSources.value = [{ packKey: row.packKey, basePackVersion: row.basePackVersion }]
}

async function handlePreview(row?: StandardReusePackInfo) {
  if (row) {
    handleSelectPack(row)
  }
  if (!selectedPack.value?.packId || !targetProjectId.value) {
    ElMessage.warning('请选择复用包和目标项目')
    return
  }
  previewLoading.value = true
  try {
    plan.value = await previewStandardReusePackApply(
      buildStandardReusePackApplyPayload(selectedPack.value.packId, targetProjectId.value)
    )
    driftReport.value = plan.value.driftReport ?? null
  } finally {
    previewLoading.value = false
  }
}

async function handleApply() {
  if (!selectedPack.value?.packId || !targetProjectId.value || !canApply.value) {
    return
  }
  applyLoading.value = true
  try {
    await applyStandardReusePack(buildStandardReusePackApplyPayload(selectedPack.value.packId, targetProjectId.value))
    ElMessage.success('标准复用包已应用')
    await Promise.all([handlePreview(), loadApplications()])
  } finally {
    applyLoading.value = false
  }
}

async function handleDrift() {
  if (!selectedPack.value?.packId || !targetProjectId.value) {
    return
  }
  driftLoading.value = true
  try {
    driftReport.value = await getStandardReusePackDrift(selectedPack.value.packId, targetProjectId.value)
  } finally {
    driftLoading.value = false
  }
}

function actionLabel(action?: string) {
  const labels: Record<string, string> = {
    CREATE: '创建',
    SKIP: '跳过',
    MATCHED: '匹配',
    MISSING: '缺失',
    DRIFTED: '漂移',
    OVERRIDDEN: '覆盖',
    BLOCKED: '阻塞'
  }
  return action ? labels[action] || action : '-'
}
</script>

<style scoped>
.standard-reuse-pack-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.section-header,
.header-actions,
.apply-toolbar {
  display: flex;
  align-items: center;
}

.page-header,
.section-header {
  justify-content: space-between;
}

.page-header h2,
.section-header h3 {
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
}

.header-actions,
.apply-toolbar {
  gap: 10px;
}

.tool-section {
  padding: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.create-form {
  margin-top: 12px;
}

.target-select {
  width: 240px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(96px, 1fr));
  gap: 10px;
  margin: 16px 0;
}

.summary-item {
  min-height: 64px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f9fafb;
}

.summary-item span {
  display: block;
  color: #6b7280;
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  line-height: 1;
}

.warning-alert,
.plan-table {
  margin-top: 12px;
}

.safety-summary {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #f3d19e;
  border-radius: 8px;
  background: #fdf6ec;
  color: #7c4a03;
}

.safety-summary__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.safety-summary p {
  margin: 8px 0 0;
}

.safety-summary__counts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  font-size: 13px;
}

.safety-summary__actions {
  margin: 8px 0 0;
  padding-left: 18px;
}
</style>
