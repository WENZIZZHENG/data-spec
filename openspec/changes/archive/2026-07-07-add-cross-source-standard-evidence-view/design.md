## Context

DataSpec 已有字段库、字段来源、字段来源可信度、标准使用热区、标准候选、SQL 检查记录、AI 作业摘要和标准变更日志等能力。当前这些信号分散在不同接口或内部服务里，用户和 AI 查看单个标准字段时缺少一个安全、统一、可复制的证据视图。

本次变更属于 SDD standard：新增用户可见只读 API 和 AI 可消费响应契约，但不改数据库 schema、写入语义、安全边界或外部执行协议。

## Goals / Non-Goals

**Goals:**

- 为 `FIELD` 标准对象提供跨来源只读证据视图。
- 复用已有安全摘要数据，输出来源、可信度、使用热度、候选决策、变更日志、SQL 检查命中和 AI 作业使用摘要。
- 生成 `aiEvidenceSummary`，方便用户复制给 AI，同时明确不包含 raw SQL、raw AI payload、raw candidate evidence、raw source metadata 或凭据。
- 对新增 API response model 补充字段级说明，并用 service/controller 测试覆盖路由、聚合、排序和脱敏边界。

**Non-Goals:**

- 不支持 `FIELD` 以外的 `subjectType`。
- 不新增数据库 schema、迁移、缓存、后台任务或自动重算任务。
- 不做全量血缘平台、不验证证据真伪、不自动采纳候选、不写入正式标准。
- 不返回 SQL 原文、AI 输入输出 payload、业务数据行、连接串、token、password、Authorization 或候选 raw evidence。

## Decisions

1. **新增 `standardevidence` 聚合模块，而不是改造已有来源可信度或热区模块。**
   这些已有模块分别回答“可信度”和“热区”问题，跨来源证据视图回答“某个标准字段有哪些可解释证据”。单独模块能保持 API 契约清晰，并避免把多个下游摘要语义塞进已有模块。

2. **第一版只支持 `subjectType=FIELD`。**
   DataSpec 当前最稳定的标准对象是字段；其他对象如规则、表、合同引用的证据模型还不统一。限制 subject type 可以先交付真实闭环，后续再通过 OpenSpec 扩展。

3. **Service 聚合安全摘要，不传递 raw payload。**
   字段来源、候选、SQL 检查和 AI 作业都可能关联敏感上下文。聚合服务只读取或输出安全字段：id、类型、状态、计数、字段名、时间、脱敏引用和短原因；如果必须从旧记录判断命中，只在服务内部使用并确保响应不携带原文。

4. **候选、变更日志等来源优先增加 summary 查询或复用已有 summary 方法。**
   如果现有 repository 会加载 raw evidence/before/after JSON，新增受限 summary model/query，避免 raw 字段进入证据聚合链路。无法避免读取时，测试必须断言响应和 AI 摘要不包含敏感片段。

5. **`aiEvidenceSummary` 由结构化 items 派生。**
   AI 摘要不单独引入额外事实来源，只把已返回的安全 evidence items 压缩成可复制文本，保证用户可追溯。

## Risks / Trade-offs

- **证据不完整** → 第一版明确只聚合 DataSpec 已保存的安全记录；响应中保留 `coverageNotes` 说明未覆盖范围。
- **旧数据中包含敏感片段** → 响应层统一脱敏，并用测试覆盖 `password`、`token`、`Authorization`、`jdbc:`、`dsn` 等常见片段。
- **跨服务查询变慢** → 第一版限制为单字段查询，并复用已有项目级 summary；不做项目全量批量证据包。
- **SQL/AI 命中只能近似匹配字段名** → 结果表达为命中计数和最近引用时间，不把它宣称为真实血缘或强因果证据。
- **后续 subject 扩展破坏契约** → 当前 spec 固定 `FIELD` 行为；新增类型必须另起 OpenSpec 变更或修改本 capability。
