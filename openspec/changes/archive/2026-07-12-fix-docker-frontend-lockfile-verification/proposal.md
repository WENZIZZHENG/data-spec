## Why

全新 Docker 环境启动前端时，pnpm 11 会拒绝当前 `pnpm-lock.yaml` 中由 `registry.npmmirror.com` 写入的 tarball URL，因为容器按 npm 官方元数据执行供应链校验，导致 `docker compose up` 无法满足本地一键启动契约。该问题在干净依赖卷中稳定复现，必须修复 lockfile 可移植性，而不是关闭校验。

## What Changes

- 将前端 lockfile 的 tarball URL 规范化为与 npm 官方 registry 元数据一致的 canonical 主机，同时保持依赖版本和 integrity 不变。
- 为本地启动包增加回归检查，阻止本次已知的 `registry.npmmirror.com` tarball URL 再次进入前端 lockfile。
- 验证 Docker Compose 首次安装、前端启动、后端 Flyway 启动和 demo smoke 链路。
- 保持现有 Compose 服务、默认端口、端口覆盖变量和开发期安全边界不变。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `local-demo-startup`：本地 Compose 首次启动必须在启用包管理器供应链校验时成功安装前端锁定依赖并启动前端服务。

## Impact

- 受影响文件：`dataspec-web/pnpm-lock.yaml`、`tools/dataspec-local-smoke.test.mjs`、本 change artifacts。
- 受影响系统：仅本地 Docker Compose 前端依赖安装和对应回归门禁。
- 不改变 API、数据库 schema、CLI/MCP 协议、前端业务行为或生产部署方案。
