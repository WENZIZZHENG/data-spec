## Context

DataSpec 已经有 `standard-evidence` API-only capability，用来聚合字段标准的来源可信度、使用示例、覆盖率和相关任务证据。现有 workflow recipes 覆盖建表、PR SQL Review、反向导入和最小 Context 导出，但缺少一条面向“字段标准问答/变更前证据复核”的显式路径。

## Goals / Non-Goals

**Goals:**

- 新增 `standard-evidence-review` recipe，让 AI 在回答字段标准依据、可信度、最近使用情况或准备调整标准前先读取证据。
- 复用现有 `WORKFLOW_RECIPES` 单一来源，使 CLI list/show、MCP resource、AI Context markdown 和 task card 自动共享同一 recipe。
- 在测试中锁定该 recipe 的只读、API-only、plan-only 和 task card 可派生行为。

**Non-Goals:**

- 不新增 `dataspec standard-evidence` CLI command。
- 不新增 MCP tool/resource，也不新增后端 API。
- 不把证据结果写入 fixture、日志或 OpenSpec；recipe 只描述读取路径和交付物。

## Decisions

1. **只新增 workflow recipe，不新增执行入口。**
   - 原因：`standard-evidence` 当前 capability 已声明 API surface，workflow recipe 的价值是提供任务顺序和停止条件，而不是复制一个 CLI wrapper。

2. **沿用 `tools/dataspec-workflows.mjs` 的静态 recipe 结构。**
   - 原因：该文件已经被 CLI、MCP、AI Context 和 task card 复用，新增 recipe 可用最小改动覆盖全部发现入口。

3. **recipe 使用 `GET /api/standard-evidence?...` 作为 API 步骤。**
   - 原因：当前没有一等 CLI/MCP surface；显式 API 命令能避免 AI 误认为存在独立 CLI/MCP 工具。

4. **required inputs 使用 `projectId`、`subjectType`、`subjectId`。**
   - 原因：这与 `standard-evidence` capability catalog 的输入边界一致，且第一版最常见主体是 `FIELD`。

## Risks / Trade-offs

- **[Risk] AI 误以为 recipe 会自动读取或保存证据** -> Mitigation：`sideEffectPolicy` 保持 `plan-only`，测试断言步骤不声明新增 CLI/MCP surface。
- **[Risk] API 命令样例涉及 auth 但不能泄露 token** -> Mitigation：recipe 只建议从环境变量读取 token，并继续沿用现有 secret-redaction 测试。
- **[Risk] recipe 与 capability catalog 漂移** -> Mitigation：测试同时断言 `capability show standard-evidence` 和 workflow recipe 使用一致的输入与 API endpoint。
