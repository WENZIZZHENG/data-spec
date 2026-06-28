<template>
  <div class="ai-profile-page">
    <div class="page-header">
      <div>
        <h2>AI 任务模式</h2>
        <p>当前选择只作为 AI/CLI/MCP 默认建议，不写入项目权限或外部模型配置。</p>
      </div>
      <el-button :loading="loading" @click="loadProfiles">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </div>

    <el-alert
      v-if="!projectStore.currentProjectId"
      type="warning"
      show-icon
      :closable="false"
      title="请选择项目后查看 AI 任务模式"
    />

    <template v-else>
      <div class="toolbar-panel">
        <div class="profile-selector">
          <span>当前模式</span>
          <el-select
            v-model="selectedProfileId"
            :loading="loading"
            placeholder="选择 AI profile"
            class="profile-select"
            @change="handleProfileChange"
          >
            <el-option
              v-for="profile in profiles"
              :key="profile.profileId"
              :label="profileLabel(profile)"
              :value="profile.profileId"
            />
          </el-select>
        </div>
        <el-tag v-if="selectedProfile?.taskType" type="info">{{ selectedProfile.taskType }}</el-tag>
        <el-tag v-if="selectedProfile?.fixedSqlPolicy?.mode" :type="fixModeTag(selectedProfile.fixedSqlPolicy.mode)">
          {{ selectedProfile.fixedSqlPolicy.mode }}
        </el-tag>
      </div>

      <div v-if="diagnostics.length" class="diagnostic-panel">
        <div
          v-for="diagnostic in diagnostics"
          :key="diagnostic.code"
          class="diagnostic-item"
        >
          <el-tag size="small" :type="diagnosticTag(diagnostic.status)">
            {{ diagnostic.status || '-' }}
          </el-tag>
          <div>
            <strong>{{ diagnostic.code }}</strong>
            <span>{{ diagnostic.message }}</span>
            <small v-if="diagnostic.nextAction">{{ diagnostic.nextAction }}</small>
          </div>
        </div>
      </div>

      <div class="content-grid">
        <div class="profile-list-panel">
          <div class="panel-title">可用模式</div>
          <el-table
            v-loading="loading"
            :data="profiles"
            stripe
            class="profile-table"
            @row-click="selectProfile"
          >
            <el-table-column label="模式" min-width="160">
              <template #default="{ row }">
                <div class="profile-name">
                  <strong>{{ row.displayName || row.profileId }}</strong>
                  <small>{{ row.profileId }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="taskType" label="任务类型" width="150" />
            <el-table-column label="上下文" width="150">
              <template #default="{ row }">
                {{ scopeLabel(row.contextScope) }}
              </template>
            </el-table-column>
            <el-table-column label="fixedSql" width="130">
              <template #default="{ row }">
                {{ row.fixedSqlPolicy?.mode || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="字段上限" width="100">
              <template #default="{ row }">
                {{ row.maxContextFields ?? '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="profile-detail-panel">
          <div class="panel-title">模式详情</div>
          <el-empty v-if="!selectedProfile" description="请选择 AI 任务模式" />
          <template v-else>
            <div class="detail-block">
              <h3>{{ selectedProfile.displayName }}</h3>
              <p>{{ selectedProfile.description }}</p>
            </div>

            <div class="detail-grid">
              <div>
                <span class="detail-label">contextScope</span>
                <strong>{{ scopeLabel(selectedProfile.contextScope) }}</strong>
              </div>
              <div>
                <span class="detail-label">ruleset</span>
                <strong>{{ selectedProfile.ruleset?.strictness || '-' }}</strong>
              </div>
              <div>
                <span class="detail-label">outputFormat</span>
                <strong>{{ selectedProfile.outputFormat?.format || '-' }}</strong>
              </div>
              <div>
                <span class="detail-label">maxContextFields</span>
                <strong>{{ selectedProfile.maxContextFields ?? '-' }}</strong>
              </div>
            </div>

            <div v-if="selectedProfile.ruleset?.requiredRuleCodes?.length" class="tag-block">
              <span class="detail-label">必读规则</span>
              <div class="tag-list">
                <el-tag
                  v-for="rule in selectedProfile.ruleset.requiredRuleCodes"
                  :key="rule"
                  size="small"
                  type="info"
                >
                  {{ rule }}
                </el-tag>
              </div>
            </div>

            <div v-if="selectedProfile.recommendedCommands?.length" class="command-block">
              <div class="block-header">
                <span>推荐命令</span>
                <el-button size="small" text type="primary" @click="copyFirstCommand">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
              <pre>{{ selectedProfile.recommendedCommands.join('\n') }}</pre>
            </div>

            <ul v-if="selectedProfile.nextActions?.length" class="next-actions">
              <li v-for="(action, index) in selectedProfile.nextActions" :key="index">{{ action }}</li>
            </ul>
          </template>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listAiProfiles } from '@/api/aiProfile'
import { useProjectStore } from '@/stores/project'
import {
  readSelectedAiProfile,
  resolveSelectedAiProfile,
  saveSelectedAiProfile
} from '@/utils/aiProfileSelection'
import type { AiProfileDiagnostic, AiTaskContextScope, AiTaskProfile } from '@/types'

const projectStore = useProjectStore()
const loading = ref(false)
const profiles = ref<AiTaskProfile[]>([])
const diagnostics = ref<AiProfileDiagnostic[]>([])
const selectedProfileId = ref('')

const selectedProfile = computed(() =>
  profiles.value.find((profile) => profile.profileId === selectedProfileId.value) ?? null
)

onMounted(() => {
  if (projectStore.currentProjectId) {
    void loadProfiles()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadProfiles()
  }
)

async function loadProfiles() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    profiles.value = []
    diagnostics.value = []
    selectedProfileId.value = ''
    return
  }
  loading.value = true
  try {
    const stored = readSelectedAiProfile(projectId)
    const catalog = await listAiProfiles(projectId, stored || undefined)
    profiles.value = catalog.profiles ?? []
    diagnostics.value = catalog.diagnostics ?? []
    selectedProfileId.value = resolveSelectedAiProfile(stored, catalog.selectedProfileId || catalog.defaultProfileId)
    if (selectedProfileId.value) {
      saveSelectedAiProfile(projectId, selectedProfileId.value)
    }
  } finally {
    loading.value = false
  }
}

function handleProfileChange(value?: string) {
  selectedProfileId.value = value || ''
  saveSelectedAiProfile(projectStore.currentProjectId, selectedProfileId.value)
}

function selectProfile(profile: AiTaskProfile) {
  if (!profile.profileId) {
    return
  }
  selectedProfileId.value = profile.profileId
  handleProfileChange(profile.profileId)
}

async function copyFirstCommand() {
  const command = selectedProfile.value?.recommendedCommands?.[0]
  if (!command) {
    return
  }
  try {
    await navigator.clipboard.writeText(command)
    ElMessage.success('已复制推荐命令')
  } catch {
    ElMessage.error('复制失败，请手动选择命令')
  }
}

function profileLabel(profile: AiTaskProfile) {
  return `${profile.displayName || profile.profileId} / ${profile.taskType || '-'}`
}

function scopeLabel(scope?: AiTaskContextScope) {
  if (!scope) {
    return '-'
  }
  return [
    scope.scope || 'all',
    scope.query ? `query=${scope.query}` : '',
    scope.status ? `status=${scope.status}` : '',
    scope.limit ? `limit=${scope.limit}` : ''
  ].filter(Boolean).join(' / ')
}

function diagnosticTag(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    pass: 'success',
    warn: 'warning',
    fail: 'danger'
  }
  return status ? map[status] ?? 'info' : 'info'
}

function fixModeTag(mode?: string) {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    GENERATE: 'success',
    DRY_RUN: 'warning',
    DISABLED: 'info'
  }
  return mode ? map[mode] ?? 'info' : 'info'
}
</script>

<style scoped>
.ai-profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2,
.detail-block h3 {
  margin: 0;
}

.page-header p,
.detail-block p {
  margin: 6px 0 0;
  color: #606266;
  font-size: 13px;
}

.toolbar-panel,
.diagnostic-panel,
.profile-list-panel,
.profile-detail-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.toolbar-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 12px;
}

.profile-selector {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.profile-select {
  width: 260px;
}

.diagnostic-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
}

.diagnostic-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
  align-items: flex-start;
}

.diagnostic-item div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #606266;
  font-size: 13px;
}

.diagnostic-item small,
.profile-name small,
.detail-label {
  color: #909399;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 16px;
}

.panel-title {
  padding: 10px 12px;
  font-weight: 600;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.profile-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-detail-panel {
  padding-bottom: 12px;
}

.profile-detail-panel > :not(.panel-title) {
  margin: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-grid > div,
.tag-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-weight: 600;
}

.command-block pre {
  margin: 0;
  padding: 10px;
  overflow: auto;
  color: #1f2d3d;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
  line-height: 1.6;
}

.next-actions {
  margin-bottom: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
