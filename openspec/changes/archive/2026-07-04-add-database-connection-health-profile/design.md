## Context

当前 `/api/reverse-import/database/test` 已能在连接成功时返回 `security`，用于判断账号是否适合只读 metadata 读取。但失败场景只有脱敏 message，成功场景也没有机器可读方言能力；AI 仍难以判断下一步是重试、换密码、修 schema、换数据库类型，还是继续执行反向导入。

## Goals / Non-Goals

**Goals:**
- 在现有连接测试接口中增加 `health` 字段，不引入新写入路径。
- 成功时输出连接耗时、数据库产品、版本、schema/comment/index 支持、只读检查和所需权限。
- 失败时输出脱敏状态、原因分类、是否可重试和 nextActions。
- 前端在反向导入与覆盖率报告页展示 health/capability 摘要。

**Non-Goals:**
- 不做长期监控、历史记录或定时探测。
- 不保存数据库密码、token、JDBC URL 或源数据库数据行。
- 不执行写探测，不替代数据库安全审计。
- 不扩展到 PostgreSQL/MySQL 之外的完整方言实现；未知方言只返回 unsupported capability。

## Decisions

1. **复用现有 test API，而不是新增并行接口。**
   - 原因：前端和 AI 现有调用入口已经是连接测试；扩展响应兼容旧字段，避免重复输入连接信息。

2. **新增 `health`，保留 `security`。**
   - 原因：`security` 已被前端消费，继续表达只读风险；`health` 负责连接状态、错误分类和方言 capability，职责清晰。

3. **失败也返回结构化 `health`。**
   - 原因：失败时最需要 AI 判断恢复动作；message 继续给人读，health 给机器读。

4. **能力画像使用确定性规则。**
   - 原因：PostgreSQL/MySQL 的 schema/comment/index metadata 支持可以按方言和 JDBC metadata 判断，第一版不需要外部依赖或真实写入试探。

## Risks / Trade-offs

- [Risk] 驱动错误消息各不相同，分类不可能完全准确。→ Mitigation：使用保守关键词分类，并保留原始脱敏 message 与 nextActions。
- [Risk] capability 不是生产权限证明。→ Mitigation：前端文案和字段名定位为“探测画像”，不等同于审计或长期监控。
- [Risk] 成功诊断可能增加一次 metadata 查询耗时。→ Mitigation：只在用户点击“测试连接”时执行，不影响表加载、预览和比对。
