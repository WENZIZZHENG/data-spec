## Context

DataSpec CLI 已有 `lint`、`lint-files`、`review-pr`、`export-context`、`suggest-field`、`generate-ddl` 和 `doctor`，并通过 `tools/dataspec-config.mjs` 从当前目录向上读取 `.dataspec/config.json`。AI agent 在业务仓库中使用这些能力前，仍需要人工创建 `.dataspec/config.json`、补默认扫描路径、写使用说明和复制 AGENTS 片段。

P6-4 的定位是个人/小团队的接入脚手架，不引入企业治理流程。第一版优先做参数化初始化，便于 AI agent、CI 或用户复制命令稳定执行。

## Goals / Non-Goals

**Goals:**

- 新增 `dataspec init` CLI 命令，在当前工作目录创建或更新 `.dataspec/` 初始化文件。
- 生成 `.dataspec/config.json`，包含 `projectId`、`server` 和 `defaultPaths`，沿用现有 config loader 格式。
- 生成 `.dataspec/README.md`，说明常用 CLI/MCP 命令、token 使用方式、默认路径和更新方式。
- 可选更新根目录 `AGENTS.md`，插入或替换 DataSpec 管理片段，帮助 AI agent 按项目标准工作。
- 重复执行默认不覆盖已有文件；`--force` 才覆盖 DataSpec 管理文件。
- 初始化完成后自动运行轻量 `doctor`，并支持 text/json 输出。

**Non-Goals:**

- 不做交互式 TUI/多轮问答；后续可在参数化稳定后增加。
- 不修改业务代码，不自动创建 Git commit，不执行数据库写操作。
- 不把明文 API token 写入 `.dataspec/config.json`、README 或 AGENTS；token 继续通过 `DATASPEC_TOKEN`、`--dataspec-token` 或本地忽略配置传递。
- 不自动下载完整 AI Context zip；本轮只生成接入配置和说明。

## Decisions

1. **参数化命令优先，而非交互式向导。**
   - 理由：当前 CLI 已是 AI/CI 调用入口，参数化命令可测试、可复现，也适合 coding agent 自动执行。
   - 替代方案：交互式 prompt。暂不采用，Windows/CI/agent 环境下更难验证，后续可作为增强。

2. **复用 `.dataspec/config.json` 的现有字段。**
   - 理由：`dataspec-config.mjs`、CLI、MCP 和 `doctor` 已围绕 `projectId/server/defaultPaths/apiToken` 工作；`init` 不应引入第二套配置。
   - 取舍：第一版不新增 `tokenMode` 等字段，避免现有 loader 和文档语义漂移。

3. **AGENTS 片段使用受控 marker。**
   - 理由：重复执行需要幂等，不应把同一段指令不断追加到业务仓库 `AGENTS.md`。
   - 方案：写入 `<!-- dataspec-agents:start -->` 和 `<!-- dataspec-agents:end -->` 之间的内容；存在 marker 时可由 `--force` 替换，不存在时追加。

4. **默认安全策略是不覆盖。**
   - 理由：业务仓库可能已有人工配置或项目规范；初始化命令不能误删或覆盖用户内容。
   - 方案：写入前检查目标文件，已有文件默认跳过并在结果里标明；`--force` 覆盖 `.dataspec/config.json`、`.dataspec/README.md` 和 DataSpec marker 片段。

5. **doctor 作为初始化后的反馈，而不是阻断写入。**
   - 理由：用户可能先初始化文件，再启动后端或配置 token；doctor 失败也应给出诊断而不是回滚初始化文件。
   - 方案：`init` 返回写入结果和 doctor 结果；整体退出码沿用 doctor 是否有失败检查。

## Risks / Trade-offs

- [误覆盖业务文档] → 默认跳过已有文件，AGENTS 仅在 marker 范围内替换；`--force` 才覆盖。
- [token 泄漏] → 不提供写入 token 的参数；README/AGENTS 只提示 `DATASPEC_TOKEN` 和 `--dataspec-token`。
- [doctor 失败导致用户误以为 init 失败] → 输出区分 `written/skipped` 和 `doctor`，让 AI 能判断是文件已生成还是环境未 ready。
- [默认路径不适配所有仓库] → 允许多次传 `--default-path`；不传时使用 `sql` 和 `db/migrations` 的轻量默认值。
- [AGENTS 格式差异] → 只追加 Markdown 片段和 marker，不尝试解析用户已有规范。

## Migration Plan

1. 新增 OpenSpec artifacts，并用 CLI 测试先锁定 `init` 行为。
2. 在 `tools/dataspec-cli.mjs` 中新增 `init` 分支和帮助文案，复用现有 `runDoctor` 的检查模型。
3. 新增初始化文件写入 helper、marker 更新 helper 和 JSON/text 输出格式。
4. 更新 README/TODO，运行 CLI 测试、OpenSpec validate 和 diff 检查。
5. 回滚时删除 `init` 分支、测试和文档；已由用户在业务仓库生成的文件不由 DataSpec 自动删除。

## Open Questions

- 后续是否需要交互式 `dataspec init`？第一版先用参数化实现，等实际使用反馈后再决定。
- 是否需要 `init --export-context` 直接下载 AI Context？本轮不做，避免初始化命令依赖后端包下载成功。
