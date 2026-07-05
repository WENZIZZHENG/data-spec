## 1. 草稿确认

- [x] 1.1 确认 OpenSpec change id、SDD full 分级、验收标准和边界。
- [x] 1.2 基于 P6-65 TODO、字段冲突、候选合并、字段生命周期和变更日志现状形成产品契约。

## 2. 测试先行

- [x] 2.1 新增后端失败测试：merge preview/apply 模型和接口缺失；跨项目、同字段、缺 reason、已 replacement 来源字段应失败。
- [x] 2.2 新增后端成功测试：preview 输出 alias/tag/example/source 迁移建议、风险、rollbackHints；apply 更新目标字段、废弃来源字段并记录变更日志。
- [x] 2.3 新增前端失败测试：合并向导工具和页面耦合缺失；缺少确认理由时不调用 apply。
- [x] 2.4 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 后端新增 merge preview/apply 请求、响应、风险、变更和回滚提示模型，所有公共字段补 Javadoc。
- [x] 3.2 后端新增合并服务和 controller，事务化 apply，沿用字段 repository、字段生命周期和变更日志模式。
- [x] 3.3 前端新增 API wrapper、类型、合并向导工具函数和字段冲突/字段库入口，导出类型补 TSDoc。
- [x] 3.4 更新 README/TODO/docs/ai-contracts 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate add-standard-field-merge-wizard --strict`。
- [x] 4.2 运行后端目标测试和必要全量测试。
- [x] 4.3 运行前端目标测试、`pnpm test` 和必要 build。
- [x] 4.4 启动独立子 agent 做只读代码评审并修复或记录 findings。
- [x] 4.5 完成本地 commit；除非用户后续要求，否则暂不 archive。

## Verification Evidence

- RED（2026-07-05）：`mvn "-Dtest=StandardFieldMergeServiceImplTest,StandardFieldMergeControllerTest" test` 失败于缺少 `com.dataspec.fieldmerge` 模型、controller 和 service；`pnpm exec node --test tests/standardFieldMergeDisplay.test.ts` 失败于缺少 `src/utils/standardFieldMerge.ts`。失败原因来自 P6-65 功能缺失。
- GREEN（2026-07-05）：`mvn "-Dtest=StandardFieldMergeServiceImplTest,StandardFieldMergeControllerTest" test` 通过 6 tests；`pnpm exec node --test tests/standardFieldMergeDisplay.test.ts tests/frontendSmoke.test.ts` 通过 29 tests。
- OpenSpec（2026-07-05）：`openspec validate add-standard-field-merge-wizard --strict` 通过；`openspec validate --all` 通过 99 items。
- 评审（2026-07-05）：独立只读代码评审 agent `019f303f-21a6-7a00-9bb1-af0961963f30`（用途：P6-65 SDD full 强制评审）发现 4 个 Important 和 1 个 Minor；已修复格式风险 target-only 漏报、来源字段并发合并条件更新、前端旧 preview 误提交、standard-field-merge result 契约缺口和 design 文档迁移边界；agent 已关闭。
- 后端（2026-07-05）：修复评审后 `mvn "-Dtest=StandardFieldMergeServiceImplTest,StandardFieldMergeControllerTest,AiCapabilityCatalogServiceImplTest,SchemaRegistryServiceImplTest" test` 通过 20 tests；`mvn test` 通过 432 tests。
- 前端（2026-07-05）：修复评审后 `pnpm exec node --test tests/standardFieldMergeDisplay.test.ts tests/frontendSmoke.test.ts` 通过 30 tests；`pnpm test` 通过 120 tests；`pnpm build` 通过，仅有既有依赖 pure annotation 和 chunk size warning。
- 通用检查（2026-07-05）：`git diff --check` 和 `git diff --cached --check` 通过，仅报告 Windows 工作区 LF/CRLF 提示；`git diff --cached --stat` 和 `git diff --cached` 已核对；staged diff/files 敏感词扫描只命中文档边界说明、测试脚本和 `apiToken` 文件名，未发现真实凭据。
