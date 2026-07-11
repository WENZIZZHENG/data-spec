import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { renderApiPath } from '../src/api/typedPath.ts'

function readSource(path: string) {
  return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
}

function assertContains(source: string, expected: string[], label: string) {
  for (const snippet of expected) {
    assert.ok(source.includes(snippet), `${label} missing ${snippet}`)
  }
}

test('renders OpenAPI paths to axios base-url relative paths', () => {
  assert.equal(renderApiPath('/api/ai-batches'), '/ai-batches')
  assert.equal(renderApiPath('/api/ai-batches/{id}', { id: 12 }), '/ai-batches/12')
  assert.equal(renderApiPath('/api/fields/{id}/impact', { id: 7 }), '/fields/7/impact')
  assert.equal(renderApiPath('/api/projects/{projectId}/activities', { projectId: 'a/b c' }), '/projects/a%2Fb%20c/activities')
  assert.throws(() => renderApiPath('/api/ai-batches/{id}'), /缺少路径参数：id/)
})

test('keeps typed client wired to generated OpenAPI paths', () => {
  const client = readSource('src/api/typedClient.ts')

  assertContains(client, [
    "import type { paths } from '@/api/schema'",
    'export type ApiResponse',
    'export { renderApiPath }',
    'export function typedGet',
    'export function typedPost'
  ], 'typed api client')
})

test('migrates high-frequency list/detail APIs to typed helpers', () => {
  const aiBatch = readSource('src/api/aiBatch.ts')
  const aiTaskRun = readSource('src/api/aiTaskRun.ts')

  assertContains(aiBatch, [
    "typedGet('/api/ai-batches'",
    "typedGet('/api/ai-batches/{id}'"
  ], 'ai batch api')
  assertContains(aiTaskRun, [
    "typedGet('/api/ai-task-runs'",
    "typedGet('/api/ai-task-runs/{id}'",
    "typedGet('/api/ai-task-runs/recent-failures'"
  ], 'ai task run api')
})

test('keeps table standard API wrappers base-url relative and typed', () => {
  const templateApi = readSource('src/api/template.ts')
  const businessObjectApi = readSource('src/api/businessObject.ts')

  assertContains(templateApi, [
    'import type { Template, TemplateField, TemplateSaveReq }',
    "request.get<unknown, Template[]>('/templates'",
    'export function getTemplate(id: number)',
    "request.get<unknown, TemplateResp>(`/templates/${id}`)",
    'export function createTemplate(data: TemplateSaveReq)',
    "request.post<unknown, TemplateResp>('/templates', data)",
    'export function updateTemplate(id: number, data: TemplateSaveReq)',
    "request.put<unknown, TemplateResp>(`/templates/${id}`, data)"
  ], 'template table standard api')

  assertContains(businessObjectApi, [
    'import type { BusinessObjectRelationSummary, BusinessObjectStandard, BusinessObjectStandardReq }',
    'export function listBusinessObjects(projectId: number)',
    "request.get<unknown, BusinessObjectStandard[]>('/business-objects'",
    'export function getBusinessObject(id: number)',
    "request.get<unknown, BusinessObjectStandard>(`/business-objects/${id}`)",
    'export function createBusinessObject(data: BusinessObjectStandardReq)',
    "request.post<unknown, BusinessObjectStandard>('/business-objects', data)",
    'export function updateBusinessObject(id: number, data: BusinessObjectStandardReq)',
    "request.put<unknown, BusinessObjectStandard>(`/business-objects/${id}`, data)",
    'export function deleteBusinessObject(id: number)',
    "request.delete<unknown, void>(`/business-objects/${id}`)",
    'export function getBusinessObjectRelationSummary(projectId: number)',
    "request.get<unknown, BusinessObjectRelationSummary>('/business-objects/relation-summary'"
  ], 'business object api')
})
