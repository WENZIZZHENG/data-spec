import type { AxiosRequestConfig } from 'axios'
import request from '@/api/request'
import type { paths } from '@/api/schema'
import { renderApiPath } from '@/api/typedPath'

type HttpMethod = 'get' | 'post' | 'put' | 'delete' | 'patch'
type ApiPath = keyof paths
type OperationOf<Path extends ApiPath, Method extends HttpMethod> =
  Method extends keyof paths[Path] ? NonNullable<paths[Path][Method]> : never
type PathsWithMethod<Method extends HttpMethod> = {
  [Path in ApiPath]: OperationOf<Path, Method> extends never ? never : Path
}[ApiPath]
type JsonContent<Content> = Content extends { '*/*': infer Body }
  ? Body
  : Content extends { 'application/json': infer Body }
    ? Body
    : unknown
type SuccessEnvelope<Operation> = Operation extends {
  responses: { 200: { content: infer Content } }
} ? JsonContent<Content> : unknown
type UnwrapData<Envelope> = Envelope extends { data?: infer Data } ? Data : Envelope

/**
 * 从 OpenAPI `paths` 推导指定 path/method 成功响应里的业务 data 类型。
 */
export type ApiResponse<Path extends ApiPath, Method extends HttpMethod> =
  UnwrapData<SuccessEnvelope<OperationOf<Path, Method>>>

type ApiQuery<Path extends ApiPath, Method extends HttpMethod> =
  OperationOf<Path, Method> extends { parameters: { query?: infer Query } } ? Query : never
type ApiPathParams<Path extends ApiPath, Method extends HttpMethod> =
  OperationOf<Path, Method> extends { parameters: { path: infer Params } } ? Params : never
type ApiRequestBody<Path extends ApiPath, Method extends HttpMethod> =
  OperationOf<Path, Method> extends { requestBody: { content: infer Content } } ? JsonContent<Content> : never
type PathsWithRequestBody<Method extends HttpMethod> = {
  [Path in PathsWithMethod<Method>]: [ApiRequestBody<Path, Method>] extends [never] ? never : Path
}[PathsWithMethod<Method>]
type PathsWithoutRequestBody<Method extends HttpMethod> = Exclude<PathsWithMethod<Method>, PathsWithRequestBody<Method>>
type UntypedRequestOptions = Omit<AxiosRequestConfig, 'params'> & {
  params?: unknown
  path?: unknown
}

/**
 * typed API helper 的请求选项。`path` 只用于替换 OpenAPI `{id}` 这类路径参数，
 * `params` 继续传给 Axios query string，保持现有拦截器、token 和错误处理行为。
 */
export type TypedRequestOptions<Path extends ApiPath, Method extends HttpMethod> =
  Omit<AxiosRequestConfig, 'params'> & {
    params?: ApiQuery<Path, Method>
    path?: ApiPathParams<Path, Method>
  }

export { renderApiPath }

/**
 * 使用 OpenAPI `paths` 约束 GET path、query 参数和返回 data 类型。
 */
export function typedGet<Path extends PathsWithMethod<'get'>>(
  path: Path,
  options: TypedRequestOptions<Path, 'get'> = {}
) {
  const { path: pathParams, ...config } = options
  return request.get<unknown, ApiResponse<Path, 'get'>>(renderApiPath(path, asRenderablePathParams(pathParams)), config)
}

/**
 * 使用 OpenAPI `paths` 约束 POST path、body、query 参数和返回 data 类型。
 */
export function typedPost<Path extends PathsWithRequestBody<'post'>>(
  path: Path,
  data: ApiRequestBody<Path, 'post'>,
  options?: TypedRequestOptions<Path, 'post'>
): Promise<ApiResponse<Path, 'post'>>

export function typedPost<Path extends PathsWithoutRequestBody<'post'>>(
  path: Path,
  data?: undefined,
  options?: TypedRequestOptions<Path, 'post'>
): Promise<ApiResponse<Path, 'post'>>

export function typedPost(
  path: PathsWithMethod<'post'>,
  data?: unknown,
  options: UntypedRequestOptions = {}
) {
  const { path: pathParams, ...config } = options
  return request.post(renderApiPath(path, asRenderablePathParams(pathParams)), data, config)
}

function asRenderablePathParams(params: unknown) {
  return params as Record<string, string | number | boolean> | undefined
}
