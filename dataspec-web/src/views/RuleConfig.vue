<template>
  <div class="rule-page">
    <div class="page-header">
      <div>
        <h2>规则配置</h2>
        <p class="page-subtitle">
          {{ projectStore.currentProjectName || '未选择项目' }}
        </p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadRuleConfigs">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建规则
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <el-table
      v-else
      v-loading="loading"
      :data="rules"
      stripe
      class="rule-table"
      empty-text="暂无规则配置"
    >
      <el-table-column prop="ruleCode" label="规则编码" min-width="190" fixed="left" />
      <el-table-column prop="ruleName" label="规则名称" min-width="180" />
      <el-table-column label="级别" width="120">
        <template #default="{ row }">
          <el-tag :type="severityTagType(row.severity)" size="small">
            {{ row.severity || 'ERROR' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled ?? false"
            @change="(value: string | number | boolean) => handleToggleChange(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="paramsJson" label="参数 JSON" min-width="260" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingRule ? '编辑规则' : '新建规则'" width="680px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="104px">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-select
            v-model="form.ruleCode"
            filterable
            allow-create
            class="full-width"
            :disabled="Boolean(editingRule)"
            @change="handleRuleCodeChange"
          >
            <el-option
              v-for="item in availableRules"
              :key="item.code"
              :label="`${item.code}｜${item.name}`"
              :value="item.code"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="级别">
              <el-select v-model="form.severity" class="full-width">
                <el-option label="ERROR" value="ERROR" />
                <el-option label="WARNING" value="WARNING" />
                <el-option label="SUGGESTION" value="SUGGESTION" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="参数 JSON" prop="paramsJson">
          <el-input
            v-model="form.paramsJson"
            type="textarea"
            :rows="10"
            placeholder="{&quot;suffixTypes&quot;:{&quot;_id&quot;:[&quot;bigint&quot;]}}"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formatParamsJson">格式化 JSON</el-button>
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
  type FormItemRule,
  type FormRules
} from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listAvailableLintRules } from '@/api/lint'
import {
  createRuleConfig,
  deleteRuleConfig,
  listRuleConfigs,
  toggleRuleConfig,
  updateRuleConfig
} from '@/api/rule'
import { useProjectStore } from '@/stores/project'
import type { RuleConfig, RuleConfigReq } from '@/types'

interface AvailableRule {
  code?: string
  name?: string
}

const projectStore = useProjectStore()
const rules = ref<RuleConfig[]>([])
const availableRules = ref<AvailableRule[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingRule = ref<RuleConfig | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<RuleConfigReq>({
  projectId: 0,
  ruleCode: '',
  ruleName: '',
  severity: 'ERROR',
  enabled: true,
  paramsJson: '{}'
})

const validateParamsJson: FormItemRule['validator'] = (_rule, value, callback) => {
  const text = typeof value === 'string' ? value.trim() : ''
  if (!text) {
    callback()
    return
  }
  try {
    JSON.parse(text)
    callback()
  } catch {
    callback(new Error('请输入合法 JSON'))
  }
}

const formRules: FormRules<RuleConfigReq> = {
  ruleCode: [{ required: true, message: '请选择或输入规则编码', trigger: 'change' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  paramsJson: [{ validator: validateParamsJson, trigger: 'blur' }]
}

const hasProject = computed(() => Boolean(projectStore.currentProjectId))

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
  void loadAvailableRules()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadRuleConfigs()
  },
  { immediate: true }
)

async function loadAvailableRules() {
  availableRules.value = await listAvailableLintRules()
}

async function loadRuleConfigs() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    rules.value = []
    return
  }
  loading.value = true
  try {
    rules.value = await listRuleConfigs(projectId)
  } finally {
    loading.value = false
  }
}

function resetForm(rule?: RuleConfig) {
  form.projectId = projectStore.currentProjectId ?? rule?.projectId ?? 0
  form.ruleCode = rule?.ruleCode ?? ''
  form.ruleName = rule?.ruleName ?? ''
  form.severity = rule?.severity ?? 'ERROR'
  form.enabled = rule?.enabled ?? true
  form.paramsJson = rule?.paramsJson?.trim() || '{}'
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  editingRule.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(rule: RuleConfig) {
  editingRule.value = rule
  resetForm(rule)
  dialogVisible.value = true
}

function handleRuleCodeChange(value: string) {
  const selected = availableRules.value.find((item) => item.code === value)
  if (selected?.name) {
    form.ruleName = selected.name
  }
}

async function handleSubmit() {
  if (!projectStore.currentProjectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: RuleConfigReq = {
      ...form,
      projectId: projectStore.currentProjectId,
      paramsJson: form.paramsJson?.trim() || undefined
    }
    if (editingRule.value?.id) {
      await updateRuleConfig(editingRule.value.id, payload)
      ElMessage.success('规则已更新')
    } else {
      await createRuleConfig(payload)
      ElMessage.success('规则已创建')
    }
    dialogVisible.value = false
    await loadRuleConfigs()
  } finally {
    submitting.value = false
  }
}

async function handleToggle(rule: RuleConfig, enabled: boolean) {
  if (!rule.id) {
    return
  }
  await toggleRuleConfig(rule.id, enabled)
  ElMessage.success(enabled ? '规则已启用' : '规则已停用')
  await loadRuleConfigs()
}

function handleToggleChange(rule: RuleConfig, value: string | number | boolean) {
  void handleToggle(rule, Boolean(value))
}

async function handleDelete(rule: RuleConfig) {
  if (!rule.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除规则「${rule.ruleName ?? ''}」吗？`, '删除规则', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteRuleConfig(rule.id)
  ElMessage.success('规则已删除')
  await loadRuleConfigs()
}

function formatParamsJson() {
  const text = form.paramsJson?.trim()
  if (!text) {
    form.paramsJson = '{}'
    return
  }
  try {
    form.paramsJson = JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    ElMessage.warning('参数不是合法 JSON')
  }
}

function severityTagType(severity?: string) {
  if (severity === 'WARNING') {
    return 'warning'
  }
  if (severity === 'SUGGESTION') {
    return 'info'
  }
  return 'danger'
}
</script>

<style scoped>
.rule-page {
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

.rule-table {
  width: 100%;
}

.full-width {
  width: 100%;
}
</style>
