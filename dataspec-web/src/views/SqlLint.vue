<template>
  <div class="sql-lint-page">
    <div class="page-header">
      <h2>SQL 校验</h2>
      <el-button type="primary" @click="handleLint">
        <el-icon><CaretRight /></el-icon>
        执行校验
      </el-button>
    </div>

    <div class="lint-content">
      <!-- 左侧：SQL 编辑器 -->
      <div class="editor-panel">
        <div class="panel-title">SQL 编辑器</div>
        <div ref="editorContainer" class="editor-container"></div>
      </div>

      <!-- 右侧：校验结果 -->
      <div class="result-panel">
        <div class="panel-title">校验结果</div>
        <div class="result-content">
          <template v-if="lintResult">
            <el-tag :type="lintResult.totalIssues === 0 ? 'success' : 'danger'" class="result-tag">
              共 {{ lintResult.totalIssues }} 个问题
            </el-tag>
            <el-table :data="lintResult.issues" stripe style="width: 100%">
              <el-table-column prop="line" label="行" width="60" />
              <el-table-column prop="column" label="列" width="60" />
              <el-table-column prop="severity" label="级别" width="80">
                <template #default="{ row }">
                  <el-tag :type="severityType(row.severity)" size="small">
                    {{ row.severity }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="ruleCode" label="规则" width="120" />
              <el-table-column prop="message" label="描述" />
            </el-table>
          </template>
          <el-empty v-else description="请输入 SQL 并点击执行校验" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as monaco from 'monaco-editor'
import type { LintResult } from '@/types'

const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

// 校验结果
const lintResult = ref<LintResult | null>(null)

onMounted(() => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: '-- 在此输入 SQL 语句\nSELECT * FROM table_name;\n',
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

/** 执行校验（占位，后续对接 API） */
const handleLint = () => {
  const sql = editor?.getValue() || ''
  if (!sql.trim()) {
    return
  }
  // 占位：返回空结果，后续替换为真实 API 调用
  lintResult.value = {
    totalIssues: 0,
    issues: []
  }
}

/** 严重级别对应的 Tag 类型 */
const severityType = (severity: string) => {
  const map: Record<string, 'danger' | 'warning' | 'info'> = {
    error: 'danger',
    warning: 'warning',
    info: 'info'
  }
  return map[severity] || 'info'
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

.result-tag {
  margin-bottom: 12px;
}
</style>
