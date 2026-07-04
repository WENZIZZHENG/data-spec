## 1. 规格与数据模型

- [x] 1.1 校验 proposal/design/spec/tasks 与 P6-58 范围一致。
- [x] 1.2 新增 Flyway 迁移 `ds_ai_task_run`，包含状态、恢复字段、索引和注释。
- [x] 1.3 新增后端 entity/mapper/repository/model DTO，并确保 OpenAPI 可见。

## 2. 后端核心实现

- [x] 2.1 新增 AI task run service，支持 start/succeed/partialFail/fail 和敏感 metadata 脱敏。
- [x] 2.2 新增只读 API：列表、最近失败、详情，并做 projectId 访问边界校验。
- [x] 2.3 AI batch SQL lint 创建流程接入 task run，返回 taskRunId、retryable、failedStep、resumeCommand。
- [x] 2.4 保持 AI batch 原 delivery package 响应兼容，失败 item 只形成 partial artifacts 摘要。

## 3. CLI / MCP 接入

- [x] 3.1 CLI 新增 `task list`、`task failures`、`task show` JSON 输出和错误诊断。
- [x] 3.2 MCP 新增 AI task runs resource 和 `get_ai_task_run` 只读 tool。
- [x] 3.3 更新 CLI/MCP 契约测试，覆盖任务查询、详情和失败诊断。

## 4. 前端与证据包

- [x] 4.1 重新生成 OpenAPI 类型并更新前端 task run API 封装。
- [x] 4.2 前端工作台或 AI 批量任务页展示最近失败/可恢复任务和复制恢复命令动作。
- [x] 4.3 AI evidence package 支持 task run 来源摘要，保持脱敏边界。
- [x] 4.4 更新 README/TODO，记录 P6-58 第一版能力与边界。

## 5. 测试、评审与收口

- [x] 5.1 新增/更新后端测试覆盖 task run lifecycle、查询、AI batch 接入、脱敏和重复 retry。
- [x] 5.2 新增/更新前端与 CLI/MCP 测试覆盖 task run 入口。
- [x] 5.3 运行必要验证：OpenSpec strict、后端相关测试、`mvn test`、`pnpm gen:api`、`pnpm check:api`、`pnpm test`、`pnpm build`、`node --test`、`openspec validate --all`。
- [x] 5.4 使用独立代码评审 agent 审查本次变更，修复 findings 后复跑必要验证。
- [x] 5.5 归档 OpenSpec change 并提交。
