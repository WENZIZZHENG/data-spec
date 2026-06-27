## Why

DataSpec 字段标准长期维护后，容易出现同义字段重复、别名互相占用、显示名近似但类型不同、敏感标识或代码集不一致等问题。字段推荐、AI Context 和 SQL 修复都依赖字段库质量，如果这些冲突没有显式暴露，AI 会更容易复用错误字段或生成不一致 DDL。

## What Changes

- 新增项目级字段冲突检测报告 API，实时扫描当前项目字段库，不写入数据库。
- 检测字段名、别名、显示名、语义词组、数据类型、代码集、敏感标记、状态等冲突。
- 报告输出冲突类型、严重级别、涉及字段、命中证据、建议动作和摘要统计。
- 前端新增“字段冲突”页面，可按类型/严重级别筛选，并跳转字段库编辑具体字段。
- README/TODO 更新 P6-8 状态和用法。

## Capabilities

### New Capabilities

- `field-conflict-detection`: 定义项目内标准字段重复与冲突检测的可观察行为。

## Impact

- 后端：新增 `fieldconflict` model/service/controller 和单元测试。
- 前端：新增 API wrapper、类型、结果页、展示工具函数和单元测试。
- 文档：更新 README、TODO、OpenSpec tasks。
- 不新增数据库表，不自动合并或删除字段。
