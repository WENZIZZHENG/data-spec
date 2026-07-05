#!/usr/bin/env node

import { execFile } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'
import path from 'node:path'

const execFileAsync = promisify(execFile)
const SCRIPT_PATH = fileURLToPath(import.meta.url)
const REPO_ROOT = path.resolve(path.dirname(SCRIPT_PATH), '..')
const KIND = 'dataspec.validation-advice'
const SCHEMA_VERSION = 1

const VALIDATION_RULES = [
  {
    id: 'backend-tests',
    category: 'backend',
    command: 'mvn test',
    cwd: 'dataspec-server',
    estimatedSeconds: 90,
    reason: '后端 Java/Spring Boot、SQL lint、反向导入或公共服务改动需要跑后端测试。',
    patterns: [/^dataspec-server\//]
  },
  {
    id: 'frontend-tests',
    category: 'frontend',
    command: 'pnpm test',
    cwd: 'dataspec-web',
    estimatedSeconds: 20,
    reason: '前端源码、测试或配置改动需要跑源码级前端冒烟与契约测试。',
    patterns: [/^dataspec-web\//]
  },
  {
    id: 'frontend-build',
    category: 'frontend',
    command: 'pnpm build',
    cwd: 'dataspec-web',
    estimatedSeconds: 35,
    reason: '前端页面、类型或构建配置改动需要确认 TypeScript 与 Vite 生产构建通过。',
    patterns: [/^dataspec-web\/(src|tests|package\.json|pnpm-lock\.yaml|vite\.config|tsconfig)/]
  },
  {
    id: 'frontend-api-check',
    category: 'contract',
    command: 'pnpm check:api',
    cwd: 'dataspec-web',
    estimatedSeconds: 10,
    reason: 'OpenAPI 生成类型、前端 API 封装或后端 Controller/契约改动需要检查 schema 漂移。',
    patterns: [
      /^dataspec-web\/src\/api\//,
      /^dataspec-web\/scripts\/check-openapi-schema\.mjs$/,
      /^dataspec-web\/tests\/checkOpenapiSchema\.test\.mjs$/,
      /^dataspec-server\/src\/main\/java\/com\/dataspec\/.*(Controller|Contract|Schema|Dto|Req|Resp|Result)\.java$/
    ]
  },
  {
    id: 'cli-contract-tests',
    category: 'cli',
    command: 'node --test tools/dataspec-config.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs',
    cwd: '.',
    estimatedSeconds: 15,
    reason: 'CLI/MCP、配置读取或 AI 可读 JSON 契约改动需要跑 Node 契约测试。',
    patterns: [/^tools\/dataspec-(config|cli|mcp)\.(mjs|test\.mjs)$/]
  },
  {
    id: 'verify-advisor-tests',
    category: 'tooling',
    command: 'node --test tools/dataspec-verify-advisor.test.mjs',
    cwd: '.',
    estimatedSeconds: 5,
    reason: '验证建议规则或输出格式改动需要跑验证建议工具单测。',
    patterns: [/^tools\/dataspec-verify-advisor\.(mjs|test\.mjs)$/]
  },
  {
    id: 'todo-openspec-handoff-tests',
    category: 'tooling',
    command: 'node --test tools/dataspec-todo-openspec-handoff.test.mjs',
    cwd: '.',
    estimatedSeconds: 5,
    reason: 'TODO 到 OpenSpec 交接助手或草稿生成规则改动需要跑 handoff 单测。',
    patterns: [/^tools\/dataspec-todo-openspec-handoff\.(mjs|test\.mjs)$/]
  },
  {
    id: 'status-check',
    category: 'docs',
    command: 'node tools/dataspec-status-check.mjs --format json',
    cwd: '.',
    estimatedSeconds: 5,
    reason: 'README/TODO/OpenSpec 状态、完成项或状态检查工具改动需要检查文档与规格入口是否漂移。',
    patterns: [
      /^README\.md$/,
      /^TODO\.md$/,
      /^openspec\//,
      /^tools\/dataspec-status-check\.(mjs|test\.mjs)$/
    ]
  },
  {
    id: 'status-check-tests',
    category: 'tooling',
    command: 'node --test tools/dataspec-status-check.test.mjs',
    cwd: '.',
    estimatedSeconds: 5,
    reason: '状态一致性检查工具或测试改动需要跑 status-check 单测，确认漂移规则和 CLI 输出稳定。',
    patterns: [/^tools\/dataspec-status-check\.(mjs|test\.mjs)$/]
  },
  {
    id: 'prompt-eval-tests',
    category: 'prompt',
    command: 'node --test tools/prompt-template-eval.test.mjs',
    cwd: '.',
    estimatedSeconds: 5,
    reason: 'Prompt fixture 或本地评测脚本改动需要跑 prompt golden 评测测试。',
    patterns: [/^tools\/prompt-template-eval\.(mjs|test\.mjs)$/, /^dataspec-server\/src\/test\/resources\/fixtures\/prompts\//]
  },
  {
    id: 'local-smoke-tests',
    category: 'local-smoke',
    command: 'node --test tools/dataspec-local-smoke.test.mjs',
    cwd: '.',
    estimatedSeconds: 8,
    reason: '本地启动包、smoke 脚本或 Compose/Vite 代理契约改动需要跑本地 smoke 单测。',
    patterns: [/^tools\/dataspec-local-smoke\.(mjs|test\.mjs)$/, /^docker-compose\.local\.yml$/]
  },
  {
    id: 'docker-compose-config',
    category: 'local-smoke',
    command: 'docker compose -f docker-compose.local.yml config',
    cwd: '.',
    estimatedSeconds: 5,
    reason: 'Docker Compose 本地启动配置改动需要验证 Compose 文件可解析。',
    patterns: [/^docker-compose\.local\.yml$/]
  },
  {
    id: 'openspec-validate',
    category: 'openspec',
    command: 'openspec validate --all',
    cwd: '.',
    estimatedSeconds: 10,
    reason: 'OpenSpec change 或主规格改动需要校验规范结构。',
    patterns: [/^openspec\//]
  }
]

const BASE_DIFF_CHECK = {
  id: 'diff-check',
  category: 'git',
  command: 'git diff --check',
  cwd: '.',
  estimatedSeconds: 3,
  reason: '所有代码和文档改动提交前都需要检查空白、冲突标记和行尾问题。'
}

export function buildValidationAdvice(inputPaths = []) {
  const paths = normalizePaths(inputPaths)
  const commands = []
  const seen = new Set()

  for (const rule of VALIDATION_RULES) {
    if (paths.some((inputPath) => matchesRule(inputPath, rule))) {
      addCommand(commands, seen, rule)
    }
  }

  if (paths.length > 0) {
    addCommand(commands, seen, BASE_DIFF_CHECK)
  }

  return {
    kind: KIND,
    schemaVersion: SCHEMA_VERSION,
    inputPaths: paths,
    commands,
    summary: {
      totalPaths: paths.length,
      totalCommands: commands.length,
      categories: [...new Set(commands.map((command) => command.category))],
      estimatedSeconds: commands.reduce((total, command) => total + command.estimatedSeconds, 0)
    },
    nextActions: buildNextActions(commands)
  }
}

export function formatValidationAdviceText(advice) {
  const lines = ['DataSpec 验证建议']
  if (advice.inputPaths.length === 0) {
    lines.push('', '未收到变更路径。可传 `--path <file>`，或用 `--changed` 读取当前 git 变更。')
    return `${lines.join('\n')}\n`
  }

  lines.push('', `输入路径：${advice.inputPaths.length} 个`, '')
  advice.commands.forEach((command, index) => {
    const cwd = command.cwd === '.' ? '仓库根目录' : command.cwd
    lines.push(`${index + 1}. ${command.command}`)
    lines.push(`   cwd: ${cwd}`)
    lines.push(`   原因: ${command.reason}`)
    lines.push(`   预计: ${command.estimatedSeconds}s`)
  })
  lines.push('', '下一步:')
  for (const action of advice.nextActions) {
    lines.push(`- ${action}`)
  }
  return `${lines.join('\n')}\n`
}

export async function runAdvisorCli(args = process.argv.slice(2), io = defaultIo(), deps = {}) {
  try {
    const options = parseArgs(args)
    if (options.help) {
      io.writeOut(helpText())
      return 0
    }

    let inputPaths = options.paths
    if (options.changed) {
      const getChangedPaths = deps.getChangedPaths ?? getGitChangedPaths
      inputPaths = inputPaths.concat(await getChangedPaths())
    }

    const advice = buildValidationAdvice(inputPaths)
    if (options.format === 'json') {
      io.writeOut(`${JSON.stringify(advice, null, 2)}\n`)
    } else {
      io.writeOut(formatValidationAdviceText(advice))
    }
    return 0
  } catch (error) {
    io.writeErr(`错误: ${error.message}\n`)
    return 2
  }
}

function parseArgs(args) {
  const options = {
    paths: [],
    changed: false,
    format: 'text',
    help: false
  }

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]
    if (arg === '--help' || arg === '-h') {
      options.help = true
      continue
    }
    if (arg === '--changed') {
      options.changed = true
      continue
    }
    if (arg === '--path') {
      const value = args[index + 1]
      if (!value) {
        throw new Error('--path 需要文件路径')
      }
      options.paths.push(value)
      index += 1
      continue
    }
    if (arg.startsWith('--path=')) {
      options.paths.push(arg.slice('--path='.length))
      continue
    }
    if (arg === '--format') {
      const value = args[index + 1]
      if (!value) {
        throw new Error('--format 需要 text 或 json')
      }
      options.format = normalizeFormat(value)
      index += 1
      continue
    }
    if (arg.startsWith('--format=')) {
      options.format = normalizeFormat(arg.slice('--format='.length))
      continue
    }
    if (arg.startsWith('-')) {
      throw new Error(`未知参数: ${arg}`)
    }
    options.paths.push(arg)
  }

  return options
}

function normalizeFormat(value) {
  if (value !== 'text' && value !== 'json') {
    throw new Error('--format 只支持 text 或 json')
  }
  return value
}

async function getGitChangedPaths() {
  const [tracked, untracked] = await Promise.all([
    execFileAsync('git', ['diff', '--name-only', 'HEAD'], {
      cwd: REPO_ROOT,
      encoding: 'utf8'
    }),
    execFileAsync('git', ['ls-files', '--others', '--exclude-standard'], {
      cwd: REPO_ROOT,
      encoding: 'utf8'
    })
  ])
  return collectChangedPathsFromGitOutput(tracked.stdout, untracked.stdout)
}

export function collectChangedPathsFromGitOutput(trackedStdout, untrackedStdout) {
  return normalizePaths([
    ...splitGitPathOutput(trackedStdout),
    ...splitGitPathOutput(untrackedStdout)
  ])
}

function splitGitPathOutput(stdout) {
  return String(stdout ?? '').split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
}

function matchesRule(inputPath, rule) {
  return rule.patterns.some((pattern) => pattern.test(inputPath))
}

function addCommand(commands, seen, rule) {
  if (seen.has(rule.id)) {
    return
  }
  seen.add(rule.id)
  commands.push({
    id: rule.id,
    command: rule.command,
    cwd: rule.cwd,
    reason: rule.reason,
    estimatedSeconds: rule.estimatedSeconds,
    category: rule.category
  })
}

function normalizePaths(inputPaths) {
  return [...new Set(inputPaths
    .map((inputPath) => String(inputPath ?? '').trim())
    .filter(Boolean)
    .map((inputPath) => inputPath.replace(/\\/g, '/').replace(/^\.\//, '')))]
}

function buildNextActions(commands) {
  if (commands.length === 0) {
    return ['传入变更路径后再生成验证建议；例如 `--changed` 或 `--path README.md`。']
  }
  return [
    '先运行推荐命令中耗时最短且最贴近本次改动的检查。',
    '若任一命令失败，先修复对应模块，再重新运行该命令和 `git diff --check`。',
    '提交或归档 OpenSpec 前，把关键验证输出记录到变更说明或交付消息中。'
  ]
}

function defaultIo() {
  return {
    writeOut: (text) => process.stdout.write(text),
    writeErr: (text) => process.stderr.write(text)
  }
}

function helpText() {
  return `DataSpec 验证建议工具

用法:
  node tools/dataspec-verify-advisor.mjs --changed [--format text|json]
  node tools/dataspec-verify-advisor.mjs --path README.md --path dataspec-server/src/main/java/App.java
  node tools/dataspec-verify-advisor.mjs README.md dataspec-web/src/App.vue --format json

说明:
  本工具只推荐验证命令，不会自动执行命令。
`
}

if (process.argv[1] && path.resolve(process.argv[1]) === SCRIPT_PATH) {
  process.exitCode = await runAdvisorCli()
}
