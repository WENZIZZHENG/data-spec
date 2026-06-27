## Why

DataSpec 优先给 AI agent 使用，字段目录、规则、lint 结果、字段推荐、DDL 生成和 MCP 输出一旦悄悄漂移，就会让 agent 读错上下文或自动化脚本失效。P6-12 需要把主要 AI 可消费契约沉淀成 fixture/golden 测试，让兼容新增可见、破坏性变化被拦住。

## What Changes

- 新增 AI contract fixtures，覆盖 AI Context、CLI JSON、MCP resources/tools、字段推荐、lint/fixedSql 和 DDL 预览等高价值输出。
- 新增 golden 断言工具或测试入口，将关键字段、类型、稳定枚举和向后兼容规则纳入现有 `mvn test` 与 `node --test`。
- 更新 README 或 `.dataspec/README.md`，说明 AI 消费方应依赖哪些稳定字段，以及新增字段的兼容策略。
- 第一版只锁定外部可消费字段，不冻结全部内部 DTO，不引入外部契约服务。

## Capabilities

### New Capabilities

- `ai-contract-fixtures`: 面向 AI/CLI/MCP 的稳定输出契约、golden fixtures 和兼容性验证入口。

### Modified Capabilities

无。

## Impact

- 后端：补充 AI Context、lint/fixedSql、字段推荐和 DDL 预览相关契约测试与 fixture。
- CLI/MCP：补充 JSON 输出 golden 测试，覆盖 workflow、field catalog、lint、suggest、generate DDL 等入口。
- 文档：README 或 `.dataspec/README.md` 补稳定字段与兼容策略说明。
- 验证：`mvn test`、`node --test tools/...`、OpenSpec validate 和 `git diff --check`。
