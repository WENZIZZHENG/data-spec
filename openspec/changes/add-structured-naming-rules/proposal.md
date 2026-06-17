## Why

DataSpec 已有 `RuleConfig.paramsJson` 和若干固定 lint 规则，但 AI 使用时仍缺一份稳定、结构化、可复制的命名规则模型。第一版需要把内置默认规则和项目 `paramsJson` 汇总成 `naming:` 导出，同时补一个可执行的字段后缀/前缀类型规则，覆盖 `_id/_at/_no/_count/is_` 这类个人高频约束。

## What Changes

- 新增 `field_suffix_type` lint 规则，默认校验 `_id/_at/_no/_count` 后缀和 `is_` 前缀对应的数据类型。
- 支持通过 `paramsJson.suffixTypes` 和 `paramsJson.prefixTypes` 覆盖后缀/前缀类型配置。
- 扩展 AI `rules.yaml`，新增结构化 `naming:` 节，包含 table/field case、required columns、forbidden names、recommendations、suffix/prefix type rules。
- 新增规则测试和 AI rules 导出测试。
- 本阶段不做复杂规则 DSL、拼音检测或规则配置页面重做。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `sql-lint-rules`: 增加字段后缀/前缀类型规则。
- `ai-context-package`: rules.yaml 增加结构化命名规则模型。

## Impact

- 新增 lint rule 类和单元测试。
- 扩展 `AiContextExportService.generateRulesYaml`。
- README/TODO 更新 P0-5 状态。
