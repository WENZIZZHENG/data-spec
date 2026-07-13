import { expect, test, type Page, type Route } from '@playwright/test'
import { stableTestIds } from '../../src/utils/stableTestIds'
import { FieldLibraryPage } from './pages/FieldLibraryPage'
import { controlInput } from './pages/locators'
import { ProjectListPage } from './pages/ProjectListPage'
import { installDataSpecRouteHarness } from './support/routeHarness'

const paginatedFields = Array.from({ length: 65 }, (_, index) => {
  const ordinal = String(index + 1).padStart(3, '0')
  return {
    id: 1_000 + index,
    projectId: 101,
    name: `pagination_field_${ordinal}`,
    displayName: `分页字段 ${ordinal}`,
    dataType: 'varchar(32)',
    category: '性能回归',
    tags: '分页,回归',
    status: 'enabled',
    nullable: true,
    sensitive: false,
    comment: `分页搜索回归字段 ${ordinal}`
  }
})

const staleField = {
  ...paginatedFields[0],
  id: 2_001,
  name: 'stale_response_field',
  displayName: '旧响应字段'
}

const freshField = {
  ...paginatedFields[0],
  id: 2_002,
  name: 'fresh_response_field',
  displayName: '新响应字段'
}

const slowField = {
  ...paginatedFields[0],
  id: 2_003,
  name: 'slow_response_field',
  displayName: '慢请求字段'
}

test('字段搜索超过 50 条时可服务端翻页，元数据和全量候选不会重复加载', async ({ page }) => {
  const state = await openFieldLibrary(page)
  const input = controlInput(page, stableTestIds.fields.searchInput)
  const table = page.getByTestId(stableTestIds.fields.table)

  await input.fill('分')
  await page.waitForTimeout(60)
  await input.fill('分页')
  await page.waitForTimeout(60)
  await input.fill('分页字段')

  await expect(table).toContainText('pagination_field_001')
  await expect.poll(() => state.searchRequests.length).toBe(1)
  expect(state.searchRequests[0]).toMatchObject({
    query: '分页字段', current: 1, size: 20, includeAllStatuses: true
  })
  await expect(table).not.toContainText('pagination_field_021')
  const page1 = await visibleFieldNames(page)
  expect(page1).toHaveLength(20)

  const pagination = page.getByTestId(stableTestIds.fields.pagination)
  await pagination.locator('button.btn-next').click()
  await expect(table).toContainText('pagination_field_021')
  await expect(table).not.toContainText('pagination_field_001')
  expect(state.searchRequests.at(-1)).toMatchObject({ query: '分页字段', current: 2, size: 20 })
  const page2 = await visibleFieldNames(page)
  expect(page2).toHaveLength(20)

  await pagination.locator('button.btn-next').click()
  await expect(table).toContainText('pagination_field_041')
  await expect(table).not.toContainText('pagination_field_021')
  expect(state.searchRequests.at(-1)).toMatchObject({ query: '分页字段', current: 3, size: 20 })
  const page3 = await visibleFieldNames(page)
  expect(page3).toHaveLength(20)

  await pagination.locator('button.btn-next').click()
  await expect(table).toContainText('pagination_field_061')
  await expect(table).not.toContainText('pagination_field_041')
  expect(state.searchRequests.at(-1)).toMatchObject({ query: '分页字段', current: 4, size: 20 })
  const page4 = await visibleFieldNames(page)
  expect(page4).toHaveLength(5)

  const allPages = [page1, page2, page3, page4]
  for (let left = 0; left < allPages.length; left += 1) {
    for (let right = left + 1; right < allPages.length; right += 1) {
      expect(allPages[left].filter((name) => allPages[right].includes(name))).toEqual([])
    }
  }
  expect(new Set(allPages.flat())).toEqual(new Set(paginatedFields.map((field) => field.name)))

  expect(requestCount(state.paths, '/api/fields/groups')).toBe(1)
  expect(requestCount(state.paths, '/api/domains')).toBe(1)
  expect(requestCount(state.paths, '/api/field-semantics')).toBe(1)
  expect(state.fullCatalogRequests).toBe(0)

  await page.getByRole('button', { name: '合并字段 pagination_field_061' }).click()
  await expect.poll(() => state.fullCatalogRequests).toBe(1)
  await page.keyboard.press('Escape')
  await page.getByRole('button', { name: '合并字段 pagination_field_061' }).click()
  await expect.poll(() => state.fullCatalogRequests).toBe(1)
})

test('字段搜索超过 600ms 时显示可访问慢状态并在完成后清除', async ({ page }) => {
  await openFieldLibrary(page)
  const input = controlInput(page, stableTestIds.fields.searchInput)
  const slowState = page.getByTestId(stableTestIds.fields.slowState)

  await input.fill('慢查询')
  await expect(slowState).toBeVisible({ timeout: 1_200 })
  await expect(slowState).toContainText('正在加载当前页')
  await expect(page.getByTestId(stableTestIds.fields.table)).toContainText('slow_response_field')
  await expect(slowState).toBeHidden()
})

test('较旧搜索响应晚到时不会覆盖较新的字段结果', async ({ page }) => {
  await openFieldLibrary(page)
  const input = controlInput(page, stableTestIds.fields.searchInput)
  const table = page.getByTestId(stableTestIds.fields.table)

  const staleRequest = page.waitForRequest((request) => {
    const url = new URL(request.url())
    return url.pathname === '/api/fields/search' && url.searchParams.get('query') === '旧查询'
  })
  await input.fill('旧查询')
  await staleRequest
  await input.fill('新查询')

  await expect(table).toContainText('fresh_response_field')
  await page.waitForTimeout(900)
  await expect(table).toContainText('fresh_response_field')
  await expect(table).not.toContainText('stale_response_field')
})

test('关键词变化后旧响应即使在新请求发出前返回也不会覆盖当前状态', async ({ page }) => {
  await openFieldLibrary(page)
  const input = controlInput(page, stableTestIds.fields.searchInput)
  const table = page.getByTestId(stableTestIds.fields.table)
  const staleRequest = page.waitForRequest((request) => {
    const url = new URL(request.url())
    return url.pathname === '/api/fields/search' && url.searchParams.get('query') === '窗口旧查询'
  })
  const staleResponse = page.waitForResponse((response) => {
    const url = new URL(response.url())
    return url.pathname === '/api/fields/search' && url.searchParams.get('query') === '窗口旧查询'
  })

  await input.fill('窗口旧查询')
  await staleRequest
  await input.fill('窗口新查询')
  await staleResponse

  // 在新请求的 300ms 防抖窗口内取一次快照，避免重试断言越过窗口后只看到最终新响应。
  await page.waitForTimeout(50)
  const tableTextDuringDebounce = await table.textContent()
  expect(tableTextDuringDebounce).toContain('pagination_field_001')
  expect(tableTextDuringDebounce).not.toContain('stale_response_field')
  await expect(table).toContainText('fresh_response_field')
})

test('显式刷新元数据后立即搜索不会丢失较晚返回的元数据', async ({ page }) => {
  const state = await openFieldLibrary(page)
  state.delayNextMetadata = true
  await page.getByRole('button', { name: '刷新' }).click()
  await controlInput(page, stableTestIds.fields.searchInput).fill('刷新竞态')

  await expect(page.getByRole('button', { name: '分类：refreshed，1 个字段' })).toBeVisible()
  expect(state.groupRequests).toBe(2)
})

test('移动端字段库保持可用宽度且导航、状态行、表格和分页不重叠', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await openFieldLibrary(page)

  const menuButton = page.getByRole('button', { name: '打开主导航' })
  await expect(menuButton).toBeVisible()
  await menuButton.click()
  const navigation = page.locator('#primary-navigation')
  await expect(navigation).not.toHaveAttribute('aria-hidden', 'true')
  const closeButton = navigation.getByRole('button', { name: '关闭主导航' })
  await expect(closeButton).toBeFocused()
  await expect(page.locator('.app-content')).toHaveAttribute('inert', '')
  await expect(page.locator('.skip-link')).toHaveAttribute('inert', '')
  await expect(page.locator('.skip-link')).toHaveAttribute('aria-hidden', 'true')
  await expect(page.locator('.mobile-nav-backdrop')).toHaveAttribute('aria-hidden', 'true')
  expect(await page.locator('.mobile-nav-backdrop').evaluate((element) => element.tagName)).toBe('DIV')
  const outsideFocusable = await page.evaluate(() => {
    const navigationElement = document.querySelector('#primary-navigation')
    return Array.from(document.querySelectorAll<HTMLElement>(
      'a[href], button, input, select, textarea, [tabindex]'
    )).filter((element) =>
      !navigationElement?.contains(element)
      && !element.closest('[inert]')
      && !element.hasAttribute('disabled')
      && element.tabIndex >= 0
      && element.offsetParent !== null
    ).map((element) => element.getAttribute('aria-label') || element.textContent?.trim() || element.tagName)
  })
  expect(outsideFocusable).toEqual([])
  await page.keyboard.press('Tab')
  expect(await page.evaluate(() => document.activeElement?.closest('#primary-navigation') !== null)).toBe(true)
  await navigation.getByRole('menuitem', { name: '标准字段库' }).click()
  await expect(navigation).toHaveAttribute('aria-hidden', 'true')
  await expect(menuButton).toBeFocused()

  const layout = await page.evaluate(() => {
    const rect = (selector: string) => {
      const element = document.querySelector(selector)
      if (!element) {
        return null
      }
      const box = element.getBoundingClientRect()
      return { top: box.top, bottom: box.bottom, left: box.left, right: box.right, width: box.width }
    }
    return {
      clientWidth: document.documentElement.clientWidth,
      scrollWidth: document.documentElement.scrollWidth,
      slowState: rect('[data-testid="fields.slowState"]'),
      table: rect('[data-testid="fields.table"]'),
      pagination: rect('[data-testid="fields.pagination"]'),
      tableRegion: rect('.field-table-region')
    }
  })

  expect(layout.scrollWidth).toBeLessThanOrEqual(layout.clientWidth)
  expect(layout.tableRegion?.width).toBeGreaterThan(280)
  expect(layout.slowState?.bottom).toBeLessThanOrEqual(layout.table?.top ?? 0)
  expect(layout.table?.bottom).toBeLessThanOrEqual(layout.pagination?.top ?? 0)
})

async function openFieldLibrary(page: Page) {
  const harness = await installDataSpecRouteHarness(page)
  const state = await installPaginationRoutes(page)
  const projects = new ProjectListPage(page)
  await projects.goto()
  await projects.createAndSelectProject(harness.project)

  const fields = new FieldLibraryPage(page)
  await fields.goto()
  await fields.expectSearchResult('pagination_field_001')
  return state
}

async function installPaginationRoutes(page: Page) {
  const state = {
    paths: [] as string[],
    searchRequests: [] as Array<{
      query: string, current: number, size: number, includeAllStatuses: boolean
    }>,
    fullCatalogRequests: 0,
    groupRequests: 0,
    delayNextMetadata: false
  }

  page.on('request', (request) => {
    state.paths.push(new URL(request.url()).pathname)
  })

  await page.route('**/api/fields**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (request.method() !== 'GET') {
      await route.fallback()
      return
    }
    if (url.pathname === '/api/fields/all') {
      state.fullCatalogRequests += 1
      await ok(route, paginatedFields)
      return
    }
    if (url.pathname === '/api/fields/groups') {
      state.groupRequests += 1
      if (state.delayNextMetadata) {
        state.delayNextMetadata = false
        await delay(800)
        await ok(route, {
          totalFieldCount: paginatedFields.length,
          groups: [{ groupType: 'category', groupKey: 'refreshed', fieldCount: 1 }]
        })
        return
      }
      await route.fallback()
      return
    }
    if (url.pathname === '/api/fields') {
      const current = positiveInt(url.searchParams.get('current'), 1)
      const size = positiveInt(url.searchParams.get('size'), 20)
      await ok(route, fieldPage(paginatedFields, current, size))
      return
    }
    if (url.pathname === '/api/fields/search') {
      const query = url.searchParams.get('query') ?? ''
      const current = positiveInt(url.searchParams.get('current'), 1)
      const size = positiveInt(url.searchParams.get('size'), 20)
      const includeAllStatuses = url.searchParams.get('includeAllStatuses') === 'true'
      state.searchRequests.push({ query, current, size, includeAllStatuses })
      const resultFields = query === '旧查询'
        ? [staleField]
        : query === '窗口旧查询'
          ? [staleField]
        : query === '新查询'
          ? [freshField]
          : query === '窗口新查询'
            ? [freshField]
          : query === '慢查询'
            ? [slowField]
            : paginatedFields
      if (query === '旧查询') {
        await delay(1_100)
      } else if (query === '窗口旧查询') {
        await delay(120)
      } else if (query === '慢查询') {
        await delay(850)
      } else if (query === '新查询' || query === '窗口新查询') {
        await delay(40)
      }
      await ok(route, fieldSearchResult(resultFields, current, size))
      return
    }
    await route.fallback()
  })

  return state
}

function fieldSearchResult(items: typeof paginatedFields, current: number, size: number) {
  const page = fieldPage(items, current, size)
  return {
    summary: {
      totalCandidates: items.length,
      matchedCount: items.length,
      returnedCount: page.records.length,
      truncated: items.length > page.records.length,
      appliedFilters: { current, size },
      hints: []
    },
    items: page.records.map((field) => ({
      field,
      score: 100,
      matchReasons: ['E2E 确定性字段命中'],
      evidence: []
    })),
    nextActions: [],
    page: {
      current,
      size,
      total: items.length,
      pages: page.pages,
      hasPrevious: current > 1,
      hasNext: current < page.pages
    }
  }
}

function fieldPage<T>(items: T[], current: number, size: number) {
  const start = (current - 1) * size
  return {
    records: items.slice(start, start + size),
    total: items.length,
    current,
    size,
    pages: items.length === 0 ? 0 : Math.ceil(items.length / size)
  }
}

function positiveInt(value: string | null, fallback: number) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

function requestCount(paths: string[], path: string) {
  return paths.filter((item) => item === path).length
}

async function visibleFieldNames(page: Page) {
  return page.getByTestId(stableTestIds.fields.table).locator('.field-name-cell').allTextContents()
}

async function ok(route: Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data })
  })
}

function delay(milliseconds: number) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}
