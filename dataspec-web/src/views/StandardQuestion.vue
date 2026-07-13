<template>
  <div class="standard-question-page">
    <div class="page-header">
      <div>
        <h2>标准问答</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!answer" @click="handleCopyAnswer">
          <el-icon><DocumentCopy /></el-icon>
          复制答案
        </el-button>
      </div>
    </div>

    <ProjectRequired
      v-if="!hasProject"
      :has-project="hasProject"
      title="请先创建并选择项目"
      @action="goProjects"
    />

    <template v-else>
      <section class="question-panel">
        <el-form label-position="top" @submit.prevent="handleAsk">
          <el-form-item label="问题">
            <el-input
              v-model="question"
              type="textarea"
              :rows="3"
              maxlength="240"
              show-word-limit
              placeholder="手机号标准字段叫什么"
              @keydown.enter.ctrl.prevent="handleAsk"
            />
          </el-form-item>
          <div class="question-actions">
            <div class="question-presets" aria-label="常用问题">
              <el-button
                v-for="preset in questionPresets"
                :key="preset"
                size="small"
                plain
                @click="askPreset(preset)"
              >
                {{ preset }}
              </el-button>
            </div>
            <el-button type="primary" :loading="loading" @click="handleAsk">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </div>
        </el-form>
      </section>

      <StateBlock
        v-if="errorMessage"
        type="error"
        title="标准问答查询失败"
        :description="errorMessage"
        suggested-action="检查后端服务、项目选择和字段标准后重试。"
        action-text="重试"
        @action="handleAsk"
      />

      <StateBlock
        v-else-if="!answer"
        type="empty"
        title="暂无问答结果"
        description="输入字段命名、单位、敏感标记或生命周期问题后，这里会展示只读答案和证据。"
      />

      <template v-else>
        <section class="answer-panel" :data-confidence="answer.confidence">
          <div class="answer-heading">
            <h3>答案</h3>
            <div class="answer-tags">
              <el-tag :type="answerStatusTagType(answer.answerStatus)" effect="plain">
                {{ answerStatusText(answer.answerStatus) }}
              </el-tag>
              <el-tag type="info" effect="plain">
                {{ answerabilityText(answer.answerability) }}
              </el-tag>
              <el-tag :type="confidenceTagType(answer.confidence)" effect="plain">
                置信度：{{ confidenceText(answer.confidence) }}
              </el-tag>
            </div>
          </div>
          <p class="answer-text">{{ answer.answer }}</p>
          <p class="confidence-reason">{{ answer.confidenceReason }}</p>
          <div v-if="answer.unresolvedQuestions.length" class="unresolved-list">
            <div v-for="item in answer.unresolvedQuestions" :key="item">{{ item }}</div>
          </div>
          <div v-if="answer.missingEvidence.length || answer.conflicts.length || answer.suggestedNextQuery" class="answerability-grid">
            <div v-if="answer.missingEvidence.length" class="answerability-block">
              <strong>缺失证据</strong>
              <ul>
                <li v-for="item in answer.missingEvidence" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div v-if="answer.conflicts.length" class="answerability-block">
              <strong>标准冲突</strong>
              <ul>
                <li v-for="item in answer.conflicts" :key="item.message">{{ item.message }}</li>
              </ul>
            </div>
            <div v-if="answer.suggestedNextQuery" class="answerability-block">
              <strong>建议追问</strong>
              <p>{{ answer.suggestedNextQuery }}</p>
            </div>
          </div>
        </section>

        <section class="result-grid">
          <div class="result-section">
            <div class="section-header">
              <h3>匹配字段</h3>
              <el-tag type="info">{{ answer.matchedFields.length }}</el-tag>
            </div>
            <el-table :data="answer.matchedFields" stripe empty-text="暂无字段证据">
              <el-table-column prop="name" label="字段名" min-width="150" />
              <el-table-column prop="displayName" label="显示名" min-width="120" />
              <el-table-column prop="dataType" label="类型" min-width="120" />
              <el-table-column label="状态" width="88">
                <template #default="{ row }">{{ statusText(row.status) }}</template>
              </el-table-column>
              <el-table-column label="敏感" width="78">
                <template #default="{ row }">
                  <el-tag :type="row.sensitive ? 'danger' : 'info'" size="small">
                    {{ row.sensitive ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="result-section">
            <div class="section-header">
              <h3>证据引用</h3>
              <el-tag type="info">{{ answer.evidence.length }}</el-tag>
            </div>
            <div class="evidence-list">
              <article v-for="item in answer.evidence" :key="`${item.type}-${item.title}-${item.ref ?? ''}`" class="evidence-item">
                <el-tag size="small" effect="plain">{{ evidenceTypeText(item.type) }}</el-tag>
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.description || '-' }}</p>
                  <small v-if="item.ref">{{ item.ref }}</small>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section class="result-section">
          <div class="section-header">
            <h3>下一步动作</h3>
          </div>
          <ul class="next-actions">
            <li v-for="item in answer.suggestedNextActions" :key="item">{{ item }}</li>
          </ul>
        </section>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Search } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { searchFields } from '@/api/field'
import { listRuleConfigs } from '@/api/rule'
import ProjectRequired from '@/components/ProjectRequired.vue'
import StateBlock from '@/components/StateBlock.vue'
import { useProjectStore } from '@/stores/project'
import { readStringQuery, replaceRouteQuery } from '@/utils/urlState'
import {
  buildStandardQuestionAnswer,
  buildStandardQuestionMarkdown,
  answerabilityText,
  answerStatusText,
  confidenceText,
  createStandardQuestionRequestGuard,
  evidenceTypeText,
  statusText,
  type StandardQuestionAnswer,
  type StandardQuestionAnswerStatus,
  type StandardQuestionConfidence
} from '@/utils/standardQuestion'

const projectStore = useProjectStore()
const route = useRoute()
const router = useRouter()
const question = ref(readStringQuery(route.query, 'q'))
const answer = ref<StandardQuestionAnswer | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const requestGuard = createStandardQuestionRequestGuard()

const questionPresets = [
  '手机号标准字段叫什么',
  '订单金额应该用什么单位',
  '这个字段是否已废弃'
]

const hasProject = computed(() => Boolean(projectStore.currentProjectId))

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  if (question.value.trim()) {
    await handleAsk()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    requestGuard.invalidate()
    answer.value = null
    errorMessage.value = ''
    loading.value = false
  }
)

async function handleAsk() {
  const projectId = projectStore.currentProjectId
  const normalizedQuestion = question.value.trim()
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  if (!normalizedQuestion) {
    ElMessage.warning('请输入问题')
    return
  }
  const requestSnapshot = requestGuard.begin(projectId, normalizedQuestion)
  loading.value = true
  errorMessage.value = ''
  try {
    await replaceRouteQuery(router, route, { q: normalizedQuestion })
    const [fieldSearch, rules] = await Promise.all([
      searchFields({ projectId, query: normalizedQuestion, limit: 8 }),
      listRuleConfigs(projectId)
    ])
    const nextAnswer = buildStandardQuestionAnswer({
      question: normalizedQuestion,
      fieldSearch,
      rules
    })
    if (requestGuard.isCurrent(requestSnapshot, projectStore.currentProjectId, question.value)) {
      answer.value = nextAnswer
    }
  } catch (error) {
    if (requestGuard.isCurrent(requestSnapshot, projectStore.currentProjectId, question.value)) {
      errorMessage.value = error instanceof Error ? error.message : '查询失败'
    }
  } finally {
    if (requestGuard.isCurrent(requestSnapshot, projectStore.currentProjectId, question.value)) {
      loading.value = false
    }
  }
}

function askPreset(value: string) {
  question.value = value
  void handleAsk()
}

async function handleCopyAnswer() {
  if (!answer.value) {
    return
  }
  try {
    if (!navigator.clipboard?.writeText) {
      throw new Error('clipboard unavailable')
    }
    await navigator.clipboard.writeText(buildStandardQuestionMarkdown(answer.value))
    ElMessage.success('已复制答案')
  } catch {
    ElMessage.error('复制失败，请手动选择答案复制')
  }
}

async function goProjects() {
  await router.push('/projects')
}

function confidenceTagType(confidence: StandardQuestionConfidence) {
  if (confidence === 'HIGH') {
    return 'success'
  }
  if (confidence === 'MEDIUM') {
    return 'warning'
  }
  return 'info'
}

function answerStatusTagType(status: StandardQuestionAnswerStatus) {
  if (status === 'ADOPTABLE') {
    return 'success'
  }
  if (status === 'NEEDS_CONFIRMATION') {
    return 'warning'
  }
  return 'danger'
}
</script>

<style scoped>
.standard-question-page {
  max-width: 1200px;
  margin: 0 auto;
}

.question-panel,
.answer-panel,
.result-section {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.question-actions,
.answer-heading,
.section-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.question-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.answer-text {
  margin: 10px 0 0;
  color: #111827;
  line-height: 1.7;
}

.answer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.confidence-reason {
  margin: 8px 0 0;
  color: #4b5563;
  line-height: 1.6;
}

.unresolved-list {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #fff7ed;
  color: #9a3412;
  line-height: 1.6;
}

.answerability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
}

.answerability-block {
  padding-left: 10px;
  border-left: 3px solid #d1d5db;
  color: #374151;
}

.answerability-block strong {
  display: block;
  margin-bottom: 6px;
  color: #111827;
}

.answerability-block ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.6;
}

.answerability-block p {
  margin: 0;
  line-height: 1.6;
}

.result-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.evidence-list {
  display: grid;
  gap: 10px;
}

.evidence-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #eef2f7;
}

.evidence-item p {
  margin: 4px 0;
  color: #4b5563;
  line-height: 1.6;
}

.evidence-item small {
  color: #64748b;
}

.next-actions {
  margin: 0;
  padding-left: 20px;
  color: #374151;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .question-actions,
  .answer-heading,
  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .result-grid {
    grid-template-columns: 1fr;
  }

  .answer-tags {
    justify-content: flex-start;
  }

  .answerability-grid {
    grid-template-columns: 1fr;
  }
}
</style>
