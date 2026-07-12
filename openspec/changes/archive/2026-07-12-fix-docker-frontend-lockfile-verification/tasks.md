## 1. 回归保护

- [x] 1.1 扩展本地启动包测试，检测前端 lockfile 中本次已知会被 pnpm 11 拒绝的 `registry.npmmirror.com` tarball URL。
- [x] 1.2 运行目标测试并确认它因当前 `registry.npmmirror.com` lockfile 条目按预期失败。

## 2. 最小修复

- [x] 2.1 将 lockfile tarball URL 规范化为 npm 官方 canonical 主机，不升级业务依赖、不关闭供应链校验。
- [x] 2.2 核对 lockfile importer、依赖版本和 Compose 冻结安装命令没有无关漂移。

## 3. 验证与收口

- [x] 3.1 运行本地启动包目标测试、tools 统一测试、Compose 配置解析和 OpenSpec strict 校验。
- [x] 3.2 用干净前端依赖 volume 启动 PostgreSQL、后端和前端，并运行 demo smoke。
- [x] 3.3 完成实现规范自查、独立子 agent 评审和 findings 处理。
- [x] 3.4 在本文件记录 Verification Evidence，检查 staged diff 和敏感项后创建本地 commit。

## Verification Evidence

- 失败复现：首次 `docker compose -f docker-compose.local.yml up -d` 的前端容器退出码为 1，pnpm 11.12.0 报 `ERR_PNPM_TARBALL_URL_MISMATCH`，198 个 `registry.npmmirror.com` tarball 与 npm 官方元数据不一致。
- TDD 红灯：新增回归测试后运行 `node --test tools/dataspec-local-smoke.test.mjs`，7 pass / 1 fail，失败项为 `frontend lockfile uses canonical registry tarballs`，直接命中 mirror URL。
- lockfile 最小性：反向等价检查确认旧 lockfile 有 198 个 mirror tarball，修复后有 198 个 canonical tarball；将主机反向替换后与 `HEAD` 逐字一致，版本、integrity、importer 和 snapshot 无漂移。
- 供应链验证：容器内执行 `pnpm install --frozen-lockfile`，198 个条目全部通过 supply-chain policies；安装版本保持 Vue 3.5.38、Vite 8.0.16、Element Plus 2.14.2 等原锁定版本。
- 目标测试：`node --test tools/dataspec-local-smoke.test.mjs` 8 pass / 0 fail；旧 lockfile 对收窄后的 tarball regex 命中 198 条，修复后命中 0 条。
- 统一 tools 测试：`node --test tools/*.test.mjs` 共 433 项，431 pass / 2 skipped / 0 fail；跳过项为当前 Windows 环境无法创建 symlink 的既有条件测试。
- 前端验证：运行中 web 容器内 `pnpm test` 为 187 pass / 0 fail；`pnpm build` 通过，保留既有 Rolldown pure annotation、plugin timing 和 chunk size warning。
- Docker 验证：`docker compose -f docker-compose.local.yml config --quiet` 通过；本机 `5432` 被现有 PostgreSQL 占用，因此以 `DATASPEC_DB_PORT=15432` 启动，PostgreSQL healthy、后端 `8090`、前端 `5173` 均持续运行。
- Demo smoke：`node tools/dataspec-local-smoke.mjs --server http://localhost:8090 --web http://localhost:5173 --json --timeout-ms 300000` 返回 `ok=true`，web、api-docs、demo-project、dashboard-summary、sql-lint 五项均 pass，创建演示项目 `projectId=1`。
- OpenSpec：`openspec validate fix-docker-frontend-lockfile-verification --strict` 通过；`openspec validate --all` 为 131 passed / 0 failed。
- 通用检查：`git diff --check` 通过；`node tools/dataspec-status-check.mjs --format json` 无 error，仅报告 7 个按项目约定保留的 active change warning。
- 独立评审：子 agent `019f551e-7aa8-7090-8f71-d37f465a222c` 已完成并关闭，无 Critical，3 个 Important 均已处理：回归范围收窄到 tarball 字段中的已知 npmmirror 污染、修正 pnpm 版本来源描述并记录未固定风险、补齐本节证据。
- 剩余风险：`node:22-bookworm` 与 Corepack 未固定 pnpm 精确版本，未来供应链策略可能变化；本变更通过 frozen install、目标回归和真实 Compose smoke 守住当前契约，精确固定工具链需作为独立部署决策评估。
