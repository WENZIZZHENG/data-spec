## Context

DataSpec 当前有三种问题结构：后端 `LintIssue`、`AiOutputPostCheckIssue`，以及 CLI `review-pr` 内部的 inline/fallback issue。它们分别表达规则码、严重度、SQL range、引用校验和评论去重，但没有共同的 subject、evidenceRefs、waiver 或交付元数据。Evidence Package 又只保存松散的 `postCheckSummary`，无法证明结构化 finding 已经过 project-scoped evidence resolver。

本变更跨后端 OpenAPI、CLI、MCP、GitHub API 和 Evidence Package，属于 SDD full。约束是保持既有 issue JSON、退出码、评论 marker、GitHub 写入权限和数据库 schema 兼容；所有外部文本必须 bounded、secret-safe，外部 AI finding 不得仅凭自报 confidence 成为门禁。

## Goals / Non-Goals

**Goals:**

- 提供一个稳定、additive、可跨模块消费的 Review Finding 读模型。
- 让 SQL lint 和 AI post-check 同时保留旧 issue 并输出 `findings[]`。
- 让外部结构化 finding 在进入 Evidence Package 或门禁前经过确定性 post-check 和 evidence resolver。
- 让 `review-pr` JSON 一次交付 commit、评论、finding、SQL check 和 evidence package 入口。
- 维持空 findings 为正常结果，不生成总分或无证据 AI 结论。

**Non-Goals:**

- 不调用外部 LLM，不新增 AI reviewer、PR 总分或自动修复执行器。
- 不新增 finding 数据库表、队列、审批流或 GitHub App。
- 不替换 `LintIssue`、`AiOutputPostCheckIssue`、SQL check record 或现有 Evidence Package source。
- 不自动创建远程 PR、自动应用 fixedSql，或把 GitHub 评论 URL 当作确定性问题证据。

## Decisions

### 1. 共享 Finding 是 versioned additive read model

新增 `ReviewFinding`、`ReviewFindingSubject`、`ReviewFindingLocation`、`ReviewFindingWaiver` 和 severity/source enum。字段覆盖 code、severity、subject、location、trigger、expected、observed、evidenceRefs、confidence、suggestedFix、autoFixSafe、waiver，并额外提供稳定 source/findingKey 以便去重。`findingKey` 按固定字段顺序、显式 null、UTF-8 byte length 前缀和 SHA-256 生成，Java/CLI 共用固定 fixture，避免 record/string 拼接边界歧义。record 构造边界统一限制文本和列表规模并调用 `SensitiveDataSanitizer`。

保留旧 issue 而不是直接重命名，因为现有前端、CLI、MCP、历史 SQL record JSON 和 fixtures 已依赖它们。适配器在生成响应时派生 findings，旧客户端忽略新增字段即可。

### 2. SQL check record ID 在保存后 additive 回填

`SqlLintService` 继续先完成规则、range、suppression 和 fixedSql 计划，再派生 Finding。SQL check 保存成功后把 `sqlCheckRecordId` 和 `dataspec://evidence/sql-check/<id>` 回填到结果/findings；保存失败仍返回 finding，但 evidenceRefs 为空并保留既有非阻断日志语义。

不新增事务或表字段：SQL record 已是可解析的持久化 evidence source，复用现有 resolver 即可。

### 3. 外部 AI findings 通过 post-check 请求进入

`AiOutputPostCheckRequest` additive 接受最多 100 条 findings，旧四参数构造和 content 校验保持兼容。服务对每条 evidenceRef 调用 project-scoped `EvidenceClaimResolver`；ERROR、confidence >= 80 或调用方自报 `autoFixSafe=true` 的 finding 缺少可验证 evidence 时产生 FAIL issue，低置信 finding 缺证据产生 WARN。外部 finding 的规范化输出始终强制 `autoFixSafe=false`，即使证据有效也不把外部 AI 声明升级为可自动修复；只有 SQL lint 派生的未豁免、LOW-risk、真正 APPLIED 确定性修复可以输出 true。结果同时包含经脱敏规范化后的 findings 和由 legacy issues 派生的 findings。

备选方案是在 CLI 本地信任 `confidence`。该方案无法验证跨项目、已删除或伪造 evidence ref，因此不采用。

### 4. Evidence Package 只接受带签名 receipt 的已 post-check 外部 findings

`AiEvidencePackageReq/Package` additive 增加 findings。SQL_CHECK 来源加载记录后要求记录存在非空项目归属，并按该项目执行授权检查；请求中的 projectId 不得替代缺失的持久化归属。COVERAGE_REPORT 在读取 payload 前先校验调用方对请求项目的访问权限。持久化 `LintIssue` 再自动派生并附 canonical evidence ref。

其他请求携带 findings 时，必须同时提供 `postCheckSummary.safeToUse=true` 和 post-check 签发的进程内 HMAC receipt。receipt 绑定 projectId、PASS/safeToUse、规范化外部 findings 的完整稳定摘要和数量；Evidence Package 以相同规范化规则重算摘要、验证签名，并再次由 resolver 验证 evidence refs。receipt 不绑定 package sourceType/sourceId，也不提供 nonce 或一次性消费语义，因此同一进程内可为同项目、同 findings 重复使用；该取舍符合只读导出的第一版边界。receipt 不持久化到 package，服务重启后旧 receipt 失效，调用方需要重新 post-check。这样可防止调用方仅伪造 summary，或在 post-check 后修改 severity、evidence、confidence 等字段。外部 finding 在 Evidence Package 中同样固定 `autoFixSafe=false`。

Evidence Package 仍只保存摘要、bounded finding 和 canonical refs，不保存 raw AI output、SQL 之外的业务数据行或凭据。

### 5. `review-pr` 绑定真实 PR 文件与 head 后输出 delivery envelope

CLI 先固定 PR head SHA 并读取 PR files。每个本地待 lint SQL 必须唯一映射到一个 PR file，且本地原始 bytes 计算的 Git blob SHA 必须与 GitHub file `sha` 一致；缺少 sha、非 PR 文件、路径歧义或内容漂移都在 lint 和远程评论前失败。获取 files 后以及发布任何评论前再次读取 PR head，head 变化时停止，避免把任意本地文件的结果标记为另一提交的评审。

inline API 和 summary comment API 返回的 `html_url` 被保留为 delivery 元数据。`review-pr --format json` 新增 kind/schemaVersion、commitSha、reviewCommentUrl、inlineCommentUrls、findings、sqlCheckRecordIds、postCheck 和 evidencePackages，同时保留 `reviewCommentAction`、summary、inline、files；inline 规划复用已绑定的 PR files，不重复读取另一份文件列表。

后端较旧、尚未返回 findings 时，CLI 从 `issues[]` 兼容派生；Finding location 补充业务仓库相对路径。确定性 lint finding 的 postCheck 状态为 `NOT_REQUIRED`；只有外部 AI finding 才必须运行 AI output post-check。

### 6. GitHub URL 只作交付证据，不作问题真实性证据

commit SHA、summary/inline comment URL 证明“评论发到了哪里”，不放入 finding.evidenceRefs。问题真实性只引用 DataSpec canonical evidence ref；这样评论删除或 PR force-push 不会伪造规则证据。

## Risks / Trade-offs

- [additive finding 让响应变大] → findings 与 evidenceRefs 有数量/长度上限；Evidence Package 继续使用预览上限。
- [旧后端没有 findings 或 sqlCheckRecordId] → CLI 保留 issue fallback，输出空 record/evidence 列表而不编造 ID。
- [GitHub API 返回缺少 html_url] → URL 字段可空，action 和退出码仍保持原语义。
- [PR head 或本地 SQL 在评审期间变化] → lint 使用已完成 blob 校验的同一份内存 bytes，发布前复查 head；任何漂移均停止远程写入并要求重跑。
- [外部 finding 的 expected/observed 或未知字段可能携带大 payload] → CLI/MCP 使用字段 allowlist、正整数 schemaVersion、嵌套 `additionalProperties=false` 和逐字段上限在请求前拒绝；后端 record 构造和 Evidence Package 再次递归脱敏并在声明上限内截断。
- [Java 与 JavaScript 对 supplementary 字符长度口径不同] → 所有 Finding/post-check 字符串边界统一按 Unicode code point 校验；OpenAPI/MCP `maxLength` 与 sanitizer 使用同一口径。
- [fixture 元数据与 MCP schema 常量同时漂移] → fixture checker 递归比对 live `findings.items` 的字段、required、additionalProperties、版本下限、数组和文本上限，并用 live descriptor mutation 回归测试守门。
- [schemaVersion=1 后续扩展] → 新字段继续 additive；删除、改类型或改变 evidence gating 时升级版本并走兼容评审。

## Migration Plan

1. 先发布后端 finding model、lint/post-check/evidence additive 字段和 OpenAPI。
2. 更新 CLI/MCP 发送/消费 findings，并保持旧服务器 fallback。
3. 更新 contract fixtures、README、AI 契约和 GitHub workflow 示例说明。
4. 回滚 CLI 时后端新增字段被忽略；回滚后端时新版 CLI 退回 issue adapter，既有评论 marker 和退出码不变。

## Open Questions

- 无。Finding 持久化、跨 PR 历史查询、自动 waiver 审批和自动修复执行均在出现真实需求后另立变更。
