<template>
  <div class="rule-exemptions-page">
    <div class="page-header">
      <div>
        <h2>规则例外</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadExemptions">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建例外
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <el-alert
        class="scope-alert"
        type="warning"
        :closable="false"
        show-icon
        title="规则例外只用于兼容历史表、第三方字段或框架约定，不会作为新建表标准推荐给 AI。"
      />

      <el-table
        v-loading="loading"
        :data="exemptions"
        stripe
        class="exemption-table"
        empty-text="暂无规则例外"
      >
        <el-table-column prop="ruleCode" label="规则编码" min-width="210" fixed="left" />
        <el-table-column label="范围" min-width="180">
          <template #default="{ row }">
            <el-tag effect="plain">{{ ruleExemptionScopeLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="ruleExemptionStatusTagType(row)" size="small">
              {{ ruleExemptionStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">
            {{ row.expiresAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="warning"
              :disabled="!row.enabled"
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-dialog v-model="dialogVisible" title="新建规则例外" width="720px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="104px">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-select v-model="form.ruleCode" filterable allow-create class="full-width">
            <el-option
              v-for="item in availableRules"
              :key="item.code"
              :label="`${item.code}｜${item.name}`"
              :value="item.code"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="表名">
              <el-input v-model="form.tableName" placeholder="UserOrder" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段名">
              <el-input v-model="form.columnName" placeholder="userId" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expiresAt"
            class="full-width"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="不设置表示长期有效"
          />
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listAvailableLintRules } from '@/api/lint'
import {
  createRuleExemption,
  deleteRuleExemption,
  disableRuleExemption,
  listRuleExemptions
} from '@/api/ruleExemption'
import { useProjectStore } from '@/stores/project'
import {
  normalizeRuleExemptionPayload,
  ruleExemptionScopeLabel,
  ruleExemptionStatusLabel,
  ruleExemptionStatusTagType
} from '@/utils/ruleExemptionDisplay'
import type { RuleExemption, RuleExemptionReq } from '@/types'

interface AvailableRule {
  code?: string
  name?: string
}

const projectStore = useProjectStore()
const exemptions = ref<RuleExemption[]>([])
const availableRules = ref<AvailableRule[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<RuleExemptionReq>({
  projectId: 0,
  ruleCode: '',
  tableName: null,
  columnName: null,
  reason: '',
  expiresAt: null
})

const formRules: FormRules<RuleExemptionReq> = {
  ruleCode: [{ required: true, message: '请选择或输入规则编码', trigger: 'change' }],
  reason: [{ required: true, message: '请输入豁免原因', trigger: 'blur' }]
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
    void loadExemptions()
  },
  { immediate: true }
)

async function loadAvailableRules() {
  availableRules.value = await listAvailableLintRules()
}

async function loadExemptions() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    exemptions.value = []
    return
  }
  loading.value = true
  try {
    exemptions.value = await listRuleExemptions(projectId)
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  form.projectId = projectStore.currentProjectId ?? 0
  form.ruleCode = ''
  form.tableName = null
  form.columnName = null
  form.reason = ''
  form.expiresAt = null
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSubmit() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  await formRef.value?.validate()
  const payload = normalizeRuleExemptionPayload({
    ...form,
    projectId
  }) as RuleExemptionReq
  if (!payload.tableName && !payload.columnName) {
    ElMessage.warning('请至少填写表名或字段名')
    return
  }
  submitting.value = true
  try {
    await createRuleExemption(payload)
    ElMessage.success('规则例外已创建')
    dialogVisible.value = false
    await loadExemptions()
  } finally {
    submitting.value = false
  }
}

async function handleDisable(exemption: RuleExemption) {
  if (!exemption.id) {
    return
  }
  try {
    await ElMessageBox.confirm('禁用后，该例外不再抑制 SQL 检查问题。', '禁用规则例外', {
      type: 'warning',
      confirmButtonText: '禁用',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await disableRuleExemption(exemption.id)
  ElMessage.success('规则例外已禁用')
  await loadExemptions()
}

async function handleDelete(exemption: RuleExemption) {
  if (!exemption.id) {
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该规则例外吗？', '删除规则例外', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteRuleExemption(exemption.id)
  ElMessage.success('规则例外已删除')
  await loadExemptions()
}
</script>

<style scoped>
.rule-exemptions-page {
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

.scope-alert {
  margin-bottom: 14px;
}

.exemption-table {
  width: 100%;
}

.full-width {
  width: 100%;
}

@media (max-width: 760px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
