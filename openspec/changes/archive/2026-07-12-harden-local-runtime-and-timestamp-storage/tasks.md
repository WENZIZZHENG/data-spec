## 1. 数据库时间兼容

- [x] 1.1 增加真实 PostgreSQL 迁移回归测试，确认完整迁移后不存在由 `LocalDateTime` 承载的 `timestamptz` 列，并观察预期红灯。
- [x] 1.2 新增 Flyway 迁移，明确规范化受影响表的时间列类型与默认值，保留现有墙钟值。
- [x] 1.3 在空库和现有 Docker 演示库验证迁移，并复测此前失败的 SQL 检查记录与变更日志读取。

## 2. Docker 安全与可复现工具链

- [x] 2.1 先扩展本地启动契约测试，覆盖 loopback、默认 `15432`、精确 Node/pnpm 和健康依赖，并观察预期红灯。
- [x] 2.2 更新 Compose 端口绑定、工具链、healthcheck、服务依赖和 web 信号转发。
- [x] 2.3 更新前端 Node engine、packageManager 与 README 启动/覆盖/安全边界说明。

## 3. Smoke 真实超时

- [x] 3.1 增加挂起等待请求和挂起业务请求的 timeout 回归测试，并观察预期红灯。
- [x] 3.2 为所有 smoke HTTP 请求实现可中止 deadline，保持参数和结果 schema 兼容。
- [x] 3.3 运行 tools 目标测试，确认超时、脱敏和健康路径全部通过。

## 4. 验证、评审与归档

- [x] 4.1 运行后端、前端、tools、Docker 和 OpenSpec 接近全量验证，记录 Verification Evidence。
- [x] 4.2 完成实现规范、迁移回滚、安全边界、公共字段说明和无关改动自查。
- [x] 4.3 启动独立子 agent 评审迁移、安全、部署、逻辑和测试，修复或记录全部 findings，并关闭 agent。
- [x] 4.4 按提交顺序同步并归档 7 个既有完成 change 和本 change，运行 archive 后全量 OpenSpec 校验。
- [x] 4.5 检查 staged diff 与敏感项，创建本地 commit，不 push。

## Verification Evidence

- 根因复现：现有 Docker V30 演示库读取 `ds_standard_change_log.changed_at` 和 `ds_sql_check_record.created_at` 时，PostgreSQL JDBC 报 `Cannot convert the column of type TIMESTAMPTZ to requested type java.time.LocalDateTime`；information_schema 确认 28 个应用列不兼容。
- 数据库 TDD 红灯：首次 V1→V30 真实 PostgreSQL 测试为 1 failure / 1 error，分别报告 28 个残留列和代表性 `changed_at` 类型转换失败；升级测试强制 Flyway UTC session 后，旧 V31 把 `12:34:56+08:00` 错转为 `04:34:56`，按预期失败。
- 数据库目标验证：在固定名称、空 public schema、显式 opt-in 的一次性 PostgreSQL 17 数据库运行 `mvn -Pdb-integration "-Dtest=TimestampStorageMigrationIT" test`，先迁移到 V30、写入带 offset 历史值和 nullable Token 时间，再迁移 V31；1 test / 0 failures，确认 `Asia/Shanghai` 墙钟值、nullable、LocalDateTime 读取和 0 个残留 `timestamptz` 列。
- Docker/工具链 TDD：Compose 契约先因缺少 loopback、默认 15432、精确 Node/pnpm、healthcheck 和 6 分钟冷启动宽限失败；修复后目标测试 12 pass。结构化测试通过 `docker compose config --format json` 验证服务归属、健康依赖和 `DATASPEC_SECURITY_ENABLED=true` 覆盖。
- timeout TDD：两个挂起 fetch 测试在旧实现中均达到 500ms 测试上限并取消；修复后分别约 20ms 返回，新增测试确认底层 `AbortSignal` 恰好触发一次。
- 后端全量：`mvn test` 为 674 tests / 0 failures / 0 errors；保留既有本地 Maven `javax.annotation-api` POM warning、JDK dynamic agent warning 和性能基线日志。
- 前端全量：`pnpm install --frozen-lockfile` 使用 pnpm 11.12.0 且 lockfile 不变；`pnpm test` 为 187 pass / 0 fail；`pnpm build` 通过，保留既有 Rolldown annotation、plugin timing 和 chunk size warning。
- tools 全量：`node --test tools/*.test.mjs` 为 437 total / 435 pass / 2 skipped / 0 fail；跳过项为 Windows 无法创建 symlink 的既有条件测试。
- Docker 真实启动：移除固定 project name 后，旧 `dataspec-local_*` volumes 保留未删除；全新 `data-spec_*` volumes 执行 `docker compose up -d --wait`，PostgreSQL、server、web 均 healthy，端口仅绑定 `127.0.0.1`，Node 22.23.1、pnpm 11.12.0。空库 Flyway 到 V31，应用表 `timestamptz` 计数为 0。
- Docker smoke：`node tools/dataspec-local-smoke.mjs --json --timeout-ms 300000` 返回 `ok=true`，web、api-docs、demo-project、dashboard-summary、sql-lint 五项通过；web 直接运行 Vite 后 SIGTERM 为标准退出码 143，不再输出 pnpm `ELIFECYCLE`。
- OpenSpec：`openspec validate harden-local-runtime-and-timestamp-storage --strict` valid；archive 前 `openspec validate --all` 为 132 passed / 0 failed。
- Archive：按实现顺序归档 7 个既有完成 change 和本 change，active change 归零；补齐 11 个归档生成主规格的 Purpose 后，`openspec validate --all` 为 135 passed / 0 failed，`node tools/dataspec-status-check.mjs --format json` 为 pass / 0 issue。
- 独立评审：迁移/安全 agent `019f5642-2750-7aa0-abdd-621ebda8bba5` 与 runtime/OpenSpec agent `019f5642-5bfe-70f1-95ca-1c780d0cea4f` 均完成并关闭；发现的显式时区、升级测试、外部库保护、安全开关、project name、结构化 Compose 测试、abort 断言和 README 范围问题均已修复。终审 agent `019f565a-dc78-7c03-9491-b45b72bd5ff8` 首轮指出外部 currentSchema 绕过和文档残留，修复为固定/校验 public schema 后复核结论 Ready，无剩余 finding，已关闭。
- Archive 独立复评：agent `019f5667-8bac-7441-ad45-baf6646063fc` 检查 8 个 archive、主规格、Purpose、TODO 和 active 状态后已关闭；发现最终本地启动规格遗漏“不得禁用或放宽供应链策略”和 commit 任务尚未完成。前者已同步回主规格和归档 delta，后者在 staged diff 与敏感项检查后勾选。
- 已知验证限制：本机 Testcontainers 1.20.4 的 Windows npipe provider 返回 Docker API 400，因此未重复运行包含 MySQL 的整个 `db-integration` profile；本次新增迁移测试通过显式受保护的外部一次性 PostgreSQL 运行，既有 PostgreSQL/MySQL metadata IT 未受实现文件影响。
- 剩余运行边界：旧 `dataspec-local_*` volumes 为避免破坏性删除而保留；需要复用旧本地数据时显式设置 `COMPOSE_PROJECT_NAME=dataspec-local`，但该旧演示卷已执行过开发过程中的 V31 草案，仅作为保留备份，不作为最终验证证据。
