## 1. 测试先行

- [x] 1.1 新增字段来源可信度 service 单测，覆盖可信字段聚合、低证据字段提醒和敏感来源脱敏。
- [x] 1.2 新增字段来源可信度 controller 单测，覆盖 API 委派与响应包装。
- [x] 1.3 运行新增测试并确认 RED 失败原因是能力缺失。

## 2. 后端实现

- [x] 2.1 新增 `fieldprovenance` 响应模型与 `FieldProvenanceConfidenceLevel`，补充公共字段语义说明。
- [x] 2.2 新增 `FieldProvenanceConfidenceService` 与实现，聚合字段、来源、候选和质量评分并生成置信度摘要。
- [x] 2.3 为 `StandardCandidateRepository` 增加按项目只读查询，供聚合服务计算候选证据。
- [x] 2.4 新增 `FieldProvenanceConfidenceController` 暴露 `GET /api/fields/provenance-confidence`。

## 3. 验证与收口

- [x] 3.1 运行受影响后端测试并确认 GREEN。
- [x] 3.2 运行 `openspec validate add-field-provenance-confidence-summary --strict`。
- [x] 3.3 运行 `git diff --check` 并补充 Verification Evidence。
- [x] 3.4 启动独立子 agent 做只读代码评审，关闭子 agent，并处理或记录 findings。
- [x] 3.5 commit 前执行 staged diff、staged check、敏感词扫描和本地 commit。

## Verification Evidence

- RED：`mvn "-Dtest=com.dataspec.fieldprovenance.FieldProvenanceConfidenceServiceImplTest,com.dataspec.fieldprovenance.FieldProvenanceConfidenceControllerTest" test`，结果失败，原因是 `fieldprovenance` controller/model/service 尚不存在。
- GREEN：`mvn "-Dtest=com.dataspec.fieldprovenance.FieldProvenanceConfidenceServiceImplTest,com.dataspec.fieldprovenance.FieldProvenanceConfidenceControllerTest" test`，结果 3 个测试通过。
- 受影响后端测试：`mvn "-Dtest=FieldProvenanceConfidence*Test,FieldQuality*Test,StandardCandidate*Test" test`，结果 21 个测试通过。
- 评审修复 RED：新增 `primarySourceType` 脱敏、无证据字段 `UNKNOWN`、MockMvc 路由测试后，`mvn "-Dtest=com.dataspec.fieldprovenance.FieldProvenanceConfidenceServiceImplTest,com.dataspec.fieldprovenance.FieldProvenanceConfidenceControllerTest" test` 失败 2 个行为断言，分别证明 `primarySourceType` 未脱敏、无证据字段被评为 `REVIEW`。
- 评审修复 GREEN：同一定点命令结果 6 个测试通过。
- 评审修复后受影响后端测试：`mvn "-Dtest=FieldProvenanceConfidence*Test,FieldQuality*Test,StandardCandidate*Test" test`，结果 24 个测试通过。
- OpenSpec：`openspec validate add-field-provenance-confidence-summary --strict`，结果通过。
- 通用检查：`git diff --check`，结果无空白错误；Git 提示 `StandardCandidateRepository.java` 工作区 LF 将在 Git touch 时转换为 CRLF。
- 敏感词扫描：`rg -n "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" ...`，命中项均为 OpenSpec 禁止 raw secret 的说明和测试中的脱敏假输入，无真实凭据。
- 评审：子 agent `019f3994-ea6a-7a90-85a4-7d3c4fdf05fb` 做只读代码评审，结论为 `With fixes`，Important findings 为 `primarySourceType` 未脱敏、无证据字段等级与 OpenSpec 不一致、缺少 MockMvc 路由契约测试；三项均已修复。该 agent 已调用 `close_agent` 关闭。前一个中断期间创建的评审 agent `019f398e-c0ea-78e1-8ca2-b1d1e68f490f` 后续查询/关闭均返回 `not_found`，无需继续清理。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-field-provenance-confidence-summary --yes`，创建主规格 `openspec/specs/field-provenance-confidence/spec.md`，并归档到 `openspec/changes/archive/2026-07-07-add-field-provenance-confidence-summary/`。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，active change warning 从 13 降至 8；第三条 next action 为 `当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=8,severity=warning)`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：独立只读复评子 agent `019f3abb-7619-78c0-98de-9672aa19b115`（Planck）复评 staged archive diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
- 2026-07-07：补齐新增主规格 Purpose 后，独立只读复评子 agent `019f3ac0-d02f-75a1-801f-b97679d4f29c`（Helmholtz）复评 staged diff，结论 Ready，无 Critical / Important / Minor findings；已调用 `close_agent` 关闭。
