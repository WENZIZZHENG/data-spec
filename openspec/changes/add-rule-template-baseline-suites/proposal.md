## Why

DataSpec 已经有单条规则配置、AI `rules.yaml` 和项目初始化，但个人/小团队会反复配置相似的命名、注释、敏感字段、金额、时间和状态字段规则。AI 使用时也需要知道当前项目采用的是哪套规则基线，否则只能读到零散规则，难以判断严格度、来源和版本。

## What Changes

- 新增内置规则模板库与项目基线套件，第一版覆盖“个人默认基线”“严格基线”“兼容历史库基线”。
- 支持按项目应用内置基线，默认不覆盖用户已调整的规则；需要覆盖时必须显式选择。
- 支持导出当前项目规则基线为机器可读 JSON，并从 JSON 导入到项目。
- 在规则配置页增加基线选择、应用、导出和导入入口。
- 在 AI Context `rules.yaml` 中标明当前基线名称、版本、来源和应用时间，让 AI 能解释规则来自哪套基线。

## Capabilities

### New Capabilities

- `rule-baseline-suites`: 覆盖内置规则模板库、项目基线应用、导入/导出和基线元数据。

### Modified Capabilities

- `rule-config-experience`: 规则配置页需要展示并操作项目规则基线。
- `field-rule-pages`: 规则配置页面需要继续遵守当前项目边界，并提供基线相关可用入口。
- `project-standards`: 新建或初始化项目可选择轻量默认规则基线。
- `ai-context-package`: `rules.yaml` 需要暴露当前规则基线名称、版本、来源和应用时间。

## Impact

- 后端：新增规则基线模板模型、服务和 API；复用现有 `RuleConfigService` 写入项目规则。
- 前端：规则配置页新增基线操作区域，支持应用、导出、导入和查看当前基线。
- 数据：新增轻量项目基线记录表或等价持久化，保存 baseline key/version/source/appliedAt，不保存审批流状态。
- AI/CLI/MCP：AI Context 输出新增 baseline metadata；CLI/MCP 可通过现有 API 或后续命令读取导出结果。
- 文档与待办：README/TODO 标明 P6-23 完成范围和边界。
- 边界：不做组织级发布审批，不强制所有项目统一规则，不自动覆盖用户已调整的规则。
