## Context

DataSpec 已能导出 AI Context zip，也能通过 `.dataspec/config.json` 让 CLI/MCP 知道当前项目和服务地址。但在业务仓库里，AI agent 往往先看本地文件：如果 DataSpec 服务没启动、token 不可用或网络断开，现有 `export-context --output` 只能留下一个 zip 文件，AI 无法稳定知道里面是不是当前项目、是否过期、该优先读哪些文件。

P6-29 第一版优先做业务仓库本地缓存，不改后端数据模型：CLI 仍从现有 AI Context zip 下载内容，然后把可读文件解到 `.dataspec/context/`，并写入一个 metadata 文件供 doctor、AI 和后续 MCP 使用。

## Goals / Non-Goals

**Goals:**

- `export-context --cache` 将 AI Context 写入业务仓库 `.dataspec/context/`，适合 AI 直接读取。
- 缓存 metadata 记录项目、服务端、导出参数、导出时间、标准版本/hash/source 和内容摘要。
- `doctor` 能在服务可用或不可用时报告缓存状态：missing、fresh、stale、remote-different、unreadable。
- 离线模式只读，不把缓存当成服务端状态，也不保存 token、密码、完整 JDBC URL 或业务数据行。

**Non-Goals:**

- 不实现新的后端 API 或数据库表。
- 不让 MCP 在本轮自动改为离线缓存优先；只保留 metadata 和目录，为后续 MCP 接入准备。
- 不自动提交 `.dataspec/context/` 到业务仓库，不替用户决定是否纳入 Git。
- 不做复杂 zip 权限、符号链接或跨平台压缩边界之外的通用归档工具。

## Decisions

### 1. 复用现有 AI Context zip，而不是新增后端 cache API

CLI 已能下载 `/api/ai-context/package/download`，zip 里也已有 `.dataspec/manifest.json`、field catalog、rules、prompts 和 AGENTS fragment。第一版直接复用它，避免后端新增重复导出逻辑。

备选方案是后端新增 `/api/ai-context/cache` 返回目录结构 JSON。它更直接，但会复制 zip 生成路径，并扩大 OpenAPI/前端契约范围。

### 2. 缓存目录固定为 `.dataspec/context/`

业务仓库已有 `.dataspec/config.json`，把上下文放在 `.dataspec/context/` 能让 AI 按固定路径读取，同时不覆盖用户已有 config。目录内保留从 zip 解出的 `.dataspec/*` 文件，并额外写入 `.dataspec/context/cache-metadata.json`。

备选方案是直接覆盖业务仓库 `.dataspec/field-catalog.json`。它更短，但容易和 config、README、手工文件混在一起，也不利于清理和过期判断。

### 3. metadata 使用保守脱敏和 stale 策略

metadata 只保存 server host/path、projectId、scope/query/status/limit、snapshotId/snapshotVersion、exportedAt、expiresAt、specVersion/specHash/source 和 contentHash。默认 TTL 第一版使用 7 天，可通过 `--cache-ttl-days` 调整；doctor 离线时只根据本地 expiresAt 判断 stale，在线时再对比远端 manifest 的 specHash。

### 4. 写缓存前清理旧上下文目录

为了避免旧文件残留误导 AI，`--cache` 写入时会重建 `.dataspec/context/`。清理范围必须严格限制在业务仓库 `.dataspec/context/` 下，不触碰 `.dataspec/config.json`、token、业务 SQL 或其他目录。

## Risks / Trade-offs

- [Risk] 解 zip 引入依赖或安全边界。→ 使用项目已有 Node 运行环境，优先选择轻量 JS zip 依赖或已有依赖；解压时拒绝 `..`、绝对路径和越界目标。
- [Risk] 缓存可能过期但服务不可用。→ doctor 明确输出 stale/offline，而不是假装缓存有效；AI 仍可读取最近上下文但必须看到过期提示。
- [Risk] `.dataspec/context/` 被用户提交到 Git。→ README 说明可提交或本地缓存由用户决定，但 metadata 不包含 secrets；第一版不自动改 `.gitignore`。
- [Risk] zip 内 manifest 字段未来变化。→ metadata 读取失败时仍写入 contentHash/exportedAt，并把标准元数据标记为 unknown。

## Migration Plan

- 新增 CLI cache helper 和测试，不修改现有 `export-context --output` 行为。
- `doctor` 增加一个非阻断或降级检查项；无缓存时不影响既有 server/project/OpenAPI 检查。
- README/TODO 更新 P6-29 状态和命令说明。
