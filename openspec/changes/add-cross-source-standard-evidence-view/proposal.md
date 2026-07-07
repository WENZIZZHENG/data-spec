## Why

DataSpec 已经能积累字段来源、来源可信度、标准候选、SQL 检查、AI 作业、标准使用热区和变更日志等信号，但用户和 AI 在查看某个标准字段时仍需要跨页面拼证据。P6-115 需要把这些已有安全摘要聚合成只读证据视图，让 AI 能稳定判断“这个标准从哪里来、近期如何被使用、是否需要复核”。

## What Changes

- 新增跨来源标准证据视图能力，按 `projectId + subjectType + subjectId` 返回某个标准对象的证据摘要。
- 新增后端只读 API：`GET /api/standard-evidence?projectId=<id>&subjectType=FIELD&subjectId=<fieldId>`，第一版仅支持 `FIELD`。
- 证据视图聚合字段来源、来源可信度摘要、使用热区、候选决策、变更日志、AI 作业摘要和 SQL 检查命中计数。
- 响应包含可复制给 AI 的 `aiEvidenceSummary`，只使用字段名、类型、来源类别、计数、状态和脱敏引用。
- 第一版只复用 DataSpec 已保存的安全记录，不新增数据库 schema、迁移、缓存、后台任务或自动写入。
- 不做全量血缘平台、不判断证据真伪、不把临时低置信度证据写成正式标准、不返回业务数据行或敏感连接信息。

## Capabilities

### New Capabilities

- `cross-source-standard-evidence-view`: 聚合某个标准字段的跨来源证据，输出用户和 AI 可消费的只读证据视图。

### Modified Capabilities

- 无。

## Impact

- 后端：新增 `standardevidence` controller/service/model；只读依赖字段库、字段来源、字段来源可信度、标准使用热区、标准候选、变更日志、SQL 检查记录和 AI 作业摘要。
- API：新增只读响应契约，需要字段级说明、Controller 路由测试和 Service 聚合/脱敏测试。
- 安全：不返回 SQL 原文、AI raw payload、候选 raw evidence、raw source metadata、JDBC URL、DSN、token、password、Authorization 或业务数据行。
- 验证：新增 service/controller 单测，运行受影响后端测试、OpenSpec strict、`git diff --check`、敏感词扫描和独立子 agent 评审。
