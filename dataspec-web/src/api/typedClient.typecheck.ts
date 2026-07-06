import { typedPost } from '@/api/typedClient'

/**
 * typed client 的编译期契约样例，只由 vue-tsc 校验，不参与运行时调用。
 */
function assertTypedPostContracts() {
  typedPost('/api/projects', { name: '类型测试项目' })

  // @ts-expect-error OpenAPI 声明 requestBody 的 POST 必须显式传入 body。
  typedPost('/api/projects')
}

void assertTypedPostContracts
