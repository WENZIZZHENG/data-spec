## 1. OpenSpec 与测试准备

- [x] 1.1 校验 proposal、design 和 spec delta 一致，确认新增 capability 为 `cross-source-standard-evidence-view`
- [x] 1.2 定位字段、来源可信度、使用热区、候选、变更日志、SQL 检查和 AI 作业摘要的现有 service/repository/test 模式
- [x] 1.3 先写 `StandardEvidenceServiceTest`，覆盖字段证据聚合、证据不足、候选/变更/使用摘要和敏感片段不出现在响应中
- [x] 1.4 先写 `StandardEvidenceControllerTest`，覆盖 `GET /api/standard-evidence` 路由、参数绑定和不支持 `subjectType` 的错误行为
- [x] 1.5 运行新增测试并确认因生产实现缺失而失败

## 2. 后端实现

- [x] 2.1 新增 `standardevidence` response/query/model，字段补充业务语义说明和脱敏约束说明
- [x] 2.2 新增 `StandardEvidenceService`，按 `projectId + FIELD + subjectId` 聚合字段摘要、来源、可信度、热区、候选决策、变更日志、SQL 检查命中和 AI 作业摘要
- [x] 2.3 为候选、变更日志或其他来源补充必要 summary 查询，避免 raw evidence、before/after JSON、SQL 原文或 AI payload 进入聚合响应
- [x] 2.4 新增 `StandardEvidenceController`，提供只读 API `GET /api/standard-evidence?projectId=<id>&subjectType=FIELD&subjectId=<fieldId>`
- [x] 2.5 实现统一脱敏与 `aiEvidenceSummary` 生成，确保摘要只复述结构化安全证据

## 3. 验证与收口

- [x] 3.1 运行受影响后端测试并修复失败
- [x] 3.2 运行 `openspec validate add-cross-source-standard-evidence-view --strict`
- [x] 3.3 运行 `git diff --check` 和敏感词扫描，确认命中仅为安全边界说明、脱敏规则或合成测试夹具，不包含真实 secret
- [x] 3.4 启动独立子 agent 只读评审 API 契约、安全脱敏、分层和测试覆盖；关闭子 agent 并处理 findings
- [x] 3.5 在本文件追加 `Verification Evidence`，记录关键验证命令、结果和评审证据
- [x] 3.6 按 Git 规则暂存本次相关文件，检查 staged diff 和敏感词后创建本地 commit

## Verification Evidence

- 新增测试红灯：`mvn "-Dtest=StandardEvidence*Test" test` 首次失败于缺少 `standardevidence` 生产包和 summary 查询，证明测试先行覆盖新增行为。
- 新增测试：`mvn "-Dtest=StandardEvidence*Test" test` 通过，8 tests，覆盖 service、controller 和 repository summary 查询。
- 受影响后端回归：`mvn "-Dtest=StandardEvidence*Test,FieldProvenanceConfidence*Test,StandardUsageHeatmap*Test,AiTaskRecommendation*Test,StandardCandidate*Test,StandardChangeLog*Test" test` 通过，39 tests。
- OpenSpec：`openspec validate add-cross-source-standard-evidence-view --strict` 通过。
- 空白检查：`git diff --check` 通过；仅提示两个已修改 Java 文件工作区行尾未来会按 Git 规则转 CRLF。
- 敏感词扫描：`rg -n -i "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" ...` 命中仅为 OpenSpec 安全边界说明、生产脱敏正则、合成测试夹具和 `doesNotContain` 断言；未发现真实 secret。
- 独立评审：子 agent `019f39df-2ab8-7450-b8d9-34c34dddb355`，用途为 P6-115 只读代码评审，已调用 `close_agent` 关闭。评审提出 4 条 findings：SQL 原文装入聚合链路、AI 摘要缺少主要来源和候选/变更摘要、驼峰敏感标签脱敏不足、summary 查询缺少回归测试。
- 评审处理：SQL 命中改为复用使用热区安全计数；`aiEvidenceSummary` 改为从结构化 `items` 派生主要来源和候选/变更摘要；脱敏覆盖 `accessToken`、`jdbcUrl`、`clientSecret` 等驼峰标签；新增 `StandardEvidenceRepositorySummaryTest` 断言候选和变更日志 summary 查询不选择 raw 字段。
- 剩余风险：SQL 检查和 AI 作业命中仍是字段名近似匹配，不代表完整血缘；第一版只支持 `subjectType=FIELD`。
