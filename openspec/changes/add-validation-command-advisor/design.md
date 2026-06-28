## Context

README 已集中列出后端、前端、CLI/MCP、本地 smoke、Docker Compose 和 OpenSpec 的验证命令，但它是一段静态说明。实际开发时，AI agent 需要根据本轮改动路径快速决定最小验证集，并能把建议写入执行证据包或交接说明。现有 `dataspec doctor` 偏运行环境自检，不适合承担“代码变更验证选择器”的职责。

## Goals / Non-Goals

**Goals:**
- 提供一个无需后端启动的本地 Node 脚本，根据路径推荐验证命令。
- 输出 text/json 两种格式，JSON 字段稳定，适合 AI 读取。
- 覆盖后端、前端、CLI/MCP、OpenSpec、README/TODO/docs、Docker/local smoke 的第一版规则。
- 让 README 和 `dataspec init --with-agents` 生成片段指向该入口。

**Non-Goals:**
- 不自动执行验证命令，不替代 CI。
- 不分析 git diff 内容语义，只按文件路径和少量命令行参数推荐。
- 不做耗时估算的动态采样，第一版使用保守静态估计。
- 不新增后端 API、数据库表或前端页面。

## Decisions

1. **新增独立脚本，而不是塞进主 CLI。**
   - 选择：`tools/dataspec-verify-advisor.mjs`。
   - 原因：验证建议主要用于本仓库开发，不需要 DataSpec server，也不应让业务仓库 CLI 命令变得更重。
   - 备选：给 `tools/dataspec-cli.mjs` 增加 `verify-plan` 子命令；缺点是主 CLI 已很大，且命令面向业务仓库会混淆“验证 DataSpec 项目本身”和“验证业务仓库 SQL”。

2. **规则使用声明式路径匹配。**
   - 每条规则包含 path pattern、推荐命令、原因、预计耗时和失败后的下一步。
   - 始终追加 `git diff --check`，作为跨文件基础检查。
   - 多条路径命中同一命令时去重并合并原因。

3. **JSON 输出固定为 advice contract。**
   - 输出包含 `kind`、`schemaVersion`、`inputPaths`、`commands`、`summary`、`nextActions`。
   - `commands[]` 使用稳定字段：`id`、`command`、`cwd`、`reason`、`estimatedSeconds`、`category`。

4. **AGENTS 片段只提示入口，不复制完整规则。**
   - 原因：规则应集中维护在脚本和测试里，避免 init 片段与 README 漂移。

## Risks / Trade-offs

- 规则过宽导致验证偏重 → 第一版优先安全，命中前端源代码时推荐 `pnpm test` 与 `pnpm build`；后续可按耗时数据细分。
- 规则过窄导致漏验证 → 通过 Node 单测覆盖典型路径，并默认保留 `git diff --check`。
- JSON 被下游依赖后难以修改 → 使用 `schemaVersion: 1`，新增字段保持兼容。
- Windows/Linux 路径分隔差异 → 输入路径统一转换为 `/` 再匹配。
