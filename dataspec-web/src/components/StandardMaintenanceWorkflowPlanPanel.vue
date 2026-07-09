<template>
  <section class="workflow-panel">
    <div class="workflow-header">
      <div>
        <h3>{{ workflowPlan.inboxAction?.title || '维护 workflow dry-run' }}</h3>
        <p>{{ workflowPlan.inboxAction?.description || '当前计划只描述步骤，不自动执行写入。' }}</p>
      </div>
      <div class="workflow-tags">
        <el-tag type="info" effect="plain">{{ workflowPlan.recipeBinding?.recipeId || 'standard-maintenance' }}</el-tag>
        <el-tag :type="executionTagType" effect="plain">{{ workflowPlan.executionState?.status || 'DRY_RUN' }}</el-tag>
      </div>
    </div>

    <div class="workflow-meta">
      <span v-if="workflowPlan.workflowId">计划：{{ workflowPlan.workflowId }}</span>
      <span v-if="workflowPlan.inboxAction?.targetCount !== undefined">待处理：{{ workflowPlan.inboxAction.targetCount }}</span>
      <span v-if="workflowPlan.executionState?.currentStepId">当前位置：{{ workflowPlan.executionState.currentStepId }}</span>
    </div>

    <el-alert
      v-if="workflowPlan.executionState?.blockedReason"
      type="warning"
      :closable="false"
      show-icon
      :title="workflowPlan.executionState.blockedReason"
    />
    <el-alert
      v-if="workflowPlan.undoHint"
      type="info"
      :closable="false"
      show-icon
      :title="workflowPlan.undoHint"
    />

    <div v-if="workflowPlan.dryRunSteps?.length" class="workflow-steps">
      <div v-for="step in workflowPlan.dryRunSteps" :key="step.stepId" class="workflow-step">
        <div class="step-heading">
          <el-tag size="small" effect="plain">{{ step.phase }}</el-tag>
          <strong>{{ step.title }}</strong>
          <el-tag v-if="step.requiresConfirmation" size="small" type="warning" effect="plain">
            需确认
          </el-tag>
        </div>
        <div class="step-description">{{ step.description }}</div>
        <code v-if="step.recommendedAction">{{ step.recommendedAction }}</code>
        <small v-if="step.expectedEvidence">{{ step.expectedEvidence }}</small>
      </div>
    </div>

    <div class="workflow-columns">
      <div>
        <h4>证据</h4>
        <el-empty v-if="!(workflowPlan.evidenceLinks?.length)" description="暂无证据链接" />
        <ul v-else>
          <li v-for="link in workflowPlan.evidenceLinks" :key="`${link.sourceCapability}-${link.label}`">
            <strong>{{ link.label }}</strong>
            <span>{{ link.summary }}</span>
            <small>{{ link.sourceCapability }} · {{ link.count ?? 0 }}</small>
          </li>
        </ul>
      </div>
      <div>
        <h4>下一步</h4>
        <el-empty v-if="!(workflowPlan.nextActions?.length)" description="暂无下一步" />
        <ul v-else>
          <li v-for="action in workflowPlan.nextActions" :key="`${action.code}-${action.message}`">
            <strong>{{ action.message }}</strong>
            <code v-if="action.command">{{ action.command }}</code>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StandardMaintenanceWorkflowPlan } from '@/types'

const props = defineProps<{
  /** 后端返回的标准维护 dry-run 计划，只用于展示，不代表前端已执行步骤。 */
  workflowPlan: StandardMaintenanceWorkflowPlan
}>()

const executionTagType = computed(() => {
  const status = props.workflowPlan.executionState?.status
  if (status === 'BLOCKED') {
    return 'warning'
  }
  if (status === 'READY_FOR_REVIEW') {
    return 'success'
  }
  return 'info'
})
</script>

<style scoped>
.workflow-panel {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 16px;
  margin: 14px 0;
  background: #fff;
}

.workflow-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.workflow-header h3,
.workflow-columns h4 {
  margin: 0 0 6px;
}

.workflow-header p,
.workflow-meta,
.step-description,
.workflow-columns small {
  color: #606266;
}

.workflow-tags,
.workflow-meta,
.step-heading {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.workflow-meta {
  margin: 10px 0;
}

.workflow-steps {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.workflow-step {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px;
}

.workflow-step code,
.workflow-columns code {
  display: block;
  margin-top: 6px;
  white-space: pre-wrap;
  word-break: break-word;
}

.workflow-step small {
  display: block;
  margin-top: 6px;
  color: #909399;
}

.workflow-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
}

.workflow-columns ul {
  margin: 0;
  padding-left: 18px;
}

.workflow-columns li {
  margin-bottom: 8px;
}

.workflow-columns span,
.workflow-columns small {
  display: block;
}

@media (max-width: 800px) {
  .workflow-header,
  .workflow-columns {
    grid-template-columns: 1fr;
  }

  .workflow-header {
    display: grid;
  }
}
</style>
