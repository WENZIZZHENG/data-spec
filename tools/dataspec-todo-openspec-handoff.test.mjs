import assert from 'node:assert/strict'
import { mkdir, mkdtemp, readFile, rm, stat, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  buildOpenSpecDraft,
  parseTodoItem,
  runHandoffCli,
  writeOpenSpecDraft
} from './dataspec-todo-openspec-handoff.mjs'

const TODO_FIXTURE = `# DataSpec 待办路线图

### P6-47：TODO 到 OpenSpec 的实施交接助手
- 状态：待办。
- 为什么做：主待办已经积累大量 P6 任务，真正开工时仍需要把边界手动转成 OpenSpec。
- 已有基础：已有结构化 TODO、OpenSpec-first 流程和多个已完成 change。
- 缺口：缺少从单个 TODO 条目生成 OpenSpec change 草稿的稳定模板。
- 落地产物：新增轻量脚本；读取指定 P6 条目，生成 change_id、proposal、design、spec 草稿和 tasks 初稿。
- 验收标准：选择一个 P6 待办后，可快速生成符合项目格式的 OpenSpec 草稿。
- 边界：不自动实现代码，不自动归档 change。

### P6-48：业务术语表与同义词词根库
- 状态：待办。
- 为什么做：字段别名散落在单个字段上后，AI 很难稳定理解“用户/账号/会员”“手机号/电话/mobile”等项目级术语关系。
- 已有基础：字段已有 alias、category、tags、字段推荐原因、字段检索待办和 AI Context 导出。
- 缺口：缺少项目级 glossary，把中文术语、英文词根、拼音缩写、禁用词、推荐 canonical 字段和适用范围统一管理。
- 落地产物：新增术语表模型/API/前端维护入口；支持术语、同义词、英文词根、适用分组、禁用说明和示例字段。
- 验收标准：AI 能在字段推荐、检索和 Context 中读取术语命中原因；维护入口可增删改查术语。
- 边界：不做跨项目企业级知识图谱，不自动把术语写成正式字段。
`

test('parseTodoItem extracts structured TODO fields', () => {
  const item = parseTodoItem(TODO_FIXTURE, 'P6-48')

  assert.equal(item.id, 'P6-48')
  assert.equal(item.title, '业务术语表与同义词词根库')
  assert.equal(item.fields.status, '待办。')
  assert.match(item.fields.why, /字段别名散落/)
  assert.match(item.fields.foundation, /AI Context/)
  assert.match(item.fields.gap, /项目级 glossary/)
  assert.match(item.fields.deliverables, /术语表模型/)
  assert.match(item.fields.acceptance, /术语命中原因/)
  assert.match(item.fields.boundary, /不做跨项目/)
})

test('parseTodoItem fails clearly when item is missing', () => {
  assert.throws(() => parseTodoItem(TODO_FIXTURE, 'P6-404'), /找不到 TODO 条目 P6-404/)
})

test('buildOpenSpecDraft generates reviewable OpenSpec artifacts', () => {
  const item = parseTodoItem(TODO_FIXTURE, 'P6-48')
  const draft = buildOpenSpecDraft(item)

  assert.equal(draft.changeId, 'add-business-glossary-synonym-roots')
  assert.equal(draft.capability, 'business-glossary-synonym-roots')
  assert.equal(draft.files['.openspec.yaml'], 'schema: spec-driven\nid: add-business-glossary-synonym-roots\nstatus: proposed\n')
  assert.match(draft.files['proposal.md'], /字段别名散落/)
  assert.match(draft.files['design.md'], /不自动实现/)
  assert.match(draft.files['specs/business-glossary-synonym-roots/spec.md'], /#### Scenario: Preserve TODO acceptance criteria/)
  assert.match(draft.files['tasks.md'], /人工确认 OpenSpec 草稿/)
  assert.ok(draft.openQuestions.some((question) => question.includes('change id')))
})

test('writeOpenSpecDraft writes files and refuses overwrite', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-handoff-'))
  try {
    const item = parseTodoItem(TODO_FIXTURE, 'P6-48')
    const draft = buildOpenSpecDraft(item, { changeId: 'add-test-glossary', capability: 'test-glossary' })
    const result = await writeOpenSpecDraft(draft, path.join(dir, 'changes'))

    assert.equal(result.changeDir, path.join(dir, 'changes', 'add-test-glossary'))
    assert.match(await readFile(path.join(result.changeDir, 'proposal.md'), 'utf8'), /字段别名散落/)
    assert.match(await readFile(path.join(result.changeDir, 'tasks.md'), 'utf8'), /验收标准/)
    await assert.rejects(() => writeOpenSpecDraft(draft, path.join(dir, 'changes')), /已存在/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('writeOpenSpecDraft rejects draft files outside change directory', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-handoff-'))
  try {
    const item = parseTodoItem(TODO_FIXTURE, 'P6-48')
    const draft = buildOpenSpecDraft(item, { changeId: 'add-test-glossary', capability: 'test-glossary' })
    draft.files['../outside.md'] = 'should not be written'

    await assert.rejects(
      () => writeOpenSpecDraft(draft, path.join(dir, 'changes')),
      /路径越界/
    )
    assert.equal(await exists(path.join(dir, 'changes', 'add-test-glossary', 'proposal.md')), false)
    assert.equal(await exists(path.join(dir, 'changes', 'outside.md')), false)
    assert.equal(await exists(path.join(dir, 'outside.md')), false)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runHandoffCli supports dry-run json without writing files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-handoff-'))
  try {
    const todoPath = path.join(dir, 'TODO.md')
    const outputDir = path.join(dir, 'changes')
    await mkdir(dir, { recursive: true })
    await writeFile(todoPath, TODO_FIXTURE, 'utf8')
    const io = createIo()

    const code = await runHandoffCli([
      '--todo',
      todoPath,
      '--item',
      'P6-48',
      '--output-dir',
      outputDir,
      '--dry-run',
      '--format',
      'json'
    ], io)

    const output = JSON.parse(io.stdout)
    assert.equal(code, 0)
    assert.equal(output.kind, 'dataspec.todo-openspec-handoff')
    assert.equal(output.changeId, 'add-business-glossary-synonym-roots')
    assert.equal(output.files.length, 5)
    assert.equal(await exists(path.join(outputDir, 'add-business-glossary-synonym-roots')), false)
    assert.match(output.nextActions.join('\n'), /人工确认/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

async function exists(filePath) {
  try {
    await stat(filePath)
    return true
  } catch (error) {
    if (error.code === 'ENOENT') {
      return false
    }
    throw error
  }
}

function createIo() {
  return {
    stdout: '',
    stderr: '',
    writeOut(text) {
      this.stdout += text
    },
    writeErr(text) {
      this.stderr += text
    }
  }
}
