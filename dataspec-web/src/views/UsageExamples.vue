<template>
  <div class="usage-examples-page">
    <div class="page-header">
      <div>
        <h2>示例与反例库</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建示例
        </el-button>
      </div>
    </div>

    <ProjectRequired
      v-if="!hasProject"
      :has-project="hasProject"
      title="请先创建并选择项目"
      @action="$router.push('/projects')"
    />

    <template v-else>
      <div class="filter-bar">
        <el-input
          v-model="filters.query"
          clearable
          placeholder="搜索输入、输出、反模式、原因或标签"
          class="keyword-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filters.scope" clearable placeholder="范围" class="filter-select">
          <el-option label="字段" value="FIELD" />
          <el-option label="规则" value="RULE" />
          <el-option label="模板" value="TEMPLATE" />
          <el-option label="通用" value="GENERAL" />
        </el-select>
        <el-select v-model="filters.exampleType" clearable placeholder="类型" class="filter-select">
          <el-option label="正例" value="GOOD" />
          <el-option label="反例" value="BAD" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="状态" class="filter-select">
          <el-option label="启用" value="enabled" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        <el-button type="primary" plain @click="handleSearch">查询</el-button>
      </div>

      <el-alert
        class="guardrail-alert"
        type="info"
        show-icon
        :closable="false"
        title="示例会进入 AI Context，请使用脱敏样例，不要填写真实 token、密码、JDBC URL 或业务数据行。"
      />

      <el-table
        v-loading="loading"
        :data="examples"
        stripe
        class="usage-table"
        empty-text="暂无示例或反例"
      >
        <el-table-column label="类型" width="92" fixed="left">
          <template #default="{ row }">
            <el-tag :type="row.exampleType === 'BAD' ? 'danger' : 'success'" size="small" effect="plain">
              {{ exampleTypeLabel(row.exampleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="范围 / 目标" min-width="190">
          <template #default="{ row }">
            <div class="target-cell">
              <el-tag size="small">{{ scopeLabel(row.scope) }}</el-tag>
              <span>{{ targetLabel(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="input" label="输入/场景" min-width="230" show-overflow-tooltip />
        <el-table-column label="输出/反模式" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.exampleType === 'BAD' ? row.antiPattern : row.expectedOutput || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag
                v-for="tag in splitTags(row.tags)"
                :key="tag"
                size="small"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
              <span v-if="splitTags(row.tags).length === 0">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === 'disabled' ? 'info' : 'success'" size="small">
              {{ row.status === 'disabled' ? '禁用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="178" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text :type="row.status === 'disabled' ? 'success' : 'warning'" @click="toggleStatus(row)">
              {{ row.status === 'disabled' ? '启用' : '禁用' }}
            </el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          layout="total, sizes, prev, pager, next"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </template>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑示例' : '新建示例'" width="820px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="116px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="范围" prop="scope">
              <el-select v-model="form.scope" class="full-width" @change="handleScopeChange">
                <el-option label="字段" value="FIELD" />
                <el-option label="规则" value="RULE" />
                <el-option label="模板" value="TEMPLATE" />
                <el-option label="通用" value="GENERAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型" prop="exampleType">
              <el-select v-model="form.exampleType" class="full-width">
                <el-option label="正例" value="GOOD" />
                <el-option label="反例" value="BAD" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" class="full-width">
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item v-if="form.scope === 'FIELD'" label="字段" prop="fieldId">
              <el-select v-model="form.fieldId" filterable clearable class="full-width">
                <el-option
                  v-for="field in fields"
                  :key="field.id"
                  :label="`${field.name || ''}${field.displayName ? `｜${field.displayName}` : ''}`"
                  :value="field.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-else-if="form.scope === 'RULE'" label="规则" prop="ruleCode">
              <el-select v-model="form.ruleCode" filterable clearable class="full-width">
                <el-option
                  v-for="rule in rules"
                  :key="rule.ruleCode"
                  :label="`${rule.ruleCode || ''}${rule.ruleName ? `｜${rule.ruleName}` : ''}`"
                  :value="rule.ruleCode"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-else-if="form.scope === 'TEMPLATE'" label="模板" prop="templateId">
              <el-select v-model="form.templateId" filterable clearable class="full-width">
                <el-option
                  v-for="template in templates"
                  :key="template.id"
                  :label="template.name"
                  :value="template.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="目标">
              <el-input model-value="通用示例" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="0" :max="100" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="输入/场景" prop="input">
          <el-input v-model="form.input" type="textarea" :rows="3" maxlength="4000" show-word-limit />
        </el-form-item>
        <el-form-item v-if="form.exampleType === 'GOOD'" label="期望输出" prop="expectedOutput">
          <el-input v-model="form.expectedOutput" type="textarea" :rows="3" maxlength="4000" show-word-limit />
        </el-form-item>
        <el-form-item v-else label="反模式" prop="antiPattern">
          <el-input v-model="form.antiPattern" type="textarea" :rows="3" maxlength="4000" show-word-limit />
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="4000" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" maxlength="500" show-word-limit placeholder="ddl,phone,legacy" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules
} from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import ProjectRequired from '@/components/ProjectRequired.vue'
import { listFields } from '@/api/field'
import { listRuleConfigs } from '@/api/rule'
import { listTemplates } from '@/api/template'
import {
  createUsageExample,
  deleteUsageExample,
  listUsageExamples,
  updateUsageExample
} from '@/api/usageExample'
import { useProjectStore } from '@/stores/project'
import type {
  Field,
  RuleConfig,
  StandardUsageExample,
  StandardUsageExampleSaveReq,
  Template
} from '@/types'

const projectStore = useProjectStore()
const examples = ref<StandardUsageExample[]>([])
const fields = ref<Field[]>([])
const rules = ref<RuleConfig[]>([])
const templates = ref<Template[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const filters = reactive({
  query: '',
  scope: '',
  exampleType: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const form = reactive<StandardUsageExampleSaveReq>({
  projectId: 0,
  fieldId: undefined,
  ruleCode: undefined,
  templateId: undefined,
  scope: 'FIELD',
  exampleType: 'GOOD',
  input: '',
  expectedOutput: '',
  antiPattern: '',
  reason: '',
  tags: '',
  priority: 50,
  status: 'enabled'
})

const formRules: FormRules<StandardUsageExampleSaveReq> = {
  scope: [{ required: true, message: '请选择范围', trigger: 'change' }],
  exampleType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  fieldId: [{ required: true, message: '请选择字段', trigger: 'change' }],
  ruleCode: [{ required: true, message: '请选择规则', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }],
  input: [{ required: true, message: '请输入场景或输入', trigger: 'blur' }],
  expectedOutput: [{ required: true, message: '请输入期望输出', trigger: 'blur' }],
  antiPattern: [{ required: true, message: '请输入反模式', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入原因', trigger: 'blur' }]
}

const hasProject = computed(() => Boolean(projectStore.currentProjectId))

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    pagination.current = 1
    void loadData()
  },
  { immediate: true }
)

async function loadData() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    examples.value = []
    fields.value = []
    rules.value = []
    templates.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const [page, fieldList, ruleList, templateList] = await Promise.all([
      listUsageExamples({
        projectId,
        query: filters.query || undefined,
        scope: filters.scope || undefined,
        exampleType: filters.exampleType || undefined,
        status: filters.status || undefined,
        current: pagination.current,
        size: pagination.size
      }),
      listFields(projectId),
      listRuleConfigs(projectId),
      listTemplates(projectId)
    ])
    examples.value = page.records ?? []
    pagination.total = page.total ?? 0
    fields.value = fieldList
    rules.value = ruleList
    templates.value = templateList
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  void loadData()
}

function handleSizeChange() {
  pagination.current = 1
  void loadData()
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: StandardUsageExample) {
  editingId.value = row.id ?? null
  resetForm(row)
  dialogVisible.value = true
}

function handleScopeChange() {
  form.fieldId = undefined
  form.ruleCode = undefined
  form.templateId = undefined
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  await formRef.value?.validate()
  const payload = normalizePayload({ ...form, projectId })
  submitting.value = true
  try {
    if (editingId.value) {
      await updateUsageExample(editingId.value, payload)
      ElMessage.success('示例已更新')
    } else {
      await createUsageExample(payload)
      ElMessage.success('示例已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: StandardUsageExample) {
  if (!row.id || !projectStore.currentProjectId) {
    return
  }
  const nextStatus = row.status === 'disabled' ? 'enabled' : 'disabled'
  await updateUsageExample(row.id, normalizePayload({
    ...row,
    projectId: projectStore.currentProjectId,
    status: nextStatus
  }))
  ElMessage.success(nextStatus === 'enabled' ? '示例已启用' : '示例已禁用')
  await loadData()
}

async function handleDelete(row: StandardUsageExample) {
  if (!row.id || !projectStore.currentProjectId) {
    return
  }
  try {
    await ElMessageBox.confirm('删除后，该示例不再进入 AI Context 导出。', '删除示例', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteUsageExample(projectStore.currentProjectId, row.id)
  ElMessage.success('示例已删除')
  await loadData()
}

function resetForm(row?: StandardUsageExample) {
  form.projectId = projectStore.currentProjectId ?? 0
  form.fieldId = row?.fieldId ?? undefined
  form.ruleCode = row?.ruleCode ?? undefined
  form.templateId = row?.templateId ?? undefined
  form.scope = row?.scope ?? 'FIELD'
  form.exampleType = row?.exampleType ?? 'GOOD'
  form.input = row?.input ?? ''
  form.expectedOutput = row?.expectedOutput ?? ''
  form.antiPattern = row?.antiPattern ?? ''
  form.reason = row?.reason ?? ''
  form.tags = row?.tags ?? ''
  form.priority = row?.priority ?? 50
  form.status = row?.status ?? 'enabled'
  formRef.value?.clearValidate()
}

function normalizePayload(payload: StandardUsageExampleSaveReq): StandardUsageExampleSaveReq {
  const exampleType = payload.exampleType || 'GOOD'
  const scope = payload.scope || 'FIELD'
  return {
    projectId: payload.projectId,
    fieldId: scope === 'FIELD' ? payload.fieldId : undefined,
    ruleCode: scope === 'RULE' ? emptyToUndefined(payload.ruleCode) : undefined,
    templateId: scope === 'TEMPLATE' ? payload.templateId : undefined,
    scope,
    exampleType,
    input: emptyToUndefined(payload.input),
    expectedOutput: exampleType === 'GOOD' ? emptyToUndefined(payload.expectedOutput) : undefined,
    antiPattern: exampleType === 'BAD' ? emptyToUndefined(payload.antiPattern) : undefined,
    reason: emptyToUndefined(payload.reason),
    tags: emptyToUndefined(payload.tags),
    priority: payload.priority ?? 50,
    status: payload.status || 'enabled'
  }
}

function emptyToUndefined(value?: string | null) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

function splitTags(value?: string | null) {
  return value?.split(',').map((item) => item.trim()).filter(Boolean) ?? []
}

function exampleTypeLabel(value?: string) {
  return value === 'BAD' ? '反例' : '正例'
}

function scopeLabel(value?: string) {
  const labelMap: Record<string, string> = {
    FIELD: '字段',
    RULE: '规则',
    TEMPLATE: '模板',
    GENERAL: '通用'
  }
  return labelMap[value || ''] || value || '-'
}

function targetLabel(row: StandardUsageExample) {
  if (row.scope === 'FIELD') {
    return fieldLabel(row.fieldId)
  }
  if (row.scope === 'RULE') {
    return row.ruleCode || '-'
  }
  if (row.scope === 'TEMPLATE') {
    return templateLabel(row.templateId)
  }
  return '全部任务'
}

function fieldLabel(id?: number | null) {
  if (!id) {
    return '-'
  }
  const field = fields.value.find((item) => item.id === id)
  return field?.name || `#${id}`
}

function templateLabel(id?: number | null) {
  if (!id) {
    return '-'
  }
  const template = templates.value.find((item) => item.id === id)
  return template?.name || `#${id}`
}
</script>

<style scoped>
.usage-examples-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.filter-bar,
.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.keyword-input {
  width: 360px;
}

.filter-select {
  width: 130px;
}

.guardrail-alert {
  max-width: 900px;
}

.usage-table {
  width: 100%;
}

.target-cell,
.tag-list {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.tag-list {
  flex-wrap: wrap;
}

.pagination-row {
  justify-content: flex-end;
}

.full-width {
  width: 100%;
}
</style>
