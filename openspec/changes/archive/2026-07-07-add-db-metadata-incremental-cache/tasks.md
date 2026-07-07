## 1. 测试先行

- [x] 1.1 新增后端 RED 测试：metadata cache miss 写入结构快照和 fingerprint，缓存内容不包含密码、JDBC URL、token 或业务行。
- [x] 1.2 新增后端 RED 测试：`AUTO` 命中新鲜缓存时复用 dump 且不打开 JDBC 连接，`BYPASS` 不读写缓存。
- [x] 1.3 新增后端 RED 测试：`REFRESH` 比较旧快照并返回字段新增、删除、属性变化和无变化摘要。
- [x] 1.4 新增 API/controller RED 测试：scan/browser/coverage 返回 cache status、metadataFingerprint、lastSeenAt、expiresAt 和 refreshMode。
- [x] 1.5 新增前端 RED 测试：缓存状态展示、手动刷新参数、AI 摘要包含 fingerprint 且脱敏。

## 2. 后端持久化与缓存服务

- [x] 2.1 新增 Flyway 迁移 `ds_database_metadata_cache`，包含字段 COMMENT、唯一索引和必要查询索引。
- [x] 2.2 新增 cache entity、mapper、repository，公共表面补充 Javadoc 和字段语义说明。
- [x] 2.3 新增请求/响应模型：`metadataCacheMode`、cache status、metadataFingerprint、changeSummary、lastSeenAt、expiresAt、sourceVersion 和 refreshMode。
- [x] 2.4 实现 `DatabaseMetadataCacheService`：来源 hash、快照规范化、SHA-256 fingerprint、TTL 判断、diff、脱敏和 upsert。
- [x] 2.5 确保缓存写入只保存 schema metadata，不保存密码、token、JDBC URL、连接串或源库业务数据行。

## 3. 后端流程接入

- [x] 3.1 将 cache service 接入 `DatabaseReverseImportServiceImpl.exportDump`，让 browser/preview/compare/coverage 复用统一 dump 链路。
- [x] 3.2 将 cache status 和 fingerprint 接入 scan plan，保持分页、cursor、cancel 和 resumeCommand 兼容。
- [x] 3.3 将 metadata fingerprint 和 cache evidence 接入 browser、dump、preview、compare、coverage 响应和 AI-readable summary。
- [x] 3.4 更新 capability/OpenAPI 相关模型或 schema，确保 AI 能发现缓存模式和返回字段。

## 4. 前端接入

- [x] 4.1 更新前端 API schema/types 和 database metadata 工具类型，保留新增字段可选以兼容旧响应。
- [x] 4.2 在反向导入页展示缓存新鲜度、fingerprint、lastSeenAt、expiresAt、refreshMode 和变化摘要。
- [x] 4.3 增加手动刷新入口，发送 `metadataCacheMode=REFRESH` 并保留已选表。
- [x] 4.4 在覆盖率数据库直连流程展示关联 metadata fingerprint 和缓存状态。

## 5. 文档、验证与收口

- [x] 5.1 更新直接受影响的 README/TODO 或等价文档，记录第一版能力、边界和 P6-71 状态。
- [x] 5.2 运行 `openspec validate add-db-metadata-incremental-cache --strict`。
- [x] 5.3 运行后端目标测试和必要的 `mvn test` 验证。
- [x] 5.4 运行前端目标测试、`pnpm test` 和必要的 `pnpm build` 验证。
- [x] 5.5 运行 `git diff --check`、暂存检查和敏感词扫描。
- [x] 5.6 启动独立子 agent 做只读代码评审，修复或记录 findings，并关闭 agent。
- [x] 5.7 补充 `Verification Evidence` 后按项目 Git 规则创建本地 commit；完成后再按用户目标进入下一个 TODO。

## Verification Evidence

- 2026-07-06 验证命令与结果：
  - `openspec validate add-db-metadata-incremental-cache --strict`：通过。
  - `mvn "-Dtest=DatabaseMetadataCacheServiceImplTest,DatabaseMetadataCacheRepositoryImplTest,DatabaseReverseImportServiceTest,AiCapabilityCatalogServiceImplTest" test`：45 tests 通过。
  - `mvn test`：475 tests 通过。
  - `node --test tests/databaseMetadataScan.test.ts tests/databaseMetadataBrowser.test.ts tests/frontendSmoke.test.ts`：38 tests 通过。
  - `pnpm test`：148 tests 通过。
  - `pnpm build`：通过；保留既有 Rolldown `INVALID_ANNOTATION` 与 chunk size warning，均未导致失败。
  - `git diff --check`：通过；PowerShell/git 输出包含 CRLF warning，未发现 whitespace error。
- 敏感词扫描：
  - changed files：45。
  - 命中 331 处，集中在脱敏测试 fixture、字段名、`<password>` 示例、JDBC 构造代码、安全边界文档和 TODO 说明；未发现真实凭据、token、私钥、生产 JDBC URL 或 DSN。
- 独立评审：
  - 初评 agent `019f37fa-11bc-7460-adbd-5bd45600fee4`，用途：只读代码评审；状态：已关闭；结论：5 个 Important finding 已修复，覆盖 scan REFRESH、summarize 部分命中、index membership diff、错误脱敏、upsert 并发冲突。
  - 复评 agent `019f380b-4507-7833-9324-e5ff3bc2e1bb`，用途：只读复评；状态：已关闭；结论：3 个 Important finding 已修复，覆盖删除表旧缓存失效、V27 时间列类型、BYPASS expiresAt；Minor 为本 Evidence 占位和 5.2-5.7 未勾选。
- 剩余风险：
  - Maven 本地依赖 POM warning、Java agent warning、前端 Rolldown annotation/chunk warning 均为既有非失败 warning。
  - 本 change 尚未 archive；当前收口目标为本地 commit，不 push。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-db-metadata-incremental-cache --yes`，同步 `db-metadata-dump`、`db-metadata-incremental-cache`、`db-metadata-scan-plan`、`db-reverse-import-frontend`、`field-coverage-report` 主规格，并归档到 `openspec/changes/archive/2026-07-07-add-db-metadata-incremental-cache/`；CLI 关于 delta 数量较多的 proposal warning 为非阻塞提示，归档已成功。
- 2026-07-07：补齐新建主规格 `openspec/specs/db-metadata-incremental-cache/spec.md` 的 Purpose，确认默认占位文本扫描无命中。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=pass`，active changes 为空，`totalIssues=0`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：补齐本次 touched 主规格 `openspec/specs/db-reverse-import-frontend/spec.md` 的 Purpose；新增行占位文本扫描无命中。
- 2026-07-07：`git diff --cached --check` 通过；暂存区敏感词扫描仅命中 password、token、JDBC、DSN 等安全约束文字，未发现真实凭据或可复制连接信息。
- 2026-07-07：独立归档复评 agent `019f3ad4-eaf6-76e3-9d9b-aa37f31bacf5`（Tesla，用途：最后 2 个数据库 OpenSpec change 归档只读复评）已完成并关闭；结论为未发现阻塞 commit 的问题，确认 active changes 清零、8 个主规格同步一致、占位无新增命中、敏感词命中均为安全说明。
