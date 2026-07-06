import assert from 'node:assert/strict'
import { test } from 'node:test'
import { normalizeRequestError, useRequestState } from '../src/composables/useRequestState.ts'

test('tracks successful request and refresh metadata', async () => {
  const state = useRequestState<number>()

  const result = await state.run(async () => 42)

  assert.equal(result, 42)
  assert.equal(state.data.value, 42)
  assert.equal(state.loading.value, false)
  assert.equal(state.errorMessage.value, '')
  assert.match(state.lastUpdatedAt.value, /^\d{4}-\d{2}-\d{2}T/)
})

test('captures DataSpec error detail without losing suggested action', async () => {
  const state = useRequestState<number>()
  const error = Object.assign(new Error('后端未启动'), {
    dataspecError: {
      code: 'SERVER_UNAVAILABLE',
      category: 'NETWORK',
      retryable: true,
      suggestedAction: '启动 dataspec-server 后重试。',
      docsRef: 'README.md#启动'
    }
  })

  await assert.rejects(() => state.run(async () => {
    throw error
  }), /后端未启动/)

  assert.equal(state.loading.value, false)
  assert.equal(state.errorMessage.value, '后端未启动')
  assert.equal(state.suggestedAction.value, '启动 dataspec-server 后重试。')
  assert.deepEqual(state.nextActions.value, ['启动 dataspec-server 后重试。'])
  assert.equal(state.docsRef.value, 'README.md#启动')
  assert.equal(state.retryable.value, true)
})

test('retries the last failed request and replaces visible state', async () => {
  const state = useRequestState<string>()
  let attempt = 0
  let shouldFail = true

  await assert.rejects(() => state.run(async () => {
    attempt += 1
    if (shouldFail) {
      shouldFail = false
      throw new Error('第一次失败')
    }
    return 'ok'
  }), /第一次失败/)

  const retryResult = await state.retry()

  assert.equal(retryResult, 'ok')
  assert.equal(attempt, 2)
  assert.equal(state.errorMessage.value, '')
  assert.equal(state.data.value, 'ok')
})

test('normalizes axios response error details', () => {
  const summary = normalizeRequestError({
    response: {
      data: {
        message: '无权访问项目',
        error: {
          code: 'PROJECT_ACCESS_DENIED',
          retryable: false,
          suggestedAction: '切换到 token 授权的项目。'
        }
      }
    }
  })

  assert.equal(summary.message, '无权访问项目')
  assert.equal(summary.retryable, false)
  assert.equal(summary.suggestedAction, '切换到 token 授权的项目。')
  assert.deepEqual(summary.nextActions, ['切换到 token 授权的项目。'])
})

test('normalizes response next actions for shared page error display', async () => {
  const state = useRequestState<number>()
  const error = Object.assign(new Error('数据库连接失败'), {
    dataspecError: {
      code: 'DATABASE_UNAVAILABLE',
      retryable: false,
      suggestedAction: '检查连接配置。',
      nextActions: ['检查连接配置。', '打开连接诊断页查看失败原因。']
    }
  })

  await assert.rejects(() => state.run(async () => {
    throw error
  }), /数据库连接失败/)

  assert.deepEqual(state.nextActions.value, [
    '检查连接配置。',
    '打开连接诊断页查看失败原因。'
  ])
})
