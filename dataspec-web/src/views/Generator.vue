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

      <section class="dictionary-section">
        <div class="section-header">
          <h3>数据字典</h3>
          <div class="dictionary-actions">
            <el-button
              :loading="dictionaryLoading === 'html'"
              :disabled="!hasProject || dictionaryBusy"
              @click="handlePreviewDictionaryHtml"
            >
              <el-icon><View /></el-icon>
              预览 HTML
            </el-button>
            <el-button
              :loading="dictionaryLoading === 'erd'"
              :disabled="!hasProject || dictionaryBusy"
              @click="handlePreviewDictionaryErd"
            >
              <el-icon><View /></el-icon>
              预览 ERD
            </el-button>
            <el-button
              type="primary"
              :loading="dictionaryDownloading === 'html'"
              :disabled="!hasProject || dictionaryDownloadBusy"
              @click="handleDownloadDictionaryHtml"
            >
              <el-icon><Download /></el-icon>
              下载 HTML
            </el-button>
            <el-button
              :loading="dictionaryDownloading === 'erd'"
              :disabled="!hasProject || dictionaryDownloadBusy"
              @click="handleDownloadDictionaryErd"
            >
              <el-icon><Download /></el-icon>
              下载 ERD
            </el-button>
          </div>
        </div>

        <el-tabs v-if="hasDictionaryPreview" v-model="activeDictionaryTab" class="dictionary-preview">
          <el-tab-pane label="HTML" name="html">
            <iframe
              v-if="dictionaryHtml"
              class="html-preview"
              title="HTML 数据字典预览"
              sandbox=""
              :srcdoc="dictionaryHtml"
            />
            <el-empty v-else description="暂无 HTML 预览" />
          </el-tab-pane>
          <el-tab-pane label="ERD" name="erd">
            <pre v-if="dictionaryErd" class="erd-code">{{ dictionaryErd }}</pre>
            <el-empty v-else description="暂无 ERD 预览" />
          </el-tab-pane>
        </el-tabs>
      </section>

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

        <div v-if="ddlDialectDiagnostics.length" class="dialect-panel">
          <div class="dialect-header">
            <span>方言诊断</span>
            <el-tag size="small" :type="diagnosticSummaryTagType(ddlDialectDiagnostics)">
              {{ dialectSummary(ddlDialectDiagnostics) }}
            </el-tag>
          </div>
          <div class="diagnostic-list">
            <div
              v-for="diagnostic in ddlDialectDiagnostics"
              :key="diagnostic.code || `${diagnostic.dialect}-${diagnostic.capability}`"
              class="diagnostic-item"
            >
              <el-tag size="small" :type="diagnosticTagType(diagnostic.level)">
                {{ diagnosticLevelLabel(diagnostic.level) }}
              </el-tag>
              <div class="diagnostic-copy">
                <span>{{ diagnostic.message }}</span>
                <small v-if="diagnostic.nextAction">{{ diagnostic.nextAction }}</small>
              </div>
            </div>
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
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CopyDocument, Download, Refresh, View } from '@element-plus/icons-vue'
import {
  downloadDataDictionaryErd,
  downloadDataDictionaryHtml,
  previewDataDictionaryErd,
  previewDataDictionaryHtml,
  previewDdl
} from '@/api/generator'
import { listTemplateFields, listTemplates } from '@/api/template'
import { useProjectStore } from '@/stores/project'
import {
  diagnosticLevelLabel,
  diagnosticSummaryTagType,
  diagnosticTagType,
  dialectSummary
} from '@/utils/dialectDiagnostics'
import type { DdlGenerateResult, LintIssue, Template, TemplateField } from '@/types'

const projectStore = useProjectStore()
const route = useRoute()
const templates = ref<Template[]>([])
const templateFields = ref<TemplateField[]>([])
const selectedTemplateId = ref<number | null>(null)
const tableName = ref('')
const result = ref<DdlGenerateResult | null>(null)
const templateLoading = ref(false)
const fieldLoading = ref(false)
const generating = ref(false)
const dictionaryHtml = ref('')
const dictionaryErd = ref('')
const activeDictionaryTab = ref<'html' | 'erd'>('html')
const dictionaryLoading = ref<'html' | 'erd' | ''>('')
const dictionaryDownloading = ref<'html' | 'erd' | ''>('')

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const canGenerate = computed(() =>
  Boolean(projectStore.currentProjectId && selectedTemplateId.value && tableName.value.trim())
)
const lintIssues = computed<LintIssue[]>(() => result.value?.lintResult?.issues ?? [])
const ddlDialectDiagnostics = computed(() => [
  ...(result.value?.dialectDiagnostics ?? []),
  ...(result.value?.lintResult?.dialectDiagnostics ?? [])
])
const hasDictionaryPreview = computed(() => Boolean(dictionaryHtml.value || dictionaryErd.value))
const dictionaryBusy = computed(() => Boolean(dictionaryLoading.value))
const dictionaryDownloadBusy = computed(() => Boolean(dictionaryDownloading.value))

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

watch(
  () => [route.query.templateId, route.query.tableName],
  () => {
    applyTableNameFromQuery()
    const queryTemplateId = templateIdFromQuery()
    if (queryTemplateId) {
      selectedTemplateId.value = queryTemplateId
    }
  }
)

async function loadTemplates() {
  const projectId = projectStore.currentProjectId
  result.value = null
  templateFields.value = []
  selectedTemplateId.value = null
  dictionaryHtml.value = ''
  dictionaryErd.value = ''
  activeDictionaryTab.value = 'html'
  if (!projectId) {
    templates.value = []
    return
  }
  templateLoading.value = true
  try {
    templates.value = await listTemplates(projectId)
    selectedTemplateId.value = templateIdFromQuery()
      ?? templates.value.find((template) => template.id)?.id
      ?? null
    applyTableNameFromQuery()
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
  saveBlob(new Blob([result.value.ddl], { type: 'text/sql;charset=utf-8' }), `${safeTableName}.sql`)
}

async function handlePreviewDictionaryHtml() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  dictionaryLoading.value = 'html'
  try {
    dictionaryHtml.value = await previewDataDictionaryHtml(projectId)
    activeDictionaryTab.value = 'html'
    ElMessage.success('HTML 数据字典已生成')
  } finally {
    dictionaryLoading.value = ''
  }
}

async function handlePreviewDictionaryErd() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  dictionaryLoading.value = 'erd'
  try {
    dictionaryErd.value = await previewDataDictionaryErd(projectId)
    activeDictionaryTab.value = 'erd'
    ElMessage.success('ERD 已生成')
  } finally {
    dictionaryLoading.value = ''
  }
}

async function handleDownloadDictionaryHtml() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  dictionaryDownloading.value = 'html'
  try {
    saveBlob(await downloadDataDictionaryHtml(projectId), 'data-dictionary.html')
  } finally {
    dictionaryDownloading.value = ''
  }
}

async function handleDownloadDictionaryErd() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  dictionaryDownloading.value = 'erd'
  try {
    saveBlob(await downloadDataDictionaryErd(projectId), 'data-dictionary.mmd')
  } finally {
    dictionaryDownloading.value = ''
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

function severityTagType(severity?: string) {
  if (severity === 'ERROR') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

function templateIdFromQuery() {
  const rawTemplateId = route.query.templateId
  const value = Array.isArray(rawTemplateId) ? rawTemplateId[0] : rawTemplateId
  const templateId = value ? Number(value) : NaN
  if (!Number.isFinite(templateId)) {
    return null
  }
  return templates.value.find((template) => template.id === templateId)?.id ?? null
}

function applyTableNameFromQuery() {
  const rawTableName = route.query.tableName
  const value = Array.isArray(rawTableName) ? rawTableName[0] : rawTableName
  if (value && /^[a-z][a-z0-9_]*$/.test(value)) {
    tableName.value = value
  }
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
.section-header,
.result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2,
.section-header h3,
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

.dictionary-section,
.result-section {
  margin-top: 20px;
}

.dictionary-section {
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.lint-summary,
.dictionary-actions,
.result-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.lint-summary {
  margin-top: 10px;
}

.dialect-panel {
  padding: 10px 12px;
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
}

.dialect-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.diagnostic-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.diagnostic-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.diagnostic-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.diagnostic-copy small {
  color: #909399;
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

.dictionary-preview {
  margin-top: 12px;
}

.html-preview {
  width: 100%;
  min-height: 520px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}

.erd-code {
  min-height: 220px;
  max-height: 440px;
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
}

@media (max-width: 960px) {
  .generator-toolbar {
    grid-template-columns: 1fr;
  }

  .page-header,
  .section-header,
  .result-header {
    flex-direction: column;
  }
}
</style>
