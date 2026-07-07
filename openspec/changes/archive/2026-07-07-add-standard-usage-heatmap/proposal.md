## Why

DataSpec 已经能维护字段标准、评分质量、检测冲突、记录 SQL 检查和 AI 作业，但用户还缺少一个统一视角来判断“哪些标准正在被频繁使用且质量风险高，哪些长期无人使用可以清理”。P6-97 需要把这些已有信号聚合成只读使用热区与清理优先级报告，帮助用户和 AI 优先处理真正影响标准质量的字段。

## What Changes

- 新增标准使用热区报告能力，按项目聚合字段质量、字段冲突、来源证据、近期 SQL 检查和近期 AI 作业中的字段命中。
- 新增后端只读 API：`GET /api/standard-usage/heatmap?projectId=<id>`，返回字段级 `usageScore`、`cleanupPriority`、`sourceKinds`、`qualityScore`、`conflictCount`、`lintHits`、`aiJobHits`、`lastReferencedAt` 和 `suggestedNextAction`。
- 第一版只复用已有后端数据和近期记录，不新增数据库 schema、迁移、缓存或异步任务。
- 输出只包含字段名、来源类型和计数摘要，不返回 SQL 原文、AI payload、raw issues、raw source metadata 或凭据。
- 不做前端页面、CLI/MCP 命令、业务代码引用索引集成、自动清理或自动修改字段。

## Capabilities

### New Capabilities

- `standard-usage-heatmap`: 聚合标准字段使用热区、质量风险和清理优先级，提供只读 API 给用户和 AI 消费。

### Modified Capabilities

- 无。

## Impact

- 后端：新增 `standardusageheatmap` controller/service/model；只读依赖字段库、字段质量、字段冲突、字段来源、SQL 检查记录和 AI 作业记录。
- API：新增只读响应契约，字段需要业务语义说明和 MockMvc/Service 单测覆盖。
- 安全：沿用项目访问边界；不返回 SQL 原文、AI 原始输入输出、raw issue JSON、raw source metadata 或可复制凭据。
- 验证：新增 service/controller 单测，运行受影响后端测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
