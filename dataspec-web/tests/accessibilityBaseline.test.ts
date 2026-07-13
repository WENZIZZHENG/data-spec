import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

function readSource(relativePath: string) {
  try {
    return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
      return ''
    }
    throw error
  }
}

function assertContains(source: string, snippets: string[], context: string) {
  for (const snippet of snippets) {
    assert.ok(source.includes(snippet), `${context} should include ${snippet}`)
  }
}

test('keeps app shell keyboard and landmark accessibility baseline wired', () => {
  const app = readSource('src/App.vue')
  const styles = readSource('src/styles/main.css')
  const focusReturn = readSource('src/composables/useDialogFocusReturn.ts')

  assertContains(app, [
    'class="skip-link"',
    'href="#main-content"',
    'role="navigation"',
    'aria-label="主导航"',
    'aria-label="打开主导航"',
    'aria-label="关闭主导航"',
    'aria-controls="primary-navigation"',
    ':aria-expanded="mobileNavOpen"',
    ':aria-hidden="isMobile && mobileNavOpen ? \'true\' : undefined"',
    ':inert="isMobile && !mobileNavOpen ? true : undefined"',
    ':inert="isMobile && mobileNavOpen ? true : undefined"',
    'class="mobile-nav-backdrop"',
    'aria-hidden="true"',
    'trapMobileNavigationFocus',
    "document.getElementById('mobile-nav-close')?.focus",
    'void closeMobileNavigation()',
    "event.key === 'Escape' && mobileNavOpen.value",
    'id="main-content"',
    'role="main"',
    'tabindex="-1"',
    'focusMainContent',
    'aria-keyshortcuts="Control+K Meta+K"',
    'aria-label="当前项目"',
    'aria-label="退出 API Token 登录"',
    'useDialogFocusReturn'
  ], 'App.vue accessibility shell')

  assertContains(styles, [
    '.skip-link',
    '.skip-link:focus',
    ':focus-visible'
  ], 'global accessibility styles')

  assertContains(focusReturn, [
    'export function useDialogFocusReturn',
    'preventScroll: true',
    'document.contains'
  ], 'dialog focus return composable')
})

test('keeps core workflow controls discoverable by keyboard and automation', () => {
  const projectList = readSource('src/views/ProjectList.vue')
  const sqlLint = readSource('src/views/SqlLint.vue')
  const fieldLibrary = readSource('src/views/FieldLibrary.vue')
  const reverseImport = readSource('src/views/ReverseImport.vue')
  const commandPalette = readSource('src/components/CommandPaletteDialog.vue')

  assertContains(projectList, [
    'projectActionLabel',
    ':aria-label="projectActionLabel(row, \'选择\')"',
    ':aria-label="projectActionLabel(row, \'删除\')"',
    'useDialogFocusReturn'
  ], 'ProjectList action accessibility')

  assertContains(sqlLint, [
    "ariaLabel: 'SQL 编辑器'",
    'aria-label="执行校验 SQL"',
    'aria-label="查看详情 SQL 检查记录"',
    'aria-label="复制修正 SQL"',
    'aria-label="AI 模式"',
    'aria-label="修复模式"',
    'aria-label="最高风险"'
  ], 'SqlLint accessibility controls')

  assertContains(fieldLibrary, [
    'aria-label="筛选标准字段"',
    'aria-label="筛选字段状态"',
    ':aria-pressed="activeGroupKey === group.optionKey"',
    'fieldActionLabel',
    ':aria-label="fieldActionLabel(row, \'编辑\')"',
    ':aria-label="fieldActionLabel(row, \'删除\')"'
  ], 'FieldLibrary accessibility controls')

  assertContains(reverseImport, [
    'aria-label="SQL DDL 输入"',
    'aria-label="数据库表筛选"',
    'aria-label="刷新连接预设"',
    'aria-label="生成预览 反向导入"',
    'aria-label="加载表 数据库"',
    'aria-label="浏览元数据 数据库"'
  ], 'ReverseImport accessibility controls')

  assertContains(commandPalette, [
    'aria-label="命令搜索"',
    ':aria-label="commandItemLabel(item)"',
    ':disabled="item.disabled"',
    'useDialogFocusReturn'
  ], 'CommandPalette accessibility controls')
})
