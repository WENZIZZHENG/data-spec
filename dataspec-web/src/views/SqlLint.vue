<template>
  <div class="sql-lint-page" :data-testid="stableTestIds.sqlLint.page">
    <div class="page-header">
      <h2>SQL 校验</h2>
      <div class="header-actions">
        <el-button
          type="primary"
          :loading="linting"
          :data-testid="stableTestIds.sqlLint.runButton"
          aria-label="执行校验 SQL"
          @click="handleLint"
        >
          <el-icon><CaretRight /></el-icon>
          执行校验
        </el-button>
        <el-button :loading="debugging" aria-label="打开规则调试" @click="handleDebug">
          <el-icon><Search /></el-icon>
          规则调试
        </el-button>
      </div>
    </div>
    <div class="fix-policy-toolbar">
      <div class="policy-control profile-control">
        <span class="policy-label">AI 模式</span>
        <el-select
          v-model="selectedProfileId"
          :loading="profileLoading"
          size="small"
          class="profile-select"
          clearable
          placeholder="默认模式"
          aria-label="AI 模式"
          @change="handleProfileChange"
        >
          <el-option
            v-for="profile in aiProfiles"
            :key="profile.profileId"
            :label="profile.displayName || profile.profileId"
            :value="profile.profileId"
          />
        </el-select>
        <el-tag v-if="selectedAiProfile?.taskType" size="small" type="info">
          {{ selectedAiProfile.taskType }}
        </el-tag>
      </div>
      <el-switch
        v-model="useProfileFixPolicy"
        size="small"
        :disabled="!selectedProfileId"
        active-text="profile 策略"
        inactive-text="手动策略"
      />
      <div class="policy-control">
        <span class="policy-label">修复模式</span>
        <el-radio-group
          v-model="fixPolicyMode"
          size="small"
          :disabled="profileFixPolicyActive"
          aria-label="修复模式"
        >
          <el-radio-button label="GENERATE">生成</el-radio-button>
          <el-radio-button label="DRY_RUN">dry-run</el-radio-button>
          <el-radio-button label="DISABLED">关闭</el-radio-button>
        </el-radio-group>
      </div>
      <div class="policy-control">
        <span class="policy-label">最高风险</span>
        <el-select
          v-model="fixMaxRiskLevel"
          size="small"
          class="risk-select"
          :disabled="profileFixPolicyActive"
          aria-label="最高风险"
        >
          <el-option label="低" value="LOW" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="高" value="HIGH" />
        </el-select>
      </div>
      <el-switch
        v-model="includeFixExplanations"
        size="small"
        :disabled="profileFixPolicyActive"
        active-text="解释"
        inactive-text="简略"
      />
    </div>

    <div class="lint-content">
      <div class="editor-panel">
        <div class="panel-title">SQL 编辑器</div>
        <div ref="editorContainer" class="editor-container"></div>
      </div>

      <div class="result-panel">
        <div class="panel-title">校验结果</div>
        <div class="result-content">
          <div v-if="debugResult || debugError || debugging" class="debug-panel">
            <div class="debug-header">
              <div>
                <strong>规则调试</strong>
                <span v-if="debugResult?.debugVersion" class="debug-version">{{ debugResult.debugVersion }}</span>
              </div>
              <div class="debug-tags">
                <el-tag size="small" type="success">命中 {{ debugMatchedCount }}</el-tag>
                <el-tag size="small" type="info">未命中 {{ debugNoMatchCount }}</el-tag>
                <el-tag size="small" type="warning">禁用 {{ debugDisabledCount }}</el-tag>
              </div>
            </div>
            <el-alert
              v-if="debugError"
              type="error"
              :closable="false"
              :title="debugError"
              show-icon
            />
            <div v-else-if="debugResult" class="debug-grid">
              <div class="debug-rule-list">
                <button
                  v-for="rule in debugRules"
                  :key="rule.ruleCode"
                  type="button"
                  :class="['debug-rule-button', { active: rule.ruleCode === selectedDebugRuleCode }]"
                  @click="selectedDebugRuleCode = rule.ruleCode || ''"
                >
                  <span class="debug-rule-main">
                    <span class="debug-rule-name">{{ rule.ruleName || rule.ruleCode }}</span>
                    <span class="debug-rule-code">{{ rule.ruleCode }}</span>
                  </span>
                  <span class="debug-rule-tags">
                    <el-tag size="small" :type="debugStatusType(debugRuleStatus(rule))">
                      {{ debugStatusLabel(debugRuleStatus(rule)) }}
                    </el-tag>
                    <el-tag v-if="rule.severity" size="small" :type="severityType(rule.severity)">
                      {{ severityLabel(rule.severity) }}
                    </el-tag>
                  </span>
                </button>
              </div>
              <div v-if="selectedDebugRule" class="debug-detail">
                <div class="debug-detail-title">
                  <div>
                    <strong>{{ selectedDebugRule.ruleName || selectedDebugRule.ruleCode }}</strong>
                    <small>{{ selectedDebugRule.ruleCode }}</small>
                  </div>
                  <el-tag size="small" :type="selectedDebugRule.enabled ? 'success' : 'warning'">
                    {{ selectedDebugRule.enabled ? '启用' : '禁用' }}
                  </el-tag>
                </div>
                <div class="debug-metrics">
                  <span>active {{ selectedDebugRule.suppressionStatus?.activeIssueCount ?? 0 }}</span>
                  <span>suppressed {{ selectedDebugRule.suppressionStatus?.suppressedIssueCount ?? 0 }}</span>
                  <span>fix {{ selectedDebugRule.fixStrategy?.fixSummary?.availableCount ?? 0 }}</span>
                </div>
                <div class="debug-section">
                  <div class="debug-section-title">matchTrace</div>
                  <el-table :data="selectedDebugRule.matchTrace ?? []" size="small" class="debug-trace-table">
                    <el-table-column label="状态" width="86">
                      <template #default="{ row }">
                        <el-tag size="small" :type="debugStatusType(row.status)">
                          {{ debugStatusLabel(row.status) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="位置" width="120">
                      <template #default="{ row }">
                        <el-button
                          v-if="row.sourceRange?.line"
                          size="small"
                          text
                          type="primary"
                          @click="handleGoToDebugRange(row.sourceRange)"
                        >
                          {{ sourceRangeLabel(row.sourceRange) }}
                        </el-button>
                        <span v-else>{{ sourceRangeLabel(row.sourceRange) }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="对象" min-width="130" show-overflow-tooltip>
                      <template #default="{ row }">
                        {{ debugTargetLabel(row) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="message" label="说明" min-width="180" show-overflow-tooltip />
                    <el-table-column prop="issueMessage" label="issue" min-width="220" show-overflow-tooltip />
                  </el-table>
                </div>
                <div class="debug-section">
                  <div class="debug-section-title">paramsSnapshot</div>
                  <pre class="debug-json">{{ formatDebugJson(selectedDebugRule.paramsSnapshot) }}</pre>
                </div>
                <div class="debug-section debug-columns">
                  <div>
                    <div class="debug-section-title">fixStrategy</div>
                    <div class="debug-pill-row">
                      <el-tag size="small" type="success">应用 {{ selectedDebugRule.fixStrategy?.fixSummary?.appliedCount ?? 0 }}</el-tag>
                      <el-tag size="small" type="warning">预览 {{ selectedDebugRule.fixStrategy?.fixSummary?.plannedCount ?? 0 }}</el-tag>
                      <el-tag size="small" type="info">跳过 {{ selectedDebugRule.fixStrategy?.fixSummary?.skippedCount ?? 0 }}</el-tag>
                    </div>
                    <ul class="debug-notes">
                      <li v-for="(action, index) in selectedDebugRule.fixStrategy?.nextActions ?? []" :key="`fix-${index}`">
                        {{ action }}
                      </li>
                    </ul>
                  </div>
                  <div>
                    <div class="debug-section-title">suppressionStatus</div>
                    <p class="debug-summary">{{ selectedDebugRule.suppressionStatus?.summary || '-' }}</p>
                    <p v-if="selectedDebugRule.suppressionStatus?.suppressionReasons?.length" class="debug-summary">
                      {{ selectedDebugRule.suppressionStatus.suppressionReasons.join(' / ') }}
                    </p>
                  </div>
                </div>
                <div v-if="selectedDebugRule.debugNotes?.length" class="debug-section">
                  <div class="debug-section-title">debugNotes</div>
                  <ul class="debug-notes">
                    <li v-for="(note, index) in selectedDebugRule.debugNotes" :key="`note-${index}`">
                      {{ note }}
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>

          <template v-if="lintResult">
            <div class="summary-row">
              <el-tag :type="issueTotal === 0 ? 'success' : 'danger'">
                共 {{ issueTotal }} 个问题
              </el-tag>
              <el-tag type="danger">错误 {{ lintResult.errorCount ?? 0 }}</el-tag>
              <el-tag type="warning">警告 {{ lintResult.warningCount ?? 0 }}</el-tag>
              <el-tag type="info">建议 {{ lintResult.suggestionCount ?? 0 }}</el-tag>
            </div>

            <div v-if="lintDialectDiagnostics.length" class="dialect-panel">
              <div class="dialect-header">
                <span>方言诊断</span>
                <el-tag size="small" :type="diagnosticSummaryTagType(lintDialectDiagnostics)">
                  {{ dialectSummary(lintDialectDiagnostics) }}
                </el-tag>
              </div>
              <div class="diagnostic-list">
                <div
                  v-for="diagnostic in lintDialectDiagnostics"
                  :key="diagnostic.code || `${diagnostic.dialect}-${diagnostic.capability}`"
                  class="diagnostic-item"
                >
                  <el-tag size="small" :type="diagnosticTagType(diagnostic.level)">
                    {{ diagnosticLevelLabel(diagnostic.level) }}
                  </el-tag>
                  <div class="diagnostic-copy">
                    <span>{{ diagnostic.message }}</span>
                    <small v-if="diagnostic.nextAction">{{ diagnostic.nextAction }}</small>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="hasFixPlan" class="fix-plan-panel">
              <div class="fix-plan-header">
                <span>修复策略</span>
                <div class="fix-plan-tags">
                  <el-tag size="small" type="info">{{ fixModeLabel(lintResult.fixPolicy?.mode) }}</el-tag>
                  <el-tag size="small" :type="riskTagType(lintResult.fixPolicy?.maxRiskLevel)">
                    {{ riskLabel(lintResult.fixPolicy?.maxRiskLevel) }}
                  </el-tag>
                  <el-tag v-if="lintResult.fixDryRun" size="small" type="warning">dry-run</el-tag>
                </div>
              </div>
              <div class="fix-summary-row">
                <el-tag size="small" type="success">应用 {{ lintResult.fixSummary?.appliedCount ?? 0 }}</el-tag>
                <el-tag size="small" type="warning">预览 {{ lintResult.fixSummary?.plannedCount ?? 0 }}</el-tag>
                <el-tag size="small" type="info">跳过 {{ lintResult.fixSummary?.skippedCount ?? 0 }}</el-tag>
              </div>
              <ul v-if="lintResult.fixNextActions?.length" class="fix-actions">
                <li v-for="(action, index) in lintResult.fixNextActions" :key="index">{{ action }}</li>
              </ul>
              <el-table
                v-if="fixChanges.length"
                :data="fixChanges"
                size="small"
                class="fix-change-table"
                empty-text="暂无修复变更"
              >
                <el-table-column label="状态" width="86">
                  <template #default="{ row }">
                    <el-tag size="small" :type="fixStatusType(row.status)">
                      {{ fixStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="风险" width="76">
                  <template #default="{ row }">
                    <el-tag size="small" :type="riskTagType(row.riskLevel)">
                      {{ riskLabel(row.riskLevel) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="ruleCode" label="规则" min-width="150" />
                <el-table-column label="变更" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ fixChangeLabel(row) }}
                  </template>
                </el-table-column>
                <el-table-column prop="explain" label="解释" min-width="240" show-overflow-tooltip />
              </el-table>
            </div>

            <div
              v-if="lintResult.fixedSql"
              class="fixed-sql-panel"
              :data-testid="stableTestIds.sqlLint.fixedSqlPanel"
            >
              <div class="fixed-sql-header">
                <span>修正 SQL</span>
                <el-button size="small" text type="primary" aria-label="复制修正 SQL" @click="handleCopySql">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
              <div v-if="fixedSqlDiffLines.length" class="sql-diff">
                <div
                  v-for="(line, index) in fixedSqlDiffLines"
                  :key="`current-${index}`"
                  :class="['diff-line', `diff-line-${line.type}`]"
                >
                  {{ line.text || ' ' }}
                </div>
              </div>
              <pre class="sql-code">{{ lintResult.fixedSql }}</pre>
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
              <el-table-column label="位置" width="100">
                <template #default="{ row }">
                  <el-button
                    v-if="row.line"
                    size="small"
                    text
                    type="primary"
                    @click="handleGoToIssue(row)"
                  >
                    {{ locationLabel(row) }}
                  </el-button>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="ruleCode" label="规则" width="180" />
              <el-table-column label="修复" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.fixStatus" size="small" :type="fixStatusType(row.fixStatus)">
                    {{ fixStatusLabel(row.fixStatus) }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="描述" min-width="260" />
              <el-table-column label="建议" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  <span>{{ fixSuggestion(row) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else description="请输入 SQL 并点击执行校验" />
        </div>
      </div>
    </div>

    <div class="history-panel" :data-testid="stableTestIds.sqlLint.historyPanel">
      <el-collapse v-model="historyActiveNames">
        <el-collapse-item name="records">
          <template #title>
            <div class="history-title" :data-testid="stableTestIds.sqlLint.historyToggle">
              <span>最近检查记录</span>
              <el-tag size="small" type="info">{{ recordTotal }} 条</el-tag>
            </div>
          </template>

          <StateBlock
            v-if="!projectStore.currentProjectId"
            type="project"
            title="请选择项目后查看记录"
            description="SQL 可以先校验；选择项目后会展示该项目最近检查记录、回放和证据包入口。"
            action-text="去项目列表"
            @action="goProjects"
          />
          <template v-else>
            <StateBlock
              v-if="recordState.errorMessage.value"
              type="error"
              title="检查记录加载失败"
              :description="recordState.errorMessage.value"
              :suggested-action="recordState.suggestedAction.value"
              :next-actions="recordState.nextActions.value"
              :docs-ref="recordState.docsRef.value"
              action-text="重试"
              @action="loadRecords"
            />
            <StateBlock
              v-else-if="!recordLoading && records.length === 0"
              type="empty"
              title="暂无检查记录"
              description="执行一次项目内 SQL 校验后，记录、回放和证据包入口会出现在这里。"
              action-text="刷新记录"
              @action="loadRecords"
            />
            <el-table
              v-else
              v-loading="recordLoading"
              :data="records"
              stripe
              class="record-table"
              :data-testid="stableTestIds.sqlLint.recordTable"
              empty-text="暂无检查记录"
            >
              <el-table-column label="检查时间" min-width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column prop="errorCount" label="错误" width="90" />
              <el-table-column prop="warningCount" label="警告" width="90" />
              <el-table-column prop="suggestionCount" label="建议" width="90" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    text
                    type="primary"
                    :loading="recordDetailLoading && loadingRecordId === row.id"
                    :data-testid="stableTestIds.sqlLint.recordDetailButton"
                    aria-label="查看详情 SQL 检查记录"
                    @click="handleOpenRecordDetail(row.id)"
                  >
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-row">
              <el-pagination
                v-if="recordTotal > recordSize"
                background
                layout="prev, pager, next"
                :current-page="recordCurrent"
                :page-size="recordSize"
                :total="recordTotal"
                @current-change="handleRecordPageChange"
              />
            </div>
          </template>
        </el-collapse-item>
      </el-collapse>
    </div>

    <el-dialog
      v-model="recordDialogVisible"
      title="检查记录详情"
      width="860px"
      @closed="recordDialogFocus.restoreFocus"
    >
      <div
        v-if="activeRecord?.record"
        class="record-detail"
        :data-testid="stableTestIds.sqlLint.recordDialog"
      >
        <div class="summary-row">
          <el-tag type="danger">错误 {{ activeRecord.record.errorCount ?? 0 }}</el-tag>
          <el-tag type="warning">警告 {{ activeRecord.record.warningCount ?? 0 }}</el-tag>
          <el-tag type="info">建议 {{ activeRecord.record.suggestionCount ?? 0 }}</el-tag>
          <span class="record-time">{{ formatDate(activeRecord.record.createdAt) }}</span>
        </div>

        <div class="record-evidence-actions">
          <el-button
            size="small"
            @click="handleCopyRecordLink"
          >
            <el-icon><CopyDocument /></el-icon>
            复制链接
          </el-button>
          <el-button
            size="small"
            :loading="evidenceLoading"
            @click="handleCopyRecordEvidence"
          >
            复制证据 JSON
          </el-button>
          <el-button
            size="small"
            type="primary"
            :loading="evidenceLoading"
            @click="handleDownloadRecordEvidence"
          >
            下载证据包
          </el-button>
        </div>

        <div v-if="activeRecord.replay" class="detail-section replay-section">
          <div class="detail-title replay-title">
            <span>标准回放</span>
            <el-tag size="small" :type="replayStatusType(activeRecord.replay.status)">
              {{ replayStatusLabel(activeRecord.replay.status) }}
            </el-tag>
          </div>
          <div class="replay-grid">
            <div class="replay-meta">
              <span class="replay-label">记录标准</span>
              <span>{{ standardLabel(activeRecord.replay.recordedStandard) }}</span>
            </div>
            <div class="replay-meta">
              <span class="replay-label">当前标准</span>
              <span>{{ standardLabel(activeRecord.replay.currentStandard) }}</span>
            </div>
            <div class="replay-meta">
              <span class="replay-label">计数</span>
              <span>
                字段 {{ activeRecord.replay.summary?.fieldCount ?? 0 }} /
                枚举 {{ activeRecord.replay.summary?.enumCount ?? 0 }} /
                规则 {{ activeRecord.replay.summary?.ruleCount ?? 0 }}
              </span>
            </div>
          </div>

          <div v-if="activeRecord.replay.summary?.exportCommand" class="replay-command">
            <div class="fixed-sql-header compact-header">
              <span>历史 Context 导出命令</span>
              <el-button size="small" text type="primary" @click="handleCopyReplayCommand">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </div>
            <pre class="sql-code compact">{{ activeRecord.replay.summary.exportCommand }}</pre>
          </div>

          <ul v-if="activeRecord.replay.nextActions?.length" class="replay-actions">
            <li v-for="(action, index) in activeRecord.replay.nextActions" :key="index">
              {{ action }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <div class="detail-title">原始 SQL</div>
          <pre class="sql-code compact">{{ activeRecord.record.originalSql }}</pre>
        </div>

        <div v-if="activeRecord.record.fixedSql" class="detail-section">
          <div class="detail-title">修正 SQL</div>
          <div v-if="recordDiffLines.length" class="sql-diff compact">
            <div
              v-for="(line, index) in recordDiffLines"
              :key="`record-${index}`"
              :class="['diff-line', `diff-line-${line.type}`]"
            >
              {{ line.text || ' ' }}
            </div>
          </div>
          <pre class="sql-code compact">{{ activeRecord.record.fixedSql }}</pre>
        </div>

        <div class="detail-section">
          <div class="detail-title">问题列表</div>
          <el-table :data="activeRecord.issues ?? []" stripe style="width: 100%">
            <el-table-column prop="severity" label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)" size="small">
                  {{ severityLabel(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tableName" label="表" width="120" />
            <el-table-column prop="columnName" label="字段" width="120" />
            <el-table-column label="位置" width="90">
              <template #default="{ row }">
                {{ locationLabel(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="ruleCode" label="规则" width="170" />
            <el-table-column label="修复" width="120">
              <template #default="{ row }">
                <div v-if="row.fixStatus" class="issue-fix-cell">
                  <el-tag size="small" :type="fixStatusType(row.fixStatus)">
                    {{ fixStatusLabel(row.fixStatus) }}
                  </el-tag>
                  <span v-if="row.fixRiskLevel">{{ riskLabel(row.fixRiskLevel) }}</span>
                </div>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="描述" min-width="240" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as monaco from 'monaco-editor'
import { listAiProfiles } from '@/api/aiProfile'
import { downloadEvidencePackage, generateEvidencePackage } from '@/api/evidence'
import { debugLintSql, getLintRecord, lintSql, listLintRecords } from '@/api/lint'
import StateBlock from '@/components/StateBlock.vue'
import { useDialogFocusReturn } from '@/composables/useDialogFocusReturn'
import { useRequestState } from '@/composables/useRequestState'
import { useProjectStore } from '@/stores/project'
import {
  readSelectedAiProfile,
  resolveSelectedAiProfile,
  saveSelectedAiProfile
} from '@/utils/aiProfileSelection'
import {
  diagnosticLevelLabel,
  diagnosticSummaryTagType,
  diagnosticTagType,
  dialectSummary
} from '@/utils/dialectDiagnostics'
import { stableTestIds } from '@/utils/stableTestIds'
import { copyRouteUrl, readPositiveIntQuery, replaceRouteQuery } from '@/utils/urlState'
import type {
  AiTaskProfile,
  FixChange,
  FixPolicy,
  LintIssue,
  LintRequest,
  LintResult,
  PageResult,
  RecordDetail,
  SqlCheckRecord,
  SqlLintDebugResult,
  SqlRuleDebugTrace,
  SqlRuleMatchTrace,
  SqlRuleSourceRange,
  StandardSnapshotInfo
} from '@/types'

const editorContainer = ref<HTMLElement>()
const lintResult = ref<LintResult | null>(null)
const debugResult = ref<SqlLintDebugResult | null>(null)
const selectedDebugRuleCode = ref('')
const linting = ref(false)
const debugging = ref(false)
const debugError = ref('')
const records = ref<SqlCheckRecord[]>([])
const recordTotal = ref(0)
const recordCurrent = ref(1)
const recordSize = ref(10)
const recordState = useRequestState<PageResult<SqlCheckRecord>>()
const recordDetailLoading = ref(false)
const loadingRecordId = ref<number | null>(null)
const activeRecord = ref<RecordDetail | null>(null)
const recordDialogVisible = ref(false)
const evidenceLoading = ref(false)
const historyActiveNames = ref<string[]>([])
const profileLoading = ref(false)
const aiProfiles = ref<AiTaskProfile[]>([])
const selectedProfileId = ref('')
const useProfileFixPolicy = ref(true)
const fixPolicyMode = ref<NonNullable<FixPolicy['mode']>>('GENERATE')
const fixMaxRiskLevel = ref<NonNullable<FixPolicy['maxRiskLevel']>>('MEDIUM')
const includeFixExplanations = ref(true)
const projectStore = useProjectStore()
const route = useRoute()
const router = useRouter()
let editor: monaco.editor.IStandaloneCodeEditor | null = null
const recordDialogFocus = useDialogFocusReturn(recordDialogVisible)

const DEFAULT_SQL = `CREATE TABLE users (
    id bigserial PRIMARY KEY,
    username varchar(50) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false
);`
const DEMO_LINT_SQL = `CREATE TABLE UserOrder (
    id bigserial PRIMARY KEY,
    uid bigint NOT NULL,
    phone varchar(20),
    amount decimal(10,2) DEFAULT 0,
    create_time timestamp,
    update_time timestamp,
    del_flag boolean DEFAULT false
);`

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
const fixedSqlDiffLines = computed(() => parseDiff(lintResult.value?.fixedSqlDiff))
const lintDialectDiagnostics = computed(() => lintResult.value?.dialectDiagnostics ?? [])
const currentFixPolicy = computed<FixPolicy>(() => ({
  mode: fixPolicyMode.value,
  maxRiskLevel: fixMaxRiskLevel.value,
  includeExplanations: includeFixExplanations.value
}))
const selectedAiProfile = computed(() =>
  aiProfiles.value.find((profile) => profile.profileId === selectedProfileId.value) ?? null
)
const profileFixPolicyActive = computed(() => Boolean(selectedProfileId.value && useProfileFixPolicy.value))
const fixChanges = computed<FixChange[]>(() => lintResult.value?.fixChanges ?? [])
const hasFixPlan = computed(() => Boolean(
  lintResult.value?.fixSummary ||
  lintResult.value?.fixPolicy ||
  fixChanges.value.length ||
  lintResult.value?.fixNextActions?.length
))
const recordDiffLines = computed(() => {
  const record = activeRecord.value?.record
  return parseDiff(buildSqlDiff(record?.originalSql, record?.fixedSql))
})
const recordLoading = computed(() => recordState.loading.value)
const debugRules = computed<SqlRuleDebugTrace[]>(() => debugResult.value?.rules ?? [])
const selectedDebugRule = computed(() =>
  debugRules.value.find((rule) => rule.ruleCode === selectedDebugRuleCode.value) ?? debugRules.value[0] ?? null
)
const debugMatchedCount = computed(() =>
  debugRules.value.filter((rule) => debugRuleStatus(rule) === 'MATCHED').length
)
const debugNoMatchCount = computed(() =>
  debugRules.value.filter((rule) => debugRuleStatus(rule) === 'NO_MATCH').length
)
const debugDisabledCount = computed(() =>
  debugRules.value.filter((rule) => debugRuleStatus(rule) === 'DISABLED').length
)

onMounted(() => {
  applyRecordPageFromRoute()
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: initialSql(),
      language: 'sql',
      theme: 'vs-dark',
      automaticLayout: true,
      ariaLabel: 'SQL 编辑器',
      minimap: { enabled: false },
      fontSize: 14,
      lineNumbers: 'on',
      scrollBeyondLastLine: false
    })
  }
  if (projectStore.currentProjectId) {
    void loadRecords()
    void loadAiProfiles()
  } else if (!projectStore.loading && projectStore.projects.length === 0) {
    void projectStore.loadProjects().then(() => {
      void loadRecords()
      void loadAiProfiles()
    })
  }
})

onBeforeUnmount(() => {
  editor?.dispose()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    applyRecordPageFromRoute()
    void loadRecords()
    void loadAiProfiles()
  }
)

watch(
  () => route.query.page,
  () => {
    applyRecordPageFromRoute()
    void loadRecords()
  }
)

watch(
  () => route.query.recordId,
  () => {
    void openRecordFromRoute()
  }
)

watch(recordDialogVisible, (visible) => {
  if (!visible && route.query.recordId) {
    activeRecord.value = null
    void syncRecordUrlState({ recordId: null })
  }
})

watch(
  () => route.query.demo,
  () => {
    if (route.query.demo === 'lint' && editor) {
      editor.setValue(DEMO_LINT_SQL)
    }
  }
)

async function handleLint() {
  const sql = editor?.getValue() || ''
  if (!sql.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }

  linting.value = true
  debugError.value = ''
  debugResult.value = null
  try {
    lintResult.value = await lintSql(buildLintRequest(sql))
    recordCurrent.value = 1
    await syncRecordUrlState({ recordId: null })
    await loadRecords()
  } finally {
    linting.value = false
  }
}

async function handleDebug() {
  const sql = editor?.getValue() || ''
  if (!sql.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }

  debugging.value = true
  debugError.value = ''
  try {
    const result = await debugLintSql(buildLintRequest(sql))
    debugResult.value = result
    lintResult.value = result.lintResult ?? null
    selectedDebugRuleCode.value = result.rules?.[0]?.ruleCode ?? ''
  } catch (error) {
    debugError.value = error instanceof Error ? error.message : '规则调试失败'
  } finally {
    debugging.value = false
  }
}

function buildLintRequest(sql: string): LintRequest {
  const request: LintRequest = {
    sql,
    projectId: projectStore.currentProjectId ?? undefined
  }
  if (selectedProfileId.value) {
    request.profileId = selectedProfileId.value
  }
  if (!profileFixPolicyActive.value) {
    request.fixPolicy = currentFixPolicy.value
  }
  return request
}

async function loadRecords() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    records.value = []
    recordTotal.value = 0
    recordState.reset()
    return
  }

  try {
    await recordState.run(async () => {
      const page = await listLintRecords(projectId, recordCurrent.value, recordSize.value)
      records.value = page.records ?? []
      recordTotal.value = page.total ?? 0
      recordCurrent.value = page.current ?? recordCurrent.value
      recordSize.value = page.size ?? recordSize.value
      await openRecordFromRoute()
      return page
    })
  } catch {
    // 页面内 StateBlock 会展示可恢复错误和重试入口。
  }
}

async function loadAiProfiles() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    aiProfiles.value = []
    selectedProfileId.value = ''
    return
  }

  profileLoading.value = true
  try {
    const storedProfile = readSelectedAiProfile(projectId)
    const catalog = await listAiProfiles(projectId, storedProfile || undefined)
    aiProfiles.value = catalog.profiles ?? []
    selectedProfileId.value = resolveSelectedAiProfile(storedProfile, catalog.selectedProfileId || catalog.defaultProfileId)
    if (selectedProfileId.value) {
      saveSelectedAiProfile(projectId, selectedProfileId.value)
    }
    applyProfileFixPolicy(selectedAiProfile.value)
  } finally {
    profileLoading.value = false
  }
}

function handleProfileChange(value?: string) {
  const profileId = value || ''
  selectedProfileId.value = profileId
  saveSelectedAiProfile(projectStore.currentProjectId, profileId)
  if (!profileId) {
    useProfileFixPolicy.value = false
    return
  }
  useProfileFixPolicy.value = true
  applyProfileFixPolicy(selectedAiProfile.value)
}

function applyProfileFixPolicy(profile: AiTaskProfile | null) {
  const policy = profile?.fixedSqlPolicy
  if (!policy) {
    return
  }
  if (policy.mode) {
    fixPolicyMode.value = policy.mode
  }
  if (policy.maxRiskLevel) {
    fixMaxRiskLevel.value = policy.maxRiskLevel
  }
  if (typeof policy.includeExplanations === 'boolean') {
    includeFixExplanations.value = policy.includeExplanations
  }
}

async function handleCopySql() {
  const fixedSql = lintResult.value?.fixedSql
  if (!fixedSql) {
    return
  }
  await copyToClipboard(fixedSql, '已复制修正 SQL', '复制失败，请手动选择修正 SQL')
}

async function handleCopyReplayCommand() {
  const command = activeRecord.value?.replay?.summary?.exportCommand
  if (!command) {
    return
  }
  await copyToClipboard(command, '已复制回放命令', '复制失败，请手动选择回放命令')
}

async function copyToClipboard(text: string, successMessage: string, errorMessage: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch {
    ElMessage.error(errorMessage)
  }
}

async function handleViewRecord(id?: number) {
  if (!id) {
    return
  }
  recordDetailLoading.value = true
  loadingRecordId.value = id
  try {
    activeRecord.value = await getLintRecord(id)
    recordDialogVisible.value = true
    await syncRecordUrlState({ recordId: id })
  } catch {
    ElMessage.warning('链接中的检查记录不存在或不可访问')
    await syncRecordUrlState({ recordId: null })
  } finally {
    recordDetailLoading.value = false
    loadingRecordId.value = null
  }
}

function handleOpenRecordDetail(id?: number) {
  recordDialogFocus.rememberFocus()
  void handleViewRecord(id)
}

async function openRecordFromRoute() {
  const recordId = readPositiveIntQuery(route.query, 'recordId')
  if (!recordId) {
    if (recordDialogVisible.value) {
      recordDialogVisible.value = false
      activeRecord.value = null
    }
    return
  }
  if (!projectStore.currentProjectId || loadingRecordId.value === recordId) {
    return
  }
  if (recordDialogVisible.value && activeRecord.value?.record?.id === recordId) {
    return
  }
  historyActiveNames.value = ['records']
  await handleViewRecord(recordId)
}

function applyRecordPageFromRoute() {
  recordCurrent.value = readPositiveIntQuery(route.query, 'page') ?? 1
}

async function syncRecordUrlState(patch: Record<string, number | null> = {}) {
  await replaceRouteQuery(router, route, {
    projectId: projectStore.currentProjectId,
    page: recordCurrent.value > 1 ? recordCurrent.value : null,
    ...patch
  })
}

async function handleCopyRecordLink() {
  const recordId = activeRecord.value?.record?.id
  if (!recordId) {
    return
  }
  try {
    await syncRecordUrlState({ recordId })
    await copyRouteUrl(route, navigator.clipboard)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.error('复制失败，请手动复制浏览器地址')
  }
}

async function handleCopyRecordEvidence() {
  const req = recordEvidenceRequest()
  if (!req) {
    return
  }
  evidenceLoading.value = true
  try {
    const evidence = await generateEvidencePackage(req)
    await copyToClipboard(
      JSON.stringify(evidence, null, 2),
      '已复制证据 JSON',
      '复制失败，请手动选择证据 JSON'
    )
  } finally {
    evidenceLoading.value = false
  }
}

async function handleDownloadRecordEvidence() {
  const req = recordEvidenceRequest()
  const recordId = activeRecord.value?.record?.id
  if (!req || !recordId) {
    return
  }
  evidenceLoading.value = true
  try {
    saveBlob(
      await downloadEvidencePackage(req),
      `dataspec-sql-check-evidence-${recordId}.zip`
    )
    ElMessage.success('已下载证据包')
  } finally {
    evidenceLoading.value = false
  }
}

function recordEvidenceRequest() {
  const record = activeRecord.value?.record
  if (!record?.id) {
    ElMessage.warning('当前记录缺少 ID，无法导出证据包')
    return null
  }
  return {
    projectId: record.projectId ?? projectStore.currentProjectId ?? undefined,
    sourceType: 'SQL_CHECK',
    sourceId: record.id,
    sourceTitle: `SQL 检查记录 #${record.id}`
  } as const
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function handleRecordPageChange(page: number) {
  recordCurrent.value = page
  void syncRecordUrlState()
}

function goProjects() {
  router.push('/projects')
}

function handleGoToIssue(issue: LintIssue) {
  if (!editor || !issue.line) {
    return
  }
  const position = {
    lineNumber: issue.line,
    column: issue.column ?? 1
  }
  editor.setPosition(position)
  editor.revealPositionInCenter(position)
  editor.focus()
}

function handleGoToDebugRange(range?: SqlRuleSourceRange) {
  if (!editor || !range?.line) {
    return
  }
  const position = {
    lineNumber: range.line,
    column: range.column ?? 1
  }
  editor.setPosition(position)
  editor.revealPositionInCenter(position)
  editor.focus()
}

function debugRuleStatus(rule: SqlRuleDebugTrace): NonNullable<SqlRuleMatchTrace['status']> {
  if (rule.enabled === false) {
    return 'DISABLED'
  }
  const traces = rule.matchTrace ?? []
  if (traces.some((trace) => trace.status === 'ERROR')) {
    return 'ERROR'
  }
  if (traces.some((trace) => trace.status === 'MATCHED')) {
    return 'MATCHED'
  }
  if (traces.some((trace) => trace.status === 'UNPARSED')) {
    return 'UNPARSED'
  }
  return traces[0]?.status ?? 'NO_MATCH'
}

function debugStatusLabel(status?: SqlRuleMatchTrace['status']) {
  const map: Record<string, string> = {
    MATCHED: '命中',
    NO_MATCH: '未命中',
    DISABLED: '禁用',
    UNPARSED: '未解析',
    ERROR: '异常'
  }
  return status ? map[status] ?? status : '-'
}

function debugStatusType(status?: SqlRuleMatchTrace['status']) {
  const map: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
    MATCHED: 'success',
    NO_MATCH: 'info',
    DISABLED: 'warning',
    UNPARSED: 'info',
    ERROR: 'danger'
  }
  return status ? map[status] ?? 'info' : 'info'
}

function sourceRangeLabel(range?: SqlRuleSourceRange) {
  if (!range?.line) {
    return '-'
  }
  const start = `${range.line}:${range.column ?? 1}`
  if (!range.lineEnd || !range.columnEnd) {
    return start
  }
  return `${start}-${range.lineEnd}:${range.columnEnd}`
}

function debugTargetLabel(trace: SqlRuleMatchTrace) {
  return [trace.tableName, trace.columnName].filter(Boolean).join('.') || '-'
}

function formatDebugJson(value: unknown) {
  if (value === undefined || value === null) {
    return '{}'
  }
  return JSON.stringify(value, null, 2)
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

function fixSuggestion(issue: LintIssue) {
  if (issue.suggestion) {
    return issue.suggestion
  }
  if (issue.replacement) {
    return `建议替换为 ${issue.replacement}`
  }
  return '-'
}

function fixModeLabel(mode?: FixPolicy['mode']) {
  const map: Record<string, string> = {
    GENERATE: '生成',
    DRY_RUN: 'dry-run',
    DISABLED: '关闭'
  }
  return mode ? map[mode] ?? mode : '生成'
}

function riskLabel(risk?: FixPolicy['maxRiskLevel'] | FixChange['riskLevel']) {
  const map: Record<string, string> = {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险'
  }
  return risk ? map[risk] ?? risk : '中风险'
}

function riskTagType(risk?: FixPolicy['maxRiskLevel'] | FixChange['riskLevel']) {
  const map: Record<string, 'success' | 'warning' | 'danger'> = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger'
  }
  return risk ? map[risk] ?? 'warning' : 'warning'
}

function fixStatusLabel(status?: FixChange['status'] | LintIssue['fixStatus']) {
  const map: Record<string, string> = {
    APPLIED: '已应用',
    PLANNED: '预览',
    SKIPPED: '跳过'
  }
  return status ? map[status] ?? status : '-'
}

function fixStatusType(status?: FixChange['status'] | LintIssue['fixStatus']) {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    APPLIED: 'success',
    PLANNED: 'warning',
    SKIPPED: 'info'
  }
  return status ? map[status] ?? 'info' : 'info'
}

function fixChangeLabel(change: FixChange) {
  const target = [change.tableName, change.columnName].filter(Boolean).join('.')
  const before = change.before ?? '-'
  const after = change.after ?? '-'
  return `${target || change.changeType || '变更'}：${before} -> ${after}`
}

function locationLabel(issue: LintIssue) {
  if (!issue.line) {
    return '-'
  }
  const start = `${issue.line}:${issue.column ?? 1}`
  if (!issue.lineEnd || !issue.columnEnd) {
    return start
  }
  return `${start}-${issue.lineEnd}:${issue.columnEnd}`
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

function replayStatusLabel(status?: string) {
  const map: Record<string, string> = {
    current: '当前一致',
    historical: '历史标准',
    unversioned: '未版本化',
    missing_snapshot: '快照缺失'
  }
  return status ? map[status] ?? status : '-'
}

function replayStatusType(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    current: 'success',
    historical: 'warning',
    unversioned: 'info',
    missing_snapshot: 'danger'
  }
  return status ? map[status] ?? 'info' : 'info'
}

function standardLabel(standard?: StandardSnapshotInfo) {
  if (!standard) {
    return '-'
  }
  const version = standard.specVersion || 'unversioned'
  const hash = standard.specHash ? ` / ${shortHash(standard.specHash)}` : ''
  const source = standard.source ? ` / ${standard.source}` : ''
  return `${version}${hash}${source}`
}

function shortHash(hash: string) {
  return hash.length > 12 ? `${hash.slice(0, 12)}...` : hash
}

function initialSql() {
  return route.query.demo === 'lint' ? DEMO_LINT_SQL : DEFAULT_SQL
}

type DiffLineType = 'header' | 'add' | 'remove' | 'context'

function parseDiff(diff?: string | null) {
  if (!diff) {
    return []
  }
  return diff.split('\n').map((text) => ({
    text,
    type: diffLineType(text)
  }))
}

function diffLineType(line: string): DiffLineType {
  if (line.startsWith('+++') || line.startsWith('---') || line.startsWith('@@')) {
    return 'header'
  }
  if (line.startsWith('+')) {
    return 'add'
  }
  if (line.startsWith('-')) {
    return 'remove'
  }
  return 'context'
}

function buildSqlDiff(originalSql?: string, fixedSql?: string) {
  if (!originalSql || !fixedSql || originalSql === fixedSql) {
    return null
  }
  const originalLines = originalSql.split(/\r?\n/)
  const fixedLines = fixedSql.split(/\r?\n/)
  if (originalLines.join('\n') === fixedLines.join('\n')) {
    return null
  }
  return [
    '--- original.sql',
    '+++ fixed.sql',
    '@@',
    ...buildDiffLines(originalLines, fixedLines)
  ].join('\n')
}

function buildDiffLines(originalLines: string[], fixedLines: string[]) {
  const lcs = Array.from({ length: originalLines.length + 1 }, () =>
    Array.from({ length: fixedLines.length + 1 }, () => 0)
  )
  for (let i = originalLines.length - 1; i >= 0; i--) {
    for (let j = fixedLines.length - 1; j >= 0; j--) {
      lcs[i][j] = originalLines[i] === fixedLines[j]
        ? lcs[i + 1][j + 1] + 1
        : Math.max(lcs[i + 1][j], lcs[i][j + 1])
    }
  }

  const lines: string[] = []
  let i = 0
  let j = 0
  while (i < originalLines.length && j < fixedLines.length) {
    if (originalLines[i] === fixedLines[j]) {
      lines.push(` ${originalLines[i]}`)
      i++
      j++
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      lines.push(`-${originalLines[i]}`)
      i++
    } else {
      lines.push(`+${fixedLines[j]}`)
      j++
    }
  }
  while (i < originalLines.length) {
    lines.push(`-${originalLines[i]}`)
    i++
  }
  while (j < fixedLines.length) {
    lines.push(`+${fixedLines[j]}`)
    j++
  }
  return lines
}
</script>

<style scoped>
.sql-lint-page {
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.fix-policy-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.policy-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.profile-control {
  min-width: 300px;
}

.policy-label {
  color: #606266;
  font-size: 13px;
}

.profile-select {
  width: 190px;
}

.risk-select {
  width: 92px;
}

.lint-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  min-height: 560px;
}

.editor-panel,
.result-panel {
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
  min-height: 520px;
}

.result-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.summary-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.debug-panel {
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
}

.debug-header,
.debug-detail-title,
.debug-tags,
.debug-rule-tags,
.debug-pill-row {
  display: flex;
  align-items: center;
}

.debug-header {
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #303133;
}

.debug-version {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}

.debug-tags,
.debug-rule-tags,
.debug-pill-row {
  flex-wrap: wrap;
  gap: 6px;
}

.debug-grid {
  display: grid;
  grid-template-columns: minmax(210px, 260px) minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
}

.debug-rule-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 520px;
  overflow: auto;
}

.debug-rule-button {
  display: flex;
  width: 100%;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  cursor: pointer;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  transition: border-color 0.18s ease, background-color 0.18s ease;
}

.debug-rule-button:hover,
.debug-rule-button.active {
  background: #eef5ff;
  border-color: #409eff;
}

.debug-rule-button:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 2px;
}

.debug-rule-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.debug-rule-name {
  overflow: hidden;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.debug-rule-code {
  overflow: hidden;
  color: #909399;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.debug-detail {
  min-width: 0;
}

.debug-detail-title {
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.debug-detail-title small {
  display: block;
  color: #909399;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
}

.debug-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 12px;
}

.debug-section {
  margin-top: 10px;
}

.debug-section-title {
  margin-bottom: 6px;
  color: #303133;
  font-size: 12px;
  font-weight: 600;
}

.debug-trace-table {
  width: 100%;
}

.debug-json {
  margin: 0;
  max-height: 180px;
  overflow: auto;
  padding: 10px;
  color: #1f2d3d;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.debug-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.debug-notes {
  margin: 6px 0 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.debug-summary {
  margin: 0 0 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.fixed-sql-panel,
.history-panel,
.fix-plan-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.fixed-sql-panel,
.fix-plan-panel {
  margin-bottom: 14px;
}

.fixed-sql-header,
.history-title,
.pagination-row,
.fix-plan-header,
.fix-summary-row {
  display: flex;
  align-items: center;
}

.fixed-sql-header {
  justify-content: space-between;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.history-title {
  gap: 8px;
  width: 100%;
  font-weight: 600;
}

.fix-plan-panel {
  padding: 10px 12px;
}

.fix-plan-header {
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.fix-plan-tags,
.fix-summary-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.fix-summary-row {
  margin-bottom: 8px;
}

.fix-actions {
  margin: 0 0 8px;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.fix-change-table {
  width: 100%;
}

.issue-fix-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
}

.sql-code {
  margin: 0;
  padding: 12px;
  max-height: 280px;
  overflow: auto;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #1f2d3d;
  background: #f7f8fa;
  white-space: pre-wrap;
  word-break: break-word;
}

.sql-code.compact {
  max-height: 220px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.sql-diff {
  max-height: 260px;
  overflow: auto;
  border-bottom: 1px solid #e4e7ed;
  font-family: "Cascadia Mono", "Consolas", monospace;
  font-size: 12px;
  line-height: 1.6;
  background: #f8fafc;
}

.sql-diff.compact {
  max-height: 220px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.diff-line {
  min-height: 19px;
  padding: 0 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.diff-line-header {
  color: #64748b;
  background: #f1f5f9;
}

.diff-line-add {
  color: #14532d;
  background: #dcfce7;
}

.diff-line-remove {
  color: #7f1d1d;
  background: #fee2e2;
}

.diff-line-context {
  color: #334155;
}

.record-table {
  width: 100%;
}

.pagination-row {
  justify-content: flex-end;
  padding-top: 12px;
}

.record-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.record-time {
  margin-left: auto;
  color: #606266;
  font-size: 13px;
}

.record-evidence-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.dialect-panel {
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
}

.dialect-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.diagnostic-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.diagnostic-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.diagnostic-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.diagnostic-copy small {
  color: #909399;
}

.replay-section {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #f9fafb;
}

.replay-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.replay-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.replay-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  font-size: 13px;
  color: #303133;
}

.replay-label {
  color: #909399;
  font-size: 12px;
}

.replay-command {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.compact-header {
  padding: 8px 10px;
}

.replay-actions {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .lint-content {
    grid-template-columns: 1fr;
  }

  .debug-grid,
  .debug-columns {
    grid-template-columns: 1fr;
  }

  .replay-grid {
    grid-template-columns: 1fr;
  }
}
</style>
