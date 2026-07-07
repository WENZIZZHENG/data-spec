## 1. OpenSpec 与 RED 测试

- [x] 1.1 完成 proposal、design、spec delta，并运行 `openspec validate add-testcontainers-db-integration-tests --strict`。
- [x] 1.2 新增后端 RED 集成测试骨架，先证明默认 Maven 配置尚不能运行 `*IT` Testcontainers 矩阵。
- [x] 1.3 新增 PostgreSQL RED 场景：真实 schema/table/comment/index metadata 可被 list/dump/browser/compare/coverage 读取。
- [x] 1.4 新增 MySQL RED 场景：真实 catalog/table/comment/index metadata 和 schemaName 边界可被 list/dump/browser/compare/coverage 读取。
- [x] 1.5 新增只读诊断 RED 场景：真实 PostgreSQL/MySQL 只读用户返回安全/健康诊断且不暴露凭据。

## 2. Maven Profile 与测试实现

- [x] 2.1 在 `dataspec-server/pom.xml` 新增 test scope Testcontainers 依赖、版本属性和 `db-integration` profile。
- [x] 2.2 实现 PostgreSQL/MySQL 容器 fixture 初始化，只创建 schema、空表、注释、索引和只读用户，不插入业务数据行。
- [x] 2.3 复用现有 `DatabaseReverseImportServiceImpl`、`JdbcDatabaseMetadataAdapter` 和 `FieldCoverageServiceImpl` 验证真实 metadata 流程。
- [x] 2.4 确保默认 `mvn test` 不运行容器测试，`mvn test -Pdb-integration` 才执行 `*IT.java`。
- [x] 2.5 检查新增测试、日志和文档不包含 raw password、完整 JDBC URL、DSN、token 或业务数据行。

## 3. 文档、验证与评审

- [x] 3.1 更新 README 验证章节和 TODO P6-82 状态，说明可选命令、Docker 边界和验证证据。
- [x] 3.2 运行 `openspec validate add-testcontainers-db-integration-tests --strict`。
- [x] 3.3 运行后端基础验证 `mvn test`，确认默认路径不依赖 Docker。
- [x] 3.4 在 Docker 可用时运行 `mvn test -Pdb-integration`；若环境不可用，记录明确阻塞原因和替代验证。
- [x] 3.5 运行 `git diff --check`、状态检查和敏感词扫描。
- [x] 3.6 启动独立子 agent 只读代码评审，修复或记录 findings。
- [x] 3.7 补充本 change 的 Verification Evidence，并创建本地 commit；不主动 archive 或 push。

## Verification Evidence

- RED：`cd dataspec-server; mvn -Dtest=DatabaseMetadataTestcontainersIT test` 失败，原因是 `org.testcontainers.*` 依赖尚未接入，证明测试先行覆盖缺口。
- RED：`cd dataspec-server; mvn -Dtest=DatabaseReverseImportServiceTest#testConnection_marksPostgresqlSelectOnlyAccountSafeWhenWriteRiskIsFalse test` 首次业务断言失败，现有 PostgreSQL 只读诊断在无写权限但 JDBC 未声明 readOnly 时返回非 SAFE。
- GREEN：`cd dataspec-server; mvn -Dtest=DatabaseReverseImportServiceTest#testConnection_marksPostgresqlSelectOnlyAccountSafeWhenWriteRiskIsFalse test` 通过，确认 PostgreSQL 无写权限账号可被判定为只读安全。
- 默认后端验证：`cd dataspec-server; mvn test` 通过，476 tests，确认默认路径不运行 `*IT.java`，不依赖 Docker；增强后的 IT 覆盖列和断言也已完成 testCompile。
- Docker profile：`cd dataspec-server; mvn test -Pdb-integration` 已进入并只运行 `DatabaseMetadataTestcontainersIT`；当前环境无有效 Docker，失败信息为 `Could not find a valid Docker environment`。替代证据是 IT 编译通过、profile 启动到 Testcontainers、默认 `mvn test` 通过；真实容器矩阵需在 Docker 可用环境补跑。
- OpenSpec：`openspec validate add-testcontainers-db-integration-tests --strict` 通过。
- 通用检查：`git diff --check` 通过，仅提示工作区 LF 将由 Git 转 CRLF。
- 状态检查：`node tools/dataspec-status-check.mjs --format json` 返回 `status=warn`，仅剩 active change warning：既有 `add-db-metadata-incremental-cache` 与本次 `add-testcontainers-db-integration-tests`。
- 敏感信息扫描：对新增 IT、README/TODO 和本 change 执行 `rg "(top-secret|Bearer |jdbc:postgresql://|jdbc:mysql://|password=|Authorization|api_key|apikey|token123)" ...`，未发现新增 raw password、完整 JDBC URL、DSN、token 或业务数据行；命中项为脱敏边界说明或测试中的 `doesNotContain` 断言。
- 独立评审：子 agent `019f3860-1984-7ac0-b3ac-d899e4b419a2`（用途：P6-82 真实数据库 Testcontainers 集成测试矩阵只读代码评审）已完成并关闭。Critical 无；Important 1 指出 compare/coverage spec 与 IT 断言覆盖不一致，已通过扩展 schema-only fixture 和断言补齐 matched/changed/new/missing-comment/non-standard、standard/alias/missing/possible-duplicate/unmanaged 与 coverageRate；Important 2 指出真实容器矩阵尚无 Docker green evidence，当前环境无 Docker，已作为未覆盖风险和后续补跑要求记录；Minor 的 MySQL `execInContainer` exit code 未检查已修复。
- 复评：子 agent `019f386a-008b-7ac1-9da9-683ea4a845c7`（用途：P6-82 修复后只读复评）已完成并关闭。Critical 无；Important 指出 MySQL root 密码与 Testcontainers `withPassword` 行为可能不一致，已移除独立 root password env，改用容器实际 `MYSQL.getPassword()` 执行 root 初始化，并保留输出不含 owner/readonly 动态密码断言。

## Archive Verification Evidence

- 2026-07-07：执行 `openspec archive add-testcontainers-db-integration-tests --yes`，同步 `db-metadata-dump`、`db-readonly-security-diagnostics`、`db-reverse-import-compare`、`db-testcontainers-integration-tests`、`field-coverage-report` 主规格，并归档到 `openspec/changes/archive/2026-07-07-add-testcontainers-db-integration-tests/`。
- 2026-07-07：补齐新建主规格 `openspec/specs/db-testcontainers-integration-tests/spec.md` 的 Purpose，确认默认占位文本扫描无命中。
- 2026-07-07：`openspec validate --all` 通过，118 passed、0 failed。
- 2026-07-07：`node --test tools/dataspec-status-check.test.mjs tools/dataspec-verify-advisor.test.mjs tools/dataspec-cli-mcp-contract-check.test.mjs` 通过，44 pass、0 fail。
- 2026-07-07：`node tools/dataspec-status-check.mjs --format json` 返回 `status=pass`，active changes 为空，`totalIssues=0`。
- 2026-07-07：`git diff --check` 退出码 0，仅输出 Windows LF/CRLF 提示。
- 2026-07-07：补齐本次 touched 主规格 `openspec/specs/db-reverse-import-frontend/spec.md` 的 Purpose；新增行占位文本扫描无命中。
- 2026-07-07：`git diff --cached --check` 通过；暂存区敏感词扫描仅命中 password、token、JDBC、DSN 等安全约束文字，未发现真实凭据或可复制连接信息。
- 2026-07-07：独立归档复评 agent `019f3ad4-eaf6-76e3-9d9b-aa37f31bacf5`（Tesla，用途：最后 2 个数据库 OpenSpec change 归档只读复评）已完成并关闭；结论为未发现阻塞 commit 的问题，确认 active changes 清零、8 个主规格同步一致、占位无新增命中、敏感词命中均为安全说明。
