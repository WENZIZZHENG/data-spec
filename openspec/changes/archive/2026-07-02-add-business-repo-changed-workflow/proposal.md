## Why

AI 在业务仓库中工作时，通常只需要处理本次 git diff 中的 SQL、迁移文件或模型文件；如果每次都扫描全仓和导出完整 Context，会浪费上下文并增加误报。

## What Changes

- 新增 `dataspec changed`：读取业务仓库 `.dataspec/config.json`、`defaultPaths` 与当前 git 变更，输出变更文件清单、SQL 子集、最小 AI Context 建议和下一步命令。
- 新增 `dataspec lint-changed`：只对 `changed` 发现的 SQL 文件运行现有 SQL lint，并把 lint 汇总合并进同一份 JSON。
- 在无 git 仓库、未配置 `defaultPaths`、无匹配变更或无 SQL 变更时返回可恢复诊断，不自动扫描全仓。

## Capabilities

### New Capabilities
- `business-repo-changed-workflow`: 面向 AI/CLI 的业务仓库变更感知入口。

### Modified Capabilities
- 无。

## Impact

- 已有基础：已有 `dataspec init`、`.dataspec/config.json` 默认路径、`doctor`、`lint-files`、`review-pr`、AI Context 导出和按需裁剪待办。
- 缺口：CLI 还缺少基于 git diff/defaultPaths 的 changed-file 发现、只对变更文件 lint、并按变更内容导出最小 Context 的稳定入口。
- 落地产物：新增 `changed`、`lint-changed` CLI 命令；读取 `.dataspec/config.json`、git diff 和默认路径，输出变更 SQL/DDL 文件、推荐 Context scope、lint 摘要和下一步命令。
- 验收标准：在业务仓库改动少量 SQL 文件后，AI 可一条命令拿到变更文件列表、对应 lint 结果和最小标准上下文；无 git 仓库或无变更时有可恢复提示。
- 边界：不自动修改业务代码，不自动提交，不扫描未配置的大型目录。

## Verification Evidence

- `openspec validate add-business-repo-changed-workflow --strict`：通过。
- `node --test tools/dataspec-config.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs`：105 tests，0 failures。
- `openspec validate --all`：89 passed，0 failed。
- `git diff --check`：通过，仅提示 LF/CRLF 工作区换行转换 warning。
- 本地结构化代码评审：已执行，不使用子 agent；发现并修复 JSON 输出包含本机绝对路径的问题，并补充断言确认 `changed` / `lint-changed` 输出使用相对路径。
