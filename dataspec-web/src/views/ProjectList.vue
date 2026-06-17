<template>
  <div class="project-page">
    <div class="page-header">
      <h2>项目列表</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建项目
      </el-button>
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
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click.stop="handleSelectProject(row)">
            选择
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createProject, deleteProject, updateProject } from '@/api/project'
import { useProjectStore } from '@/stores/project'
import type { CreateProjectReq, Project } from '@/types'

const projectStore = useProjectStore()
const dialogVisible = ref(false)
const submitting = ref(false)
const editingProject = ref<Project | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<CreateProjectReq>({
  name: '',
  description: '',
  dbType: 'postgresql'
})

const rules: FormRules<CreateProjectReq> = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }]
}

onMounted(() => {
  projectStore.loadProjects()
})

function resetForm(project?: Project) {
  form.name = project?.name ?? ''
  form.description = project?.description ?? ''
  form.dbType = project?.dbType ?? 'postgresql'
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
      await updateProject(editingProject.value.id, { ...form })
      ElMessage.success('项目已更新')
    } else {
      const created = await createProject({ ...form })
      ElMessage.success('项目已创建')
      projectStore.setCurrentProject(created)
    }
    dialogVisible.value = false
    await projectStore.loadProjects()
  } finally {
    submitting.value = false
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

.project-table {
  width: 100%;
}

.full-width {
  width: 100%;
}
</style>
