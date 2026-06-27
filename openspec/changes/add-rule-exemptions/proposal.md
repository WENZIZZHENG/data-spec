## Why

真实项目会存在少量历史表、第三方字段或框架约定无法完全符合 DataSpec 规则。如果没有结构化例外，SQL lint、CI 和 AI agent 会持续报告已知误报，用户只能关闭整条规则，反而降低标准系统约束力。

## What Changes

- 新增项目级规则豁免模型/API/前端管理页。
- 支持按 `ruleCode` + 表名/字段名/原因声明例外，可禁用或设置过期时间。
- SQL lint 结果将被豁免命中的 issue 标记为 suppressed，并从 active error/warning/suggestion 统计中排除。
- AI rules.yaml / DATABASE_RULES.md 导出项目例外说明，提醒 agent 例外不是新建表推荐标准。
- 不允许无范围、无原因的全局静默。

## Capabilities

### New Capabilities

- `rule-exemptions`: 定义项目级规则误报豁免、lint 抑制和 AI 导出的可观察行为。

## Impact

- 数据库：新增 `ds_rule_exemption` 表和 Flyway migration。
- 后端：新增 rule exemption entity/repository/service/controller，并在 `SqlLintService` 与 `AiContextExportService` 中读取。
- 前端：新增例外管理页、API wrapper、类型和展示测试。
- 文档：更新 README、TODO 和 OpenSpec tasks。
