<template>
  <div class="template-page">
    <div class="page-header">
      <div>
        <h2>表模板</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <el-button :loading="loading" :disabled="!hasProject" @click="loadWorkbench">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div class="workbench-layout">
        <section class="list-panel" aria-labelledby="template-list-title">
          <div class="section-header">
            <h3 id="template-list-title">模板列表</h3>
            <el-button size="small" @click="startCreateTemplate">新建模板</el-button>
          </div>
          <el-table
            v-loading="templateLoading"
            :data="templates"
            stripe
            class="dense-table"
            empty-text="当前项目暂无表模板"
            @row-click="selectTemplate"
          >
            <el-table-column prop="name" label="模板名称" min-width="130" show-overflow-tooltip />
            <el-table-column label="业务对象" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                {{ businessObjectName(row.structure?.businessObjectId) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="82" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click.stop="selectTemplate(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="section-header object-header">
            <h3>业务对象标准</h3>
            <el-button size="small" @click="startCreateBusinessObject">新建业务对象</el-button>
          </div>
          <el-table
            v-loading="objectLoading"
            :data="businessObjects"
            stripe
            class="dense-table"
            empty-text="当前项目暂无业务对象"
            @row-click="selectBusinessObject"
          >
            <el-table-column prop="objectKey" label="对象键" min-width="120" show-overflow-tooltip />
            <el-table-column prop="entityName" label="实体名称" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="88">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'DISABLED' ? 'info' : 'success'">
                  {{ row.status || 'ENABLED' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="82" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click.stop="selectBusinessObject(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="editor-panel" aria-labelledby="template-editor-title">
          <div class="section-header">
            <h3 id="template-editor-title">模板基础信息</h3>
            <el-button type="primary" :loading="templateSaving" @click="saveTemplate">
              保存模板标准
            </el-button>
          </div>

          <el-form class="dense-form" label-width="112px" @submit.prevent>
            <div class="form-grid">
              <el-form-item label="模板名称">
                <el-input v-model="templateForm.name" placeholder="订单表模板" clearable />
              </el-form-item>
              <el-form-item label="表名前缀">
                <el-input v-model="templateForm.tablePrefix" placeholder="biz_" clearable />
              </el-form-item>
              <el-form-item label="关联业务对象">
                <el-select
                  v-model="templateForm.structure.businessObjectId"
                  class="full-width"
                  clearable
                  filterable
                  placeholder="未关联"
                >
                  <el-option
                    v-for="item in businessObjects"
                    :key="item.id ?? item.objectKey"
                    :label="`${item.entityName || item.objectKey}（${item.objectKey || '未命名'}）`"
                    :value="item.id"
                    :disabled="!item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="模板说明">
                <el-input v-model="templateForm.description" placeholder="说明模板适用的业务场景" clearable />
              </el-form-item>
            </div>

            <h4>表结构标准</h4>
            <div class="json-grid">
              <el-form-item label="主键字段">
                <el-input
                  v-model="templateJson.primaryKey"
                  type="textarea"
                  :rows="4"
                  placeholder='{"name":"order_pk","columns":["id"],"notes":"订单主键"}'
                />
              </el-form-item>
              <el-form-item label="唯一键 JSON">
                <el-input
                  v-model="templateJson.uniqueKeys"
                  type="textarea"
                  :rows="4"
                  placeholder='[{"name":"uk_order_no","columns":["order_no"]}]'
                />
              </el-form-item>
              <el-form-item label="索引 JSON">
                <el-input
                  v-model="templateJson.indexes"
                  type="textarea"
                  :rows="4"
                  placeholder='[{"name":"idx_order_user","columns":["user_id"],"method":"btree"}]'
                />
              </el-form-item>
              <el-form-item label="外键 JSON">
                <el-input
                  v-model="templateJson.foreignKeys"
                  type="textarea"
                  :rows="4"
                  placeholder='[{"columns":["user_id"],"targetTable":"user","targetColumns":["id"],"advisoryOnly":true}]'
                />
              </el-form-item>
              <el-form-item label="Check 提示 JSON">
                <el-input
                  v-model="templateJson.checkHints"
                  type="textarea"
                  :rows="3"
                  placeholder='["金额需大于等于 0"]'
                />
              </el-form-item>
              <el-form-item label="审计策略 JSON">
                <el-input
                  v-model="templateJson.auditPolicy"
                  type="textarea"
                  :rows="3"
                  placeholder='{"requiredFields":["created_at","updated_at"],"notes":"写入时维护"}'
                />
              </el-form-item>
              <el-form-item label="软删除策略 JSON">
                <el-input
                  v-model="templateJson.softDeletePolicy"
                  type="textarea"
                  :rows="3"
                  placeholder='{"fieldName":"is_deleted","activeValue":"0","deletedValue":"1"}'
                />
              </el-form-item>
              <el-form-item label="方言说明">
                <el-input
                  v-model="templateJson.dialectNotes"
                  type="textarea"
                  :rows="3"
                  placeholder='["PostgreSQL preview 为准，MySQL 方言需人工确认"]'
                />
              </el-form-item>
            </div>
            <el-form-item label="AI 使用说明">
              <el-input
                v-model="templateForm.structure.aiUsageNotes"
                type="textarea"
                :rows="3"
                placeholder="说明 AI 生成 DDL 时如何使用该模板，不填写凭据或业务数据行"
              />
            </el-form-item>
          </el-form>

          <section class="field-section" aria-labelledby="field-list-title">
            <div class="section-header">
              <h3 id="field-list-title">字段列表</h3>
              <span class="muted-text">模板字段只读展示，保存结构标准不会删除字段。</span>
            </div>
            <el-table
              v-loading="fieldLoading"
              :data="templateFields"
              stripe
              class="dense-table"
              empty-text="暂无模板字段"
            >
              <el-table-column prop="name" label="字段名" min-width="130" />
              <el-table-column prop="dataType" label="类型" width="130" />
              <el-table-column label="可空" width="76">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.nullable === false ? 'warning' : 'info'">
                    {{ row.nullable === false ? '否' : '是' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="defaultValue" label="默认值" width="130" show-overflow-tooltip />
              <el-table-column prop="comment" label="注释" min-width="180" show-overflow-tooltip />
            </el-table>
          </section>
        </section>

        <section class="editor-panel" aria-labelledby="business-object-editor-title">
          <div class="section-header">
            <h3 id="business-object-editor-title">业务对象编辑</h3>
            <el-button type="primary" :loading="objectSaving" @click="saveBusinessObject">
              保存业务对象
            </el-button>
          </div>

          <el-form class="dense-form" label-width="112px" @submit.prevent>
            <div class="form-grid">
              <el-form-item label="对象键">
                <el-input v-model="businessObjectForm.objectKey" placeholder="order" clearable />
              </el-form-item>
              <el-form-item label="实体名称">
                <el-input v-model="businessObjectForm.entityName" placeholder="订单" clearable />
              </el-form-item>
              <el-form-item label="表名模式">
                <el-input v-model="businessObjectForm.tablePattern" placeholder="order_*" clearable />
              </el-form-item>
              <el-form-item label="关联模板">
                <el-select
                  v-model="businessObjectForm.templateId"
                  class="full-width"
                  clearable
                  filterable
                  placeholder="未关联"
                >
                  <el-option
                    v-for="template in templates"
                    :key="template.id ?? template.name"
                    :label="template.name || `模板 ${template.id}`"
                    :value="template.id"
                    :disabled="!template.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="businessObjectForm.status" class="full-width">
                  <el-option label="ENABLED" value="ENABLED" />
                  <el-option label="DISABLED" value="DISABLED" />
                </el-select>
              </el-form-item>
              <el-form-item label="导出到 AI">
                <el-switch v-model="businessObjectForm.contextExport" />
              </el-form-item>
            </div>

            <div class="json-grid">
              <el-form-item label="必选字段 JSON">
                <el-input v-model="objectJson.requiredFields" type="textarea" :rows="3" placeholder='["id","created_at"]' />
              </el-form-item>
              <el-form-item label="可选字段 JSON">
                <el-input v-model="objectJson.optionalFields" type="textarea" :rows="3" placeholder='["remark"]' />
              </el-form-item>
              <el-form-item label="关系 JSON">
                <el-input
                  v-model="objectJson.relations"
                  type="textarea"
                  :rows="4"
                  placeholder='[{"sourceObjectKey":"order","targetObjectKey":"user","relationType":"MANY_TO_ONE"}]'
                />
              </el-form-item>
              <el-form-item label="外键提示 JSON">
                <el-input v-model="objectJson.foreignKeyHints" type="textarea" :rows="4" placeholder="[]" />
              </el-form-item>
              <el-form-item label="审计字段 JSON">
                <el-input v-model="objectJson.auditFields" type="textarea" :rows="3" placeholder='{"requiredFields":["created_at"]}' />
              </el-form-item>
              <el-form-item label="常见反模式 JSON">
                <el-input v-model="objectJson.commonPitfalls" type="textarea" :rows="3" placeholder='["不要把支付状态塞进订单备注"]' />
              </el-form-item>
            </div>
            <el-form-item label="AI 使用说明">
              <el-input
                v-model="businessObjectForm.aiUsageNotes"
                type="textarea"
                :rows="3"
                placeholder="说明 AI 如何理解该业务对象，不填写凭据或业务数据行"
              />
            </el-form-item>
          </el-form>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listBusinessObjects, createBusinessObject, updateBusinessObject } from '@/api/businessObject'
import { createTemplate, getTemplate, listTemplateFields, listTemplates, updateTemplate } from '@/api/template'
import { useProjectStore } from '@/stores/project'
import type {
  BusinessObjectStandard,
  BusinessObjectStandardReq,
  TableAuditPolicy,
  TableForeignKeyStandard,
  TableIndexStandard,
  TablePrimaryKeyStandard,
  TableRelationHint,
  TableSoftDeletePolicy,
  TableStructureStandard,
  TableUniqueKeyStandard,
  Template,
  TemplateField,
  TemplateResp,
  TemplateSaveReq
} from '@/types'

interface TemplateEditorForm extends Omit<TemplateResp, 'structure'> {
  structure: TableStructureStandard
}

const projectStore = useProjectStore()
const templates = ref<Template[]>([])
const businessObjects = ref<BusinessObjectStandard[]>([])
const templateFields = ref<TemplateField[]>([])
const selectedTemplateId = ref<number | null>(null)
const selectedBusinessObjectId = ref<number | null>(null)
const templateLoading = ref(false)
const objectLoading = ref(false)
const fieldLoading = ref(false)
const templateSaving = ref(false)
const objectSaving = ref(false)

const templateForm = reactive<TemplateEditorForm>(emptyTemplate())
const businessObjectForm = reactive<BusinessObjectStandard>(emptyBusinessObject())
const templateJson = reactive({
  primaryKey: 'null',
  uniqueKeys: '[]',
  indexes: '[]',
  foreignKeys: '[]',
  checkHints: '[]',
  auditPolicy: 'null',
  softDeletePolicy: 'null',
  dialectNotes: '[]'
})
const objectJson = reactive({
  requiredFields: '[]',
  optionalFields: '[]',
  relations: '[]',
  foreignKeyHints: '[]',
  auditFields: 'null',
  commonPitfalls: '[]'
})

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const loading = computed(() => templateLoading.value || objectLoading.value || fieldLoading.value)

watch(
  () => projectStore.currentProjectId,
  () => {
    void loadWorkbench()
  },
  { immediate: true }
)

async function loadWorkbench() {
  const projectId = projectStore.currentProjectId
  resetForms(projectId ?? undefined)
  if (!projectId) {
    templates.value = []
    businessObjects.value = []
    templateFields.value = []
    return
  }
  await Promise.all([loadTemplates(projectId), loadBusinessObjects(projectId)])
  const firstTemplate = templates.value.find((item) => item.id)
  if (firstTemplate) {
    await selectTemplate(firstTemplate)
  }
}

async function loadTemplates(projectId: number) {
  templateLoading.value = true
  try {
    templates.value = await listTemplates(projectId)
  } finally {
    templateLoading.value = false
  }
}

async function loadBusinessObjects(projectId: number) {
  objectLoading.value = true
  try {
    businessObjects.value = await listBusinessObjects(projectId)
  } finally {
    objectLoading.value = false
  }
}

async function selectTemplate(row: Template) {
  if (!row.id) {
    return
  }
  selectedTemplateId.value = row.id
  const detail = await getTemplate(row.id)
  assignTemplate(detail)
  await loadFields(row.id)
}

async function loadFields(templateId: number) {
  fieldLoading.value = true
  try {
    templateFields.value = await listTemplateFields(templateId)
  } finally {
    fieldLoading.value = false
  }
}

function selectBusinessObject(row: BusinessObjectStandard) {
  selectedBusinessObjectId.value = row.id ?? null
  assignBusinessObject(row)
}

function startCreateTemplate() {
  assignTemplate(emptyTemplate(projectStore.currentProjectId ?? undefined))
  selectedTemplateId.value = null
  templateFields.value = []
}

function startCreateBusinessObject() {
  assignBusinessObject(emptyBusinessObject(projectStore.currentProjectId ?? undefined))
  selectedBusinessObjectId.value = null
}

async function saveTemplate() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先创建并选择项目')
    return
  }
  if (!templateForm.name?.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  let payload: TemplateSaveReq
  try {
    payload = buildTemplatePayload(projectId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模板 JSON 格式不正确')
    return
  }
  templateSaving.value = true
  try {
    const saved = selectedTemplateId.value
      ? await updateTemplate(selectedTemplateId.value, payload)
      : await createTemplate(payload)
    selectedTemplateId.value = saved.id ?? null
    assignTemplate(saved)
    await loadTemplates(projectId)
    if (saved.id) {
      await loadFields(saved.id)
    }
    ElMessage.success('模板结构标准已保存')
  } finally {
    templateSaving.value = false
  }
}

async function saveBusinessObject() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先创建并选择项目')
    return
  }
  if (!businessObjectForm.objectKey?.trim() || !businessObjectForm.entityName?.trim()) {
    ElMessage.warning('请填写对象键和实体名称')
    return
  }
  let payload: BusinessObjectStandardReq
  try {
    payload = buildBusinessObjectPayload(projectId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '业务对象 JSON 格式不正确')
    return
  }
  objectSaving.value = true
  try {
    const saved = selectedBusinessObjectId.value
      ? await updateBusinessObject(selectedBusinessObjectId.value, payload)
      : await createBusinessObject(payload)
    selectedBusinessObjectId.value = saved.id ?? null
    assignBusinessObject(saved)
    await loadBusinessObjects(projectId)
    ElMessage.success('业务对象标准已保存')
  } finally {
    objectSaving.value = false
  }
}

function buildTemplatePayload(projectId: number): TemplateSaveReq {
  const structure: TableStructureStandard = {
    businessObjectId: templateForm.structure?.businessObjectId ?? undefined,
    primaryKey: optionalJsonObject(parseJson<TablePrimaryKeyStandard | null>(templateJson.primaryKey, null, '主键字段')),
    uniqueKeys: optionalJsonArray(parseJson<TableUniqueKeyStandard[] | null>(templateJson.uniqueKeys, [], '唯一键 JSON')),
    indexes: optionalJsonArray(parseJson<TableIndexStandard[] | null>(templateJson.indexes, [], '索引 JSON')),
    foreignKeys: optionalJsonArray(parseJson<TableForeignKeyStandard[] | null>(templateJson.foreignKeys, [], '外键 JSON')),
    checkHints: optionalJsonArray(parseJson<string[] | null>(templateJson.checkHints, [], 'Check 提示 JSON')),
    auditPolicy: optionalJsonObject(parseJson<TableAuditPolicy | null>(templateJson.auditPolicy, null, '审计策略 JSON')),
    softDeletePolicy: optionalJsonObject(parseJson<TableSoftDeletePolicy | null>(templateJson.softDeletePolicy, null, '软删除策略 JSON')),
    dialectNotes: optionalJsonArray(parseJson<string[] | null>(templateJson.dialectNotes, [], '方言说明')),
    aiUsageNotes: templateForm.structure?.aiUsageNotes?.trim() || undefined
  }
  return {
    projectId,
    name: templateForm.name?.trim() || '',
    description: templateForm.description?.trim() || undefined,
    tablePrefix: templateForm.tablePrefix?.trim() || undefined,
    structure
  }
}

function buildBusinessObjectPayload(projectId: number): BusinessObjectStandardReq {
  return {
    projectId,
    objectKey: businessObjectForm.objectKey?.trim() || '',
    entityName: businessObjectForm.entityName?.trim() || '',
    tablePattern: businessObjectForm.tablePattern?.trim() || undefined,
    templateId: businessObjectForm.templateId ?? undefined,
    requiredFields: optionalJsonArray(parseJson<string[] | null>(objectJson.requiredFields, [], '必选字段 JSON')),
    optionalFields: optionalJsonArray(parseJson<string[] | null>(objectJson.optionalFields, [], '可选字段 JSON')),
    relations: optionalJsonArray(parseJson<TableRelationHint[] | null>(objectJson.relations, [], '关系 JSON')),
    foreignKeyHints: optionalJsonArray(parseJson<TableForeignKeyStandard[] | null>(objectJson.foreignKeyHints, [], '外键提示 JSON')),
    auditFields: optionalJsonObject(parseJson<TableAuditPolicy | null>(objectJson.auditFields, null, '审计字段 JSON')),
    commonPitfalls: optionalJsonArray(parseJson<string[] | null>(objectJson.commonPitfalls, [], '常见反模式 JSON')),
    aiUsageNotes: businessObjectForm.aiUsageNotes?.trim() || undefined,
    contextExport: businessObjectForm.contextExport ?? true,
    status: businessObjectForm.status || 'ENABLED'
  }
}

function assignTemplate(template: TemplateResp) {
  Object.assign(templateForm, {
    ...emptyTemplate(projectStore.currentProjectId ?? undefined),
    ...template,
    structure: normalizeStructure(template.structure)
  })
  syncTemplateJson(templateForm.structure)
}

function assignBusinessObject(item: BusinessObjectStandard) {
  Object.assign(businessObjectForm, {
    ...emptyBusinessObject(projectStore.currentProjectId ?? undefined),
    ...item
  })
  syncBusinessObjectJson(businessObjectForm)
}

function resetForms(projectId?: number) {
  selectedTemplateId.value = null
  selectedBusinessObjectId.value = null
  assignTemplate(emptyTemplate(projectId))
  assignBusinessObject(emptyBusinessObject(projectId))
}

function syncTemplateJson(structure?: TableStructureStandard | null) {
  const value = normalizeStructure(structure)
  templateJson.primaryKey = toPrettyJson(value.primaryKey ?? null)
  templateJson.uniqueKeys = toPrettyJson(value.uniqueKeys ?? [])
  templateJson.indexes = toPrettyJson(value.indexes ?? [])
  templateJson.foreignKeys = toPrettyJson(value.foreignKeys ?? [])
  templateJson.checkHints = toPrettyJson(value.checkHints ?? [])
  templateJson.auditPolicy = toPrettyJson(value.auditPolicy ?? null)
  templateJson.softDeletePolicy = toPrettyJson(value.softDeletePolicy ?? null)
  templateJson.dialectNotes = toPrettyJson(value.dialectNotes ?? [])
}

function syncBusinessObjectJson(item: BusinessObjectStandard) {
  objectJson.requiredFields = toPrettyJson(item.requiredFields ?? [])
  objectJson.optionalFields = toPrettyJson(item.optionalFields ?? [])
  objectJson.relations = toPrettyJson(item.relations ?? [])
  objectJson.foreignKeyHints = toPrettyJson(item.foreignKeyHints ?? [])
  objectJson.auditFields = toPrettyJson(item.auditFields ?? null)
  objectJson.commonPitfalls = toPrettyJson(item.commonPitfalls ?? [])
}

function normalizeStructure(structure?: TableStructureStandard | null): TableStructureStandard {
  return {
    businessObjectId: structure?.businessObjectId ?? undefined,
    primaryKey: structure?.primaryKey ?? undefined,
    uniqueKeys: structure?.uniqueKeys ?? [],
    indexes: structure?.indexes ?? [],
    foreignKeys: structure?.foreignKeys ?? [],
    checkHints: structure?.checkHints ?? [],
    auditPolicy: structure?.auditPolicy ?? undefined,
    softDeletePolicy: structure?.softDeletePolicy ?? undefined,
    dialectNotes: structure?.dialectNotes ?? [],
    aiUsageNotes: structure?.aiUsageNotes ?? ''
  }
}

function emptyTemplate(projectId?: number): TemplateEditorForm {
  return {
    projectId,
    name: '',
    description: '',
    tablePrefix: '',
    structure: normalizeStructure()
  }
}

function emptyBusinessObject(projectId?: number): BusinessObjectStandard {
  return {
    projectId,
    objectKey: '',
    entityName: '',
    tablePattern: '',
    templateId: undefined,
    requiredFields: [],
    optionalFields: [],
    relations: [],
    foreignKeyHints: [],
    auditFields: undefined,
    commonPitfalls: [],
    aiUsageNotes: '',
    contextExport: true,
    status: 'ENABLED'
  }
}

function parseJson<T>(text: string, fallback: T, label: string): T {
  const normalized = text.trim()
  if (!normalized) {
    return fallback
  }
  try {
    return JSON.parse(text) as T
  } catch {
    throw new Error(`${label} 格式不正确`)
  }
}

function optionalJsonObject<T>(value: T | null | undefined): T | undefined {
  return value ?? undefined
}

function optionalJsonArray<T>(value: T[] | null | undefined): T[] | undefined {
  return value ?? []
}

function toPrettyJson(value: unknown) {
  return JSON.stringify(value, null, 2)
}

function businessObjectName(id?: number | null) {
  if (!id) {
    return '未关联'
  }
  const item = businessObjects.value.find((object) => object.id === id)
  return item?.entityName || item?.objectKey || `业务对象 ${id}`
}
</script>

<style scoped>
.template-page {
  min-height: calc(100vh - 140px);
  padding: 20px;
  border-radius: 4px;
  background: #fff;
}

.page-header,
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.page-header h2,
.section-header h3 {
  margin: 0;
}

.page-subtitle,
.muted-text {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.workbench-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.82fr) minmax(420px, 1.2fr) minmax(380px, 1fr);
  gap: 16px;
  align-items: start;
}

.list-panel,
.editor-panel {
  min-width: 0;
  padding: 14px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
}

.object-header,
.field-section {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.dense-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.form-grid,
.json-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 12px;
}

.json-grid :deep(.el-form-item),
.dense-form :deep(.el-form-item:last-child) {
  align-items: flex-start;
}

.dense-form h4 {
  margin: 4px 0 12px;
  color: #303133;
  font-size: 14px;
}

.full-width {
  width: 100%;
}

.dense-table {
  width: 100%;
}

@media (max-width: 1280px) {
  .workbench-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .template-page {
    padding: 12px;
  }

  .page-header,
  .section-header {
    flex-direction: column;
  }

  .form-grid,
  .json-grid {
    grid-template-columns: 1fr;
  }
}
</style>
