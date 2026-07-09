/**
 * 前端稳定测试选择器约定。
 *
 * 这些 id 同时服务 Playwright 页面对象和 AI browser automation。它们描述用户可识别的页面语义，
 * 不绑定 Element Plus 的 DOM 结构或中文文案，避免样式和文案微调导致浏览器级测试脆弱。
 */
export const dataTestIdPolicy = {
  attribute: 'data-testid',
  scope: 'core-e2e-and-ai-browser-automation',
  naming: 'pageArea.semanticElement',
  owner: 'dataspec-web/tests/e2e'
} as const

/**
 * AI 自动化可复用的页面动作名称。
 *
 * 名称保持稳定，具体点击、输入和断言逻辑由 Playwright page object 维护。
 */
export const aiActionNames = {
  projects: {
    createAndSelectProject: 'projects.createAndSelectProject'
  },
  sqlLint: {
    runAndOpenRecord: 'sqlLint.runAndOpenRecord'
  },
  reverseImport: {
    browseDatabaseMetadata: 'reverseImport.browseDatabaseMetadata',
    generatePreview: 'reverseImport.generatePreview'
  },
  fields: {
    searchField: 'fields.searchField'
  },
  aiContext: {
    inspectPreviewTabs: 'aiContext.inspectPreviewTabs'
  }
} as const

/**
 * 核心 E2E 页面稳定选择器。
 *
 * 第一版覆盖项目选择、字段库、SQL 校验、反向导入和 AI Context 主路径；后续页面按同一命名约定追加。
 */
export const stableTestIds = {
  projects: {
    page: 'projects.page',
    table: 'projects.table',
    newProjectButton: 'projects.newProjectButton',
    projectNameInput: 'projects.projectNameInput',
    projectDescriptionInput: 'projects.projectDescriptionInput',
    saveProjectButton: 'projects.saveProjectButton'
  },
  sqlLint: {
    page: 'sqlLint.page',
    runButton: 'sqlLint.runButton',
    fixedSqlPanel: 'sqlLint.fixedSqlPanel',
    historyPanel: 'sqlLint.historyPanel',
    historyToggle: 'sqlLint.historyToggle',
    recordTable: 'sqlLint.recordTable',
    recordDetailButton: 'sqlLint.recordDetailButton',
    recordDialog: 'sqlLint.recordDialog'
  },
  reverseImport: {
    page: 'reverseImport.page',
    databaseModeTab: 'reverseImport.databaseModeTab',
    databaseNameInput: 'reverseImport.databaseNameInput',
    schemaNameInput: 'reverseImport.schemaNameInput',
    usernameInput: 'reverseImport.usernameInput',
    loadTablesButton: 'reverseImport.loadTablesButton',
    tableChecklist: 'reverseImport.tableChecklist',
    browseMetadataButton: 'reverseImport.browseMetadataButton',
    metadataBrowserPanel: 'reverseImport.metadataBrowserPanel',
    generatePreviewButton: 'reverseImport.generatePreviewButton',
    fieldCandidatesTab: 'reverseImport.fieldCandidatesTab',
    previewTabs: 'reverseImport.previewTabs'
  },
  fields: {
    page: 'fields.page',
    toolbar: 'fields.toolbar',
    searchInput: 'fields.searchInput',
    searchInsight: 'fields.searchInsight',
    table: 'fields.table'
  },
  aiContext: {
    page: 'aiContext.page',
    scopeToolbar: 'aiContext.scopeToolbar',
    databaseRulesTab: 'aiContext.databaseRulesTab',
    fieldCatalogTab: 'aiContext.fieldCatalogTab',
    rulesYamlTab: 'aiContext.rulesYamlTab',
    previewTabs: 'aiContext.previewTabs'
  }
} as const

export type StableTestIds = typeof stableTestIds
export type AiActionNames = typeof aiActionNames

/**
 * 生成反向导入表项的动态稳定选择器。
 *
 * 选择器只来源于 schema/table 元数据，不依赖页面展示文案；缺少 schema 时仍保持同一张表的确定性 id。
 */
export function reverseImportTableOptionTestId(schemaName?: string, tableName?: string): string {
  return `reverseImport.tableOption${toStableSegment([schemaName, tableName])}`
}

function toStableSegment(parts: Array<string | undefined>): string {
  const words = parts
    .flatMap((part) => (part ?? '').split(/[^a-zA-Z0-9]+/))
    .filter(Boolean)
  if (words.length === 0) {
    return 'Unknown'
  }
  return words
    .map((word) => `${word.charAt(0).toUpperCase()}${word.slice(1)}`)
    .join('')
}
