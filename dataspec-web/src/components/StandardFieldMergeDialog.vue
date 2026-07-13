<template>
  <el-dialog v-model="visible" title="字段合并" width="860px" @closed="resetTransientState">
    <div class="merge-dialog">
      <el-form label-width="88px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="保留字段">
              <el-select v-model="targetFieldId" filterable :loading="optionsLoading" class="full-width">
                <el-option
                  v-for="option in options"
                  :key="`target-${option.fieldId}`"
                  :disabled="option.fieldId === sourceFieldId"
                  :label="mergeFieldOptionLabel(option.name, option.displayName, option.status)"
                  :value="option.fieldId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="来源字段">
              <el-select v-model="sourceFieldId" filterable :loading="optionsLoading" class="full-width">
                <el-option
                  v-for="option in options"
                  :key="`source-${option.fieldId}`"
                  :disabled="option.fieldId === targetFieldId"
                  :label="mergeFieldOptionLabel(option.name, option.displayName, option.status)"
                  :value="option.fieldId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="dialog-actions">
        <el-button :disabled="!canPreview" :loading="previewLoading" @click="loadPreview">
          生成预览
        </el-button>
      </div>

      <template v-if="preview">
        <el-alert
          v-if="blockingRisks.length"
          type="error"
          :closable="false"
          show-icon
          title="存在阻断风险"
        />

        <div class="merge-grid">
          <div class="merge-panel">
            <h3>保留字段</h3>
            <strong>{{ preview.targetAfter?.name || '-' }}</strong>
            <p>{{ preview.targetAfter?.displayName || '-' }}</p>
            <div class="chip-list">
              <el-tag v-for="alias in preview.targetAfter?.aliases ?? []" :key="`alias-${alias}`" size="small">
                {{ alias }}
              </el-tag>
            </div>
          </div>
          <div class="merge-panel">
            <h3>来源字段</h3>
            <strong>{{ preview.sourceAfter?.name || '-' }}</strong>
            <p>{{ preview.sourceAfter?.replacementReason || '-' }}</p>
            <el-tag type="warning" size="small">{{ preview.sourceAfter?.status || 'deprecated' }}</el-tag>
          </div>
        </div>

        <el-table :data="preview.changes ?? []" size="small" border>
          <el-table-column prop="attribute" label="属性" width="140" />
          <el-table-column label="迁移" width="140">
            <template #default="{ row }">
              <el-tag :type="row.migrationMode === 'SAFE_MERGE' ? 'success' : 'info'" size="small">
                {{ row.migrationMode || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="变更后" min-width="220">
            <template #default="{ row }">{{ formatMergeValue(row.afterValue) }}</template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        </el-table>

        <div v-if="preview.risks?.length" class="risk-list">
          <el-tag
            v-for="risk in preview.risks"
            :key="`${risk.code}-${risk.message}`"
            :type="fieldMergeRiskTagType(risk.severity)"
            effect="plain"
          >
            {{ risk.code }}：{{ risk.message }}
          </el-tag>
        </div>

        <el-form label-width="88px">
          <el-form-item label="合并原因" :error="reasonError">
            <el-input
              v-model="reason"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="例如：统一手机号标准字段，保留历史字段为别名"
            />
          </el-form-item>
        </el-form>

        <div v-if="mergeResult?.rollbackHints?.length" class="rollback-list">
          <div v-for="hint in mergeResult.rollbackHints" :key="hint.targetPath" class="rollback-item">
            <strong>{{ hint.action }}</strong>
            <span>{{ hint.description }}</span>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button
        type="primary"
        :disabled="!preview || applying || blockingRisks.length > 0"
        :loading="applying"
        @click="applyMerge"
      >
        确认合并
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { applyStandardFieldMerge, previewStandardFieldMerge } from '@/api/field'
import {
  blockingMergeRisks,
  buildFieldMergePreviewPayload,
  fieldMergeRiskTagType,
  formatMergeValue,
  mergeFieldOptionLabel,
  submitFieldMergeApply
} from '@/utils/standardFieldMerge'
import type {
  StandardFieldMergeOption,
  StandardFieldMergePreview,
  StandardFieldMergeResult
} from '@/types'

const visible = defineModel<boolean>({ default: false })

const props = defineProps<{
  /** 当前项目 ID；缺失时禁止预览和提交。 */
  projectId?: number | null
  /** 可跨分页选择的字段候选。 */
  options: StandardFieldMergeOption[]
  /** 候选字段是否仍在按需加载。 */
  optionsLoading?: boolean
  /** 默认保留字段 ID。 */
  initialTargetId?: number | null
  /** 默认来源字段 ID。 */
  initialSourceId?: number | null
}>()

const emit = defineEmits<{
  applied: [result: StandardFieldMergeResult]
}>()

const targetFieldId = ref<number | undefined>()
const sourceFieldId = ref<number | undefined>()
const preview = ref<StandardFieldMergePreview | null>(null)
const mergeResult = ref<StandardFieldMergeResult | null>(null)
const reason = ref('')
const reasonError = ref('')
const previewLoading = ref(false)
const applying = ref(false)

const options = computed(() => props.options.filter((option) => typeof option.fieldId === 'number'))
const canPreview = computed(() => Boolean(props.projectId && targetFieldId.value && sourceFieldId.value && targetFieldId.value !== sourceFieldId.value))
const blockingRisks = computed(() => blockingMergeRisks(preview.value))

watch(
  () => visible.value,
  (open) => {
    if (open) {
      hydrateSelection()
    }
  }
)

watch(
  () => [props.initialTargetId, props.initialSourceId, props.options.length] as const,
  () => {
    if (visible.value) {
      hydrateSelection()
    }
  }
)

watch(
  () => [props.projectId, targetFieldId.value, sourceFieldId.value] as const,
  () => {
    resetTransientState()
  }
)

function hydrateSelection() {
  targetFieldId.value = normalizeOptionId(props.initialTargetId) ?? options.value[0]?.fieldId
  sourceFieldId.value = normalizeOptionId(props.initialSourceId)
    ?? options.value.find((option) => option.fieldId !== targetFieldId.value)?.fieldId
  resetTransientState()
}

function normalizeOptionId(value?: number | null) {
  return options.value.some((option) => option.fieldId === value) ? value ?? undefined : undefined
}

function resetTransientState() {
  preview.value = null
  mergeResult.value = null
  reason.value = ''
  reasonError.value = ''
}

async function loadPreview() {
  if (!props.projectId || !targetFieldId.value || !sourceFieldId.value) {
    return
  }
  previewLoading.value = true
  reasonError.value = ''
  try {
    preview.value = await previewStandardFieldMerge(
      buildFieldMergePreviewPayload(props.projectId, targetFieldId.value, sourceFieldId.value)
    )
    mergeResult.value = null
  } finally {
    previewLoading.value = false
  }
}

async function applyMerge() {
  applying.value = true
  reasonError.value = ''
  try {
    const submitted = await submitFieldMergeApply(preview.value, reason.value, applyStandardFieldMerge, {
      projectId: props.projectId,
      targetFieldId: targetFieldId.value,
      sourceFieldId: sourceFieldId.value
    })
    if (!submitted.submitted) {
      reasonError.value = submitted.error ?? '无法应用合并'
      return
    }
    mergeResult.value = submitted.result ?? null
    ElMessage.success('字段已合并')
    emit('applied', submitted.result as StandardFieldMergeResult)
  } finally {
    applying.value = false
  }
}
</script>

<style scoped>
.merge-dialog {
  display: grid;
  gap: 14px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
}

.full-width {
  width: 100%;
}

.merge-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.merge-panel {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.merge-panel h3,
.merge-panel p {
  margin: 0;
}

.merge-panel h3 {
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
}

.chip-list,
.risk-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rollback-list {
  display: grid;
  gap: 8px;
}

.rollback-item {
  display: flex;
  gap: 8px;
  color: #606266;
  font-size: 13px;
}

@media (max-width: 640px) {
  .merge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
