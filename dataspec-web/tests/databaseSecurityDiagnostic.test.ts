import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  databaseSecuritySummary,
  readOnlyLabel,
  securityRiskLabel,
  securityRiskTagType,
  writeRiskLabel
} from '../src/utils/databaseSecurityDiagnostic.ts'

test('formats database security risk labels and tag types', () => {
  assert.equal(securityRiskLabel('SAFE'), '只读安全')
  assert.equal(securityRiskLabel('WARNING'), '需要确认')
  assert.equal(securityRiskLabel('DANGER'), '高权限风险')
  assert.equal(securityRiskLabel('UNKNOWN'), '未知')
  assert.equal(securityRiskTagType('SAFE'), 'success')
  assert.equal(securityRiskTagType('WARNING'), 'warning')
  assert.equal(securityRiskTagType('DANGER'), 'danger')
  assert.equal(securityRiskTagType('UNKNOWN'), 'info')
})

test('formats readonly and write risk labels without exposing credentials', () => {
  assert.equal(readOnlyLabel(true), '只读：是')
  assert.equal(readOnlyLabel(false), '只读：未确认')
  assert.equal(readOnlyLabel(undefined), '只读：未知')
  assert.equal(writeRiskLabel(true), '写风险：有')
  assert.equal(writeRiskLabel(false), '写风险：未发现')
  assert.equal(writeRiskLabel(undefined), '写风险：未知')

  const summary = databaseSecuritySummary({
    databaseType: 'POSTGRESQL',
    currentUser: 'dataspec_ro',
    riskLevel: 'SAFE',
    accessibleTableCount: 12,
    recommendedSql: ['GRANT SELECT ON ALL TABLES IN SCHEMA public TO dataspec_ro;']
  })

  assert.equal(summary, 'POSTGRESQL · dataspec_ro · 只读安全 · 12 张表')
  assert.equal(summary.includes('password'), false)
  assert.equal(summary.includes('jdbc:'), false)
})
