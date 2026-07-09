import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  aiActionNames,
  reverseImportTableOptionTestId,
  dataTestIdPolicy,
  stableTestIds
} from '../src/utils/stableTestIds.ts'

function collectIds(value: unknown): string[] {
  if (typeof value === 'string') {
    return [value]
  }
  if (!value || typeof value !== 'object') {
    return []
  }
  return Object.values(value as Record<string, unknown>).flatMap(collectIds)
}

test('defines shared stable selectors for high-frequency browser workflows', () => {
  assert.equal(dataTestIdPolicy.attribute, 'data-testid')
  assert.equal(dataTestIdPolicy.scope, 'core-e2e-and-ai-browser-automation')
  assert.equal(aiActionNames.projects.createAndSelectProject, 'projects.createAndSelectProject')
  assert.equal(aiActionNames.sqlLint.runAndOpenRecord, 'sqlLint.runAndOpenRecord')

  const requiredIds = [
    stableTestIds.projects.page,
    stableTestIds.projects.newProjectButton,
    stableTestIds.projects.saveProjectButton,
    stableTestIds.sqlLint.page,
    stableTestIds.sqlLint.runButton,
    stableTestIds.sqlLint.fixedSqlPanel,
    stableTestIds.sqlLint.historyToggle,
    stableTestIds.reverseImport.page,
    stableTestIds.reverseImport.databaseModeTab,
    stableTestIds.reverseImport.databaseNameInput,
    stableTestIds.reverseImport.metadataBrowserPanel,
    stableTestIds.reverseImport.fieldCandidatesTab,
    stableTestIds.fields.page,
    stableTestIds.fields.searchInput,
    stableTestIds.aiContext.page,
    stableTestIds.aiContext.fieldCatalogTab,
    stableTestIds.aiContext.rulesYamlTab,
    stableTestIds.aiContext.previewTabs
  ]

  for (const id of requiredIds) {
    assert.match(id, /^[a-z]+(?:[A-Z][a-z]+)*\.[a-z][a-zA-Z0-9]*$/)
  }

  const allIds = collectIds(stableTestIds)
  assert.equal(new Set(allIds).size, allIds.length, 'stable selector ids should be unique')
  assert.equal(reverseImportTableOptionTestId('public', 'user_order'), 'reverseImport.tableOptionPublicUserOrder')
})
