## Context

数据库直连能力目前集中在 `DatabaseReverseImportServiceImpl`：连接测试、表列表、metadata 预览、二次比对和覆盖率报告都复用同一份 `DatabaseConnectionReq`。`DatabaseConnectionResult` 当前只包含 `success/message`，前端反向导入和覆盖率报告只展示“连接成功/失败”，无法提示高权限账号、只读适配度、可访问范围或最小权限建议。

该变化属于安全提示增强：需要让用户和 AI 更早知道当前连接是否适合反向导入/比对，但不能为了诊断执行写操作，也不能保存或回显 password、token、完整 JDBC URL。

## Goals / Non-Goals

**Goals:**

- 在连接测试成功时返回结构化安全诊断：数据库类型、当前用户、只读/写权限推断、可访问 schema/table 数、风险等级、warnings、recommendedActions 和推荐最小权限 SQL。
- 在连接失败时保持原有 `success=false/message` 行为，并避免泄漏密码、完整连接串或 token。
- 在反向导入页和覆盖率报告页展示诊断摘要，不阻塞后续加载表、预览、比对或报告生成。
- 覆盖 PostgreSQL/MySQL 两条主路径；其他类型返回 UNKNOWN/UNSUPPORTED 风格诊断而不假装安全。

**Non-Goals:**

- 不自动创建数据库账号，不生成或保存数据库凭据。
- 不执行写操作探测权限；不为了确认写权限创建临时表或修改源库。
- 不做企业级权限审计、密钥托管或数据库堡垒机。
- 不把安全诊断结果落库；第一版只随连接测试响应返回。

## Decisions

### 1. 以 additive 字段增强 `DatabaseConnectionResult`

保留 `success` 和 `message` 字段不变，新增 `security` 对象。这样现有前端、CLI 或调用方即使不读取 `security` 也不会破坏。

`security` 建议字段：

- `databaseType`
- `currentUser`
- `readOnly`
- `writeRisk`
- `riskLevel`: `SAFE`、`WARNING`、`DANGER`、`UNKNOWN`
- `accessibleSchemaCount`
- `accessibleTableCount`
- `warnings`
- `recommendedActions`
- `recommendedSql`

### 2. 使用只读 metadata 和方言特定只读查询

第一版允许执行只读查询：

- PostgreSQL：`select current_user`、`select current_setting('transaction_read_only', true)`、`select has_database_privilege(current_database(), 'CREATE')`，并通过 `DatabaseMetaData` 统计可访问 schema/table。
- MySQL：`select current_user()`、`select @@read_only`、`select @@super_read_only`，并通过 `DatabaseMetaData` 统计可访问 table；不依赖创建临时表。

如果查询失败，诊断降级为 `UNKNOWN`，连接测试本身仍可成功，warning 说明无法完成权限判断。

### 3. 风险等级保守判定

- 明确只读、没有明显写权限：`SAFE`，提示“适合反向导入/比对”。
- 无法确认只读或 metadata 权限有限：`WARNING`。
- 检测到高风险写权限、数据库级 CREATE 权限、MySQL 全局 read_only 关闭且用户可能写入：`DANGER`。
- 非 PostgreSQL/MySQL 或诊断查询不可用：`UNKNOWN`。

第一版只给建议，不阻断个人使用。

### 4. 前端只展示摘要，不缓存诊断

反向导入页和覆盖率报告页在测试连接后展示诊断卡片。页面本地记忆继续只保存非敏感连接元数据，不保存 password、完整 JDBC URL 或诊断结果。

## Risks / Trade-offs

- [Risk] 权限推断不是完整审计，部分数据库角色权限复杂。→ 以 warning 和 recommendedActions 表达不确定性，不阻塞使用。
- [Risk] 不执行写操作会让写权限判断偏保守。→ 明确这是“只读安全诊断”，宁可提示风险，不做危险探测。
- [Risk] 不同 MySQL/PostgreSQL 版本系统变量或权限函数行为不同。→ 查询失败降级为 UNKNOWN，并保留连接成功。
- [Risk] 前端展示过多安全文字影响高频流程。→ 只展示摘要、warning 和可复制 recommended SQL，不改变原有按钮链路。

## Migration Plan

- 后端 DTO additive 增强，无数据库迁移。
- 前端类型手动同步或生成 OpenAPI 后更新。
- 回滚时可保留新增字段，前端不展示即可；旧调用方不受影响。

## Open Questions

- 是否需要在后续 P6 中把安全诊断结果纳入连接预设或项目活动时间线？第一版不保存，后续可由用户实际使用反馈决定。
