<template>
  <div class="project-page">
    <div class="page-header">
      <h2>项目列表</h2>
      <div class="header-actions">
        <el-button :loading="demoLoading" @click="handleCreateDemoProject">
          演示项目
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建项目
        </el-button>
      </div>
    </div>

    <el-table
      v-loading="projectStore.loading"
      :data="projectStore.projects"
      stripe
      class="project-table"
      empty-text="暂无项目"
      @row-click="handleSelectProject"
    >
      <el-table-column prop="name" label="项目名称" min-width="180" />
      <el-table-column prop="dbType" label="数据库类型" width="130" />
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="当前" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.id === projectStore.currentProjectId" type="success" size="small">
            当前
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click.stop="handleSelectProject(row)">
            选择
          </el-button>
          <el-button text type="primary" @click.stop="openStarterKitDialog(row)">
            Starter Kit
          </el-button>
          <el-button text type="primary" @click.stop="openEditDialog(row)">
            编辑
          </el-button>
          <el-button text type="danger" @click.stop="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingProject ? '编辑项目' : '新建项目'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="数据库类型" prop="dbType">
          <el-select v-model="form.dbType" placeholder="请选择数据库类型" class="full-width">
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="MySQL" value="mysql" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入项目描述"
          />
        </el-form-item>
        <el-form-item v-if="!editingProject" label="初始化">
          <el-switch
            v-model="form.importBuiltInStandards"
            active-text="导入内置标准"
            inactive-text="空白项目"
          />
        </el-form-item>
        <el-form-item v-if="!editingProject" label="Starter Kit">
          <el-select
            v-model="selectedStarterKitKeys"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="可选领域模板"
            class="full-width"
          >
            <el-option
              v-for="kit in starterKits"
              :key="kit.key"
              :label="kitLabel(kit)"
              :value="kit.key ?? ''"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="starterKitDialogVisible"
      :title="starterKitTarget ? `应用 Starter Kit：${starterKitTarget.name ?? ''}` : '应用 Starter Kit'"
      width="640px"
    >
      <el-form label-width="96px">
        <el-form-item label="选择模板">
          <el-select
            v-model="applyStarterKitKeys"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择要应用的 Starter Kit"
            class="full-width"
          >
            <el-option
              v-for="kit in starterKits"
              :key="kit.key"
              :label="kitLabel(kit)"
              :value="kit.key ?? ''"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="starter-kit-catalog">
        <el-tag
          v-for="kit in starterKits"
          :key="kit.key"
          type="info"
          effect="plain"
          class="starter-kit-tag"
        >
          {{ kit.name }} · 字段 {{ kit.fieldCount ?? kit.fields?.length ?? 0 }}
        </el-tag>
      </div>
      <el-divider content-position="left">最近安装记录</el-divider>
      <el-table
        v-loading="installationsLoading"
        :data="starterKitInstallations"
        size="small"
        max-height="220"
        empty-text="暂无安装记录"
      >
        <el-table-column prop="kitName" label="Starter Kit" min-width="150" />
        <el-table-column prop="kitVersion" label="版本" width="100" />
        <el-table-column label="创建" width="120">
          <template #default="{ row }">
            字段 {{ row.created?.fields ?? 0 }} / 模板 {{ row.created?.templates ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="跳过" width="120">
          <template #default="{ row }">
            字段 {{ row.skipped?.fields ?? 0 }} / 模板 {{ row.skipped?.templates ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="appliedAt" label="应用时间" min-width="150" />
      </el-table>
      <template #footer>
        <el-button @click="starterKitDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="applyStarterKitKeys.length === 0"
          :loading="starterKitApplying"
          @click="handleApplyStarterKits"
        >
          应用
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createProject, deleteProject, updateProject } from '@/api/project'
import {
  applyStarterKit,
  listStarterKitInstallations,
  listStarterKits
} from '@/api/starterKit'
import { useProjectStore } from '@/stores/project'
import type {
  CreateProjectReq,
  Project,
  StarterKitApplyResult,
  StarterKitDefinition,
  StarterKitInstallationInfo
} from '@/types'

const projectStore = useProjectStore()
const dialogVisible = ref(false)
const submitting = ref(false)
const demoLoading = ref(false)
const editingProject = ref<Project | null>(null)
const formRef = ref<FormInstance>()
const starterKits = ref<StarterKitDefinition[]>([])
const selectedStarterKitKeys = ref<string[]>([])
const starterKitDialogVisible = ref(false)
const starterKitTarget = ref<Project | null>(null)
const applyStarterKitKeys = ref<string[]>([])
const starterKitApplying = ref(false)
const starterKitInstallations = ref<StarterKitInstallationInfo[]>([])
const installationsLoading = ref(false)

const form = reactive<CreateProjectReq>({
  name: '',
  description: '',
  dbType: 'postgresql',
  importBuiltInStandards: true
})

const rules: FormRules<CreateProjectReq> = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }]
}

onMounted(() => {
  projectStore.loadProjects()
  loadStarterKits()
})

function resetForm(project?: Project) {
  form.name = project?.name ?? ''
  form.description = project?.description ?? ''
  form.dbType = project?.dbType ?? 'postgresql'
  form.importBuiltInStandards = !project
  selectedStarterKitKeys.value = []
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  editingProject.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(project: Project) {
  editingProject.value = project
  resetForm(project)
  dialogVisible.value = true
}

function handleSelectProject(project: Project) {
  projectStore.setCurrentProject(project)
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (editingProject.value?.id) {
      await updateProject(editingProject.value.id, {
        name: form.name,
        description: form.description,
        dbType: form.dbType
      })
      ElMessage.success('项目已更新')
    } else {
      const created = await createProject({ ...form })
      const results = await applyStarterKitsAfterCreate(created)
      ElMessage.success(results.length > 0 ? starterKitSummaryMessage('项目已创建', results) : '项目已创建')
      projectStore.setCurrentProject(created)
    }
    dialogVisible.value = false
    await projectStore.loadProjects()
  } finally {
    submitting.value = false
  }
}

async function loadStarterKits(showWarning = false) {
  try {
    starterKits.value = await listStarterKits()
  } catch {
    starterKits.value = []
    if (showWarning) {
      ElMessage.warning('Starter Kit 列表加载失败，可稍后重试')
    }
  }
}

function kitLabel(kit: StarterKitDefinition) {
  const fields = kit.fieldCount ?? kit.fields?.length ?? 0
  const templates = kit.templateCount ?? kit.templates?.length ?? 0
  return `${kit.name ?? kit.key} · ${fields} 字段 / ${templates} 模板`
}

async function applyStarterKitsAfterCreate(project: Project) {
  if (!project.id || selectedStarterKitKeys.value.length === 0) {
    return []
  }
  try {
    return await applyStarterKitsToProject(project.id, selectedStarterKitKeys.value)
  } catch (error) {
    ElMessage.warning('项目已创建，Starter Kit 应用失败，可稍后在项目列表重试')
    return []
  }
}

async function applyStarterKitsToProject(projectId: number, kitKeys: string[]) {
  const results: StarterKitApplyResult[] = []
  const selected = kitKeys.filter(Boolean)
  for (const kitKey of selected) {
    const kit = starterKits.value.find(item => item.key === kitKey)
    results.push(await applyStarterKit({
      projectId,
      kitKey,
      kitVersion: kit?.version
    }))
  }
  return results
}

function starterKitSummaryMessage(prefix: string, results: StarterKitApplyResult[]) {
  const createdFields = results.reduce((sum, item) => sum + (item.created?.fields ?? 0), 0)
  const skippedFields = results.reduce((sum, item) => sum + (item.skipped?.fields ?? 0), 0)
  const createdTemplates = results.reduce((sum, item) => sum + (item.created?.templates ?? 0), 0)
  return `${prefix}，Starter Kit 创建字段 ${createdFields}、模板 ${createdTemplates}，跳过字段 ${skippedFields}`
}

async function openStarterKitDialog(project: Project) {
  starterKitTarget.value = project
  applyStarterKitKeys.value = []
  starterKitDialogVisible.value = true
  if (starterKits.value.length === 0) {
    await loadStarterKits(true)
  }
  await loadInstallations(project.id)
}

async function loadInstallations(projectId?: number) {
  if (!projectId) {
    starterKitInstallations.value = []
    return
  }
  installationsLoading.value = true
  try {
    starterKitInstallations.value = await listStarterKitInstallations(projectId)
  } finally {
    installationsLoading.value = false
  }
}

async function handleApplyStarterKits() {
  if (!starterKitTarget.value?.id) {
    ElMessage.warning('请先选择项目')
    return
  }
  starterKitApplying.value = true
  try {
    const results = await applyStarterKitsToProject(starterKitTarget.value.id, applyStarterKitKeys.value)
    ElMessage.success(starterKitSummaryMessage('Starter Kit 已应用', results))
    applyStarterKitKeys.value = []
    await loadInstallations(starterKitTarget.value.id)
  } finally {
    starterKitApplying.value = false
  }
}

async function handleCreateDemoProject() {
  demoLoading.value = true
  try {
    const result = await projectStore.createDemoProjectAndSelect()
    ElMessage.success(result.created ? '演示项目已创建' : '已切换到演示项目')
  } finally {
    demoLoading.value = false
  }
}

async function handleDelete(project: Project) {
  if (!project.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除项目「${project.name ?? ''}」吗？`, '删除项目', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteProject(project.id)
  ElMessage.success('项目已删除')
  await projectStore.loadProjects()
}
</script>

<style scoped>
.project-page {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 140px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.project-table {
  width: 100%;
}

.full-width {
  width: 100%;
}

.starter-kit-catalog {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.starter-kit-tag {
  max-width: 100%;
}
</style>
