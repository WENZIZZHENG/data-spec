## Context

DataSpec CLI 已有 `changed` 与 `lint-changed`：它们能在业务仓库内读取 git 变更、按 `.dataspec/config.json` 的 `defaultPaths` 收窄范围，并只对变更 SQL 调用 `/api/lint`。当前缺口是本地开发入口还需要用户自己拼接 hook、VS Code task 和失败输出格式，导致很多问题仍等到 CI 或 PR review 才暴露。

P6-91 第一版选择补“安装器和模板”，而不是引入新的守护进程或 IDE 插件。它应沿用现有 CLI 测试、fixture 和脱敏机制，只在当前业务仓库内写 DataSpec 管理的本地文件。

## Goals / Non-Goals

**Goals:**
- 提供 CLI `install-hook`，一键安装 DataSpec 管理的 `pre-commit` hook。
- 可选生成 VS Code task 和 Problem Matcher 示例，支持本地保存前或手动任务运行时跳转到 SQL 诊断位置。
- 保留 `lint-changed --format json` 作为 pre-commit 的默认 AI 可读输出，并增加 `lint-changed --format text` 作为 IDE matcher 的行格式输出。
- 输出稳定安装 JSON，包含 written/skipped、diagnostics、safety 和 nextActions，且不泄漏 token/password/API key/JDBC URL/DSN。
- 不覆盖非 DataSpec 管理的用户 hook，不修改远端仓库，不替代 CI/GitHub Review。

**Non-Goals:**
- 不实现完整 VS Code 扩展、JetBrains 插件或跨编辑器自动配置。
- 不自动修改用户全局 git hook、pre-commit framework 配置或远端仓库设置。
- 不新增后端 API，不改变 `/api/lint` 语义。
- 不引入外部 LLM、npm 依赖或后台文件监听服务。

## Decisions

1. **新增 `install-hook` 独立命令，而不是扩展 `init`。**
   - 选择：`install-hook [--hook pre-commit] [--with-vscode] [--format json|text]`。
   - 原因：`init` 面向 `.dataspec/config.json` 和 AGENTS 片段；hook 安装会写 `.git/hooks`，风险边界更高，独立命令便于用户显式启用和 AI 识别。
   - 备选：在 `init --with-hook` 中顺带安装。放弃原因是容易让初始化和本地 Git hook 副作用混在一起。

2. **只覆盖 DataSpec marker 管理的 hook。**
   - 选择：生成 hook 文件时写入固定 marker；已有非 marker hook 时拒绝覆盖并输出 `HOOK_EXISTS_UNMANAGED` 诊断。
   - 原因：项目规则要求不绕过用户本地 Git 配置；即使传 `--force`，第一版也只允许刷新 DataSpec 管理文件，避免误删用户已有脚本。
   - 备选：支持合并到现有 hook。放弃原因是 shell 脚本合并和跨平台转义风险高，第一版更适合可解释的保守行为。

3. **pre-commit 使用 JSON，VS Code task 使用 text。**
   - 选择：hook 执行 `lint-changed --format json`，失败时原样输出 AI 可读 JSON；VS Code task 使用 `lint-changed --format text`，每个诊断一行，便于 Problem Matcher 正则解析。
   - 原因：JSON 对 AI 友好，但 VS Code Problem Matcher 更适合单行 `file:line:column: severity rule - message`；两种输出复用同一 lint 结果，不改变服务端。
   - 备选：让 Problem Matcher 解析 pretty JSON。放弃原因是不稳定且难以在一行同时获得 file/line/rule/severity/suggestion。

4. **生成文件只包含命令模板，不写凭据。**
   - 选择：hook 和 VS Code task 不内嵌 token、password 或服务连接串；认证继续通过 `.dataspec/config.json`、环境变量或用户显式命令参数。
   - 原因：本地模板可能被提交或复制给 AI，必须默认可安全展示。

## Risks / Trade-offs

- **已有用户 hook 无法自动合并** → 输出 `HOOK_EXISTS_UNMANAGED` 和手动合并建议，避免破坏用户配置。
- **Windows Git hook 执行环境差异** → 生成 POSIX shell hook，这是 Git hooks 的通用格式；命令通过 `node <repo>/tools/dataspec-cli.mjs` 调用，README 说明需要 Node.js。
- **IDE task 不能覆盖所有编辑器保存事件** → 第一版只提供 VS Code 示例和 matcher，其他编辑器可按同一 text 行格式适配。
- **`lint-changed --format text` 依赖后端 issue span 完整度** → 缺 line/column 时回退到 `1:1`，仍保留 rule/severity/message/suggestion 供定位和 AI 修复。

## Migration Plan

- 新命令和 `lint-changed --format text` 都是向后兼容新增；现有 `changed`、`lint-changed --format json`、CI workflow 和 PR review 不改变默认语义。
- 用户可通过删除 `.git/hooks/pre-commit` 或重新运行 `install-hook` 刷新 DataSpec 管理 hook；非托管 hook 不会被覆盖。
- 本次完成后保留 OpenSpec active，不自动 archive。
