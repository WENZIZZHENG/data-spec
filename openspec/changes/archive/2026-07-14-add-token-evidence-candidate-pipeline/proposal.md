## Why

DataSpec 已能稳定输出未知词、歧义缩写和禁用词的确定性 token evidence，也已有标准候选 Inbox，但两者之间缺少可追溯、可预览且幂等的入口。用户和 AI 目前需要手工复制证据创建候选，容易丢失来源、重复入箱或绕过人工确认。

## What Changes

- 新增命名证据候选 preview/apply API，把一个字段来源中的未知词、歧义缩写和禁用词整理成 secret-safe dry-run 候选。
- preview 只读返回候选 payload、token signals、重复/冲突状态和进程内签名 dry-run token；不自动写入 Inbox。
- apply 要求显式确认和匹配的 dry-run token，写入后仍进入既有 PENDING 候选决策流程，不自动采纳或改写标准字段。
- 对 `projectId + candidateName + sourceType + sourceRef` 的 `TOKEN_EVIDENCE` 事实增加数据库级幂等约束，并在并发重试时返回既有候选。
- 将 `TOKEN_EVIDENCE` 设为通用 create 不可声明的受控来源，并让专用 apply、通用候选 create、直接/批量字段 create、字段重命名/撤销与候选 accept 共用项目字段名事务锁；批量入口锁后重新查询待创建名称，阻止字段/候选跨表并发穿透或字段重复插入。
- dry-run token 同时绑定完整脱敏候选元数据；preview/apply 使用专用 DTO，避免持久化 Entity 字段泄漏到新契约。
- 在现有标准候选页面增加命名证据预览与确认写入入口，并支持按 `TOKEN_EVIDENCE` 来源筛选。
- 同步 OpenAPI 生成类型、前后端测试、README 和待办状态。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `deterministic-name-tokenization`: token evidence 可作为候选来源，但只有未知词、歧义缩写和禁用词进入 dry-run signals。
- `standard-candidate-inbox`: 增加命名证据 preview/apply、签名确认、项目隔离和数据库级幂等语义。
- `field-model`: 标准字段直接创建与候选采纳加入 active 候选同名预留边界，避免字段/候选跨表并发冲突。

## Impact

- API：新增 `/api/standard-candidates/token-evidence/preview` 与 `/api/standard-candidates/token-evidence/apply`；既有候选 API 保持兼容。
- 后端：扩展 standardcandidate 与 field service/repository/mapper，复用 QueryNormalizationService 和 DryRunEvidenceSigner。
- 存储：新增 Flyway migration，只为 `TOKEN_EVIDENCE` 来源增加 partial unique index，不修改既有列或历史数据。
- 前端：扩展候选 API wrapper、生成类型、候选工作台和对应测试。
- 安全：不保存 raw sourceText、业务数据行或凭据；sourceRef、comment 和 evidence 输出继续脱敏与限长；旧 preview 的任一候选元数据漂移都会使 token 失效。
