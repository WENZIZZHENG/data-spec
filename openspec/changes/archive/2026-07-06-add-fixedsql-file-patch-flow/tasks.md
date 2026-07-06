## 1. 测试先行

- [x] 1.1 新增 fixedSql patch 纯函数单测，覆盖 dry-run 计划、无 fixedSql、无变化、内容漂移和确认 hash。
- [x] 1.2 新增 CLI 单测，覆盖 `fixed-sql patch` dry-run、apply 成功、缺少确认和路径越界。
- [x] 1.3 更新 CLI/MCP contract fixture 测试，覆盖新增命令的输出 shape 和安全 metadata。

## 2. 核心实现

- [x] 2.1 新增 fixedSql 文件补丁计划构建函数，输出稳定 JSON、unified diff、planHash、applyCommand、rollbackHint、evidenceRef 和 nextActions。
- [x] 2.2 实现目标路径解析和漂移检测，确保目标文件位于当前工作目录且当前内容匹配 lint 原文。
- [x] 2.3 实现 apply 模式，只有 `--apply --confirm <planHash>` 且重新计算计划匹配时才写入目标 SQL 文件。

## 3. CLI 与契约接入

- [x] 3.1 在 `tools/dataspec-cli.mjs` 接入 `fixed-sql patch` 命令、help 文案、JSON 输出和退出码。
- [x] 3.2 更新 `tools/fixtures/cli-mcp-contracts.json`，补新增命令的输入、输出、退出码、安全 metadata 和示例。
- [x] 3.3 确保错误输出沿用现有脱敏逻辑，不泄露 token、password、Authorization、JDBC URL、DSN 或连接串。

## 4. 文档与验证

- [x] 4.1 更新 TODO 中 P6-78 状态与完成能力说明。
- [x] 4.2 运行 `openspec validate add-fixedsql-file-patch-flow --strict`、`node --test tools/*.test.mjs` 和 `git diff --check`。
- [x] 4.3 在本文件记录 Verification Evidence，并启动独立子 agent 评审 P6-78 变更。

## Verification Evidence

- `openspec validate add-fixedsql-file-patch-flow --strict`：通过，输出 `Change 'add-fixedsql-file-patch-flow' is valid`。
- `node --test tools/*.test.mjs`：通过，205 个测试中 204 个通过、1 个跳过；跳过项为当前 Windows 平台无法创建 symlink（`EPERM`），实现仍通过 realpath 校验并拒绝符号链接路径。
- `git diff --check`：通过，exit 0；仅输出工作区 CRLF 提示。
- 独立评审子 agent：`019f37b1-500b-7af1-b99d-bd8406ce7758`（Aristotle），用途：P6-78 CLI 外部协议、本地文件写入安全、路径越界、脱敏输出、测试与 OpenSpec 一致性只读评审；结果：发现 `files[].path` 错配、缺少原文证据、symlink/realpath、写前复核和 fixture 覆盖问题；已修复并关闭。
- 独立复评子 agent：`019f37be-a19f-7cf3-8b32-cbda5fcaa12f`（Carson），用途：复评初评阻塞项是否已处理；结果：无 Critical；指出 mixed `fixedSql + files[]` 选择顺序和 hash-only 测试覆盖不足，已修复并关闭。
