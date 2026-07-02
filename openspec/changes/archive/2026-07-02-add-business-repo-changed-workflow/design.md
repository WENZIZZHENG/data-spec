## Context

P6-52 解决的是 AI 在业务仓库中工作时的入口问题：AI 需要优先理解本次 git 变更、只 lint 相关 SQL 文件，并拿到最小 AI Context 建议，而不是每次扫描全仓或导出完整包。

## Goals / Non-Goals

**Goals:**
- 新增 `dataspec changed`，读取 `.dataspec/config.json`、`defaultPaths` 与 git diff，输出稳定 JSON 和文本摘要。
- 新增 `dataspec lint-changed`，复用 `changed` 的发现结果，只 lint 变更 SQL 文件，并返回 lint 汇总。
- 输出最小 AI Context 建议，包括 `scope=changed`、由变更文件名推导的 query、推荐命令和可恢复诊断。
- 在业务仓库改动少量 SQL 文件后，AI 可一条命令拿到变更文件列表、对应 lint 结果和最小标准上下文；无 git 仓库或无变更时有可恢复提示。

**Non-Goals:**
- 不自动修改业务代码，不自动提交，不扫描未配置的大型目录。
- 不新增后端 API、数据库表或 AI Context 服务端语义。
- 不解析非 SQL 模型文件内容；第一版只把它们作为 Context query 的证据。

## Decisions

1. **CLI-first，不改后端。**
   - 原因：已有 `lint-files`、`export-context --scope changed` 和 `.dataspec/config.json`，P6-52 的缺口主要在业务仓库内的编排入口。

2. **`changed` 和 `lint-changed` 分层。**
   - `changed` 不调用后端，只做文件发现、分类和建议。
   - `lint-changed` 在 SQL 文件非空时调用现有 `/api/lint`，输出同一份发现结果加 lint 汇总。
   - 原因：AI 可先低成本获取上下文，也可在需要时执行检查。

3. **只在配置范围内匹配变更。**
   - `defaultPaths` 为空时不降级全仓扫描，而是提示运行 `dataspec init --default-path <path>` 或传入配置。
   - 原因：避免大型业务仓库被意外扫描，符合 TODO 边界。

4. **机器可读优先。**
   - JSON 输出包含 `kind`、`schemaVersion`、`git`、`config`、`files`、`summary`、`contextRecommendation`、`lint`、`nextActions` 和 `diagnostics`。
   - 原因：这是给 AI 使用的稳定入口，文本只是人类摘要。

## Risks / Trade-offs

- [Risk] git 命令在没有 git 或非仓库目录下失败。→ Mitigation：捕获失败并输出 `NO_GIT_REPOSITORY` 诊断，命令返回 0，让 AI 能读取恢复建议。
- [Risk] 新增文件尚未纳入 diff。→ Mitigation：同时读取 tracked diff 与 untracked 文件，并按 `defaultPaths` 过滤。
- [Risk] 变更文件名不一定能精准定位字段标准。→ Mitigation：第一版只生成 query 建议和 `export-context --scope changed --query <text>` 命令，不承诺自动导出上下文。

## Open Questions

- 无。
