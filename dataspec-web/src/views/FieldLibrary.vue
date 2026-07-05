<template>
  <div class="field-page">
    <div class="page-header">
      <div>
        <h2>标准字段库</h2>
        <p class="page-subtitle">
          {{ projectStore.currentProjectName || '未选择项目' }}
        </p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadFields">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" :disabled="!hasProject" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建字段
        </el-button>
      </div>
    </div>

    <el-empty v-if="!hasProject" description="请先创建并选择项目">
      <el-button type="primary" @click="$router.push('/projects')">去项目列表</el-button>
    </el-empty>

    <template v-else>
      <div class="field-toolbar">
        <el-input
          v-model="fieldKeyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索字段名、显示名、别名、分类、注释或替代说明"
        />
        <el-select v-model="fieldStatusFilter" clearable class="status-filter" placeholder="全部状态">
          <el-option
            v-for="option in lifecycleStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <div class="toolbar-actions">
          <span class="toolbar-count">匹配 {{ filteredFields.length }} / {{ fields.length }}</span>
          <el-button :disabled="selectedFields.length === 0" @click="openBatchDialog">
            批量归组
          </el-button>
          <el-button plain @click="handleCopyFieldLink">
            <el-icon><Link /></el-icon>
            复制链接
          </el-button>
          <el-button type="primary" plain :disabled="selectedFields.length === 0" @click="openBulkDialog">
            批量维护
          </el-button>
        </div>
      </div>

      <div v-if="hasFieldSearchConditions" class="search-insight">
        <div class="search-insight-main">
          <span class="search-insight-label">字段标准检索</span>
          <span>
            命中 {{ fieldSearchSummary?.matchedCount ?? filteredFields.length }}，
            返回 {{ fieldSearchSummary?.returnedCount ?? fields.length }}
          </span>
        </div>
        <div v-if="fieldSearchHints.length" class="search-insight-line">
          {{ fieldSearchHints.join('；') }}
        </div>
        <div v-if="fieldSearchNextActions.length" class="search-insight-line">
          下一步建议：{{ fieldSearchNextActions.join('；') }}
        </div>
      </div>

      <div class="field-content">
        <aside class="group-panel">
          <button
            v-for="group in groupOptions"
            :key="group.optionKey"
            class="group-option"
            :class="{ active: activeGroupKey === group.optionKey }"
            type="button"
            @click="selectGroup(group.optionKey)"
          >
            <span class="group-name">{{ group.displayName }}</span>
            <span class="group-meta">{{ group.fieldCount }}</span>
          </button>
        </aside>

        <el-table
          v-loading="loading"
          :data="pagedFields"
          row-key="id"
          stripe
          class="field-table"
          empty-text="暂无标准字段"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="44" />
          <el-table-column label="字段名" min-width="190" fixed="left">
            <template #default="{ row }">
              <div class="field-name-cell">{{ row.name }}</div>
              <div v-if="fieldSearchReasons(row).length" class="search-reason">
                命中原因：{{ fieldSearchReasons(row).join('；') }}
              </div>
              <div v-if="fieldSearchRecommendedUse(row)" class="search-reason">
                {{ fieldSearchRecommendedUse(row) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="displayName" label="显示名" min-width="120" />
          <el-table-column label="类型" min-width="150">
            <template #default="{ row }">
              <span>{{ formatDataType(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="值格式" min-width="220">
            <template #default="{ row }">
              <div v-if="fieldFormatSummary(row)" class="format-summary">
                {{ fieldFormatSummary(row) }}
              </div>
              <span v-else class="muted-text">-</span>
            </template>
          </el-table-column>
          <el-table-column label="分组" min-width="180">
            <template #default="{ row }">
              <div>{{ fieldGroupLabel(row) }}</div>
              <div class="muted-text">{{ row.tags || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="空值" width="86">
            <template #default="{ row }">
              <el-tag :type="row.nullable === false ? 'warning' : 'info'" size="small">
                {{ row.nullable === false ? '非空' : '可空' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="aliases" label="别名" min-width="180" show-overflow-tooltip />
          <el-table-column label="敏感" width="86">
            <template #default="{ row }">
              <el-tag v-if="row.sensitive" type="danger" size="small">是</el-tag>
              <el-tag v-else type="info" size="small">否</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="160">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusText(row.status) }}
              </el-tag>
              <div v-if="replacementSummary(row)" class="muted-text lifecycle-hint">
                {{ replacementSummary(row) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="350" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openImpactDialog(row)">影响</el-button>
              <el-button text type="primary" @click="openSourceDialog(row)">来源</el-button>
              <el-button text type="primary" @click="openChangeLogDialog(row)">变更</el-button>
              <el-button text type="primary" @click="openMergeDialog(row)">合并</el-button>
              <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredFields.length"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <el-dialog v-model="dialogVisible" :title="editingField ? '编辑字段' : '新建字段'" width="860px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字段名" prop="name">
              <el-input v-model="form.name" placeholder="mobile_no" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示名">
              <el-input v-model="form.displayName" placeholder="手机号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="数据类型" prop="dataType">
              <el-select v-model="form.dataType" filterable allow-create class="full-width">
                <el-option v-for="type in dataTypeOptions" :key="type" :label="type" :value="type" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="长度">
              <el-input-number v-model="form.length" :min="1" :controls="false" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认值">
              <el-input v-model="form.defaultValue" placeholder="可留空" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="精度">
              <el-input-number
                v-model="form.precisionVal"
                :min="0"
                :controls="false"
                class="full-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="小数位">
              <el-input-number
                v-model="form.scaleVal"
                :min="0"
                :controls="false"
                class="full-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="允许空值">
              <el-switch v-model="form.nullable" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-input v-model="form.category" placeholder="user / order" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" class="full-width">
                <el-option
                  v-for="option in lifecycleStatusOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="敏感字段">
              <el-switch v-model="form.sensitive" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="数据域">
              <el-select v-model="form.domainId" clearable filterable class="full-width">
                <el-option
                  v-for="domain in domains"
                  :key="domain.id"
                  :label="domainLabel(domain)"
                  :value="domain.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="代码集 ID">
              <el-input-number v-model="form.codeSetId" :min="1" :controls="false" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="示例值">
              <el-input v-model="form.exampleValue" placeholder="13800138000" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="替代字段">
              <el-select
                v-model="form.replacementFieldId"
                clearable
                filterable
                class="full-width"
                placeholder="选择同项目字段"
              >
                <el-option
                  v-for="field in replacementFieldOptions"
                  :key="field.id"
                  :label="replacementFieldLabel(field)"
                  :value="field.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="替代说明">
              <el-input v-model="form.replacementReason" clearable placeholder="历史兼容字段，改用 mobile_no" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="别名">
          <el-input v-model="form.aliases" placeholder="phone,mobile,tel,user_phone" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="用户,联系方式" />
        </el-form-item>
        <el-form-item label="字段注释">
          <el-input v-model="form.comment" type="textarea" :rows="3" placeholder="请输入字段注释" />
        </el-form-item>

        <div class="format-section-title">值格式与样例</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="格式类型">
              <el-select v-model="form.formatType" clearable filterable allow-create class="full-width">
                <el-option
                  v-for="option in formatTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-input v-model="form.formatUnit" placeholder="cent / yuan / ms" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="值精度">
              <el-input v-model="form.formatPrecision" placeholder="scale=2 / millisecond" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="时区">
              <el-input v-model="form.formatTimezone" placeholder="UTC / Asia/Shanghai" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="空值策略">
              <el-select v-model="form.formatNullPolicy" clearable filterable allow-create class="full-width">
                <el-option
                  v-for="option in formatNullPolicyOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="格式模式">
              <el-input v-model="form.formatPattern" placeholder="^1\\d{10}$" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="正例">
              <el-input
                v-model="formatExamplesForm.validExamplesText"
                type="textarea"
                :rows="3"
                placeholder="每行一个合法示例；空字符串可写 &quot;&quot;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="反例">
              <el-input
                v-model="formatExamplesForm.invalidExamplesText"
                type="textarea"
                :rows="3"
                placeholder="每行一个非法示例；空字符串可写 &quot;&quot;"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="格式备注">
          <el-input
            v-model="form.formatNotes"
            type="textarea"
            :rows="2"
            placeholder="例如：金额以分为单位存储，展示时除以 100"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量归组" width="560px">
      <el-form label-width="96px">
        <el-form-item label="已选字段">
          <span>{{ selectedFields.length }} 个</span>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="batchForm.applyDomain">数据域</el-checkbox>
          <el-select
            v-model="batchForm.domainId"
            clearable
            filterable
            class="batch-input"
            placeholder="选择数据域；留空可清空"
          >
            <el-option
              v-for="domain in domains"
              :key="domain.id"
              :label="domainLabel(domain)"
              :value="domain.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="batchForm.applyCategory">分类</el-checkbox>
          <el-input v-model="batchForm.category" class="batch-input" placeholder="contact / order；留空可清空" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="batchForm.applyTags">标签</el-checkbox>
          <el-input v-model="batchForm.tags" class="batch-input" placeholder="pii,customer；留空可清空" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">
          保存归组
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bulkDialogVisible" title="批量维护字段" width="880px">
      <div class="bulk-dialog">
        <el-form label-width="96px">
          <el-form-item label="已选字段">
            <span>{{ selectedFields.length }} 个</span>
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item>
                <el-checkbox v-model="bulkForm.applyStatus">状态</el-checkbox>
                <el-select v-model="bulkForm.status" class="bulk-input">
                  <el-option
                    v-for="option in lifecycleStatusOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item>
                <el-checkbox v-model="bulkForm.applySensitive">敏感字段</el-checkbox>
                <el-switch v-model="bulkForm.sensitive" class="bulk-switch" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item>
                <el-checkbox v-model="bulkForm.applyCategory">分类</el-checkbox>
                <el-input v-model="bulkForm.category" class="bulk-input" placeholder="contact；留空可清空" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item>
                <el-checkbox v-model="bulkForm.applyCodeSetId">代码集 ID</el-checkbox>
                <el-input-number
                  v-model="bulkForm.codeSetId"
                  :min="1"
                  :controls="false"
                  class="bulk-input"
                  placeholder="留空可清空"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item>
            <el-checkbox v-model="bulkForm.applyTags">标签</el-checkbox>
            <el-input v-model="bulkForm.tags" class="bulk-wide-input" placeholder="pii,customer；留空可清空" />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="bulkForm.applyAliases">别名</el-checkbox>
            <el-input v-model="bulkForm.aliases" class="bulk-wide-input" placeholder="phone,mobile；留空可清空" />
          </el-form-item>
        </el-form>

        <div v-if="bulkPreview" class="bulk-preview">
          <div class="bulk-preview-summary">
            将变更 {{ bulkPreview.changedCount ?? 0 }} 个字段，跳过 {{ bulkPreview.unchangedCount ?? 0 }} 个无变化字段。
          </div>
          <el-table
            :data="bulkChangedItems"
            max-height="260"
            stripe
            empty-text="所选字段没有变化"
          >
            <el-table-column prop="fieldName" label="字段" width="180" show-overflow-tooltip />
            <el-table-column label="变更内容" min-width="420">
              <template #default="{ row }">
                <div class="change-chip-list">
                  <span
                    v-for="change in row.changes ?? []"
                    :key="`${row.fieldId}-${change.attribute}`"
                    class="change-chip"
                  >
                    {{ bulkAttributeText(change.attribute) }}:
                    {{ formatChangeValue(change.beforeValue) }} → {{ formatChangeValue(change.afterValue) }}
                  </span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="bulkDialogVisible = false">取消</el-button>
        <el-button :loading="bulkPreviewLoading" @click="handleBulkPreview">生成预览</el-button>
        <el-button
          type="primary"
          :disabled="!bulkPreview || (bulkPreview.changedCount ?? 0) === 0"
          :loading="bulkSubmitting"
          @click="handleBulkSubmit"
        >
          提交维护
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sourceDialogVisible" :title="sourceDialogTitle" width="820px">
      <el-table
        v-loading="sourceLoading"
        :data="fieldSources"
        stripe
        empty-text="暂无来源记录"
      >
        <el-table-column label="导入时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.batch?.createdAt ?? row.source?.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="来源库" min-width="180">
          <template #default="{ row }">
            <div>{{ row.batch?.databaseType || '-' }}</div>
            <div class="muted-text">{{ sourceDatabaseLabel(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="来源字段" min-width="180">
          <template #default="{ row }">
            <div>{{ sourceColumnLabel(row) }}</div>
            <div class="muted-text">{{ row.source?.dataType || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="批次统计" min-width="120">
          <template #default="{ row }">
            <span>新增 {{ row.batch?.importedCount ?? 0 }} / 跳过 {{ row.batch?.skippedCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="source.comment" label="原注释" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="impactDialogVisible" :title="impactDialogTitle" width="860px">
      <el-skeleton v-if="impactLoading" :rows="5" animated />
      <template v-else-if="impactReport">
        <el-alert
          v-if="impactReport.editWarnings?.length"
          type="warning"
          show-icon
          :closable="false"
          class="impact-alert"
          :title="`关注关键属性：${warningSummaryText(impactReport.editWarnings)}`"
        />
        <div class="impact-summary">
          <div class="impact-metric">
            <span>总影响</span>
            <strong>{{ impactReport.summary?.totalImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>模板</span>
            <strong>{{ impactReport.summary?.templateImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>导入来源</span>
            <strong>{{ impactReport.summary?.importSourceImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>SQL</span>
            <strong>{{ impactReport.summary?.sqlCheckImpactCount ?? 0 }}</strong>
          </div>
          <div class="impact-metric">
            <span>快照</span>
            <strong>{{ impactReport.summary?.snapshotImpactCount ?? 0 }}</strong>
          </div>
        </div>
        <el-table :data="impactReport.impacts ?? []" stripe empty-text="暂无已知影响">
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="impactSeverityTagType(row.severity)">
                {{ impactTypeLabel(row.impactType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.sourceName || '-' }}</template>
          </el-table-column>
          <el-table-column label="数量" width="82">
            <template #default="{ row }">{{ row.count ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="说明" min-width="320" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.description || '-' }}</span>
              <el-tag v-if="row.possibleReference" class="possible-tag" size="small" type="info">疑似</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <el-dialog v-model="changeLogDialogVisible" :title="changeLogDialogTitle" width="920px">
      <el-table
        v-loading="changeLogLoading"
        :data="changeLogs"
        stripe
        empty-text="暂无字段变更"
      >
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDate(row.changedAt) }}</template>
        </el-table-column>
        <el-table-column label="动作" width="100">
          <template #default="{ row }">
            <el-tag :type="changeLogActionTagType(row.action)" size="small">
              {{ changeLogActionText(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作者" width="120" show-overflow-tooltip />
        <el-table-column label="关键属性变化" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">{{ changeLogSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              :disabled="!canUndoLog(row)"
              :loading="undoSubmitting === row.id"
              @click="handleUndoChange(row)"
            >
              回退
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row compact-pagination">
        <el-pagination
          v-model:current-page="changeLogCurrent"
          :page-size="changeLogSize"
          :total="changeLogTotal"
          layout="total, prev, pager, next"
          @current-change="handleChangeLogPageChange"
        />
      </div>
    </el-dialog>

    <StandardFieldMergeDialog
      v-model="mergeDialogVisible"
      :project-id="projectStore.currentProjectId"
      :options="fieldMergeOptions"
      :initial-target-id="mergeInitialTargetId"
      @applied="handleMergeApplied"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Link, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { listChangeLogs } from '@/api/changeLog'
import { listDomains } from '@/api/domain'
import StandardFieldMergeDialog from '@/components/StandardFieldMergeDialog.vue'
import {
  batchUpdateFieldGrouping,
  bulkUpdateFields,
  createField,
  deleteField,
  getField,
  getFieldGroupSummary,
  getFieldImpactReport,
  listFields,
  listFieldSources,
  previewFieldBulkUpdate,
  searchFields,
  undoFieldChange,
  updateField
} from '@/api/field'
import { previewFieldChange } from '@/api/standardChange'
import { useProjectStore } from '@/stores/project'
import {
  impactSeverityTagType,
  impactTypeLabel,
  warningSummaryText
} from '@/utils/fieldImpactDisplay'
import {
  shouldShowStandardChangeConfirm,
  standardChangeConfirmMessage,
  standardChangeRiskText
} from '@/utils/standardChangeDisplay'
import {
  copyRouteUrl,
  readPositiveIntQuery,
  replaceRouteQuery
} from '@/utils/urlState'
import type {
  Domain,
  Field,
  FieldBulkUpdatePreview,
  FieldBulkUpdateReq,
  FieldGroupItem,
  FieldGroupingBatchUpdateReq,
  FieldGroupSummary,
  FieldImpactReport,
  FieldReq,
  FieldSearchItem,
  FieldSearchReq,
  FieldSearchSummary,
  FieldSourceDetail,
  StandardChangePreview,
  StandardChangeLog,
  StandardFieldMergeOption
} from '@/types'

const projectStore = useProjectStore()
const route = useRoute()
const router = useRouter()
const fields = ref<Field[]>([])
const groupSummary = ref<FieldGroupSummary | null>(null)
const domains = ref<Domain[]>([])
const fieldKeyword = ref(routeKeyword(route.query.keyword))
const fieldStatusFilter = ref('')
const fieldSearchItems = ref<FieldSearchItem[]>([])
const fieldSearchSummary = ref<FieldSearchSummary | null>(null)
const fieldSearchNextActions = ref<string[]>([])
const sourceBatchIdFilter = computed(() => readPositiveIntQuery(route.query, 'sourceBatchId'))
const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)
const bulkPreviewLoading = ref(false)
const bulkSubmitting = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const bulkDialogVisible = ref(false)
const sourceDialogVisible = ref(false)
const impactDialogVisible = ref(false)
const changeLogDialogVisible = ref(false)
const mergeDialogVisible = ref(false)
const editingField = ref<Field | null>(null)
const sourceField = ref<Field | null>(null)
const impactField = ref<Field | null>(null)
const changeLogField = ref<Field | null>(null)
const fieldSources = ref<FieldSourceDetail[]>([])
const sourceLoading = ref(false)
const impactLoading = ref(false)
const changeLogLoading = ref(false)
const impactReport = ref<FieldImpactReport | null>(null)
const bulkPreview = ref<FieldBulkUpdatePreview | null>(null)
const changeLogs = ref<StandardChangeLog[]>([])
const undoSubmitting = ref<number | null>(null)
const formRef = ref<FormInstance>()
const openedRouteFieldId = ref<number | null>(null)
const selectedFields = ref<Field[]>([])
const activeGroupKey = ref('all')
const mergeInitialTargetId = ref<number | null>(null)
let loadSequence = 0

const pagination = reactive({
  current: 1,
  size: 20
})

const batchForm = reactive({
  applyDomain: true,
  applyCategory: true,
  applyTags: true,
  domainId: undefined as number | undefined,
  category: '',
  tags: ''
})

const bulkForm = reactive({
  applyStatus: false,
  status: 'enabled',
  applyCategory: false,
  category: '',
  applyTags: false,
  tags: '',
  applySensitive: false,
  sensitive: false,
  applyCodeSetId: false,
  codeSetId: undefined as number | undefined,
  applyAliases: false,
  aliases: ''
})

const formatExamplesForm = reactive({
  validExamplesText: '',
  invalidExamplesText: ''
})

const changeLogCurrent = ref(1)
const changeLogSize = 10
const changeLogTotal = ref(0)

const dataTypeOptions = [
  'bigint',
  'integer',
  'varchar',
  'text',
  'boolean',
  'timestamp',
  'timestamptz',
  'numeric',
  'decimal',
  'jsonb'
]

const lifecycleStatusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '启用', value: 'enabled' },
  { label: '废弃', value: 'deprecated' },
  { label: '停用', value: 'disabled' }
]

const formatTypeOptions = [
  { label: '手机号', value: 'mobile' },
  { label: '邮箱', value: 'email' },
  { label: '金额', value: 'money' },
  { label: '时间戳', value: 'timestamp' },
  { label: '日期', value: 'date' },
  { label: 'JSON', value: 'json' },
  { label: '状态码', value: 'status' },
  { label: '编码', value: 'code' },
  { label: '文本', value: 'text' }
]

const formatNullPolicyOptions = [
  { label: '不允许空字符串', value: 'not_blank' },
  { label: '空字符串视为空', value: 'empty_string_as_null' },
  { label: '不适用', value: 'not_applicable' },
  { label: '保留原值', value: 'preserve' }
]

const form = reactive<FieldReq>({
  projectId: 0,
  name: '',
  displayName: '',
  dataType: 'varchar',
  length: undefined,
  precisionVal: undefined,
  scaleVal: undefined,
  nullable: true,
  defaultValue: '',
  comment: '',
  domainId: undefined,
  tags: '',
  aliases: '',
  category: '',
  codeSetId: undefined,
  sensitive: false,
  status: 'enabled',
  replacementFieldId: undefined,
  replacementReason: '',
  exampleValue: '',
  formatType: '',
  formatPattern: '',
  formatUnit: '',
  formatPrecision: '',
  formatTimezone: '',
  formatNullPolicy: '',
  validExamplesJson: '',
  invalidExamplesJson: '',
  formatNotes: ''
})

const rules: FormRules<FieldReq> = {
  name: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  dataType: [{ required: true, message: '请选择数据类型', trigger: 'change' }]
}

const hasProject = computed(() => Boolean(projectStore.currentProjectId))
const sourceDialogTitle = computed(() =>
  sourceField.value?.name ? `字段来源：${sourceField.value.name}` : '字段来源'
)
const impactDialogTitle = computed(() =>
  impactField.value?.name ? `字段影响：${impactField.value.name}` : '字段影响'
)
const changeLogDialogTitle = computed(() =>
  changeLogField.value?.name ? `字段变更：${changeLogField.value.name}` : '字段变更'
)
const groupOptions = computed(() => [
  {
    optionKey: 'all',
    displayName: '全部字段',
    fieldCount: groupSummary.value?.totalFieldCount ?? fields.value.length
  },
  ...(groupSummary.value?.groups ?? []).map((group) => ({
    optionKey: groupOptionKey(group),
    displayName: groupDisplayName(group),
    fieldCount: group.fieldCount ?? 0
  }))
])
const hasFieldSearchConditions = computed(() =>
  Boolean(fieldKeyword.value.trim())
    || Boolean(fieldStatusFilter.value)
    || isSearchableGroupKey(activeGroupKey.value)
    || Boolean(sourceBatchIdFilter.value)
)
const fieldSearchHints = computed(() => fieldSearchSummary.value?.hints?.filter(Boolean) ?? [])
const fieldSearchItemByFieldId = computed(() => {
  const items = new Map<number, FieldSearchItem>()
  for (const item of fieldSearchItems.value) {
    const id = item.field?.id
    if (typeof id === 'number') {
      items.set(id, item)
    }
  }
  return items
})
const filteredFields = computed(() => {
  const keyword = fieldKeyword.value.trim().toLowerCase()
  if (hasFieldSearchConditions.value) {
    return fields.value.filter((field) => matchesActiveGroup(field) && matchesFieldStatus(field))
  }
  return fields.value.filter((field) =>
    matchesActiveGroup(field) && matchesFieldStatus(field) && (!keyword || [
        field.name,
        field.displayName,
        field.aliases,
        field.category,
        field.tags,
        field.comment,
        field.replacementReason,
        field.formatType,
        field.formatPattern,
        field.formatUnit,
        field.formatPrecision,
        field.formatTimezone,
        field.formatNullPolicy,
        field.formatNotes,
        fieldFormatSummary(field),
        field.dataType,
        fieldGroupLabel(field)
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword)))
  )
})
const pagedFields = computed(() => {
  const start = (pagination.current - 1) * pagination.size
  return filteredFields.value.slice(start, start + pagination.size)
})
const bulkChangedItems = computed(() => bulkPreview.value?.items?.filter((item) => item.changed) ?? [])
const replacementFieldOptions = computed(() =>
  fields.value.filter((field): field is Field & { id: number } =>
    typeof field.id === 'number' && field.id !== editingField.value?.id)
)
const fieldMergeOptions = computed<StandardFieldMergeOption[]>(() =>
  fields.value
    .filter((field): field is Field & { id: number } => typeof field.id === 'number')
    .map((field) => ({
      fieldId: field.id,
      name: field.name,
      displayName: field.displayName,
      dataType: field.dataType,
      status: field.status
    }))
)

onMounted(() => {
  if (projectStore.projects.length === 0) {
    void projectStore.loadProjects()
  }
})

watch(
  () => projectStore.currentProjectId,
  () => {
    pagination.current = 1
    openedRouteFieldId.value = null
    activeGroupKey.value = 'all'
    selectedFields.value = []
    void loadFields()
  },
  { immediate: true }
)

watch(
  () => route.query.keyword,
  (keyword) => {
    const nextKeyword = routeKeyword(keyword)
    if (fieldKeyword.value !== nextKeyword) {
      fieldKeyword.value = nextKeyword
    }
  }
)

watch([fieldKeyword, fieldStatusFilter, activeGroupKey], () => {
  pagination.current = 1
  selectedFields.value = []
  void syncFieldUrlState({ fieldId: null })
  void loadFields()
})

watch(dialogVisible, (visible) => {
  if (!visible && route.query.fieldId) {
    openedRouteFieldId.value = null
    void syncFieldUrlState({ fieldId: null })
  }
})

watch(bulkForm, () => {
  bulkPreview.value = null
}, { deep: true })

watch(
  () => route.query.fieldId,
  () => {
    openedRouteFieldId.value = null
    void openFieldFromRoute()
  }
)

watch(
  () => route.query.sourceBatchId,
  () => {
    pagination.current = 1
    selectedFields.value = []
    void loadFields()
  }
)

async function loadFields() {
  const sequence = ++loadSequence
  const projectId = projectStore.currentProjectId
  if (!projectId) {
    fields.value = []
    domains.value = []
    groupSummary.value = null
    fieldSearchItems.value = []
    fieldSearchSummary.value = null
    fieldSearchNextActions.value = []
    loading.value = false
    return
  }
  loading.value = true
  try {
    const searchRequest = buildFieldSearchRequest(projectId)
    const fieldRequest = searchRequest ? searchFields(searchRequest) : listFields(projectId)
    const [fieldResult, summary, domainList] = await Promise.all([
      fieldRequest,
      getFieldGroupSummary(projectId),
      listDomains(projectId)
    ])
    if (sequence !== loadSequence) {
      return
    }
    if (searchRequest) {
      const searchResult = fieldResult as Awaited<ReturnType<typeof searchFields>>
      fieldSearchItems.value = searchResult.items ?? []
      fieldSearchSummary.value = searchResult.summary ?? null
      fieldSearchNextActions.value = searchResult.nextActions ?? []
      fields.value = fieldSearchItems.value
        .map((item) => item.field)
        .filter((field): field is Field => Boolean(field))
    } else {
      fieldSearchItems.value = []
      fieldSearchSummary.value = null
      fieldSearchNextActions.value = []
      fields.value = fieldResult as Awaited<ReturnType<typeof listFields>> ?? []
    }
    groupSummary.value = summary
    domains.value = domainList ?? []
    selectedFields.value = []
    ensureActiveGroupExists()
    await openFieldFromRoute()
  } finally {
    if (sequence === loadSequence) {
      loading.value = false
    }
  }
}

function buildFieldSearchRequest(projectId: number): FieldSearchReq | null {
  const query = fieldKeyword.value.trim()
  const request: FieldSearchReq = {
    projectId,
    limit: 50
  }
  if (query) {
    request.query = query
  }
  const [groupType, groupKey] = activeGroupKey.value.split(':')
  if (groupType === 'category' && groupKey) {
    request.category = groupKey
  }
  if (groupType === 'tag' && groupKey) {
    request.tag = groupKey
  }
  if (sourceBatchIdFilter.value) {
    request.sourceBatchId = sourceBatchIdFilter.value
  }
  if (fieldStatusFilter.value) {
    request.status = fieldStatusFilter.value
  }
  return request.query || request.category || request.tag || request.status || request.sourceBatchId ? request : null
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.current = 1
}

function handlePageChange(page: number) {
  pagination.current = page
}

function selectGroup(optionKey: string) {
  activeGroupKey.value = optionKey
}

function ensureActiveGroupExists() {
  if (!groupOptions.value.some((group) => group.optionKey === activeGroupKey.value)) {
    activeGroupKey.value = 'all'
  }
}

function handleSelectionChange(selection: Field[]) {
  selectedFields.value = selection
}

function openBatchDialog() {
  if (selectedFields.value.length === 0) {
    ElMessage.warning('请先选择字段')
    return
  }
  batchForm.applyDomain = true
  batchForm.applyCategory = true
  batchForm.applyTags = true
  batchForm.domainId = undefined
  batchForm.category = ''
  batchForm.tags = ''
  batchDialogVisible.value = true
}

async function handleBatchSubmit() {
  const projectId = projectStore.currentProjectId
  if (!projectId || selectedFields.value.length === 0) {
    return
  }
  const fieldIds = selectedFields.value
    .map((field) => Number(field.id))
    .filter((id) => Number.isFinite(id) && id > 0)
  if (fieldIds.length === 0) {
    ElMessage.warning('所选字段缺少有效 ID，请刷新后重试')
    return
  }
  const updates: NonNullable<FieldGroupingBatchUpdateReq['updates']> = {}
  if (batchForm.applyDomain) {
    updates.domainId = batchForm.domainId ?? ''
  }
  if (batchForm.applyCategory) {
    updates.category = batchForm.category
  }
  if (batchForm.applyTags) {
    updates.tags = batchForm.tags
  }
  if (Object.keys(updates).length === 0) {
    ElMessage.warning('请选择要更新的归组字段')
    return
  }
  try {
    await ElMessageBox.confirm(`确定更新 ${selectedFields.value.length} 个字段的归组信息吗？`, '批量归组', {
      type: 'warning',
      confirmButtonText: '更新',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  batchSubmitting.value = true
  try {
    const result = await batchUpdateFieldGrouping({
      projectId,
      fieldIds,
      updates
    })
    ElMessage.success(`已更新 ${result.updatedCount ?? selectedFields.value.length} 个字段`)
    batchDialogVisible.value = false
    selectedFields.value = []
    await loadFields()
  } finally {
    batchSubmitting.value = false
  }
}

function openBulkDialog() {
  if (selectedFields.value.length === 0) {
    ElMessage.warning('请先选择字段')
    return
  }
  bulkForm.applyStatus = false
  bulkForm.status = 'enabled'
  bulkForm.applyCategory = false
  bulkForm.category = ''
  bulkForm.applyTags = false
  bulkForm.tags = ''
  bulkForm.applySensitive = false
  bulkForm.sensitive = false
  bulkForm.applyCodeSetId = false
  bulkForm.codeSetId = undefined
  bulkForm.applyAliases = false
  bulkForm.aliases = ''
  bulkPreview.value = null
  bulkDialogVisible.value = true
}

function buildBulkUpdateRequest(): FieldBulkUpdateReq | null {
  const projectId = projectStore.currentProjectId
  if (!projectId || selectedFields.value.length === 0) {
    return null
  }
  const fieldIds = selectedFields.value
    .map((field) => Number(field.id))
    .filter((id) => Number.isFinite(id) && id > 0)
  if (fieldIds.length === 0) {
    ElMessage.warning('所选字段缺少有效 ID，请刷新后重试')
    return null
  }
  const updates: NonNullable<FieldBulkUpdateReq['updates']> = {}
  if (bulkForm.applyStatus) {
    updates.status = bulkForm.status
  }
  if (bulkForm.applyCategory) {
    updates.category = bulkForm.category
  }
  if (bulkForm.applyTags) {
    updates.tags = bulkForm.tags
  }
  if (bulkForm.applySensitive) {
    updates.sensitive = bulkForm.sensitive
  }
  if (bulkForm.applyCodeSetId) {
    updates.codeSetId = bulkForm.codeSetId ?? null
  }
  if (bulkForm.applyAliases) {
    updates.aliases = bulkForm.aliases
  }
  if (Object.keys(updates).length === 0) {
    ElMessage.warning('请选择要维护的字段属性')
    return null
  }
  return {
    projectId,
    fieldIds,
    updates
  }
}

async function handleBulkPreview() {
  const payload = buildBulkUpdateRequest()
  if (!payload) {
    return
  }
  bulkPreviewLoading.value = true
  try {
    bulkPreview.value = await previewFieldBulkUpdate(payload)
  } finally {
    bulkPreviewLoading.value = false
  }
}

async function handleBulkSubmit() {
  const payload = buildBulkUpdateRequest()
  if (!payload || !bulkPreview.value) {
    ElMessage.warning('请先生成预览')
    return
  }
  const changedCount = bulkPreview.value.changedCount ?? 0
  if (changedCount === 0) {
    ElMessage.info('所选字段没有变化')
    return
  }
  try {
    await ElMessageBox.confirm(`确定维护 ${changedCount} 个字段吗？`, '批量维护', {
      type: 'warning',
      confirmButtonText: '提交',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  bulkSubmitting.value = true
  try {
    const result = await bulkUpdateFields(payload)
    ElMessage.success(`已维护 ${result.updatedCount ?? changedCount} 个字段`)
    bulkDialogVisible.value = false
    selectedFields.value = []
    await loadFields()
  } finally {
    bulkSubmitting.value = false
  }
}

function resetForm(field?: Field) {
  form.projectId = projectStore.currentProjectId ?? field?.projectId ?? 0
  form.name = field?.name ?? ''
  form.displayName = field?.displayName ?? ''
  form.dataType = field?.dataType ?? 'varchar'
  form.length = field?.length
  form.precisionVal = field?.precisionVal
  form.scaleVal = field?.scaleVal
  form.nullable = field?.nullable ?? true
  form.defaultValue = field?.defaultValue ?? ''
  form.comment = field?.comment ?? ''
  form.domainId = field?.domainId
  form.tags = field?.tags ?? ''
  form.aliases = field?.aliases ?? ''
  form.category = field?.category ?? ''
  form.codeSetId = field?.codeSetId
  form.sensitive = field?.sensitive ?? false
  form.status = field?.status ?? 'enabled'
  form.replacementFieldId = field?.replacementFieldId
  form.replacementReason = field?.replacementReason ?? ''
  form.exampleValue = field?.exampleValue ?? ''
  form.formatType = field?.formatType ?? ''
  form.formatPattern = field?.formatPattern ?? ''
  form.formatUnit = field?.formatUnit ?? ''
  form.formatPrecision = field?.formatPrecision ?? ''
  form.formatTimezone = field?.formatTimezone ?? ''
  form.formatNullPolicy = field?.formatNullPolicy ?? ''
  form.validExamplesJson = field?.validExamplesJson ?? ''
  form.invalidExamplesJson = field?.invalidExamplesJson ?? ''
  form.formatNotes = field?.formatNotes ?? ''
  formatExamplesForm.validExamplesText = examplesJsonToLines(field?.validExamplesJson)
  formatExamplesForm.invalidExamplesText = examplesJsonToLines(field?.invalidExamplesJson)
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  editingField.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(field: Field) {
  editingField.value = field
  resetForm(field)
  dialogVisible.value = true
  if (field.id) {
    void syncFieldUrlState({ fieldId: field.id })
  }
}

function openMergeDialog(field: Field) {
  if (!field.id) {
    return
  }
  mergeInitialTargetId.value = field.id
  mergeDialogVisible.value = true
}

async function handleMergeApplied() {
  await loadFields()
}

async function openFieldFromRoute() {
  const fieldId = routeFieldId()
  const projectId = projectStore.currentProjectId
  if (!fieldId) {
    openedRouteFieldId.value = null
    return
  }
  if (!projectId || openedRouteFieldId.value === fieldId) {
    return
  }
  let field = fields.value.find((item) => item.id === fieldId)
  if (!field) {
    try {
      field = await getField(fieldId)
    } catch {
      ElMessage.warning('链接中的字段不存在或不可访问')
      void syncFieldUrlState({ fieldId: null })
      return
    }
  }
  if (field?.projectId !== projectId) {
    ElMessage.warning('链接中的字段不属于当前项目')
    void syncFieldUrlState({ fieldId: null })
    return
  }
  openedRouteFieldId.value = fieldId
  openEditDialog(field)
}

async function syncFieldUrlState(patch: Record<string, string | number | null> = {}) {
  await replaceRouteQuery(router, route, {
    projectId: projectStore.currentProjectId,
    keyword: fieldKeyword.value.trim() || null,
    ...patch
  })
}

async function handleCopyFieldLink() {
  try {
    await syncFieldUrlState()
    await copyRouteUrl(route, navigator.clipboard)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.error('复制失败，请手动复制浏览器地址')
  }
}

async function openSourceDialog(field: Field) {
  if (!field.id) {
    return
  }
  sourceField.value = field
  sourceDialogVisible.value = true
  sourceLoading.value = true
  try {
    fieldSources.value = await listFieldSources(field.id)
  } finally {
    sourceLoading.value = false
  }
}

async function openImpactDialog(field: Field) {
  if (!field.id || !projectStore.currentProjectId) {
    return
  }
  impactField.value = field
  impactReport.value = null
  impactDialogVisible.value = true
  impactLoading.value = true
  try {
    impactReport.value = await getFieldImpactReport(field.id, projectStore.currentProjectId)
  } finally {
    impactLoading.value = false
  }
}

async function openChangeLogDialog(field: Field) {
  if (!field.id) {
    return
  }
  changeLogField.value = field
  changeLogCurrent.value = 1
  changeLogDialogVisible.value = true
  await loadChangeLogs()
}

async function loadChangeLogs() {
  const projectId = projectStore.currentProjectId
  const fieldId = changeLogField.value?.id
  if (!projectId || !fieldId) {
    changeLogs.value = []
    changeLogTotal.value = 0
    return
  }
  changeLogLoading.value = true
  try {
    const page = await listChangeLogs(projectId, 'field', fieldId, changeLogCurrent.value, changeLogSize)
    changeLogs.value = page.records ?? []
    changeLogTotal.value = Number(page.total ?? 0)
  } finally {
    changeLogLoading.value = false
  }
}

function handleChangeLogPageChange(page: number) {
  changeLogCurrent.value = page
  void loadChangeLogs()
}

function canUndoLog(log: StandardChangeLog) {
  return Boolean(
    log.id &&
      log.beforeJson &&
      (log.action === 'update' || log.action === 'undo')
  )
}

async function handleUndoChange(log: StandardChangeLog) {
  const fieldId = changeLogField.value?.id
  if (!fieldId || !log.id || !canUndoLog(log)) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定将字段恢复到该日志变更前的版本吗？',
      '回退字段变更',
      {
        type: 'warning',
        confirmButtonText: '回退',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  undoSubmitting.value = log.id
  try {
    await undoFieldChange(fieldId, log.id)
    ElMessage.success('字段已回退')
    await loadFields()
    await loadChangeLogs()
  } finally {
    undoSubmitting.value = null
  }
}

async function handleSubmit() {
  if (!projectStore.currentProjectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  await formRef.value?.validate()
  if (!(await confirmImpactBeforeSave())) {
    return
  }
  submitting.value = true
  try {
    const payload: FieldReq = {
      ...form,
      projectId: projectStore.currentProjectId,
      validExamplesJson: examplesLinesToJson(formatExamplesForm.validExamplesText),
      invalidExamplesJson: examplesLinesToJson(formatExamplesForm.invalidExamplesText)
    }
    if (editingField.value?.id) {
      await updateField(editingField.value.id, payload)
      ElMessage.success('字段已更新')
    } else {
      await createField(payload)
      ElMessage.success('字段已创建')
    }
    dialogVisible.value = false
    await loadFields()
  } finally {
    submitting.value = false
  }
}

async function confirmImpactBeforeSave() {
  const projectId = projectStore.currentProjectId
  const field = editingField.value
  if (!projectId || !field?.id) {
    return true
  }
  const payload: FieldReq = {
    ...form,
    projectId,
    validExamplesJson: examplesLinesToJson(formatExamplesForm.validExamplesText),
    invalidExamplesJson: examplesLinesToJson(formatExamplesForm.invalidExamplesText)
  }
  let preview: StandardChangePreview
  try {
    preview = await previewFieldChange(field.id, payload)
  } catch {
    ElMessage.warning('标准变更预览暂不可用，已继续保存')
    return true
  }
  if (!shouldShowStandardChangeConfirm(preview)) {
    return true
  }
  try {
    await ElMessageBox.confirm(
      standardChangeConfirmMessage(preview),
      `标准变更预览：${standardChangeRiskText(preview.riskLevel)}`,
      {
        type: preview.riskLevel === 'HIGH' ? 'error' : 'warning',
        confirmButtonText: '继续保存',
        cancelButtonText: '返回编辑'
      }
    )
    return true
  } catch {
    ElMessage.info('已取消保存')
    return false
  }
}

async function handleDelete(field: Field) {
  if (!field.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除字段「${field.name ?? ''}」吗？`, '删除字段', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteField(field.id)
  ElMessage.success('字段已删除')
  await loadFields()
}

function groupOptionKey(group: FieldGroupItem) {
  return `${group.groupType ?? 'unknown'}:${group.groupKey ?? ''}`
}

function isSearchableGroupKey(optionKey: string) {
  const [groupType, groupKey] = optionKey.split(':')
  return Boolean(groupKey && (groupType === 'category' || groupType === 'tag'))
}

function groupDisplayName(group: FieldGroupItem) {
  if (group.groupType === 'ungrouped') {
    return '未分组'
  }
  if (group.groupType === 'domain') {
    return `数据域：${domainNameById(group.groupKey)}`
  }
  if (group.groupType === 'category') {
    return `分类：${group.groupName || group.groupKey || '-'}`
  }
  if (group.groupType === 'tag') {
    return `标签：${group.groupName || group.groupKey || '-'}`
  }
  return group.groupName || group.groupKey || '其他'
}

function matchesActiveGroup(field: Field) {
  if (activeGroupKey.value === 'all') {
    return true
  }
  const [groupType, groupKey] = activeGroupKey.value.split(':')
  if (groupType === 'domain') {
    return String(field.domainId ?? '') === groupKey
  }
  if (groupType === 'category') {
    return (field.category ?? '').trim() === groupKey
  }
  if (groupType === 'tag') {
    return splitTags(field.tags).includes(groupKey)
  }
  if (groupType === 'ungrouped') {
    return isUngrouped(field)
  }
  return true
}

function matchesFieldStatus(field: Field) {
  return !fieldStatusFilter.value || field.status === fieldStatusFilter.value
}

function fieldSearchReasons(field: Field) {
  const id = field.id
  if (typeof id !== 'number') {
    return []
  }
  return fieldSearchItemByFieldId.value.get(id)?.matchReasons?.filter(Boolean) ?? []
}

function fieldSearchRecommendedUse(field: Field) {
  const id = field.id
  if (typeof id !== 'number') {
    return ''
  }
  return fieldSearchItemByFieldId.value.get(id)?.recommendedUse ?? ''
}

function fieldGroupLabel(field: Field) {
  const parts = []
  if (field.domainId) {
    parts.push(domainNameById(String(field.domainId)))
  }
  if (field.category) {
    parts.push(field.category)
  }
  return parts.length > 0 ? parts.join(' / ') : '未分组'
}

function replacementFieldLabel(field: Field) {
  return `${field.name ?? '-'}${field.displayName ? `（${field.displayName}）` : ''}`
}

function fieldFormatSummary(field: Field) {
  const parts = [
    formatPart('类型', field.formatType),
    formatPart('单位', field.formatUnit),
    formatPart('精度', field.formatPrecision),
    formatPart('时区', field.formatTimezone),
    formatPart('空值', field.formatNullPolicy),
    formatPart('正例', examplesJsonPreview(field.validExamplesJson)),
    formatPart('反例', examplesJsonPreview(field.invalidExamplesJson))
  ].filter(Boolean)
  if (parts.length === 0 && !field.formatPattern && !field.formatNotes) {
    return ''
  }
  if (field.formatPattern) {
    parts.push(`模式 ${field.formatPattern}`)
  }
  if (field.formatNotes) {
    parts.push(field.formatNotes)
  }
  return parts.join(' / ')
}

function formatPart(label: string, value?: string | null) {
  return value?.trim() ? `${label} ${value.trim()}` : ''
}

function examplesJsonPreview(value?: string | null) {
  const examples = parseExamplesJson(value)
  if (examples.length === 0) {
    return ''
  }
  const visible = examples.slice(0, 2).map(formatExampleForPreview).join(', ')
  return examples.length > 2 ? `${visible} 等 ${examples.length} 个` : visible
}

function formatExampleForPreview(value: string) {
  return value.trim() === value && value.length > 0 ? value : JSON.stringify(value)
}

function replacementSummary(field: Field) {
  if (field.replacementFieldId) {
    const replacement = fields.value.find((item) => item.id === field.replacementFieldId)
    return replacement ? `替代：${replacement.name}` : `替代：#${field.replacementFieldId}`
  }
  return field.replacementReason ? `说明：${field.replacementReason}` : ''
}

function domainNameById(value?: string) {
  const domain = domains.value.find((item) => String(item.id) === String(value ?? ''))
  return domain ? domainLabel(domain) : value || '-'
}

function domainLabel(domain: Domain) {
  const name = domain.name || `#${domain.id ?? '-'}`
  return domain.code ? `${name} (${domain.code})` : name
}

function splitTags(value?: string) {
  return Array.from(new Set((value ?? '')
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)))
    .sort()
}

function parseExamplesJson(value?: string | null) {
  if (!value?.trim()) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed)
      ? parsed.filter((item): item is string => typeof item === 'string')
      : []
  } catch {
    return []
  }
}

function examplesJsonToLines(value?: string | null) {
  return parseExamplesJson(value).map(formatExampleForLineEdit).join('\n')
}

function formatExampleForLineEdit(value: string) {
  return value.trim() === value && value.length > 0 ? value : JSON.stringify(value)
}

function examplesLinesToJson(value: string) {
  const examples = value
    .split(/\r?\n/)
    .map(parseExampleLine)
    .filter((item): item is string => item !== null)
  return examples.length > 0 ? JSON.stringify(examples) : ''
}

function parseExampleLine(line: string) {
  const text = line.trim()
  if (!text) {
    return null
  }
  try {
    const parsed = JSON.parse(text)
    if (typeof parsed === 'string') {
      return parsed
    }
  } catch {
    // 普通示例按原有每行文本处理；JSON 字符串行只用于表达空串或保留首尾空格。
  }
  return text
}

function isUngrouped(field: Field) {
  return !field.domainId && !field.category?.trim() && splitTags(field.tags).length === 0
}

function formatDataType(field: Field) {
  if (!field.dataType) {
    return '-'
  }
  if (field.precisionVal !== undefined && field.precisionVal !== null) {
    const scale = field.scaleVal !== undefined && field.scaleVal !== null ? `,${field.scaleVal}` : ''
    return `${field.dataType}(${field.precisionVal}${scale})`
  }
  return field.length ? `${field.dataType}(${field.length})` : field.dataType
}

function statusText(status?: string) {
  if (status === 'draft') {
    return '草稿'
  }
  if (status === 'disabled') {
    return '停用'
  }
  if (status === 'deprecated') {
    return '废弃'
  }
  return '启用'
}

function statusTagType(status?: string) {
  if (status === 'draft') {
    return 'primary'
  }
  if (status === 'disabled') {
    return 'info'
  }
  if (status === 'deprecated') {
    return 'warning'
  }
  return 'success'
}

function bulkAttributeText(attribute?: string) {
  const labels: Record<string, string> = {
    status: '状态',
    category: '分类',
    tags: '标签',
    sensitive: '敏感',
    codeSetId: '代码集',
    aliases: '别名',
    name: '字段名',
    displayName: '显示名',
    dataType: '类型',
    nullable: '空值',
    comment: '注释',
    domainId: '数据域',
    replacementFieldId: '替代字段',
    replacementReason: '替代说明',
    exampleValue: '示例',
    formatType: '格式类型',
    formatPattern: '格式模式',
    formatUnit: '单位',
    formatPrecision: '值精度',
    formatTimezone: '时区',
    formatNullPolicy: '空值策略',
    validExamplesJson: '正例',
    invalidExamplesJson: '反例',
    formatNotes: '格式备注'
  }
  return attribute ? labels[attribute] ?? attribute : '-'
}

function formatChangeValue(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '空'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  if (Array.isArray(value)) {
    return value.length > 0 ? value.join(', ') : '空'
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

function changeLogActionText(action?: string) {
  if (action === 'update') {
    return '更新'
  }
  if (action === 'undo') {
    return '回退'
  }
  if (action === 'create') {
    return '创建'
  }
  if (action === 'delete') {
    return '删除'
  }
  return action || '-'
}

function changeLogActionTagType(action?: string) {
  if (action === 'undo') {
    return 'warning'
  }
  if (action === 'update') {
    return 'primary'
  }
  if (action === 'create') {
    return 'success'
  }
  if (action === 'delete') {
    return 'danger'
  }
  return 'info'
}

function changeLogSummary(log: StandardChangeLog) {
  const before = parseJsonRecord(log.beforeJson)
  const after = parseJsonRecord(log.afterJson)
  if (!before || !after) {
    return '-'
  }
  const keys = [
    'name',
    'displayName',
    'dataType',
    'status',
    'replacementFieldId',
    'replacementReason',
    'category',
    'tags',
    'sensitive',
    'codeSetId',
    'aliases',
    'domainId',
    'formatType',
    'formatPattern',
    'formatUnit',
    'formatPrecision',
    'formatTimezone',
    'formatNullPolicy',
    'validExamplesJson',
    'invalidExamplesJson',
    'formatNotes'
  ]
  const changedLabels = keys
    .filter((key) => JSON.stringify(before[key]) !== JSON.stringify(after[key]))
    .map((key) => bulkAttributeText(key))
  if (changedLabels.length === 0) {
    return '无关键属性变化'
  }
  const visible = changedLabels.slice(0, 5).join('、')
  return changedLabels.length > 5 ? `${visible} 等 ${changedLabels.length} 项` : visible
}

function parseJsonRecord(value?: string): Record<string, unknown> | null {
  if (!value) {
    return null
  }
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as Record<string, unknown>
      : null
  } catch {
    return null
  }
}

function sourceDatabaseLabel(row: FieldSourceDetail) {
  const parts = [row.batch?.databaseName, row.batch?.schemaName].filter(Boolean)
  return parts.length > 0 ? parts.join(' / ') : '-'
}

function sourceColumnLabel(row: FieldSourceDetail) {
  const parts = [row.source?.tableName, row.source?.columnName].filter(Boolean)
  return parts.length > 0 ? parts.join('.') : '-'
}

function formatDate(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

function routeKeyword(value: unknown) {
  if (Array.isArray(value)) {
    return typeof value[0] === 'string' ? value[0].trim() : ''
  }
  return typeof value === 'string' ? value.trim() : ''
}

function routeFieldId() {
  const fieldId = readPositiveIntQuery(route.query, 'fieldId')
  if (route.query.fieldId && !fieldId) {
    ElMessage.warning('链接中的字段 ID 无效，已清理')
    void syncFieldUrlState({ fieldId: null })
  }
  return fieldId
}
</script>

<style scoped>
.field-page {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 140px);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #606266;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.field-table {
  width: 100%;
}

.field-name-cell {
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}

.search-reason {
  margin-top: 3px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
  word-break: break-word;
}

.format-summary {
  color: #303133;
  font-size: 12px;
  line-height: 1.45;
  word-break: break-word;
}

.field-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 420px) 136px 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.status-filter {
  width: 136px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.toolbar-count {
  color: #6b7280;
  font-size: 13px;
}

.search-insight {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #d9e8ff;
  border-radius: 4px;
  background: #f7fbff;
  color: #303133;
  font-size: 13px;
}

.search-insight-main {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.search-insight-label {
  font-weight: 600;
  color: #1d4ed8;
}

.search-insight-line {
  margin-top: 4px;
  color: #606266;
}

.field-content {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.group-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 560px;
  overflow: auto;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
}

.group-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 34px;
  padding: 6px 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #303133;
  text-align: left;
  cursor: pointer;
}

.group-option:hover,
.group-option.active {
  background: #eef5ff;
  color: #1d4ed8;
}

.group-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-meta {
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.full-width {
  width: 100%;
}

.format-section-title {
  margin: 12px 0 10px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.batch-input {
  width: 320px;
  margin-left: 12px;
}

.bulk-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bulk-input {
  width: 220px;
  margin-left: 12px;
}

.bulk-wide-input {
  width: 520px;
  margin-left: 12px;
}

.bulk-switch {
  margin-left: 12px;
}

.bulk-preview {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.bulk-preview-summary {
  margin-bottom: 10px;
  color: #303133;
  font-size: 13px;
}

.change-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.change-chip {
  max-width: 100%;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f4f6f8;
  color: #303133;
  font-size: 12px;
  line-height: 22px;
  word-break: break-all;
}

.compact-pagination {
  margin-top: 12px;
}

.muted-text {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

.lifecycle-hint {
  max-width: 132px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .field-toolbar {
    grid-template-columns: 1fr;
  }

  .status-filter {
    width: 100%;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .field-content {
    grid-template-columns: 1fr;
  }

  .group-panel {
    max-height: 220px;
    border-right: 0;
    border-bottom: 1px solid #ebeef5;
    padding-right: 0;
    padding-bottom: 10px;
  }

  .batch-input {
    width: 100%;
    margin-left: 0;
    margin-top: 8px;
  }

  .bulk-input,
  .bulk-wide-input {
    width: 100%;
    margin-left: 0;
    margin-top: 8px;
  }
}
</style>
