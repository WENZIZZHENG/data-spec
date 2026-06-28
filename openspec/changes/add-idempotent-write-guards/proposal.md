## Why

DataSpec 已经有多条面向 AI/CLI/MCP 的写入链路：标准快照、反向导入确认、批量 SQL lint、AI job 回放记录和项目备份恢复。多个 coding agent、CLI 自动重试或前端重复点击同时操作同一项目时，可能重复创建记录，或在同一批候选导入中互相穿插。P6-40 第一版需要在不引入外部队列、不做分布式事务的前提下，补上个人/小团队场景足够实用的幂等和项目级任务锁。

## What Changes

- 新增后端轻量写入保护服务，支持 `Idempotency-Key` 作用域缓存、项目级操作 try-lock 和明确的可重试冲突错误。
- 高风险同步写接口接入保护：标准快照创建、数据库反向导入确认、AI 批量 SQL lint、项目备份恢复 apply。
- AI job 回放记录在服务层按稳定请求指纹去重，避免同一请求重试时生成重复回放记录。
- CLI 对会触发后端写入的命令支持透传 `--idempotency-key` 或 `DATASPEC_IDEMPOTENCY_KEY`。
- 增加后端单测和 CLI 契约测试，覆盖重复 key 不重复写、并发锁冲突提示和 header 透传。
- 边界：不新增外部队列，不做分布式锁，不阻塞普通单条 CRUD，不保证服务重启后的历史幂等。

## Capabilities

### New Capabilities

- `idempotent-write-guards`: DataSpec 后端提供单机轻量幂等与项目级写入锁，供 AI/CLI/前端高风险写入复用。

### Modified Capabilities

- `standard-snapshot-versioning`: 标准快照创建支持 idempotency key 和项目级写入锁。
- `reverse-import`: 反向导入确认支持 idempotency key 和项目级写入锁。
- `ai-batch-delivery-package`: 批量 SQL lint 运行记录支持 idempotency key 和项目级写入锁。
- `project-backup-restore`: 项目恢复 apply 支持 idempotency key 和项目级写入锁。
- `ai-decision-replay`: AI job 回放记录按稳定指纹去重。
- `dataspec-cli`: CLI 写入命令可透传 idempotency key。

## Impact

- 后端：新增 `idempotency` 包和少量 controller/service 接入点；复用现有 `BizException`/`ErrorCatalog` 生成 AI 可读错误。
- CLI：新增通用 header 注入和参数解析；保持原有命令兼容。
- 测试：新增后端幂等服务单测、目标服务接入单测和 CLI header 透传测试；继续使用 `mvn test`、`node --test tools/dataspec-cli.test.mjs` 和 OpenSpec 校验。
- 风险：内存幂等缓存只覆盖单 JVM 生命周期；第一版明确作为个人/小团队保护，不宣称跨实例分布式保证。

## Verification Evidence

- 后端：`mvn test` 通过，320 tests，0 failures，0 errors。
- 前端：`pnpm test` 通过，84 tests；`pnpm build` 通过，保留既有 Rolldown/VueUse pure annotation 和 chunk size warning。
- CLI：`node --test tools/dataspec-cli.test.mjs` 通过，62 tests。
- OpenSpec：`npx.cmd openspec validate --all` 通过，77 items。
- Diff 检查：`git diff --check` 退出码 0，仅有既有 CRLF 换行提示。
- 本地结构化代码评审：发现事务方法里成功缓存和锁释放早于事务提交，可能导致提交失败后缓存误判或提交窗口内重复写入；已改为事务提交成功后缓存，并在事务完成后释放项目级 operation 锁，新增提交/回滚/无 key 事务锁用例覆盖。
