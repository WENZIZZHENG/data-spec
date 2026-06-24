<template>
  <div class="token-page">
    <div class="page-header">
      <div>
        <h2>API Token 管理</h2>
        <p class="page-subtitle">管理 CLI、MCP 和个人脚本访问 DataSpec 的轻量凭据</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadTokens">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建 Token
        </el-button>
      </div>
    </div>

    <el-alert
      class="scope-alert"
      type="info"
      :closable="false"
      show-icon
      title="管理 Token 需要全项目权限；创建后只显示一次明文 Token。"
    />

    <el-table
      v-loading="loading"
      :data="tokens"
      stripe
      class="token-table"
      empty-text="暂无 API Token"
    >
      <el-table-column prop="name" label="名称" min-width="180" fixed="left" />
      <el-table-column prop="operatorName" label="操作者" min-width="140" />
      <el-table-column label="项目范围" min-width="180">
        <template #default="{ row }">
          {{ formatTokenProjectScope(row) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近使用" width="180">
        <template #default="{ row }">
          {{ formatDate(row.lastUsedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="停用时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.disabledAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            text
            type="danger"
            :disabled="!row.enabled || !row.id"
            @click="handleDisable(row)"
          >
            停用
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createDialogVisible" title="新建 API Token" width="560px">
      <el-form label-width="104px" @submit.prevent="handleCreate">
        <el-form-item label="Token 名称">
          <el-input v-model="form.name" maxlength="100" placeholder="cli-main" />
        </el-form-item>
        <el-form-item label="操作者">
          <el-input v-model="form.operatorName" maxlength="100" placeholder="alice" />
        </el-form-item>
        <el-form-item label="全部项目">
          <el-switch v-model="form.allProjects" @change="handleScopeModeChange" />
        </el-form-item>
        <el-form-item v-if="!form.allProjects" label="授权项目">
          <el-select
            v-model="form.projectIds"
            multiple
            filterable
            class="full-width"
            placeholder="请选择项目"
            :loading="projectStore.loading"
          >
            <el-option
              v-for="project in projectOptions"
              :key="project.id"
              :label="project.name || `项目 ${project.id}`"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleCreate"
        >
          创建
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="plainTokenDialogVisible" title="Token 已创建" width="640px">
      <el-alert
        class="one-time-alert"
        type="warning"
        :closable="false"
        show-icon
        title="请立即复制保存，关闭后无法再次查看明文 Token。"
      />
      <el-input
        :model-value="createdToken?.plainToken"
        type="textarea"
        :rows="3"
        readonly
        class="plain-token-input"
      />
      <template #footer>
        <el-button @click="plainTokenDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleCopyCreatedToken">
          <el-icon><DocumentCopy /></el-icon>
          复制
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy, Plus, Refresh } from '@element-plus/icons-vue'
import { createApiToken, disableApiToken, listApiTokens } from '@/api/token'
import { useProjectStore } from '@/stores/project'
import { canSubmitApiTokenForm, formatTokenProjectScope } from '@/utils/apiTokenDisplay'
import type { ApiTokenCreateReq, ApiTokenCreateResp, ApiTokenInfo, Project } from '@/types'

type ProjectOption = Project & { id: number }

const projectStore = useProjectStore()
const tokens = ref<ApiTokenInfo[]>([])
const loading = ref(false)
const submitting = ref(false)
const createDialogVisible = ref(false)
const plainTokenDialogVisible = ref(false)
const createdToken = ref<ApiTokenCreateResp | null>(null)

const form = reactive<ApiTokenCreateReq>({
  name: '',
  operatorName: '',
  allProjects: true,
  projectIds: []
})

const projectOptions = computed<ProjectOption[]>(() =>
  projectStore.projects.filter((project): project is ProjectOption => typeof project.id === 'number')
)

const canSubmit = computed(() => canSubmitApiTokenForm(form))

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
  void loadTokens()
})

watch(plainTokenDialogVisible, (visible) => {
  if (!visible) {
    createdToken.value = null
  }
})

async function loadTokens() {
  loading.value = true
  try {
    tokens.value = await listApiTokens()
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  form.name = ''
  form.operatorName = ''
  form.allProjects = true
  form.projectIds = []
  createDialogVisible.value = true
}

function handleScopeModeChange(value: string | number | boolean) {
  if (Boolean(value)) {
    form.projectIds = []
  }
}

async function handleCreate() {
  if (!canSubmit.value) {
    ElMessage.warning('请填写 Token 名称、操作者和授权项目')
    return
  }
  submitting.value = true
  try {
    createdToken.value = await createApiToken({
      name: form.name?.trim(),
      operatorName: form.operatorName?.trim(),
      allProjects: form.allProjects,
      projectIds: form.allProjects ? [] : form.projectIds
    })
    createDialogVisible.value = false
    plainTokenDialogVisible.value = true
    ElMessage.success('API Token 已创建')
    await loadTokens()
  } finally {
    submitting.value = false
  }
}

async function handleDisable(token: ApiTokenInfo) {
  if (!token.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定停用「${token.name ?? ''}」吗？`, '停用 API Token', {
      type: 'warning',
      confirmButtonText: '停用',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await disableApiToken(token.id)
  ElMessage.success('API Token 已停用')
  await loadTokens()
}

async function handleCopyCreatedToken() {
  const token = createdToken.value?.plainToken
  if (!token) {
    return
  }
  await copyText(token)
  ElMessage.success('已复制 Token')
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
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
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.token-page {
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

.scope-alert {
  margin-bottom: 16px;
}

.token-table {
  width: 100%;
}

.full-width {
  width: 100%;
}

.one-time-alert {
  margin-bottom: 14px;
}

.plain-token-input {
  font-family: Consolas, Monaco, monospace;
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
