<template>
  <div class="state-block" :data-state-type="type">
    <el-empty :description="title">
      <p v-if="description" class="state-description">{{ description }}</p>
      <p v-if="visibleSuggestedAction" class="state-suggestion">建议：{{ visibleSuggestedAction }}</p>
      <ul v-if="visibleNextActions.length" class="state-next-actions" aria-label="下一步动作">
        <li v-for="action in visibleNextActions" :key="action">{{ action }}</li>
      </ul>
      <p v-if="docsRef" class="state-docs">参考：{{ docsRef }}</p>
      <div v-if="actionText || secondaryActionText" class="state-actions">
        <el-button
          v-if="actionText"
          type="primary"
          :loading="loading"
          @click="$emit('action')"
        >
          {{ actionText }}
        </el-button>
        <el-button v-if="secondaryActionText" @click="$emit('secondary')">
          {{ secondaryActionText }}
        </el-button>
      </div>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export type StateBlockType = 'empty' | 'error' | 'project'

const props = withDefaults(defineProps<{
  type?: StateBlockType
  title: string
  description?: string
  suggestedAction?: string
  nextActions?: string[]
  docsRef?: string
  actionText?: string
  secondaryActionText?: string
  loading?: boolean
}>(), {
  type: 'empty',
  description: '',
  suggestedAction: '',
  nextActions: () => [],
  docsRef: '',
  actionText: '',
  secondaryActionText: '',
  loading: false
})

const visibleNextActions = computed(() => {
  const actions = props.nextActions.map((action) => action.trim()).filter(Boolean)
  return Array.from(new Set(actions))
})
const visibleSuggestedAction = computed(() => visibleNextActions.value.length > 0 ? '' : props.suggestedAction.trim())

defineEmits<{
  action: []
  secondary: []
}>()
</script>

<style scoped>
.state-block {
  width: 100%;
  padding: 24px 0;
}

.state-description,
.state-suggestion,
.state-docs {
  max-width: 560px;
  margin: 8px auto 0;
  color: #606266;
  line-height: 1.6;
}

.state-suggestion {
  color: #92400e;
}

.state-next-actions {
  max-width: 560px;
  margin: 10px auto 0;
  padding-left: 20px;
  color: #92400e;
  line-height: 1.7;
  text-align: left;
}

.state-next-actions li + li {
  margin-top: 4px;
}

.state-docs {
  color: #64748b;
}

.state-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}
</style>
