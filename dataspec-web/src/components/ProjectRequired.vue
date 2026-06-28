<template>
  <slot v-if="hasProject" />
  <StateBlock
    v-else
    type="project"
    :title="title"
    :description="description"
    :action-text="actionText"
    :secondary-action-text="secondaryActionText"
    :loading="loading"
    @action="$emit('action')"
    @secondary="$emit('secondary')"
  />
</template>

<script setup lang="ts">
import StateBlock from '@/components/StateBlock.vue'

withDefaults(defineProps<{
  hasProject: boolean
  title?: string
  description?: string
  actionText?: string
  secondaryActionText?: string
  loading?: boolean
}>(), {
  title: '请先创建并选择项目',
  description: '项目是字段标准、SQL 检查和 AI Context 的边界。选择项目后页面会自动加载当前数据。',
  actionText: '去项目列表',
  secondaryActionText: '',
  loading: false
})

defineEmits<{
  action: []
  secondary: []
}>()
</script>
