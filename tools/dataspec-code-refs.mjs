import { readdir, readFile, stat } from 'node:fs/promises'
import path from 'node:path'

const SCHEMA_VERSION = 1
const MAX_SCAN_FILE_BYTES = 1024 * 1024
const SKIPPED_DIRECTORIES = new Set([
  '.git',
  '.idea',
  '.vscode',
  'build',
  'dist',
  'node_modules',
  'target'
])
const SQL_EXTENSIONS = new Set(['.sql', '.ddl', '.psql'])
const MODEL_EXTENSIONS = new Set(['.java', '.kt', '.ts', '.tsx', '.js', '.jsx', '.py', '.go', '.cs', '.prisma', '.proto'])
const MAPPER_EXTENSIONS = new Set(['.xml'])
const CONFIG_EXTENSIONS = new Set(['.json', '.yaml', '.yml', '.properties', '.toml', '.ini'])
const TEXT_EXTENSIONS = new Set(['.md', '.txt'])
const SCANNABLE_EXTENSIONS = new Set([
  ...SQL_EXTENSIONS,
  ...MODEL_EXTENSIONS,
  ...MAPPER_EXTENSIONS,
  ...CONFIG_EXTENSIONS,
  ...TEXT_EXTENSIONS
])

/**
 * 构建业务仓库字段引用索引。该函数只读取调用方传入的本地路径，不写入业务仓库。
 */
export async function buildCodeFieldReferenceIndex({
  fieldNames,
  aliases = [],
  scanPaths,
  rootDir,
  outputRootDir = rootDir
}) {
  const fields = normalizeFields(fieldNames, aliases)
  const diagnostics = []
  const scanState = {
    scannedFiles: 0,
    skippedDirectories: [],
    skippedFiles: []
  }
  const references = []

  const resolvedScanPaths = await resolveScanPaths(scanPaths, rootDir, outputRootDir)
  for (const scanPath of resolvedScanPaths) {
    await collectReferences(scanPath, fields, outputRootDir, references, diagnostics, scanState)
  }

  references.sort(compareReference)
  const summary = summarizeReferences(fields, references, scanState)
  const renameRisk = summarizeRenameRisk(references)
  return {
    kind: 'dataspec.code-field-reference-index',
    schemaVersion: SCHEMA_VERSION,
    fields: fields.map((field) => ({
      fieldName: field.fieldName,
      aliases: field.aliases
    })),
    scan: {
      rootDir: '.',
      paths: resolvedScanPaths.map((item) => toPosixPath(path.relative(outputRootDir, item) || '.')),
      skippedDirectories: scanState.skippedDirectories.map((item) => toPosixPath(path.relative(outputRootDir, item) || '.'))
    },
    summary,
    references,
    renameRisk,
    suggestedAction: suggestedAction(renameRisk, references.length),
    diagnostics,
    nextActions: nextActions(renameRisk, references.length)
  }
}

export function formatCodeFieldReferenceIndexText(output) {
  const lines = ['DataSpec Code Field References']
  lines.push(`Fields: ${output.fields.map((field) => field.fieldName).join(', ')}`)
  lines.push(`References: ${output.summary.totalReferences}`)
  lines.push(`Rename risk: ${output.renameRisk}`)
  lines.push(`Scanned files: ${output.summary.scannedFileCount}`)
  if (output.summary.skippedDirectoryCount > 0 || output.summary.skippedFileCount > 0) {
    lines.push(`Skipped: ${output.summary.skippedDirectoryCount} directories, ${output.summary.skippedFileCount} files`)
  }
  if (output.references.length > 0) {
    lines.push('', 'References:')
    for (const item of output.references.slice(0, 20)) {
      lines.push(`- ${item.file}:${item.line}:${item.column} [${item.confidence}] ${item.referenceKind} ${item.matchedText}`)
    }
  }
  if (output.diagnostics.length > 0) {
    lines.push('', 'Diagnostics:')
    for (const item of output.diagnostics) {
      lines.push(`- ${item.code}: ${item.message}`)
    }
  }
  lines.push('', 'Next actions:')
  for (const action of output.nextActions) {
    lines.push(`- ${action}`)
  }
  return `${lines.join('\n')}\n`
}

function normalizeFields(fieldNames, aliases) {
  const normalizedNames = uniqueSorted(fieldNames.map(normalizeName).filter(Boolean))
  if (normalizedNames.length === 0) {
    throw new Error('index-refs 需要至少提供一个 --field')
  }
  const aliasesByField = normalizeAliases(normalizedNames, aliases)
  return normalizedNames.map((fieldName) => ({
    fieldName,
    aliases: aliasesByField.get(fieldName) ?? [],
    terms: uniqueSorted([fieldName, ...(aliasesByField.get(fieldName) ?? [])])
  }))
}

function normalizeName(value) {
  return String(value ?? '').trim()
}

async function resolveScanPaths(scanPaths, rootDir, outputRootDir) {
  const scanRoot = path.resolve(outputRootDir)
  const resolvedPaths = uniqueSorted(scanPaths.map((item) => path.resolve(rootDir, item)))
  for (const resolvedPath of resolvedPaths) {
    if (!isPathInsideOrEqual(resolvedPath, scanRoot)) {
      throw scanPathError(
        'DATASPEC_SCAN_PATH_OUT_OF_SCOPE',
        `扫描路径超出业务仓库范围: ${safeOutputPath(resolvedPath, scanRoot)}`,
        '请传入业务仓库目录内的 --path，或修正 .dataspec/config.json 的 defaultPaths。'
      )
    }
    try {
      await stat(resolvedPath)
    } catch (error) {
      throw scanPathError(
        'DATASPEC_SCAN_PATH_NOT_FOUND',
        `扫描路径不存在: ${safeOutputPath(resolvedPath, scanRoot)}`,
        '请确认 --path 或 defaultPaths 指向存在的文件或目录后重试。'
      )
    }
  }
  return resolvedPaths
}

function normalizeAliases(fieldNames, aliases) {
  const aliasesByField = new Map(fieldNames.map((fieldName) => [fieldName, []]))
  for (const rawAlias of aliases.map(normalizeName).filter(Boolean)) {
    const separatorIndex = rawAlias.indexOf('=')
    if (separatorIndex >= 0) {
      const fieldName = normalizeName(rawAlias.slice(0, separatorIndex))
      const alias = normalizeName(rawAlias.slice(separatorIndex + 1))
      if (!fieldName || !alias) {
        throw new Error('index-refs 的 --alias field=alias 需要同时提供字段名和别名')
      }
      if (!aliasesByField.has(fieldName)) {
        throw new Error(`index-refs 的 --alias 指向了未提供的字段: ${fieldName}`)
      }
      aliasesByField.get(fieldName).push(alias)
      continue
    }

    if (fieldNames.length !== 1) {
      throw new Error('index-refs 多字段扫描时 --alias 需要使用 field=alias 形式，避免别名归属不明确')
    }
    aliasesByField.get(fieldNames[0]).push(rawAlias)
  }

  for (const [fieldName, fieldAliases] of aliasesByField) {
    aliasesByField.set(fieldName, uniqueSorted(fieldAliases))
  }
  return aliasesByField
}

async function collectReferences(inputPath, fields, outputRootDir, references, diagnostics, scanState) {
  let info
  try {
    info = await stat(inputPath)
  } catch (error) {
    diagnostics.push(diagnostic('SCAN_PATH_NOT_FOUND', `扫描路径不存在: ${safeOutputPath(inputPath, outputRootDir)}`))
    return
  }

  if (info.isDirectory()) {
    if (SKIPPED_DIRECTORIES.has(path.basename(inputPath))) {
      scanState.skippedDirectories.push(inputPath)
      return
    }
    const entries = await readdir(inputPath, { withFileTypes: true })
    for (const entry of entries) {
      await collectReferences(path.join(inputPath, entry.name), fields, outputRootDir, references, diagnostics, scanState)
    }
    return
  }

  if (!info.isFile()) {
    return
  }
  if (info.size > MAX_SCAN_FILE_BYTES) {
    scanState.skippedFiles.push(inputPath)
    diagnostics.push(diagnostic('SCAN_FILE_TOO_LARGE', `文件超过扫描上限，已跳过: ${safeOutputPath(inputPath, outputRootDir)}`))
    return
  }
  if (!isScannableFile(inputPath)) {
    return
  }

  const text = await readFile(inputPath, 'utf8').catch((error) => {
    diagnostics.push(diagnostic('SCAN_FILE_READ_FAILED', `无法读取文件，已跳过: ${safeOutputPath(inputPath, outputRootDir)}`))
    return null
  })
  if (text === null) {
    return
  }

  scanState.scannedFiles += 1
  scanFile(inputPath, text, fields, outputRootDir, references)
}

function scanFile(filePath, text, fields, outputRootDir, references) {
  const seen = new Set()
  const lines = text.split(/\r?\n/)
  for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
    const line = lines[lineIndex]
    for (const field of fields) {
      for (const term of field.terms) {
        for (const match of findTermMatches(line, term)) {
          const key = `${field.fieldName}\0${term}\0${lineIndex}\0${match.index}`
          if (seen.has(key)) {
            continue
          }
          seen.add(key)
          const classification = classifyReference(filePath)
          references.push({
            fieldName: field.fieldName,
            matchedText: match.text,
            referenceKind: classification.referenceKind,
            file: safeOutputPath(filePath, outputRootDir),
            line: lineIndex + 1,
            column: match.index + 1,
            confidence: classification.confidence,
            possibleReference: classification.confidence === 'LOW',
            snippet: sanitizeSecretText(line.trim())
          })
        }
      }
    }
  }
}

function findTermMatches(line, term) {
  const escaped = escapeRegExp(term)
  const pattern = new RegExp(`(^|[^A-Za-z0-9_])(${escaped})(?=$|[^A-Za-z0-9_])`, 'giu')
  const matches = []
  let match
  while ((match = pattern.exec(line)) !== null) {
    const prefixLength = match[1].length
    matches.push({
      index: match.index + prefixLength,
      text: match[2]
    })
  }
  return matches
}

function classifyReference(filePath) {
  const extension = path.extname(filePath).toLowerCase()
  const normalizedPath = toPosixPath(filePath).toLowerCase()
  if (SQL_EXTENSIONS.has(extension)) {
    return {
      referenceKind: normalizedPath.includes('migration') || normalizedPath.includes('migrations') ? 'DDL_IDENTIFIER' : 'SQL_IDENTIFIER',
      confidence: 'HIGH'
    }
  }
  if (MAPPER_EXTENSIONS.has(extension)) {
    return { referenceKind: 'MAPPER_REFERENCE', confidence: 'HIGH' }
  }
  if (MODEL_EXTENSIONS.has(extension)) {
    return { referenceKind: 'MODEL_PROPERTY', confidence: 'MEDIUM' }
  }
  if (CONFIG_EXTENSIONS.has(extension)) {
    return { referenceKind: 'CONFIG_KEY', confidence: 'MEDIUM' }
  }
  return { referenceKind: 'TEXT_MENTION', confidence: 'LOW' }
}

function summarizeReferences(fields, references, scanState) {
  return {
    fieldCount: fields.length,
    scannedFileCount: scanState.scannedFiles,
    skippedDirectoryCount: scanState.skippedDirectories.length,
    skippedFileCount: scanState.skippedFiles.length,
    totalReferences: references.length,
    highConfidenceCount: references.filter((item) => item.confidence === 'HIGH').length,
    mediumConfidenceCount: references.filter((item) => item.confidence === 'MEDIUM').length,
    lowConfidenceCount: references.filter((item) => item.confidence === 'LOW').length
  }
}

function summarizeRenameRisk(references) {
  if (references.some((item) => item.confidence === 'HIGH')) {
    return 'HIGH'
  }
  if (references.some((item) => item.confidence === 'MEDIUM')) {
    return 'MEDIUM'
  }
  return 'LOW'
}

function suggestedAction(renameRisk, count) {
  if (count === 0) {
    return '未发现配置范围内的业务代码引用；仍建议在重命名前确认 defaultPaths 覆盖了相关 SQL、迁移和模型目录。'
  }
  if (renameRisk === 'HIGH') {
    return '发现高置信字段引用；重命名、废弃或合并标准前需要先复核并更新业务 SQL、迁移或模型文件。'
  }
  return '发现疑似字段引用；重命名前建议人工确认这些文件是否依赖该字段。'
}

function nextActions(renameRisk, count) {
  if (count === 0) {
    return [
      '确认 .dataspec/config.json 的 defaultPaths 已覆盖业务 SQL、迁移、模型和配置目录。',
      '若字段仍计划重命名，可在业务仓库运行测试或 lint-changed 做二次确认。'
    ]
  }
  const actions = [
    '重命名或废弃字段前，逐项复核 references 中的业务文件位置。',
    '更新业务 SQL、迁移、ORM/model 或配置后，再运行 index-refs 验证引用是否清空。'
  ]
  if (renameRisk === 'HIGH') {
    actions.push('高风险引用需要人工确认，不要让 AI 自动批量改名。')
  }
  return actions
}

function isScannableFile(filePath) {
  return SCANNABLE_EXTENSIONS.has(path.extname(filePath).toLowerCase())
}

function diagnostic(code, message) {
  return {
    code,
    severity: 'WARNING',
    message: sanitizeSecretText(message)
  }
}

function scanPathError(code, message, suggestedAction) {
  const error = new Error(sanitizeSecretText(message))
  error.diagnostic = {
    code,
    category: 'CONFIGURATION',
    severity: 'ERROR',
    retryable: true,
    suggestedAction
  }
  return error
}

function safeOutputPath(filePath, rootDir) {
  const relative = path.relative(rootDir, filePath)
  const outputPath = relative && !relative.startsWith('..') && !path.isAbsolute(relative)
    ? relative
    : path.basename(filePath)
  return toPosixPath(outputPath)
}

function compareReference(left, right) {
  return left.file.localeCompare(right.file)
    || left.line - right.line
    || left.column - right.column
    || left.matchedText.localeCompare(right.matchedText)
}

function uniqueSorted(items) {
  return [...new Set(items)].sort((left, right) => left.localeCompare(right))
}

function toPosixPath(value) {
  return String(value ?? '').replaceAll('\\', '/')
}

function isPathInsideOrEqual(targetPath, rootDir) {
  const relative = path.relative(path.resolve(rootDir), path.resolve(targetPath))
  return relative === '' || (!relative.startsWith('..') && !path.isAbsolute(relative))
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function sanitizeSecretText(value) {
  if (value === undefined || value === null) {
    return value
  }
  return String(value)
    .replace(/\b(https?:\/\/)[^\s/]*@/gi, '$1')
    .replace(/jdbc:[^\s"'<>]+/gi, 'jdbc:***')
    .replace(/\b((?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis):\/\/)[^\s"'<>]+/gi, '$1***')
    .replace(/(authorization\s*[:=]\s*bearer\s+)[^\s,;]+/gi, '$1***')
    .replace(/(authorization\s*[:=]\s*)(?!\s*['"]?bearer\s+)(['"]?)[^,;}&\r\n]+\2/gi, '$1$2***$2')
    .replace(/\b(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1***')
    .replace(/((?:"|')?\b(?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|jdbc[_-]?url|connection[_-]?string|dsn)\b(?:"|')?\s*[:=]\s*)(['"]?)[^\s"',;}&]+\2/gi, '$1$2***$2')
}
