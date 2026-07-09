import { readFileSync, statSync } from 'node:fs'
import path from 'node:path'

const CONFIG_DIR = '.dataspec'
const CONFIG_FILE = 'config.json'

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
    defaultPaths: normalizeDefaultPaths(rawConfig.defaultPaths, configPath)
  }
}

function normalizeProjectId(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const projectId = Number(value)
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
  return server || undefined
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
