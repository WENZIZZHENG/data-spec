import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { expect, test } from '@playwright/test'
import { AiContextPage } from './pages/AiContextPage'
import { FieldLibraryPage } from './pages/FieldLibraryPage'
import { ProjectListPage } from './pages/ProjectListPage'
import { ReverseImportPage } from './pages/ReverseImportPage'
import { SqlLintPage } from './pages/SqlLintPage'
import { installDataSpecRouteHarness } from './support/routeHarness'

test.afterEach(async ({ page }, testInfo) => {
  if (testInfo.status === testInfo.expectedStatus) {
    return
  }
  const contextDir = path.join(testInfo.outputDir, 'failure-context')
  await mkdir(contextDir, { recursive: true })
  await writeFile(path.join(contextDir, 'current-url.txt'), page.url(), 'utf8')
})

test('page objects expose stable actions for core browser workflows', async ({ page }) => {
  const harness = await installDataSpecRouteHarness(page)

  const projects = new ProjectListPage(page)
  await test.step(projects.actionNames.createAndSelectProject, async () => {
    await projects.goto()
    await projects.createAndSelectProject(harness.project)
  })

  const sqlLint = new SqlLintPage(page)
  await test.step(sqlLint.actionNames.runAndOpenRecord, async () => {
    await sqlLint.gotoDemoLint()
    await sqlLint.runLint()
    await sqlLint.openLatestRecordDetail()
    await sqlLint.expectRecordDialogContains('历史 Context 导出命令')
  })

  const reverseImport = new ReverseImportPage(page)
  await test.step(reverseImport.actionNames.browseDatabaseMetadata, async () => {
    await reverseImport.goto()
    await reverseImport.openDatabaseMode()
    await reverseImport.fillConnection({ databaseName: 'sales_demo', schemaName: 'public', username: 'readonly_user' })
    await reverseImport.loadTables()
    await reverseImport.selectTable({ schemaName: 'public', tableName: 'user_order' })
    await reverseImport.browseMetadata()
    await reverseImport.expectMetadataField('public.user_order.buyer_mobile')
  })

  await test.step(reverseImport.actionNames.generatePreview, async () => {
    await reverseImport.generatePreview()
    await reverseImport.openFieldCandidates()
    await reverseImport.expectCandidateRow(/public\.user_order\.buyer_mobile.*买家手机号/)
  })

  const fields = new FieldLibraryPage(page)
  await test.step(fields.actionNames.searchField, async () => {
    await fields.goto()
    await fields.search('手机号')
    await fields.expectSearchResult('buyer_mobile')
    await fields.expectSearchResultHidden('order_amount')
  })

  const aiContext = new AiContextPage(page)
  await test.step(aiContext.actionNames.inspectPreviewTabs, async () => {
    await aiContext.goto()
    await aiContext.expectDatabaseRules('字段命名必须使用 snake_case')
    await aiContext.expectFieldCatalog('"buyer_mobile"')
    await aiContext.expectRulesYaml('required_columns')
  })

  expect(harness.unhandledApiRequests).toEqual([])
})
