## Why

DataSpec 已经能给 AI 导出上下文、生成 Prompt、校验 SQL、输出 fixedSql 和生成 DDL，但一次 AI 相关操作结束后，用户很难回看“当时用了哪版标准、哪份 prompt、哪些输入输出和 lint 结果”。P6-3 需要把这些已有链路串成轻量回放记录，方便个人/小团队定位 AI 为什么这样生成或修复。

## What Changes

- 新增 AI 作业记录能力，记录建表 Prompt、SQL 修正 Prompt、SQL lint/fixedSql 和 DDL preview 的关键输入、输出、标准快照和 prompt 模板版本。
- 新增后端记录模型/API，支持按项目分页查看作业、查看详情和复制可回放 JSON/CLI 命令。
- 改造现有 AI Context Prompt、SQL lint 和 DDL 生成服务，在不调用外部 LLM 的前提下写入回放记录。
- 前端新增“AI 回放”入口，展示作业列表、详情、标准版本、输入输出、lint 结果和复制回放 payload。
- README/TODO 更新 P6-3 状态、使用方式和边界。

## Capabilities

### New Capabilities

- `ai-decision-replay`: AI 生成与修复决策回放，覆盖作业记录、详情查看、标准快照引用、promptVersion 和可复制回放 payload。

### Modified Capabilities

无。

## Impact

- 后端：新增 `aireplay` 模块、Flyway 迁移、MyBatis mapper/repository/service/controller；接入 `AiContextExportService`、`SqlLintService` 和 `DdlGeneratorService`。
- 前端：新增 API/types、AI 回放页面/路由/菜单和必要展示工具函数测试。
- 数据：新增 `ds_ai_job_record` 表，保存 DataSpec 本地生成/检查的文本与 JSON；不保存第三方 API key，不接外部 LLM。
- 验证：后端 AI 作业记录服务测试、现有 prompt/lint/generator 测试更新、前端展示工具测试、`mvn test`、`pnpm test`、`pnpm build`、OpenSpec validate 和 diff 检查。
