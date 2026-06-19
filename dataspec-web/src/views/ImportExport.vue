<template>
  <div class="import-export-page">
    <div class="page-header">
      <div>
        <h2>导入导出</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="templateLoading" @click="handleDownloadTemplate">
          <el-icon><Download /></el-icon>
          下载模板
        </el-button>
        <el-button type="primary" :disabled="!hasProject" :loading="exportLoading" @click="handleExportExcel">
          <el-icon><Download /></el-icon>
          导出项目 Excel
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <section class="tool-section">
        <div class="section-header">
          <h3>Excel 导入</h3>
          <el-tag v-if="selectedFile" type="info">{{ selectedFile.name }}</el-tag>
        </div>

        <div class="upload-row">
          <el-upload
            class="upload-box"
            drag
            accept=".xlsx"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="upload-icon"><Upload /></el-icon>
            <div class="upload-title">选择 Excel</div>
          </el-upload>

          <div class="import-actions">
            <el-button :disabled="!selectedFile" :loading="previewLoading" @click="handlePreview">
              <el-icon><View /></el-icon>
              预览导入
            </el-button>
            <el-button
              type="primary"
              :disabled="!canImport"
              :loading="importLoading"
              @click="handleImport"
            >
              <el-icon><Check /></el-icon>
              确认导入
            </el-button>
          </div>
        </div>
      </section>

      <section v-if="preview" class="tool-section">
        <div class="section-header">
          <h3>导入预览</h3>
          <el-tag :type="preview.valid ? 'success' : 'danger'">
            {{ preview.valid ? '可导入' : '需修正' }}
          </el-tag>
        </div>

        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.key" class="summary-item">
            <div class="summary-label">{{ item.label }}</div>
            <div class="summary-main">{{ item.total }}</div>
            <div class="summary-sub">
              新增 {{ item.createCount }} / 更新 {{ item.updateCount }} / 冲突 {{ item.conflictCount }}
            </div>
          </div>
        </div>

        <el-table
          v-if="preview.items?.length"
          :data="preview.items"
          stripe
          class="dry-run-table"
          empty-text="暂无 dry-run 明细"
        >
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table
                v-if="row.diffs?.length"
                :data="row.diffs"
                size="small"
                class="diff-table"
                empty-text="暂无字段差异"
              >
                <el-table-column prop="field" label="字段" width="160" />
                <el-table-column prop="beforeValue" label="当前值" min-width="180" show-overflow-tooltip />
                <el-table-column prop="afterValue" label="导入值" min-width="180" show-overflow-tooltip />
              </el-table>
              <el-empty v-else description="无字段变化" :image-size="48" />
            </template>
          </el-table-column>
          <el-table-column prop="sheet" label="Sheet" width="130" />
          <el-table-column prop="rowNumber" label="行号" width="90" />
          <el-table-column prop="key" label="业务键" min-width="160" show-overflow-tooltip />
          <el-table-column label="动作" width="110">
            <template #default="{ row }">
              <el-tag :type="actionTagType(row.action)" size="small">
                {{ actionLabel(row.action) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        </el-table>

        <el-table
          v-if="preview.errors?.length"
          :data="preview.errors"
          stripe
          class="error-table"
          empty-text="暂无错误"
        >
          <el-table-column prop="sheet" label="Sheet" width="130" />
          <el-table-column prop="rowNumber" label="行号" width="90" />
          <el-table-column prop="field" label="字段" width="140" />
          <el-table-column prop="message" label="错误" min-width="240" show-overflow-tooltip />
        </el-table>
      </section>

      <section v-if="importResult" class="tool-section">
        <div class="section-header">
          <h3>导入结果</h3>
          <el-tag :type="importResult.success === false ? 'danger' : 'success'">
            {{ importResult.success === false ? '未写入' : '已完成' }}
          </el-tag>
        </div>
        <div class="result-grid">
          <div>
            <span class="result-number">{{ importResult.importedFields ?? 0 }}</span>
            <span>字段</span>
          </div>
          <div>
            <span class="result-number">{{ importResult.importedEnumDicts ?? 0 }}</span>
            <span>代码集</span>
          </div>
          <div>
            <span class="result-number">{{ importResult.importedEnumValues ?? 0 }}</span>
            <span>枚举值</span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { Check, Download, Upload, View } from '@element-plus/icons-vue'
import {
  downloadExcelTemplate,
  exportExcel,
  importExcel,
  previewExcelImport
} from '@/api/importExport'
import { useProjectStore } from '@/stores/project'
import type { ExcelImportPreview, ExcelImportResult, ExcelSheetSummary } from '@/types'

const projectStore = useProjectStore()
const selectedFile = ref<File | null>(null)
const preview = ref<ExcelImportPreview | null>(null)
const importResult = ref<ExcelImportResult | null>(null)
const templateLoading = ref(false)
const exportLoading = ref(false)
const previewLoading = ref(false)
const importLoading = ref(false)

const hasProject = computed(() => projectStore.currentProjectId !== null)
const canImport = computed(() => Boolean(selectedFile.value && preview.value?.valid))
const summaryItems = computed(() => [
  summaryItem('fields', '标准字段', preview.value?.fields),
  summaryItem('enumDicts', '代码集', preview.value?.enumDicts),
  summaryItem('enumValues', '枚举值', preview.value?.enumValues)
])

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    preview.value = null
    importResult.value = null
  }
)

async function handleDownloadTemplate() {
  templateLoading.value = true
  try {
    const blob = await downloadExcelTemplate()
    saveBlob(blob, 'dataspec-import-template.xlsx')
  } finally {
    templateLoading.value = false
  }
}

async function handleExportExcel() {
  if (!projectStore.currentProjectId) {
    return
  }
  exportLoading.value = true
  try {
    const blob = await exportExcel(projectStore.currentProjectId)
    saveBlob(blob, `dataspec-project-${projectStore.currentProjectId}.xlsx`)
  } finally {
    exportLoading.value = false
  }
}

function handleFileChange(uploadFile: UploadFile) {
  selectedFile.value = uploadFile.raw ?? null
  preview.value = null
  importResult.value = null
}

function handleFileRemove() {
  selectedFile.value = null
  preview.value = null
  importResult.value = null
}

async function handlePreview() {
  if (!projectStore.currentProjectId || !selectedFile.value) {
    return
  }
  previewLoading.value = true
  try {
    preview.value = await previewExcelImport(projectStore.currentProjectId, selectedFile.value)
    importResult.value = null
    if (preview.value.valid) {
      ElMessage.success('预览通过')
    }
  } finally {
    previewLoading.value = false
  }
}

async function handleImport() {
  if (!projectStore.currentProjectId || !selectedFile.value || !preview.value?.valid) {
    return
  }
  importLoading.value = true
  try {
    importResult.value = await importExcel(projectStore.currentProjectId, selectedFile.value)
    if (importResult.value.success === false) {
      ElMessage.warning('导入未写入，请先修正 Excel')
      return
    }
    ElMessage.success('导入完成')
  } finally {
    importLoading.value = false
  }
}

function summaryItem(key: string, label: string, summary?: ExcelSheetSummary) {
  return {
    key,
    label,
    total: summary?.total ?? 0,
    createCount: summary?.createCount ?? 0,
    updateCount: summary?.updateCount ?? 0,
    conflictCount: summary?.conflictCount ?? 0
  }
}

function actionLabel(action?: string) {
  if (action === 'CREATE') {
    return '新增'
  }
  if (action === 'UPDATE') {
    return '更新'
  }
  if (action === 'CONFLICT') {
    return '冲突'
  }
  return action || '-'
}

function actionTagType(action?: string) {
  if (action === 'CREATE') {
    return 'success'
  }
  if (action === 'UPDATE') {
    return 'warning'
  }
  if (action === 'CONFLICT') {
    return 'danger'
  }
  return 'info'
}

function statusLabel(status?: string) {
  if (status === 'READY') {
    return '待写入'
  }
  if (status === 'BLOCKED') {
    return '阻塞'
  }
  return status || '-'
}

function statusTagType(status?: string) {
  return status === 'BLOCKED' ? 'danger' : 'success'
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.import-export-page {
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

.header-actions,
.import-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tool-section {
  padding: 18px 0;
  border-top: 1px solid #ebeef5;
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.upload-row {
  display: grid;
  grid-template-columns: minmax(260px, 420px) 1fr;
  gap: 16px;
  align-items: start;
}

.upload-box {
  width: 100%;
}

.upload-icon {
  margin-top: 12px;
  font-size: 28px;
  color: #64748b;
}

.upload-title {
  margin-bottom: 12px;
  color: #334155;
  font-size: 14px;
}

.summary-grid,
.result-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-item,
.result-grid > div {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.summary-label {
  color: #6b7280;
  font-size: 13px;
}

.summary-main {
  margin-top: 6px;
  color: #111827;
  font-size: 28px;
  font-weight: 700;
}

.summary-sub {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
}

.dry-run-table,
.error-table {
  margin-top: 14px;
}

.diff-table {
  width: calc(100% - 24px);
  margin: 4px 12px;
}

.result-number {
  margin-right: 8px;
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

@media (max-width: 760px) {
  .page-header,
  .section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .import-actions {
    justify-content: flex-start;
  }

  .upload-row,
  .summary-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
