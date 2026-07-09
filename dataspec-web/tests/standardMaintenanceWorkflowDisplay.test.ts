import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

function readSource(relativePath: string) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

function assertContains(source: string, snippets: string[], context: string) {
  for (const snippet of snippets) {
    assert.ok(source.includes(snippet), `${context} should include ${snippet}`)
  }
}

test('standard maintenance workflow api and types expose dry-run plan contract', () => {
  const api = readSource('src/api/standardMaintenanceWorkflow.ts')
  const types = readSource('src/types/index.ts')

  assertContains(api, [
    'typedPost',
    "'/api/standard-maintenance/workflows/plan'",
    'generateStandardMaintenanceWorkflowPlan',
    'StandardMaintenanceWorkflowPlanReq',
    'StandardMaintenanceWorkflowPlan'
  ], 'standard maintenance workflow api')

  assertContains(types, [
    'export interface StandardMaintenanceWorkflowPlanReq',
    'sourceType?:',
    'issueCodes?: string[]',
    'coverageStatuses?: string[]',
    'failedTableCount?: number',
    'skippedTableCount?: number',
    'export interface StandardMaintenanceWorkflowPlan',
    'inboxAction?: StandardMaintenanceWorkflowInboxAction',
    'recipeBinding?: StandardMaintenanceWorkflowRecipeBinding',
    'dryRunSteps?: StandardMaintenanceWorkflowStep[]',
    'executionState?: StandardMaintenanceWorkflowExecutionState',
    'evidenceLinks?: StandardMaintenanceWorkflowEvidenceLink[]',
    'nextActions?: StandardMaintenanceWorkflowNextAction[]'
  ], 'standard maintenance workflow types')
})

test('standard maintenance workflow panel renders steps evidence and confirmation boundary', () => {
  const component = readSource('src/components/StandardMaintenanceWorkflowPlanPanel.vue')

  assertContains(component, [
    'workflowPlan.inboxAction',
    'workflowPlan.recipeBinding',
    'workflowPlan.dryRunSteps',
    'workflowPlan.evidenceLinks',
    'workflowPlan.nextActions',
    'requiresConfirmation',
    'executionState',
    'undoHint'
  ], 'StandardMaintenanceWorkflowPlanPanel')
})

test('candidate quality and coverage pages can generate workflow dry-run plans', () => {
  const candidate = readSource('src/views/StandardCandidate.vue')
  const quality = readSource('src/views/FieldQuality.vue')
  const coverage = readSource('src/views/FieldCoverage.vue')

  assertContains(candidate, [
    "generateStandardMaintenanceWorkflowPlan",
    "sourceType: 'STANDARD_CANDIDATE'",
    'maintenanceWorkflowPlan',
    '<StandardMaintenanceWorkflowPlanPanel'
  ], 'StandardCandidate workflow entry')

  assertContains(quality, [
    "generateStandardMaintenanceWorkflowPlan",
    "sourceType: 'FIELD_QUALITY'",
    'issueCodes: maintenanceIssueCodes',
    '<StandardMaintenanceWorkflowPlanPanel'
  ], 'FieldQuality workflow entry')

  assertContains(coverage, [
    "generateStandardMaintenanceWorkflowPlan",
    "sourceType: 'FIELD_COVERAGE'",
    'coverageStatuses:',
    'sourceStatus: reportSnapshot.inputStatus',
    'failedTableCount: reportSnapshot.failedTableCount',
    'skippedTableCount: reportSnapshot.skippedTableCount',
    '<StandardMaintenanceWorkflowPlanPanel'
  ], 'FieldCoverage workflow entry')
})
