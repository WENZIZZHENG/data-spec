import { expect, type Page } from '@playwright/test'
import { aiActionNames, stableTestIds } from '../../../src/utils/stableTestIds'

/**
 * SQL 校验页的浏览器级页面对象。
 *
 * 聚合执行校验、查看 fixedSql 和打开最近记录详情的主路径，避免 E2E 用例直接依赖中文标题或表格结构。
 */
export class SqlLintPage {
  /** AI browser automation 可复用的动作名称。 */
  readonly actionNames = aiActionNames.sqlLint

  constructor(private readonly page: Page) {}

  /** 打开带演示 SQL 的校验页并等待页面根节点。 */
  async gotoDemoLint() {
    await this.page.goto('/sql-lint?demo=lint')
    await expect(this.page.getByTestId(stableTestIds.sqlLint.page)).toBeVisible()
  }

  /** 执行校验并等待 fixedSql 面板出现。 */
  async runLint() {
    await this.page.getByTestId(stableTestIds.sqlLint.runButton).click()
    await expect(this.page.getByTestId(stableTestIds.sqlLint.fixedSqlPanel)).toContainText('buyer_mobile')
  }

  /** 展开历史记录并打开最新记录详情。 */
  async openLatestRecordDetail() {
    await this.page.getByTestId(stableTestIds.sqlLint.historyToggle).click()
    await this.page.getByTestId(stableTestIds.sqlLint.recordDetailButton).click()
  }

  /** 断言记录弹窗包含指定证据文本，并在完成后关闭弹窗。 */
  async expectRecordDialogContains(text: string) {
    const dialog = this.page.getByTestId(stableTestIds.sqlLint.recordDialog)
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText(text)
    await this.page.keyboard.press('Escape')
  }
}
