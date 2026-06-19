<template>
  <div class="ai-export-page">
    <div class="page-header">
      <div>
        <h2>AI Context</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!hasProject" :loading="previewLoading" @click="loadPreviews">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button
          type="primary"
          :disabled="!hasProject"
          :loading="downloadLoading"
          @click="handleDownloadPackage"
        >
          <el-icon><Download /></el-icon>
          下载 Zip
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" :loading="demoLoading" @click="handleCreateDemoProject">
        创建演示项目
      </el-button>
    </el-empty>

    <template v-else>
      <el-tabs v-model="activeTab" class="preview-tabs">
        <el-tab-pane label="DATABASE_RULES.md" name="databaseRules">
          <pre class="preview-code">{{ databaseRules || '暂无预览' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="field-catalog.json" name="fieldCatalog">
          <pre class="preview-code">{{ fieldCatalog || '暂无预览' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="rules.yaml" name="rulesYaml">
          <pre class="preview-code">{{ rulesYaml || '暂无预览' }}</pre>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import {
  downloadAiContextPackage,
  previewDatabaseRules,
  previewFieldCatalog,
  previewRulesYaml
} from '@/api/aicontext'
import { useProjectStore } from '@/stores/project'

const projectStore = useProjectStore()
const activeTab = ref<'databaseRules' | 'fieldCatalog' | 'rulesYaml'>('databaseRules')
const databaseRules = ref('')
const fieldCatalog = ref('')
const rulesYaml = ref('')
const previewLoading = ref(false)
const downloadLoading = ref(false)
const demoLoading = ref(false)

const hasProject = computed(() => Boolean(projectStore.currentProjectId))

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  await loadPreviews()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadPreviews()
  }
)

async function loadPreviews() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    databaseRules.value = ''
    fieldCatalog.value = ''
    rulesYaml.value = ''
    return
  }
  previewLoading.value = true
  try {
    const [rules, fields, yaml] = await Promise.all([
      previewDatabaseRules(projectId),
      previewFieldCatalog(projectId),
      previewRulesYaml(projectId)
    ])
    databaseRules.value = rules
    fieldCatalog.value = fields
    rulesYaml.value = yaml
  } finally {
    previewLoading.value = false
  }
}

async function handleDownloadPackage() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  downloadLoading.value = true
  try {
    saveBlob(await downloadAiContextPackage(projectId), 'dataspec-ai-context.zip')
  } finally {
    downloadLoading.value = false
  }
}

async function handleCreateDemoProject() {
  demoLoading.value = true
  try {
    const result = await projectStore.createDemoProjectAndSelect()
    ElMessage.success(result.created ? '演示项目已创建' : '已切换到演示项目')
    await loadPreviews()
  } finally {
    demoLoading.value = false
  }
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.ai-export-page {
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
  color: #1f2937;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preview-tabs {
  margin-top: 8px;
}

.preview-code {
  min-height: 520px;
  max-height: calc(100vh - 270px);
  overflow: auto;
  margin: 0;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #1f2933;
  color: #f8fafc;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
