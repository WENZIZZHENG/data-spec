<template>
  <div class="field-conflicts-page">
    <div class="page-header">
      <div>
        <h2>字段冲突</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <el-button :disabled="!hasProject" :loading="loading" @click="loadReport">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
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
            <span class="metric-label">冲突组</span>
            <strong>{{ summary.conflictGroupCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">影响字段</span>
            <strong>{{ summary.affectedFieldCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">ERROR</span>
            <strong>{{ summary.errorCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">别名冲突</span>
            <strong>{{ summary.aliasConflictCount ?? 0 }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="metric-card">
            <span class="metric-label">属性不一致</span>
            <strong>{{ summary.attributeMismatchCount ?? 0 }}</strong>
          </div>
        </el-col>
      </el-row>

      <div class="conflict-toolbar">
        <el-select v-model="severityFilter" class="filter-select">
          <el-option label="全部级别" value="ALL" />
          <el-option label="ERROR" value="ERROR" />
          <el-option label="WARNING" value="WARNING" />
          <el-option label="INFO" value="INFO" />
        </el-select>
        <el-select v-model="typeFilter" class="filter-select">
          <el-option label="全部类型" value="ALL" />
          <el-option label="字段名重复" value="NAME_DUPLICATE" />
          <el-option label="别名冲突" value="ALIAS_CONFLICT" />
          <el-option label="显示名重复" value="DISPLAY_NAME_DUPLICATE" />
          <el-option label="语义疑似重复" value="SEMANTIC_DUPLICATE" />
        </el-select>
        <span class="toolbar-count">当前匹配 {{ filteredGroups.length }} / {{ groups.length }}</span>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredGroups"
        stripe
        class="conflict-table"
        empty-text="暂无字段冲突"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="group-detail">
              <div class="detail-section">
                <strong>证据</strong>
                <div class="evidence-list">
                  <el-tag v-for="item in row.evidence" :key="item" size="small" effect="plain">
                    {{ item }}
                  </el-tag>
                </div>
              </div>
              <div class="detail-section">
                <strong>涉及字段</strong>
                <el-table :data="row.fields ?? []" size="small" border>
                  <el-table-column prop="name" label="字段名" min-width="140" />
                  <el-table-column prop="displayName" label="显示名" min-width="120" />
                  <el-table-column prop="dataType" label="类型" min-width="130" show-overflow-tooltip />
                  <el-table-column label="敏感" width="80">
                    <template #default="{ row: field }">
                      <el-tag :type="field.sensitive ? 'danger' : 'info'" size="small">
                        {{ field.sensitive ? '是' : '否' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" width="110" />
                  <el-table-column label="别名" min-width="180" show-overflow-tooltip>
                    <template #default="{ row: field }">
                      {{ field.aliases?.join(', ') || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="110">
                    <template #default="{ row: field }">
                      <el-button text type="primary" @click="goToField(field)">编辑</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="110">
          <template #default="{ row }">
            <el-tag :type="conflictSeverityTagType(row.severity)" size="small">
              {{ row.severity ?? 'INFO' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="150">
          <template #default="{ row }">
            {{ conflictTypeLabel(row.conflictType) }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="冲突" min-width="220" show-overflow-tooltip />
        <el-table-column label="字段" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ conflictFieldSummary(row.fields) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="suggestedAction" label="建议" min-width="300" show-overflow-tooltip />
      </el-table>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getFieldConflictReport } from '@/api/field'
import { useProjectStore } from '@/stores/project'
import {
  conflictFieldEditQuery,
  conflictFieldSummary,
  conflictSeverityTagType,
  conflictTypeLabel,
  filterConflictGroups
} from '@/utils/fieldConflictDisplay'
import type {
  FieldConflictField,
  FieldConflictReport,
  FieldConflictSeverity,
  FieldConflictType
} from '@/types'

const projectStore = useProjectStore()
const router = useRouter()

const loading = ref(false)
const report = ref<FieldConflictReport>({})
const severityFilter = ref<FieldConflictSeverity | 'ALL'>('ALL')
const typeFilter = ref<FieldConflictType | 'ALL'>('ALL')

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const summary = computed(() => report.value.summary ?? {})
const groups = computed(() => report.value.groups ?? [])
const filteredGroups = computed(() => filterConflictGroups(groups.value, severityFilter.value, typeFilter.value))

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    severityFilter.value = 'ALL'
    typeFilter.value = 'ALL'
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
    report.value = await getFieldConflictReport(projectId)
  } finally {
    loading.value = false
  }
}

function goToField(field: FieldConflictField) {
  void router.push({
    path: '/fields',
    query: conflictFieldEditQuery(field)
  })
}
</script>

<style scoped>
.field-conflicts-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
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

.conflict-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-select {
  width: 180px;
}

.toolbar-count {
  color: #909399;
  font-size: 13px;
}

.conflict-table {
  width: 100%;
}

.group-detail {
  display: grid;
  gap: 14px;
  padding: 12px 32px 18px;
  background: #fafafa;
}

.detail-section {
  display: grid;
  gap: 8px;
}

.evidence-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
</style>
