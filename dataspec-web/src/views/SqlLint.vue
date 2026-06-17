<template>
  <div class="sql-lint-page">
    <div class="page-header">
      <h2>SQL 校验</h2>
      <el-button type="primary" :loading="linting" @click="handleLint">
        <el-icon><CaretRight /></el-icon>
        执行校验
      </el-button>
    </div>

    <div class="lint-content">
      <div class="editor-panel">
        <div class="panel-title">SQL 编辑器</div>
        <div ref="editorContainer" class="editor-container"></div>
      </div>

      <div class="result-panel">
        <div class="panel-title">校验结果</div>
        <div class="result-content">
          <template v-if="lintResult">
            <div class="summary-row">
              <el-tag :type="issueTotal === 0 ? 'success' : 'danger'">
                共 {{ issueTotal }} 个问题
              </el-tag>
              <el-tag type="danger">错误 {{ lintResult.errorCount ?? 0 }}</el-tag>
              <el-tag type="warning">警告 {{ lintResult.warningCount ?? 0 }}</el-tag>
              <el-tag type="info">建议 {{ lintResult.suggestionCount ?? 0 }}</el-tag>
            </div>

            <el-table :data="lintResult.issues ?? []" stripe style="width: 100%">
              <el-table-column prop="severity" label="级别" width="110">
                <template #default="{ row }">
                  <el-tag :type="severityType(row.severity)" size="small">
                    {{ severityLabel(row.severity) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="tableName" label="表" width="130" />
              <el-table-column prop="columnName" label="字段" width="130" />
              <el-table-column prop="ruleCode" label="规则" width="180" />
              <el-table-column prop="message" label="描述" min-width="260" />
            </el-table>
          </template>
          <el-empty v-else description="请输入 SQL 并点击执行校验" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as monaco from 'monaco-editor'
import { lintSql } from '@/api/lint'
import { useProjectStore } from '@/stores/project'
import type { LintIssue, LintResult } from '@/types'

const editorContainer = ref<HTMLElement>()
const lintResult = ref<LintResult | null>(null)
const linting = ref(false)
const projectStore = useProjectStore()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const issueTotal = computed(() => {
  if (!lintResult.value) {
    return 0
  }
  return (
    (lintResult.value.errorCount ?? 0) +
    (lintResult.value.warningCount ?? 0) +
    (lintResult.value.suggestionCount ?? 0)
  )
})

onMounted(() => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: `CREATE TABLE users (
    id bigserial PRIMARY KEY,
    username varchar(50) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false
);`,
      language: 'sql',
      theme: 'vs-dark',
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 14,
      lineNumbers: 'on',
      scrollBeyondLastLine: false
    })
  }
})

onBeforeUnmount(() => {
  editor?.dispose()
})

async function handleLint() {
  const sql = editor?.getValue() || ''
  if (!sql.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }

  linting.value = true
  try {
    lintResult.value = await lintSql({
      sql,
      projectId: projectStore.currentProjectId ?? undefined
    })
  } finally {
    linting.value = false
  }
}

function severityType(severity: LintIssue['severity']) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = {
    ERROR: 'danger',
    WARNING: 'warning',
    SUGGESTION: 'info'
  }
  return severity ? map[severity] : 'info'
}

function severityLabel(severity: LintIssue['severity']) {
  const map: Record<string, string> = {
    ERROR: '错误',
    WARNING: '警告',
    SUGGESTION: '建议'
  }
  return severity ? map[severity] : '-'
}
</script>

<style scoped>
.sql-lint-page {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-header h2 {
  margin: 0;
}

.lint-content {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

.editor-panel,
.result-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.panel-title {
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.editor-container {
  flex: 1;
  min-height: 0;
}

.result-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.summary-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
