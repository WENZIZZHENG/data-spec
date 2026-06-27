## Context

当前 `tools/dataspec-cli.mjs` 和 `tools/dataspec-mcp.mjs` 都要求用户显式传入项目 ID，服务地址默认 `http://localhost:8090`，但业务仓库已经有 `.dataspec/` 约定。对个人和 AI agent 来说，仓库级配置应该成为默认上下文，命令行只负责覆盖默认值。

## Goals / Non-Goals

**Goals:**

- 从当前工作目录向上查找 `.dataspec/config.json`。
- 支持 `projectId`、`server`、`defaultPaths` 三个字段。
- CLI/MCP 共用同一套配置解析逻辑。
- 显式命令行参数优先于配置文件。
- 为配置读取、CLI 默认路径和 MCP 默认项目补 Node 测试。

**Non-Goals:**

- 不发布 npm package，不实现全局安装器。
- 不保存 token、密码或用户身份。
- 不支持 YAML/TOML 配置格式。
- 不改变后端接口和现有显式参数行为。

## Decisions

1. **新增共享 `tools/dataspec-config.mjs`**
   - 理由：CLI 与 MCP 都需要同样的查找、JSON 解析和优先级逻辑，抽成纯工具函数便于测试。
   - 替代方案：分别在 CLI/MCP 中实现；会产生配置语义漂移。

2. **向上查找 `.dataspec/config.json`**
   - 理由：命令通常从业务仓库子目录执行，向上查找符合 Git 工具和 Node 工具的常见习惯。
   - 替代方案：只读当前目录；实现更简单，但对 migrations/sql 子目录使用不友好。

3. **显式参数优先**
   - 理由：现有脚本和 CI 依赖命令行参数确定性，配置文件只能做默认值。
   - 替代方案：配置优先；容易让临时覆盖失效。

4. **`defaultPaths` 只作为 `lint-files` 缺省位置参数**
   - 理由：P4-3 目标是减少常用命令输入，最自然的场景是 `dataspec lint-files` 默认扫描项目约定 SQL 路径。
   - 替代方案：对所有命令都使用默认路径；语义不清晰，容易误操作。

## Risks / Trade-offs

- **配置文件 JSON 损坏** → 返回清晰错误，避免静默使用错误项目。
- **缺少 `projectId`** → 保持现有错误语义，提示需要提供 `--project` 或配置文件。
- **`defaultPaths` 配到不存在目录** → 复用现有文件扫描错误，让用户看到具体路径问题。
- **配置字段继续扩展** → 第一版只读取已知字段，未知字段保留兼容但不参与行为。
