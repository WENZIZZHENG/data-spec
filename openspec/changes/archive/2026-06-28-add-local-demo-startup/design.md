## Context

当前 README 说明了手动启动 PostgreSQL、后端和前端的步骤，但本地新机器仍要自己创建数据库、串命令、确认端口和手动点击演示项目。项目已有 `/api/projects/demo`、Dashboard 演示入口和 CLI doctor，可复用为一键启动后的 smoke 验证。

## Goals / Non-Goals

**Goals:**

- 提供可复制的本地 Docker Compose，覆盖 PostgreSQL、后端和前端。
- 提供本地 smoke 脚本，等待服务可用、创建或复用演示项目，并验证 dashboard 和 lint 链路。
- 输出人类可读结果和 JSON 结果，方便 AI agent 判断环境是否可用。
- README 说明一键启动、开发模式和清理方式。

**Non-Goals:**

- 不做生产部署方案、高可用、TLS、远程密钥管理或 Kubernetes。
- 不保存数据库密码以外的新敏感配置；本地 compose 使用默认开发账号。
- 不改变演示项目 API 或正式数据模型。
- 不要求普通 `mvn test` / `pnpm test` 依赖 Docker。

## Decisions

1. **Compose 覆盖三件套，脚本负责 smoke。**

   `docker-compose.local.yml` 使用 PostgreSQL 官方镜像、Maven 镜像运行后端、Node 镜像运行前端，适合新机器快速试用。smoke 独立成 Node 脚本，既可配合 compose，也可验证手动开发模式。

2. **前端代理目标用环境变量覆盖。**

   Vite 现有 proxy 写死 `http://localhost:8090`，容器内前端需要访问 `http://server:8090`。保留默认值用于本地 `pnpm dev`，在 compose 中用 `VITE_PROXY_TARGET` 覆盖。

3. **smoke 调用现有公开 API。**

   脚本只调用 `/api-docs`、`/api/projects/demo`、`/api/dashboard/summary` 和 `/api/lint`，不新增后端 health API。这样能同时验证 Flyway、内置 standards、demo seed、Dashboard 和 lint 核心链路。

4. **JSON 输出不泄漏敏感信息。**

   smoke 输出服务地址、项目 ID、检查结果和问题数，不输出数据库密码、token 或完整连接串。

## Risks / Trade-offs

- [Risk] 首次 compose 会下载 Maven/Node 依赖，启动较慢。→ Mitigation：使用命名 volume 缓存 Maven 仓库、pnpm store 和 node_modules。
- [Risk] 端口 5432/8090/5173 被占用。→ Mitigation：README 和脚本提示端口；compose 支持通过环境变量覆盖对外端口。
- [Risk] Docker 不可用时用户仍需开发模式。→ Mitigation：smoke 脚本不依赖 Docker，可验证手动启动的服务。
- [Risk] `/api/projects/demo` 在安全模式开启时需要全项目 token。→ Mitigation：本地 compose 默认关闭安全模式；脚本支持 `DATASPEC_TOKEN` / `--token` 透传。
