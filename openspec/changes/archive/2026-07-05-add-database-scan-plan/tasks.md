## 1. 草稿确认

- [x] 1.1 确认 OpenSpec change id、capability、验收标准和边界。
- [x] 1.2 将自动 handoff 草稿改为 `db-metadata-scan-plan` 产品契约，删除人工确认类 Open Questions。

## 2. 测试先行

- [x] 2.1 根据验收标准新增失败测试：上百张表的数据库可分批加载并生成部分预览；中途取消不会写入标准库；AI 能根据 cursor 继续下一批或停止，resumeCommand 不泄漏敏感信息。
- [x] 2.2 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 按最小改动实现：扩展直连 metadata 查询为可分页扫描；输出 scanId、estimatedTableCount、cursor、progress、partialSummary、resumeCommand 和 cancel 状态；前端按批次展示和筛选。
- [x] 3.2 更新 README/TODO 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate <change-id> --strict`。
- [x] 4.2 运行与改动范围匹配的验证命令，并记录证据。
- [x] 4.3 启动独立子 agent 做只读代码评审并修复或记录 findings。
- [x] 4.4 完成本地 commit；除非用户后续要求，否则暂不 archive。

## Verification Evidence

- RED：`mvn "-Dtest=DatabaseReverseImportServiceTest,ReverseImportControllerTest" test` 曾因缺少 `DatabaseMetadataScanReq/Result` 失败；`node --test tests/databaseMetadataScan.test.ts` 曾因缺少 `src/utils/databaseMetadataScan.ts` 失败。
- GREEN（目标验证）：`mvn "-Dtest=DatabaseReverseImportServiceTest,ReverseImportControllerTest" test` 通过，20 tests，0 failures/errors/skipped；`node --test tests/databaseMetadataScan.test.ts tests/frontendSmoke.test.ts` 通过，30 tests。
- OpenSpec：`openspec validate add-database-scan-plan --strict` 通过。
- 全量验证：`mvn test`（`dataspec-server`）通过，423 tests，0 failures/errors/skipped；`pnpm test`（`dataspec-web`）通过，116 tests；`pnpm build`（`dataspec-web`）通过。
- 通用检查：`git diff --check` 通过，仅输出 Windows 行尾提示。
- 独立评审：子 agent `019f3016-b1bd-7931-8b8c-839c00563593` 完成只读评审并已关闭；发现扫描失败脱敏、完成态取消、跨批次选择计数和 capability catalog 漏同步问题，均已修复并补测试。
- 评审修复验证：`mvn "-Dtest=DatabaseReverseImportServiceTest,AiCapabilityCatalogServiceImplTest" test` 通过，26 tests；`node --test tests/reverseImportSelection.test.ts tests/frontendSmoke.test.ts` 通过，33 tests。
- Commit 前检查：`git diff --cached --check` 通过；staged filename 敏感词扫描无命中；staged diff 敏感词命中均为 `<password>` 占位、脱敏边界说明、字段名、`top-secret`/`password=secret` 测试 fixture 和 `jdbc:` 脱敏断言，无真实凭据。
