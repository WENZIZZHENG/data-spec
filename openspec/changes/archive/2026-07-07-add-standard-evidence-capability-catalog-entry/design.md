## Context

DataSpec 的 AI 新会话入口通常是 session bootstrap、capability catalog、README 或离线 `.dataspec/capabilities.json`。P6-115 新增了跨来源标准证据 API，但若不进入 catalog，AI 不知道在回答“这个字段标准可信吗、来自哪里、最近是否被用过”时应该优先调用该证据视图。

本次变更属于 SDD standard 的小范围 AI 契约补强：修改能力清单中的公开能力描述和 README，不改变证据 API 行为、不新增存储、不新增 CLI/MCP 执行命令。

## Goals / Non-Goals

**Goals:**

- 在 capability catalog 中加入 `standard-evidence`。
- 标明它是只读能力，输入为 `projectId + subjectType=FIELD + subjectId`。
- 标明输出为字段摘要、证据摘要、证据列表、AI 可复制摘要和覆盖说明。
- 标明安全边界：不返回 raw SQL、AI payload、候选 raw evidence、raw source metadata 或凭据。
- 用定点测试锁定 catalog list、single capability、version supported capability 和 safety metadata。

**Non-Goals:**

- 不修改 `/api/standard-evidence` 响应字段。
- 不把该能力加入 CLI/MCP 命令。
- 不自动调用证据视图，不生成或缓存 evidence package。

## Decisions

1. **只新增 capability entry，不改证据服务。**
   - 原因：P6-115 已经验证证据 API；当前缺口是 AI discovery。

2. **使用 `standard-evidence` 作为 stable capability id。**
   - 原因：和 API 路径 `/api/standard-evidence` 一致，便于 CLI/README/AI prompt 稳定引用。

3. **安全 metadata 明确为 read-only。**
   - 原因：catalog 只描述能力，不执行证据聚合；实际 API 也只读且服务层校验项目访问和字段归属。

## Risks / Trade-offs

- **能力条目描述过度承诺** → 只写第一版支持 `FIELD`，不暗示表/规则证据。
- **AI 误以为有 CLI/MCP 命令** → surfaces 只列 API，nextActions 引导读取 API 或先查询字段 ID。
- **安全边界漂移** → 测试检查 safety metadata；README 明确不含 raw SQL、AI payload 或凭据。
