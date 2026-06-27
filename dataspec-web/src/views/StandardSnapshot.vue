<template>
  <div class="snapshot-page">
    <div class="page-header">
      <div>
        <h2>标准快照</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" :disabled="!hasProject" @click="loadSnapshots">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          创建快照
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <el-alert
        class="snapshot-alert"
        type="success"
        :closable="false"
        show-icon
        :title="`当前标准版本：${formatSnapshotLabel(currentSnapshot)}`"
      />

      <el-table
        v-loading="loading"
        :data="snapshots"
        stripe
        class="snapshot-table"
        empty-text="暂无标准快照"
      >
        <el-table-column prop="specVersion" label="版本" min-width="160" fixed="left" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="Hash" min-width="220">
          <template #default="{ row }">
            <code>{{ row.specHash || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-dialog v-model="dialogVisible" title="创建标准快照" width="560px">
      <el-form label-width="96px" @submit.prevent="handleCreate">
        <el-form-item label="版本号">
          <el-input v-model="form.version" maxlength="100" placeholder="v2026.06.24" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="100" placeholder="AI Context 可复现基线" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleCreate">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  createStandardSnapshot,
  getCurrentStandardSnapshot,
  listStandardSnapshots
} from '@/api/standardSnapshot'
import { useProjectStore } from '@/stores/project'
import { canSubmitSnapshotForm, formatSnapshotLabel } from '@/utils/standardSnapshotDisplay'
import type { StandardSnapshotCreateReq, StandardSnapshotInfo } from '@/types'

const projectStore = useProjectStore()
const currentSnapshot = ref<StandardSnapshotInfo | null>(null)
const snapshots = ref<StandardSnapshotInfo[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)

const form = reactive<StandardSnapshotCreateReq>({
  version: '',
  name: '',
  description: ''
})

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const canSubmit = computed(() => canSubmitSnapshotForm(form))

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadSnapshots()
  },
  { immediate: true }
)

async function loadSnapshots() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    currentSnapshot.value = null
    snapshots.value = []
    return
  }
  loading.value = true
  try {
    const [current, list] = await Promise.all([
      getCurrentStandardSnapshot(projectId),
      listStandardSnapshots(projectId)
    ])
    currentSnapshot.value = current
    snapshots.value = list
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  form.version = defaultVersion()
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

async function handleCreate() {
  const projectId = projectStore.currentProjectId
  if (!projectId || !canSubmit.value) {
    return
  }
  submitting.value = true
  try {
    await createStandardSnapshot(projectId, {
      version: form.version?.trim(),
      name: form.name?.trim() || undefined,
      description: form.description?.trim() || undefined
    })
    dialogVisible.value = false
    ElMessage.success('标准快照已创建')
    await loadSnapshots()
  } finally {
    submitting.value = false
  }
}

function defaultVersion() {
  const date = new Date()
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `v${yyyy}.${mm}.${dd}`
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.snapshot-page {
  min-height: calc(100vh - 140px);
  padding: 20px;
  background: #fff;
  border-radius: 4px;
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
}

.page-subtitle {
  margin: 6px 0 0;
  color: #606266;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.snapshot-alert {
  margin-bottom: 16px;
}

.snapshot-table {
  width: 100%;
}

code {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
  }
}
</style>
