## Context

当前 DataSpec 已有 `doctor`、OpenAPI 漂移检查、AI capability catalog、MCP resources/tools 和多条 CLI 命令，但缺少一个“先确认本地 CLI/MCP 与服务端是否兼容”的稳定入口。AI 在业务仓库中运行旧脚本时，常见失败会混在服务不可达、能力未启用、schema 漂移、命令参数过旧等诊断里。

本变更跨后端、CLI、MCP 和 AI capability catalog，属于 SDD standard。设计目标是新增 additive、只读、可机器解析的握手契约，避免改变已有命令的成功/失败语义。

## Goals / Non-Goals

**Goals:**
- 提供一个后端只读版本兼容 payload，包含服务端版本、API schema hash、最小 CLI 版本、支持能力、废弃字段、升级建议和兼容状态。
- CLI 提供显式兼容检查命令，并让 `doctor` 能展示同一份兼容摘要。
- MCP 提供同一份兼容信息的 resource 或 tool，便于 agent 启动后先读取。
- capability catalog 公开该能力，让 AI 能发现推荐入口。

**Non-Goals:**
- 不做自动在线升级。
- 不引入遥测、远端版本查询或外部网络访问。
- 不承诺无限期兼容所有历史 CLI/MCP 行为。
- 不改变现有 lint、export-context、reverse import 等业务命令的必填参数或退出码。

## Decisions

1. **新增 `/api/capabilities/version` 作为握手 API。**
   - 理由：该能力与 AI capability catalog 的发现路径一致，语义是只读能力元信息，不需要项目上下文。
   - 备选：复用 `/api/capabilities` 顶层字段。放弃原因是会让 catalog 过重，也不利于 CLI/MCP 单独读取。

2. **兼容 payload 采用 additive JSON object。**
   - 字段包含 `kind/schemaVersion/serverVersion/apiSchemaHash/minCliVersion/supportedCapabilities/deprecatedFields/compatibility/upgradeHints/generatedAt`。
   - `compatibility` 使用 `status`、`clientVersion`、`compatible`、`reasons`、`nextActions` 表达 AI 可执行判断；缺少 clientVersion 时返回 `UNKNOWN`，不阻塞。

3. **CLI 采用显式 `compat check` 命令，并接入 `doctor` 摘要。**
   - 理由：显式命令便于 AI 在关键任务前主动运行；`doctor` 保持“环境入口”的现有心智。
   - 备选：每条命令执行前都强制握手。放弃原因是会增加所有命令延迟和服务端耦合，第一版先提供可选但稳定的 preflight。

4. **MCP 暴露 resource 优先，tool 可作为后续增强。**
   - 理由：兼容握手是只读状态信息，resource 更符合“先读上下文再选工具”的 MCP 使用方式。
   - 第一版资源 URI 使用 `dataspec://version-compatibility`，项目配置存在时也可带当前 projectId 作为诊断上下文但不读取项目业务数据。

## Risks / Trade-offs

- [Risk] `apiSchemaHash` 如果直接读取完整 OpenAPI 生成可能偏重。
  → Mitigation: 第一版可复用已有 API docs 文本 hash 或稳定版本常量；无法读取时返回 `unknown` 并给出 nextAction。
- [Risk] 旧 CLI 不会主动调用新握手。
  → Mitigation: README、doctor 和 capability catalog 明确推荐 AI 先运行 `compat check`。
- [Risk] active 服务端和本地 CLI 版本字符串格式不统一。
  → Mitigation: 第一版只做简单 semver-ish 比较；无法比较时返回 `UNKNOWN` 而不是误判失败。
- [Risk] MCP resource 后端不可达时可能阻断 agent 初始化。
  → Mitigation: 资源读取失败返回 AI 可读 JSON-RPC 诊断，不影响 MCP initialize。
