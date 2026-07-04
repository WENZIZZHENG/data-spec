## Context

DataSpec 已有字段质量评分、字段覆盖率报告、SQL lint、标准健康趋势和验证命令推荐，但这些信号目前偏“查看型”。业务仓库 CI、CLI 和 AI agent 需要一个统一的项目级质量门禁结果：哪些阈值被检查、实际值是多少、是否失败、应该先修什么。

P6-59 第一版服务个人/小团队场景，质量门禁是显式检查命令和只读评估结果，不默认阻断字段保存、候选采纳或本地页面操作。

## Goals / Non-Goals

**Goals:**

- 新增项目级质量门禁配置，支持字段质量、覆盖率、lint error、未纳管字段和敏感标记等阈值。
- 提供评估 API，返回 `PASS/FAIL`、单项检查结果、失败原因、实际值、阈值和 `nextActions`。
- CLI 可用于 CI：门禁失败返回退出码 `1`，参数或服务错误返回 `2`。
- 前端能显示当前项目门禁状态和失败项，并跳转到字段质量、覆盖率或 SQL 校验等修复入口。

**Non-Goals:**

- 不做企业审批流、发布流、权限审批或多人签核。
- 不默认阻断个人本地保存字段、编辑规则或采纳候选。
- 不引入后台调度、队列或定时扫描；评估由 API/CLI/前端显式触发。
- 不扫描业务数据行，不保存数据库密码、token 或完整 JDBC URL。

## Decisions

### 独立 `ds_standard_quality_gate` 配置表

使用独立表保存项目级阈值，而不是把阈值塞进字段质量或规则配置。质量门禁组合多个信号，独立模型能避免污染单一规则实现，也方便 CLI/AI 读取统一契约。

第一版字段：

- `project_id`：项目唯一配置。
- `enabled`：是否启用门禁。
- `min_coverage`：字段覆盖率最低值，百分比整数。
- `min_average_field_score`：字段质量均分最低值。
- `max_error_issues`：允许的 ERROR 级字段质量问题上限。
- `max_new_unmanaged_fields`：允许的未纳管字段数量上限。
- `required_sensitive_marking`：是否要求敏感疑似字段必须标注。
- `config_json`：为后续兼容保存非核心阈值，第一版只读写安全字段。

### 评估复用现有服务

质量门禁不重新实现质量评分和覆盖率算法。字段质量直接调用 `FieldQualityService.report(projectId)`；覆盖率第一版使用最近一次标准健康快照或显式请求传入的覆盖摘要，避免为 CI 强制连接数据库。SQL lint error 可通过请求体提供当前 lint summary，也可省略。

### CLI 是 CI 主入口

新增 `quality-gate check --project <id> --format json`。命令读取服务端门禁评估结果并使用稳定退出码：PASS 为 `0`，FAIL 为 `1`，参数/服务/权限错误为 `2`。这比前端状态更适合业务仓库 CI，也符合 DataSpec 现有 CLI 模式。

### 前端只展示和引导修复

前端第一版在标准健康或字段质量入口展示门禁状态、失败项和 nextActions；不做“保存前拦截”。失败项应链接到已有页面：字段质量、覆盖率、SQL 校验、反向导入或规则配置。

## Risks / Trade-offs

- [Risk] 没有最新覆盖率输入时，门禁覆盖率检查可能缺数据。→ Mitigation：返回 `SKIPPED` 或 `WARNING` 诊断，并提示先生成覆盖率报告或标准健康快照。
- [Risk] 门禁变成过重治理流程。→ Mitigation：第一版只做显式 check 和状态展示，不做审批、不默认阻断编辑。
- [Risk] 阈值配置错误导致 CI 误失败。→ Mitigation：配置 API 校验百分比范围和非负整数，CLI JSON 输出实际值、阈值和修复建议。
- [Risk] 敏感字段判断误报。→ Mitigation：复用字段质量的 `sensitive_not_marked` issue，门禁只统计已有确定性诊断，不额外扩大扫描范围。

## Migration Plan

1. 新增 Flyway 迁移创建 `ds_standard_quality_gate`，为已有项目使用默认配置。
2. 实现后端配置 CRUD/评估 API、DTO 和测试。
3. 更新 OpenAPI 类型、前端 API 封装和状态展示。
4. CLI 新增 `quality-gate check` 并补 Node 测试。
5. 更新 README/TODO，运行后端、前端、CLI 和 OpenSpec 验证。

Rollback：移除新表和入口不会影响字段质量、覆盖率、SQL lint 或标准健康原有功能；CLI 命令失败时只影响显式调用该命令的 CI。
