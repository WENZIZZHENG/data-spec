<template>
  <div class="state-block" :data-state-type="type">
    <el-empty :description="title">
      <p v-if="description" class="state-description">{{ description }}</p>
      <p v-if="suggestedAction" class="state-suggestion">建议：{{ suggestedAction }}</p>
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
export type StateBlockType = 'empty' | 'error' | 'project'

withDefaults(defineProps<{
  type?: StateBlockType
  title: string
  description?: string
  suggestedAction?: string
  docsRef?: string
  actionText?: string
  secondaryActionText?: string
  loading?: boolean
}>(), {
  type: 'empty',
  description: '',
  suggestedAction: '',
  docsRef: '',
  actionText: '',
  secondaryActionText: '',
  loading: false
})

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
