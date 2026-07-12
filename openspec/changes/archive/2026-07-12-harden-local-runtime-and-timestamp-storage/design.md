## Context

V1 创建的核心表最初使用 `timestamp with time zone`，V2 已明确把 Java `LocalDateTime` 对应列迁移为 `timestamp without time zone`。后续 V4-V14、V29-V30 的迁移没有延续该约定，当前演示库仍有 16 张表包含 `timestamptz` 应用列。实体和 DTO 普遍使用 `LocalDateTime`，PostgreSQL JDBC 不执行隐式的有时区到无时区转换，因此查询直接失败。

Compose 面向个人本地开发并显式关闭安全校验，但 Docker `ports` 的短格式默认绑定 `0.0.0.0`。这使“本地关闭鉴权”的假设与实际网络暴露不一致。前端镜像使用浮动 `node:22-bookworm`，且仓库没有 `packageManager` 声明，Corepack 的 pnpm 选择也会随镜像变化。后端和前端缺少 healthcheck，smoke 虽有 timeout 参数，但单次 `fetch` 无截止时间。

## Goals / Non-Goals

**Goals:**

- 让所有应用拥有的 PostgreSQL 时间列与现有 `LocalDateTime` 模型一致，并保留历史记录的本地墙钟值。
- 让本地 Compose 默认只从本机访问，同时保留显式覆盖能力。
- 固定前端容器的 Node/pnpm 工具链，不改变锁定依赖。
- 让 Compose 健康状态表示应用真实可访问，让 smoke 的 timeout 对挂起请求生效。

**Non-Goals:**

- 不把全项目时间模型改为 `Instant` 或 `OffsetDateTime`，不改变 API 时间字段格式。
- 不把本地 Compose 改造成生产部署方案，不默认开启 token 管理。
- 不构建自定义生产镜像，不升级 Vue、Vite 或其他业务依赖。
- 不改变 demo smoke 的检查集合或机器可读结果 schema。

## Decisions

### 1. 继续采用 `LocalDateTime` + `timestamp without time zone`

沿用 V2 已建立且所有实体、JSON 契约和测试依赖的时间模型。新增迁移显式使用 `AT TIME ZONE 'Asia/Shanghai'` 将 `timestamptz` 转为应用在 `application.yml` 中声明的权威本地墙钟值，避免 Flyway/JVM/database session timezone 造成漂移；默认值改为 `localtimestamp`。相比批量把 Java 模型改为 `OffsetDateTime`，该方案不会引发 API、前端和历史 fixture 的跨模块 breaking change。

迁移必须列出明确表和列，不使用运行时动态 SQL 扫描整个 schema，避免修改第三方表或未来语义不同的时间列。

### 2. 用真实 PostgreSQL 集成测试守住迁移契约

单元测试和 H2 无法复现 PostgreSQL JDBC 的类型拒绝。新增 Testcontainers/Flyway 集成测试，先迁移到 V30 并在 UTC session 下写入带 `+08:00` offset 的历史记录，再执行 V31，检查墙钟值、可空列、最终列类型和 `LocalDateTime` 读取。测试接入现有 `db-integration` profile。本地复用外部 PostgreSQL 时必须显式 opt-in、提供账号密码、使用固定的一次性数据库名并保证 public schema 为空，防止测试误迁移开发库或共享库。

### 3. loopback 是关闭本地鉴权的必要边界

Compose 三个宿主端口统一使用 `${DATASPEC_BIND_HOST:-127.0.0.1}`。需要局域网演示时必须显式设置 bind host，并可通过 `${DATASPEC_SECURITY_ENABLED:-false}` 同时开启鉴权；README 同时提示口令、防火墙和安全模式边界。数据库默认宿主端口改为 `15432`，避免与开发机 PostgreSQL 的常见 `5432` 冲突，容器内连接仍使用 `postgres:5432`。移除固定 project name，避免不同 worktree 默认复用同一不可逆迁移数据卷。

### 4. 固定执行工具，不重算依赖

前端 `package.json` 声明 Node 22 engine 和 `packageManager: pnpm@11.12.0`，Compose 使用当前已验证的 `node:22.23.1-bookworm`。继续执行 `pnpm install --frozen-lockfile`，不重新解析版本，也不关闭供应链校验。

### 5. 健康检查和 smoke timeout 分层负责

Compose 使用镜像已有的 `curl` 检查后端 `/api-docs` 和前端 `/`，并让 web 等待 server healthy。README 使用 `up -d --wait` 返回可操作的启动结果。

smoke 在每次 HTTP 调用外包一层 deadline：同时触发 `AbortController` 和 Promise timeout，既释放真实 fetch 连接，也保证测试替身忽略 signal 时调用方仍会按时返回。等待轮询和 demo API 请求都复用该边界。

## Risks / Trade-offs

- [Risk] 时间列转换丢失原始 offset 信息 -> 当前 Java/API 从未暴露 offset，迁移统一保留 `Asia/Shanghai` 应用时区下的墙钟值；V30→V31 测试强制 Flyway 使用 UTC session 并验证转换不受 session timezone 影响。
- [Risk] 大表类型转换持锁 -> 当前项目定位个人/小团队且这些表为应用记录表；迁移逐表执行并记录回滚边界，不对源数据库执行任何写入。
- [Risk] 默认端口变化影响旧命令 -> README、Compose 契约测试和 OpenSpec 同步更新，仍允许 `DATASPEC_DB_PORT=5432` 显式恢复。
- [Risk] 固定 Node patch 需要后续安全升级 -> 精确版本换取首次安装可复现性；升级通过独立依赖维护变更完成。
- [Risk] healthcheck 拉长冷启动等待 -> 设置 `start_period` 覆盖 Maven 首次启动，`--wait` 只在真实可用后返回。
- [Risk] loopback 阻止局域网演示 -> 保留显式 `DATASPEC_BIND_HOST` 与 `DATASPEC_SECURITY_ENABLED`，但不允许关闭鉴权的共享暴露成为默认值。
- [Risk] 移除固定 project name 后旧 volume 不会自动附着 -> 旧 `dataspec-local_*` volume 保留且不删除；需要延续旧本地数据时可显式设置 `COMPOSE_PROJECT_NAME=dataspec-local`，新 worktree 默认隔离。

## Migration / Rollback

1. 在现有 V30 之后新增单向 Flyway 迁移，逐列转换类型并恢复默认值。
2. 在一次性 PostgreSQL 容器中从空库执行全量迁移，验证无 `timestamptz` 应用列残留。
3. 在现有 Docker volume 上执行迁移，验证历史演示记录可由相关 API 读取。
4. 代码回滚不能自动撤销已执行的 Flyway 迁移；如必须恢复旧 schema，需要先确认目标时区，再用受控 SQL 转回 `timestamptz`。由于旧应用本身无法读取这些列，默认回滚策略是保留规范化 schema 并回滚应用与 Compose 文件。

## Verification Strategy

- TDD 红灯：真实 PostgreSQL 迁移测试在 V31 缺失时报告残留 `timestamptz` 列；smoke 挂起请求测试在 timeout 修复前超时失败。
- 后端：目标迁移集成测试、`mvn test`、`mvn test -Pdb-integration`。
- 前端：容器内 frozen install、`pnpm test`、`pnpm build`。
- tools：本地启动目标测试和 `node --test tools/*.test.mjs`。
- Docker：`docker compose config --quiet`、干净重建、`up -d --wait`、demo smoke 和端口绑定检查。
- OpenSpec：change strict、archive 后 `openspec validate --all`。
- 评审：安全、迁移、部署和完整 diff 的独立子 agent 评审，所有 findings 修复或记录技术理由。
