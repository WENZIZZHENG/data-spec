<template>
  <div class="requirement-draft-page">
    <div class="page-header">
      <div>
        <h2>需求草案</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!hasProject" :loading="loading" @click="handleDraft">
          <el-icon><MagicStick /></el-icon>
          生成草案
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <section class="draft-input-section">
        <el-form class="draft-form" label-width="96px" @submit.prevent>
          <el-form-item label="业务描述" required>
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="会员支付流水表，记录会员、支付金额、支付状态、第三方流水号"
            />
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="目标表名" required>
              <el-input v-model="form.targetTableName" clearable placeholder="pay_trade" />
            </el-form-item>
            <el-form-item label="分组提示">
              <el-input v-model="form.groupHint" clearable placeholder="payment" />
            </el-form-item>
            <el-form-item label="字段上限">
              <el-input-number v-model="form.limit" :min="3" :max="20" />
            </el-form-item>
          </div>
          <div class="form-actions">
            <el-button type="primary" :loading="loading" @click="handleDraft">
              <el-icon><MagicStick /></el-icon>
              生成草案
            </el-button>
            <el-button :disabled="!result?.copyablePrompt" @click="copyPrompt">
              <el-icon><CopyDocument /></el-icon>
              复制 Prompt
            </el-button>
          </div>
        </el-form>
      </section>

      <section v-if="result" class="result-section">
        <div class="result-summary">
          <el-statistic title="标准字段" :value="result.matchedFields?.length ?? 0" />
          <el-statistic title="缺失候选" :value="result.missingCandidates?.length ?? 0" />
          <el-statistic title="歧义词" :value="result.ambiguousTerms?.length ?? 0" />
          <el-statistic title="模板分" :value="result.recommendedTemplate?.score ?? 0" />
        </div>

        <div class="template-panel">
          <div>
            <h3>推荐模板</h3>
            <p>{{ result.recommendedTemplate?.name || '未命中模板' }}</p>
          </div>
          <div class="template-actions">
            <el-tag v-if="result.recommendedTemplate" type="success" effect="plain">
              {{ result.recommendedTemplate.score ?? 0 }}
            </el-tag>
            <el-button
              type="primary"
              :disabled="!result.recommendedTemplate?.id"
              @click="openDdlPreview"
            >
              <el-icon><Document /></el-icon>
              DDL 预览
            </el-button>
          </div>
        </div>
        <div v-if="result.recommendedTemplate?.matchReasons?.length" class="reason-list">
          <el-tag
            v-for="reason in result.recommendedTemplate.matchReasons"
            :key="reason"
            size="small"
            effect="plain"
          >
            {{ reason }}
          </el-tag>
        </div>
        <div v-if="result.recommendedTemplate?.evidence?.length" class="evidence-section">
          <span class="evidence-title">证据来源</span>
          <div class="evidence-list">
            <el-tag
              v-for="trace in renderEvidenceTrace(result.recommendedTemplate.evidence)"
              :key="trace"
              size="small"
              effect="plain"
            >
              {{ trace }}
            </el-tag>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="draft-tabs">
          <el-tab-pane label="标准字段" name="fields">
            <el-table
              :data="result.matchedFields ?? []"
              stripe
              class="result-table"
              empty-text="暂无命中字段"
            >
              <el-table-column label="字段" min-width="180">
                <template #default="{ row }">
                  <div class="field-cell">
                    <strong>{{ row.field?.name || '-' }}</strong>
                    <small>{{ row.field?.displayName || '-' }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="130">
                <template #default="{ row }">{{ row.field?.dataType || '-' }}</template>
              </el-table-column>
              <el-table-column label="分数" width="110">
                <template #default="{ row }">
                  <el-progress :percentage="row.score ?? 0" :stroke-width="8" />
                </template>
              </el-table-column>
              <el-table-column label="采用" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.recommended ? 'success' : 'info'" size="small">
                    {{ row.recommended ? '建议' : '备选' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="原因" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ (row.matchReasons ?? []).join('；') || '-' }}</template>
              </el-table-column>
              <el-table-column label="证据来源" min-width="260" show-overflow-tooltip>
                <template #default="{ row }">
                  <div v-if="renderEvidenceTrace(row.evidence).length" class="evidence-list">
                    <el-tag
                      v-for="trace in renderEvidenceTrace(row.evidence)"
                      :key="trace"
                      size="small"
                      effect="plain"
                    >
                      {{ trace }}
                    </el-tag>
                  </div>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="缺失候选" name="missing">
            <el-table
              :data="result.missingCandidates ?? []"
              stripe
              class="result-table"
              empty-text="暂无缺失候选"
            >
              <el-table-column prop="candidateName" label="候选字段" min-width="180" />
              <el-table-column prop="displayName" label="显示名" width="140" />
              <el-table-column prop="dataType" label="类型" width="130" />
              <el-table-column label="置信度" width="140">
                <template #default="{ row }">
                  <el-progress :percentage="row.confidence ?? 0" :stroke-width="8" />
                </template>
              </el-table-column>
              <el-table-column prop="evidence" label="证据" min-width="260" show-overflow-tooltip />
              <el-table-column label="证据来源" min-width="260" show-overflow-tooltip>
                <template #default="{ row }">
                  <div v-if="renderEvidenceTrace(row.evidenceTrace).length" class="evidence-list">
                    <el-tag
                      v-for="trace in renderEvidenceTrace(row.evidenceTrace)"
                      :key="trace"
                      size="small"
                      effect="plain"
                    >
                      {{ trace }}
                    </el-tag>
                  </div>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" text type="primary" @click="copyCandidatePayload(row)">
                    复制 Payload
                  </el-button>
                  <el-button size="small" text @click="openCandidateInbox(row.candidateName)">
                    去 Inbox
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="歧义词" name="ambiguous">
            <el-empty
              v-if="(result.ambiguousTerms ?? []).length === 0"
              description="暂无歧义词"
            />
            <el-collapse v-else class="ambiguous-list">
              <el-collapse-item
                v-for="term in result.ambiguousTerms"
                :key="term.term"
                :name="term.term"
              >
                <template #title>
                  <span class="collapse-title">{{ term.term }}</span>
                  <el-tag size="small" type="warning">{{ term.candidates?.length ?? 0 }} 个候选</el-tag>
                </template>
                <p class="ambiguous-reason">{{ term.reason }}</p>
                <el-table :data="term.candidates ?? []" size="small" stripe>
                  <el-table-column label="字段" min-width="180">
                    <template #default="{ row }">
                      <strong>{{ row.field?.name || '-' }}</strong>
                      <small class="inline-small">{{ row.field?.displayName || '-' }}</small>
                    </template>
                  </el-table-column>
                  <el-table-column label="分数" width="120">
                    <template #default="{ row }">{{ row.score ?? 0 }}</template>
                  </el-table-column>
                  <el-table-column label="原因" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">{{ (row.matchReasons ?? []).join('；') || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="证据来源" min-width="260" show-overflow-tooltip>
                    <template #default="{ row }">
                      <div v-if="renderEvidenceTrace(row.evidence).length" class="evidence-list">
                        <el-tag
                          v-for="trace in renderEvidenceTrace(row.evidence)"
                          :key="trace"
                          size="small"
                          effect="plain"
                        >
                          {{ trace }}
                        </el-tag>
                      </div>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-collapse-item>
            </el-collapse>
          </el-tab-pane>

          <el-tab-pane label="Prompt" name="prompt">
            <div class="prompt-panel">
              <div class="prompt-actions">
                <el-button :disabled="!result.copyablePrompt" @click="copyPrompt">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
              <pre>{{ result.copyablePrompt }}</pre>
            </div>
          </el-tab-pane>

          <el-tab-pane label="下一步" name="actions">
            <ul class="action-list">
              <li v-for="action in result.nextActions ?? []" :key="action">{{ action }}</li>
            </ul>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CopyDocument, Document, MagicStick } from '@element-plus/icons-vue'
import { createRequirementDraft } from '@/api/requirementDraft'
import { useProjectStore } from '@/stores/project'
import type { ExplainTrace, RequirementDraftResult, RequirementMissingCandidate } from '@/types'

const router = useRouter()
const projectStore = useProjectStore()
const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const loading = ref(false)
const result = ref<RequirementDraftResult | null>(null)
const activeTab = ref('fields')

const form = reactive({
  description: '',
  targetTableName: '',
  groupHint: '',
  limit: 10
})

async function handleDraft() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  if (!form.description.trim()) {
    ElMessage.warning('请输入业务描述')
    return
  }
  if (!/^[a-z][a-z0-9_]*$/.test(form.targetTableName.trim())) {
    ElMessage.warning('表名需使用 snake_case，例如 pay_trade')
    return
  }
  loading.value = true
  try {
    result.value = await createRequirementDraft({
      projectId,
      description: form.description.trim(),
      targetTableName: form.targetTableName.trim(),
      groupHint: form.groupHint.trim() || undefined,
      limit: form.limit
    })
    activeTab.value = 'fields'
    ElMessage.success('草案已生成')
  } finally {
    loading.value = false
  }
}

async function copyPrompt() {
  if (!result.value?.copyablePrompt) {
    return
  }
  await copyText(result.value.copyablePrompt, 'Prompt 已复制')
}

async function copyCandidatePayload(candidate: RequirementMissingCandidate) {
  if (!candidate.inboxPayload) {
    return
  }
  await copyText(JSON.stringify(candidate.inboxPayload, null, 2), '候选 Payload 已复制')
}

async function copyText(text: string, message: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(message)
  } catch {
    ElMessage.error('复制失败，请手动选择文本')
  }
}

function renderEvidenceTrace(evidence?: ExplainTrace[]) {
  return (evidence ?? [])
    .filter((trace) => trace?.sourceType || trace?.matchReason || trace?.ruleCode)
    .map((trace) => {
      const sourceId = typeof trace.sourceId === 'number' ? `#${trace.sourceId}` : ''
      const source = `${trace.sourceType || 'UNKNOWN'}${sourceId}`
      const confidence = typeof trace.confidence === 'number' ? ` · ${trace.confidence}` : ''
      const reason = trace.matchReason || trace.ruleCode || trace.docsRef || '证据'
      return `${source}${confidence} · ${reason}`
    })
}

function openDdlPreview() {
  const templateId = result.value?.recommendedTemplate?.id
  const tableName = result.value?.targetTableName || form.targetTableName.trim()
  if (!templateId || !tableName) {
    return
  }
  void router.push({
    path: '/generator',
    query: { templateId: String(templateId), tableName }
  })
}

function openCandidateInbox(candidateName?: string) {
  void router.push({
    path: '/standard-candidates',
    query: candidateName ? { keyword: candidateName } : undefined
  })
}
</script>

<style scoped>
.requirement-draft-page {
  max-width: 1500px;
  margin: 0 auto;
  padding: 20px;
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
  color: #6b7280;
}

.header-actions,
.form-actions,
.template-actions,
.prompt-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.draft-input-section,
.result-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 18px;
}

.draft-input-section {
  margin-bottom: 16px;
}

.draft-form {
  max-width: 1100px;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr) 180px;
  gap: 12px;
}

.form-actions {
  margin-left: 96px;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.result-summary :deep(.el-statistic) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}

.template-panel {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 12px;
}

.template-panel h3 {
  margin: 0 0 6px;
}

.template-panel p {
  margin: 0;
  color: #606266;
}

.reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.evidence-section {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 12px 0;
}

.evidence-title {
  flex: 0 0 auto;
  color: #606266;
  font-size: 13px;
  line-height: 24px;
}

.evidence-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.draft-tabs {
  margin-top: 14px;
}

.result-table {
  width: 100%;
}

.field-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-cell small,
.inline-small {
  color: #909399;
}

.inline-small {
  margin-left: 8px;
}

.ambiguous-list {
  border-top: none;
}

.collapse-title {
  margin-right: 8px;
  font-weight: 600;
}

.ambiguous-reason {
  margin: 0 0 12px;
  color: #606266;
}

.prompt-panel {
  position: relative;
}

.prompt-actions {
  justify-content: flex-end;
  margin-bottom: 8px;
}

.prompt-panel pre {
  min-height: 260px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f8fafc;
  color: #1f2937;
  white-space: pre-wrap;
}

.action-list {
  margin: 0;
  padding-left: 18px;
  color: #303133;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .page-header,
  .template-panel {
    flex-direction: column;
  }

  .form-grid,
  .result-summary {
    grid-template-columns: 1fr;
  }

  .form-actions {
    margin-left: 0;
  }
}
</style>
