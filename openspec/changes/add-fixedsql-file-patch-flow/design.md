## Context

当前 `tools/dataspec-cli.mjs` 已支持 `lint`、`lint-files`、AI batch delivery package、task-card 和 evidence export。SQL lint 响应可包含 `fixedSql` 与 `fixedSqlDiff`，但 CLI 只负责展示或打包，不负责把修复安全落到业务仓库文件。P6-78 的核心风险不是 SQL 生成，而是本地文件写入：AI 需要先输出可审查补丁计划，用户确认后才允许写入，并且不能在文件内容已变化时静默覆盖。

## Goals / Non-Goals

**Goals:**
- 新增本地 CLI 能力 `fixed-sql patch`，默认 dry-run 生成补丁计划。
- 补丁计划提供稳定 JSON：`unifiedDiff`、`conflictWarnings`、`dryRunResult`、`applyCommand`、`rollbackHint`、`evidenceRef`、`planHash`。
- 只有显式 `--apply --confirm <planHash>` 才写目标 SQL 文件。
- 拒绝目标路径越界、符号链接路径、缺少 `fixedSql`、缺少 lint 原文或匹配 hash、补丁无变化、当前文件内容与 lint 原文不匹配或确认 hash 不匹配。
- 通过 Node 单测和 CLI/MCP contract fixture 覆盖 AI-facing 输出形状和安全 metadata。

**Non-Goals:**
- 不新增后端 API、数据库表或 OpenAPI schema。
- 不自动提交业务仓库，不调用 Git 写入，不替代人工 code review。
- 不尝试复杂 SQL AST 合并；第一版只做单文件整段替换或小批量 lint item 里的单文件目标。
- 不把 raw token、password、JDBC URL、DSN 或完整连接串写进补丁计划、错误信息或 fixture 示例。

## Decisions

1. **CLI-only 第一版**
   - 方案：在现有 CLI 中新增 `fixed-sql patch` 子命令，输入 lint JSON 文件与目标 SQL 文件。
   - 理由：P6-78 的主要价值是业务仓库本地文件补丁；CLI 离目标文件最近，能直接复用 cwd 路径边界保护，也避免新增服务端文件写入能力。
   - 替代方案：前端上传/下载 patch；会增加浏览器文件授权和 UX 复杂度，但不能直接安全写入业务仓库。后端 API 写文件则越过本地仓库边界，不适合作为第一版。

2. **默认 dry-run，apply 需要 planHash**
   - 方案：不传 `--apply` 时只输出计划；apply 时必须同时传 `--confirm <planHash>`，hash 基于目标相对路径、当前文件 hash、fixedSql hash 和 diff 摘要计算。
   - 理由：AI 可以把 planHash 呈现给用户或任务卡；确认 hash 可防止旧计划在文件变化后被复用。
   - 替代方案：只用 `--yes`；可读性强但无法防止旧计划漂移。

3. **整段替换优先，内容匹配防漂移**
   - 方案：从 lint JSON 中读取 `originalSql`、`sql`，或读取与目标内容匹配的 `originalSqlSha256` / `sqlSha256` / `sourceSqlSha256` / `currentFileSha256`。`lint-files` 的 `files[]` 只接受 `path` 与目标文件匹配的 item；生成计划时对比目标文件当前内容。只有当前文件仍等于 lint 原文或 hash 能证明目标内容就是 lint 原文时才允许 apply。
   - 理由：不依赖脆弱行号；文件已变时直接阻断，避免把 fixedSql 应到错误位置。
   - 替代方案：基于 source range 做局部 patch；更精细，但需要每个 lint 结果都有稳定范围与原文片段，第一版风险较高。

4. **输出形状稳定并脱敏**
   - 方案：补丁计划 JSON 不输出完整 sensitive diagnostics；错误通过现有 `formatCliError` 脱敏，fixture 示例使用无密钥 SQL。
   - 理由：CLI 输出会被 AI、CI 和任务卡复制，必须默认可安全传播。

## Risks / Trade-offs

- **Risk: 整段替换过粗，可能不适合大型 SQL 文件。** → Mitigation：第一版只在当前文件内容与 lint 原文一致时应用；否则输出冲突并让用户手工处理。
- **Risk: lint JSON 来源多样，字段名可能不同。** → Mitigation：支持单次 lint 输出和 `lint-files` 的 `files[]` item；`files[].path` 必须匹配目标文件，缺少原文或匹配 hash 时返回参数错误。
- **Risk: AI 误用 apply。** → Mitigation：默认 dry-run、apply 要求 `--confirm <planHash>`，输出 `applyCommand` 明确包含确认 hash。
- **Risk: 本地路径越界。** → Mitigation：复用/扩展 cwd 内路径解析，目标和 lint JSON 默认必须位于当前工作目录，并通过 realpath 校验真实路径、拒绝符号链接。

## Migration Plan

- 新增命令为 additive，不改变现有 CLI 命令输出和退出码。
- 新增工具单测和 contract fixture 后，现有 `node --test tools/*.test.mjs` 能覆盖该能力。
- 回滚时删除 `fixed-sql patch` 分支、纯函数和 fixture；不涉及数据库或后端迁移。

## Open Questions

- 暂不支持局部 source-range patch；后续如果 lint result 稳定提供原文范围，可在不破坏当前整段替换模式的前提下新增局部模式。
