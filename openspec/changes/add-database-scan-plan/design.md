## Context

P6-63 已新增 `/api/reverse-import/database/browser`，适合少量已选表的只读结构浏览。P6-64 面向大库：用户可能只知道连接信息，不希望一次性读取或勾选上百张表，也需要让 AI 根据 cursor 分批处理。

## Goals / Non-Goals

**Goals:**
- 提供只读 scan plan endpoint，按 `pageSize` 和 `cursor` 返回一批表、总量估算、进度和下一批 cursor。
- scan plan 响应可直接作为 AI handoff：包含 `scanId`、`resumeCommand`、`partialSummary`、`cancelled` 和 nextActions。
- 前端反向导入页支持按批次加载表、选择当前批次、继续下一批、取消扫描，并可用当前页生成部分 metadata browser。
- 中途取消只影响前端/请求状态，不写源库、不写标准库、不保存数据库凭据。

**Non-Goals:**
- 不引入分布式调度、后台长任务、服务端持久化 scan session 或凭据缓存。
- 不默认全库扫描；用户仍需显式点击继续或选择表。
- 不做真实 PostgreSQL/MySQL 集成测试；第一版用 H2/JDBC metadata、service/controller 和前端工具测试覆盖契约。

## Decisions

1. **cursor 使用无凭据偏移模型。**
   - 第一版 cursor 表达页偏移或已读取表数，不包含 password/token/JDBC URL。
   - 每次请求仍由用户提交连接信息；服务端不持久化连接凭据，符合个人工具安全边界。

2. **scanId 是可诊断标识，不是服务端任务 ID。**
   - scanId 用于前端和 AI 识别一轮扫描；取消状态随请求返回或前端本地状态表达。
   - 不承诺服务端后台任务生命周期，避免引入调度和清理复杂度。

3. **部分预览复用既有 browser/preview 流程。**
   - scan plan 只负责分页列出候选表；选中当前批次表后仍调用既有 browser/preview/import API。
   - 这样保持写入语义集中在既有确认导入流程，降低一致性风险。

## Risks / Trade-offs

- [Risk] 每页请求仍需要读取完整表列表来计算估算总量。→ Mitigation：第一版只读取表级 metadata，不读列；pageSize 有上限，后续再做驱动级游标或增量缓存。
- [Risk] offset cursor 在库结构变化后可能重复或跳过表。→ Mitigation：响应里说明 cursor 只适合同一连接和短时间恢复，AI 可根据 tableName 去重。
- [Risk] 前端取消不等于中断已经发出的 JDBC 请求。→ Mitigation：第一版取消表示不再继续下一批且不写标准库；不承诺 kill 数据库请求。

## Validation Strategy

- 后端测试覆盖 120+ 表分页、cursor 继续、cancelled 响应、不写标准库、resumeCommand 脱敏。
- 前端测试覆盖分页合并、取消状态、当前批次选择和 resume 文案脱敏。
- OpenSpec strict、后端目标测试、前端目标测试、全量回归、独立子 agent 评审。
