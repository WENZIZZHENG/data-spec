<template>
  <div class="standard-candidate-page">
    <div class="page-header">
      <div>
        <h2>标准候选</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button :disabled="!hasProject" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建候选
        </el-button>
        <el-button :disabled="!hasProject" :loading="loading" @click="loadCandidates">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div class="candidate-toolbar">
        <el-select v-model="statusFilter" class="filter-select" @change="resetAndLoad">
          <el-option label="全部状态" value="ALL" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="已延后" value="POSTPONED" />
          <el-option label="已采纳" value="ACCEPTED" />
          <el-option label="已合并" value="MERGED" />
          <el-option label="已忽略" value="IGNORED" />
        </el-select>
        <el-select v-model="sourceFilter" class="filter-select" @change="resetAndLoad">
          <el-option label="全部来源" value="ALL" />
          <el-option label="手动" value="MANUAL" />
          <el-option label="覆盖率" value="COVERAGE" />
          <el-option label="反向导入" value="REVERSE_IMPORT" />
          <el-option label="AI 反馈" value="AI_FEEDBACK" />
        </el-select>
        <el-input
          v-model="keyword"
          clearable
          class="keyword-input"
          placeholder="搜索字段名、显示名、注释"
          @clear="resetAndLoad"
          @keyup.enter="resetAndLoad"
        />
        <el-button @click="resetAndLoad">搜索</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="candidates"
        stripe
        class="candidate-table"
        empty-text="暂无标准候选"
      >
        <el-table-column label="状态" width="100" fixed="left">
          <template #default="{ row }">
            <el-tag :type="standardCandidateStatusTag(row.status)" size="small">
              {{ standardCandidateStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="候选字段" min-width="190" fixed="left">
          <template #default="{ row }">
            <div class="field-cell">
              <strong>{{ row.candidateName }}</strong>
              <small>{{ row.displayName || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dataType" label="类型" width="120" />
        <el-table-column prop="comment" label="注释" min-width="200" show-overflow-tooltip />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ standardCandidateSourceLabel(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column label="置信度" width="150">
          <template #default="{ row }">
            <div class="confidence-cell">
              <span>{{ row.confidence ?? 0 }}</span>
              <el-progress :percentage="row.confidence ?? 0" :show-text="false" :stroke-width="8" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="证据" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ formatCandidateEvidence(row.evidenceJson) }}</template>
        </el-table-column>
        <el-table-column label="决策" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.decisionReason || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                size="small"
                text
                type="primary"
                :disabled="!isStandardCandidateDecidable(row)"
                @click="openDecision('accept', row)"
              >
                采纳
              </el-button>
              <el-button
                size="small"
                text
                type="primary"
                :disabled="!isStandardCandidateDecidable(row)"
                @click="openMerge(row)"
              >
                合并
              </el-button>
              <el-button
                size="small"
                text
                :disabled="!isStandardCandidateDecidable(row)"
                @click="openDecision('postpone', row)"
              >
                延后
              </el-button>
              <el-button
                size="small"
                text
                type="danger"
                :disabled="!isStandardCandidateDecidable(row)"
                @click="openDecision('ignore', row)"
              >
                忽略
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="loadCandidates"
          @size-change="resetAndLoad"
        />
      </div>
    </template>

    <el-dialog v-model="createVisible" title="新建候选" width="520px">
      <el-form label-width="90px">
        <el-form-item label="字段名" required>
          <el-input v-model="createForm.candidateName" placeholder="user_id" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="createForm.displayName" placeholder="用户ID" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-input v-model="createForm.dataType" placeholder="bigint" />
        </el-form-item>
        <el-form-item label="注释">
          <el-input v-model="createForm.comment" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="来源" required>
          <el-select v-model="createForm.sourceType">
            <el-option label="手动" value="MANUAL" />
            <el-option label="覆盖率" value="COVERAGE" />
            <el-option label="反向导入" value="REVERSE_IMPORT" />
            <el-option label="AI 反馈" value="AI_FEEDBACK" />
          </el-select>
        </el-form-item>
        <el-form-item label="证据">
          <el-input v-model="createForm.evidenceJson" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="置信度">
          <el-slider v-model="createForm.confidence" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="decisionVisible" :title="decisionTitle" width="460px">
      <el-form label-width="90px">
        <el-form-item label="候选字段">
          <el-input :model-value="activeCandidate?.candidateName || '-'" disabled />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="decisionReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decisionVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDecision">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="mergeVisible" title="合并候选" width="520px">
      <el-form label-width="90px">
        <el-form-item label="候选字段">
          <el-input :model-value="activeCandidate?.candidateName || '-'" disabled />
        </el-form-item>
        <el-form-item label="目标字段" required>
          <el-select v-model="mergeTargetFieldId" filterable class="field-select" placeholder="选择已有字段">
            <el-option
              v-for="field in fieldsWithId"
              :key="field.id"
              :label="`${field.name} ${field.displayName || ''}`"
              :value="field.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="mergeReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mergeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMerge">确认合并</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listFields } from '@/api/field'
import {
  acceptStandardCandidate,
  createStandardCandidate,
  ignoreStandardCandidate,
  listStandardCandidates,
  mergeStandardCandidate,
  postponeStandardCandidate
} from '@/api/standardCandidate'
import { useProjectStore } from '@/stores/project'
import {
  formatCandidateEvidence,
  isStandardCandidateDecidable,
  standardCandidateSourceLabel,
  standardCandidateStatusLabel,
  standardCandidateStatusTag
} from '@/utils/standardCandidateDisplay'
import type { Field, StandardCandidate, StandardCandidateCreateReq } from '@/types'

type DecisionMode = 'accept' | 'ignore' | 'postpone'

const projectStore = useProjectStore()
const route = useRoute()
const hasProject = computed(() => Boolean(projectStore.currentProjectId))

const loading = ref(false)
const submitting = ref(false)
const candidates = ref<StandardCandidate[]>([])
const fields = ref<Field[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const statusFilter = ref('PENDING')
const sourceFilter = ref('ALL')
const keyword = ref(keywordFromQuery())

const createVisible = ref(false)
const decisionVisible = ref(false)
const mergeVisible = ref(false)
const activeCandidate = ref<StandardCandidate | null>(null)
const decisionMode = ref<DecisionMode>('accept')
const decisionReason = ref('')
const mergeTargetFieldId = ref<number | null>(null)
const mergeReason = ref('')
const fieldsWithId = computed(() =>
  fields.value.filter((field): field is Field & { id: number } => typeof field.id === 'number')
)

const createForm = reactive<StandardCandidateCreateReq>({
  projectId: 0,
  candidateName: '',
  displayName: '',
  dataType: 'varchar',
  comment: '',
  sourceType: 'MANUAL',
  sourceRef: '',
  evidenceJson: '',
  confidence: 50
})

const decisionTitle = computed(() => {
  if (decisionMode.value === 'accept') {
    return '采纳候选'
  }
  if (decisionMode.value === 'ignore') {
    return '忽略候选'
  }
  return '延后候选'
})

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    current.value = 1
    candidates.value = []
    fields.value = []
    createVisible.value = false
    decisionVisible.value = false
    mergeVisible.value = false
    activeCandidate.value = null
    void loadCandidates()
  },
  { immediate: true }
)

watch(
  () => route.query.keyword,
  () => {
    applyKeywordFromQuery()
    resetAndLoad()
  }
)

async function loadCandidates() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    candidates.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await listStandardCandidates({
      projectId,
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      sourceType: sourceFilter.value === 'ALL' ? undefined : sourceFilter.value,
      keyword: keyword.value || undefined,
      current: current.value,
      size: size.value
    })
    candidates.value = result.records ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  current.value = 1
  void loadCandidates()
}

function applyKeywordFromQuery() {
  keyword.value = keywordFromQuery()
}

function keywordFromQuery() {
  const rawKeyword = route.query.keyword
  const value = Array.isArray(rawKeyword) ? rawKeyword[0] : rawKeyword
  if (typeof value === 'string') {
    return value
  }
  return ''
}

function openCreate() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    return
  }
  Object.assign(createForm, {
    projectId,
    candidateName: '',
    displayName: '',
    dataType: 'varchar',
    comment: '',
    sourceType: 'MANUAL',
    sourceRef: '',
    evidenceJson: '',
    confidence: 50
  })
  createVisible.value = true
}

async function submitCreate() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    ElMessage.error('请先选择项目')
    return
  }
  submitting.value = true
  try {
    await createStandardCandidate({ ...createForm, projectId })
    ElMessage.success('候选已创建')
    createVisible.value = false
    resetAndLoad()
  } finally {
    submitting.value = false
  }
}

function openDecision(mode: DecisionMode, candidate: StandardCandidate) {
  activeCandidate.value = candidate
  decisionMode.value = mode
  decisionReason.value = ''
  decisionVisible.value = true
}

async function submitDecision() {
  const candidateId = activeCandidate.value?.id
  if (!candidateId) {
    return
  }
  submitting.value = true
  try {
    const payload = { reason: decisionReason.value }
    if (decisionMode.value === 'accept') {
      await acceptStandardCandidate(candidateId, payload)
      fields.value = []
    } else if (decisionMode.value === 'ignore') {
      await ignoreStandardCandidate(candidateId, payload)
    } else {
      await postponeStandardCandidate(candidateId, payload)
    }
    ElMessage.success('候选已更新')
    decisionVisible.value = false
    await loadCandidates()
  } finally {
    submitting.value = false
  }
}

async function openMerge(candidate: StandardCandidate) {
  activeCandidate.value = candidate
  mergeTargetFieldId.value = null
  mergeReason.value = ''
  await loadFields()
  mergeVisible.value = true
}

async function loadFields() {
  const projectId = projectStore.currentProjectId
  if (!projectId || fields.value.length > 0) {
    return
  }
  fields.value = await listFields(projectId)
}

async function submitMerge() {
  const candidateId = activeCandidate.value?.id
  if (!candidateId || !mergeTargetFieldId.value) {
    ElMessage.error('请选择目标字段')
    return
  }
  submitting.value = true
  try {
    await mergeStandardCandidate(candidateId, {
      targetFieldId: mergeTargetFieldId.value,
      reason: mergeReason.value
    })
    ElMessage.success('候选已合并')
    mergeVisible.value = false
    await loadCandidates()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.standard-candidate-page {
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
.candidate-toolbar,
.table-actions,
.pagination-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.candidate-toolbar {
  margin-bottom: 12px;
}

.filter-select {
  width: 150px;
}

.keyword-input {
  width: 280px;
}

.field-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-cell small {
  color: #6b7280;
}

.confidence-cell {
  display: grid;
  grid-template-columns: 36px 1fr;
  align-items: center;
  gap: 8px;
}

.candidate-table {
  width: 100%;
}

.pagination-row {
  justify-content: flex-end;
  margin-top: 12px;
}

.field-select {
  width: 100%;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }

  .keyword-input {
    width: 100%;
  }
}
</style>
