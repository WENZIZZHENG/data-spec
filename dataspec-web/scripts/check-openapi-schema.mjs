#!/usr/bin/env node

import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

export const DEFAULT_OPENAPI_SOURCE = 'http://localhost:8090/api-docs'
export const DEFAULT_SCHEMA_PATH = 'src/api/schema.ts'

export function parseCheckArgs(argv = [], env = process.env) {
  const options = {}
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index]
    if (token !== '--source' && token !== '--schema') {
      throw new Error(`未知参数: ${token}`)
    }
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) {
      throw new Error(`缺少参数值: ${token}`)
    }
    options[token.slice(2)] = value
    index += 1
  }
  return {
    source: (options.source ?? env.DATASPEC_API_DOCS_URL) || DEFAULT_OPENAPI_SOURCE,
    schemaPath: options.schema ?? DEFAULT_SCHEMA_PATH
  }
}

export function normalizeSchemaText(text) {
  return text.replace(/\r\n/g, '\n')
}

export function buildDriftMessage({ schemaPath, source }) {
  return [
    `OpenAPI schema.ts 已过期: ${schemaPath}`,
    `来源: ${source}`,
    '',
    '请先启动后端后运行 `pnpm gen:api`，或使用最新 OpenAPI 文档重新生成并提交 `src/api/schema.ts`。'
  ].join('\n')
}

export async function checkOpenapiSchema({
  argv = [],
  env = process.env,
  cwd = process.cwd(),
  runGenerator = runOpenapiTypescript
} = {}) {
  const { source, schemaPath } = parseCheckArgs(argv, env)
  const resolvedSchemaPath = path.resolve(cwd, schemaPath)
  const tempDir = await mkdtemp(path.join(tmpdir(), 'dataspec-openapi-schema-'))
  const generatedSchemaPath = path.join(tempDir, 'schema.ts')

  try {
    await runGenerator(source, generatedSchemaPath, cwd)
    const [currentSchema, generatedSchema] = await Promise.all([
      readFile(resolvedSchemaPath, 'utf8'),
      readFile(generatedSchemaPath, 'utf8')
    ])
    const ok = normalizeSchemaText(currentSchema) === normalizeSchemaText(generatedSchema)
    return ok
      ? { ok: true, message: `OpenAPI schema.ts 已是最新: ${schemaPath}` }
      : {
          ok: false,
          message: buildDriftMessage({ schemaPath, source })
        }
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
}

export async function runOpenapiTypescript(source, outputPath, cwd = process.cwd()) {
  const localBin = path.join(
    cwd,
    'node_modules',
    '.bin',
    process.platform === 'win32' ? 'openapi-typescript.cmd' : 'openapi-typescript'
  )
  const command = existsSync(localBin)
    ? localBin
    : process.platform === 'win32'
      ? 'openapi-typescript.cmd'
      : 'openapi-typescript'

  await runCommand(command, [source, '-o', outputPath], cwd)
}

async function runCommand(command, args, cwd) {
  const { code, stdout, stderr } = await new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd,
      shell: process.platform === 'win32',
      stdio: ['ignore', 'pipe', 'pipe']
    })
    const stdoutChunks = []
    const stderrChunks = []
    child.stdout.on('data', (chunk) => stdoutChunks.push(Buffer.from(chunk)))
    child.stderr.on('data', (chunk) => stderrChunks.push(Buffer.from(chunk)))
    child.on('error', reject)
    child.on('close', (exitCode) => {
      resolve({
        code: exitCode ?? 1,
        stdout: Buffer.concat(stdoutChunks).toString('utf8'),
        stderr: Buffer.concat(stderrChunks).toString('utf8')
      })
    })
  })

  if (code !== 0) {
    const detail = stderr.trim() || stdout.trim() || `exit code ${code}`
    throw new Error(`openapi-typescript 执行失败: ${detail}`)
  }
}

async function main(argv = process.argv.slice(2), io = process) {
  try {
    const result = await checkOpenapiSchema({ argv })
    if (result.ok) {
      io.stdout.write(`${result.message}\n`)
      return 0
    }
    io.stderr.write(`${result.message}\n`)
    return 1
  } catch (error) {
    io.stderr.write(`OpenAPI schema 检查失败: ${error.message}\n`)
    return 2
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = await main()
}
