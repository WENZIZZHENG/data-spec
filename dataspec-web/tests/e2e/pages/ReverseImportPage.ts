import { expect, type Page } from '@playwright/test'
import {
  aiActionNames,
  reverseImportTableOptionTestId,
  stableTestIds
} from '../../../src/utils/stableTestIds'
import { controlInput } from './locators'

/**
 * 反向导入数据库连接输入。
 *
 * 仅包含测试所需的非敏感连接元数据；密码、token、JDBC URL 等凭据不应进入 POM 输入。
 */
export interface ReverseImportConnectionInput {
  /** 测试数据库名或等价 fixture 名称。 */
  databaseName: string
  /** 只读 metadata 查询使用的 schema/database 名称。 */
  schemaName: string
  /** 无密码的只读用户名 fixture。 */
  username: string
}

/** 反向导入表选择目标，使用源 metadata 字段而非可见文案定位。 */
export interface ReverseImportTableTarget {
  /** 表所属 schema；MySQL 等无 schema 场景可为空。 */
  schemaName?: string
  /** 源库表名。 */
  tableName: string
}

/**
 * 反向导入页的浏览器级页面对象。
 *
 * 覆盖数据库直连、表选择、元数据浏览和字段候选预览主路径；所有点击点通过稳定选择器定位。
 */
export class ReverseImportPage {
  /** AI browser automation 可复用的动作名称。 */
  readonly actionNames = aiActionNames.reverseImport

  constructor(private readonly page: Page) {}

  /** 打开反向导入页并等待页面根节点。 */
  async goto() {
    await this.page.goto('/reverse-import')
    await expect(this.page.getByTestId(stableTestIds.reverseImport.page)).toBeVisible()
  }

  /** 切换到数据库直连模式。 */
  async openDatabaseMode() {
    await this.page.getByTestId(stableTestIds.reverseImport.databaseModeTab).click()
  }

  /** 填充无敏感信息的连接元数据。 */
  async fillConnection(input: ReverseImportConnectionInput) {
    await controlInput(this.page, stableTestIds.reverseImport.databaseNameInput).fill(input.databaseName)
    await controlInput(this.page, stableTestIds.reverseImport.schemaNameInput).fill(input.schemaName)
    await controlInput(this.page, stableTestIds.reverseImport.usernameInput).fill(input.username)
  }

  /** 加载可选择的数据库表列表。 */
  async loadTables() {
    await this.page.getByTestId(stableTestIds.reverseImport.loadTablesButton).click()
  }

  /** 按源 metadata 选择一张表。 */
  async selectTable(target: ReverseImportTableTarget) {
    const tableChoice = this.page.getByTestId(
      reverseImportTableOptionTestId(target.schemaName, target.tableName)
    )
    await expect(tableChoice).toBeVisible()
    await tableChoice.click()
  }

  /** 打开只读 metadata 浏览面板。 */
  async browseMetadata() {
    await this.page.getByTestId(stableTestIds.reverseImport.browseMetadataButton).click()
    await expect(this.page.getByTestId(stableTestIds.reverseImport.metadataBrowserPanel)).toBeVisible()
  }

  /** 断言 metadata 浏览结果包含指定字段。 */
  async expectMetadataField(fieldName: string) {
    await expect(this.page.getByTestId(stableTestIds.reverseImport.metadataBrowserPanel)).toContainText(fieldName)
  }

  /** 生成反向导入预览并等待结果 tabs。 */
  async generatePreview() {
    await this.page.getByTestId(stableTestIds.reverseImport.generatePreviewButton).click()
    await expect(this.page.getByTestId(stableTestIds.reverseImport.previewTabs)).toBeVisible()
  }

  /** 生成只读 COMMENT 回写计划并等待结果面板。 */
  async generateCommentPlan() {
    await this.page.getByTestId(stableTestIds.reverseImport.commentPlanButton).click()
    await expect(this.page.getByTestId(stableTestIds.reverseImport.commentPlanPanel)).toBeVisible()
  }

  /** 断言 COMMENT 回写计划面板包含安全审阅所需内容。 */
  async expectCommentPlan(content: RegExp | string) {
    await expect(this.page.getByTestId(stableTestIds.reverseImport.commentPlanPanel)).toContainText(content)
  }

  /** 切换到字段候选结果页签。 */
  async openFieldCandidates() {
    await this.page.getByTestId(stableTestIds.reverseImport.fieldCandidatesTab).click()
  }

  /** 断言字段候选表格中存在匹配行。 */
  async expectCandidateRow(name: RegExp) {
    await expect(this.page.getByRole('row', { name })).toBeVisible()
  }
}
