## Why

DataSpec 已经能导出 AI Context、字段目录、规则和 Prompt，但这些内容始终读取“当前实时标准”。当字段、枚举或规则发生变化后，AI 产出的 SQL、DDL 或修复建议缺少可追溯的标准版本，后续很难判断结果来自输入变化、标准变化还是 AI 行为变化。

P6-1 要补一个个人/小团队可用的标准版本快照：给当前项目标准生成命名版本、内容 hash 和稳定元数据，并让 AI Context 与核心记录带上该版本信息。

## What Changes

- 新增项目级标准快照模型、迁移、服务和 API，支持创建快照、查看最新快照和列表。
- 快照内容基于当前项目字段、枚举和规则生成确定性 payload，并计算 SHA-256 hash。
- AI Context 的 manifest、field-catalog、rules.yaml、README/AGENTS 说明携带当前 `specVersion` 和 `specHash`。
- SQL 检查记录保存当前标准快照 ID/版本/hash，便于回放当时使用的标准上下文。
- DDL 生成结果返回当前标准版本信息，后续 AI/CLI 可知道本次生成参考了哪版标准。

## Capabilities

### New Capabilities

- `standard-snapshot-versioning`: 项目级标准快照创建、最新快照查询、AI Context 版本标识和核心记录引用。

### Modified Capabilities

- `ai-context-export`: 导出包和单文件导出携带标准版本元数据。
- `sql-lint-record`: SQL 检查记录保存标准快照引用。
- `ddl-generation`: DDL 生成结果返回标准版本元数据。

## Impact

- 后端：新增快照 entity/mapper/repository/service/controller、V9 迁移，扩展 AI Context、SQL 检查记录和 DDL 结果。
- 前端：本轮只做最小能力入口时，可先不新增复杂页面；如需要，补 API 类型和项目页轻量按钮。
- 数据：新增快照表，SQL 检查记录增加可空快照引用列；旧记录保持兼容。
- 验证入口：后端单测/全量测试、前端构建（若改前端）、OpenSpec validate、diff 检查。
