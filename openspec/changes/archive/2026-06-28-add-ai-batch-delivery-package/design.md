## Context

DataSpec 已有单次 SQL lint、SQL 检查记录、fixedSql、AI 回放、CLI `lint-files`、PR review 和前端 SQL 校验页。批量场景目前主要依靠 CLI 在本地递归扫描 SQL 文件并聚合 summary/files JSON，但该 JSON 不是稳定的“交付包”：没有 batch run ID、没有可在前端查看的历史记录、没有统一 evidence/nextActions，也没有便于 AI 继续处理的下载入口。

P6-26 面向个人/小团队和 AI agent 的高频工作流，不做后台任务平台。第一版应优先把已有同步能力组织成稳定交付结构，降低 AI 继续修复和用户复盘的成本。

## Goals / Non-Goals

**Goals:**

- 新增轻量 batch run 契约，统一描述批量 SQL lint 的输入、summary、文件级结果、问题、fixedSql、证据和下一步动作。
- 提供后端 API 保存 batch 摘要和 JSON payload，支持列表、详情和下载交付包。
- 增强 CLI `lint-files`，支持生成同构 delivery package 文件，并保持原有 JSON 输出和退出码兼容。
- 前端提供批量任务入口或结果页，能查看最近 run、结果详情和下载 JSON。
- 输出不包含 token/password/完整数据库连接串/业务数据行，不自动修改业务仓库文件。

**Non-Goals:**

- 不实现后台队列、调度器、取消/恢复、分布式 worker 或外部对象存储。
- 不自动应用 fixedSql 到业务仓库文件；文件级补丁留给后续 fixedSql patch 待办。
- 不扫描非 SQL 语言 AST；业务代码引用索引由 P6-88 承接。
- 不把所有历史 `lint-files` 输出强制迁移为 batch run。

## Decisions

### 1. 新增 `ai-batch` 后端模块，以 JSON payload 持久化第一版交付包

后端新增 `ds_ai_batch_run` 表，保存 projectId、batchType、source、status、summaryJson、payloadJson、createdAt、updatedAt 和 operatorName。第一版 payload 使用 JSON 字段保存，避免过早拆成多张明细表；列表页只读 summary，详情和下载读取 payload。

备选方案是完全不落库、只由 CLI 生成本地文件。它更轻，但前端无法查看最近任务，也不能形成统一回放入口，因此不采用。

### 2. 第一版只实现同步批量 SQL lint

API 形态建议：

- `POST /api/ai-batches/sql-lint`：提交 projectId、source、items[]，同步执行每个 SQL item 的 lint，返回 batch package 并保存 run。
- `GET /api/ai-batches?projectId=&current=&size=`：分页查看最近 run。
- `GET /api/ai-batches/{id}`：查看详情。
- `GET /api/ai-batches/{id}/download`：下载 JSON 交付包。

其中 `items[]` 可表示前端粘贴 SQL 或 CLI 传入的 file/sql 内容。后端不会读取本地业务仓库文件；CLI 扫描文件后把内容交给后端或在本地同构生成。

### 3. Delivery package 是 AI 稳定契约

交付包顶层字段建议：

- `packageVersion`
- `batchId`
- `projectId`
- `batchType`
- `source`
- `status`
- `summary`
- `items[]`
- `issueSummary`
- `fixedSqlSummary`
- `unmanagedHints`
- `evidence`
- `nextActions`
- `createdAt`

`items[]` 包含 filePath/itemName、lint result、fixedSql、fixedSqlDiff、dialectDiagnostics、recordId、errorCount/warningCount/suggestionCount。新增字段保持可选，删除或改名必须更新 AI contract 测试。

### 4. CLI 保持兼容，新增显式交付包参数

`lint-files --format json` 继续输出现有结构和退出码；新增参数如 `--delivery-package <path>` 或 `--batch-package <path>` 时，把同构 delivery package 写入文件。是否调用后端 batch API可以由后续参数控制，第一版可先本地生成 package，避免要求后端能访问业务文件路径。

### 5. 前端结果页以扫描和复盘为主

新增“AI 批量任务”页面或在 AI 回放附近增加入口。第一版重点展示：最近 run、summary、文件级问题列表、fixedSql 可用数、下载 JSON。前端不直接读取本机业务文件，不提供自动覆盖文件按钮。

## Risks / Trade-offs

- [Risk] payload JSON 后续查询维度有限。→ 第一版只需要列表/详情/下载；后续有高频过滤再拆明细表。
- [Risk] 同步执行大批量 SQL 可能慢。→ 限制 item 数/内容大小，输出 warning 和 nextActions；后台队列留后续任务。
- [Risk] CLI 本地 package 和后端 package 漂移。→ 抽取共享 builder 或用 contract fixture 锁定稳定字段。
- [Risk] 交付包包含原 SQL，可能被视为敏感。→ 明确不包含 token/password/连接串/业务数据行；原 SQL 是用户提交的结构定义，下载动作显式触发。

## Migration Plan

- 新增 Flyway 迁移创建 batch run 表，不影响已有 SQL 检查记录。
- 新 API/前端入口为 additive；旧 CLI 输出保持兼容。
- 回滚时可保留表和新增字段，停用页面/API 即可；旧功能不依赖 batch run。

## Open Questions

- 第一版 CLI 是否默认只本地写 package，还是在提供 `--server-batch` 时保存到后端？建议先本地生成，后端 API 服务前端和后续 agent 集成。
