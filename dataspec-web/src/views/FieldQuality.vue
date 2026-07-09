<template>
  <div class="field-quality-page">
    <div class="page-header">
      <div>
        <h2>字段质量</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :disabled="!hasProject" :loading="maintenanceWorkflowLoading" @click="generateQualityMaintenanceWorkflow">
          <el-icon><DataAnalysis /></el-icon>
          生成维护 workflow
        </el-button>
        <el-button :disabled="!hasProject" :loading="loading" @click="loadReport">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <el-row :gutter="12" class="summary-row">
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">字段数</span>
            <strong>{{ summary.totalFieldCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">平均分</span>
            <strong>{{ summary.averageScore ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">低质量</span>
            <strong>{{ summary.lowQualityCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">ERROR</span>
            <strong>{{ summary.errorIssueCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">WARNING</span>
            <strong>{{ summary.warningIssueCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">建议</span>
            <strong>{{ summary.suggestionIssueCount ?? 0 }}</strong>
          </div>
        </el-col>
      </el-row>

      <div class="quality-toolbar">
        <el-select v-model="levelFilter" class="filter-select">
          <el-option label="全部等级" value="ALL" />
          <el-option label="良好" value="GOOD" />
          <el-option label="待完善" value="WARNING" />
          <el-option label="低质量" value="POOR" />
        </el-select>
        <el-select v-model="issueFilter" class="filter-select" filterable>
          <el-option label="全部问题" value="ALL" />
          <el-option v-for="code in issueCodes" :key="code" :label="code" :value="code" />
        </el-select>
        <span class="toolbar-count">当前匹配 {{ filteredItems.length }} / {{ items.length }}</span>
      </div>

      <el-alert
        v-if="maintenanceWorkflowError"
        type="warning"
        :closable="false"
        show-icon
        :title="maintenanceWorkflowError"
      />
      <StandardMaintenanceWorkflowPlanPanel
        v-if="maintenanceWorkflowPlan"
        :workflow-plan="maintenanceWorkflowPlan"
      />

      <el-table
        v-loading="loading"
        :data="filteredItems"
        stripe
        class="quality-table"
        empty-text="暂无字段质量数据"
      >
        <el-table-column label="评分" width="150" fixed="left">
          <template #default="{ row }">
            <div class="score-cell">
              <span class="score-value">{{ row.score ?? 0 }}</span>
              <el-progress
                :percentage="row.score ?? 0"
                :show-text="false"
                :stroke-width="8"
                :color="scoreColor(row.score)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="110">
          <template #default="{ row }">
            <el-tag :type="qualityLevelTagType(row.level)" size="small">
              {{ qualityLevelLabel(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="字段名" min-width="160" />
        <el-table-column prop="displayName" label="显示名" min-width="130" />
        <el-table-column prop="dataType" label="类型" min-width="130" show-overflow-tooltip />
        <el-table-column label="问题" min-width="320">
          <template #default="{ row }">
            <div v-if="row.issues?.length" class="issue-list">
              <el-tag
                v-for="issue in row.issues"
                :key="`${row.fieldId}-${issue.code}`"
                :type="qualitySeverityTagType(issue.severity)"
                size="small"
              >
                {{ issue.code }}
              </el-tag>
            </div>
            <span v-else class="muted-text">无</span>
          </template>
        </el-table-column>
        <el-table-column label="建议" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.suggestions?.join('；') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="goToField(row)">编辑字段</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, Refresh } from '@element-plus/icons-vue'
import { getFieldQualityReport } from '@/api/field'
import { generateStandardMaintenanceWorkflowPlan } from '@/api/standardMaintenanceWorkflow'
import StandardMaintenanceWorkflowPlanPanel from '@/components/StandardMaintenanceWorkflowPlanPanel.vue'
import { useProjectStore } from '@/stores/project'
import {
  fieldQualityEditQuery,
  filterQualityItems,
  issueOptions,
  qualityLevelLabel,
  qualityLevelTagType,
  qualitySeverityTagType
} from '@/utils/fieldQualityDisplay'
import type { FieldQualityItem, FieldQualityLevel, FieldQualityReport, StandardMaintenanceWorkflowPlan } from '@/types'

const projectStore = useProjectStore()
const router = useRouter()

const loading = ref(false)
const report = ref<FieldQualityReport>({})
const levelFilter = ref<FieldQualityLevel | 'ALL'>('ALL')
const issueFilter = ref('ALL')
const maintenanceWorkflowLoading = ref(false)
const maintenanceWorkflowError = ref('')
const maintenanceWorkflowPlan = ref<StandardMaintenanceWorkflowPlan | null>(null)

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const summary = computed(() => report.value.summary ?? {})
const items = computed(() => report.value.fields ?? [])
const issueCodes = computed(() => issueOptions(items.value))
const filteredItems = computed(() =>
  filterQualityItems(items.value, levelFilter.value, issueFilter.value)
)
const maintenanceIssueCodes = computed(() =>
  issueFilter.value === 'ALL' ? issueCodes.value : [issueFilter.value]
)

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    levelFilter.value = 'ALL'
    issueFilter.value = 'ALL'
    maintenanceWorkflowPlan.value = null
    maintenanceWorkflowError.value = ''
    void loadReport()
  },
  { immediate: true }
)

async function loadReport() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    report.value = {}
    return
  }
  loading.value = true
  try {
    report.value = await getFieldQualityReport(projectId)
  } finally {
    loading.value = false
  }
}

function scoreColor(score?: number) {
  const value = score ?? 0
  if (value >= 85) {
    return '#67c23a'
  }
  if (value >= 65) {
    return '#e6a23c'
  }
  return '#f56c6c'
}

function goToField(item: FieldQualityItem) {
  void router.push({
    path: '/fields',
    query: fieldQualityEditQuery(item)
  })
}

async function generateQualityMaintenanceWorkflow() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    maintenanceWorkflowPlan.value = null
    maintenanceWorkflowError.value = '请先选择项目'
    return
  }
  maintenanceWorkflowLoading.value = true
  maintenanceWorkflowError.value = ''
  try {
    maintenanceWorkflowPlan.value = await generateStandardMaintenanceWorkflowPlan({
      projectId,
      sourceType: 'FIELD_QUALITY',
      sourceIds: filteredItems.value
        .map((item) => item.fieldId)
        .filter((fieldId): fieldId is number => typeof fieldId === 'number')
        .slice(0, 50),
      issueCodes: maintenanceIssueCodes.value,
      itemCount: filteredItems.value.length,
      sourceRoute: `/field-quality?projectId=${projectId}`
    })
  } catch (error) {
    maintenanceWorkflowPlan.value = null
    maintenanceWorkflowError.value = error instanceof Error ? error.message : '维护 workflow 生成失败'
  } finally {
    maintenanceWorkflowLoading.value = false
  }
}
</script>

<style scoped>
.field-quality-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.page-header h2 {
  margin: 0 0 4px;
}

.page-subtitle {
  margin: 0;
  color: #909399;
}

.summary-row {
  margin-bottom: 16px;
}

.metric-card {
  min-height: 72px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.metric-label {
  color: #909399;
  font-size: 12px;
}

.metric-card strong {
  font-size: 24px;
  line-height: 1;
}

.quality-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-select {
  width: 180px;
}

.toolbar-count,
.muted-text {
  color: #909399;
  font-size: 13px;
}

.quality-table {
  width: 100%;
}

.score-cell {
  display: grid;
  grid-template-columns: 38px 1fr;
  align-items: center;
  gap: 8px;
}

.score-value {
  font-weight: 600;
}

.issue-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
</style>
