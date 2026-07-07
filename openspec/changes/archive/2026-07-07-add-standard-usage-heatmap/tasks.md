## 1. 测试先行

- [x] 1.1 新增标准使用热区 service 单测，覆盖高使用低质量字段、低使用废弃字段和不暴露原始 SQL/AI payload。
- [x] 1.2 新增标准使用热区 controller MockMvc 测试，覆盖 `GET /api/standard-usage/heatmap?projectId=<id>` 路由、参数绑定和 `R` JSON 包装。
- [x] 1.3 运行新增测试并确认 RED 失败原因是能力缺失。

## 2. 后端实现

- [x] 2.1 新增 `standardusageheatmap` 响应模型与字段级说明。
- [x] 2.2 新增 `StandardUsageHeatmapService` 与实现，聚合字段、质量、冲突、来源、SQL 检查和 AI 作业近期命中。
- [x] 2.3 新增 `StandardUsageHeatmapController` 暴露只读 API。
- [x] 2.4 确保响应不返回 SQL 原文、AI payload、raw issue JSON 或 raw source metadata。

## 3. 验证与收口

- [x] 3.1 运行受影响后端测试并确认 GREEN。
- [x] 3.2 运行 `openspec validate add-standard-usage-heatmap --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描，并补充 Verification Evidence。
- [x] 3.4 启动独立子 agent 做只读代码评审，关闭子 agent，并处理或记录 findings。
- [x] 3.5 commit 前执行 staged diff、staged check、敏感词扫描和本地 commit。

## Verification Evidence

- RED：`mvn "-Dtest=com.dataspec.standardusageheatmap.StandardUsageHeatmapServiceImplTest,com.dataspec.standardusageheatmap.StandardUsageHeatmapControllerTest" test`，结果失败，原因是 `standardusageheatmap` controller/model/service 尚不存在。
- Review RED：同一定点命令在补充 review 回归测试后失败，原因是高使用仅冲突字段 `cleanupPriority` 只有 25，废弃但仍有近期引用字段被误计入 cleanup candidate。
- GREEN：同一定点命令结果 8 个测试通过。
- 受影响后端测试：`mvn "-Dtest=StandardUsageHeatmap*Test,FieldQuality*Test,FieldConflict*Test,SqlCheckRecordServiceImplTest,AiJobRecord*Test" test`，结果 33 个测试通过。
- OpenSpec：`openspec validate add-standard-usage-heatmap --strict`，结果通过。
- 通用检查：`git diff --check`，结果通过。
- 敏感词扫描：`rg -n "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" dataspec-server/src/main/java/com/dataspec/standardusageheatmap dataspec-server/src/test/java/com/dataspec/standardusageheatmap dataspec-server/src/main/java/com/dataspec/reverseimport/repository/FieldSourceRepository.java dataspec-server/src/main/java/com/dataspec/aireplay/repository/AiJobRecordRepository.java openspec/changes/add-standard-usage-heatmap`，命中项均为 OpenSpec 禁止 raw secret 的说明和测试中的脱敏假输入，无真实凭据。
- 评审 1：子 agent `019f39a7-c9ba-76b1-af7f-3f19ac96aae1` 结论 `With fixes`，指出来源/AI 作业应使用摘要查询、优先级边界不足、字段名边界和 design 表述问题；已修复并关闭。
- 评审 2：子 agent `019f39b3-29ee-7ce1-a8b0-35894785934b` 结论 `With fixes`，指出高使用仅冲突字段优先级不足、废弃但仍有近期引用字段误归档、验证证据和 design 表述需更新；已修复并关闭。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-standard-usage-heatmap --yes`，创建主规格 `openspec/specs/standard-usage-heatmap/spec.md`，并归档到 `openspec/changes/archive/2026-07-07-add-standard-usage-heatmap/`。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 13 降至 8；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=8,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3abb-7619-78c0-98de-9672aa19b115`（Planck）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
- 2026-07-07：补齐新增主规格 Purpose 后，独立只读复评子 agent `019f3ac0-d02f-75a1-801f-b97679d4f29c`（Helmholtz）复评 staged diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
