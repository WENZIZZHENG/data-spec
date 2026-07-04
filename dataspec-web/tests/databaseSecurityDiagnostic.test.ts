import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  capabilitySupportLabel,
  capabilitySupportTagType,
  connectionStatusLabel,
  connectionStatusTagType,
  databaseHealthSummary,
  databaseSecuritySummary,
  failureCategoryLabel,
  metadataReadableLabel,
  readOnlyLabel,
  retryableLabel,
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

test('formats connection health and capability labels for AI-facing diagnostics', () => {
  assert.equal(connectionStatusLabel('CONNECTED'), '连接可用')
  assert.equal(connectionStatusTagType('CONNECTED'), 'success')
  assert.equal(connectionStatusLabel('FAILED'), '连接失败')
  assert.equal(connectionStatusTagType('FAILED'), 'danger')
  assert.equal(failureCategoryLabel('AUTHENTICATION'), '认证失败')
  assert.equal(failureCategoryLabel('UNSUPPORTED_DIALECT'), '方言不支持')
  assert.equal(capabilitySupportLabel('SUPPORTED'), '支持')
  assert.equal(capabilitySupportTagType('SUPPORTED'), 'success')
  assert.equal(capabilitySupportLabel('UNSUPPORTED'), '不支持')
  assert.equal(capabilitySupportTagType('UNSUPPORTED'), 'danger')
  assert.equal(metadataReadableLabel(true), 'Metadata：可读')
  assert.equal(retryableLabel(false), '不建议重试')

  const connectedSummary = databaseHealthSummary({
    connectionStatus: 'CONNECTED',
    latencyMs: 32,
    databaseProduct: 'PostgreSQL',
    version: '15.6',
    dialect: 'POSTGRESQL',
    capability: {
      dialect: 'POSTGRESQL',
      schemaSupport: 'SUPPORTED',
      commentSupport: 'SUPPORTED',
      indexSupport: 'SUPPORTED',
      metadataReadable: true
    }
  })
  assert.equal(connectedSummary, '连接可用 · POSTGRESQL / PostgreSQL / 15.6 · 32ms')

  const failedSummary = databaseHealthSummary({
    connectionStatus: 'FAILED',
    latencyMs: 8,
    failureCategory: 'AUTHENTICATION',
    message: '密码错误，请检查 [REDACTED]'
  })
  assert.equal(failedSummary, '连接失败 · 认证失败 · 8ms')
  assert.equal(failedSummary.includes('password'), false)
  assert.equal(failedSummary.includes('jdbc:'), false)
})
