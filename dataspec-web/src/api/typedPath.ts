import type { paths } from './schema.ts'

type ApiPath = keyof paths
type PathParamValue = string | number | boolean

/**
 * 把 OpenAPI path 转成当前 Axios baseURL(`/api`) 下使用的相对路径。
 */
export function renderApiPath<Path extends ApiPath>(
  path: Path,
  params: Record<string, PathParamValue> = {}
): string {
  const relativePath = String(path).replace(/^\/api/, '')
  return relativePath.replace(/\{([^}]+)\}/g, (_, key: string) => {
    const value = params[key]
    if (value === undefined || value === null) {
      throw new Error(`缺少路径参数：${key}`)
    }
    return encodeURIComponent(String(value))
  })
}
