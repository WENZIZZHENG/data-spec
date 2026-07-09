import { expect, type Page } from '@playwright/test'
import { aiActionNames, stableTestIds } from '../../../src/utils/stableTestIds'

/**
 * AI Context 页的浏览器级页面对象。
 *
 * 封装三类上下文预览切换和断言，避免自动化脚本直接依赖 tab 文案。
 */
export class AiContextPage {
  /** AI browser automation 可复用的动作名称。 */
  readonly actionNames = aiActionNames.aiContext

  constructor(private readonly page: Page) {}

  /** 打开 AI Context 页并等待页面根节点。 */
  async goto() {
    await this.page.goto('/ai-export')
    await expect(this.page.getByTestId(stableTestIds.aiContext.page)).toBeVisible()
  }

  /** 断言 DATABASE_RULES.md 预览包含目标文本。 */
  async expectDatabaseRules(text: string) {
    await expect(this.page.getByTestId(stableTestIds.aiContext.previewTabs)).toContainText(text)
  }

  /** 切换到 field-catalog.json 并断言预览内容。 */
  async expectFieldCatalog(text: string) {
    const tabs = this.page.getByTestId(stableTestIds.aiContext.previewTabs)
    await this.page.getByTestId(stableTestIds.aiContext.fieldCatalogTab).click()
    await expect(tabs).toContainText(text)
  }

  /** 切换到 rules.yaml 并断言预览内容。 */
  async expectRulesYaml(text: string) {
    const tabs = this.page.getByTestId(stableTestIds.aiContext.previewTabs)
    await this.page.getByTestId(stableTestIds.aiContext.rulesYamlTab).click()
    await expect(tabs).toContainText(text)
  }
}
