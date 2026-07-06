## 1. 测试先行

- [x] 1.1 新增 AI 任务推荐 service 单测，覆盖热区治理、待处理候选、质量门禁失败和安全摘要。
- [x] 1.2 新增 AI 任务推荐 controller MockMvc 测试，覆盖 `GET /api/ai-task-recommendations?projectId=<id>` 路由、参数绑定和 `R` JSON 包装。
- [x] 1.3 运行新增测试并确认 RED 失败原因是能力缺失。

## 2. 后端实现

- [x] 2.1 新增 `aitaskrecommendation` 响应模型与字段级说明。
- [x] 2.2 新增 `AiTaskRecommendationService` 与实现，聚合标准健康、热区、候选和质量门禁摘要。
- [x] 2.3 新增 `AiTaskRecommendationController` 暴露只读 API。
- [x] 2.4 确保响应不返回 SQL 原文、AI payload、候选 raw evidence 或凭据。

## 3. 验证与收口

- [x] 3.1 运行受影响后端测试并确认 GREEN。
- [x] 3.2 运行 `openspec validate add-ai-task-recommendation-queue --strict`。
- [x] 3.3 运行 `git diff --check` 和敏感词扫描，并补充 Verification Evidence。
- [x] 3.4 启动独立子 agent 做只读代码评审，关闭子 agent，并处理或记录 findings。
- [x] 3.5 commit 前执行 staged diff、staged check、敏感词扫描和本地 commit。

## Verification Evidence

- RED：`mvn "-Dtest=com.dataspec.aitaskrecommendation.AiTaskRecommendationServiceImplTest,com.dataspec.aitaskrecommendation.AiTaskRecommendationControllerTest" test`，结果失败，原因是 `aitaskrecommendation` controller/model/service 尚不存在。
- GREEN：同一定点命令结果 3 个测试通过。
- Review RED：同一定点命令在补充 review 回归测试后失败，原因是低信号项目少于 3 个任务、健康动作带 query route 拼接错误、候选任务重复。
- GREEN：同一定点命令结果 6 个测试通过。
- 受影响后端测试：`mvn "-Dtest=AiTaskRecommendation*Test,StandardUsageHeatmap*Test,StandardQualityGate*Test,StandardHealth*Test,StandardCandidate*Test" test`，结果 34 个测试通过。
- OpenSpec：`openspec validate add-ai-task-recommendation-queue --strict`，结果通过。
- 通用检查：`git diff --check`，结果通过。
- 敏感词扫描：`rg -n "password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn" dataspec-server/src/main/java/com/dataspec/aitaskrecommendation dataspec-server/src/test/java/com/dataspec/aitaskrecommendation openspec/changes/add-ai-task-recommendation-queue`，命中项均为 OpenSpec 禁止 raw secret 的说明、测试中的脱敏假输入和出口脱敏正则，无真实凭据。
- 评审：子 agent `019f39c7-e6a6-7a80-af63-61965d6c2fca` 结论 `With fixes`，指出任务数与 OpenSpec 不一致、健康动作 route query 拼接错误、候选任务重复和脱敏测试不足；已修复并关闭。
