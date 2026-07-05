## 1. 草稿确认

- [x] 1.1 确认 OpenSpec change id、capability、验收标准和只读边界。
- [x] 1.2 补充 browser API、schema dump index metadata、前端子视图、AI 摘要和子 agent 评审要求。

## 2. 测试先行

- [x] 2.1 根据验收标准新增失败测试：连接数据库后无需导入即可浏览元数据；AI 可读取选中表的结构摘要并继续生成候选导入或覆盖率报告；全流程只读且不采样业务数据行。
- [x] 2.2 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 按最小改动实现：新增只读 metadata browser API、前端元数据浏览入口或反向导入页子视图；支持按 schema/table/column/comment/index 搜索，展示字段标准匹配、缺注释、类型差异和可加入导入候选的勾选状态。
- [x] 3.2 更新 README/TODO 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate <change-id> --strict`。
- [x] 4.2 运行与改动范围匹配的验证命令，并记录证据。
- [x] 4.3 启动独立子 agent 做只读代码评审并修复或记录 findings。
- [x] 4.4 完成本地 commit；除非用户后续要求，否则暂不 archive。

## Verification Evidence

- OpenSpec：`openspec validate add-database-candidate --strict` 通过，确认 browser API、schema dump index metadata、AI 摘要和候选选择要求可被严格校验。
- 后端 RED：`mvn "-Dtest=DatabaseReverseImportServiceTest,ReverseImportControllerTest" test` 曾因 `DatabaseMetadataBrowser` 等新模型缺失失败；补充评审回归时，`mvn "-Dtest=DatabaseReverseImportServiceTest" test` 曾因 metadata `defaultValue/comment` 泄漏连接密码原文失败，确认测试先于修复覆盖缺口。
- 前端 RED：`node --test tests/databaseMetadataBrowser.test.ts` 曾因 `src/utils/databaseMetadataBrowser.ts` 缺失失败，确认测试先于实现覆盖缺口。
- 后端 GREEN：`mvn "-Dtest=DatabaseReverseImportServiceTest,ReverseImportControllerTest" test` 通过，17 tests，覆盖只读 browser service、索引 metadata、候选选择、AI 摘要脱敏、metadata 自由文本脱敏和 controller 委托。
- 前端 GREEN：`node --test tests/databaseMetadataBrowser.test.ts` 通过，覆盖字段扁平化、schema/table/column/comment/type/index/标准匹配搜索、默认候选 key 和 AI 摘要脱敏。
- 后端回归：`mvn test` 通过，418 tests，0 failures/errors/skipped；仅保留既有 Maven POM 与 ByteBuddy 动态 agent warning。
- 前端回归：`pnpm test` 通过，111 tests；`pnpm build` 通过，仅保留既有 Rolldown annotation、chunk size 和 plugin timing warning。
- 通用检查：`git diff --check` 通过；Git 仅提示 Windows 工作区 CRLF 转换 warning，无 whitespace error。
- 评审：只读代码评审子 agent `019f2ff7-f59b-7710-a5cb-3b30c5f2a5d5`（用途：P6-63 API/AI 契约和前后端实现复评）已完成并关闭。P1 metadata `defaultValue/comment` 可能泄漏连接密码原文，已在 `DatabaseReverseImportServiceImpl` 返回前递归脱敏并补后端回归测试；P2 `AGENTS.md` 变更为误报，当前 `git status --short AGENTS.md` 与 `git diff -- AGENTS.md` 无输出且本次提交将显式 pathspec；P3 public API/service 方法缺少契约说明，已补 `browseDatabaseMetadata` 与 `browse` Javadoc。
- 未覆盖风险：未在本轮启动真实后端服务重新运行 `pnpm check:api` 从 `/api-docs` 再生成 schema；当前通过后端编译、controller 测试、手工 schema patch、前端类型/构建和 smoke 测试降低漂移风险。
