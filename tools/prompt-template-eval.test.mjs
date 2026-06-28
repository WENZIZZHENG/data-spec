import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  evaluatePromptFixtures,
  evaluatePromptOutput,
  runPromptTemplateEval
} from './prompt-template-eval.mjs'

test('prompt fixture eval passes checked-in golden prompts', async () => {
  const result = await evaluatePromptFixtures()

  assert.equal(result.passed, true)
  assert.equal(result.results.length, 2)
  assert.deepEqual(result.results.map(item => item.templateKey), [
    'create-table-prompt',
    'fix-sql-prompt'
  ])
})

test('prompt output eval reports missing markers', () => {
  const result = evaluatePromptOutput('create-table-prompt', '# DataSpec 建表 Prompt')

  assert.equal(result.passed, false)
  assert.ok(result.failures.some(item => item.kind === 'MISSING_SECTION'))
  assert.ok(result.failures.some(item => item.kind === 'MISSING_PHRASE'))
})

test('prompt fixture cli prints json and returns non-zero for broken fixture', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-prompt-eval-'))
  try {
    await mkdir(dir, { recursive: true })
    await writeFile(path.join(dir, 'create-table-prompt-golden.md'), '# DataSpec 建表 Prompt\n', 'utf8')
    const io = createIo()

    const code = await runPromptTemplateEval([
      '--fixture-dir',
      dir,
      '--template',
      'create-table-prompt',
      '--format',
      'json'
    ], io)

    assert.equal(code, 1)
    const payload = JSON.parse(io.stdout)
    assert.equal(payload.passed, false)
    assert.equal(payload.results[0].templateKey, 'create-table-prompt')
    assert.ok(payload.results[0].failures.length > 0)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('prompt fixture cli reports missing option value', async () => {
  const io = createIo()

  await assert.rejects(
    () => runPromptTemplateEval(['--template'], io),
    /--template 缺少参数值/
  )
})

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
