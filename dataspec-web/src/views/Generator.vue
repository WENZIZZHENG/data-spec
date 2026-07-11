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

      <section class="relation-section">
        <div class="section-header">
          <h3>关系摘要</h3>
          <el-tag size="small" type="info">
            边 {{ relationEdges.length }}
          </el-tag>
        </div>
        <el-table
          :data="relationEdges"
          stripe
          class="relation-table"
          empty-text="暂无关系 edge"
        >
          <el-table-column label="来源" min-width="150">
            <template #default="{ row }">
              {{ relationNodeLabel(row.source) }}
            </template>
          </el-table-column>
          <el-table-column prop="kind" label="关系" width="140" show-overflow-tooltip />
          <el-table-column label="目标" min-width="150">
            <template #default="{ row }">
              {{ relationNodeLabel(row.target) }}
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" width="100" />
          <el-table-column prop="evidence" label="证据" min-width="220" show-overflow-tooltip />
        </el-table>
      </section>

      <section class="metric-section">
        <div class="section-header">
          <div>
            <h3>指标口径</h3>
            <p class="page-subtitle">示例 SQL 仅作说明，不会执行</p>
          </div>
          <el-button type="primary" plain :disabled="!hasProject" @click="openMetricDialog()">
            <el-icon><Plus /></el-icon>
            新建指标
          </el-button>
        </div>
        <el-table
          v-loading="metricLoading"
          :data="metricDefinitions"
          stripe
          class="metric-table"
          empty-text="暂无指标口径"
        >
          <el-table-column label="metricKey / 名称" min-width="210">
            <template #default="{ row }">
              <div class="metric-key">{{ row.metricKey || '-' }}</div>
              <div class="muted-text">{{ row.displayName || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="definition" label="业务定义" min-width="260" show-overflow-tooltip />
          <el-table-column label="字段引用" min-width="240">
            <template #default="{ row }">
              <div>度量：{{ metricFieldLabels(row.measureFieldIds) }}</div>
              <div class="muted-text">维度：{{ metricFieldLabels(row.dimensionFieldIds) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="aggregationRule" label="聚合" min-width="150" show-overflow-tooltip />
          <el-table-column prop="timeGrain" label="时间粒度" width="120" show-overflow-tooltip />
          <el-table-column prop="exampleSql" label="exampleSql" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openMetricDialog(row)">编辑</el-button>
              <el-button text type="danger" @click="handleDeleteMetric(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

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

        <el-alert
          class="preview-safety"
          title="只读 preview，未应用到数据库，执行前需人工确认"
          type="warning"
          :closable="false"
          show-icon
        />

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

        <div v-if="hasStructureSummary" class="structure-panel">
          <div class="dialect-header">
            <span>结构标准摘要</span>
            <el-tag size="small" type="success">
              {{ structureSummaryItemCount }} 项
            </el-tag>
          </div>
          <div class="structure-grid">
            <div
              v-for="section in structureSections"
              :key="section.key"
              class="structure-section"
            >
              <div class="structure-title">
                <span>{{ section.label }}</span>
                <code>{{ section.key }}</code>
              </div>
              <ul v-if="section.items.length" class="structure-list">
                <li v-for="item in section.items" :key="item">{{ item }}</li>
              </ul>
              <span v-else class="empty-inline">暂无</span>
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

    <el-dialog v-model="metricDialogVisible" :title="editingMetric ? '编辑指标口径' : '新建指标口径'" width="820px">
      <el-form ref="metricFormRef" :model="metricForm" :rules="metricRules" label-width="104px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="metricKey" prop="metricKey">
              <el-input v-model="metricForm.metricKey" placeholder="paid_order_amount" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="展示名称" prop="displayName">
              <el-input v-model="metricForm.displayName" placeholder="已支付订单金额" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="指标定义" prop="definition">
          <el-input v-model="metricForm.definition" type="textarea" :rows="3" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="度量字段">
              <el-select v-model="metricForm.measureFieldIds" multiple filterable class="full-width">
                <el-option
                  v-for="field in standardFields"
                  :key="field.id"
                  :label="fieldOptionLabel(field)"
                  :value="field.id"
                  :disabled="!field.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维度字段">
              <el-select v-model="metricForm.dimensionFieldIds" multiple filterable class="full-width">
                <el-option
                  v-for="field in standardFields"
                  :key="field.id"
                  :label="fieldOptionLabel(field)"
                  :value="field.id"
                  :disabled="!field.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="聚合规则">
              <el-input v-model="metricForm.aggregationRule" placeholder="sum(amount_cent) / 100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时间粒度">
              <el-input v-model="metricForm.timeGrain" placeholder="day / month" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="metricForm.status" class="full-width">
                <el-option label="启用" value="enabled" />
                <el-option label="草稿" value="draft" />
                <el-option label="停用" value="disabled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="过滤规则">
          <el-input v-model="metricForm.filterRule" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="exampleSql">
          <el-input
            v-model="metricForm.exampleSql"
            type="textarea"
            :rows="3"
            placeholder="仅作口径说明，不会执行"
          />
        </el-form-item>
        <el-form-item label="维护说明">
          <el-input v-model="metricForm.ownerNotes" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="证据引用">
          <el-input v-model="metricEvidenceRefsText" placeholder="每行或逗号分隔一个 evidence ref" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metricDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="metricSubmitting" @click="handleSubmitMetric">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CopyDocument, Download, Plus, Refresh, View } from '@element-plus/icons-vue'
import {
  downloadDataDictionaryErd,
  downloadDataDictionaryHtml,
  previewDataDictionaryErd,
  previewDataDictionaryHtml,
  previewDdl
} from '@/api/generator'
import { getBusinessObjectRelationSummary } from '@/api/businessObject'
import { listFields } from '@/api/field'
import {
  createMetricDefinition,
  deleteMetricDefinition,
  listMetricDefinitions,
  updateMetricDefinition
} from '@/api/metricDefinition'
import { listTemplateFields, listTemplates } from '@/api/template'
import { useProjectStore } from '@/stores/project'
import {
  diagnosticLevelLabel,
  diagnosticSummaryTagType,
  diagnosticTagType,
  dialectSummary
} from '@/utils/dialectDiagnostics'
import type {
  BusinessObjectRelationEdge,
  BusinessObjectRelationSummary,
  DdlGenerateResult,
  Field,
  LintIssue,
  MetricDefinitionReq,
  MetricDefinitionResp,
  Template,
  TemplateField
} from '@/types'

const projectStore = useProjectStore()
const route = useRoute()
const templates = ref<Template[]>([])
const templateFields = ref<TemplateField[]>([])
const standardFields = ref<Field[]>([])
const metricDefinitions = ref<MetricDefinitionResp[]>([])
const relationSummary = ref<BusinessObjectRelationSummary | null>(null)
const selectedTemplateId = ref<number | null>(null)
const tableName = ref('')
const result = ref<DdlGenerateResult | null>(null)
const templateLoading = ref(false)
const fieldLoading = ref(false)
const metricLoading = ref(false)
const metricSubmitting = ref(false)
const generating = ref(false)
const dictionaryHtml = ref('')
const dictionaryErd = ref('')
const metricDialogVisible = ref(false)
const editingMetric = ref<MetricDefinitionResp | null>(null)
const metricFormRef = ref<FormInstance>()
const metricEvidenceRefsText = ref('')
const activeDictionaryTab = ref<'html' | 'erd'>('html')
const dictionaryLoading = ref<'html' | 'erd' | ''>('')
const dictionaryDownloading = ref<'html' | 'erd' | ''>('')

const metricForm = ref<MetricDefinitionReq>({
  projectId: 0,
  metricKey: '',
  displayName: '',
  definition: '',
  measureFieldIds: [],
  dimensionFieldIds: [],
  filterRule: '',
  aggregationRule: '',
  timeGrain: '',
  ownerNotes: '',
  exampleSql: '',
  evidenceRefs: [],
  status: 'enabled'
})

const metricRules: FormRules<MetricDefinitionReq> = {
  metricKey: [{ required: true, message: '请输入 metricKey', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入展示名称', trigger: 'blur' }],
  definition: [{ required: true, message: '请输入指标定义', trigger: 'blur' }]
}

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
const relationEdges = computed<BusinessObjectRelationEdge[]>(() => relationSummary.value?.edges ?? [])
const structureSummary = computed(() => result.value?.structureSummary ?? null)
const structureSections = computed(() => [
  {
    key: 'appliedConstraints',
    label: '已应用约束',
    items: structureSummary.value?.appliedConstraints ?? []
  },
  {
    key: 'generatedIndexes',
    label: '生成索引',
    items: structureSummary.value?.generatedIndexes ?? []
  },
  {
    key: 'skippedHints',
    label: '跳过提示',
    items: structureSummary.value?.skippedHints ?? []
  },
  {
    key: 'policyNotes',
    label: '策略说明',
    items: structureSummary.value?.policyNotes ?? []
  },
  {
    key: 'evidence',
    label: '证据',
    items: structureSummary.value?.evidence ?? []
  }
])
const structureSummaryItemCount = computed(() =>
  structureSections.value.reduce((total, section) => total + section.items.length, 0)
)
const hasStructureSummary = computed(() => structureSummaryItemCount.value > 0)

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadTemplates()
    void loadMetricDefinitions()
    void loadStandardFields()
  },
  { immediate: true }
)

watch(selectedTemplateId, () => {
  result.value = null
  void loadTemplateFields()
  void loadRelationSummary()
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
  relationSummary.value = null
  dictionaryHtml.value = ''
  dictionaryErd.value = ''
  activeDictionaryTab.value = 'html'
  if (!projectId) {
    templates.value = []
    relationSummary.value = null
    return
  }
  templateLoading.value = true
  try {
    templates.value = await listTemplates(projectId)
    selectedTemplateId.value = templateIdFromQuery()
      ?? templates.value.find((template) => template.id)?.id
      ?? null
    applyTableNameFromQuery()
    await loadRelationSummary()
  } finally {
    templateLoading.value = false
  }
}

async function loadMetricDefinitions() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    metricDefinitions.value = []
    return
  }
  metricLoading.value = true
  try {
    metricDefinitions.value = await listMetricDefinitions({ projectId })
  } finally {
    metricLoading.value = false
  }
}

async function loadStandardFields() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    standardFields.value = []
    return
  }
  standardFields.value = await listFields(projectId)
}

async function loadRelationSummary() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    relationSummary.value = null
    return
  }
  try {
    relationSummary.value = await getBusinessObjectRelationSummary(projectId)
  } catch {
    relationSummary.value = null
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

function openMetricDialog(metric?: MetricDefinitionResp) {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  editingMetric.value = metric ?? null
  metricForm.value = {
    projectId,
    metricKey: metric?.metricKey ?? '',
    displayName: metric?.displayName ?? '',
    definition: metric?.definition ?? '',
    measureFieldIds: metric?.measureFieldIds ?? [],
    dimensionFieldIds: metric?.dimensionFieldIds ?? [],
    filterRule: metric?.filterRule ?? '',
    aggregationRule: metric?.aggregationRule ?? '',
    timeGrain: metric?.timeGrain ?? '',
    ownerNotes: metric?.ownerNotes ?? '',
    exampleSql: metric?.exampleSql ?? '',
    evidenceRefs: metric?.evidenceRefs ?? [],
    status: metric?.status ?? 'enabled'
  }
  metricEvidenceRefsText.value = (metric?.evidenceRefs ?? []).join('\n')
  metricFormRef.value?.clearValidate()
  metricDialogVisible.value = true
}

async function handleSubmitMetric() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  await metricFormRef.value?.validate()
  metricSubmitting.value = true
  try {
    const payload: MetricDefinitionReq = {
      ...metricForm.value,
      projectId,
      evidenceRefs: splitTextList(metricEvidenceRefsText.value)
    }
    if (editingMetric.value?.id) {
      await updateMetricDefinition(editingMetric.value.id, payload)
      ElMessage.success('指标口径已更新')
    } else {
      await createMetricDefinition(payload)
      ElMessage.success('指标口径已创建')
    }
    metricDialogVisible.value = false
    await loadMetricDefinitions()
  } finally {
    metricSubmitting.value = false
  }
}

async function handleDeleteMetric(metric: MetricDefinitionResp) {
  if (!metric.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除指标口径「${metric.metricKey ?? ''}」吗？`, '删除指标口径', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteMetricDefinition(metric.id)
  ElMessage.success('指标口径已删除')
  await loadMetricDefinitions()
}

function metricFieldLabels(ids?: number[]) {
  if (!ids?.length) {
    return '-'
  }
  return ids.map((id) => standardFields.value.find((field) => field.id === id)?.name ?? `#${id}`).join(', ')
}

function fieldOptionLabel(field: Field) {
  return `${field.name ?? '-'}${field.displayName ? `（${field.displayName}）` : ''}`
}

function splitTextList(value: string) {
  return Array.from(new Set(value
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean)))
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

function relationNodeLabel(nodeId?: string) {
  if (!nodeId) {
    return '未知节点'
  }
  const node = relationSummary.value?.nodes?.find((item) => item.id === nodeId)
  if (!node) {
    return nodeId
  }
  return node.label ? `${node.label}（${node.type || node.id}）` : node.id
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
.issue-table,
.relation-table,
.metric-table {
  width: 100%;
}

.dictionary-section,
.relation-section,
.metric-section,
.result-section {
  margin-top: 20px;
}

.metric-section {
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
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

.metric-key {
  color: #303133;
  font-weight: 600;
  word-break: break-word;
}

.muted-text {
  color: #909399;
  font-size: 12px;
}

.lint-summary {
  margin-top: 10px;
}

.preview-safety {
  margin-bottom: 12px;
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

.structure-panel {
  padding: 10px 12px;
  margin-bottom: 12px;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  background: #f5faff;
}

.structure-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.structure-section {
  min-width: 0;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
}

.structure-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.structure-title code {
  color: #606266;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 12px;
  font-weight: 400;
}

.structure-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.empty-inline {
  color: #909399;
  font-size: 13px;
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

  .structure-grid {
    grid-template-columns: 1fr;
  }
}
</style>
