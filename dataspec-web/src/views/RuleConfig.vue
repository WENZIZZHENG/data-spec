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
      <el-table-column label="参数摘要" min-width="260">
        <template #default="{ row }">
          <div class="params-summary">
            <span>{{ parameterSummary(row) }}</span>
            <el-popover placement="left" width="420" trigger="click">
              <template #reference>
                <el-button text size="small">JSON</el-button>
              </template>
              <pre class="json-preview">{{ row.paramsJson || '{}' }}</pre>
            </el-popover>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingRule ? '编辑规则' : '新建规则'" width="860px">
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

        <el-form-item label="规则参数" prop="paramsJson">
          <div class="params-editor">
            <template v-if="isStructuredRuleCode">
              <section v-if="form.ruleCode === 'required_columns'" class="params-section">
                <div class="section-title">必含列</div>
                <div v-for="(_, index) in paramsForm.requiredColumns" :key="`required-${index}`" class="inline-row">
                  <el-input v-model="paramsForm.requiredColumns[index]" placeholder="created_at" />
                  <el-button @click="removeListItem('requiredColumns', index)">删除</el-button>
                </div>
                <el-button plain @click="addListItem('requiredColumns')">新增必含列</el-button>
              </section>

              <section v-else-if="form.ruleCode === 'forbidden_field_name'" class="params-section">
                <div class="section-title">禁用字段名</div>
                <div v-for="(_, index) in paramsForm.forbiddenNames" :key="`forbidden-${index}`" class="inline-row">
                  <el-input v-model="paramsForm.forbiddenNames[index]" placeholder="tmp" />
                  <el-button @click="removeListItem('forbiddenNames', index)">删除</el-button>
                </div>
                <el-button plain @click="addListItem('forbiddenNames')">新增禁用字段</el-button>
              </section>

              <section v-else-if="form.ruleCode === 'recommended_field_name'" class="params-section">
                <div class="section-title">推荐替换</div>
                <div v-for="(item, index) in paramsForm.recommendations" :key="`recommendation-${index}`" class="mapping-row">
                  <el-input v-model="item.from" placeholder="create_time" />
                  <span class="mapping-arrow">→</span>
                  <el-input v-model="item.to" placeholder="created_at" />
                  <el-button @click="removeRecommendation(index)">删除</el-button>
                </div>
                <el-button plain @click="addRecommendation">新增替换</el-button>
              </section>

              <section v-else-if="form.ruleCode === 'field_suffix_type'" class="params-section">
                <div class="type-rule-columns">
                  <div>
                    <div class="section-title">后缀类型</div>
                    <div v-for="(item, index) in paramsForm.suffixTypes" :key="`suffix-${index}`" class="type-rule-row">
                      <el-input v-model="item.pattern" placeholder="_id" />
                      <el-input v-model="item.typesText" placeholder="bigint, integer" />
                      <el-button @click="removeTypeRule('suffixTypes', index)">删除</el-button>
                    </div>
                    <el-button plain @click="addTypeRule('suffixTypes')">新增后缀</el-button>
                  </div>
                  <div>
                    <div class="section-title">前缀类型</div>
                    <div v-for="(item, index) in paramsForm.prefixTypes" :key="`prefix-${index}`" class="type-rule-row">
                      <el-input v-model="item.pattern" placeholder="is_" />
                      <el-input v-model="item.typesText" placeholder="boolean" />
                      <el-button @click="removeTypeRule('prefixTypes', index)">删除</el-button>
                    </div>
                    <el-button plain @click="addTypeRule('prefixTypes')">新增前缀</el-button>
                  </div>
                </div>
              </section>

              <div class="section-title">JSON 预览</div>
              <el-input :model-value="paramsJsonPreview" type="textarea" :rows="8" readonly />
            </template>

            <template v-else>
              <el-input
                v-model="form.paramsJson"
                type="textarea"
                :rows="10"
                placeholder="{&quot;suffixTypes&quot;:{&quot;_id&quot;:[&quot;bigint&quot;]}}"
              />
            </template>
          </div>
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
import {
  buildRuleParamsJson,
  createRuleParamsForm,
  isStructuredRule,
  parseRuleParamsForm,
  summarizeRuleParams,
  type RuleParamsForm
} from '@/utils/ruleParams'
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
const paramsForm = reactive<RuleParamsForm>(createRuleParamsForm())

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
const isStructuredRuleCode = computed(() => isStructuredRule(form.ruleCode))
const paramsJsonPreview = computed(() =>
  isStructuredRuleCode.value ? buildRuleParamsJson(form.ruleCode, paramsForm) : form.paramsJson || '{}'
)

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
  resetParamsForm()
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
  form.paramsJson = '{}'
  resetParamsForm()
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
      paramsJson: isStructuredRuleCode.value
        ? buildRuleParamsJson(form.ruleCode, paramsForm)
        : form.paramsJson?.trim() || undefined
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
  if (isStructuredRuleCode.value) {
    form.paramsJson = buildRuleParamsJson(form.ruleCode, paramsForm)
    ElMessage.success('已同步 JSON 预览')
    return
  }
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

function resetParamsForm() {
  Object.assign(paramsForm, parseRuleParamsForm(form.ruleCode, form.paramsJson))
}

function parameterSummary(rule: RuleConfig) {
  return summarizeRuleParams(rule.ruleCode, rule.paramsJson)
}

function addListItem(key: 'requiredColumns' | 'forbiddenNames') {
  paramsForm[key].push('')
}

function removeListItem(key: 'requiredColumns' | 'forbiddenNames', index: number) {
  paramsForm[key].splice(index, 1)
}

function addRecommendation() {
  paramsForm.recommendations.push({ from: '', to: '' })
}

function removeRecommendation(index: number) {
  paramsForm.recommendations.splice(index, 1)
}

function addTypeRule(key: 'suffixTypes' | 'prefixTypes') {
  paramsForm[key].push({ pattern: '', typesText: '' })
}

function removeTypeRule(key: 'suffixTypes' | 'prefixTypes', index: number) {
  paramsForm[key].splice(index, 1)
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

.params-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.json-preview {
  max-height: 360px;
  margin: 0;
  overflow: auto;
  color: #1f2937;
  font-size: 12px;
  white-space: pre-wrap;
}

.params-editor {
  width: 100%;
}

.params-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.section-title {
  margin: 8px 0 6px;
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
}

.inline-row,
.mapping-row,
.type-rule-row {
  display: grid;
  gap: 10px;
  align-items: center;
}

.inline-row {
  grid-template-columns: minmax(220px, 1fr) auto;
}

.mapping-row {
  grid-template-columns: minmax(160px, 1fr) auto minmax(160px, 1fr) auto;
}

.type-rule-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.type-rule-row {
  grid-template-columns: minmax(90px, 0.8fr) minmax(160px, 1.2fr) auto;
  margin-bottom: 10px;
}

.mapping-arrow {
  color: #909399;
}

@media (max-width: 760px) {
  .type-rule-columns,
  .inline-row,
  .mapping-row,
  .type-rule-row {
    grid-template-columns: 1fr;
  }

  .mapping-arrow {
    display: none;
  }
}
</style>
