<template>
  <div class="ai-feedback-page">
    <div class="page-header">
      <div>
        <h2>AI 反馈</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <el-tooltip content="刷新反馈">
        <el-button aria-label="刷新反馈" :disabled="!hasProject" :loading="loading" @click="loadReport">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </el-tooltip>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div v-loading="loading" class="feedback-body">
        <div class="summary-grid">
          <div class="summary-tile">
            <span class="summary-label">AI 作业</span>
            <strong>{{ report?.summary?.aiJobCount ?? 0 }}</strong>
          </div>
          <div class="summary-tile">
            <span class="summary-label">SQL 检查</span>
            <strong>{{ report?.summary?.sqlCheckCount ?? 0 }}</strong>
          </div>
          <div class="summary-tile">
            <span class="summary-label">规则信号</span>
            <strong>{{ report?.summary?.ruleSignalCount ?? 0 }}</strong>
          </div>
          <div class="summary-tile">
            <span class="summary-label">fixedSql</span>
            <strong>{{ report?.summary?.fixedSqlAvailableCount ?? 0 }}</strong>
          </div>
          <div class="summary-tile">
            <span class="summary-label">生成时间</span>
            <strong class="summary-time">{{ formatAiFeedbackTime(report?.generatedAt) }}</strong>
          </div>
        </div>

        <el-alert
          v-if="report?.summary?.insufficientSuggestionHistory"
          class="gap-alert"
          type="info"
          show-icon
          :closable="false"
          :title="report.summary.recommendationHistoryNote || '推荐历史不足'"
        />

        <section class="feedback-section">
          <div class="section-header">
            <h3>下一步动作</h3>
          </div>
          <div class="action-list">
            <button
              v-for="action in report?.nextActions ?? []"
              :key="action.title"
              class="action-row"
              type="button"
              @click="goTarget(action.targetRoute)"
            >
              <span>
                <strong>{{ action.title }}</strong>
                <small>{{ action.description }}</small>
              </span>
              <el-tag size="small" :type="aiFeedbackPriorityTagType(action.priority)" effect="plain">
                {{ action.priority || 'LOW' }}
              </el-tag>
            </button>
            <el-empty v-if="(report?.nextActions ?? []).length === 0" description="暂无下一步动作" />
          </div>
        </section>

        <section class="feedback-section">
          <div class="section-header">
            <h3>高频字段信号</h3>
          </div>
          <el-table :data="report?.fieldSignals ?? []" stripe empty-text="暂无字段信号">
            <el-table-column prop="title" label="信号" min-width="180" show-overflow-tooltip />
            <el-table-column label="次数" width="80">
              <template #default="{ row }">{{ row.count ?? 0 }}</template>
            </el-table-column>
            <el-table-column prop="suggestedAction" label="建议动作" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="goTarget(row.targetRoute)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="feedback-section two-column">
          <div>
            <div class="section-header">
              <h3>规则问题排行</h3>
            </div>
            <signal-list :signals="report?.ruleSignals ?? []" empty-text="暂无规则信号" @open="goTarget" />
          </div>
          <div>
            <div class="section-header">
              <h3>fixedSql 机会</h3>
            </div>
            <signal-list :signals="report?.fixedSqlSignals ?? []" empty-text="暂无 fixedSql 信号" @open="goTarget" />
          </div>
        </section>

        <section class="feedback-section">
          <div class="section-header">
            <h3>标准化信号</h3>
          </div>
          <signal-list :signals="report?.unmanagedSignals ?? []" empty-text="暂无标准化信号" @open="goTarget" />
        </section>

        <section class="feedback-section">
          <div class="section-header">
            <h3>样本范围</h3>
          </div>
          <div class="sample-row">
            <el-tag effect="plain">AI {{ report?.sampleSize?.aiJobRecords ?? 0 }}</el-tag>
            <el-tag effect="plain">SQL {{ report?.sampleSize?.sqlCheckRecords ?? 0 }}</el-tag>
            <el-tag effect="plain">规则例外 {{ report?.sampleSize?.ruleExemptions ?? 0 }}</el-tag>
            <el-tag effect="plain">来源 {{ report?.sampleSize?.fieldSources ?? 0 }}</el-tag>
            <el-tag effect="plain">字段 {{ report?.sampleSize?.fields ?? 0 }}</el-tag>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import type { PropType } from 'vue'
import { useRouter } from 'vue-router'
import { getAiFeedbackReport } from '@/api/aiFeedback'
import { useProjectStore } from '@/stores/project'
import {
  aiFeedbackPriorityTagType,
  aiFeedbackSeverityTagType,
  buildAiFeedbackRoute,
  formatAiFeedbackTime
} from '@/utils/aiFeedbackDisplay'
import type { AiFeedbackReport, AiFeedbackSignal } from '@/types'

const SignalList = defineComponent({
  props: {
    signals: {
      type: Array as PropType<AiFeedbackSignal[]>,
      required: true
    },
    emptyText: {
      type: String,
      required: true
    }
  },
  emits: ['open'],
  setup(props, { emit }) {
    return () => props.signals.length > 0
      ? h('div', { class: 'signal-list' }, props.signals.map((signal) =>
          h('button', {
            key: signal.title,
            class: 'signal-row',
            type: 'button',
            onClick: () => emit('open', signal.targetRoute)
          }, [
            h('span', { class: 'signal-main' }, [
              h('strong', signal.title || '-'),
              h('small', signal.suggestedAction || '')
            ]),
            h('span', { class: 'signal-meta' }, [
              h('span', { class: `signal-severity is-${aiFeedbackSeverityTagType(signal.severity)}` }, signal.severity || 'info'),
              h('span', `${signal.count ?? 0}`)
            ])
          ])
        ))
      : h('div', { class: 'empty-inline' }, props.emptyText)
  }
})

const projectStore = useProjectStore()
const router = useRouter()
const report = ref<AiFeedbackReport | null>(null)
const loading = ref(false)

const hasProject = computed(() => projectStore.currentProjectId !== null)

onMounted(() => {
  loadReport()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    report.value = null
    loadReport()
  }
)

async function loadReport() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    report.value = null
    return
  }
  loading.value = true
  try {
    report.value = await getAiFeedbackReport(projectId)
  } finally {
    loading.value = false
  }
}

function goTarget(route?: string | null) {
  router.push(buildAiFeedbackRoute(route))
}
</script>

<style scoped>
.ai-feedback-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2,
.section-header h3 {
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
}

.feedback-body {
  min-height: 260px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}

.summary-tile {
  min-height: 72px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.summary-label {
  display: block;
  margin-bottom: 8px;
  color: #6b7280;
  font-size: 12px;
}

.summary-tile strong {
  color: #111827;
  font-size: 22px;
}

.summary-time {
  font-size: 14px !important;
}

.gap-alert,
.feedback-section {
  margin-top: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.action-list,
.signal-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-row,
.signal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.action-row small,
.signal-row small {
  display: block;
  margin-top: 4px;
  color: #6b7280;
}

.signal-main {
  min-width: 0;
}

.signal-main strong,
.signal-main small {
  overflow: hidden;
  text-overflow: ellipsis;
}

.signal-meta,
.sample-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.signal-severity {
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #374151;
  font-size: 12px;
}

.signal-severity.is-warning {
  background: #fff7ed;
  color: #b45309;
}

.signal-severity.is-danger {
  background: #fef2f2;
  color: #b91c1c;
}

.empty-inline {
  padding: 20px;
  color: #909399;
  text-align: center;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }

  .two-column {
    grid-template-columns: 1fr;
  }
}
</style>
