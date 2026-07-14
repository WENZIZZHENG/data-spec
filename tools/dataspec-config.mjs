import { readFileSync, statSync } from 'node:fs'
import path from 'node:path'

const CONFIG_DIR = '.dataspec'
const CONFIG_FILE = 'config.json'

/** 当前 CLI 可理解的 `.dataspec/config.json` 契约版本。 */
export const DATASPEC_CONFIG_SCHEMA_VERSION = 1

/** `dataspec init` 写入业务仓库的本地 schema 文件名。 */
export const DATASPEC_CONFIG_SCHEMA_FILE = 'config.schema.json'

/** DataSpec tools 目录内随 CLI 分发的 canonical schema 文件名。 */
export const DATASPEC_CONFIG_SCHEMA_SOURCE_FILE = 'dataspec-config.schema.json'

/** config 中用于离线编辑器提示的稳定相对引用。 */
export const DATASPEC_CONFIG_SCHEMA_REF = `./${DATASPEC_CONFIG_SCHEMA_FILE}`

export function findDataSpecConfig(startDir = process.cwd()) {
  let currentDir = path.resolve(startDir)
  while (true) {
    const candidate = path.join(currentDir, CONFIG_DIR, CONFIG_FILE)
    try {
      if (statSync(candidate).isFile()) {
        return candidate
      }
    } catch (error) {
      if (error.code !== 'ENOENT' && error.code !== 'ENOTDIR') {
        throw error
      }
    }

    const parentDir = path.dirname(currentDir)
    if (parentDir === currentDir) {
      return null
    }
    currentDir = parentDir
  }
}

export function loadDataSpecConfig(startDir = process.cwd()) {
  const configPath = findDataSpecConfig(startDir)
  if (!configPath) {
    return emptyConfig(startDir)
  }

  const rawText = readFileSync(configPath, 'utf8')
  let rawConfig
  try {
    rawConfig = JSON.parse(rawText)
  } catch (error) {
    throw new Error(`无法解析 DataSpec 配置 ${configPath}: ${error.message}`)
  }
  return normalizeDataSpecConfig(rawConfig, configPath)
}

export function resolveDefaultPaths(config) {
  return config.defaultPaths.map((inputPath) => path.resolve(config.rootDir, inputPath))
}

function emptyConfig(startDir) {
  return {
    configPath: null,
    rootDir: path.resolve(startDir),
    projectId: undefined,
    server: undefined,
    apiToken: undefined,
    aiProfile: undefined,
    taskType: undefined,
    securityProfile: undefined,
    schemaRef: undefined,
    configVersion: undefined,
    defaultPaths: []
  }
}

function normalizeDataSpecConfig(rawConfig, configPath) {
  if (!rawConfig || typeof rawConfig !== 'object' || Array.isArray(rawConfig)) {
    throw new Error(`DataSpec 配置必须是 JSON 对象: ${configPath}`)
  }
  const rootDir = path.dirname(path.dirname(configPath))
  return {
    configPath,
    rootDir,
    projectId: normalizeProjectId(rawConfig.projectId),
    server: normalizeServer(rawConfig.server),
    apiToken: normalizeApiToken(rawConfig.apiToken),
    aiProfile: normalizeOptionalString(rawConfig.aiProfile, 'aiProfile', configPath),
    taskType: normalizeOptionalString(rawConfig.taskType, 'taskType', configPath),
    securityProfile: normalizeSecurityProfile(rawConfig.securityProfile, configPath),
    schemaRef: normalizeSchemaRef(rawConfig, configPath),
    configVersion: normalizeConfigVersion(rawConfig.configVersion, configPath),
    defaultPaths: normalizeDefaultPaths(rawConfig.defaultPaths, configPath)
  }
}

function normalizeConfigVersion(value, configPath) {
  if (value === undefined) {
    return undefined
  }
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`DataSpec 配置 configVersion 必须是正整数: ${configPath}`)
  }
  return value
}

function normalizeSchemaRef(rawConfig, configPath) {
  if (!Object.hasOwn(rawConfig, '$schema')) {
    return undefined
  }
  const value = rawConfig.$schema
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(`DataSpec 配置 $schema 必须是非空字符串: ${configPath}`)
  }
  // 保留用户声明原文，使 doctor 能将带空格等非 canonical 关联识别为异常。
  return value
}

function normalizeProjectId(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const normalized = typeof value === 'string' ? value.trim() : value
  if (typeof normalized !== 'number' && (typeof normalized !== 'string' || !/^[1-9]\d*$/.test(normalized))) {
    throw new Error(`无效 DataSpec projectId: ${String(value)}`)
  }
  const projectId = Number(normalized)
  if (!Number.isInteger(projectId) || projectId <= 0) {
    throw new Error(`无效 DataSpec projectId: ${value}`)
  }
  return projectId
}

function normalizeServer(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  if (typeof value !== 'string') {
    throw new Error(`无效 DataSpec server: ${value}`)
  }
  const server = value.replace(/\/+$/, '')
  rejectServerUserinfo(server)
  return server || undefined
}

function rejectServerUserinfo(server) {
  try {
    const parsed = new URL(server)
    if (parsed.username || parsed.password) {
      throw new Error('DataSpec server URL 不能包含用户名或密码')
    }
  } catch (error) {
    if (error.message === 'DataSpec server URL 不能包含用户名或密码') {
      throw error
    }
    // 既有 loader 允许非 URL server 字符串；本变更只收紧可解析 URL 的 userinfo。
  }
}

function normalizeApiToken(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  if (typeof value !== 'string') {
    throw new Error(`无效 DataSpec apiToken: ${value}`)
  }
  const token = value.trim()
  return token || undefined
}

function normalizeOptionalString(value, fieldName, configPath) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  if (typeof value !== 'string') {
    throw new Error(`DataSpec 配置 ${fieldName} 必须是字符串: ${configPath}`)
  }
  const normalized = value.trim()
  return normalized || undefined
}

function normalizeDefaultPaths(value, configPath) {
  if (value === undefined || value === null) {
    return []
  }
  if (!Array.isArray(value)) {
    throw new Error(`DataSpec 配置 defaultPaths 必须是字符串数组: ${configPath}`)
  }
  return value
    .map((inputPath) => {
      if (typeof inputPath !== 'string') {
        throw new Error(`DataSpec 配置 defaultPaths 只能包含字符串: ${configPath}`)
      }
      return inputPath.trim()
    })
    .filter(Boolean)
}

function normalizeSecurityProfile(value, configPath) {
  if (value === undefined || value === null) {
    return undefined
  }
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`DataSpec 配置 securityProfile 必须是 JSON 对象: ${configPath}`)
  }

  const profile = {}
  setOptionalPolicy(profile, 'redactionStrictness', value.redactionStrictness, configPath)
  setOptionalPolicy(profile, 'sensitiveFieldPolicy', value.sensitiveFieldPolicy, configPath)
  setOptionalStringArray(profile, 'allowedAiTools', value.allowedAiTools, configPath)
  setOptionalStringArray(profile, 'neverExportPatterns', value.neverExportPatterns, configPath)
  setOptionalStringArray(profile, 'localOnlyPaths', value.localOnlyPaths, configPath)
  setOptionalPolicy(profile, 'samplePolicy', value.samplePolicy, configPath)
  setOptionalPolicy(profile, 'credentialPolicy', value.credentialPolicy, configPath)

  return Object.keys(profile).length === 0 ? undefined : profile
}

function setOptionalPolicy(target, fieldName, value, configPath) {
  const normalized = normalizeOptionalString(value, `securityProfile.${fieldName}`, configPath)
  if (normalized !== undefined) {
    target[fieldName] = normalized
  }
}

function setOptionalStringArray(target, fieldName, value, configPath) {
  if (value === undefined || value === null) {
    return
  }
  if (!Array.isArray(value)) {
    throw new Error(`DataSpec 配置 securityProfile.${fieldName} 必须是字符串数组: ${configPath}`)
  }
  target[fieldName] = value
    .map((item) => {
      if (typeof item !== 'string') {
        throw new Error(`DataSpec 配置 securityProfile.${fieldName} 只能包含字符串: ${configPath}`)
      }
      return item.trim()
    })
    .filter(Boolean)
}
