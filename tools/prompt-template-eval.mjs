#!/usr/bin/env node

import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const TOOLS_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.dirname(TOOLS_DIR)
const DEFAULT_FIXTURE_DIR = path.join(REPO_ROOT, 'dataspec-server', 'src', 'test', 'resources', 'fixtures', 'prompts')

export const promptTemplateContracts = {
  'create-table-prompt': {
    promptVersion: 'create-table-prompt@1',
    requiredSections: [
      '# DataSpec 建表 Prompt',
      '## Prompt Metadata',
      '## 业务需求',
      '## 字段目录 field-catalog.json',
      '## 命名规则 rules.yaml',
      '## 数据库规则 DATABASE_RULES.md',
      '## 输出要求'
    ],
    requiredPhrases: [
      'promptVersion: create-table-prompt@1',
      'PostgreSQL CREATE TABLE',
      'COMMENT ON TABLE / COMMENT ON COLUMN',
      '优先复用 field-catalog.json'
    ]
  },
  'fix-sql-prompt': {
    promptVersion: 'fix-sql-prompt@1',
    requiredSections: [
      '# DataSpec SQL 修正 Prompt',
      '## Prompt Metadata',
      '## 原始 SQL',
      '## Lint 统计',
      '## Lint issues',
      '## 字段目录 field-catalog.json',
      '## 命名规则 rules.yaml',
      '## 输出要求'
    ],
    requiredPhrases: [
      'promptVersion: fix-sql-prompt@1',
      '修正后的 SQL',
      '优先复用标准字段'
    ]
  }
}

export function evaluatePromptOutput(templateKey, output) {
  const contract = promptTemplateContracts[templateKey]
  if (!contract) {
    throw new Error(`未知 Prompt 模板: ${templateKey}`)
  }
  const content = output ?? ''
  const failures = []
  for (const section of contract.requiredSections) {
    if (!content.includes(section)) {
      failures.push({
        kind: 'MISSING_SECTION',
        marker: section,
        message: `Prompt 缺少必备段落: ${section}`
      })
    }
  }
  for (const phrase of contract.requiredPhrases) {
    if (!content.includes(phrase)) {
      failures.push({
        kind: 'MISSING_PHRASE',
        marker: phrase,
        message: `Prompt 缺少必备短语: ${phrase}`
      })
    }
  }
  return {
    templateKey,
    promptVersion: contract.promptVersion,
    passed: failures.length === 0,
    failures,
    requiredSections: contract.requiredSections,
    requiredPhrases: contract.requiredPhrases
  }
}

export async function evaluatePromptFixtures(options = {}) {
  const fixtureDir = options.fixtureDir ?? DEFAULT_FIXTURE_DIR
  const templateKeys = options.templateKeys ?? Object.keys(promptTemplateContracts)
  const results = []
  for (const templateKey of templateKeys) {
    const fixturePath = path.join(fixtureDir, `${templateKey}-golden.md`)
    const output = await readFile(fixturePath, 'utf8')
    results.push({
      ...evaluatePromptOutput(templateKey, output),
      fixturePath
    })
  }
  return {
    fixtureDir,
    passed: results.every(result => result.passed),
    results
  }
}

export async function runPromptTemplateEval(argv, io = processIo()) {
  const options = parseArgs(argv)
  if (options.help) {
    io.writeOut(helpText())
    return 0
  }
  const result = await evaluatePromptFixtures(options)
  if (options.format === 'json') {
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  } else {
    io.writeOut(formatText(result))
  }
  return result.passed ? 0 : 1
}

function parseArgs(argv) {
  const options = { fixtureDir: DEFAULT_FIXTURE_DIR, format: 'text', templateKeys: undefined }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--fixture-dir') {
      options.fixtureDir = requiredValue(argv, ++i, arg)
    } else if (arg === '--format') {
      options.format = requiredValue(argv, ++i, arg)
    } else if (arg === '--template') {
      options.templateKeys = [requiredValue(argv, ++i, arg)]
    } else if (arg === '--help' || arg === '-h') {
      options.help = true
    } else {
      throw new Error(`未知参数: ${arg}`)
    }
  }
  if (options.help) {
    return options
  }
  if (!['text', 'json'].includes(options.format)) {
    throw new Error('--format 仅支持 text 或 json')
  }
  return options
}

function requiredValue(argv, index, flag) {
  const value = argv[index]
  if (!isOptionValue(value)) {
    throw new Error(`${flag} 缺少参数值`)
  }
  return value
}

function isOptionValue(value) {
  return typeof value === 'string' && value.length > 0 && !value.startsWith('-')
}

function formatText(result) {
  const lines = [`Prompt template eval: ${result.passed ? 'PASS' : 'FAIL'}`]
  for (const item of result.results) {
    lines.push(`- ${item.templateKey} (${item.promptVersion}): ${item.passed ? 'PASS' : 'FAIL'}`)
    for (const failure of item.failures) {
      lines.push(`  - [${failure.kind}] ${failure.marker}`)
    }
  }
  return `${lines.join('\n')}\n`
}

function helpText() {
  return `DataSpec prompt-template-eval

Usage:
  node tools/prompt-template-eval.mjs [--fixture-dir <dir>] [--template <key>] [--format text|json]

默认读取 dataspec-server/src/test/resources/fixtures/prompts 下的 golden prompt，并做本地 marker 评测。
`
}

function processIo() {
  return {
    writeOut: text => process.stdout.write(text),
    writeErr: text => process.stderr.write(text)
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  runPromptTemplateEval(process.argv.slice(2))
    .then(code => {
      process.exitCode = code
    })
    .catch(error => {
      process.stderr.write(`${error.message}\n`)
      process.exitCode = 1
    })
}
