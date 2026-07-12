## Why

当前 PostgreSQL schema 在 V2 之后重新引入了多组 `timestamp with time zone` 列，而对应 Java 实体仍使用 `LocalDateTime`。PostgreSQL JDBC 42.7.x 会拒绝把这些列读取为 `LocalDateTime`，已在 Docker 演示库中导致 SQL 检查记录和标准变更日志查询失败。

本地 Compose 同时存在三项确定性缺口：关闭鉴权的服务和弱口令数据库默认绑定所有网卡，Node/Corepack 会解析浮动 pnpm 版本，后端与前端没有健康状态；smoke 的 `--timeout-ms` 也不能中止卡住的 HTTP 请求。这些问题会让本地一键启动在不同机器上表现不一致，并扩大本地开发配置的暴露面。

该变更涉及数据库迁移、安全边界和部署行为，按 SDD full 执行。

## What Changes

- 新增 Flyway 迁移，将所有由 `LocalDateTime` 承载的 PostgreSQL `timestamptz` 列规范化为 `timestamp without time zone`，并恢复 `localtimestamp` 默认值。
- 为真实 PostgreSQL 迁移增加集成回归，验证应用时间列均可由 `LocalDateTime` 稳定读取。
- Compose 默认仅将 PostgreSQL、后端和前端绑定到 `127.0.0.1`，数据库宿主端口改为 `15432`，仍允许通过环境变量显式覆盖 bind host、安全开关和端口。
- 固定 Docker Node patch 版本和 pnpm 精确版本，声明前端 Node engine，继续使用 frozen lockfile 与供应链校验。
- 为后端和前端增加 healthcheck，使前端等待后端健康，并支持 `docker compose up -d --wait`。
- 为 smoke 的等待请求和业务请求增加真实可中止超时，防止连接建立后无响应时永久挂起。
- 更新 README、本地启动契约测试和 OpenSpec 验证证据。

## Capabilities

### New Capabilities

- `timestamp-storage-compatibility`：数据库时间列与 Java `LocalDateTime` 的持久化类型保持一致，历史 schema 可通过迁移安全规范化。

### Modified Capabilities

- `local-demo-startup`：本地服务默认只监听 loopback，工具链可复现，Compose 报告真实健康状态，smoke 超时能够终止挂起请求。

## Impact

- 数据库：新增 Flyway 迁移，修改现有时间列类型和默认值，不删除业务记录。
- 后端：新增 PostgreSQL 集成迁移测试，不改变 API route、请求或响应 schema。
- 前端工具链：固定 Node 22 patch 与 pnpm 11.12.0，不升级业务依赖。
- 本地部署：PostgreSQL 默认宿主端口由 `5432` 调整为 `15432`；服务默认仅能从本机访问；Compose project name 不再跨 worktree 固定共享。
- tools：smoke 保持现有参数和 JSON 输出结构，只修正 timeout 的实际行为。
