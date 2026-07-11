<template>
  <div class="enum-page">
    <div class="page-header">
      <div>
        <h2>枚举字典</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="dictLoading" :disabled="!hasProject" @click="loadDicts">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openDictDialog()">
          <el-icon><Plus /></el-icon>
          新建枚举
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div class="enum-layout">
        <section class="dict-panel">
          <div class="section-header">
            <h3>字典列表</h3>
            <span class="muted-text">{{ enumDicts.length }} 个</span>
          </div>
          <el-table
            v-loading="dictLoading"
            :data="enumDicts"
            row-key="id"
            stripe
            empty-text="暂无枚举字典"
            highlight-current-row
            @row-click="selectDict"
          >
            <el-table-column label="名称" min-width="180">
              <template #default="{ row }">
                <div class="dict-name">{{ row.name || '-' }}</div>
                <div class="muted-text">{{ row.code || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="valueType" label="值类型" width="100" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click.stop="openDictDialog(row)">编辑</el-button>
                <el-button text type="danger" @click.stop="handleDeleteDict(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="value-panel">
          <div class="section-header">
            <div>
              <h3>枚举值生命周期</h3>
              <p class="page-subtitle">
                {{ selectedDict?.name || '请选择一个枚举字典' }}
              </p>
            </div>
            <el-button type="primary" plain :disabled="!selectedDict?.id" @click="openValueDialog()">
              <el-icon><Plus /></el-icon>
              新建枚举值
            </el-button>
          </div>

          <el-table
            v-loading="valueLoading"
            :data="enumValues"
            row-key="id"
            stripe
            empty-text="暂无枚举值"
          >
            <el-table-column label="值 / 标签" min-width="180">
              <template #default="{ row }">
                <div class="dict-name">{{ row.value || '-' }}</div>
                <div class="muted-text">{{ row.label || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="别名 / 替代值" min-width="220">
              <template #default="{ row }">
                <div>{{ jsonArrayPreview(row.aliasesJson) || '-' }}</div>
                <div v-if="row.replacementValue" class="muted-text">替代值：{{ row.replacementValue }}</div>
              </template>
            </el-table-column>
            <el-table-column label="有效期" min-width="170">
              <template #default="{ row }">
                {{ valueDateRange(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="mappingHints" label="映射提示" min-width="180" show-overflow-tooltip />
            <el-table-column prop="aiUsageNotes" label="AI 使用说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openValueDialog(row)">编辑</el-button>
                <el-button text type="danger" @click="handleDeleteValue(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </template>

    <el-dialog v-model="dictDialogVisible" :title="editingDict ? '编辑枚举字典' : '新建枚举字典'" width="560px">
      <el-form ref="dictFormRef" :model="dictForm" :rules="dictRules" label-width="92px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="dictForm.name" placeholder="订单状态" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="dictForm.code" placeholder="order_status" />
        </el-form-item>
        <el-form-item label="值类型">
          <el-input v-model="dictForm.valueType" placeholder="string / number" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dictForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dictSubmitting" @click="handleSubmitDict">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="valueDialogVisible" :title="editingValue ? '编辑枚举值' : '新建枚举值'" width="720px">
      <el-form ref="valueFormRef" :model="valueForm" :rules="valueRules" label-width="104px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="枚举值" prop="value">
              <el-input v-model="valueForm.value" placeholder="PAID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示标签" prop="label">
              <el-input v-model="valueForm.label" placeholder="已支付" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="valueForm.sortOrder" :controls="false" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="valueForm.status" class="full-width">
                <el-option label="启用" value="enabled" />
                <el-option label="废弃" value="deprecated" />
                <el-option label="停用" value="disabled" />
                <el-option label="草稿" value="draft" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="替代值">
              <el-input v-model="valueForm.replacementValue" placeholder="SUCCESS" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="有效期开始">
              <el-date-picker v-model="valueForm.validFrom" value-format="YYYY-MM-DD" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期结束">
              <el-date-picker v-model="valueForm.validTo" value-format="YYYY-MM-DD" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="别名">
          <el-input v-model="valueForm.aliasesJson" placeholder='["paid","pay_success"]' />
        </el-form-item>
        <el-form-item label="来源证据">
          <el-input v-model="valueForm.sourceEvidence" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="映射提示">
          <el-input v-model="valueForm.mappingHints" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="AI 使用说明">
          <el-input v-model="valueForm.aiUsageNotes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="valueDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="valueSubmitting" @click="handleSubmitValue">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  createEnumDict,
  createEnumValue,
  deleteEnumDict,
  deleteEnumValue,
  listEnumDicts,
  listEnumValues,
  updateEnumDict,
  updateEnumValue
} from '@/api/enumDict'
import { useProjectStore } from '@/stores/project'
import type { EnumDict, EnumDictReq, EnumValue, EnumValueReq } from '@/types'

const projectStore = useProjectStore()
const enumDicts = ref<EnumDict[]>([])
const enumValues = ref<EnumValue[]>([])
const selectedDict = ref<EnumDict | null>(null)
const editingDict = ref<EnumDict | null>(null)
const editingValue = ref<EnumValue | null>(null)
const dictDialogVisible = ref(false)
const valueDialogVisible = ref(false)
const dictLoading = ref(false)
const valueLoading = ref(false)
const dictSubmitting = ref(false)
const valueSubmitting = ref(false)
const dictFormRef = ref<FormInstance>()
const valueFormRef = ref<FormInstance>()

const dictForm = reactive<EnumDictReq>({
  projectId: 0,
  name: '',
  code: '',
  description: '',
  valueType: 'string'
})

const valueForm = reactive<EnumValueReq>({
  value: '',
  label: '',
  sortOrder: 0,
  status: 'enabled',
  aliasesJson: '',
  replacementValue: '',
  validFrom: '',
  validTo: '',
  sourceEvidence: '',
  mappingHints: '',
  aiUsageNotes: ''
})

const dictRules: FormRules<EnumDictReq> = {
  name: [{ required: true, message: '请输入枚举名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入枚举编码', trigger: 'blur' }]
}

const valueRules: FormRules<EnumValueReq> = {
  value: [{ required: true, message: '请输入枚举值', trigger: 'blur' }],
  label: [{ required: true, message: '请输入显示标签', trigger: 'blur' }]
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
    selectedDict.value = null
    enumValues.value = []
    void loadDicts()
  },
  { immediate: true }
)

async function loadDicts() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    enumDicts.value = []
    selectedDict.value = null
    enumValues.value = []
    return
  }
  dictLoading.value = true
  try {
    enumDicts.value = await listEnumDicts(projectId)
    if (!selectedDict.value || !enumDicts.value.some((dict) => dict.id === selectedDict.value?.id)) {
      selectedDict.value = enumDicts.value[0] ?? null
    }
    await loadValues()
  } finally {
    dictLoading.value = false
  }
}

async function selectDict(dict: EnumDict) {
  selectedDict.value = dict
  await loadValues()
}

async function loadValues() {
  const enumId = selectedDict.value?.id
  if (!enumId) {
    enumValues.value = []
    return
  }
  valueLoading.value = true
  try {
    enumValues.value = await listEnumValues(enumId)
  } finally {
    valueLoading.value = false
  }
}

function openDictDialog(dict?: EnumDict) {
  editingDict.value = dict ?? null
  dictForm.projectId = projectStore.currentProjectId ?? dict?.projectId ?? 0
  dictForm.name = dict?.name ?? ''
  dictForm.code = dict?.code ?? ''
  dictForm.description = dict?.description ?? ''
  dictForm.valueType = dict?.valueType ?? 'string'
  dictFormRef.value?.clearValidate()
  dictDialogVisible.value = true
}

function openValueDialog(value?: EnumValue) {
  if (!selectedDict.value?.id) {
    ElMessage.warning('请先选择枚举字典')
    return
  }
  editingValue.value = value ?? null
  valueForm.value = value?.value ?? ''
  valueForm.label = value?.label ?? ''
  valueForm.sortOrder = value?.sortOrder ?? 0
  valueForm.status = value?.status ?? 'enabled'
  valueForm.aliasesJson = value?.aliasesJson ?? ''
  valueForm.replacementValue = value?.replacementValue ?? ''
  valueForm.validFrom = value?.validFrom ?? ''
  valueForm.validTo = value?.validTo ?? ''
  valueForm.sourceEvidence = value?.sourceEvidence ?? ''
  valueForm.mappingHints = value?.mappingHints ?? ''
  valueForm.aiUsageNotes = value?.aiUsageNotes ?? ''
  valueFormRef.value?.clearValidate()
  valueDialogVisible.value = true
}

async function handleSubmitDict() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  await dictFormRef.value?.validate()
  dictSubmitting.value = true
  try {
    const payload: EnumDictReq = { ...dictForm, projectId }
    if (editingDict.value?.id) {
      await updateEnumDict(editingDict.value.id, payload)
      ElMessage.success('枚举字典已更新')
    } else {
      await createEnumDict(payload)
      ElMessage.success('枚举字典已创建')
    }
    dictDialogVisible.value = false
    await loadDicts()
  } finally {
    dictSubmitting.value = false
  }
}

async function handleSubmitValue() {
  const enumId = selectedDict.value?.id
  if (!enumId) {
    return
  }
  await valueFormRef.value?.validate()
  valueSubmitting.value = true
  try {
    const payload: EnumValueReq = { ...valueForm }
    if (editingValue.value?.id) {
      await updateEnumValue(editingValue.value.id, payload)
      ElMessage.success('枚举值已更新')
    } else {
      await createEnumValue(enumId, payload)
      ElMessage.success('枚举值已创建')
    }
    valueDialogVisible.value = false
    await loadValues()
  } finally {
    valueSubmitting.value = false
  }
}

async function handleDeleteDict(dict: EnumDict) {
  if (!dict.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除枚举字典「${dict.name ?? ''}」吗？`, '删除枚举字典', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteEnumDict(dict.id)
  ElMessage.success('枚举字典已删除')
  await loadDicts()
}

async function handleDeleteValue(value: EnumValue) {
  if (!value.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除枚举值「${value.value ?? ''}」吗？`, '删除枚举值', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteEnumValue(value.id)
  ElMessage.success('枚举值已删除')
  await loadValues()
}

function statusText(status?: string) {
  if (status === 'draft') {
    return '草稿'
  }
  if (status === 'disabled') {
    return '停用'
  }
  if (status === 'deprecated') {
    return '废弃'
  }
  return '启用'
}

function statusTagType(status?: string) {
  if (status === 'draft') {
    return 'primary'
  }
  if (status === 'disabled') {
    return 'info'
  }
  if (status === 'deprecated') {
    return 'warning'
  }
  return 'success'
}

function valueDateRange(value: EnumValue) {
  if (!value.validFrom && !value.validTo) {
    return '-'
  }
  return `${value.validFrom || '不限'} 至 ${value.validTo || '不限'}`
}

function jsonArrayPreview(value?: string | null) {
  if (!value?.trim()) {
    return ''
  }
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) {
      return parsed.map(String).join(', ')
    }
  } catch {
    return value
  }
  return value
}
</script>

<style scoped>
.enum-page {
  min-height: calc(100vh - 140px);
  padding: 20px;
  background: #fff;
  border-radius: 4px;
}

.page-header,
.section-header,
.header-actions {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2,
.section-header h3 {
  margin: 0;
  color: #1f2937;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.enum-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(520px, 1.4fr);
  gap: 16px;
  align-items: start;
}

.dict-panel,
.value-panel {
  min-width: 0;
}

.dict-name {
  color: #303133;
  font-weight: 600;
  word-break: break-word;
}

.muted-text {
  color: #6b7280;
  font-size: 12px;
}

.full-width {
  width: 100%;
}

@media (max-width: 980px) {
  .enum-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-header,
  .section-header {
    flex-direction: column;
  }
}
</style>
