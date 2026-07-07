## Why

DataSpec 已经能输出字段质量、冲突、覆盖率、候选、AI 反馈、Context 和热区等治理信号，但用户和 AI 仍需要自己判断下一步该做什么。P6-114 需要把这些诊断信号整理成项目级推荐任务队列，让 AI 能稳定选择下一步动作、复制命令并知道完成判定。

## What Changes

- 新增 AI 任务推荐队列能力，按项目生成只读 recommended task cards。
- 新增后端只读 API：`GET /api/ai-task-recommendations?projectId=<id>`，返回任务摘要和任务卡列表。
- 任务卡包含 `taskType`、`priority`、`title`、`reason`、`targetRoute`、`recommendedCommand`、`evidenceRefs` 和 `completionCheck`。
- 第一版只聚合已有安全摘要信号，不新增数据库 schema、迁移、后台调度或自动执行。
- 不做前端页面、CLI/MCP 新命令、自动写入、自动采纳候选或高风险任务执行。

## Capabilities

### New Capabilities

- `ai-task-recommendation-queue`: 基于 DataSpec 现有只读诊断信号生成项目级 AI 下一步任务推荐队列。

### Modified Capabilities

- 无。

## Impact

- 后端：新增 `aitaskrecommendation` controller/service/model；只读依赖标准健康、质量门禁、标准候选、标准使用热区等已有能力。
- API：新增只读响应契约，需要字段级说明、Controller 路由测试和 Service 排序/边界测试。
- 安全：不返回 SQL 原文、AI payload、候选 raw evidence、数据库连接串、token、password 或 Authorization。
- 验证：新增 service/controller 单测，运行受影响后端测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
