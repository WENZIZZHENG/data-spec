<template>
  <el-dialog
    v-model="visible"
    class="token-evidence-dialog"
    :data-testid="stableTestIds.standardCandidates.tokenEvidenceDialog"
    title="命名证据候选"
    width="760px"
    align-center
    destroy-on-close
    :close-on-click-modal="!applying"
    :close-on-press-escape="!applying"
    :show-close="!applying"
    @open="handleOpen"
    @opened="focusCandidateName"
    @closed="handleClosed"
  >
    <div class="token-evidence-content">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        :disabled="applying"
      >
        <div class="evidence-form-grid">
          <el-form-item label="候选字段名" prop="candidateName">
            <el-input
              ref="candidateNameInput"
              v-model.trim="form.candidateName"
              :data-testid="stableTestIds.standardCandidates.candidateNameInput"
              maxlength="100"
              placeholder="order_amount"
              aria-label="命名证据候选字段名"
              autocomplete="off"
              autofocus
            />
          </el-form-item>
          <el-form-item label="显示名" prop="displayName">
            <el-input
              v-model.trim="form.displayName"
              maxlength="100"
              placeholder="订单金额"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="数据类型" prop="dataType">
            <el-input
              v-model.trim="form.dataType"
              maxlength="50"
              placeholder="decimal(18,2)"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="来源引用" prop="sourceRef">
            <el-input
              v-model.trim="form.sourceRef"
              :data-testid="stableTestIds.standardCandidates.sourceRefInput"
              maxlength="300"
              placeholder="field:orders.ord_amt"
              aria-label="命名证据来源引用"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item class="full-row" label="候选说明" prop="comment">
            <el-input
              v-model.trim="form.comment"
              type="textarea"
              :rows="2"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
          <el-form-item class="full-row" label="解析文本" prop="sourceText">
            <el-input
              v-model.trim="form.sourceText"
              type="textarea"
              :rows="2"
              maxlength="512"
              show-word-limit
              placeholder="可选；为空时使用字段名和显示名"
            />
          </el-form-item>
        </div>
      </el-form>

      <el-alert
        v-if="requestError"
        type="warning"
        show-icon
        :closable="false"
        :title="requestError"
      />

      <section v-if="preview" class="preview-section" aria-live="polite">
        <el-alert
          show-icon
          :closable="false"
          :type="previewAlertType"
          :title="tokenEvidencePreviewStatusLabel(preview.status)"
        />

        <el-descriptions :column="2" border size="small" class="preview-summary">
          <el-descriptions-item label="候选字段">
            {{ preview.candidateName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="来源">
            {{ standardCandidateSourceLabel(preview.sourceType) }}
          </el-descriptions-item>
          <el-descriptions-item label="来源引用">
            {{ preview.sourceRef || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="既有候选">
            {{ preview.duplicateCandidateId ? `#${preview.duplicateCandidateId}` : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="preview.signals?.length" class="signal-section">
          <h3>命名信号</h3>
          <ul class="signal-list">
            <li
              v-for="(signal, index) in preview.signals"
              :key="`${signal.signalType}-${signal.tokenEvidence.normalizedToken || index}`"
              class="signal-item"
            >
              <div class="signal-heading">
                <strong>{{ signal.tokenEvidence.token || '-' }}</strong>
                <div class="signal-tags">
                  <el-tag :type="signalTagType(signal.signalType)" size="small">
                    {{ tokenEvidenceSignalLabel(signal.signalType) }}
                  </el-tag>
                  <el-tag type="info" effect="plain" size="small">
                    {{ tokenEvidenceResolutionLabel(signal.tokenEvidence.resolutionStatus) }}
                  </el-tag>
                </div>
              </div>
              <span class="signal-reason">{{ signal.tokenEvidence.reason || '-' }}</span>
            </li>
          </ul>
        </div>

        <el-checkbox
          v-if="preview.status === 'READY'"
          v-model="confirmed"
          class="confirmation-check"
          :data-testid="stableTestIds.standardCandidates.confirmCheckbox"
        >
          我已核对候选字段、来源引用和命名证据
        </el-checkbox>
      </section>
    </div>

    <template #footer>
      <el-button :disabled="applying" @click="visible = false">取消</el-button>
      <el-button
        :data-testid="stableTestIds.standardCandidates.previewButton"
        :loading="previewLoading"
        :disabled="applying"
        @click="loadPreview"
      >
        {{ preview ? '重新预览' : '生成预览' }}
      </el-button>
      <el-button
        type="primary"
        :data-testid="stableTestIds.standardCandidates.applyButton"
        :loading="applying"
        :disabled="!canApply"
        @click="applyCandidate"
      >
        确认写入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, TagProps } from 'element-plus'
import {
  applyTokenEvidenceCandidate,
  previewTokenEvidenceCandidate
} from '@/api/standardCandidate'
import { useDialogFocusReturn } from '@/composables/useDialogFocusReturn'
import {
  buildTokenEvidenceCandidateApplyReq,
  shouldHandleTokenEvidenceCandidateApplyResult,
  standardCandidateSourceLabel,
  tokenEvidencePreviewStatusLabel,
  tokenEvidenceResolutionLabel,
  tokenEvidenceSignalLabel
} from '@/utils/standardCandidateDisplay'
import { stableTestIds } from '@/utils/stableTestIds'
import type {
  TokenEvidenceCandidateApplyResult,
  TokenEvidenceCandidatePreview,
  TokenEvidenceCandidatePreviewReq,
  TokenEvidenceCandidateSignalType
} from '@/types'

const visible = defineModel<boolean>({ default: false })
const dialogFocus = useDialogFocusReturn(visible)

const props = defineProps<{
  /** 当前项目 ID；缺失时禁止发送项目级 preview/apply 请求。 */
  projectId?: number | null
}>()

const emit = defineEmits<{
  /** 命名证据候选完成幂等写入后触发，供工作台刷新现有 Inbox。 */
  applied: [result: TokenEvidenceCandidateApplyResult]
}>()

type FocusableInput = { focus: () => void }

const formRef = ref<FormInstance>()
const candidateNameInput = ref<FocusableInput | null>(null)
const preview = shallowRef<TokenEvidenceCandidatePreview | null>(null)
const previewInput = shallowRef<TokenEvidenceCandidatePreviewReq | null>(null)
const previewLoading = ref(false)
const applying = ref(false)
const confirmed = ref(false)
const requestError = ref('')
let previewRequestId = 0
let applyRequestId = 0

const form = reactive({
  projectId: 0,
  candidateName: '',
  displayName: '',
  dataType: 'varchar',
  comment: '',
  sourceRef: '',
  sourceText: ''
})

const rules: FormRules = {
  candidateName: [
    { required: true, message: '请输入候选字段名', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '字段名必须使用 snake_case', trigger: 'blur' }
  ],
  displayName: [{ max: 100, message: '显示名不能超过 100 个字符', trigger: 'blur' }],
  dataType: [{ required: true, message: '请输入数据类型', trigger: 'blur' }],
  comment: [{ max: 1000, message: '候选说明不能超过 1000 个字符', trigger: 'blur' }],
  sourceRef: [{ required: true, message: '请输入稳定来源引用', trigger: 'blur' }],
  sourceText: [{ max: 512, message: '解析文本不能超过 512 个字符', trigger: 'blur' }]
}

const canApply = computed(() => Boolean(
  previewInput.value
  && buildTokenEvidenceCandidateApplyReq(previewInput.value, preview.value, confirmed.value)
  && !previewLoading.value
  && !applying.value
))

const previewAlertType = computed<'success' | 'info' | 'warning'>(() => {
  if (preview.value?.status === 'READY') {
    return 'success'
  }
  if (preview.value?.status === 'NO_ACTIONABLE_SIGNAL') {
    return 'info'
  }
  return 'warning'
})

watch(
  () => [
    form.projectId,
    form.candidateName,
    form.displayName,
    form.dataType,
    form.comment,
    form.sourceRef,
    form.sourceText
  ],
  () => invalidatePreview()
)

watch(
  () => props.projectId,
  (projectId) => {
    invalidatePreview()
    form.projectId = projectId ?? 0
    if (!projectId && visible.value) {
      visible.value = false
    }
  }
)

function handleOpen() {
  form.projectId = props.projectId ?? 0
  requestError.value = ''
}

async function focusCandidateName() {
  await nextTick()
  candidateNameInput.value?.focus()
}

function invalidatePreview() {
  previewRequestId += 1
  applyRequestId += 1
  preview.value = null
  previewInput.value = null
  confirmed.value = false
  requestError.value = ''
  previewLoading.value = false
  applying.value = false
}

function resetDialog() {
  invalidatePreview()
  applying.value = false
  Object.assign(form, {
    projectId: props.projectId ?? 0,
    candidateName: '',
    displayName: '',
    dataType: 'varchar',
    comment: '',
    sourceRef: '',
    sourceText: ''
  })
  formRef.value?.clearValidate()
}

async function handleClosed() {
  resetDialog()
  await dialogFocus.restoreFocus()
}

function buildPreviewInput(): TokenEvidenceCandidatePreviewReq {
  return {
    projectId: form.projectId,
    candidateName: form.candidateName,
    displayName: form.displayName || undefined,
    dataType: form.dataType,
    comment: form.comment || undefined,
    sourceRef: form.sourceRef,
    sourceText: form.sourceText || undefined
  }
}

async function loadPreview() {
  if (!props.projectId) {
    requestError.value = '请先选择项目'
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  const input = buildPreviewInput()
  const requestId = ++previewRequestId
  previewLoading.value = true
  requestError.value = ''
  preview.value = null
  previewInput.value = null
  confirmed.value = false
  try {
    const result = await previewTokenEvidenceCandidate(input)
    if (requestId !== previewRequestId) {
      return
    }
    preview.value = result
    previewInput.value = input
  } catch (error) {
    if (requestId === previewRequestId) {
      requestError.value = errorMessage(error, '预览失败，请重试')
    }
  } finally {
    if (requestId === previewRequestId) {
      previewLoading.value = false
    }
  }
}

async function applyCandidate() {
  const input = previewInput.value
  if (!input) {
    requestError.value = '请重新生成预览'
    return
  }
  const payload = buildTokenEvidenceCandidateApplyReq(input, preview.value, confirmed.value)
  if (!payload) {
    requestError.value = '请核对预览并勾选确认'
    return
  }

  const requestedProjectId = props.projectId
  if (!requestedProjectId || input.projectId !== requestedProjectId) {
    requestError.value = '项目已变化，请重新生成预览'
    return
  }
  const requestId = ++applyRequestId
  applying.value = true
  requestError.value = ''
  try {
    const result = await applyTokenEvidenceCandidate(payload)
    if (!shouldHandleTokenEvidenceCandidateApplyResult(result, {
      requestId,
      currentRequestId: applyRequestId,
      requestedProjectId,
      currentProjectId: props.projectId,
      dialogVisible: visible.value
    })) {
      return
    }
    ElMessage.success(result.deduplicated ? '候选已存在，已返回原记录' : '命名证据候选已写入')
    emit('applied', result)
    visible.value = false
  } catch (error) {
    if (!isCurrentApplyRequest(requestId, requestedProjectId)) {
      return
    }
    requestError.value = errorMessage(error, '写入失败，请重新生成预览')
    preview.value = null
    previewInput.value = null
    confirmed.value = false
  } finally {
    if (requestId === applyRequestId) {
      applying.value = false
    }
  }
}

function isCurrentApplyRequest(requestId: number, requestedProjectId: number): boolean {
  return requestId === applyRequestId
    && requestedProjectId === props.projectId
    && visible.value
}

function signalTagType(signalType?: TokenEvidenceCandidateSignalType): TagProps['type'] {
  if (signalType === 'DISABLED_NAMING') {
    return 'danger'
  }
  if (signalType === 'AMBIGUOUS_ABBREVIATION') {
    return 'warning'
  }
  return 'info'
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<style scoped>
.token-evidence-content {
  display: grid;
  gap: 14px;
  max-height: min(72vh, 720px);
  overflow-y: auto;
  padding-right: 2px;
}

.evidence-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.full-row {
  grid-column: 1 / -1;
}

.preview-section,
.signal-section {
  display: grid;
  gap: 12px;
}

.preview-summary {
  width: 100%;
}

.signal-section h3 {
  margin: 0;
  font-size: 15px;
}

.signal-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.signal-item {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}

.signal-heading,
.signal-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.signal-heading {
  justify-content: space-between;
}

.signal-reason {
  color: #606266;
  overflow-wrap: anywhere;
}

.confirmation-check {
  min-height: 32px;
  white-space: normal;
}

:global(.token-evidence-dialog) {
  max-width: calc(100vw - 24px);
}

@media (max-width: 640px) {
  .evidence-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .full-row {
    grid-column: auto;
  }

  .token-evidence-content {
    max-height: 68vh;
  }
}
</style>
