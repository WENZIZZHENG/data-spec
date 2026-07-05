## 1. 草稿确认

- [x] 1.1 确认 P6-68 命中 SDD full：数据库迁移、API 公共契约、标准资产写入和 AI 外部可见契约。
- [x] 1.2 明确第一版边界：版本化共享包、应用预览、确认应用、漂移报告和 AI Context 来源说明；不做企业级包仓库、审批或自动覆盖。

## 2. 测试先行

- [x] 2.1 新增后端失败测试：标准复用包 service/controller/API 模型缺失；包 hash、未知包、目标项目缺失应失败且不写入。
- [x] 2.2 新增后端成功测试：创建包生成稳定 hash；预览展示 create/skip/drift；应用只创建缺失资产、标记字段来源并保存应用记录。
- [x] 2.3 新增 AI Context 测试：字段目录导出 `standardPackSources`，manifest 导出最近标准包应用摘要。
- [x] 2.4 新增前端失败测试：标准复用包显示工具和页面入口缺失。
- [x] 2.5 运行失败测试，确认失败原因来自 P6-68 功能缺失。

## 3. 实现

- [x] 3.1 新增数据库迁移、实体、mapper、Repository 和公共模型，所有公共字段补充职责或字段语义说明。
- [x] 3.2 新增标准复用包 Service：创建包、列表、详情、预览、应用、应用历史和 drift report。
- [x] 3.3 新增 Controller 和 API 契约，确保响应不含凭据、源库行值或 raw secret。
- [x] 3.4 扩展 AI Context 导出标准包来源和应用摘要，保持 additive 兼容。
- [x] 3.5 新增前端 API wrapper、类型、工具函数、路由、导航和“标准复用包”页面。
- [x] 3.6 更新 README/TODO 或相关权威文档，记录能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate add-standard-reuse-pack --strict`。
- [x] 4.2 运行后端目标测试和必要全量测试。
- [x] 4.3 运行前端目标测试、`pnpm test` 和 `pnpm build`。
- [x] 4.4 启动独立子 agent 做只读代码评审并修复或记录 findings。
- [x] 4.5 完成本地 commit；除非用户后续要求，否则暂不 archive。

## Verification Evidence

- RED（2026-07-05）：`mvn "-Dtest=StandardReusePackServiceImplTest,StandardReusePackControllerTest,AiContextExportServiceTest" test` 失败于缺少 `com.dataspec.standardreuse` 模型、service、controller、entity 和 repository；`pnpm exec node --test tests/standardReusePackDisplay.test.ts tests/frontendSmoke.test.ts` 失败于缺少 `src/utils/standardReusePackDisplay.ts`、`StandardReusePack.vue` 和 `/standard-reuse-packs` 路由。失败原因来自 P6-68 功能缺失。
- GREEN（后端目标，2026-07-05）：`mvn "-Dtest=StandardReusePackServiceImplTest,StandardReusePackControllerTest,AiContextExportServiceTest" test` 通过 31 tests。
- GREEN（前端目标，2026-07-05）：`pnpm exec node --test tests/standardReusePackDisplay.test.ts tests/frontendSmoke.test.ts` 通过 31 tests。
- FULL（后端，2026-07-05）：`mvn test` 通过 440 tests；存在既有 Maven transitive POM warning、JSqlParser 负例解析 warning、PerformanceBaselineTest 慢操作 warning 和 ByteBuddy agent warning，未导致失败。
- FULL（前端，2026-07-05）：`pnpm test` 通过 134 tests；`pnpm build` 通过，存在既有 Rolldown 第三方 `#__PURE__` annotation 与 chunk size warning，未导致失败。
- OpenSpec（2026-07-05）：`openspec validate add-standard-reuse-pack --strict` 通过；`openspec validate --all` 通过 100 items。
- 通用检查（2026-07-05）：`git diff --check` 通过；仅提示工作区文件行尾将按 Git 配置从 LF 转为 CRLF。
- Review（2026-07-05）：独立只读评审子 agent `019f3094-46ef-7891-aac2-686abcc8a5ef`（用途：P6-68 SDD full 代码评审）发现 2 个 P1、1 个 P2 和 1 个 P3；已关闭该 agent。已修复：复用包 payload 脱敏与敏感字段样例不打包、应用记录改为保存 post-apply drift、drift 比较覆盖字段/枚举/模板完整内容并区分 `OVERRIDDEN`/`DRIFTED`、TODO P6-68 状态同步。
- GREEN（评审修复目标，2026-07-05）：`mvn "-Dtest=StandardReusePackServiceImplTest,StandardReusePackControllerTest,AiContextExportServiceTest" test` 通过 33 tests。
- FULL（复评后，2026-07-05）：`mvn test` 通过 442 tests；`pnpm test` 通过 134 tests；`pnpm build` 通过；`openspec validate add-standard-reuse-pack --strict` 通过；`openspec validate --all` 通过 100 items；`git diff --check` 通过。
- Review（复评，2026-07-05）：独立只读复评子 agent `019f30a3-6229-7602-a96b-e16d8b8aeda6`（用途：P6-68 评审修复复查）未发现阻塞问题；已关闭该 agent。剩余风险：缺少真实 PostgreSQL/MyBatis/Flyway 集成测试；脱敏器覆盖明确 secret 模式但不识别所有自然语言隐私；JSON drift 为字符串级比较，可能产生语义等价格式差异的 false positive。
