## Why

大库反向导入、覆盖率报告和 metadata 浏览当前主要依赖同步请求或单页扫描；遇到上千张表、网络慢、权限不一致或中断时，用户和 AI 缺少可恢复的采集作业状态、限速边界和失败摘要，容易重复拉取并增加源库压力。

## What Changes

- 新增数据库 metadata 采集作业能力，围绕 `scanJobId`、`resumeCursor`、`pageSize`、`rateLimit`、`partialResult`、`cancelToken`、`retryPolicy`、`sourcePressureHint` 和只读 evidence 输出建立稳定契约。
- 扩展现有 database metadata scan plan：同步扫描结果继续兼容，同时可升级为作业化分页扫描、取消、恢复、失败摘要和进度轮询。
- 前端反向导入页新增采集作业进度视图，展示批次进度、当前页结果、失败/取消状态、限速提示和恢复入口。
- 覆盖率和 metadata 浏览流程可复用采集作业的部分结果，但不会写标准字段库，也不会扫描业务数据行。
- 保持敏感信息边界：请求/响应/evidence/resume 命令不得包含 password、token、Authorization、完整 JDBC URL、DSN、连接串或业务数据行。

## Capabilities

### New Capabilities
- `db-metadata-scan-jobs`: 定义数据库 metadata 采集作业模型、API、状态机、断点续扫、取消/恢复、限速、失败摘要、只读 evidence 和安全边界。

### Modified Capabilities
- `db-metadata-scan-plan`: 扩展现有只读 scan plan 输出，支持作业化扫描字段、进度状态、限速提示和恢复语义，同时保持现有分页扫描兼容。
- `db-reverse-import-frontend`: 增加反向导入页的采集作业进度视图、取消/恢复交互和部分结果选择边界。
- `field-coverage-report`: 允许覆盖率报告基于采集作业的只读部分结果生成，不要求一次性读取完整库。

## Impact

- 后端：`dataspec-server` 反向导入 controller/service/model、可能新增内存或持久化采集作业模型、OpenAPI schema、相关单元测试。
- 前端：`dataspec-web` 类型、API wrapper、反向导入页、覆盖率页或共享展示工具、前端单测/源码级 smoke。
- OpenSpec：新增 `db-metadata-scan-jobs` 主能力 delta，并修改 scan plan、反向导入前端和覆盖率报告相关规格。
- 安全影响：仍只读访问数据库 metadata，不读取业务数据行、不写源库、不写标准字段库；凭据继续只在请求内使用并在响应、日志、evidence 和恢复命令中脱敏。
