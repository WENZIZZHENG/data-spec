import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { expect, test } from '@playwright/test'
import { installDataSpecRouteHarness } from './support/routeHarness'

test.afterEach(async ({ page }, testInfo) => {
  if (testInfo.status === testInfo.expectedStatus) {
    return
  }
  const contextDir = path.join(testInfo.outputDir, 'failure-context')
  await mkdir(contextDir, { recursive: true })
  await writeFile(path.join(contextDir, 'current-url.txt'), page.url(), 'utf8')
})

test('核心工作流支持键盘入口、可读名称和弹窗焦点恢复', async ({ page }) => {
  const harness = await installDataSpecRouteHarness(page)

  await page.goto('/projects')
  await page.getByRole('button', { name: /新建项目/ }).click()
  await page.getByLabel('项目名称').fill(harness.project.name)
  await page.getByLabel('描述').fill(harness.project.description)
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByRole('row').filter({ hasText: harness.project.name })).toContainText('当前')

  await test.step('skip link 可把键盘焦点送到主内容区', async () => {
    await page.evaluate(() => {
      document.body.setAttribute('tabindex', '-1')
      document.body.focus()
      document.body.removeAttribute('tabindex')
    })
    await page.keyboard.press('Tab')
    const skipLink = page.locator('.skip-link')
    await expect(skipLink).toBeFocused()
    await page.keyboard.press('Enter')
    await expect(page.locator('#main-content')).toBeFocused()
  })

  await test.step('命令面板快捷键可打开并在关闭后恢复触发焦点', async () => {
    const commandButton = page.getByRole('button', { name: '打开命令面板' })
    await commandButton.focus()
    await page.keyboard.press('Control+K')
    const dialog = page.getByRole('dialog', { name: '命令面板' })
    await expect(dialog).toBeVisible()
    await expect(dialog.getByLabel('命令搜索')).toBeFocused()
    await page.keyboard.press('Escape')
    await expect(dialog).toBeHidden()
    await expect(commandButton).toBeFocused()
  })

  await test.step('命令面板执行导航命令后焦点落到目标主内容', async () => {
    const commandButton = page.getByRole('button', { name: '打开命令面板' })
    await commandButton.focus()
    await page.keyboard.press('Control+K')
    const dialog = page.getByRole('dialog', { name: '命令面板' })
    await expect(dialog).toBeVisible()
    await dialog.getByRole('button', { name: /SQL 校验/ }).click()
    await expect(page).toHaveURL(/\/sql-lint/)
    await expect(dialog).toBeHidden()
    await expect(page.locator('#main-content')).toBeFocused()
  })

  await test.step('SQL 校验和记录详情可以通过键盘触发', async () => {
    await page.goto('/sql-lint?demo=lint')
    const runButton = page.getByRole('button', { name: '执行校验 SQL' })
    await runButton.focus()
    await page.keyboard.press('Enter')
    await expect(page.getByText('修正 SQL').first()).toBeVisible()

    const historyToggle = page.getByRole('button', { name: /最近检查记录\s+1 条/ })
    await historyToggle.focus()
    await page.keyboard.press('Enter')
    const detailButton = page.getByRole('button', { name: '查看详情 SQL 检查记录' })
    await detailButton.focus()
    await page.keyboard.press('Enter')
    const recordDialog = page.getByRole('dialog', { name: '检查记录详情' })
    await expect(recordDialog).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(recordDialog).toBeHidden()
    await expect(detailButton).toBeFocused()

    const commandButton = page.getByRole('button', { name: '打开命令面板' })
    await commandButton.focus()
    await page.keyboard.press('Control+K')
    const commandDialog = page.getByRole('dialog', { name: '命令面板' })
    await expect(commandDialog).toBeVisible()
    await commandDialog.getByRole('button', { name: /继续 SQL 检查 #701/ }).click()
    await expect(recordDialog).toBeVisible()
    await expect(commandDialog).toBeHidden()
    await expect.poll(() => page.evaluate(() => {
      const dialog = document.querySelector('[role="dialog"][aria-label="检查记录详情"]')
      return Boolean(dialog && document.activeElement && dialog.contains(document.activeElement))
    })).toBe(true)
  })

  await test.step('字段库筛选输入拥有稳定可读名称', async () => {
    await page.goto('/fields')
    await page.getByLabel('筛选标准字段').fill('手机号')
    await expect(page.getByText('buyer_mobile')).toBeVisible()
    await expect(page.getByText('order_amount')).toHaveCount(0)
  })

  expect(harness.unhandledApiRequests).toEqual([])
})
