<template>
  <div class="field-page">
    <div class="page-header">
      <div>
        <h2>标准字段库</h2>
        <p class="page-subtitle">
          {{ projectStore.currentProjectName || '未选择项目' }}
        </p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadFields">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建字段
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div class="field-toolbar">
        <el-input
          v-model="fieldKeyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索字段名、显示名、别名、分类或注释"
        />
        <div class="toolbar-actions">
          <span class="toolbar-count">匹配 {{ filteredFields.length }} / {{ fields.length }}</span>
          <el-button :disabled="selectedFields.length === 0" @click="openBatchDialog">
            批量归组
          </el-button>
        </div>
      </div>

      <div class="field-content">
        <aside class="group-panel">
          <button
            v-for="group in groupOptions"
            :key="group.optionKey"
            class="group-option"
            :class="{ active: activeGroupKey === group.optionKey }"
            type="button"
            @click="selectGroup(group.optionKey)"
          >
            <span class="group-name">{{ group.displayName }}</span>
            <span class="group-meta">{{ group.fieldCount }}</span>
          </button>
        </aside>

        <el-table
          v-loading="loading"
          :data="pagedFields"
          row-key="id"
          stripe
          class="field-table"
          empty-text="暂无标准字段"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="44" />
          <el-table-column prop="name" label="字段名" min-width="150" fixed="left" />
          <el-table-column prop="displayName" label="显示名" min-width="120" />
          <el-table-column label="类型" min-width="150">
            <template #default="{ row }">
              <span>{{ formatDataType(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="分组" min-width="180">
            <template #default="{ row }">
              <div>{{ fieldGroupLabel(row) }}</div>
              <div class="muted-text">{{ row.tags || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="空值" width="86">
            <template #default="{ row }">
              <el-tag :type="row.nullable === false ? 'warning' : 'info'" size="small">
                {{ row.nullable === false ? '非空' : '可空' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="aliases" label="别名" min-width="180" show-overflow-tooltip />
          <el-table-column label="敏感" width="86">
            <template #default="{ row }">
              <el-tag v-if="row.sensitive" type="danger" size="small">是</el-tag>
              <el-tag v-else type="info" size="small">否</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openImpactDialog(row)">影响</el-button>
              <el-button text type="primary" @click="openSourceDialog(row)">来源</el-button>
              <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredFields.length"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <el-dialog v-model="dialogVisible" :title="editingField ? '编辑字段' : '新建字段'" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字段名" prop="name">
              <el-input v-model="form.name" placeholder="mobile_no" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示名">
              <el-input v-model="form.displayName" placeholder="手机号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="数据类型" prop="dataType">
              <el-select v-model="form.dataType" filterable allow-create class="full-width">
                <el-option v-for="type in dataTypeOptions" :key="type" :label="type" :value="type" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="长度">
              <el-input-number v-model="form.length" :min="1" :controls="false" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认值">
              <el-input v-model="form.defaultValue" placeholder="可留空" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="精度">
              <el-input-number
                v-model="form.precisionVal"
                :min="0"
                :controls="false"
                class="full-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="小数位">
              <el-input-number
                v-model="form.scaleVal"
                :min="0"
                :controls="false"
                class="full-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="允许空值">
              <el-switch v-model="form.nullable" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-input v-model="form.category" placeholder="user / order" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" class="full-width">
                <el-option label="启用" value="enabled" />
                <el-option label="停用" value="disabled" />
                <el-option label="废弃" value="deprecated" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="敏感字段">
              <el-switch v-model="form.sensitive" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="数据域">
              <el-select v-model="form.domainId" clearable filterable class="full-width">
                <el-option
                  v-for="domain in domains"
                  :key="domain.id"
                  :label="domainLabel(domain)"
                  :value="domain.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="代码集 ID">
              <el-input-number v-model="form.codeSetId" :min="1" :controls="false" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="示例值">
              <el-input v-model="form.exampleValue" placeholder="13800138000" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="别名">
          <el-input v-model="form.aliases" placeholder="phone,mobile,tel,user_phone" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="用户,联系方式" />
        </el-form-item>
        <el-form-item label="字段注释">
          <el-input v-model="form.comment" type="textarea" :rows="3" placeholder="请输入字段注释" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量归组" width="560px">
      <el-form label-width="96px">
        <el-form-item label="已选字段">
          <span>{{ selectedFields.length }} 个</span>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="batchForm.applyDomain">数据域</el-checkbox>
          <el-select
            v-model="batchForm.domainId"
            clearable
            filterable
            class="batch-input"
            placeholder="选择数据域；留空可清空"
          >
            <el-option
              v-for="domain in domains"
              :key="domain.id"
              :label="domainLabel(domain)"
              :value="domain.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="batchForm.applyCategory">分类</el-checkbox>
          <el-input v-model="batchForm.category" class="batch-input" placeholder="contact / order；留空可清空" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="batchForm.applyTags">标签</el-checkbox>
          <el-input v-model="batchForm.tags" class="batch-input" placeholder="pii,customer；留空可清空" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">
          保存归组
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sourceDialogVisible" :title="sourceDialogTitle" width="820px">
      <el-table
        v-loading="sourceLoading"
        :data="fieldSources"
        stripe
        empty-text="暂无来源记录"
      >
        <el-table-column label="导入时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.batch?.createdAt ?? row.source?.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="来源库" min-width="180">
          <template #default="{ row }">
            <div>{{ row.batch?.databaseType || '-' }}</div>
            <div class="muted-text">{{ sourceDatabaseLabel(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="来源字段" min-width="180">
          <template #default="{ row }">
            <div>{{ sourceColumnLabel(row) }}</div>
            <div class="muted-text">{{ row.source?.dataType || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="批次统计" min-width="120">
          <template #default="{ row }">
            <span>新增 {{ row.batch?.importedCount ?? 0 }} / 跳过 {{ row.batch?.skippedCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="source.comment" label="原注释" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="impactDialogVisible" :title="impactDialogTitle" width="860px">
      <el-skeleton v-if="impactLoading" :rows="5" animated />
      <template v-else-if="impactReport">
        <el-alert
          v-if="impactReport.editWarnings?.length"
          type="warning"
          show-icon
          :closable="false"
          class="impact-alert"
          :title="`关注关键属性：${warningSummaryText(impactReport.editWarnings)}`"
        />
        <div class="impact-summary">
          <div class="impact-metric">
            <span>总影响</span>
            <strong>{{ impactReport.summary?.totalImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>模板</span>
            <strong>{{ impactReport.summary?.templateImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>导入来源</span>
            <strong>{{ impactReport.summary?.importSourceImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>SQL</span>
            <strong>{{ impactReport.summary?.sqlCheckImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>快照</span>
            <strong>{{ impactReport.summary?.snapshotImpactCount ?? 0 }}</strong>
          </div>
        </div>
        <el-table :data="impactReport.impacts ?? []" stripe empty-text="暂无已知影响">
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="impactSeverityTagType(row.severity)">
                {{ impactTypeLabel(row.impactType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.sourceName || '-' }}</template>
          </el-table-column>
          <el-table-column label="数量" width="82">
            <template #default="{ row }">{{ row.count ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="说明" min-width="320" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.description || '-' }}</span>
              <el-tag v-if="row.possibleReference" class="possible-tag" size="small" type="info">疑似</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listDomains } from '@/api/domain'
import {
  batchUpdateFieldGrouping,
  createField,
  deleteField,
  getField,
  getFieldGroupSummary,
  getFieldImpactReport,
  listFields,
  listFieldSources,
  updateField
} from '@/api/field'
import { useProjectStore } from '@/stores/project'
import {
  criticalFieldChanged,
  fieldImpactSummaryText,
  impactSeverityTagType,
  impactTypeLabel,
  warningSummaryText
} from '@/utils/fieldImpactDisplay'
import type {
  Domain,
  Field,
  FieldGroupItem,
  FieldGroupingBatchUpdateReq,
  FieldGroupSummary,
  FieldImpactReport,
  FieldReq,
  FieldSourceDetail
} from '@/types'

const projectStore = useProjectStore()
const route = useRoute()
const fields = ref<Field[]>([])
const groupSummary = ref<FieldGroupSummary | null>(null)
const domains = ref<Domain[]>([])
const fieldKeyword = ref('')
const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const sourceDialogVisible = ref(false)
const impactDialogVisible = ref(false)
const editingField = ref<Field | null>(null)
const sourceField = ref<Field | null>(null)
const impactField = ref<Field | null>(null)
const fieldSources = ref<FieldSourceDetail[]>([])
const sourceLoading = ref(false)
const impactLoading = ref(false)
const impactReport = ref<FieldImpactReport | null>(null)
const formRef = ref<FormInstance>()
const openedRouteFieldId = ref<number | null>(null)
const selectedFields = ref<Field[]>([])
const activeGroupKey = ref('all')

const pagination = reactive({
  current: 1,
  size: 20
})

const batchForm = reactive({
  applyDomain: true,
  applyCategory: true,
  applyTags: true,
  domainId: undefined as number | undefined,
  category: '',
  tags: ''
})

const dataTypeOptions = [
  'bigint',
  'integer',
  'varchar',
  'text',
  'boolean',
  'timestamp',
  'timestamptz',
  'numeric',
  'decimal',
  'jsonb'
]

const form = reactive<FieldReq>({
  projectId: 0,
  name: '',
  displayName: '',
  dataType: 'varchar',
  length: undefined,
  precisionVal: undefined,
  scaleVal: undefined,
  nullable: true,
  defaultValue: '',
  comment: '',
  domainId: undefined,
  tags: '',
  aliases: '',
  category: '',
  codeSetId: undefined,
  sensitive: false,
  status: 'enabled',
  exampleValue: ''
})

const rules: FormRules<FieldReq> = {
  name: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  dataType: [{ required: true, message: '请选择数据类型', trigger: 'change' }]
}

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const sourceDialogTitle = computed(() =>
  sourceField.value?.name ? `字段来源：${sourceField.value.name}` : '字段来源'
)
const impactDialogTitle = computed(() =>
  impactField.value?.name ? `字段影响：${impactField.value.name}` : '字段影响'
)
const groupOptions = computed(() => [
  {
    optionKey: 'all',
    displayName: '全部字段',
    fieldCount: groupSummary.value?.totalFieldCount ?? fields.value.length
  },
  ...(groupSummary.value?.groups ?? []).map((group) => ({
    optionKey: groupOptionKey(group),
    displayName: groupDisplayName(group),
    fieldCount: group.fieldCount ?? 0
  }))
])
const filteredFields = computed(() => {
  const keyword = fieldKeyword.value.trim().toLowerCase()
  return fields.value.filter((field) =>
    matchesActiveGroup(field) && (!keyword || [
        field.name,
        field.displayName,
        field.aliases,
        field.category,
        field.tags,
        field.comment,
        field.dataType,
        fieldGroupLabel(field)
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword)))
  )
})
const pagedFields = computed(() => {
  const start = (pagination.current - 1) * pagination.size
  return filteredFields.value.slice(start, start + pagination.size)
})

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    pagination.current = 1
    openedRouteFieldId.value = null
    activeGroupKey.value = 'all'
    selectedFields.value = []
    void loadFields()
  },
  { immediate: true }
)

watch(
  () => route.query.keyword,
  (keyword) => {
    fieldKeyword.value = routeKeyword(keyword)
  },
  { immediate: true }
)

watch([fieldKeyword, activeGroupKey], () => {
  pagination.current = 1
  selectedFields.value = []
})

watch(
  () => route.query.fieldId,
  () => {
    openedRouteFieldId.value = null
    void openFieldFromRoute()
  }
)

async function loadFields() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    fields.value = []
    domains.value = []
    groupSummary.value = null
    return
  }
  loading.value = true
  try {
    const [fieldList, summary, domainList] = await Promise.all([
      listFields(projectId),
      getFieldGroupSummary(projectId),
      listDomains(projectId)
    ])
    fields.value = fieldList ?? []
    groupSummary.value = summary
    domains.value = domainList ?? []
    selectedFields.value = []
    ensureActiveGroupExists()
    await openFieldFromRoute()
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.current = 1
}

function handlePageChange(page: number) {
  pagination.current = page
}

function selectGroup(optionKey: string) {
  activeGroupKey.value = optionKey
}

function ensureActiveGroupExists() {
  if (!groupOptions.value.some((group) => group.optionKey === activeGroupKey.value)) {
    activeGroupKey.value = 'all'
  }
}

function handleSelectionChange(selection: Field[]) {
  selectedFields.value = selection
}

function openBatchDialog() {
  if (selectedFields.value.length === 0) {
    ElMessage.warning('请先选择字段')
    return
  }
  batchForm.applyDomain = true
  batchForm.applyCategory = true
  batchForm.applyTags = true
  batchForm.domainId = undefined
  batchForm.category = ''
  batchForm.tags = ''
  batchDialogVisible.value = true
}

async function handleBatchSubmit() {
  const projectId = projectStore.currentProjectId
  if (!projectId || selectedFields.value.length === 0) {
    return
  }
  const fieldIds = selectedFields.value
    .map((field) => Number(field.id))
    .filter((id) => Number.isFinite(id) && id > 0)
  if (fieldIds.length === 0) {
    ElMessage.warning('所选字段缺少有效 ID，请刷新后重试')
    return
  }
  const updates: NonNullable<FieldGroupingBatchUpdateReq['updates']> = {}
  if (batchForm.applyDomain) {
    updates.domainId = batchForm.domainId ?? ''
  }
  if (batchForm.applyCategory) {
    updates.category = batchForm.category
  }
  if (batchForm.applyTags) {
    updates.tags = batchForm.tags
  }
  if (Object.keys(updates).length === 0) {
    ElMessage.warning('请选择要更新的归组字段')
    return
  }
  try {
    await ElMessageBox.confirm(`确定更新 ${selectedFields.value.length} 个字段的归组信息吗？`, '批量归组', {
      type: 'warning',
      confirmButtonText: '更新',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  batchSubmitting.value = true
  try {
    const result = await batchUpdateFieldGrouping({
      projectId,
      fieldIds,
      updates
    })
    ElMessage.success(`已更新 ${result.updatedCount ?? selectedFields.value.length} 个字段`)
    batchDialogVisible.value = false
    selectedFields.value = []
    await loadFields()
  } finally {
    batchSubmitting.value = false
  }
}

function resetForm(field?: Field) {
  form.projectId = projectStore.currentProjectId ?? field?.projectId ?? 0
  form.name = field?.name ?? ''
  form.displayName = field?.displayName ?? ''
  form.dataType = field?.dataType ?? 'varchar'
  form.length = field?.length
  form.precisionVal = field?.precisionVal
  form.scaleVal = field?.scaleVal
  form.nullable = field?.nullable ?? true
  form.defaultValue = field?.defaultValue ?? ''
  form.comment = field?.comment ?? ''
  form.domainId = field?.domainId
  form.tags = field?.tags ?? ''
  form.aliases = field?.aliases ?? ''
  form.category = field?.category ?? ''
  form.codeSetId = field?.codeSetId
  form.sensitive = field?.sensitive ?? false
  form.status = field?.status ?? 'enabled'
  form.exampleValue = field?.exampleValue ?? ''
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  editingField.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(field: Field) {
  editingField.value = field
  resetForm(field)
  dialogVisible.value = true
}

async function openFieldFromRoute() {
  const fieldId = routeFieldId(route.query.fieldId)
  const projectId = projectStore.currentProjectId
  if (!fieldId || !projectId || openedRouteFieldId.value === fieldId) {
    return
  }
  let field = fields.value.find((item) => item.id === fieldId)
  if (!field) {
    try {
      field = await getField(fieldId)
    } catch {
      return
    }
  }
  if (field?.projectId !== projectId) {
    return
  }
  openedRouteFieldId.value = fieldId
  openEditDialog(field)
}

async function openSourceDialog(field: Field) {
  if (!field.id) {
    return
  }
  sourceField.value = field
  sourceDialogVisible.value = true
  sourceLoading.value = true
  try {
    fieldSources.value = await listFieldSources(field.id)
  } finally {
    sourceLoading.value = false
  }
}

async function openImpactDialog(field: Field) {
  if (!field.id || !projectStore.currentProjectId) {
    return
  }
  impactField.value = field
  impactReport.value = null
  impactDialogVisible.value = true
  impactLoading.value = true
  try {
    impactReport.value = await getFieldImpactReport(field.id, projectStore.currentProjectId)
  } finally {
    impactLoading.value = false
  }
}

async function handleSubmit() {
  if (!projectStore.currentProjectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  await formRef.value?.validate()
  if (!(await confirmImpactBeforeSave())) {
    return
  }
  submitting.value = true
  try {
    const payload: FieldReq = {
      ...form,
      projectId: projectStore.currentProjectId
    }
    if (editingField.value?.id) {
      await updateField(editingField.value.id, payload)
      ElMessage.success('字段已更新')
    } else {
      await createField(payload)
      ElMessage.success('字段已创建')
    }
    dialogVisible.value = false
    await loadFields()
  } finally {
    submitting.value = false
  }
}

async function confirmImpactBeforeSave() {
  const projectId = projectStore.currentProjectId
  const field = editingField.value
  if (!projectId || !field?.id || !criticalFieldChanged(fieldCriticalValue(field), fieldCriticalValue(form))) {
    return true
  }
  let report: FieldImpactReport
  try {
    report = await getFieldImpactReport(field.id, projectId)
  } catch {
    ElMessage.warning('影响分析暂不可用，已继续保存')
    return true
  }
  if (!report.editWarnings?.length) {
    return true
  }
  const warningText = warningSummaryText(report.editWarnings)
  const summaryText = fieldImpactSummaryText(report.summary)
  try {
    await ElMessageBox.confirm(
      `${summaryText}。将修改：${warningText}。`,
      '影响提示',
      {
        type: 'warning',
        confirmButtonText: '继续保存',
        cancelButtonText: '返回编辑'
      }
    )
    return true
  } catch {
    return false
  }
}

function fieldCriticalValue(field: Field | FieldReq) {
  return {
    name: field.name,
    dataType: field.dataType,
    status: field.status,
    codeSetId: field.codeSetId,
    sensitive: field.sensitive
  }
}

async function handleDelete(field: Field) {
  if (!field.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除字段「${field.name ?? ''}」吗？`, '删除字段', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteField(field.id)
  ElMessage.success('字段已删除')
  await loadFields()
}

function groupOptionKey(group: FieldGroupItem) {
  return `${group.groupType ?? 'unknown'}:${group.groupKey ?? ''}`
}

function groupDisplayName(group: FieldGroupItem) {
  if (group.groupType === 'ungrouped') {
    return '未分组'
  }
  if (group.groupType === 'domain') {
    return `数据域：${domainNameById(group.groupKey)}`
  }
  if (group.groupType === 'category') {
    return `分类：${group.groupName || group.groupKey || '-'}`
  }
  if (group.groupType === 'tag') {
    return `标签：${group.groupName || group.groupKey || '-'}`
  }
  return group.groupName || group.groupKey || '其他'
}

function matchesActiveGroup(field: Field) {
  if (activeGroupKey.value === 'all') {
    return true
  }
  const [groupType, groupKey] = activeGroupKey.value.split(':')
  if (groupType === 'domain') {
    return String(field.domainId ?? '') === groupKey
  }
  if (groupType === 'category') {
    return (field.category ?? '').trim() === groupKey
  }
  if (groupType === 'tag') {
    return splitTags(field.tags).includes(groupKey)
  }
  if (groupType === 'ungrouped') {
    return isUngrouped(field)
  }
  return true
}

function fieldGroupLabel(field: Field) {
  const parts = []
  if (field.domainId) {
    parts.push(domainNameById(String(field.domainId)))
  }
  if (field.category) {
    parts.push(field.category)
  }
  return parts.length > 0 ? parts.join(' / ') : '未分组'
}

function domainNameById(value?: string) {
  const domain = domains.value.find((item) => String(item.id) === String(value ?? ''))
  return domain ? domainLabel(domain) : value || '-'
}

function domainLabel(domain: Domain) {
  const name = domain.name || `#${domain.id ?? '-'}`
  return domain.code ? `${name} (${domain.code})` : name
}

function splitTags(value?: string) {
  return Array.from(new Set((value ?? '')
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)))
    .sort()
}

function isUngrouped(field: Field) {
  return !field.domainId && !field.category?.trim() && splitTags(field.tags).length === 0
}

function formatDataType(field: Field) {
  if (!field.dataType) {
    return '-'
  }
  if (field.precisionVal !== undefined && field.precisionVal !== null) {
    const scale = field.scaleVal !== undefined && field.scaleVal !== null ? `,${field.scaleVal}` : ''
    return `${field.dataType}(${field.precisionVal}${scale})`
  }
  return field.length ? `${field.dataType}(${field.length})` : field.dataType
}

function statusText(status?: string) {
  if (status === 'disabled') {
    return '停用'
  }
  if (status === 'deprecated') {
    return '废弃'
  }
  return '启用'
}

function statusTagType(status?: string) {
  if (status === 'disabled') {
    return 'info'
  }
  if (status === 'deprecated') {
    return 'warning'
  }
  return 'success'
}

function sourceDatabaseLabel(row: FieldSourceDetail) {
  const parts = [row.batch?.databaseName, row.batch?.schemaName].filter(Boolean)
  return parts.length > 0 ? parts.join(' / ') : '-'
}

function sourceColumnLabel(row: FieldSourceDetail) {
  const parts = [row.source?.tableName, row.source?.columnName].filter(Boolean)
  return parts.length > 0 ? parts.join('.') : '-'
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

function routeKeyword(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return typeof value === 'string' ? value : ''
}

function routeFieldId(value: unknown) {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (typeof rawValue !== 'string' || rawValue.trim() === '') {
    return null
  }
  const id = Number(rawValue)
  return Number.isInteger(id) && id > 0 ? id : null
}
</script>

<style scoped>
.field-page {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 140px);
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

.field-table {
  width: 100%;
}

.field-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 420px) 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.toolbar-count {
  color: #6b7280;
  font-size: 13px;
}

.field-content {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.group-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 560px;
  overflow: auto;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
}

.group-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 34px;
  padding: 6px 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #303133;
  text-align: left;
  cursor: pointer;
}

.group-option:hover,
.group-option.active {
  background: #eef5ff;
  color: #1d4ed8;
}

.group-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-meta {
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.full-width {
  width: 100%;
}

.batch-input {
  width: 320px;
  margin-left: 12px;
}

.muted-text {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

@media (max-width: 640px) {
  .field-toolbar {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .field-content {
    grid-template-columns: 1fr;
  }

  .group-panel {
    max-height: 220px;
    border-right: 0;
    border-bottom: 1px solid #ebeef5;
    padding-right: 0;
    padding-bottom: 10px;
  }

  .batch-input {
    width: 100%;
    margin-left: 0;
    margin-top: 8px;
  }
}
</style>
