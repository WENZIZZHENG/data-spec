<template>
  <div class="generator-page">
    <div class="page-header">
      <div>
        <h2>DDL 生成</h2>
        <p class="page-subtitle">
          {{ projectStore.currentProjectName || '未选择项目' }}
        </p>
      </div>
      <el-button :loading="templateLoading" :disabled="!hasProject" @click="loadTemplates">
        <el-icon><Refresh /></el-icon>
        刷新模板
      </el-button>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div class="generator-toolbar">
        <el-form class="generate-form" label-width="84px" @submit.prevent>
          <el-form-item label="表模板">
            <el-select
              v-model="selectedTemplateId"
              filterable
              class="full-width"
              placeholder="请选择表模板"
              :loading="templateLoading"
              :disabled="templateLoading || templates.length === 0"
            >
              <el-option
                v-for="template in templates"
                :key="template.id ?? template.name"
                :label="template.name || `模板 ${template.id}`"
                :value="template.id"
                :disabled="!template.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="表名">
            <el-input
              v-model="tableName"
              placeholder="user_order"
              clearable
              @keyup.enter="handleGenerate"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="generating"
              :disabled="!canGenerate"
              @click="handleGenerate"
            >
              生成 DDL
            </el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="fieldLoading"
          :data="templateFields"
          stripe
          class="field-table"
          empty-text="暂无模板字段"
        >
          <el-table-column prop="name" label="字段名" min-width="150" />
          <el-table-column prop="dataType" label="类型" width="150" />
          <el-table-column label="可空" width="80">
            <template #default="{ row }">
              <el-tag :type="row.nullable === false ? 'warning' : 'info'" size="small">
                {{ row.nullable === false ? '否' : '是' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="defaultValue" label="默认值" width="140" show-overflow-tooltip />
          <el-table-column prop="comment" label="注释" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>

      <el-empty v-if="templates.length === 0 && !templateLoading" description="当前项目暂无表模板" />

      <section v-if="result" class="result-section">
        <div class="result-header">
          <div>
            <h3>生成结果</h3>
            <div class="lint-summary">
              <el-tag type="danger">错误 {{ result.lintResult?.errorCount ?? 0 }}</el-tag>
              <el-tag type="warning">警告 {{ result.lintResult?.warningCount ?? 0 }}</el-tag>
              <el-tag type="info">建议 {{ result.lintResult?.suggestionCount ?? 0 }}</el-tag>
            </div>
          </div>
          <div class="result-actions">
            <el-button :disabled="!result.ddl" @click="handleCopyDdl">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
            <el-button type="primary" :disabled="!result.ddl" @click="handleDownloadDdl">
              <el-icon><Download /></el-icon>
              下载 SQL
            </el-button>
          </div>
        </div>

        <pre class="ddl-code">{{ result.ddl }}</pre>

        <el-table
          :data="lintIssues"
          stripe
          class="issue-table"
          empty-text="自检未发现问题"
        >
          <el-table-column label="级别" width="110">
            <template #default="{ row }">
              <el-tag :type="severityTagType(row.severity)" size="small">
                {{ row.severity || 'ERROR' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ruleCode" label="规则" width="190" />
          <el-table-column prop="tableName" label="表" width="150" />
          <el-table-column prop="columnName" label="字段" width="150" />
          <el-table-column prop="message" label="问题" min-width="260" show-overflow-tooltip />
          <el-table-column prop="suggestion" label="建议" min-width="220" show-overflow-tooltip />
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Download, Refresh } from '@element-plus/icons-vue'
import { previewDdl } from '@/api/generator'
import { listTemplateFields, listTemplates } from '@/api/template'
import { useProjectStore } from '@/stores/project'
import type { DdlGenerateResult, LintIssue, Template, TemplateField } from '@/types'

const projectStore = useProjectStore()
const templates = ref<Template[]>([])
const templateFields = ref<TemplateField[]>([])
const selectedTemplateId = ref<number | null>(null)
const tableName = ref('')
const result = ref<DdlGenerateResult | null>(null)
const templateLoading = ref(false)
const fieldLoading = ref(false)
const generating = ref(false)

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const canGenerate = computed(() =>
  Boolean(projectStore.currentProjectId && selectedTemplateId.value && tableName.value.trim())
)
const lintIssues = computed<LintIssue[]>(() => result.value?.lintResult?.issues ?? [])

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadTemplates()
  },
  { immediate: true }
)

watch(selectedTemplateId, () => {
  result.value = null
  void loadTemplateFields()
})

async function loadTemplates() {
  const projectId = projectStore.currentProjectId
  result.value = null
  templateFields.value = []
  selectedTemplateId.value = null
  if (!projectId) {
    templates.value = []
    return
  }
  templateLoading.value = true
  try {
    templates.value = await listTemplates(projectId)
    selectedTemplateId.value = templates.value.find((template) => template.id)?.id ?? null
  } finally {
    templateLoading.value = false
  }
}

async function loadTemplateFields() {
  const templateId = selectedTemplateId.value
  if (!templateId) {
    templateFields.value = []
    return
  }
  fieldLoading.value = true
  try {
    templateFields.value = await listTemplateFields(templateId)
  } finally {
    fieldLoading.value = false
  }
}

async function handleGenerate() {
  const projectId = projectStore.currentProjectId
  const templateId = selectedTemplateId.value
  const normalizedTableName = tableName.value.trim()
  if (!projectId || !templateId) {
    ElMessage.warning('请先选择项目和表模板')
    return
  }
  if (!/^[a-z][a-z0-9_]*$/.test(normalizedTableName)) {
    ElMessage.warning('表名需使用 snake_case，例如 user_order')
    return
  }
  generating.value = true
  try {
    result.value = await previewDdl(projectId, templateId, normalizedTableName)
    ElMessage.success('DDL 已生成')
  } finally {
    generating.value = false
  }
}

async function handleCopyDdl() {
  if (!result.value?.ddl) {
    return
  }
  try {
    await navigator.clipboard.writeText(result.value.ddl)
    ElMessage.success('DDL 已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择 SQL 文本')
  }
}

function handleDownloadDdl() {
  if (!result.value?.ddl) {
    return
  }
  const safeTableName = tableName.value.trim().replace(/[^a-z0-9_]+/g, '_') || 'dataspec_ddl'
  const blob = new Blob([result.value.ddl], { type: 'text/sql;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${safeTableName}.sql`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function severityTagType(severity?: string) {
  if (severity === 'ERROR') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}
</script>

<style scoped>
.generator-page {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 140px);
}

.page-header,
.result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2,
.result-header h3 {
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.generator-toolbar {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.generate-form {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.full-width {
  width: 100%;
}

.field-table,
.issue-table {
  width: 100%;
}

.result-section {
  margin-top: 20px;
}

.lint-summary,
.result-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lint-summary {
  margin-top: 10px;
}

.ddl-code {
  min-height: 220px;
  max-height: 440px;
  overflow: auto;
  margin: 0 0 16px;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #1f2933;
  color: #f8fafc;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

@media (max-width: 960px) {
  .generator-toolbar {
    grid-template-columns: 1fr;
  }

  .page-header,
  .result-header {
    flex-direction: column;
  }
}
</style>
