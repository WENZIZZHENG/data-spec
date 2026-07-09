import type { Locator, Page } from '@playwright/test'

/**
 * 从稳定选择器容器中找到可编辑控件。
 *
 * Vue 页面会把 `data-testid` 放在 Element Plus 组件外层 wrapper 上，避免依赖组件内部属性透传。
 */
export function controlInput(page: Page, testId: string): Locator {
  return page.getByTestId(testId).locator('input, textarea').first()
}
