<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div>
        <h2>工作台</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <el-button :disabled="!hasProject" :loading="loading" @click="loadSummary">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div v-loading="loading" class="dashboard-content">
        <section class="metric-grid">
          <div v-for="metric in metrics" :key="metric.key" class="metric-item">
            <div class="metric-label">{{ metric.label }}</div>
            <div class="metric-value">{{ metric.value }}</div>
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
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getDashboardSummary } from '@/api/dashboard'
import { useProjectStore } from '@/stores/project'
import type { DashboardSummary, IssueTrendPoint } from '@/types'

const projectStore = useProjectStore()
const loading = ref(false)
const summary = ref<DashboardSummary | null>(null)

const hasProject = computed(() => projectStore.currentProjectId !== null)
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

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  await loadSummary()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    loadSummary()
  }
)

async function loadSummary() {
  if (!projectStore.currentProjectId) {
    summary.value = null
    return
  }
  loading.value = true
  try {
    summary.value = await getDashboardSummary(projectStore.currentProjectId)
  } finally {
    loading.value = false
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
}

@media (max-width: 720px) {
  .page-header,
  .section-header {
    flex-direction: column;
  }

  .metric-grid,
  .trend-row {
    grid-template-columns: 1fr;
  }

  .trend-count {
    text-align: left;
  }
}
</style>
