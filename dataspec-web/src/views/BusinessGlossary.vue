<template>
  <div class="glossary-page">
    <div class="page-header">
      <div>
        <h2>业务术语表</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建术语
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索术语、同义词、词根或示例字段"
          class="keyword-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filters.status" clearable placeholder="状态" class="status-select">
          <el-option label="启用" value="enabled" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        <el-button type="primary" plain @click="handleSearch">查询</el-button>
      </div>

      <el-alert
        v-if="conflictCount > 0"
        class="conflict-alert"
        type="warning"
        show-icon
        :closable="false"
        :title="`发现 ${conflictCount} 个术语冲突，其中 ${errorCount} 个需要优先处理`"
      />

      <el-table
        v-loading="loading"
        :data="glossary"
        stripe
        class="glossary-table"
        empty-text="暂无业务术语"
      >
        <el-table-column prop="term" label="术语" min-width="130" fixed="left" />
        <el-table-column prop="synonyms" label="同义词" min-width="180" show-overflow-tooltip />
        <el-table-column prop="rootTerms" label="词根" min-width="160" show-overflow-tooltip />
        <el-table-column prop="abbreviations" label="缩写" min-width="130" show-overflow-tooltip />
        <el-table-column prop="disabledTerms" label="禁用词" min-width="150" show-overflow-tooltip />
        <el-table-column label="Canonical 字段" min-width="160">
          <template #default="{ row }">
            {{ canonicalFieldName(row.canonicalFieldId) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="范围" width="150">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ scopeLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'disabled' ? 'info' : 'success'" size="small">
              {{ row.status === 'disabled' ? '禁用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="210" fixed="right">
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

      <el-collapse v-if="conflicts.length > 0" class="conflict-collapse">
        <el-collapse-item name="conflicts">
          <template #title>
            <span class="collapse-title">
              <el-icon><Warning /></el-icon>
              术语冲突
            </span>
          </template>
          <el-table :data="conflicts" size="small" border>
            <el-table-column prop="severity" label="级别" width="100" />
            <el-table-column prop="type" label="类型" width="190" />
            <el-table-column prop="token" label="冲突词" width="150" />
            <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
            <el-table-column label="涉及术语" min-width="220">
              <template #default="{ row }">
                {{ conflictTerms(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="nextAction" label="下一步" min-width="240" show-overflow-tooltip />
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </template>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑业务术语' : '新建业务术语'" width="760px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="116px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="术语" prop="term">
              <el-input v-model="form.term" maxlength="120" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" class="full-width">
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="同义词">
          <el-input v-model="form.synonyms" placeholder="用户,账号,客户" />
        </el-form-item>
        <el-form-item label="英文词根">
          <el-input v-model="form.rootTerms" placeholder="user,member,account" />
        </el-form-item>
        <el-form-item label="缩写">
          <el-input v-model="form.abbreviations" placeholder="yh,hy,mobile" />
        </el-form-item>
        <el-form-item label="禁用词">
          <el-input v-model="form.disabledTerms" placeholder="不推荐继续使用的历史叫法" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Canonical 字段">
              <el-select v-model="form.canonicalFieldId" filterable clearable class="full-width">
                <el-option
                  v-for="field in fields"
                  :key="field.id"
                  :label="`${field.name || ''}${field.displayName ? `｜${field.displayName}` : ''}`"
                  :value="field.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="示例字段">
              <el-input v-model="form.exampleFields" placeholder="user_id,mobile_no" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="范围类型">
              <el-select v-model="form.scopeType" class="full-width">
                <el-option label="全局" value="GLOBAL" />
                <el-option label="分类" value="CATEGORY" />
                <el-option label="数据域" value="DOMAIN" />
                <el-option label="标签" value="TAG" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="范围值">
              <el-input v-model="form.scopeValue" placeholder="contact / money / domain id / tag" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
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
import { Plus, Refresh, Search, Warning } from '@element-plus/icons-vue'
import {
  createBusinessGlossary,
  deleteBusinessGlossary,
  getBusinessGlossaryConflicts,
  listBusinessGlossary,
  updateBusinessGlossary
} from '@/api/glossary'
import { listFields } from '@/api/field'
import { useProjectStore } from '@/stores/project'
import type {
  BusinessGlossary,
  BusinessGlossaryConflictGroup,
  BusinessGlossaryReq,
  Field
} from '@/types'

const projectStore = useProjectStore()
const glossary = ref<BusinessGlossary[]>([])
const conflicts = ref<BusinessGlossaryConflictGroup[]>([])
const fields = ref<Field[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const filters = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const form = reactive<BusinessGlossaryReq>({
  projectId: 0,
  term: '',
  synonyms: null,
  rootTerms: null,
  abbreviations: null,
  disabledTerms: null,
  canonicalFieldId: null,
  scopeType: 'GLOBAL',
  scopeValue: null,
  exampleFields: null,
  description: null,
  status: 'enabled'
})

const formRules: FormRules<BusinessGlossaryReq> = {
  term: [{ required: true, message: '请输入术语', trigger: 'blur' }]
}

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const conflictCount = computed(() => conflicts.value.length)
const errorCount = computed(() => conflicts.value.filter((item) => item.severity === 'ERROR').length)

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
    glossary.value = []
    conflicts.value = []
    fields.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const [page, conflictReport, fieldList] = await Promise.all([
      listBusinessGlossary({
        projectId,
        keyword: filters.keyword || undefined,
        status: filters.status || undefined,
        current: pagination.current,
        size: pagination.size
      }),
      getBusinessGlossaryConflicts(projectId),
      listFields(projectId)
    ])
    glossary.value = page.records ?? []
    pagination.total = page.total ?? 0
    conflicts.value = conflictReport.conflicts ?? []
    fields.value = fieldList
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

function openEditDialog(row: BusinessGlossary) {
  editingId.value = row.id ?? null
  resetForm(row)
  dialogVisible.value = true
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
      await updateBusinessGlossary(editingId.value, payload)
      ElMessage.success('术语已更新')
    } else {
      await createBusinessGlossary(payload)
      ElMessage.success('术语已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: BusinessGlossary) {
  if (!row.id || !projectStore.currentProjectId) {
    return
  }
  const nextStatus = row.status === 'disabled' ? 'enabled' : 'disabled'
  await updateBusinessGlossary(row.id, normalizePayload({
    ...row,
    projectId: projectStore.currentProjectId,
    status: nextStatus
  }))
  ElMessage.success(nextStatus === 'enabled' ? '术语已启用' : '术语已禁用')
  await loadData()
}

async function handleDelete(row: BusinessGlossary) {
  if (!row.id) {
    return
  }
  try {
    await ElMessageBox.confirm('删除后，该术语不再参与字段推荐、检索和 AI Context 导出。', '删除业务术语', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteBusinessGlossary(row.id)
  ElMessage.success('术语已删除')
  await loadData()
}

function resetForm(row?: BusinessGlossary) {
  form.projectId = projectStore.currentProjectId ?? 0
  form.term = row?.term ?? ''
  form.synonyms = row?.synonyms ?? null
  form.rootTerms = row?.rootTerms ?? null
  form.abbreviations = row?.abbreviations ?? null
  form.disabledTerms = row?.disabledTerms ?? null
  form.canonicalFieldId = row?.canonicalFieldId ?? null
  form.scopeType = row?.scopeType ?? 'GLOBAL'
  form.scopeValue = row?.scopeValue ?? null
  form.exampleFields = row?.exampleFields ?? null
  form.description = row?.description ?? null
  form.status = row?.status ?? 'enabled'
  formRef.value?.clearValidate()
}

function normalizePayload(payload: BusinessGlossaryReq): BusinessGlossaryReq {
  return {
    ...payload,
    synonyms: emptyToNull(payload.synonyms),
    rootTerms: emptyToNull(payload.rootTerms),
    abbreviations: emptyToNull(payload.abbreviations),
    disabledTerms: emptyToNull(payload.disabledTerms),
    scopeValue: emptyToNull(payload.scopeValue),
    exampleFields: emptyToNull(payload.exampleFields),
    description: emptyToNull(payload.description),
    canonicalFieldId: payload.canonicalFieldId ?? null,
    scopeType: payload.scopeType || 'GLOBAL',
    status: payload.status || 'enabled'
  }
}

function emptyToNull(value: string | null | undefined) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : null
}

function canonicalFieldName(id?: number | null) {
  if (!id) {
    return ''
  }
  const field = fields.value.find((item) => item.id === id)
  return field?.name || ''
}

function conflictTerms(row: BusinessGlossaryConflictGroup) {
  return row.entries?.map((entry) => entry.term).filter(Boolean).join('、') || '-'
}

function scopeLabel(row: BusinessGlossary) {
  const type = row.scopeType || 'GLOBAL'
  const value = row.scopeValue ? `：${row.scopeValue}` : ''
  const labelMap: Record<string, string> = {
    GLOBAL: '全局',
    CATEGORY: '分类',
    DOMAIN: '数据域',
    TAG: '标签'
  }
  return `${labelMap[type] || type}${value}`
}
</script>

<style scoped>
.glossary-page {
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
}

.keyword-input {
  width: 360px;
}

.status-select {
  width: 140px;
}

.conflict-alert {
  max-width: 760px;
}

.glossary-table {
  width: 100%;
}

.pagination-row {
  justify-content: flex-end;
}

.conflict-collapse {
  border-top: 1px solid #ebeef5;
}

.collapse-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.full-width {
  width: 100%;
}
</style>
