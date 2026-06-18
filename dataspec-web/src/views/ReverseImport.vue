<template>
  <div class="reverse-page">
    <div class="page-header">
      <div>
        <h2>反向导入</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <el-button type="primary" :disabled="!canPreview" :loading="loading" @click="handlePreview">
        <el-icon><View /></el-icon>
        生成预览
      </el-button>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <section class="input-section">
        <div class="input-toolbar">
          <el-upload accept=".sql" :auto-upload="false" :show-file-list="false" :on-change="handleFileChange">
            <el-button>
              <el-icon><Upload /></el-icon>
              读取 SQL 文件
            </el-button>
          </el-upload>
          <el-button :disabled="!sqlText" @click="sqlText = ''">清空</el-button>
        </div>
        <el-input
          v-model="sqlText"
          type="textarea"
          :rows="12"
          spellcheck="false"
          placeholder="CREATE TABLE ..."
        />
      </section>

      <section v-if="preview" class="result-section">
        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.key" class="summary-item">
            <div class="summary-label">{{ item.label }}</div>
            <div class="summary-value">{{ item.value }}</div>
          </div>
        </div>

        <el-tabs class="result-tabs">
          <el-tab-pane label="字段候选">
            <el-table :data="preview.fieldCandidates ?? []" stripe empty-text="暂无字段候选">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="dataType" label="类型" min-width="130" />
              <el-table-column label="空值" width="90">
                <template #default="{ row }">{{ row.nullable ? '可空' : '非空' }}</template>
              </el-table-column>
              <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="缺注释">
            <el-table :data="preview.missingComments ?? []" stripe empty-text="暂无缺注释项">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column label="对象" width="100">
                <template #default="{ row }">{{ row.targetType === 'table' ? '表' : '字段' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="非标准字段">
            <el-table :data="preview.nonStandardFields ?? []" stripe empty-text="暂无非标准字段">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="recommendedName" label="建议名" min-width="140" />
              <el-table-column prop="reason" label="原因" min-width="220" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="解析表">
            <el-table :data="tableRows" stripe empty-text="暂无解析结果">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="dataType" label="类型" min-width="130" />
              <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { Upload, View } from '@element-plus/icons-vue'
import { previewReverseImport } from '@/api/reverseImport'
import { useProjectStore } from '@/stores/project'
import type { ReverseImportPreview } from '@/types'

const projectStore = useProjectStore()
const sqlText = ref('')
const preview = ref<ReverseImportPreview | null>(null)
const loading = ref(false)

const hasProject = computed(() => projectStore.currentProjectId !== null)
const canPreview = computed(() => hasProject.value && sqlText.value.trim().length > 0)
const summaryItems = computed(() => [
  { key: 'tables', label: '表', value: preview.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: preview.value?.summary?.columnCount ?? 0 },
  { key: 'candidates', label: '字段候选', value: preview.value?.summary?.candidateCount ?? 0 },
  { key: 'comments', label: '缺注释', value: preview.value?.summary?.missingCommentCount ?? 0 },
  { key: 'nonStandard', label: '非标准字段', value: preview.value?.summary?.nonStandardFieldCount ?? 0 }
])
const tableRows = computed(() =>
  (preview.value?.tables ?? []).flatMap((table) =>
    (table.columns ?? []).map((column) => ({
      tableName: table.name,
      columnName: column.name,
      dataType: column.dataType,
      comment: column.comment
    }))
  )
)

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    preview.value = null
  }
)

async function handlePreview() {
  if (!projectStore.currentProjectId || !sqlText.value.trim()) {
    return
  }
  loading.value = true
  try {
    preview.value = await previewReverseImport(projectStore.currentProjectId, sqlText.value)
  } finally {
    loading.value = false
  }
}

function handleFileChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) {
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    sqlText.value = String(reader.result ?? '')
    preview.value = null
    ElMessage.success('SQL 已读取')
  }
  reader.readAsText(file, 'utf-8')
}
</script>

<style scoped>
.reverse-page {
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

.input-section,
.result-section {
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.input-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.summary-label {
  color: #6b7280;
  font-size: 13px;
}

.summary-value {
  margin-top: 6px;
  color: #111827;
  font-size: 26px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
