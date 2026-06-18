## Why

P2-3 要让业务项目引入 `.dataspec/` 后有稳定落地约定。当前 AI Context 包已包含规则、字段目录、prompt、示例和 `AGENTS.md.fragment`，但缺少明确的版本元数据与目录说明，AI agent 和人类维护者无法稳定判断包版本、文件用途和推荐 lint 命令。

## What Changes

- AI Context 包新增 `.dataspec/manifest.json`，记录 schemaVersion、projectId、生成时间、文件清单和推荐命令。
- AI Context 包新增 `.dataspec/README.md`，说明目录结构、使用方式和更新约定。
- `AGENTS.md.fragment` 增加 manifest 与 lint 命令入口说明。
- TODO 路线图同步更新 P2-3 状态。

## Scope

- 本轮只定义并导出业务项目内 `.dataspec/` 目录约定。
- 不自动修改外部业务仓库，不实现同步器或版本迁移。
- 不改变现有字段目录、规则和 prompt 文件的内容契约。

## Impact

- `dataspec-ai-context.zip` 增加两个文本文件。
- 现有 AI Context 包测试扩展为校验 manifest 和 README。
