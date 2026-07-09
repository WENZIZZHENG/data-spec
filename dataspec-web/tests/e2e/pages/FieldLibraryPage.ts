import { expect, type Page } from '@playwright/test'
import { aiActionNames, stableTestIds } from '../../../src/utils/stableTestIds'
import { controlInput } from './locators'

/**
 * 标准字段库页的浏览器级页面对象。
 *
 * 封装字段检索主路径，调用方通过业务关键词断言结果，而不依赖页面布局或表格列顺序。
 */
export class FieldLibraryPage {
  /** AI browser automation 可复用的动作名称。 */
  readonly actionNames = aiActionNames.fields

  constructor(private readonly page: Page) {}

  /** 打开字段库并等待页面根节点。 */
  async goto() {
    await this.page.goto('/fields')
    await expect(this.page.getByTestId(stableTestIds.fields.page)).toBeVisible()
  }

  /** 输入字段检索关键词并等待检索洞察出现。 */
  async search(keyword: string) {
    await controlInput(this.page, stableTestIds.fields.searchInput).fill(keyword)
    await expect(this.page.getByTestId(stableTestIds.fields.searchInsight)).toBeVisible()
  }

  /** 断言字段表格中存在目标文本。 */
  async expectSearchResult(text: string) {
    await expect(this.page.getByTestId(stableTestIds.fields.table)).toContainText(text)
  }

  /** 断言字段表格中不存在目标文本。 */
  async expectSearchResultHidden(text: string) {
    await expect(this.page.getByTestId(stableTestIds.fields.table).getByText(text)).toHaveCount(0)
  }
}
