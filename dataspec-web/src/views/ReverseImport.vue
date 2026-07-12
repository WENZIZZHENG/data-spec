<template>
  <div class="reverse-page" :data-testid="stableTestIds.reverseImport.page">
    <div class="page-header">
      <div>
        <h2>反向导入</h2>
        <p class="page-subtitle">{{ projectStore.currentProjectName || '未选择项目' }}</p>
      </div>
      <div class="header-actions">
        <el-button plain :disabled="!hasProject" @click="handleCopyReverseImportLink">
          <el-icon><Link /></el-icon>
          复制链接
        </el-button>
        <el-button
          type="primary"
          :disabled="!canGeneratePreview"
          :loading="previewLoading"
          :data-testid="stableTestIds.reverseImport.generatePreviewButton"
          aria-label="生成预览 反向导入"
          @click="handleGeneratePreview"
        >
          <el-icon><View /></el-icon>
          生成预览
        </el-button>
        <el-button
          v-if="activeMode === 'database'"
          :disabled="!canGenerateCompare"
          :loading="compareLoading"
          @click="handleGenerateCompare"
        >
          <el-icon><View /></el-icon>
          生成差异
        </el-button>
        <el-button
          v-if="activeMode === 'database'"
          type="success"
          :disabled="!canImportCandidates"
          :loading="importLoading"
          @click="handleImportCandidates"
        >
          <el-icon><Check /></el-icon>
          确认导入
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <el-tabs v-model="activeMode" class="mode-tabs">
        <el-tab-pane label="SQL DDL" name="sql">
          <section class="input-section">
            <div class="input-toolbar">
              <el-upload accept=".sql" :auto-upload="false" :show-file-list="false" :on-change="handleFileChange">
                <el-button>
                  <el-icon><Upload /></el-icon>
                  读取 SQL 文件
                </el-button>
              </el-upload>
              <el-button :disabled="!sqlText" @click="clearSql">清空</el-button>
            </div>
              <el-input
                v-model="sqlText"
                type="textarea"
                :rows="12"
                spellcheck="false"
                placeholder="CREATE TABLE ..."
                aria-label="SQL DDL 输入"
              />
          </section>
        </el-tab-pane>

        <el-tab-pane name="database">
          <template #label>
            <span :data-testid="stableTestIds.reverseImport.databaseModeTab">数据库直连</span>
          </template>
          <section class="input-section database-flow">
            <el-steps :active="databaseStep" align-center finish-status="success" class="db-steps">
              <el-step title="连接信息" />
              <el-step title="选择表" />
              <el-step title="预览确认" />
              <el-step title="导入结果" />
            </el-steps>

            <div class="db-workbench">
              <div class="db-panel">
                <div class="section-header compact-header">
                  <h3>连接信息</h3>
                  <div class="inline-actions">
                    <el-button
                      size="small"
                      :loading="presetLoading"
                      aria-label="刷新连接预设"
                      title="刷新连接预设"
                      @click="loadPresets"
                    >
                      <el-icon><Refresh /></el-icon>
                    </el-button>
                    <el-tag :type="connectionTagType" effect="plain">{{ connectionStatusText }}</el-tag>
                  </div>
                </div>
                <div class="preset-bar">
                  <el-select
                    v-model="presetId"
                    class="preset-select"
                    filterable
                    clearable
                    :loading="presetLoading"
                    placeholder="选择连接预设"
                    @change="handlePresetChange"
                  >
                    <el-option
                      v-for="preset in presetOptions"
                      :key="preset.id"
                      :label="presetOptionLabel(preset)"
                      :value="preset.id"
                    >
                      <div class="preset-option">
                        <span class="preset-option-title">{{ presetOptionLabel(preset) }}</span>
                        <span class="preset-option-summary">{{ presetConnectionSummary(preset) }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <el-button type="primary" plain :disabled="!canOpenPresetDialog" @click="openPresetDialog">
                    <el-icon><Connection /></el-icon>
                    保存预设
                  </el-button>
                </div>
                <div v-if="selectedPreset" class="preset-summary">
                  {{ presetConnectionSummary(selectedPreset) }}
                </div>
                <el-form class="db-form" label-width="92px">
                  <el-form-item label="数据库">
                    <el-select v-model="dbForm.databaseType" class="form-control" @change="handleDatabaseTypeChange">
                      <el-option label="PostgreSQL" value="postgresql" />
                      <el-option label="MySQL" value="mysql" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="主机">
                    <el-input v-model="dbForm.host" class="form-control" placeholder="localhost" />
                  </el-form-item>
                  <el-form-item label="端口">
                    <el-input-number v-model="dbForm.port" class="form-control number-input" :min="1" :max="65535" />
                  </el-form-item>
                  <el-form-item label="数据库名">
                    <div :data-testid="stableTestIds.reverseImport.databaseNameInput">
                      <el-input
                        v-model="dbForm.databaseName"
                        class="form-control"
                        placeholder="dataspec_demo"
                      />
                    </div>
                  </el-form-item>
                  <el-form-item label="Schema">
                    <div :data-testid="stableTestIds.reverseImport.schemaNameInput">
                      <el-input
                        v-model="dbForm.schemaName"
                        class="form-control"
                        placeholder="public / database"
                      />
                    </div>
                  </el-form-item>
                  <el-form-item label="用户名">
                    <div :data-testid="stableTestIds.reverseImport.usernameInput">
                      <el-input v-model="dbForm.username" class="form-control" autocomplete="off" />
                    </div>
                  </el-form-item>
                  <el-form-item label="密码">
                    <el-input v-model="dbForm.password" class="form-control" type="password" show-password autocomplete="new-password" />
                  </el-form-item>
                </el-form>

                <div class="input-toolbar">
                  <el-button :disabled="!canUseDatabaseConnection" :loading="testLoading" @click="handleTestConnection">
                    <el-icon><Connection /></el-icon>
                    测试连接
                  </el-button>
                  <el-button
                    :disabled="!canUseDatabaseConnection"
                    :loading="tableLoading"
                    :data-testid="stableTestIds.reverseImport.loadTablesButton"
                    aria-label="加载表 数据库"
                    @click="handleLoadTables"
                  >
                    <el-icon><Refresh /></el-icon>
                    加载表
                  </el-button>
                  <el-button :disabled="!canScanMetadata" :loading="scanLoading" @click="handleStartMetadataScan">
                    <el-icon><Refresh /></el-icon>
                    分页扫描
                  </el-button>
                  <el-button
                    :disabled="!canBrowseMetadata"
                    :loading="metadataLoading"
                    :data-testid="stableTestIds.reverseImport.browseMetadataButton"
                    aria-label="浏览元数据 数据库"
                    @click="handleBrowseMetadata"
                  >
                    <el-icon><View /></el-icon>
                    浏览元数据
                  </el-button>
                  <el-button :disabled="!canGenerateSchemaPlan" :loading="schemaPlanLoading" @click="handleGenerateSchemaPlan">
                    <el-icon><View /></el-icon>
                    生成迁移计划
                  </el-button>
                  <el-button
                    :disabled="!canGenerateCommentPlan"
                    :loading="commentPlanLoading"
                    :data-testid="stableTestIds.reverseImport.commentPlanButton"
                    @click="handleGenerateCommentPlan"
                  >
                    <el-icon><View /></el-icon>
                    COMMENT 计划
                  </el-button>
                  <el-button :disabled="!canBrowseMetadata" :loading="metadataLoading" @click="handleRefreshMetadata">
                    <el-icon><Refresh /></el-icon>
                    刷新元数据
                  </el-button>
                </div>
                <div v-if="connectionSecurity || connectionHealth" class="security-diagnostic">
                  <div v-if="connectionHealth" class="security-section">
                    <div class="security-header">
                      <span>连接健康画像</span>
                      <el-tag :type="connectionStatusTagType(connectionHealth.connectionStatus)" effect="plain">
                        {{ connectionStatusLabel(connectionHealth.connectionStatus) }}
                      </el-tag>
                    </div>
                    <div class="security-summary">{{ databaseHealthSummary(connectionHealth) }}</div>
                    <div class="security-meta">
                      <span v-if="connectionHealth.failureCategory">失败分类：{{ failureCategoryLabel(connectionHealth.failureCategory) }}</span>
                      <span v-if="connectionHealth.latencyMs !== undefined">耗时：{{ connectionHealth.latencyMs }}ms</span>
                      <span v-if="connectionHealth.connectionStatus === 'FAILED'">{{ retryableLabel(connectionHealth.retryable) }}</span>
                      <span>{{ metadataReadableLabel(connectionHealth.capability?.metadataReadable) }}</span>
                    </div>
                    <div v-if="connectionHealth.capability" class="security-meta">
                      <span>Schema：{{ capabilitySupportLabel(connectionHealth.capability.schemaSupport) }}</span>
                      <span>Comment：{{ capabilitySupportLabel(connectionHealth.capability.commentSupport) }}</span>
                      <span>Index：{{ capabilitySupportLabel(connectionHealth.capability.indexSupport) }}</span>
                    </div>
                    <div v-if="connectionHealth.requiredPrivileges?.length" class="security-line">
                      所需权限：{{ connectionHealth.requiredPrivileges.join('、') }}
                    </div>
                    <div v-if="connectionHealth.warnings?.length" class="security-list">
                      <div v-for="warning in connectionHealth.warnings" :key="warning" class="security-line">
                        {{ warning }}
                      </div>
                    </div>
                    <div v-if="connectionHealth.nextActions?.length" class="security-list">
                      <div v-for="action in connectionHealth.nextActions" :key="action" class="security-line muted">
                        {{ action }}
                      </div>
                    </div>
                  </div>
                  <div v-if="connectionSecurity" class="security-section">
                    <div class="security-header">
                    <span>只读安全诊断</span>
                    <el-tag :type="securityRiskTagType(connectionSecurity.riskLevel)" effect="plain">
                      {{ securityRiskLabel(connectionSecurity.riskLevel) }}
                    </el-tag>
                    </div>
                    <div class="security-summary">{{ databaseSecuritySummary(connectionSecurity) }}</div>
                    <div class="security-meta">
                      <span>{{ readOnlyLabel(connectionSecurity.readOnly) }}</span>
                      <span>{{ writeRiskLabel(connectionSecurity.writeRisk) }}</span>
                      <span>{{ connectionSecurity.accessibleSchemaCount ?? 0 }} 个 schema</span>
                      <span>{{ connectionSecurity.accessibleTableCount ?? 0 }} 张表</span>
                    </div>
                    <div v-if="connectionSecurity.warnings?.length" class="security-list">
                      <div v-for="warning in connectionSecurity.warnings" :key="warning" class="security-line">
                        {{ warning }}
                      </div>
                    </div>
                    <div v-if="connectionSecurity.recommendedActions?.length" class="security-list">
                      <div v-for="action in connectionSecurity.recommendedActions" :key="action" class="security-line muted">
                        {{ action }}
                      </div>
                    </div>
                    <pre v-if="connectionSecurity.recommendedSql?.length" class="security-sql">{{ connectionSecurity.recommendedSql.join('\n') }}</pre>
                  </div>
                </div>
              </div>

              <div class="db-panel">
                <div class="section-header compact-header">
                  <h3>选择表</h3>
                  <div class="table-counts">
                    <el-tag type="info" effect="plain">当前页已选 {{ currentPageSelectedTableCount }} / {{ databaseTables.length }}</el-tag>
                    <el-tag v-if="selectedTableCount !== currentPageSelectedTableCount" type="success" effect="plain">
                      累计 {{ selectedTableCount }}
                    </el-tag>
                  </div>
                </div>
                <div class="table-tools">
                  <el-input
                    v-model="tableSearch"
                    :prefix-icon="Search"
                    clearable
                    placeholder="搜索 schema、表名或注释"
                    aria-label="数据库表筛选"
                  />
                  <el-input-number v-model="scanPageSize" :min="1" :max="100" size="small" controls-position="right" />
                  <el-button :disabled="filteredDatabaseTables.length === 0" @click="selectVisibleTables">全选当前</el-button>
                  <el-button :disabled="selectedTableCount === 0" @click="clearSelectedTables">清空</el-button>
                </div>
                <div v-if="scanResult" class="scan-panel">
                  <div class="scan-row">
                    <span>{{ scanProgressText }}</span>
                    <div class="scan-tags">
                      <el-tag v-if="scanResult.cancelled" type="warning" effect="plain">已取消</el-tag>
                      <el-tag v-else-if="scanResult.progress?.hasMore" type="info" effect="plain">可继续</el-tag>
                      <el-tag v-else type="success" effect="plain">已完成</el-tag>
                      <el-tag type="info" effect="plain">{{ scanStatusText }}</el-tag>
                      <el-tag v-if="scanResult.pageSize || scanResult.progress?.pageSize" type="info" effect="plain">
                        pageSize {{ scanResult.pageSize ?? scanResult.progress?.pageSize }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="scan-row scan-actions">
                    <el-button size="small" :disabled="!canContinueScan" :loading="scanLoading" @click="handleContinueMetadataScan">
                      下一批
                    </el-button>
                    <el-button size="small" :disabled="!canCancelScan" @click="handleCancelMetadataScan">取消扫描</el-button>
                    <el-button size="small" type="primary" plain :disabled="!canGenerateScanPartialCoverage" @click="handleGenerateScanPartialCoverage">
                      <el-icon><DataAnalysis /></el-icon>
                      生成部分覆盖率
                    </el-button>
                    <span class="muted-text">{{ scanResumeText }}</span>
                  </div>
                  <div v-if="scanSourcePressureText" class="scan-row metadata-cache-row">
                    <el-tag type="warning" effect="plain">源库压力</el-tag>
                    <span>{{ scanSourcePressureText }}</span>
                  </div>
                  <div v-if="scanFailureText" class="scan-row metadata-cache-row">
                    <el-tag type="danger" effect="plain">失败摘要</el-tag>
                    <span>{{ scanFailureText }}</span>
                  </div>
                  <div v-if="scanResult.partialResult" class="scan-row metadata-cache-row">
                    <el-tag type="success" effect="plain">成功 {{ scanResult.partialResult.successfulTableNames?.length ?? 0 }}</el-tag>
                    <el-tag v-if="scanResult.partialResult.failedTableNames?.length" type="danger" effect="plain">
                      失败 {{ scanResult.partialResult.failedTableNames.length }}
                    </el-tag>
                    <el-tag v-if="scanResult.partialResult.skippedTableNames?.length" type="info" effect="plain">
                      跳过 {{ scanResult.partialResult.skippedTableNames.length }}
                    </el-tag>
                  </div>
                  <div v-if="scanEvidenceText" class="scan-row metadata-cache-row">
                    <el-tag type="info" effect="plain">Evidence</el-tag>
                    <span>{{ scanEvidenceText }}</span>
                  </div>
                  <div v-if="scanResult.metadataCache" class="scan-row metadata-cache-row">
                    <el-tag :type="metadataCacheTagType" effect="plain">{{ metadataCacheStatusLabel(scanResult.metadataCache) }}</el-tag>
                    <span>{{ buildMetadataCacheSummary(scanResult.metadataCache) }}</span>
                  </div>
                </div>

                <div v-if="databaseTables.length === 0 && selectedTableCount > 0" class="saved-table-list">
                  <el-tag
                    v-for="tableName in dbForm.tableNames ?? []"
                    :key="tableName"
                    effect="plain"
                  >
                    {{ tableName }}
                  </el-tag>
                </div>
                <el-empty v-if="databaseTables.length === 0 && selectedTableCount === 0" class="small-empty" description="暂无表，请先加载" />
                <el-empty v-else-if="databaseTables.length > 0 && filteredDatabaseTables.length === 0" class="small-empty" description="没有匹配的表" />
                <el-checkbox-group
                  v-else-if="databaseTables.length > 0"
                  v-model="dbForm.tableNames"
                  class="table-check-list"
                  :data-testid="stableTestIds.reverseImport.tableChecklist"
                >
                  <el-checkbox
                    v-for="table in filteredDatabaseTables"
                    :key="tableKey(table)"
                    :value="table.tableName || ''"
                    :disabled="!table.tableName || isScanTableDisabled(table.tableName)"
                    :data-testid="reverseImportTableOptionTestId(table.schemaName, table.tableName)"
                    class="table-check-item"
                  >
                    <span class="table-title">{{ tableLabel(table) }}</span>
                    <span v-if="table.comment" class="table-comment">{{ table.comment }}</span>
                  </el-checkbox>
                </el-checkbox-group>
              </div>
            </div>

            <div
              v-if="metadataBrowser"
              class="metadata-browser"
              :data-testid="stableTestIds.reverseImport.metadataBrowserPanel"
            >
              <div class="section-header compact-header">
                <h3>元数据浏览</h3>
                <div class="inline-actions">
                  <el-tag type="info" effect="plain">只读 schema metadata</el-tag>
                  <el-tag v-if="activeMetadataCache" :type="metadataCacheTagType" effect="plain">
                    {{ metadataCacheStatusLabel(activeMetadataCache) }}
                  </el-tag>
                  <el-button size="small" plain :disabled="!canBrowseMetadata" :loading="metadataLoading" @click="handleRefreshMetadata">
                    <el-icon><Refresh /></el-icon>
                    刷新元数据
                  </el-button>
                  <el-button size="small" plain @click="handleCopyMetadataSummary">
                    <el-icon><Link /></el-icon>
                    复制 AI 摘要
                  </el-button>
                </div>
              </div>
              <div class="summary-grid metadata-summary">
                <div v-for="item in metadataSummaryItems" :key="item.key" class="summary-item">
                  <div class="summary-label">{{ item.label }}</div>
                  <div class="summary-value">{{ item.value }}</div>
                </div>
              </div>
              <div v-if="activeMetadataCache" class="metadata-cache-panel">
                <div class="metadata-cache-summary">{{ metadataCacheSummaryText }}</div>
                <div class="metadata-cache-meta">
                  <span v-if="activeMetadataCache?.metadataFingerprint">fingerprint={{ activeMetadataCache?.metadataFingerprint?.slice(0, 12) }}</span>
                  <span v-if="activeMetadataCache?.lastSeenAt">lastSeenAt={{ activeMetadataCache.lastSeenAt }}</span>
                  <span v-if="activeMetadataCache?.expiresAt">expiresAt={{ activeMetadataCache.expiresAt }}</span>
                </div>
                <div v-if="activeMetadataCache?.changeSummary?.changed" class="metadata-cache-meta">
                  <span>新增字段 {{ activeMetadataCache.changeSummary.addedColumnCount ?? 0 }}</span>
                  <span>删除字段 {{ activeMetadataCache.changeSummary.removedColumnCount ?? 0 }}</span>
                  <span>变更字段 {{ activeMetadataCache.changeSummary.changedColumnCount ?? 0 }}</span>
                </div>
              </div>
              <div class="metadata-tools">
                <el-input
                  v-model="metadataSearch"
                  :prefix-icon="Search"
                  clearable
                  placeholder="搜索 schema、表、字段、注释、类型、索引或标准字段"
                />
                <el-button size="small" :disabled="candidateTotal === 0" @click="selectAllCandidates">全选候选</el-button>
                <el-button size="small" :disabled="selectedCandidateCount === 0" @click="clearSelectedCandidates">清空候选</el-button>
              </div>
              <pre class="metadata-ai-summary">{{ metadataAiSummary }}</pre>
              <el-table :data="filteredMetadataRows" size="small" stripe empty-text="当前筛选下暂无 metadata">
                <el-table-column label="" width="52">
                  <template #default="{ row }">
                    <el-checkbox
                      v-if="row.importCandidate"
                      :model-value="selectedCandidateKeys.has(row.candidateKey || `${row.tableName}.${row.columnName}`)"
                      @change="handleMetadataCandidateCheck(row, $event)"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" :type="metadataBrowserStatusTagType(row.matchStatus)" effect="plain">
                      {{ metadataBrowserStatusLabel(row.matchStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="字段" min-width="210">
                  <template #default="{ row }">
                    <div class="metadata-field-name">{{ row.schemaName ? `${row.schemaName}.` : '' }}{{ row.tableName }}.{{ row.columnName }}</div>
                    <div v-if="row.tableComment" class="muted-text">{{ row.tableComment }}</div>
                  </template>
                </el-table-column>
                <el-table-column prop="dataType" label="类型" min-width="120" />
                <el-table-column label="标准字段" min-width="150">
                  <template #default="{ row }">
                    <span>{{ row.standardFieldName || '-' }}</span>
                    <div v-if="row.standardDisplayName" class="muted-text">{{ row.standardDisplayName }}</div>
                  </template>
                </el-table-column>
                <el-table-column prop="comment" label="注释" min-width="180" show-overflow-tooltip />
                <el-table-column label="索引" min-width="180">
                  <template #default="{ row }">
                    <el-tag
                      v-for="indexName in row.indexNames"
                      :key="`${row.tableName}.${row.columnName}.${indexName}`"
                      size="small"
                      effect="plain"
                      class="field-chip"
                    >
                      {{ indexName }}
                    </el-tag>
                    <span v-if="!row.indexNames?.length" class="empty-inline">无</span>
                  </template>
                </el-table-column>
                <el-table-column prop="matchReason" label="命中说明" min-width="220" show-overflow-tooltip />
              </el-table>
            </div>

            <div v-if="schemaPlan" class="schema-plan-preview">
              <div class="section-header compact-header">
                <h3>Schema 迁移计划</h3>
                <div class="inline-actions">
                  <el-tag :type="schemaRiskTagType(schemaPlan.riskLevel)" effect="plain">
                    {{ schemaRiskLabel(schemaPlan.riskLevel) }}
                  </el-tag>
                  <el-tag :type="hasExecutableCommentSql ? 'info' : 'success'" effect="plain">
                    {{ hasExecutableCommentSql ? 'dry-run SQL' : '无可执行 SQL' }}
                  </el-tag>
                  <el-button size="small" plain @click="handleCopySchemaPlanSummary">
                    <el-icon><Link /></el-icon>
                    复制 AI 摘要
                  </el-button>
                </div>
              </div>
              <div class="schema-plan-meta">
                <span>{{ schemaPlan.databaseType || '-' }} / {{ schemaPlan.databaseName || '-' }} / {{ schemaPlan.schemaName || '-' }}</span>
                <span>current={{ schemaPlan.currentSchemaHash?.slice(0, 12) || '-' }}</span>
                <span>target={{ schemaPlan.targetSpecHash?.slice(0, 12) || '-' }}</span>
              </div>
              <div class="summary-grid schema-plan-summary">
                <div v-for="item in schemaPlanSummaryItems" :key="item.key" class="summary-item">
                  <div class="summary-label">{{ item.label }}</div>
                  <div class="summary-value">{{ item.value }}</div>
                </div>
              </div>
              <div class="schema-plan-notes">
                <div v-if="schemaPlan.blockedReasons?.length" class="schema-plan-list danger">
                  <div class="list-title">阻塞原因</div>
                  <ul>
                    <li v-for="reason in schemaPlan.blockedReasons" :key="reason">{{ reason }}</li>
                  </ul>
                </div>
                <div v-if="schemaPlan.manualChecks?.length" class="schema-plan-list warning">
                  <div class="list-title">人工检查</div>
                  <ul>
                    <li v-for="check in schemaPlan.manualChecks" :key="check">{{ check }}</li>
                  </ul>
                </div>
                <div v-if="schemaPlan.nextActions?.length" class="schema-plan-list">
                  <div class="list-title">下一步建议</div>
                  <ul>
                    <li v-for="action in schemaPlan.nextActions" :key="action">{{ action }}</li>
                  </ul>
                </div>
              </div>
              <pre class="metadata-ai-summary">{{ schemaPlanAiSummary }}</pre>
              <el-empty v-if="schemaPlanGroups.length === 0" description="暂无 schema 变更项" />
              <el-collapse v-else class="schema-plan-groups">
                <el-collapse-item
                  v-for="group in schemaPlanGroups"
                  :key="group.tableName"
                  :name="group.tableName"
                >
                  <template #title>
                    <span class="group-title">{{ group.tableName }}</span>
                    <el-tag size="small" effect="plain">{{ group.changeSet.length }} 个变更</el-tag>
                  </template>
                  <el-table :data="group.changeSet" size="small" stripe :row-class-name="schemaPlanRowClassName">
                    <el-table-column label="动作" width="116">
                      <template #default="{ row }">
                        <el-tag size="small" :type="schemaRiskTagType(row.riskLevel)" effect="plain">
                          {{ schemaChangeActionLabel(row.action) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="字段" min-width="170">
                      <template #default="{ row }">
                        <div class="metadata-field-name">{{ row.tableName || '-' }}.{{ row.columnName || '-' }}</div>
                        <div v-if="row.standardFieldName" class="muted-text">{{ row.standardFieldName }}</div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="property" label="属性" min-width="100" />
                    <el-table-column label="风险" width="98">
                      <template #default="{ row }">
                        <el-tag size="small" :type="schemaRiskTagType(row.riskLevel)" effect="plain">
                          {{ schemaRiskLabel(row.riskLevel) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="当前值" min-width="150" show-overflow-tooltip>
                      <template #default="{ row }">{{ formatSchemaPlanValue(row.currentValue) }}</template>
                    </el-table-column>
                    <el-table-column label="目标值" min-width="150" show-overflow-tooltip>
                      <template #default="{ row }">{{ formatSchemaPlanValue(row.targetValue) }}</template>
                    </el-table-column>
                    <el-table-column prop="reason" label="说明" min-width="220" show-overflow-tooltip />
                  </el-table>
                </el-collapse-item>
              </el-collapse>
              <div class="schema-plan-sql">
                <div class="list-title">dry-run SQL 草案</div>
                <pre class="schema-plan-sql-block">{{ schemaPlan.migrationSql || '-- 暂无 SQL 草案' }}</pre>
                <div v-if="schemaPlan.rollbackHint" class="schema-plan-rollback">
                  回滚提示：{{ schemaPlan.rollbackHint }}
                </div>
              </div>
            </div>

            <div
              v-if="commentPlan"
              class="schema-plan-preview comment-plan-preview"
              :data-testid="stableTestIds.reverseImport.commentPlanPanel"
            >
              <div class="section-header compact-header">
                <h3>COMMENT 回写计划</h3>
                <div class="inline-actions">
                  <el-tag :type="commentPlanRiskTagType(commentPlan.riskLevel)" effect="plain">
                    {{ commentPlanRiskLabel(commentPlan.riskLevel) }}
                  </el-tag>
                  <el-tag type="info" effect="plain">dry-run SQL</el-tag>
                  <el-button size="small" plain @click="handleCopyCommentPlanSummary">
                    <el-icon><Link /></el-icon>
                    复制审阅摘要
                  </el-button>
                </div>
              </div>
              <div class="schema-plan-meta">
                <span>{{ commentPlan.databaseType || '-' }} / {{ commentPlan.databaseName || '-' }} / {{ commentPlan.schemaName || '-' }}</span>
                <span>metadata={{ commentPlan.metadataFingerprint?.slice(0, 12) || '-' }}</span>
                <span>plan={{ commentPlan.planHash?.slice(0, 12) || '-' }}</span>
              </div>
              <div class="summary-grid schema-plan-summary">
                <div v-for="item in commentPlanSummaryItems" :key="item.key" class="summary-item">
                  <div class="summary-label">{{ item.label }}</div>
                  <div class="summary-value">{{ item.value }}</div>
                </div>
              </div>
              <div class="schema-plan-notes">
                <div v-if="commentPlan.dialectSupport?.unsupportedReasons?.length" class="schema-plan-list warning">
                  <div class="list-title">方言限制</div>
                  <ul>
                    <li v-for="reason in commentPlan.dialectSupport.unsupportedReasons" :key="reason">{{ reason }}</li>
                  </ul>
                </div>
                <div v-if="commentPlan.nextActions?.length" class="schema-plan-list">
                  <div class="list-title">下一步建议</div>
                  <ul>
                    <li v-for="action in commentPlan.nextActions" :key="action">{{ action }}</li>
                  </ul>
                </div>
                <div v-if="commentPlan.evidence?.standardReferences?.length" class="schema-plan-list">
                  <div class="list-title">标准证据</div>
                  <ul>
                    <li v-for="refItem in commentPlan.evidence.standardReferences" :key="refItem">{{ refItem }}</li>
                  </ul>
                </div>
              </div>
              <pre class="metadata-ai-summary">{{ commentPlanCopyPayload }}</pre>
              <el-empty v-if="commentPlanGroups.length === 0" description="暂无 COMMENT 差异项" />
              <el-collapse v-else class="schema-plan-groups">
                <el-collapse-item
                  v-for="group in commentPlanGroups"
                  :key="group.tableName"
                  :name="group.tableName"
                >
                  <template #title>
                    <span class="group-title">{{ group.tableName }}</span>
                    <el-tag size="small" effect="plain">{{ group.items.length }} 个 COMMENT 项</el-tag>
                  </template>
                  <el-table :data="group.items" size="small" stripe :row-class-name="commentPlanRowClassName">
                    <el-table-column label="状态" width="116">
                      <template #default="{ row }">
                        <el-tag size="small" :type="commentPlanRiskTagType(row.riskLevel)" effect="plain">
                          {{ commentPlanStatusLabel(row.status) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="对象" min-width="180">
                      <template #default="{ row }">
                        <div class="metadata-field-name">
                          {{ row.tableName || '-' }}<span v-if="row.columnName">.{{ row.columnName }}</span>
                        </div>
                        <div v-if="row.standardFieldName" class="muted-text">{{ row.standardFieldName }}</div>
                      </template>
                    </el-table-column>
                    <el-table-column label="当前注释" min-width="160" show-overflow-tooltip>
                      <template #default="{ row }">{{ formatSchemaPlanValue(row.currentComment) }}</template>
                    </el-table-column>
                    <el-table-column label="目标注释" min-width="160" show-overflow-tooltip>
                      <template #default="{ row }">{{ formatSchemaPlanValue(row.targetComment) }}</template>
                    </el-table-column>
                    <el-table-column prop="commentDiff" label="说明" min-width="220" show-overflow-tooltip />
                    <el-table-column label="阻塞" min-width="160" show-overflow-tooltip>
                      <template #default="{ row }">
                        {{ row.blockedReasons?.join('；') || row.manualChecks?.join('；') || '-' }}
                      </template>
                    </el-table-column>
                  </el-table>
                </el-collapse-item>
              </el-collapse>
              <div class="schema-plan-sql">
                <div class="list-title">dry-run SQL 草案</div>
                <pre class="schema-plan-sql-block">{{ commentPlan.dryRunSql || '-- 暂无 SQL 草案' }}</pre>
                <div v-if="commentPlan.rollbackHint" class="schema-plan-rollback">
                  回滚提示：{{ commentPlan.rollbackHint }}
                </div>
              </div>
            </div>

            <el-alert
              class="db-hint"
              type="info"
              :closable="false"
              show-icon
              title="连接信息只用于本次请求，DataSpec 不保存数据库密码，也不会修改源数据库。"
            />
          </section>
        </el-tab-pane>
      </el-tabs>

      <div v-if="urlSourceBatchId" class="source-link-panel">
        <div>
          <strong>来源批次 #{{ urlSourceBatchId }}</strong>
          <span>当前链接定位到一次反向导入来源，可跳转字段库查看该批次字段。</span>
        </div>
        <el-button size="small" type="primary" plain @click="goToFieldLibraryBySourceBatch">
          查看批次字段
        </el-button>
      </div>

      <el-dialog
        v-model="presetDialogVisible"
        title="保存连接预设"
        width="460px"
        @closed="presetDialogFocus.restoreFocus"
      >
        <el-form label-width="82px">
          <el-form-item label="预设名">
            <el-input v-model="presetForm.name" maxlength="100" show-word-limit placeholder="例如：本地只读库" />
          </el-form-item>
          <el-form-item label="内容">
            <div class="preset-preview">
              {{ currentPresetSummary }}
            </div>
          </el-form-item>
        </el-form>
        <p class="preset-dialog-hint">仅保存连接元数据和表选择，不保存用户名、密码、token 或 JDBC URL。</p>
        <template #footer>
          <el-button @click="presetDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!canSavePreset" :loading="presetSaving" @click="handleSavePreset">
            保存
          </el-button>
        </template>
      </el-dialog>

      <section v-if="importResult" class="result-section import-result">
        <div class="section-header">
          <h3>导入结果</h3>
          <div class="result-actions">
            <el-tag type="success">已处理</el-tag>
            <el-button size="small" type="primary" plain @click="goToFieldLibrary">查看字段库</el-button>
          </div>
        </div>
        <div class="result-grid">
          <div>
            <span class="result-number">{{ importResult.importedCount ?? 0 }}</span>
            <span>新增字段</span>
          </div>
          <div>
            <span class="result-number">{{ importResult.skippedCount ?? 0 }}</span>
            <span>跳过字段</span>
          </div>
        </div>
        <div class="import-lists">
          <div>
            <div class="list-title">新增</div>
            <el-tag
              v-for="field in importResult.importedFields ?? []"
              :key="field"
              class="field-chip"
              type="success"
              effect="plain"
            >
              {{ field }}
            </el-tag>
            <span v-if="!(importResult.importedFields?.length)" class="empty-inline">无</span>
          </div>
          <div>
            <div class="list-title">跳过</div>
            <el-tag
              v-for="field in importResult.skippedFields ?? []"
              :key="field"
              class="field-chip"
              type="warning"
              effect="plain"
            >
              {{ field }}
            </el-tag>
            <span v-if="!(importResult.skippedFields?.length)" class="empty-inline">无</span>
          </div>
        </div>
        <div v-if="importDecisionRows.length" class="decision-summary">
          <div class="list-title">映射决策</div>
          <el-table :data="importDecisionRows" size="small" stripe>
            <el-table-column prop="columnName" label="字段" min-width="140" />
            <el-table-column label="决策" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="decisionTagType(row.decisionType)">
                  {{ decisionTypeLabel(row.decisionType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="matchedFieldName" label="标准字段" min-width="140" />
            <el-table-column prop="matchReason" label="匹配理由" min-width="240" show-overflow-tooltip />
            <el-table-column prop="confirmReason" label="确认理由" min-width="220" show-overflow-tooltip />
            <el-table-column prop="ignoreReason" label="忽略理由" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>
      </section>

      <section v-if="compareResult" class="result-section compare-result">
        <div class="section-header">
          <h3>数据库差异</h3>
          <el-radio-group v-model="compareStatusFilter" size="small">
            <el-radio-button
              v-for="option in compareStatusOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </div>

        <div class="summary-grid">
          <div v-for="item in compareSummaryItems" :key="item.key" class="summary-item">
            <div class="summary-label">{{ item.label }}</div>
            <div class="summary-value">{{ item.value }}</div>
          </div>
        </div>

        <el-empty v-if="compareGroups.length === 0" description="当前筛选下暂无差异" />
        <el-collapse v-else class="compare-groups">
          <el-collapse-item
            v-for="group in compareGroups"
            :key="group.tableName"
            :name="group.tableName"
          >
            <template #title>
              <span class="group-title">{{ group.tableName }}</span>
              <el-tag size="small" effect="plain">{{ group.fieldDiffs.length }} 个字段</el-tag>
            </template>
            <el-table :data="group.fieldDiffs" stripe>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag size="small" :type="compareStatusTagType(row.status)" effect="plain">
                    {{ compareStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="columnName" label="数据库字段" min-width="150" />
              <el-table-column prop="dataType" label="数据库类型" min-width="130" />
              <el-table-column label="标准字段" min-width="160">
                <template #default="{ row }">
                  <div>{{ row.standardFieldName || '-' }}</div>
                  <div v-if="row.standardDisplayName" class="muted-text">{{ row.standardDisplayName }}</div>
                </template>
              </el-table-column>
              <el-table-column label="变化" min-width="260">
                <template #default="{ row }">
                  <div v-if="row.changes?.length" class="change-list">
                    <el-tag
                      v-for="change in row.changes"
                      :key="`${row.tableName}.${row.columnName}.${change.property}`"
                      size="small"
                      effect="plain"
                    >
                      {{ changeText(change) }}
                    </el-tag>
                  </div>
                  <span v-else class="empty-inline">无</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="原因" min-width="240" show-overflow-tooltip />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </section>

      <section v-if="preview" class="result-section">
        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.key" class="summary-item">
            <div class="summary-label">{{ item.label }}</div>
            <div class="summary-value">{{ item.value }}</div>
          </div>
        </div>

        <div v-if="previewDialectDiagnostics.length" class="dialect-panel">
          <div class="dialect-header">
            <span>方言诊断</span>
            <el-tag size="small" :type="diagnosticSummaryTagType(previewDialectDiagnostics)">
              {{ dialectSummary(previewDialectDiagnostics) }}
            </el-tag>
          </div>
          <div class="diagnostic-list">
            <div
              v-for="diagnostic in previewDialectDiagnostics"
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

        <el-tabs class="result-tabs" :data-testid="stableTestIds.reverseImport.previewTabs">
          <el-tab-pane>
            <template #label>
              <span :data-testid="stableTestIds.reverseImport.fieldCandidatesTab">字段候选</span>
            </template>
            <template v-if="activeMode === 'database'">
              <div class="candidate-toolbar">
                <span>已选 {{ selectedCandidateCount }} / {{ candidateTotal }}</span>
                <div>
                  <el-button size="small" :disabled="candidateTotal === 0" @click="selectAllCandidates">全选候选</el-button>
                  <el-button size="small" :disabled="selectedCandidateCount === 0" @click="clearSelectedCandidates">清空候选</el-button>
                </div>
              </div>

              <el-empty v-if="candidateGroups.length === 0" description="暂无字段候选" />
              <el-collapse v-else class="candidate-groups">
                <el-collapse-item
                  v-for="group in candidateGroups"
                  :key="group.tableName"
                  :name="group.tableName"
                >
                  <template #title>
                    <span class="group-title">{{ group.tableName }}</span>
                    <el-tag size="small" effect="plain">{{ group.candidates.length }} 个字段</el-tag>
                  </template>
                  <el-table :data="group.candidates" stripe>
                    <el-table-column label="" width="56">
                      <template #default="{ row }">
                        <el-checkbox
                          :model-value="isCandidateSelected(row)"
                          @change="handleCandidateCheck(row, $event)"
                        />
                      </template>
                    </el-table-column>
                    <el-table-column prop="columnName" label="字段" min-width="140" />
                    <el-table-column prop="dataType" label="类型" min-width="130" />
                    <el-table-column label="空值" width="90">
                      <template #default="{ row }">{{ row.nullable ? '可空' : '非空' }}</template>
                    </el-table-column>
                    <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
                    <el-table-column label="确认理由" min-width="260">
                      <template #default="{ row }">
                        <el-input
                          :model-value="candidateReasonValue(row)"
                          size="small"
                          maxlength="200"
                          @input="updateCandidateConfirmReason(row, String($event))"
                        />
                        <div v-if="row.matchReason" class="decision-hint">{{ row.matchReason }}</div>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-collapse-item>
              </el-collapse>
            </template>
            <el-table v-else :data="preview.fieldCandidates ?? []" stripe empty-text="暂无字段候选">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="dataType" label="类型" min-width="130" />
              <el-table-column label="空值" width="90">
                <template #default="{ row }">{{ row.nullable ? '可空' : '非空' }}</template>
              </el-table-column>
              <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="缺注释">
            <el-table :data="preview.missingComments ?? []" stripe empty-text="暂无缺注释项">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column label="对象" width="100">
                <template #default="{ row }">{{ row.targetType === 'table' ? '表' : '字段' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="非标准字段">
            <el-table :data="preview.nonStandardFields ?? []" stripe empty-text="暂无非标准字段">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="recommendedName" label="建议名" min-width="140" />
              <el-table-column prop="reason" label="原因" min-width="220" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="解析表">
            <el-table :data="tableRows" stripe empty-text="暂无解析结果">
              <el-table-column prop="tableName" label="表" min-width="140" />
              <el-table-column prop="columnName" label="字段" min-width="140" />
              <el-table-column prop="dataType" label="类型" min-width="130" />
              <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Check, Connection, DataAnalysis, Link, Refresh, Search, Upload, View } from '@element-plus/icons-vue'
import {
  createDatabaseConnectionPreset,
  listDatabaseConnectionPresets
} from '@/api/databaseConnectionPreset'
import {
  browseDatabaseMetadata,
  compareDatabaseReverseImport,
  importDatabaseCandidates,
  listDatabaseTables,
  planDatabaseCommentPatch,
  planDatabaseSchemaChange,
  previewDatabaseReverseImport,
  previewReverseImport,
  scanDatabaseMetadata,
  testDatabaseConnection
} from '@/api/reverseImport'
import { createClientIdempotencyKey } from '@/api/idempotency'
import { useDialogFocusReturn } from '@/composables/useDialogFocusReturn'
import { useProjectStore } from '@/stores/project'
import {
  attachCandidateConfirmReasons,
  buildIgnoredCandidates,
  buildCandidateKey,
  countSelectedVisibleTableNames,
  defaultCandidateConfirmReason,
  filterDatabaseTables,
  groupFieldCandidatesByTable,
  mergeSelectedTableNames,
  pickSelectedCandidates
} from '@/utils/reverseImportSelection'
import {
  buildBrowserCandidateKeySet,
  buildMetadataBrowserAiSummary,
  filterMetadataBrowserRows,
  flattenMetadataBrowserRows,
  metadataBrowserStatusLabel,
  metadataBrowserStatusTagType,
  type DatabaseMetadataBrowserRow
} from '@/utils/databaseMetadataBrowser'
import {
  buildScanEvidenceSummary,
  buildScanFailureSummary,
  buildMetadataCacheSummary,
  buildScanResumeSummary,
  buildSourcePressureHintText,
  currentSuccessfulScanTableNames,
  mergeScanTableNames,
  metadataCacheStatusLabel,
  selectSuccessfulPartialTableNames,
  scanJobStatusLabel,
  scanProgressLabel
} from '@/utils/databaseMetadataScan'
import { saveScanPartialCoveragePayload } from '@/utils/scanPartialCoverage'
import {
  fieldLibraryQueryForImportResult,
  loadReverseImportMemory,
  saveReverseImportMemory,
  type ReverseImportMemoryState
} from '@/utils/reverseImportMemory'
import {
  normalizeDatabaseConnectionPresetPayload,
  presetConnectionSummary,
  presetOptionLabel
} from '@/utils/databaseConnectionPreset'
import {
  diagnosticLevelLabel,
  diagnosticSummaryTagType,
  diagnosticTagType,
  dialectSummary
} from '@/utils/dialectDiagnostics'
import {
  capabilitySupportLabel,
  connectionStatusLabel,
  connectionStatusTagType,
  databaseHealthSummary,
  databaseSecuritySummary,
  failureCategoryLabel,
  metadataReadableLabel,
  readOnlyLabel,
  retryableLabel,
  securityRiskLabel,
  securityRiskTagType,
  writeRiskLabel
} from '@/utils/databaseSecurityDiagnostic'
import {
  buildSchemaPlanAiSummary,
  schemaChangeActionLabel,
  schemaRiskLabel,
  schemaRiskTagType
} from '@/utils/databaseSchemaPlan'
import {
  buildCommentPlanCopyPayload,
  buildCommentPlanTableGroups,
  commentPlanRiskLabel,
  commentPlanRiskTagType,
  commentPlanStatusLabel
} from '@/utils/databaseCommentPlan'
import { reverseImportTableOptionTestId, stableTestIds } from '@/utils/stableTestIds'
import { copyRouteUrl, readEnumQuery, readPositiveIntQuery, readStringQuery, replaceRouteQuery } from '@/utils/urlState'
import type {
  DatabaseConnectionReq,
  DatabaseConnectionHealthDiagnostic,
  DatabaseConnectionPreset,
  DatabaseConnectionPresetReq,
  DatabaseConnectionSecurityDiagnostic,
  DatabaseCommentPatchPlan,
  DatabaseCommentPatchPlanItem,
  DatabaseImportResult,
  DatabaseMetadataBrowser,
  DatabaseMetadataCacheMode,
  DatabaseMetadataScanReq,
  DatabaseMetadataScanResult,
  DatabaseSchemaChangeItem,
  DatabaseSchemaChangePlan,
  DatabaseTableInfo,
  FieldCandidate,
  ReverseImportDecision,
  ReverseImportCompareResult,
  ReverseImportFieldChange,
  ReverseImportFieldDiff,
  ReverseImportFieldStatus,
  ReverseImportTableDiff,
  ReverseImportPreview
} from '@/types'

type ReverseImportMode = 'sql' | 'database'
type ConnectionStatus = 'idle' | 'success' | 'error'
type CompareStatusFilter = 'ALL' | ReverseImportFieldStatus
type CompareTableGroup = Omit<ReverseImportTableDiff, 'fieldDiffs'> & {
  fieldDiffs: ReverseImportFieldDiff[]
}
type SchemaPlanTableGroup = {
  tableName: string
  changeSet: DatabaseSchemaChangeItem[]
}

const projectStore = useProjectStore()
const route = useRoute()
const router = useRouter()
const activeMode = ref<ReverseImportMode>('sql')
const sqlText = ref('')
const preview = ref<ReverseImportPreview | null>(null)
const compareResult = ref<ReverseImportCompareResult | null>(null)
const schemaPlan = ref<DatabaseSchemaChangePlan | null>(null)
const commentPlan = ref<DatabaseCommentPatchPlan | null>(null)
const importResult = ref<DatabaseImportResult | null>(null)
const previewLoading = ref(false)
const compareLoading = ref(false)
const schemaPlanLoading = ref(false)
const commentPlanLoading = ref(false)
const testLoading = ref(false)
const tableLoading = ref(false)
const scanLoading = ref(false)
const metadataLoading = ref(false)
const importLoading = ref(false)
const presetLoading = ref(false)
const presetSaving = ref(false)
const presetDialogVisible = ref(false)
const presetDialogFocus = useDialogFocusReturn(presetDialogVisible)
const restoringMemory = ref(false)
const databaseTables = ref<DatabaseTableInfo[]>([])
const metadataBrowser = ref<DatabaseMetadataBrowser | null>(null)
const scanResult = ref<DatabaseMetadataScanResult | null>(null)
const successfulScanTableNames = ref<Set<string>>(new Set())
const tableSearch = ref('')
const metadataSearch = ref('')
const scanPageSize = ref(50)
const connectionStatus = ref<ConnectionStatus>('idle')
const connectionMessage = ref('')
const connectionSecurity = ref<DatabaseConnectionSecurityDiagnostic | null>(null)
const connectionHealth = ref<DatabaseConnectionHealthDiagnostic | null>(null)
const selectedCandidateKeys = ref<Set<string>>(new Set())
const candidateConfirmReasons = ref<Record<string, string>>({})
const presets = ref<DatabaseConnectionPreset[]>([])
const presetId = ref<number | null>(null)
const presetForm = reactive({
  name: ''
})
const dbForm = reactive<DatabaseConnectionReq>({
  databaseType: 'postgresql',
  host: 'localhost',
  port: 5432,
  databaseName: '',
  schemaName: 'public',
  username: '',
  password: '',
  tableNames: []
})

const hasProject = computed(() => projectStore.currentProjectId !== null)
const canPreviewSql = computed(() => hasProject.value && sqlText.value.trim().length > 0)
const canUseDatabaseConnection = computed(() =>
  hasProject.value
  && Boolean(dbForm.databaseType)
  && Boolean(dbForm.host?.trim())
  && Boolean(dbForm.databaseName?.trim())
  && Boolean(dbForm.username?.trim())
)
const canOpenPresetDialog = computed(() =>
  hasProject.value
  && Boolean(dbForm.databaseType)
  && Boolean(dbForm.host?.trim())
  && Boolean(dbForm.port)
  && Boolean(dbForm.databaseName?.trim())
)
const canSavePreset = computed(() =>
  canOpenPresetDialog.value && Boolean(presetForm.name.trim())
)
const canPreviewDatabase = computed(() =>
  canUseDatabaseConnection.value && selectedSuccessfulTableCount.value > 0
)
const canGeneratePreview = computed(() =>
  activeMode.value === 'sql' ? canPreviewSql.value : canPreviewDatabase.value
)
const canGenerateCompare = computed(() =>
  activeMode.value === 'database' && canPreviewDatabase.value
)
const canGenerateSchemaPlan = computed(() =>
  activeMode.value === 'database' && canPreviewDatabase.value
)
const canGenerateCommentPlan = computed(() =>
  activeMode.value === 'database' && canPreviewDatabase.value
)
const canScanMetadata = computed(() =>
  activeMode.value === 'database' && canUseDatabaseConnection.value
)
const canContinueScan = computed(() =>
  canScanMetadata.value
  && Boolean(scanResult.value?.progress?.hasMore)
  && Boolean(scanResult.value?.resumeCursor ?? scanResult.value?.cursor)
  && !scanResult.value?.cancelled
)
const canCancelScan = computed(() =>
  canScanMetadata.value
  && Boolean(scanResult.value?.cancelToken ?? scanResult.value?.scanJobId ?? scanResult.value?.scanId)
  && !scanResult.value?.cancelled
  && scanResult.value?.status !== 'COMPLETED'
)
const canGenerateScanPartialCoverage = computed(() => {
  const partial = scanResult.value?.partialResult
  return activeMode.value === 'database'
    && hasProject.value
    && Boolean(partial?.completeForCoverage || partial?.successfulTableNames?.length)
})
const canBrowseMetadata = computed(() =>
  activeMode.value === 'database' && canPreviewDatabase.value
)
const filteredDatabaseTables = computed(() =>
  filterDatabaseTables(databaseTables.value, tableSearch.value)
)
const metadataRows = computed<DatabaseMetadataBrowserRow[]>(() =>
  flattenMetadataBrowserRows(metadataBrowser.value)
)
const filteredMetadataRows = computed(() =>
  filterMetadataBrowserRows(metadataRows.value, metadataSearch.value)
)
const presetOptions = computed<Array<DatabaseConnectionPreset & { id: number }>>(() =>
  presets.value.filter((preset): preset is DatabaseConnectionPreset & { id: number } => typeof preset.id === 'number')
)
const selectedPreset = computed(() =>
  presetOptions.value.find((preset) => preset.id === presetId.value) ?? null
)
const currentPresetSummary = computed(() =>
  presetConnectionSummary(presetPayload())
)
const selectedTableCount = computed(() => dbForm.tableNames?.length ?? 0)
const selectedSuccessfulTableCount = computed(() => selectedSuccessfulTableNames().length)
const currentPageSelectedTableCount = computed(() =>
  countSelectedVisibleTableNames(dbForm.tableNames, databaseTables.value)
)
const candidateTotal = computed(() => preview.value?.fieldCandidates?.length ?? 0)
const selectedCandidateCount = computed(() => selectedCandidateKeys.value.size)
const selectedFieldCandidates = computed(() =>
  attachCandidateConfirmReasons(
    pickSelectedCandidates(preview.value?.fieldCandidates ?? [], selectedCandidateKeys.value),
    candidateConfirmReasons.value
  )
)
const canImportCandidates = computed(() =>
  activeMode.value === 'database' && selectedFieldCandidates.value.length > 0
)
const ignoredFieldCandidates = computed(() =>
  buildIgnoredCandidates(preview.value?.fieldCandidates ?? [], selectedCandidateKeys.value)
)
const candidateGroups = computed(() =>
  groupFieldCandidatesByTable(preview.value?.fieldCandidates ?? [])
)
const importDecisionRows = computed<ReverseImportDecision[]>(() => importResult.value?.mappingDecisions ?? [])
const databaseStep = computed(() => {
  if (importResult.value) {
    return 3
  }
  if (preview.value || compareResult.value || schemaPlan.value || commentPlan.value) {
    return 2
  }
  if (selectedTableCount.value > 0) {
    return 1
  }
  return 0
})
const connectionStatusText = computed(() => {
  if (connectionStatus.value === 'success') {
    return connectionMessage.value || '连接可用'
  }
  if (connectionStatus.value === 'error') {
    return connectionMessage.value || '连接失败'
  }
  return '未测试'
})
const connectionTagType = computed(() => {
  if (connectionStatus.value === 'success') {
    return 'success'
  }
  if (connectionStatus.value === 'error') {
    return 'danger'
  }
  return 'info'
})
const summaryItems = computed(() => [
  { key: 'tables', label: '表', value: preview.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: preview.value?.summary?.columnCount ?? 0 },
  { key: 'candidates', label: '字段候选', value: preview.value?.summary?.candidateCount ?? 0 },
  { key: 'comments', label: '缺注释', value: preview.value?.summary?.missingCommentCount ?? 0 },
  { key: 'nonStandard', label: '非标准字段', value: preview.value?.summary?.nonStandardFieldCount ?? 0 }
])
const metadataSummaryItems = computed(() => [
  { key: 'tables', label: '表', value: metadataBrowser.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: metadataBrowser.value?.summary?.columnCount ?? 0 },
  { key: 'indexes', label: '索引', value: metadataBrowser.value?.summary?.indexCount ?? 0 },
  { key: 'candidates', label: '候选', value: metadataBrowser.value?.summary?.candidateCount ?? 0 },
  { key: 'comments', label: '缺注释', value: metadataBrowser.value?.summary?.missingCommentCount ?? 0 },
  { key: 'changed', label: '类型差异', value: metadataBrowser.value?.summary?.changedCount ?? 0 },
  { key: 'unmanaged', label: '未纳管', value: metadataBrowser.value?.summary?.unmanagedCount ?? 0 }
])
const metadataAiSummary = computed(() =>
  buildMetadataBrowserAiSummary(metadataBrowser.value)
)
const activeMetadataCache = computed(() =>
  metadataBrowser.value?.metadataCache
  ?? scanResult.value?.metadataCache
  ?? preview.value?.metadataCache
  ?? compareResult.value?.metadataCache
  ?? schemaPlan.value?.metadataCache
  ?? null
)
const metadataCacheSummaryText = computed(() =>
  buildMetadataCacheSummary(activeMetadataCache.value)
)
const metadataCacheTagType = computed(() => {
  const cache = activeMetadataCache.value
  if (!cache) {
    return 'info'
  }
  if (cache.refreshMode === 'BYPASS') {
    return 'warning'
  }
  if (cache.cacheHit) {
    return 'success'
  }
  if (cache.refreshMode === 'REFRESH') {
    return cache.changeSummary?.changed ? 'warning' : 'success'
  }
  return cache.stale ? 'warning' : 'info'
})
const scanProgressText = computed(() =>
  scanProgressLabel(scanResult.value)
)
const scanStatusText = computed(() =>
  scanJobStatusLabel(scanResult.value)
)
const scanResumeText = computed(() =>
  buildScanResumeSummary(scanResult.value)
)
const scanSourcePressureText = computed(() =>
  buildSourcePressureHintText(scanResult.value)
)
const scanFailureText = computed(() =>
  buildScanFailureSummary(scanResult.value)
)
const scanEvidenceText = computed(() =>
  buildScanEvidenceSummary(scanResult.value)
)
const previewDialectDiagnostics = computed(() => preview.value?.dialectDiagnostics ?? [])
const compareSummaryItems = computed(() => [
  { key: 'tables', label: '表', value: compareResult.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: compareResult.value?.summary?.columnCount ?? 0 },
  { key: 'matched', label: '命中标准', value: compareResult.value?.summary?.matchedCount ?? 0 },
  { key: 'changed', label: '属性变化', value: compareResult.value?.summary?.changedCount ?? 0 },
  { key: 'new', label: '新增字段', value: compareResult.value?.summary?.newCount ?? 0 },
  { key: 'comments', label: '缺注释', value: compareResult.value?.summary?.missingCommentCount ?? 0 },
  { key: 'nonStandard', label: '非标准', value: compareResult.value?.summary?.nonStandardCount ?? 0 }
])
const tableRows = computed(() =>
  (preview.value?.tables ?? []).flatMap((table) =>
    (table.columns ?? []).map((column) => ({
      tableName: table.name,
      columnName: column.name,
      dataType: column.dataType,
      comment: column.comment
    }))
  )
)
const compareStatusFilter = ref<CompareStatusFilter>('ALL')
const compareStatusOptions: Array<{ value: CompareStatusFilter; label: string }> = [
  { value: 'ALL', label: '全部' },
  { value: 'CHANGED', label: '属性变化' },
  { value: 'NEW', label: '新增' },
  { value: 'NON_STANDARD', label: '非标准' },
  { value: 'MISSING_COMMENT', label: '缺注释' },
  { value: 'MATCHED', label: '已匹配' }
]
const compareGroups = computed<CompareTableGroup[]>(() =>
  (compareResult.value?.tableDiffs ?? [])
    .map((group) => ({
      ...group,
      fieldDiffs: (group.fieldDiffs ?? []).filter((diff) =>
        compareStatusFilter.value === 'ALL'
        || diff.status === compareStatusFilter.value
        || (compareStatusFilter.value === 'NON_STANDARD' && Boolean(diff.nonStandard))
      )
    }))
    .filter((group) => group.fieldDiffs.length > 0)
)
const schemaPlanSummaryItems = computed(() => [
  { key: 'risk', label: '整体风险', value: schemaRiskLabel(schemaPlan.value?.riskLevel) },
  { key: 'tables', label: '表', value: schemaPlan.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: schemaPlan.value?.summary?.columnCount ?? 0 },
  { key: 'changes', label: '变更项', value: schemaPlan.value?.summary?.changeCount ?? 0 },
  { key: 'blocked', label: '阻塞项', value: schemaPlan.value?.summary?.blockedCount ?? 0 },
  { key: 'high', label: '高风险', value: schemaPlan.value?.summary?.highRiskCount ?? 0 }
])
const schemaPlanGroups = computed<SchemaPlanTableGroup[]>(() => {
  const groups = new Map<string, DatabaseSchemaChangeItem[]>()
  for (const item of schemaPlan.value?.changeSet ?? []) {
    const tableName = item.tableName || '未命名表'
    groups.set(tableName, [...(groups.get(tableName) ?? []), item])
  }
  return [...groups.entries()].map(([tableName, changeSet]) => ({ tableName, changeSet }))
})
const schemaPlanAiSummary = computed(() =>
  buildSchemaPlanAiSummary(schemaPlan.value)
)
const commentPlanSummaryItems = computed(() => [
  { key: 'risk', label: '整体风险', value: commentPlanRiskLabel(commentPlan.value?.riskLevel) },
  { key: 'tables', label: '表', value: commentPlan.value?.summary?.tableCount ?? 0 },
  { key: 'columns', label: '字段', value: commentPlan.value?.summary?.columnCount ?? 0 },
  { key: 'changes', label: '可执行', value: commentPlan.value?.summary?.executableChangeCount ?? 0 },
  { key: 'unsupported', label: '不支持', value: commentPlan.value?.summary?.unsupportedCount ?? 0 },
  { key: 'blocked', label: '阻塞项', value: commentPlan.value?.summary?.blockedCount ?? 0 }
])
const hasExecutableCommentSql = computed(() =>
  (commentPlan.value?.summary?.executableChangeCount ?? 0) > 0
  && Boolean(commentPlan.value?.dryRunSql?.trim())
)
const commentPlanGroups = computed(() =>
  buildCommentPlanTableGroups(commentPlan.value)
)
const commentPlanCopyPayload = computed(() =>
  buildCommentPlanCopyPayload(commentPlan.value)
)
const urlSourceBatchId = computed(() =>
  readPositiveIntQuery(route.query, 'sourceBatchId') ?? readPositiveIntQuery(route.query, 'batchId')
)

onMounted(async () => {
  if (!projectStore.currentProjectId && projectStore.projects.length === 0) {
    await projectStore.loadProjects()
  }
  applySavedReverseImportMemory()
  applyReverseImportUrlState()
  await loadPresets()
})

watch(
  () => projectStore.currentProjectId,
  () => {
    resetResults()
    databaseTables.value = []
    dbForm.tableNames = []
    successfulScanTableNames.value = new Set()
    tableSearch.value = ''
    metadataSearch.value = ''
    presetId.value = null
    presets.value = []
    resetConnectionStatus()
    applySavedReverseImportMemory()
    applyReverseImportUrlState()
    void loadPresets()
  }
)

watch(activeMode, () => {
  if (restoringMemory.value) {
    return
  }
  resetResults()
})

watch(
  () => [...(dbForm.tableNames ?? [])],
  () => {
    if (restoringMemory.value) {
      return
    }
    resetResults()
  }
)

watch(
  () => [
    dbForm.databaseType,
    dbForm.host,
    dbForm.port,
    dbForm.databaseName,
    dbForm.schemaName,
    dbForm.username,
    dbForm.password
  ],
  () => {
    if (restoringMemory.value) {
      return
    }
    resetConnectionStatus()
    databaseTables.value = []
    dbForm.tableNames = []
    successfulScanTableNames.value = new Set()
    tableSearch.value = ''
    metadataSearch.value = ''
    resetResults()
  }
)

watch(
  () => [
    projectStore.currentProjectId,
    activeMode.value,
    tableSearch.value,
    compareStatusFilter.value,
    dbForm.databaseType,
    dbForm.host,
    dbForm.port,
    dbForm.databaseName,
    dbForm.schemaName,
    dbForm.username,
    [...(dbForm.tableNames ?? [])].join('\u0000')
  ],
  () => {
    persistReverseImportMemory()
  }
)

watch(
  () => [route.query.table, route.query.status, route.query.sourceBatchId, route.query.batchId],
  () => applyReverseImportUrlState()
)

watch([tableSearch, compareStatusFilter], () => {
  if (restoringMemory.value) {
    return
  }
  void syncReverseImportUrlState()
})

function resetResults() {
  preview.value = null
  compareResult.value = null
  schemaPlan.value = null
  commentPlan.value = null
  importResult.value = null
  metadataBrowser.value = null
  metadataSearch.value = ''
  selectedCandidateKeys.value = new Set()
  candidateConfirmReasons.value = {}
}

function resetConnectionStatus() {
  connectionStatus.value = 'idle'
  connectionMessage.value = ''
  connectionSecurity.value = null
  connectionHealth.value = null
}

function clearSql() {
  sqlText.value = ''
  resetResults()
}

async function loadPresets() {
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    presets.value = []
    presetId.value = null
    return
  }
  presetLoading.value = true
  try {
    presets.value = await listDatabaseConnectionPresets(projectId)
    if (presetId.value && !presetOptions.value.some((preset) => preset.id === presetId.value)) {
      presetId.value = null
    }
  } finally {
    presetLoading.value = false
  }
}

function openPresetDialog() {
  presetDialogFocus.rememberFocus()
  presetForm.name = selectedPreset.value?.name?.trim() || defaultPresetName()
  presetDialogVisible.value = true
}

async function handleSavePreset() {
  if (!canSavePreset.value) {
    ElMessage.warning('请补齐预设名、主机、端口和数据库名')
    return
  }
  const payload = presetPayload()
  if (!isCompletePresetPayload(payload)) {
    ElMessage.warning('请补齐预设名、主机、端口和数据库名')
    return
  }
  presetSaving.value = true
  try {
    const saved = await createDatabaseConnectionPreset(payload)
    await loadPresets()
    presetId.value = saved.id ?? null
    presetDialogVisible.value = false
    ElMessage.success('连接预设已保存')
  } finally {
    presetSaving.value = false
  }
}

function handlePresetChange(value: number | string | null | undefined) {
  if (!value) {
    return
  }
  const id = Number(value)
  const preset = presetOptions.value.find((item) => item.id === id)
  if (!preset) {
    return
  }
  applyDatabasePreset(preset)
}

function applyDatabasePreset(preset: DatabaseConnectionPreset) {
  restoringMemory.value = true
  if (isDatabaseType(preset.databaseType)) {
    dbForm.databaseType = preset.databaseType
  }
  if (preset.host !== undefined) {
    dbForm.host = preset.host
  }
  if (preset.port !== undefined) {
    dbForm.port = preset.port
  }
  if (preset.databaseName !== undefined) {
    dbForm.databaseName = preset.databaseName
  }
  dbForm.schemaName = preset.schemaName ?? ''
  // 预设只复用连接元数据；切换连接时清空凭据，避免把上一连接的账号密码带到新库。
  dbForm.username = ''
  dbForm.password = ''
  dbForm.tableNames = [...(preset.tableNames ?? [])]
  databaseTables.value = []
  tableSearch.value = ''
  resetConnectionStatus()
  resetResults()
  void nextTick(() => {
    restoringMemory.value = false
    persistReverseImportMemory()
  })
  ElMessage.success(`已加载预设：${presetOptionLabel(preset)}`)
}

function presetPayload(): DatabaseConnectionPresetReq {
  return normalizeDatabaseConnectionPresetPayload({
    ...dbForm,
    projectId: projectStore.currentProjectId ?? undefined,
    name: presetForm.name,
    tableNames: [...(dbForm.tableNames ?? [])]
  })
}

function isCompletePresetPayload(payload: DatabaseConnectionPresetReq): payload is Required<Pick<
  DatabaseConnectionPresetReq,
  'projectId' | 'name' | 'databaseType' | 'host' | 'port' | 'databaseName'
>> & DatabaseConnectionPresetReq {
  return Boolean(
    payload.projectId
    && payload.name
    && payload.databaseType
    && payload.host
    && payload.port
    && payload.databaseName
  )
}

function defaultPresetName() {
  const database = [dbForm.databaseName, dbForm.schemaName].filter(Boolean).join(' / ')
  return database || [dbForm.host, dbForm.port].filter(Boolean).join(':') || '数据库连接'
}

function selectedSuccessfulTableNames(): string[] {
  return selectSuccessfulPartialTableNames(dbForm.tableNames, scanResult.value, successfulScanTableNames.value)
}

function databaseRequest(metadataCacheMode = 'AUTO', tableNames: string[] = selectedSuccessfulTableNames()): DatabaseConnectionReq {
  return {
    ...dbForm,
    projectId: projectStore.currentProjectId ?? undefined,
    presetId: presetId.value ?? undefined,
    tableNames: [...tableNames],
    metadataCacheMode: metadataCacheMode as DatabaseMetadataCacheMode
  }
}

async function handleGeneratePreview() {
  if (activeMode.value === 'sql') {
    await handleSqlPreview()
    return
  }
  await handleDatabasePreview()
}

async function handleSqlPreview() {
  if (!projectStore.currentProjectId || !sqlText.value.trim()) {
    return
  }
  previewLoading.value = true
  try {
    preview.value = await previewReverseImport(projectStore.currentProjectId, sqlText.value)
    importResult.value = null
  } finally {
    previewLoading.value = false
  }
}

async function handleDatabasePreview() {
  if (!canPreviewDatabase.value) {
    return
  }
  previewLoading.value = true
  try {
    preview.value = await previewDatabaseReverseImport(databaseRequest())
    selectAllCandidates()
    importResult.value = null
  } finally {
    previewLoading.value = false
  }
}

async function handleGenerateCompare() {
  if (!canGenerateCompare.value) {
    return
  }
  compareLoading.value = true
  try {
    compareResult.value = await compareDatabaseReverseImport(databaseRequest())
    importResult.value = null
    applyReverseImportUrlState()
  } finally {
    compareLoading.value = false
  }
}

async function handleGenerateSchemaPlan() {
  if (!canGenerateSchemaPlan.value) {
    return
  }
  schemaPlanLoading.value = true
  try {
    schemaPlan.value = await planDatabaseSchemaChange(databaseRequest())
    importResult.value = null
    connectionStatus.value = 'success'
    connectionMessage.value = `已生成 ${schemaPlan.value.summary?.changeCount ?? 0} 个 schema 变更项`
    ElMessage.success('迁移计划预览已生成')
  } finally {
    schemaPlanLoading.value = false
  }
}

async function handleGenerateCommentPlan() {
  if (!canGenerateCommentPlan.value) {
    return
  }
  commentPlanLoading.value = true
  try {
    commentPlan.value = await planDatabaseCommentPatch(databaseRequest())
    importResult.value = null
    connectionStatus.value = 'success'
    connectionMessage.value = `已生成 ${commentPlan.value.summary?.executableChangeCount ?? 0} 个 COMMENT 变更项`
    ElMessage.success('COMMENT 回写计划已生成')
  } finally {
    commentPlanLoading.value = false
  }
}

async function handleTestConnection() {
  if (!canUseDatabaseConnection.value) {
    return
  }
  testLoading.value = true
  try {
    const result = await testDatabaseConnection(databaseRequest())
    connectionHealth.value = result.health ?? null
    connectionSecurity.value = result.security ?? null
    if (result.success) {
      connectionStatus.value = 'success'
      connectionMessage.value = result.message || '连接成功'
      ElMessage.success(result.message || '连接成功')
    } else {
      connectionStatus.value = 'error'
      connectionMessage.value = result.message || '连接失败'
      connectionSecurity.value = null
      ElMessage.error(result.message || '连接失败')
    }
  } finally {
    testLoading.value = false
  }
}

async function handleLoadTables() {
  if (!canUseDatabaseConnection.value) {
    return
  }
  tableLoading.value = true
  try {
    const previousSelection = new Set(dbForm.tableNames ?? [])
    databaseTables.value = await listDatabaseTables(databaseRequest())
    scanResult.value = null
    successfulScanTableNames.value = new Set()
    const availableTables = new Set(
      databaseTables.value
        .map((table) => table.tableName)
        .filter((tableName): tableName is string => Boolean(tableName))
    )
    dbForm.tableNames = [...previousSelection].filter((tableName) => availableTables.has(tableName))
    tableSearch.value = ''
    connectionStatus.value = 'success'
    connectionMessage.value = `已加载 ${databaseTables.value.length} 张表`
    if (connectionHealth.value?.connectionStatus === 'FAILED') {
      connectionHealth.value = null
    }
    resetResults()
    ElMessage.success(`已加载 ${databaseTables.value.length} 张表`)
  } finally {
    tableLoading.value = false
  }
}

async function handleStartMetadataScan() {
  successfulScanTableNames.value = new Set()
  await runMetadataScan({ cursor: null, resetTables: true })
}

async function handleContinueMetadataScan() {
  if (!canContinueScan.value) {
    return
  }
  await runMetadataScan({ cursor: scanResult.value?.resumeCursor ?? scanResult.value?.cursor ?? null, resetTables: false })
}

async function handleCancelMetadataScan() {
  if (!canCancelScan.value) {
    return
  }
  await runMetadataScan({ cursor: scanResult.value?.resumeCursor ?? scanResult.value?.cursor ?? null, resetTables: false, cancel: true })
}

async function handleGenerateScanPartialCoverage() {
  const projectId = projectStore.currentProjectId
  if (!projectId || !scanResult.value?.partialResult || !canGenerateScanPartialCoverage.value) {
    return
  }
  const scanPartialId = saveScanPartialCoveragePayload({
    projectId,
    partialResult: scanResult.value.partialResult,
    failureSummary: scanResult.value.failureSummary,
    scanStatus: scanResult.value.status
  })
  await router.push({ path: '/field-coverage', query: { projectId, scanPartialId } })
}

async function runMetadataScan(options: { cursor: string | null; resetTables: boolean; cancel?: boolean }) {
  if (!canScanMetadata.value) {
    return
  }
  scanLoading.value = true
  try {
    const result = await scanDatabaseMetadata(scanRequest(options.cursor, Boolean(options.cancel)))
    scanResult.value = result
    if (options.cancel || result.cancelled) {
      connectionMessage.value = '扫描已取消'
      ElMessage.warning('扫描已取消')
      return
    }
    databaseTables.value = result.tables ?? []
    if (options.resetTables) {
      tableSearch.value = ''
    }
    for (const tableName of currentSuccessfulScanTableNames(result)) {
      successfulScanTableNames.value.add(tableName)
    }
    dbForm.tableNames = mergeScanTableNames(dbForm.tableNames, result)
    connectionStatus.value = 'success'
    connectionMessage.value = `已扫描 ${result.progress?.processedTableCount ?? 0} / ${result.estimatedTableCount ?? 0} 张表`
    resetResults()
    ElMessage.success(`已加载当前批次 ${result.tables?.length ?? 0} 张表`)
  } finally {
    scanLoading.value = false
  }
}

function scanRequest(cursor: string | null, cancel: boolean, metadataCacheMode: DatabaseMetadataCacheMode = 'AUTO'): DatabaseMetadataScanReq {
  return {
    ...databaseRequest(metadataCacheMode, [...(dbForm.tableNames ?? [])]),
    scanId: scanResult.value?.scanId,
    scanJobId: scanResult.value?.scanJobId ?? scanResult.value?.scanId,
    cursor: cursor ?? undefined,
    resumeCursor: cursor ?? undefined,
    pageSize: scanPageSize.value,
    cancel,
    cancelToken: cancel ? scanResult.value?.cancelToken : undefined,
    rateLimit: {
      maxTablesPerPage: scanPageSize.value
    }
  }
}

async function handleBrowseMetadata(metadataCacheMode: DatabaseMetadataCacheMode = 'AUTO') {
  if (!canBrowseMetadata.value) {
    return
  }
  metadataLoading.value = true
  try {
    const browser = await browseDatabaseMetadata(databaseRequest(metadataCacheMode))
    metadataBrowser.value = browser
    preview.value = browser.preview ?? null
    compareResult.value = browser.compare ?? null
    schemaPlan.value = null
    commentPlan.value = null
    importResult.value = null
    selectedCandidateKeys.value = buildBrowserCandidateKeySet(browser)
    candidateConfirmReasons.value = {}
    connectionStatus.value = 'success'
    connectionMessage.value = `已浏览 ${browser.summary?.tableCount ?? 0} 张表 metadata`
    ElMessage.success('元数据浏览已生成')
  } finally {
    metadataLoading.value = false
  }
}

async function handleRefreshMetadata() {
  const selectedTables = [...(dbForm.tableNames ?? [])]
  await handleBrowseMetadata('REFRESH')
  dbForm.tableNames = selectedTables
}

async function handleImportCandidates() {
  if (!projectStore.currentProjectId || selectedFieldCandidates.value.length === 0) {
    ElMessage.warning('请选择要导入的字段候选')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将导入 ${selectedFieldCandidates.value.length} 个字段候选到当前项目字段库。`,
      '确认导入数标',
      {
        type: 'warning',
        confirmButtonText: '确认导入',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  importLoading.value = true
  try {
    const idempotencyKey = createClientIdempotencyKey('reverse-import:database-import')
    importResult.value = await importDatabaseCandidates(
      projectStore.currentProjectId,
      selectedFieldCandidates.value,
      {
        databaseType: dbForm.databaseType,
        databaseName: dbForm.databaseName,
        schemaName: dbForm.schemaName,
        tableNames: [...(dbForm.tableNames ?? [])]
      },
      ignoredFieldCandidates.value,
      preview.value?.dryRunToken,
      idempotencyKey
    )
    ElMessage.success(`导入 ${importResult.value.importedCount ?? 0} 个字段，跳过 ${importResult.value.skippedCount ?? 0} 个字段`)
  } finally {
    importLoading.value = false
  }
}

async function handleCopyMetadataSummary() {
  await navigator.clipboard.writeText(metadataAiSummary.value)
  ElMessage.success('AI 摘要已复制')
}

async function handleCopySchemaPlanSummary() {
  await navigator.clipboard.writeText(schemaPlanAiSummary.value)
  ElMessage.success('迁移计划摘要已复制')
}

async function handleCopyCommentPlanSummary() {
  await navigator.clipboard.writeText(commentPlanCopyPayload.value)
  ElMessage.success('COMMENT 计划摘要已复制')
}

function handleFileChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) {
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    sqlText.value = String(reader.result ?? '')
    resetResults()
    ElMessage.success('SQL 已读取')
  }
  reader.readAsText(file, 'utf-8')
}

function handleDatabaseTypeChange(value: string) {
  dbForm.port = value === 'mysql' ? 3306 : 5432
  dbForm.schemaName = value === 'mysql' ? '' : 'public'
}

function tableKey(table: DatabaseTableInfo) {
  return `${table.schemaName ?? ''}.${table.tableName ?? ''}`
}

function tableLabel(table: DatabaseTableInfo) {
  return table.schemaName ? `${table.schemaName}.${table.tableName}` : table.tableName
}

function selectVisibleTables() {
  dbForm.tableNames = mergeSelectedTableNames(
    dbForm.tableNames,
    filteredDatabaseTables.value.filter((table) => table.tableName && !isScanTableDisabled(table.tableName))
  )
}

function clearSelectedTables() {
  dbForm.tableNames = []
}

function isScanTableDisabled(tableName?: string) {
  if (!tableName || !scanResult.value?.partialResult) {
    return false
  }
  const partial = scanResult.value.partialResult
  const successful = new Set(partial.successfulTableNames ?? [])
  const failed = new Set([...(partial.failedTableNames ?? []), ...(partial.skippedTableNames ?? [])])
  if (failed.has(tableName)) {
    return true
  }
  return successful.size > 0 && !successful.has(tableName)
}

function selectAllCandidates() {
  selectedCandidateKeys.value = new Set(
    (preview.value?.fieldCandidates ?? []).map((candidate) => buildCandidateKey(candidate))
  )
}

function clearSelectedCandidates() {
  selectedCandidateKeys.value = new Set()
}

function toggleCandidate(candidate: FieldCandidate, checked: boolean) {
  const next = new Set(selectedCandidateKeys.value)
  const key = buildCandidateKey(candidate)
  if (checked) {
    next.add(key)
  } else {
    next.delete(key)
  }
  selectedCandidateKeys.value = next
}

function isCandidateSelected(candidate: FieldCandidate) {
  return selectedCandidateKeys.value.has(buildCandidateKey(candidate))
}

function handleCandidateCheck(candidate: FieldCandidate, checked: boolean | string | number) {
  toggleCandidate(candidate, Boolean(checked))
}

function handleMetadataCandidateCheck(row: DatabaseMetadataBrowserRow, checked: boolean | string | number) {
  const key = row.candidateKey || `${row.tableName ?? ''}.${row.columnName ?? ''}`
  const next = new Set(selectedCandidateKeys.value)
  if (Boolean(checked)) {
    next.add(key)
  } else {
    next.delete(key)
  }
  selectedCandidateKeys.value = next
}

function candidateReasonValue(candidate: FieldCandidate) {
  const key = buildCandidateKey(candidate)
  return candidateConfirmReasons.value[key] ?? defaultCandidateConfirmReason(candidate)
}

function updateCandidateConfirmReason(candidate: FieldCandidate, reason: string) {
  const key = buildCandidateKey(candidate)
  candidateConfirmReasons.value = {
    ...candidateConfirmReasons.value,
    [key]: reason
  }
}

function decisionTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    EXISTING_MATCH: '已匹配',
    NEW_CANDIDATE: '新候选',
    IMPORTED: '已导入',
    SKIPPED_EXISTING: '已跳过',
    IGNORED: '已忽略'
  }
  return type ? labels[type] ?? type : '未知'
}

function decisionTagType(type?: string) {
  if (type === 'IMPORTED') {
    return 'success'
  }
  if (type === 'SKIPPED_EXISTING') {
    return 'warning'
  }
  if (type === 'IGNORED') {
    return 'info'
  }
  return 'primary'
}

function compareStatusLabel(status?: ReverseImportFieldStatus) {
  const option = compareStatusOptions.find((item) => item.value === status)
  return option?.label ?? status ?? '未知'
}

function compareStatusTagType(status?: ReverseImportFieldStatus) {
  if (status === 'MATCHED') {
    return 'success'
  }
  if (status === 'CHANGED') {
    return 'warning'
  }
  if (status === 'NEW') {
    return 'danger'
  }
  if (status === 'MISSING_COMMENT') {
    return 'info'
  }
  return 'info'
}

function changePropertyLabel(property?: string) {
  const labels: Record<string, string> = {
    dataType: '类型',
    nullable: '空值',
    defaultValue: '默认值',
    comment: '注释'
  }
  return property ? labels[property] ?? property : '属性'
}

function formatChangeValue(value?: string) {
  return value === undefined || value === null || value === '' ? '空' : value
}

function formatSchemaPlanValue(value?: string) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function schemaPlanRowClassName({ row }: { row: DatabaseSchemaChangeItem }) {
  const action = (row.action ?? '').toUpperCase()
  const riskLevel = (row.riskLevel ?? '').toUpperCase()
  if (action === 'DROP_CANDIDATE' || riskLevel === 'HIGH' || riskLevel === 'BLOCKED') {
    return 'schema-plan-row-danger'
  }
  return ''
}

function commentPlanRowClassName({ row }: { row: DatabaseCommentPatchPlanItem }) {
  const status = (row.status ?? '').toUpperCase()
  const riskLevel = (row.riskLevel ?? '').toUpperCase()
  if (status === 'UNSUPPORTED' || riskLevel === 'HIGH') {
    return 'schema-plan-row-danger'
  }
  return ''
}

function changeText(change: ReverseImportFieldChange) {
  return `${changePropertyLabel(change.property)}: ${formatChangeValue(change.currentValue)} -> ${formatChangeValue(change.standardValue)}`
}

function goToFieldLibrary() {
  router.push({
    path: '/fields',
    query: {
      projectId: projectStore.currentProjectId ?? undefined,
      ...fieldLibraryQueryForImportResult(importResult.value?.importedFields ?? [])
    }
  })
}

function goToFieldLibraryBySourceBatch() {
  router.push({
    path: '/fields',
    query: {
      projectId: projectStore.currentProjectId ?? undefined,
      sourceBatchId: urlSourceBatchId.value ?? undefined
    }
  })
}

function applyReverseImportUrlState() {
  if (hasRouteQuery('table')) {
    const table = readStringQuery(route.query, 'table')
    if (tableSearch.value !== table) {
      tableSearch.value = table
    }
  }
  if (hasRouteQuery('status')) {
    const nextStatus = readEnumQuery(route.query, 'status', compareStatusOptions.map((item) => item.value))
    if (!nextStatus) {
      ElMessage.warning('链接中的反向导入状态筛选无效，已恢复为全部')
      compareStatusFilter.value = 'ALL'
      void syncReverseImportUrlState({ status: null })
    } else {
      compareStatusFilter.value = nextStatus
    }
  }
  if (route.query.batchId && urlSourceBatchId.value) {
    void syncReverseImportUrlState({ sourceBatchId: urlSourceBatchId.value, batchId: null })
  }
}

function hasRouteQuery(key: string) {
  return Object.prototype.hasOwnProperty.call(route.query, key)
}

async function syncReverseImportUrlState(patch: Record<string, string | number | null> = {}) {
  await replaceRouteQuery(router, route, {
    projectId: projectStore.currentProjectId,
    table: tableSearch.value.trim() || null,
    status: compareStatusFilter.value !== 'ALL' ? compareStatusFilter.value : null,
    sourceBatchId: urlSourceBatchId.value,
    batchId: null,
    ...patch
  })
}

async function handleCopyReverseImportLink() {
  try {
    await syncReverseImportUrlState()
    await copyRouteUrl(route, navigator.clipboard)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.error('复制失败，请手动复制浏览器地址')
  }
}

function applySavedReverseImportMemory() {
  const projectId = projectStore.currentProjectId
  const storage = browserStorage()
  if (!projectId || !storage) {
    return
  }
  const memory = loadReverseImportMemory(storage, projectId)
  if (!memory) {
    return
  }

  restoringMemory.value = true
  if (memory.activeMode) {
    activeMode.value = memory.activeMode
  }
  applyDatabaseMemory(memory.database)
  if (memory.tableSearch !== undefined) {
    tableSearch.value = memory.tableSearch
  }
  if (isCompareStatusFilter(memory.compareStatusFilter)) {
    compareStatusFilter.value = memory.compareStatusFilter
  }
  resetConnectionStatus()
  void nextTick(() => {
    restoringMemory.value = false
  })
}

function applyDatabaseMemory(database: ReverseImportMemoryState['database']) {
  if (!database) {
    return
  }
  if (isDatabaseType(database.databaseType)) {
    dbForm.databaseType = database.databaseType
  }
  if (database.host !== undefined) {
    dbForm.host = database.host
  }
  if (database.port !== undefined) {
    dbForm.port = database.port
  }
  if (database.databaseName !== undefined) {
    dbForm.databaseName = database.databaseName
  }
  if (database.schemaName !== undefined) {
    dbForm.schemaName = database.schemaName
  }
  if (database.username !== undefined) {
    dbForm.username = database.username
  }
  if (database.tableNames !== undefined) {
    dbForm.tableNames = [...database.tableNames]
  }
  dbForm.password = ''
}

function persistReverseImportMemory() {
  const projectId = projectStore.currentProjectId
  const storage = browserStorage()
  if (!projectId || !storage || restoringMemory.value) {
    return
  }
  try {
    saveReverseImportMemory(storage, projectId, {
      activeMode: activeMode.value,
      tableSearch: tableSearch.value,
      compareStatusFilter: compareStatusFilter.value,
      database: {
        databaseType: dbForm.databaseType,
        host: dbForm.host,
        port: dbForm.port,
        databaseName: dbForm.databaseName,
        schemaName: dbForm.schemaName,
        username: dbForm.username,
        tableNames: [...(dbForm.tableNames ?? [])]
      }
    })
  } catch {
    // 本地记忆是体验增强，写入失败不能影响反向导入主流程。
  }
}

function isCompareStatusFilter(value: unknown): value is CompareStatusFilter {
  return compareStatusOptions.some((option) => option.value === value)
}

function isDatabaseType(value: unknown): value is NonNullable<DatabaseConnectionReq['databaseType']> {
  return value === 'postgresql' || value === 'mysql'
}

function browserStorage() {
  try {
    return typeof window === 'undefined' ? null : window.localStorage
  } catch {
    return null
  }
}
</script>

<style scoped>
.reverse-page {
  min-height: calc(100vh - 140px);
  padding: 20px;
  background: #fff;
  border-radius: 4px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0;
  font-weight: 600;
  color: #1f2937;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.header-actions,
.input-toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.inline-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mode-tabs {
  border-top: 1px solid #ebeef5;
}

.input-section,
.result-section {
  padding-top: 16px;
}

.database-flow {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.db-steps {
  padding: 12px 4px 4px;
}

.db-workbench {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(320px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.db-panel {
  min-width: 0;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fcfcfd;
}

.result-section {
  border-top: 1px solid #ebeef5;
  margin-top: 16px;
}

.source-link-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  background: #f4faff;
  color: #1f2937;
}

.source-link-panel span {
  margin-left: 8px;
  color: #606266;
  font-size: 13px;
}

.dialect-panel {
  padding: 10px 12px;
  margin-top: 12px;
  margin-bottom: 12px;
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

.security-diagnostic {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  border-left: 3px solid #dcdfe6;
  background: #fff;
}

.security-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.security-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.security-summary,
.security-meta,
.security-line {
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
}

.security-meta {
  display: flex;
  gap: 8px 14px;
  flex-wrap: wrap;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.security-line.muted {
  color: #6b7280;
}

.security-sql {
  max-height: 160px;
  margin: 0;
  padding: 8px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #f8fafc;
  color: #374151;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.input-toolbar {
  margin-bottom: 12px;
}

.preset-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.preset-select {
  width: 100%;
}

.preset-option {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.preset-option-title {
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preset-option-summary,
.preset-summary,
.preset-dialog-hint {
  color: #6b7280;
  font-size: 12px;
}

.preset-summary {
  margin: -2px 0 10px;
}

.preset-preview {
  min-height: 32px;
  padding: 7px 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  color: #374151;
  line-height: 1.5;
}

.preset-dialog-hint {
  margin: 4px 0 0;
}

.db-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 2px 18px;
}

.form-control {
  width: 100%;
}

.number-input {
  display: block;
}

.table-tools {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 120px auto auto;
  gap: 10px;
  align-items: center;
}

.table-counts {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.scan-panel {
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.scan-row {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #374151;
  font-size: 13px;
}

.scan-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.scan-actions {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.metadata-cache-row {
  align-items: flex-start;
  justify-content: flex-start;
  flex-wrap: wrap;
  line-height: 1.5;
}

.table-check-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 8px;
  max-height: 318px;
  margin-top: 12px;
  overflow: auto;
}

.saved-table-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.table-check-item {
  height: auto;
  min-height: 44px;
  margin-right: 0;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.table-check-item :deep(.el-checkbox__label) {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.table-title {
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-comment {
  overflow: hidden;
  max-width: 100%;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.db-hint {
  margin-top: 0;
}

.metadata-browser {
  padding: 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
}

.schema-plan-preview {
  padding: 14px;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  background: #fbfdff;
}

.comment-plan-preview {
  border-color: #d1fae5;
  background: #fbfffd;
}

.schema-plan-meta {
  display: flex;
  gap: 8px 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.metadata-summary {
  margin-bottom: 12px;
}

.metadata-cache-panel {
  display: grid;
  gap: 6px;
  margin: 0 0 12px;
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
}

.metadata-cache-summary {
  font-weight: 600;
}

.metadata-cache-meta {
  display: flex;
  gap: 8px 14px;
  flex-wrap: wrap;
  color: #64748b;
}

.metadata-tools {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.metadata-ai-summary {
  max-height: 180px;
  margin: 0 0 12px;
  padding: 10px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.metadata-field-name {
  color: #1f2937;
  font-weight: 600;
}

.schema-plan-summary {
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.schema-plan-notes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.schema-plan-list {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.schema-plan-list.danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.schema-plan-list.warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.schema-plan-list ul {
  display: grid;
  gap: 4px;
  margin: 0;
  padding-left: 18px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.schema-plan-groups {
  margin-bottom: 12px;
}

.schema-plan-sql {
  margin-top: 12px;
}

.schema-plan-sql-block {
  max-height: 260px;
  margin: 0;
  padding: 10px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #0f172a;
  color: #e5e7eb;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.schema-plan-rollback {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

:deep(.schema-plan-row-danger) {
  --el-table-tr-bg-color: #fff7f7;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.summary-label {
  color: #6b7280;
  font-size: 13px;
}

.summary-value {
  margin-top: 6px;
  color: #111827;
  font-size: 26px;
  font-weight: 700;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.compact-header {
  margin-bottom: 12px;
}

.result-grid {
  display: flex;
  gap: 24px;
  color: #4b5563;
}

.result-number {
  margin-right: 6px;
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.import-lists {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
}

.list-title {
  margin-bottom: 8px;
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
}

.field-chip {
  margin: 0 6px 6px 0;
}

.empty-inline {
  color: #9ca3af;
  font-size: 13px;
}

.decision-summary {
  margin-top: 16px;
}

.decision-hint {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.4;
}

.candidate-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #4b5563;
  font-size: 13px;
}

.candidate-groups {
  border-top: 1px solid #ebeef5;
}

.compare-result :deep(.el-radio-group) {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.compare-groups {
  border-top: 1px solid #ebeef5;
}

.change-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.muted-text {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

.group-title {
  margin-right: 8px;
  color: #1f2937;
  font-weight: 600;
}

.small-empty {
  padding: 28px 0;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .db-workbench {
    grid-template-columns: 1fr;
  }

  .db-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .table-tools,
  .metadata-tools,
  .preset-bar,
  .import-lists {
    grid-template-columns: 1fr;
  }

  .scan-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .candidate-toolbar,
  .result-actions,
  .compare-result .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .compare-result :deep(.el-radio-group) {
    justify-content: flex-start;
  }
}
</style>
