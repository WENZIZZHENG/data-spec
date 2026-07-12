## Context

`docker-compose.local.yml` 使用 `node:22-bookworm` 和 Corepack；当前镜像中的 Corepack 解析并下载 pnpm 11.12.0，但仓库没有固定 pnpm 精确版本。当前前端 lockfile 的 198 个 `resolution.tarball` 指向 `registry.npmmirror.com`，而容器按 npm 官方 registry 元数据执行供应链策略校验，稳定返回 `ERR_PNPM_TARBALL_URL_MISMATCH` 并退出。数据库和后端不受影响，但本地一键启动契约无法完成。

该变更属于部署 hotfix：必须保留 pnpm 的供应链校验、现有冻结 lockfile 安装和三服务 Compose 结构，且不能把个人镜像源配置写入团队共享启动包。

## Goals / Non-Goals

**Goals:**

- 让干净 Docker 依赖卷中的 `pnpm install --frozen-lockfile` 通过 pnpm 11 供应链校验。
- 让 lockfile 不依赖生成者本机的 npm mirror 配置。
- 用现有本地启动包测试入口阻止 mirror tarball URL 再次进入 lockfile。
- 完成 PostgreSQL、后端、前端和 demo smoke 的真实 Docker 验证。

**Non-Goals:**

- 不关闭或放宽 pnpm 供应链策略。
- 不固定团队必须使用某个 npm mirror。
- 不升级业务依赖、不改变 Compose 服务拓扑、不调整 API 或数据库模型。
- 不把本地开发 Compose 扩展为生产部署方案。

## Decisions

### 1. 规范化 lockfile tarball 主机，而不是配置镜像源或重新解析版本

将 lockfile 中 `registry.npmmirror.com` tarball 主机机械替换为 npm 元数据返回的 `registry.npmjs.org`，保持 package version、integrity、peer snapshot 和 importer 不变。一次性容器验证表明，仅设置 `NPM_CONFIG_REGISTRY=https://registry.npmmirror.com` 仍会触发同一策略失败，因此不能解决可移植性问题；执行 `pnpm clean --lockfile` 会按宽松 semver 重新解析并升级 Vue、Vite 等依赖，也不符合 hotfix 边界。

备选方案是关闭策略或降低 pnpm 版本。该方案会掩盖供应链异常并使 Docker 行为依赖旧工具版本，因此拒绝。

### 2. Compose 继续使用冻结安装

保留 `pnpm install --frozen-lockfile`。启动时自动修改 lockfile 会让容器行为不可复现，也可能悄悄升级依赖。

### 3. 在已有 local smoke 契约测试中检查 lockfile 来源

扩展 `tools/dataspec-local-smoke.test.mjs`，只检查 `resolution.tarball` 位置是否包含本次已知会被拒绝的 `registry.npmmirror.com` URL。该检查轻量、可接入现有 `node --test tools/*.test.mjs` 入口，并能在 Docker 拉镜像前给出确定性失败；其他 registry 的兼容性仍由真实 pnpm frozen install 验证。

## Risks / Trade-offs

- [Risk] 机械替换错误路径可能破坏 tarball URL → 仅替换 URL 主机，保留路径和 integrity，并用 pnpm 11 frozen install 的供应链校验验证结果。
- [Risk] 官方 registry 网络在部分环境较慢 → 依赖缓存 volume 保持不变；用户仍可配置下载代理，但共享 lockfile 保持 canonical。
- [Risk] 测试只拦截已知 mirror 域名 → 同时以真实干净 Docker 安装作为交付验证，后续发现其他污染模式再扩展确定性规则。
- [Risk] `node:22-bookworm` 和 Corepack 未固定 pnpm 精确版本，未来策略可能变化 → 保留 frozen install、回归测试和真实 Compose smoke 作为门禁；工具链精确固定作为独立部署决策，不在本 hotfix 扩张。

## Migration Plan

1. 先增加 lockfile canonical registry 回归测试并确认在当前仓库失败。
2. 仅规范化 lockfile tarball URL 主机，确认依赖版本、integrity 和 importer 没有漂移。
3. 运行目标 Node 测试、Compose 配置解析和 OpenSpec strict 校验。
4. 删除并重建前端依赖 volume，执行真实 Compose 启动和 demo smoke。
5. 回滚时恢复原 lockfile 和测试即可；Compose 服务、数据库 volume 与接口均无需迁移。

## Open Questions

无。当前失败已在干净 Docker 环境稳定复现，修复边界明确。
