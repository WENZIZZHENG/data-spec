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
      <el-table
        v-loading="loading"
        :data="fields"
        stripe
        class="field-table"
        empty-text="暂无标准字段"
      >
        <el-table-column prop="name" label="字段名" min-width="150" fixed="left" />
        <el-table-column prop="displayName" label="显示名" min-width="120" />
        <el-table-column label="类型" min-width="150">
          <template #default="{ row }">
            <span>{{ formatDataType(row) }}</span>
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
        <el-table-column prop="category" label="分类" min-width="110" show-overflow-tooltip />
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
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="loadFields"
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
            <el-form-item label="数据域 ID">
              <el-input-number v-model="form.domainId" :min="1" :controls="false" class="full-width" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { createField, deleteField, pageFields, updateField } from '@/api/field'
import { useProjectStore } from '@/stores/project'
import type { Field, FieldReq, PageResult } from '@/types'

const projectStore = useProjectStore()
const fields = ref<Field[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingField = ref<Field | null>(null)
const formRef = ref<FormInstance>()

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
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

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    pagination.current = 1
    void loadFields()
  },
  { immediate: true }
)

async function loadFields() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    fields.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const page: PageResult<Field> = await pageFields(projectId, pagination.current, pagination.size)
    fields.value = page.records ?? []
    pagination.total = page.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.current = 1
  void loadFields()
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

async function handleSubmit() {
  if (!projectStore.currentProjectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  await formRef.value?.validate()
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

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.full-width {
  width: 100%;
}
</style>
