<template>
  <div class="project-backup-page">
    <div class="page-header">
      <div>
        <h2>项目备份</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!hasProject" :loading="exportLoading" type="primary" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出备份 JSON
        </el-button>
        <el-button :disabled="!hasProject" :loading="recordsLoading" @click="loadRecords">
          <el-icon><Refresh /></el-icon>
          刷新记录
        </el-button>
      </div>
    </div>

    <el-alert
      class="security-alert"
      type="success"
      show-icon
      :closable="false"
      title="备份包不包含 password/token/source rows、API token hash 或完整 JDBC URL。"
    />

    <section class="tool-section">
      <div class="section-header">
        <h3>导出当前项目</h3>
        <el-tag :type="hasProject ? 'success' : 'info'" effect="plain">
          {{ hasProject ? '已选择项目' : '未选择项目' }}
        </el-tag>
      </div>
      <el-empty v-if="!hasProject" description="未选择项目，导出已禁用；仍可在下方恢复到新项目。">
        <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
      </el-empty>
      <div v-else class="export-summary">
        <span>当前项目：</span>
        <strong>{{ projectStore.currentProjectName }}</strong>
        <span class="muted">导出后可在其他本地环境粘贴 JSON 进行 dry-run。</span>
      </div>
    </section>

    <section class="tool-section">
      <div class="section-header">
        <h3>恢复备份包</h3>
        <el-tag type="warning" effect="plain">先 dry-run，再确认恢复</el-tag>
      </div>

      <div class="restore-grid">
        <div class="restore-input">
          <el-upload
            drag
            accept=".json,application/json"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="upload-icon"><Upload /></el-icon>
            <div class="upload-title">选择备份 JSON</div>
          </el-upload>

          <el-input
            v-model="backupText"
            class="backup-textarea"
            type="textarea"
            :rows="12"
            placeholder="粘贴备份 JSON"
            @input="resetPreview"
          />
        </div>

        <div class="restore-options">
          <el-form label-position="top">
            <el-form-item label="恢复目标">
              <el-radio-group v-model="targetMode">
                <el-radio-button label="new">恢复到新项目</el-radio-button>
                <el-radio-button label="current" :disabled="!hasProject">恢复到当前项目</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="覆盖策略">
              <el-switch
                v-model="overwrite"
                active-text="允许覆盖"
                inactive-text="默认跳过"
              />
            </el-form-item>
          </el-form>

          <div class="restore-actions">
            <el-button :disabled="!canPreview" :loading="previewLoading" @click="handlePreview">
              <el-icon><View /></el-icon>
              预览恢复
            </el-button>
            <el-button
              type="primary"
              :disabled="!canApply"
              :loading="applyLoading"
              @click="handleApply"
            >
              <el-icon><Check /></el-icon>
              确认恢复
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section v-if="restorePlan" class="tool-section">
      <div class="section-header">
        <h3>恢复计划</h3>
        <el-tag :type="restorePlan.canApply ? 'success' : 'danger'" effect="plain">
          {{ restorePlan.canApply ? '可恢复' : '不可恢复' }}
        </el-tag>
      </div>

      <el-descriptions :column="3" border class="plan-meta">
        <el-descriptions-item label="目标项目">{{ restorePlan.targetProjectName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="兼容性">{{ restorePlan.compatibilityStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="覆盖">{{ restorePlan.overwrite ? '允许' : '不覆盖' }}</el-descriptions-item>
      </el-descriptions>

      <div class="summary-grid">
        <div v-for="item in countItems" :key="item.key" class="summary-item">
          <span class="summary-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>

      <el-alert
        v-for="warning in restorePlan.warnings || []"
        :key="warning"
        class="warning-alert"
        type="warning"
        show-icon
        :closable="false"
        :title="warning"
      />

      <el-table :data="restorePlan.items || []" stripe empty-text="暂无恢复计划明细">
        <el-table-column prop="assetType" label="资产类型" width="150" />
        <el-table-column prop="key" label="业务键" min-width="180" show-overflow-tooltip />
        <el-table-column label="动作" width="120">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" size="small" effect="plain">
              {{ actionLabel(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="260" show-overflow-tooltip />
      </el-table>
    </section>

    <section v-if="restoreResult" class="tool-section">
      <div class="section-header">
        <h3>恢复结果</h3>
        <el-tag type="success" effect="plain">已完成</el-tag>
      </div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="目标项目">{{ restoreResult.plan?.targetProjectName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Package Hash">{{ shortHash(restoreResult.record?.packageHash) }}</el-descriptions-item>
        <el-descriptions-item label="恢复时间">{{ formatDate(restoreResult.record?.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="tool-section">
      <div class="section-header">
        <h3>最近恢复记录</h3>
        <el-tag effect="plain">{{ records.length }} 条</el-tag>
      </div>
      <el-empty v-if="!hasProject" description="请选择项目后查看最近恢复记录" />
      <el-table
        v-else
        v-loading="recordsLoading"
        :data="records"
        stripe
        empty-text="暂无恢复记录"
      >
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="sourceProjectName" label="来源项目" min-width="160" show-overflow-tooltip />
        <el-table-column label="Hash" width="120">
          <template #default="{ row }">{{ shortHash(row.packageHash) }}</template>
        </el-table-column>
        <el-table-column label="计数" min-width="260">
          <template #default="{ row }">
            创建 {{ row.createdCount ?? 0 }} / 更新 {{ row.updatedCount ?? 0 }} /
            跳过 {{ row.skippedCount ?? 0 }} / 冲突 {{ row.conflictCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="覆盖" width="90">
          <template #default="{ row }">{{ row.overwrite ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作者" width="120" />
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Check, Download, Refresh, Upload, View } from '@element-plus/icons-vue'
import {
  applyProjectBackupRestore,
  exportProjectBackup,
  listProjectRestoreRecords,
  previewProjectBackupRestore
} from '@/api/projectBackup'
import { createClientIdempotencyKey } from '@/api/idempotency'
import { useProjectStore } from '@/stores/project'
import type {
  ProjectBackupPackage,
  ProjectRestorePlan,
  ProjectRestoreRecord,
  ProjectRestoreResult
} from '@/types'

type TargetMode = 'new' | 'current'

const projectStore = useProjectStore()
const backupText = ref('')
const targetMode = ref<TargetMode>('new')
const overwrite = ref(false)
const restorePlan = ref<ProjectRestorePlan | null>(null)
const restoreResult = ref<ProjectRestoreResult | null>(null)
const records = ref<ProjectRestoreRecord[]>([])
const exportLoading = ref(false)
const previewLoading = ref(false)
const applyLoading = ref(false)
const recordsLoading = ref(false)

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const canPreview = computed(() => backupText.value.trim().length > 0)
const canApply = computed(() => Boolean(restorePlan.value?.canApply && backupText.value.trim()))
const countItems = computed(() => {
  const counts = restorePlan.value?.counts
  return [
    { key: 'created', label: '创建', value: counts?.created ?? 0 },
    { key: 'updated', label: '更新', value: counts?.updated ?? 0 },
    { key: 'skipped', label: '跳过', value: counts?.skipped ?? 0 },
    { key: 'conflicts', label: '冲突', value: counts?.conflicts ?? 0 },
    { key: 'blocked', label: '阻塞', value: counts?.blocked ?? 0 },
    { key: 'warnings', label: '警告', value: counts?.warnings ?? 0 }
  ]
})

onMounted(async () => {
  if (projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  await loadRecords()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    if (!hasProject.value) {
      targetMode.value = 'new'
    }
    restorePlan.value = null
    restoreResult.value = null
    void loadRecords()
  }
)

watch([targetMode, overwrite], () => {
  resetPreview()
})

async function handleExport() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  exportLoading.value = true
  try {
    const backupPackage = await exportProjectBackup(projectId)
    const filename = `dataspec-project-backup-${projectId}.json`
    downloadJson(backupPackage, filename)
    ElMessage.success('备份 JSON 已导出')
  } finally {
    exportLoading.value = false
  }
}

function handleFileChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) {
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    backupText.value = String(reader.result || '')
    resetPreview()
  }
  reader.readAsText(file, 'utf-8')
}

function handleFileRemove() {
  backupText.value = ''
  resetPreview()
}

function resetPreview() {
  restorePlan.value = null
  restoreResult.value = null
}

async function handlePreview() {
  const backupPackage = parseBackupPackage()
  if (!backupPackage) {
    return
  }
  previewLoading.value = true
  try {
    restorePlan.value = await previewProjectBackupRestore({
      targetProjectId: selectedTargetProjectId(),
      overwrite: overwrite.value,
      backupPackage
    })
    restoreResult.value = null
    ElMessage.success('dry-run 预览完成')
  } finally {
    previewLoading.value = false
  }
}

async function handleApply() {
  const backupPackage = parseBackupPackage()
  if (!backupPackage || !restorePlan.value?.canApply) {
    return
  }
  await ElMessageBox.confirm(
    '确认按当前 dry-run 计划恢复备份包吗？该操作不会删除目标项目已有资产。',
    '确认恢复',
    { type: 'warning' }
  )
  applyLoading.value = true
  try {
    const idempotencyKey = createClientIdempotencyKey('project-backup:restore-apply')
    restoreResult.value = await applyProjectBackupRestore({
      targetProjectId: selectedTargetProjectId(),
      overwrite: overwrite.value,
      backupPackage,
      dryRunToken: restorePlan.value.dryRunToken
    }, idempotencyKey)
    restorePlan.value = restoreResult.value.plan ?? restorePlan.value
    ElMessage.success('备份包已恢复')
    await projectStore.loadProjects()
    if (restoreResult.value.plan?.targetProjectId) {
      projectStore.setCurrentProjectById(restoreResult.value.plan.targetProjectId)
    }
    await loadRecords()
  } finally {
    applyLoading.value = false
  }
}

async function loadRecords() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    records.value = []
    return
  }
  recordsLoading.value = true
  try {
    records.value = await listProjectRestoreRecords(projectId)
  } finally {
    recordsLoading.value = false
  }
}

function selectedTargetProjectId() {
  return targetMode.value === 'current' ? (projectStore.currentProjectId ?? undefined) : undefined
}

function parseBackupPackage(): ProjectBackupPackage | null {
  try {
    const parsed = JSON.parse(backupText.value) as ProjectBackupPackage
    if (!parsed || typeof parsed !== 'object') {
      ElMessage.warning('备份 JSON 格式不正确')
      return null
    }
    return parsed
  } catch {
    ElMessage.warning('备份 JSON 解析失败')
    return null
  }
}

function downloadJson(data: ProjectBackupPackage, filename: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function actionLabel(action?: string) {
  if (action === 'CREATE') {
    return '创建'
  }
  if (action === 'UPDATE') {
    return '更新'
  }
  if (action === 'SKIP') {
    return '跳过'
  }
  if (action === 'CONFLICT') {
    return '冲突'
  }
  if (action === 'BLOCKED') {
    return '阻塞'
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
  if (action === 'CONFLICT' || action === 'BLOCKED') {
    return 'danger'
  }
  return 'info'
}

function shortHash(value?: string) {
  return value ? value.slice(0, 12) : '-'
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.project-backup-page {
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
  margin-bottom: 16px;
}

.page-header h2,
.section-header h3 {
  margin: 0;
  color: #1f2937;
  font-weight: 600;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.header-actions,
.restore-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.security-alert {
  margin-bottom: 16px;
}

.tool-section {
  padding: 18px 0;
  border-top: 1px solid #ebeef5;
}

.section-header {
  align-items: center;
  margin-bottom: 14px;
}

.export-summary {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  color: #334155;
}

.muted {
  color: #64748b;
  font-size: 13px;
}

.restore-grid {
  display: grid;
  grid-template-columns: minmax(360px, 1.4fr) minmax(260px, 0.8fr);
  gap: 16px;
  align-items: start;
}

.restore-input {
  min-width: 0;
}

.upload-icon {
  margin-top: 12px;
  color: #64748b;
  font-size: 28px;
}

.upload-title {
  margin-bottom: 12px;
  color: #334155;
  font-size: 14px;
}

.backup-textarea {
  margin-top: 12px;
}

.restore-options {
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.restore-actions {
  justify-content: flex-start;
}

.plan-meta,
.warning-alert {
  margin-bottom: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.summary-item {
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.summary-label {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.summary-item strong {
  color: #111827;
  font-size: 22px;
}

@media (max-width: 900px) {
  .page-header,
  .section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .restore-actions {
    justify-content: flex-start;
  }

  .restore-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
